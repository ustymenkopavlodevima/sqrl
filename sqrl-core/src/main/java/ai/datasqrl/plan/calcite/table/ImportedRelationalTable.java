package ai.datasqrl.plan.calcite.table;

import ai.datasqrl.environment.ImportManager.SourceTableImport;
import ai.datasqrl.parse.tree.name.Name;
import lombok.Getter;
import lombok.NonNull;
import org.apache.calcite.rel.RelNode;

/**
 * A relational table that is defined by a {@link SourceTableImport}, i.e. the imported data from a
 * {@link ai.datasqrl.io.sources.dataset.SourceTable} in a {@link ai.datasqrl.io.sources.dataset.SourceDataset}.
 *
 * This is a phyiscal relation with a schema that captures the input data.
 */
public class ImportedRelationalTable extends QueryRelationalTable {

  @Getter
  private final SourceTableImport sourceTableImport;

  public ImportedRelationalTable(@NonNull Name rootTableId, @NonNull TimestampHolder.Base timestamp,
                                 SourceTableImport sourceTableImport, RelNode relNode) {
    super(rootTableId, Type.STREAM, relNode, timestamp, 1);
    this.sourceTableImport = sourceTableImport;
  }

}