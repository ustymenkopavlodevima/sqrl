package ai.dataeng.sqml.analyzer;

import static ai.dataeng.sqml.analyzer.AggregationAnalyzer.verifyOrderByAggregations;
import static ai.dataeng.sqml.analyzer.AggregationAnalyzer.verifySourceAggregations;
import static ai.dataeng.sqml.analyzer.ExpressionTreeUtils.extractAggregateFunctions;
import static ai.dataeng.sqml.analyzer.ExpressionTreeUtils.extractExpressions;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static java.util.Collections.emptyList;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

import ai.dataeng.sqml.OperatorType.QualifiedObjectName;
import ai.dataeng.sqml.function.FunctionProvider;
import ai.dataeng.sqml.logical.RelationDefinition;
import ai.dataeng.sqml.metadata.Metadata;
import ai.dataeng.sqml.schema2.Field;
import ai.dataeng.sqml.schema2.RelationType;
import ai.dataeng.sqml.schema2.Type;
import ai.dataeng.sqml.schema2.basic.BooleanType;
import ai.dataeng.sqml.tree.AliasedRelation;
import ai.dataeng.sqml.tree.AllColumns;
import ai.dataeng.sqml.tree.AstVisitor;
import ai.dataeng.sqml.tree.DereferenceExpression;
import ai.dataeng.sqml.tree.Except;
import ai.dataeng.sqml.tree.Expression;
import ai.dataeng.sqml.tree.ExpressionRewriter;
import ai.dataeng.sqml.tree.ExpressionTreeRewriter;
import ai.dataeng.sqml.tree.FunctionCall;
import ai.dataeng.sqml.tree.GroupingElement;
import ai.dataeng.sqml.tree.GroupingOperation;
import ai.dataeng.sqml.tree.Identifier;
import ai.dataeng.sqml.tree.Intersect;
import ai.dataeng.sqml.tree.Join;
import ai.dataeng.sqml.tree.JoinCriteria;
import ai.dataeng.sqml.tree.JoinOn;
import ai.dataeng.sqml.tree.LongLiteral;
import ai.dataeng.sqml.tree.Node;
import ai.dataeng.sqml.tree.OrderBy;
import ai.dataeng.sqml.tree.QualifiedName;
import ai.dataeng.sqml.tree.Query;
import ai.dataeng.sqml.tree.QuerySpecification;
import ai.dataeng.sqml.tree.Relation;
import ai.dataeng.sqml.tree.Select;
import ai.dataeng.sqml.tree.SelectItem;
import ai.dataeng.sqml.tree.SetOperation;
import ai.dataeng.sqml.tree.SingleColumn;
import ai.dataeng.sqml.tree.SortItem;
import ai.dataeng.sqml.tree.Table;
import ai.dataeng.sqml.tree.TableSubquery;
import ai.dataeng.sqml.tree.Union;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatementAnalyzer extends AstVisitor<Scope, Scope> {
private Logger log = Logger.getLogger(StatementAnalyzer.class.getName());

private final Metadata metadata;
private final Analysis analysis;

public StatementAnalyzer(Metadata metadata, Analysis analysis) {
  this.metadata = metadata;
  this.analysis = analysis;
}
//
//  // Currently we only analyze queries here although this could be a Statement
//  public Scope analyze(Query query,
//      Scope scope) {
//    Visitor visitor = new Visitor();
//    return query.accept(visitor, scope);
//  }
//
//  public class Visitor extends AstVisitor<Scope, Scope> {

  @Override
  protected Scope visitNode(Node node, Scope context) {
    throw new RuntimeException(String.format("Could not process node %s : %s", node.getClass().getName(), node));
  }

  @Override
  protected Scope visitQuery(Query node, Scope scope) {
    //Unions have a limit & order that is outside the query body. If these are empty, just process the
    //  query body as if it was a standalone query.
    if (node.parseLimit().isEmpty() && node.getOrderBy().isEmpty()) {
      return node.getQueryBody().accept(this, scope);
    } else {
      throw new RuntimeException("TBD: Limit and Order");
    }
//
//    List<Expression> orderByExpressions = emptyList();
//    if (node.getOrderBy().isPresent()) {
//      orderByExpressions = analyzeOrderBy(node, getSortItemsFromOrderBy(node.getOrderBy()), queryBodyScope);
//    }
//    analysis.setOrderByExpressions(node, orderByExpressions);
//
//    if (node.parseLimit().isPresent()) {
//      analysis.setMultiplicity(node, node.parseLimit().get());
//    }
//
//    return createAndAssignScope(node, scope, queryBodyScope.getRelation());
  }

  @Override
  protected Scope visitQuerySpecification(QuerySpecification node, Scope scope) {
    Scope sourceScope = node.getFrom().accept(this, scope);

    node.getWhere().ifPresent(where -> analyzeWhere(node, sourceScope, where));

    List<Expression> outputExpressions = analyzeSelect(node, sourceScope);
    List<Expression> groupByExpressions = analyzeGroupBy(node, sourceScope, outputExpressions);
    analyzeHaving(node, sourceScope);
    Scope outputScope = computeAndAssignOutputScope(node, scope, sourceScope);

    List<Expression> orderByExpressions = emptyList();
    Optional<Scope> orderByScope = Optional.empty();
    if (node.getOrderBy().isPresent()) {
      if (node.getSelect().isDistinct()) {
        verifySelectDistinct(node, outputExpressions);
      }

      OrderBy orderBy = node.getOrderBy().get();
      orderByScope = Optional.of(computeAndAssignOrderByScope(orderBy, sourceScope, outputScope));

      orderByExpressions = analyzeOrderBy(node, orderBy.getSortItems(), orderByScope.get());
    }
    analysis.setOrderByExpressions(node, orderByExpressions);

    List<Expression> sourceExpressions = new ArrayList<>(outputExpressions);
    node.getHaving().ifPresent(sourceExpressions::add);

    analyzeGroupingOperations(node, sourceExpressions, orderByExpressions);
    List<FunctionCall> aggregates = analyzeAggregations(node, sourceExpressions, orderByExpressions);

    if (!aggregates.isEmpty() && groupByExpressions.isEmpty()) {
      // Have Aggregation functions but no explicit GROUP BY clause
      analysis.setGroupByExpressions(node, ImmutableList.of());
    }

    verifyAggregations(node, sourceScope, orderByScope, groupByExpressions, sourceExpressions, orderByExpressions);

//      analyzeWindowFunctions(node, outputExpressions, orderByExpressions);

    if (analysis.isAggregation(node) && node.getOrderBy().isPresent()) {
      // Create a different scope for ORDER BY expressions when aggregation is present.
      // This is because planner requires scope in order to resolve names against fields.
      // Original ORDER BY scope "sees" FROM query fields. However, during planning
      // and when aggregation is present, ORDER BY expressions should only be resolvable against
      // output scope, group by expressions and aggregation expressions.
      List<GroupingOperation> orderByGroupingOperations = extractExpressions(orderByExpressions, GroupingOperation.class);
      List<FunctionCall> orderByAggregations = extractAggregateFunctions(orderByExpressions, metadata.getFunctionProvider());
      computeAndAssignOrderByScopeWithAggregation(node.getOrderBy().get(), sourceScope, outputScope, orderByAggregations, groupByExpressions, orderByGroupingOperations);
    }

    if (node.parseLimit().isPresent()) {
      //todo bubble up multiplicity
      analysis.setMultiplicity(node, node.parseLimit().get());
    }

    return outputScope;
  }

  @Override
  protected Scope visitTable(Table table, Scope scope) {
    RelationDefinition relation = scope.resolveRelation(table.getName())
        .orElseThrow(() -> new RuntimeException(String.format("Could not find table: %s", table.getName())));
    return createAndAssignScope(table, scope, relation);
  }

  @Override
  protected Scope visitTableSubquery(TableSubquery node, Scope context) {
    return node.getQuery().accept(this, context);
  }

  @Override
  protected Scope visitJoin(Join node, Scope scope) {
    Scope left = node.getLeft().accept(this, scope);
    Scope right = node.getRight().accept(this, scope);

    Scope result = createAndAssignScope(node, scope, join(left.getRelation(), right.getRelation()));

    //Todo verify that an empty criteria on a join can be a valid traversal
    if (node.getType() == Join.Type.CROSS || node.getType() == Join.Type.IMPLICIT || node.getCriteria().isEmpty()) {
      return result;
    }

    JoinCriteria criteria = node.getCriteria().get();
    if (criteria instanceof JoinOn) {
      Expression expression = ((JoinOn) criteria).getExpression();

      // need to register coercions in case when join criteria requires coercion (e.g. join on char(1) = char(2))
      ExpressionAnalysis expressionAnalysis = analyzeExpression(expression, result);
      Type clauseType = expressionAnalysis.getType(expression);
      if (!(clauseType instanceof BooleanType)) {
        throw new RuntimeException(String.format("JOIN ON clause must evaluate to a boolean: actual type %s", clauseType));
      }
      verifyNoAggregateGroupingFunctions(metadata.getFunctionProvider(), expression, "JOIN clause");

      //todo: restrict grouping criteria
      analysis.setJoinCriteria(node, expression);
    } else {
      throw new RuntimeException("Unsupported join");
    }

    return result;
  }

  @Override
  protected Scope visitAliasedRelation(AliasedRelation relation, Scope scope) {
    Scope relationScope = relation.getRelation().accept(this, scope);

    RelationType relationType = relationScope.getRelation();

    RelationType descriptor = withAlias(relationType, relation.getAlias().getValue());

    return createAndAssignScope(relation, scope, descriptor);
  }

  @Override
  protected Scope visitUnion(Union node, Scope scope) {
    return visitSetOperation(node, scope);
  }

  @Override
  protected Scope visitIntersect(Intersect node, Scope scope) {
    return visitSetOperation(node, scope);
  }

  @Override
  protected Scope visitExcept(Except node, Scope scope) {
    return visitSetOperation(node, scope);
  }

  @Override
  protected Scope visitSetOperation(SetOperation node, Scope scope)
  {
    checkState(node.getRelations().size() >= 2);
    List<Scope> relationScopes = node.getRelations().stream()
        .map(relation -> {
          Scope relationScope = process(relation, scope);
          return createAndAssignScope(relation, scope, relationScope.getRelation());
        })
        .collect(toImmutableList());

    Type[] outputFieldTypes = relationScopes.get(0).getRelation().getFields().stream()
        .map(Field::getType)
        .toArray(Type[]::new);
    int outputFieldSize = outputFieldTypes.length;

    for (Scope relationScope : relationScopes) {
      RelationType relationType = relationScope.getRelation();
      int descFieldSize = relationType.getFields().size();
      String setOperationName = node.getClass().getSimpleName().toUpperCase(ENGLISH);
      if (outputFieldSize != descFieldSize) {
        throw new RuntimeException(
            String.format(
            "%s query has different number of fields: %d, %d",
            setOperationName,
            outputFieldSize,
            descFieldSize));
      }
      for (int i = 0; i < descFieldSize; i++) {
        /*
        Type descFieldType = relationType.getFieldByIndex(i).getType();
        Optional<Type> commonSuperType = metadata.getTypeManager().getCommonSuperType(outputFieldTypes[i], descFieldType);
        if (!commonSuperType.isPresent()) {
          throw new SemanticException(
              TYPE_MISMATCH,
              node,
              "column %d in %s query has incompatible types: %s, %s",
              i + 1,
              setOperationName,
              outputFieldTypes[i].getDisplayName(),
              descFieldType.getDisplayName());
        }
        outputFieldTypes[i] = commonSuperType.get();
         */
        //Super types?
      }
    }

    Field[] outputDescriptorFields = new Field[outputFieldTypes.length];
    RelationType firstDescriptor = relationScopes.get(0).getRelation();
    for (int i = 0; i < outputFieldTypes.length; i++) {
      Field oldField = (Field)firstDescriptor.getFields().get(i);
//      outputDescriptorFields[i] = new Field(
//          oldField.getRelationAlias(),
//          oldField.getName(),
//          outputFieldTypes[i],
//          oldField.isHidden(),
//          oldField.getOriginTable(),
//          oldField.getOriginColumnName(),
//          oldField.isAliased(), Optional.empty());
    }

    for (int i = 0; i < node.getRelations().size(); i++) {
      Relation relation = node.getRelations().get(i);
      Scope relationScope = relationScopes.get(i);
      RelationType relationType = relationScope.getRelation();
      for (int j = 0; j < relationType.getFields().size(); j++) {
        Type outputFieldType = outputFieldTypes[j];
        Type descFieldType = ((Field)relationType.getFields().get(j)).getType();
        if (!outputFieldType.equals(descFieldType)) {
//            analysis.addRelationCoercion(relation, outputFieldTypes);
          throw new RuntimeException(String.format("Mismatched types in set operation %s", relationType.getFields().get(j)));
//            break;
        }
      }
    }

    return createAndAssignScope(node, scope, new ArrayList<>(List.of(outputDescriptorFields)));
  }

  private void verifyAggregations(
      QuerySpecification node,
      Scope sourceScope,
      Optional<Scope> orderByScope,
      List<Expression> groupByExpressions,
      List<Expression> outputExpressions,
      List<Expression> orderByExpressions)
  {
    checkState(orderByExpressions.isEmpty() || orderByScope.isPresent(), "non-empty orderByExpressions list without orderByScope provided");

    if (analysis.isAggregation(node)) {
      // ensure SELECT, ORDER BY and HAVING are constant with respect to group
      // e.g, these are all valid expressions:
      //     SELECT f(a) GROUP BY a
      //     SELECT f(a + 1) GROUP BY a + 1
      //     SELECT a + sum(b) GROUP BY a
      List<Expression> distinctGroupingColumns = groupByExpressions.stream()
          .distinct()
          .collect(toImmutableList());

      for (Expression expression : outputExpressions) {
        verifySourceAggregations(distinctGroupingColumns, sourceScope, expression, metadata, analysis);
      }

      for (Expression expression : orderByExpressions) {
        verifyOrderByAggregations(distinctGroupingColumns, sourceScope, orderByScope.get(), expression, metadata, analysis);
      }
    }
  }

  private List<FunctionCall> analyzeAggregations(
      QuerySpecification node,
      List<Expression> outputExpressions,
      List<Expression> orderByExpressions)
  {
    List<FunctionCall> aggregates = extractAggregateFunctions(Iterables.concat(outputExpressions, orderByExpressions), metadata.getFunctionProvider());
    analysis.setAggregates(node, aggregates);
    return aggregates;
  }


  private void analyzeGroupingOperations(QuerySpecification node, List<Expression> outputExpressions, List<Expression> orderByExpressions)
  {
    List<GroupingOperation> groupingOperations = extractExpressions(Iterables.concat(outputExpressions, orderByExpressions), GroupingOperation.class);
    boolean isGroupingOperationPresent = !groupingOperations.isEmpty();

    if (isGroupingOperationPresent && !node.getGroupBy().isPresent()) {
      throw new RuntimeException(
          "A GROUPING() operation can only be used with a corresponding GROUPING SET/CUBE/ROLLUP/GROUP BY clause");
    }

    analysis.setGroupingOperations(node, groupingOperations);
  }

  private void verifySelectDistinct(QuerySpecification node, List<Expression> outputExpressions)
  {
    for (SortItem item : node.getOrderBy().get().getSortItems()) {
      Expression expression = item.getSortKey();


      Expression rewrittenOrderByExpression = ExpressionTreeRewriter.rewriteWith(
          new OrderByExpressionRewriter(extractNamedOutputExpressions(node.getSelect())), expression);
      int index = outputExpressions.indexOf(rewrittenOrderByExpression);
      if (index == -1) {
        throw new RuntimeException(String.format("For SELECT DISTINCT, ORDER BY expressions must appear in select list"));
      }
//        if (!isDeterministic(expression)) {
//          throw new SemanticException(NONDETERMINISTIC_ORDER_BY_EXPRESSION_WITH_SELECT_DISTINCT, expression, "Non deterministic ORDER BY expression is not supported with SELECT DISTINCT");
//        }
    }
  }

  private Multimap<QualifiedName, Expression> extractNamedOutputExpressions(Select node)
  {
    // Compute aliased output terms so we can resolve order by expressions against them first
    ImmutableMultimap.Builder<QualifiedName, Expression> assignments = ImmutableMultimap.builder();
    for (SelectItem item : node.getSelectItems()) {
      if (item instanceof SingleColumn) {
        SingleColumn column = (SingleColumn) item;
        Optional<Identifier> alias = column.getAlias();
        if (alias.isPresent()) {
          assignments.put(QualifiedName.of(alias.get().getValue()), column.getExpression()); // TODO: need to know if alias was quoted
        }
        else if (column.getExpression() instanceof Identifier) {
          assignments.put(QualifiedName.of(((Identifier) column.getExpression()).getValue()), column.getExpression());
        }
      }
    }

    return assignments.build();
  }

  private class OrderByExpressionRewriter
      extends ExpressionRewriter<Void>
  {
    private final Multimap<QualifiedName, Expression> assignments;

    public OrderByExpressionRewriter(Multimap<QualifiedName, Expression> assignments)
    {
      this.assignments = assignments;
    }

    @Override
    public Expression rewriteIdentifier(Identifier reference, Void context, ExpressionTreeRewriter<Void> treeRewriter)
    {
      // if this is a simple name reference, try to resolve against output columns
      QualifiedName name = QualifiedName.of(reference.getValue());
      Set<Expression> expressions = assignments.get(name)
          .stream()
          .collect(Collectors.toSet());

      if (expressions.size() > 1) {
        throw new RuntimeException(String.format("'%s' in ORDER BY is ambiguous", name));
      }

      if (expressions.size() == 1) {
        return Iterables.getOnlyElement(expressions);
      }

      // otherwise, couldn't resolve name against output aliases, so fall through...
      return reference;
    }
  }

  public void analyzeWhere(Node node, Scope scope, Expression predicate) {
    ExpressionAnalysis expressionAnalysis = analyzeExpression(predicate, scope);

    verifyNoAggregateGroupingFunctions(metadata.getFunctionProvider(), predicate, "WHERE clause");

    analysis.recordSubqueries(node, expressionAnalysis);

    Type predicateType = expressionAnalysis.getType(predicate);
    if (!(predicateType instanceof BooleanType)) {
      throw new RuntimeException(String.format("WHERE clause must evaluate to a boolean: actual type %s", predicateType));
      // coerce null to boolean
//      analysis.addCoercion(predicate, BooleanType.INSTANCE, false);
    }

    analysis.setWhere(node, predicate);
  }

  private void verifyNoAggregateGroupingFunctions(FunctionProvider functionProvider, Expression predicate, String clause) {
    List<FunctionCall> aggregates = extractAggregateFunctions(ImmutableList.of(predicate), functionProvider);

    List<GroupingOperation> groupingOperations = extractExpressions(ImmutableList.of(predicate), GroupingOperation.class);

    List<Expression> found = ImmutableList.copyOf(Iterables.concat(
        aggregates,
        groupingOperations));

    if (!found.isEmpty()) {
      throw new RuntimeException(String.format(
          "%s cannot contain aggregations, window functions or grouping operations: %s", clause, found));
    }
  }

  private List<Expression> analyzeOrderBy(Node node, List<SortItem> sortItems, Scope orderByScope)
  {
    ImmutableList.Builder<Expression> orderByFieldsBuilder = ImmutableList.builder();

    for (SortItem item : sortItems) {
      Expression expression = item.getSortKey();

      ExpressionAnalysis expressionAnalysis = analyzeExpression(expression, orderByScope);
      analysis.recordSubqueries(node, expressionAnalysis);
      Type type = analysis.getType(expression)
          .get();
      if (!type.isOrderable()) {
        throw new RuntimeException(String.format(
            "Type %s is not orderable, and therefore cannot be used in ORDER BY: %s", type, expression));
      }

      orderByFieldsBuilder.add(expression);
    }

    List<Expression> orderByFields = orderByFieldsBuilder.build();
    return orderByFields;
  }

  public List<SortItem> getSortItemsFromOrderBy(Optional<OrderBy> orderBy) {
    return orderBy.map(OrderBy::getSortItems).orElse(ImmutableList.of());
  }

  private Scope createAndAssignScope(Node node, Scope parentScope, List<Field> fields) {
    return createAndAssignScope(node, parentScope, new RelationType(fields));
  }

  private Scope createAndAssignScope(Node node, Scope parentScope, RelationType relationType)
  {
    Scope scope = scopeBuilder(parentScope)
        .withParent(parentScope)
        .withCurrentSqmlRelation(parentScope.getCurrentSqmlRelation())
        .withRelationType(relationType)
        .build();

    analysis.setScope(node, scope);

    return scope;
  }

  private Scope.Builder scopeBuilder(Scope parentScope)
  {
    Scope.Builder scopeBuilder = Scope.builder();

    if (parentScope != null) {
      // parent scope represents local query scope hierarchy. Local query scope
      // hierarchy should have outer query scope as ancestor already.
      scopeBuilder.withParent(parentScope);
    }
//      else if (outerQueryScope.isPresent()) {
//        scopeBuilder.withOuterQueryParent(outerQueryScope.get());
//      }

    return scopeBuilder;
  }

  private Scope computeAndAssignOrderByScope(OrderBy node, Scope sourceScope, Scope outputScope)
  {
    // ORDER BY should "see" both output and FROM fields during initial analysis and non-aggregation query planning
    Scope orderByScope = Scope.builder()
        .withParent(sourceScope)
        .withCurrentSqmlRelation(sourceScope.getCurrentSqmlRelation())
        .withRelationType(outputScope.getRelation())
        .build();
    analysis.setScope(node, orderByScope);
    return orderByScope;
  }

  private Scope computeAndAssignOrderByScopeWithAggregation(OrderBy node, Scope sourceScope, Scope outputScope, List<FunctionCall> aggregations, List<Expression> groupByExpressions, List<GroupingOperation> groupingOperations)
  {
    //todo
//
//      // This scope is only used for planning. When aggregation is present then
//      // only output fields, groups and aggregation expressions should be visible from ORDER BY expression
//      ImmutableList.Builder<Expression> orderByAggregationExpressionsBuilder = ImmutableList.<Expression>builder()
//          .addAll(groupByExpressions)
//          .addAll(aggregations)
//          .addAll(groupingOperations);
//
//      // Don't add aggregate complex expressions that contains references to output column because the names would clash in TranslationMap during planning.
//      List<Expression> orderByExpressionsReferencingOutputScope = AstUtils.preOrder(node)
//          .filter(Expression.class::isInstance)
//          .map(Expression.class::cast)
//          .filter(expression -> hasReferencesToScope(expression, analysis, outputScope))
//          .collect(toImmutableList());
//      List<Expression> orderByAggregationExpressions = orderByAggregationExpressionsBuilder.build().stream()
//          .filter(expression -> !orderByExpressionsReferencingOutputScope.contains(expression) || analysis.isColumnReference(expression))
//          .collect(toImmutableList());
//
//      // generate placeholder fields
//      Set<Field> seen = new HashSet<>();
//      List<Field> orderByAggregationSourceFields = orderByAggregationExpressions.stream()
//          .map(expression -> {
//            // generate qualified placeholder field for GROUP BY expressions that are column references
//            Optional<Field> sourceField = sourceScope.tryResolveField(expression)
//                .filter(resolvedField -> seen.add(resolvedField.getField()))
//                .map(ResolvedField::getField);
//            return sourceField
//                .orElse(Field.newUnqualified(Optional.empty(), analysis.getType(expression).get()));
//          })
//          .collect(toImmutableList());
//
//      Scope orderByAggregationScope = Scope.builder()
//          .withRelationType(node, new RelationSqmlType(orderByAggregationSourceFields))
//          .build();
//
//      Scope orderByScope = Scope.builder()
//          .withParent(orderByAggregationScope)
//          .withRelationType(node, outputScope.getRelationType())
//          .build();
//      analysis.setScope(node, orderByScope);
//      analysis.setOrderByAggregates(node, orderByAggregationExpressions);
    return outputScope;
  }
  private Scope computeAndAssignOutputScope(QuerySpecification node, Scope scope,
      Scope sourceScope) {
    Builder<Field> outputFields = ImmutableList.builder();

    for (SelectItem item : node.getSelect().getSelectItems()) {
      if (item instanceof AllColumns) {
        Optional<QualifiedName> starPrefix = ((AllColumns) item).getPrefix();

        for (Field field : sourceScope.resolveFieldsWithPrefix(starPrefix)) {
          outputFields.add(Field.newUnqualified(field.getName(), field.getType()));
        }
      } else if (item instanceof SingleColumn) {
        SingleColumn column = (SingleColumn) item;

        Expression expression = column.getExpression();
        Optional<Identifier> field = column.getAlias();

        Optional<QualifiedObjectName> originTable = Optional.empty();
        Optional<String> originColumn = Optional.empty();
        QualifiedName name = null;

        if (expression instanceof Identifier) {
          name = QualifiedName.of(((Identifier) expression).getValue());
        }
        else if (expression instanceof DereferenceExpression) {
          name = DereferenceExpression.getQualifiedName((DereferenceExpression) expression);
        }

        if (name != null) {
//            List<Field> matchingFields = sourceScope.resolveFields(name);
//            if (!matchingFields.isEmpty()) {
//              originTable = matchingFields.get(0).getOriginTable();
//              originColumn = matchingFields.get(0).getOriginColumnName();
//            }
        }

        if (field.isEmpty()) {
          if (name != null) {
            field = Optional.of(new Identifier(getLast(name.getOriginalParts())));
          }
        }

        outputFields.add(
            Field.newUnqualified(field.map(Identifier::getValue),
            analysis.getType(expression).orElseThrow(),
            column.getAlias().isPresent())
        );
      }
      else {
        throw new IllegalArgumentException("Unsupported SelectItem type: " + item.getClass().getName());
      }
    }

    return createAndAssignScope(node, scope, new ArrayList<>(outputFields.build()));
  }

  private void analyzeHaving(QuerySpecification node, Scope scope) {
    if (node.getHaving().isPresent()) {
      Expression predicate = node.getHaving().get();

      ExpressionAnalysis expressionAnalysis = analyzeExpression(predicate, scope);


      analysis.recordSubqueries(node, expressionAnalysis);

      Type predicateType = expressionAnalysis.getType(predicate);
      if (!(predicateType instanceof BooleanType)) {
        throw new RuntimeException(String.format("HAVING clause must evaluate to a boolean: actual type %s", predicateType));
      }

      analysis.setHaving(node, predicate);
    }
  }

  private List<Expression> analyzeSelect(QuerySpecification node, Scope scope) {
    List<Expression> outputExpressions = new ArrayList<>();

    for (SelectItem item : node.getSelect().getSelectItems()) {
      if (item instanceof AllColumns) {
        Optional<QualifiedName> starPrefix = ((AllColumns) item).getPrefix();

        List<Field> fields = scope.resolveFieldsWithPrefix(starPrefix);
        if (fields.isEmpty()) {
          if (starPrefix.isPresent()) {
            throw new RuntimeException(String.format("Table '%s' not found", starPrefix.get()));
          }
          throw new RuntimeException(String.format("SELECT * not allowed from relation that has no columns"));
        }
        for (Field field : fields) {
//            int fieldIndex = scope.getRelation().indexOf(field);
//            FieldReference expression = new FieldReference(fieldIndex);
//            outputExpressions.add(expression);
//            ExpressionAnalysis expressionAnalysis = analyzeExpression(expression, scope);

          if (node.getSelect().isDistinct() && !field.getType().isComparable()) {
            throw new RuntimeException(String.format("DISTINCT can only be applied to comparable types (actual: %s)", field.getType()));
          }
        }
      } else if (item instanceof SingleColumn) {
        SingleColumn column = (SingleColumn) item;
        ExpressionAnalysis expressionAnalysis = analyzeExpression(column.getExpression(), scope);

        analysis.recordSubqueries(node, expressionAnalysis);
        outputExpressions.add(column.getExpression());

        Type type = expressionAnalysis.getType(column.getExpression());
        if (node.getSelect().isDistinct() && !type.isComparable()) {
          throw new RuntimeException(String.format("DISTINCT can only be applied to comparable types (actual: %s): %s", type, column.getExpression()));
        }
      }
      else {
        throw new IllegalArgumentException(String.format("Unsupported SelectItem type: %s", item.getClass().getName()));
      }
    }

    analysis.setOutputExpressions(node, outputExpressions);

    return outputExpressions;
  }

  public RelationType join(RelationType left, RelationType right) {
    List<Field> joinFields = new ArrayList<>();
    joinFields.addAll(left.getFields());
    joinFields.addAll(right.getFields());
    return new RelationType(joinFields);
  }

  public RelationType withAlias(RelationType rel, String relationAlias) {
    ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
    for (int i = 0; i < rel.getFields().size(); i++) {
      Field field = ((Field)rel.getFields().get(i));
//      Optional<String> columnAlias = field.getName();
      fieldsBuilder.add(Field.newQualified(
          relationAlias,
          null,
          field.getType()));
    }

    return new RelationType(fieldsBuilder.build());
  }

  private List<Expression> analyzeGroupBy(QuerySpecification node, Scope scope, List<Expression> outputExpressions)
  {
    if (node.getGroupBy().isPresent()) {
      List<List<Set<FieldId>>> sets = new ArrayList();
      List<Expression> groupingExpressions = new ArrayList();

      for (GroupingElement groupingElement : node.getGroupBy().get().getGroupingElements()) {
        for (Expression column : groupingElement.getExpressions()) {
          if (column instanceof LongLiteral) {
            throw new RuntimeException("Ordinals not supported in group by statements");
          }
          //Group by statement must be one of the select fields
          if (!(column instanceof Identifier)) {
            log.info(String.format("GROUP BY statement should use column aliases instead of expressions. %s", column));
            analyzeExpression(column, scope);
            outputExpressions.stream()
                .filter(e->e.equals(column))
                .findAny()
                .orElseThrow(()->new RuntimeException(String.format("SELECT should contain GROUP BY expression %s", column)));
            groupingExpressions.add(column);
          } else {
            Expression rewrittenGroupByExpression = ExpressionTreeRewriter.rewriteWith(
                new OrderByExpressionRewriter(extractNamedOutputExpressions(node.getSelect())), column);
            int index = outputExpressions.indexOf(rewrittenGroupByExpression);
            if (index == -1) {
              throw new RuntimeException(String.format("SELECT should contain GROUP BY expression %s", column));
            }
            groupingExpressions.add(outputExpressions.get(index));
          }
        }
      }

      List<Expression> expressions = groupingExpressions;
      for (Expression expression : expressions) {
        Type type = analysis.getType(expression)
            .get();
        if (!type.isComparable()) {
          throw new RuntimeException(String.format("%s is not comparable, and therefore cannot be used in GROUP BY", type));
        }
      }

      analysis.setGroupByExpressions(node, groupingExpressions);

      return groupingExpressions;
    }

    return ImmutableList.of();
  }

  private ExpressionAnalysis analyzeExpression(Expression expression, Scope scope) {
    ExpressionAnalyzer analyzer = new ExpressionAnalyzer(metadata);
    ExpressionAnalysis exprAnalysis = analyzer.analyze(expression, scope);

    analysis.addCoercions(exprAnalysis.getExpressionCoercions(),
        exprAnalysis.getTypeOnlyCoercions());
    analysis.addTypes(exprAnalysis.getExpressionTypes());
    analysis.addSourceScopedFields(exprAnalysis.getSourceScopedFields());

    return exprAnalysis;
  }
//  }
}
