package ai.datasqrl.plan.nodes;

import ai.datasqrl.parse.tree.name.Name;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.plan.Context;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptSchema;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.prepare.RelOptTableImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.tools.RelBuilder;
import org.apache.flink.table.planner.plan.nodes.calcite.LogicalWatermarkAssigner;

public class SqrlRelBuilder extends RelBuilder {

  public SqrlRelBuilder(Context context,
      RelOptCluster cluster,
      RelOptSchema relOptSchema) {
    super(context, cluster, relOptSchema);
  }

  public SqrlRelBuilder scanStream(Name tableName, RelDataType toRelDataType) {
    RelOptTable table = RelOptTableImpl.create(relOptSchema, toRelDataType,
        List.of("default_catalog", "default_database", tableName.getCanonical()), null);
    RelTraitSet traits = RelTraitSet.createEmpty();
    traits = traits.plus(Convention.NONE);

    RelNode scan = new LogicalTableScan(this.cluster, traits, List.of(),
        table);
    this.push(scan);
    return this;
  }

  /**
   * Project by index
   */
  public SqrlRelBuilder project(List<Integer> index) {
    List<RexInputRef> indexes = index.stream()
        .map(i-> RexInputRef.of(i, this.peek().getRowType()))
        .collect(Collectors.toList());

    this.project(indexes);

    return this;
  }

  public SqrlRelBuilder watermark(int i) {
    RexNode watermarkExpr = getRexBuilder().makeCall(SqlStdOperatorTable.MINUS,
        List.of(RexInputRef.of(i, peek().getRowType()), getRexBuilder().makeIntervalLiteral(
            BigDecimal.valueOf(10000),
            new SqlIntervalQualifier(TimeUnit.SECOND, TimeUnit.SECOND, SqlParserPos.ZERO))));

    this.push(LogicalWatermarkAssigner
        .create(cluster, build(), i, watermarkExpr));

    return this;
  }
}