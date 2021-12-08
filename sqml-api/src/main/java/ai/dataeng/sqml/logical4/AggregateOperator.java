package ai.dataeng.sqml.logical4;

import ai.dataeng.sqml.function.FunctionHandle;

import java.util.List;
import java.util.Map;

import lombok.Value;

/**
 * Aggregates incoming records by {@link AggregateOperator#groupByKeys} and computes the provided aggregate functions
 * {@link AggregateOperator#aggregates} for each group.
 * Emits a row on each group update. The row contains the groupByKeys and one column for each aggregate function to contain its aggregate value.
 */
@Value
public class AggregateOperator extends LogicalPlan.RowNode<LogicalPlan.RowNode> {

    final LogicalPlan.Column[] groupByKeys;
    final Map<LogicalPlan.Column, Aggregation> aggregates;
    final LogicalPlan.Column[] schema;

    public AggregateOperator(LogicalPlan.RowNode input, LogicalPlan.Column[] groupByKeys, Map<LogicalPlan.Column, Aggregation> aggregates) {
        super(input);
        this.groupByKeys = groupByKeys;
        this.aggregates = aggregates;
        schema = new LogicalPlan.Column[groupByKeys.length + aggregates.size()];
        int offset = groupByKeys.length;
        System.arraycopy(groupByKeys,0, schema, 0, offset);
        for (LogicalPlan.Column col : aggregates.keySet()) {
            schema[offset++] = col;
        }
    }

    @Override
    public LogicalPlan.Column[][] getOutputSchema() {
        return new LogicalPlan.Column[][]{schema};
    }

    @Value
    public static class Aggregation {

        FunctionHandle functionHandle;
        List<LogicalPlan.Column> arguments;

    }
}