/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.dataeng.sqml.parser.validator;

import ai.dataeng.sqml.tree.Expression;
import ai.dataeng.sqml.tree.Node;
import ai.dataeng.sqml.tree.NodeRef;
import ai.dataeng.sqml.util.AstUtils;
import com.google.common.collect.Multimap;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Extract expressions that are references to a given scope.
 */
class ScopeReferenceExtractor
{
    private ScopeReferenceExtractor() {}

    public static boolean hasReferencesToScope(Node node, StatementAnalysis analysis, Scope scope)
    {
        return getReferencesToScope(node, analysis, scope).findAny().isPresent();
    }

    public static Stream<Expression> getReferencesToScope(Node node, StatementAnalysis analysis, Scope scope)
    {
        Multimap<NodeRef<Expression>, FieldId> columnReferences = analysis.getColumnReferenceFields();

        return AstUtils.preOrder(node)
                .filter(Expression.class::isInstance)
                .map(Expression.class::cast)
                .filter(expression -> columnReferences.containsKey(NodeRef.of(expression)))
                .filter(expression -> hasReferenceToScope(expression, scope, columnReferences));
    }

    private static boolean hasReferenceToScope(Expression node, Scope scope, Multimap<NodeRef<Expression>, FieldId> columnReferences)
    {
        return columnReferences.get(NodeRef.of(node)).stream().anyMatch(fieldId -> isFieldFromScope(fieldId, scope));
    }

    public static boolean isFieldFromScope(FieldId fieldId, Scope scope)
    {
        return Objects.equals(fieldId.getRelationId(), scope.getRelationId());
    }
}