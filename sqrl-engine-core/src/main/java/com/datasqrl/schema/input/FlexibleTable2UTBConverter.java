package com.datasqrl.schema.input;

import com.datasqrl.name.Name;
import com.datasqrl.name.NamePath;
import com.datasqrl.plan.calcite.SqrlTypeRelDataTypeConverter;
import com.datasqrl.schema.Multiplicity;
import com.datasqrl.schema.UniversalTableBuilder;
import com.datasqrl.schema.type.Type;
import lombok.AllArgsConstructor;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;

import java.util.ArrayDeque;
import java.util.Deque;

@AllArgsConstructor
public class FlexibleTable2UTBConverter implements FlexibleTableConverter.Visitor<UniversalTableBuilder> {

    private final UniversalTableBuilder.ImportFactory tableFactory;
    private final SqrlTypeRelDataTypeConverter typeConverter;
    private final Deque<UniversalTableBuilder> stack = new ArrayDeque<>();

    public FlexibleTable2UTBConverter() {
        this(new JavaTypeFactoryImpl());
    }

    public FlexibleTable2UTBConverter(RelDataTypeFactory typeFactory) {
        this.tableFactory = new UniversalTableBuilder.ImportFactory(typeFactory, true);
        this.typeConverter = new SqrlTypeRelDataTypeConverter(typeFactory);
    }

    @Override
    public void beginTable(Name name, NamePath namePath, boolean isNested, boolean isSingleton, boolean hasSourceTimestamp) {
        if (isNested) {
            assert stack.getFirst()!=null;
            stack.addFirst(tableFactory.createTable(name,namePath.concat(name), stack.getFirst(), isSingleton));
        } else {
            stack.addFirst(tableFactory.createTable(name,namePath.concat(name), hasSourceTimestamp));
        }
    }

    @Override
    public UniversalTableBuilder endTable(Name name, NamePath namePath, boolean isNested, boolean isSingleton) {
        return stack.removeFirst();
    }

    @Override
    public void addField(Name name, Type type, boolean nullable) {
        UniversalTableBuilder tblBuilder = stack.getFirst();
        tblBuilder.addColumn(name, tableFactory.withNullable(type.accept(typeConverter,null), nullable));
    }

    @Override
    public void addField(Name name, UniversalTableBuilder nestedTable, boolean nullable, boolean isSingleton) {
        UniversalTableBuilder tblBuilder = stack.getFirst();
        Multiplicity multi = Multiplicity.ZERO_ONE;
        if (!isSingleton) multi = Multiplicity.MANY;
        else if (!nullable) multi = Multiplicity.ONE;
        tblBuilder.addChild(name,nestedTable,multi);
    }

}