package org.apache.calcite.schema;

import ai.datasqrl.parse.tree.name.Name;
import ai.datasqrl.plan.nodes.RelNodeTable;
import ai.datasqrl.schema.Relationship;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public class SqrlCalciteSchema extends AbstractSqrlSchema {
  ai.datasqrl.schema.Schema schema;
  @Override
  public Table getTable(String s) {
    Pair<Set<Relationship>, Set<ai.datasqrl.schema.Table>> rels = getAllTables(schema);
    for (ai.datasqrl.schema.Table table : rels.getRight()) {
      if (table.getId().equals(Name.system(s))) {
        Statistic stat = Statistics.of(table.getStatistic().getRowCount(), List.of(getPrimaryKey(table)));
        return new RelNodeTable(table.getHead(), stat);
      }
    }

    throw new RuntimeException("Could not resolve table " + s);
  }

  public static ImmutableBitSet getPrimaryKey(ai.datasqrl.schema.Table table) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public static Pair<Set<Relationship>, Set<ai.datasqrl.schema.Table>> getAllTables(ai.datasqrl.schema.Schema schema) {
    final Set<ai.datasqrl.schema.Table> included = new HashSet<>();
    final Set<ai.datasqrl.schema.Table> toInclude = new HashSet<>();
    final Set<ai.datasqrl.schema.Relationship> toIncludeRel = new HashSet<>();

    for (ai.datasqrl.schema.Table table : schema.visibleList()) {
      toInclude.add(table);
    }

    while (!toInclude.isEmpty()) {
      ai.datasqrl.schema.Table next = toInclude.iterator().next();
      assert !included.contains(next);
      included.add(next);
      toInclude.remove(next);
      //Find all non-hidden related tables and add those
      next.getFields().visibleStream().filter(f -> f instanceof Relationship)
          .map(f -> (Relationship)f)
          .forEach(r -> {
            if (!included.contains(r.getToTable())) {
              toInclude.add(r.getToTable());
            }
          });
    }
    return Pair.of(toIncludeRel, included);
  }
}
