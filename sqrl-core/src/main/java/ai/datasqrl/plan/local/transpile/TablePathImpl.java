package ai.datasqrl.plan.local.transpile;

import ai.datasqrl.schema.Relationship;
import ai.datasqrl.schema.SQRLTable;
import java.util.List;
import java.util.Optional;
import lombok.Value;

@Value
public class TablePathImpl implements TablePath {
  SQRLTable baseTable;
  Optional<String> baseAlias;

  boolean relative;

  List<Relationship> relationships;
  String alias;

  public int size() {
    return relationships.size();
  }

  public Relationship getRelationship(int i) {
    return relationships.get(i);
  }
}