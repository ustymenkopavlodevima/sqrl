package ai.dataeng.sqml.execution.flink.process;

import ai.dataeng.sqml.io.sources.SourceRecord;
import ai.dataeng.sqml.planner.Column;
import ai.dataeng.sqml.planner.Table;
import ai.dataeng.sqml.type.basic.BasicType;
import ai.dataeng.sqml.type.basic.DateTimeType;
import ai.dataeng.sqml.type.basic.IntegerType;
import ai.dataeng.sqml.type.basic.UuidType;
import ai.dataeng.sqml.type.constraint.NotNull;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.tree.name.NamePath;
import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public interface FieldProjection extends Serializable {

    int getDepth();

    default Object getData(SourceRecord<Name> record) {
        return getData(record.getData());
    }

    Object getData(Map<Name, Object> data);

    abstract class SpecialCase implements FieldProjection {

        private final Name name;
        private final BasicType type;
        private final boolean isPrimary;
        private final Optional<Column> parent;

        protected SpecialCase(String name, BasicType type, boolean isPrimary,
            Optional<Column> parent) {
            this.name = Name.hidden(name);
            this.type = type;
            this.isPrimary = isPrimary;
            this.parent = parent;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        public Name getName() {
            return name;
        }

        public Column createColumn(Table table) {
            return new Column(getName(),table,0,type,0,
                    Collections.singletonList(NotNull.INSTANCE),isPrimary, parent.isPresent(), parent, false);
        }

        @Override
        public abstract Object getData(SourceRecord<Name> record);

        @Override
        public Object getData(Map<Name, Object> data) {
            throw new UnsupportedOperationException();
        }
    }

    public static SpecialCase foreignKey(Column parent) {
        return new SpecialCase("uuid", UuidType.INSTANCE, true, Optional.of(parent)){
            @Override
            public Object getData(SourceRecord<Name> record) {
                return record.getUuid().toString();
            }
        };
    }

    SpecialCase ROOT_UUID = new SpecialCase("uuid", UuidType.INSTANCE, true, Optional.empty()) {
        @Override
        public Object getData(SourceRecord<Name> record) {
            return record.getUuid().toString();
        }
    };

    SpecialCase INGEST_TIMESTAMP = new SpecialCase("ingest_time", DateTimeType.INSTANCE, false,
        Optional.empty()) {
        @Override
        public Object getData(SourceRecord<Name> record) {
            return record.getIngestTime();
        }
    };

    SpecialCase SOURCE_TIMESTAMP = new SpecialCase("source_time", DateTimeType.INSTANCE, false,
        Optional.empty()) {
        @Override
        public Object getData(SourceRecord<Name> record) {
            return record.getSourceTime();
        }
    };

    @Getter
    public static class ArrayIndex extends SpecialCase {

        private final int depth;

        public ArrayIndex(int depth, Optional<Column> parent) {
            super("idx"+depth, IntegerType.INSTANCE, true, parent);
            this.depth = depth;
        }

        @Override
        public Object getData(SourceRecord<Name> record) {
            throw new UnsupportedOperationException();
        }
    }

    class NamePathProjection implements FieldProjection {

        /**
         * The named path is starts at the depth and is relative
         */
        private final NamePath path;
        private final int depth;

        public NamePathProjection(NamePath path, int depth) {
            Preconditions.checkArgument(path.getLength()>0);
            this.path = path;
            this.depth = depth;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public Object getData(Map<Name, Object> data) {
            Map<Name, Object> base = data;
            for (int i = 0; i < path.getLength()-2; i++) {
                Object map = base.get(path.get(i));
                Preconditions.checkArgument(map instanceof Map, "Illegal field projection");
                base = (Map)map;
            }
            return base.get(path.getLast());
        }
    }

}