package ai.dataeng.sqml.type.basic;

import ai.dataeng.sqml.type.SqmlTypeVisitor;
import java.time.OffsetDateTime;
import java.util.function.Function;

public class DateTimeType extends SimpleBasicType<OffsetDateTime> {

    public static final DateTimeType INSTANCE = new DateTimeType();

    @Override
    public String getName() {
        return "DATETIME";
    }

    @Override
    protected Class<OffsetDateTime> getJavaClass() {
        return OffsetDateTime.class;
    }

    @Override
    protected Function<String, OffsetDateTime> getStringParser() {
        return s -> OffsetDateTime.parse(s);
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
        return visitor.visitDateTimeType(this, context);
    }
}