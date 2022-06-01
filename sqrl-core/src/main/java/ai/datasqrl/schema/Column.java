package ai.datasqrl.schema;

import ai.datasqrl.parse.tree.name.Name;
import ai.datasqrl.schema.type.constraint.Constraint;
import ai.datasqrl.schema.type.constraint.ConstraintHelper;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.rel.type.RelDataTypeField;

@Getter
public class Column extends Field {

  @Setter
  private Table table;
  /* Identity of the column in addition to name for shadowed columns which we need to keep
     on the table even though it is no longer referenceable since another column may depend on it
     (unlike relationships which can be ignored once they are shadowed and no longer referenceable)
   */
  public int version;

  private final boolean isPrimaryKey;
  private final boolean isTimestamp;


  public final int arrayDepth;
  public final boolean nonNull;
  public final List<Constraint> constraints;
  private final RelDataTypeField relDataTypeField;
  private final Set<Attribute> attributes; //TODO: what are those for?

  //System information
  public boolean isInternal;

  public Column(Name name, Table table, int version,
      int arrayDepth, List<Constraint> constraints,
      boolean isInternal, boolean isPrimaryKey, //boolean isTimestamp,
      RelDataTypeField relDataTypeField, Set<Attribute> attributes) {
    super(name);
    this.table = table;
    this.version = version;
    this.arrayDepth = arrayDepth;
    this.constraints = constraints;
    this.isPrimaryKey = isPrimaryKey;
    this.isTimestamp = false;
    this.relDataTypeField = relDataTypeField;
    this.isInternal = isInternal;
    this.nonNull = ConstraintHelper.isNonNull(constraints);
    this.attributes = attributes;
  }

  //Returns a calcite name for this column.
  public Name getId() {
    return Name.system(relDataTypeField.getName());
  }

  @Override
  public boolean isVisible() {
    return !isInternal;
  }

  public boolean containsAttribute(Class clazz) {
    for (Attribute attribute : attributes) {
      if (attribute.getClass().isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "Column{" +
        "version=" + version +
        ", arrayDepth=" + arrayDepth +
        ", nonNull=" + nonNull +
        ", constraints=" + constraints +
        ", isPrimaryKey=" + isPrimaryKey +
        ", isTimestamp =" + isTimestamp +
        ", relDataTypeField=" + relDataTypeField +
        ", attributes=" + attributes +
        ", isInternal=" + isInternal +
        ", name=" + name +
        '}';
  }
}
