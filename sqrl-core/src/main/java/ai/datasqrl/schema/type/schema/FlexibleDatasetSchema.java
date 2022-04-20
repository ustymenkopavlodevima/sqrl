package ai.datasqrl.schema.type.schema;

import ai.datasqrl.parse.tree.name.Name;
import ai.datasqrl.schema.type.RelationType;
import ai.datasqrl.schema.type.Type;
import ai.datasqrl.schema.type.constraint.Constraint;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
public class FlexibleDatasetSchema extends RelationType<FlexibleDatasetSchema.TableField> {

    public static final FlexibleDatasetSchema EMPTY = new FlexibleDatasetSchema(Collections.EMPTY_LIST, SchemaElementDescription.NONE);

    @NonNull
    private final SchemaElementDescription description;

    private FlexibleDatasetSchema(@NonNull List<TableField> fields, @NonNull SchemaElementDescription description) {
        super(fields);
        this.description = description;
    }

    @Setter
    public static class Builder extends RelationType.AbstractBuilder<FlexibleDatasetSchema.TableField, Builder> {

        private SchemaElementDescription description = SchemaElementDescription.NONE;

        public Builder() {
            super(true);
        }

        public FlexibleDatasetSchema build() {
            return new FlexibleDatasetSchema(fields, description);
        }

    }

    @Getter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static abstract class AbstractField implements SchemaField {

        @NonNull
        private Name name;
        @NonNull
        private SchemaElementDescription description;
        private Object default_value;

        @Setter
        public static abstract class Builder {

            protected Name name;
            protected SchemaElementDescription description = SchemaElementDescription.NONE;
            protected Object default_value;

            public void copyFrom(AbstractField f) {
                name = f.name;
                description = f.description;
                default_value = f.default_value;
            }

        }
    }

    @Getter
    @ToString
    @NoArgsConstructor
    public static class TableField extends AbstractField {

        private boolean isPartialSchema;
        @NonNull
        private RelationType<FlexibleField> fields;
        @NonNull
        private List<Constraint> constraints;

        public TableField(Name name, SchemaElementDescription description, Object default_value,
                          boolean isPartialSchema, RelationType<FlexibleField> fields, List<Constraint> constraints) {
            super(name,description,default_value);
            this.isPartialSchema = isPartialSchema;
            this.fields = fields;
            this.constraints = constraints;
        }

        @Setter
        public static class Builder extends AbstractField.Builder {

            protected boolean isPartialSchema = true;
            protected RelationType<FlexibleField> fields;
            protected List<Constraint> constraints = Collections.EMPTY_LIST;

            public void copyFrom(TableField f) {
                super.copyFrom(f);
                isPartialSchema = f.isPartialSchema;
                fields = f.fields;
                constraints = f.constraints;
            }

            public TableField build() {
                return new TableField(name,description,default_value, isPartialSchema, fields, constraints);
            }

        }

        public static TableField empty(Name name) {
            Builder b = new Builder();
            b.setName(name);
            b.setFields(RelationType.EMPTY);
            return b.build();
        }
    }

    @Getter
    @ToString
    @NoArgsConstructor
    public static class FlexibleField extends AbstractField implements SchemaField {

        @NonNull
        private List<FieldType> types;

        public FlexibleField(Name name, SchemaElementDescription description, Object default_value,
                             List<FieldType> types) {
            super(name, description, default_value);
            this.types = types;
        }

        @Setter
        public static class Builder extends AbstractField.Builder {

            protected List<FieldType> types;

            public void copyFrom(FlexibleField f) {
                super.copyFrom(f);
                types = f.types;
            }

            public FlexibleField build() {
                return new FlexibleField(name,description,default_value, types);
            }

        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class FieldType implements Serializable {

        @NonNull
        private Name variantName;

        @NonNull
        private Type type;
        private int arrayDepth;

        @NonNull
        private List<Constraint> constraints;

    }


}