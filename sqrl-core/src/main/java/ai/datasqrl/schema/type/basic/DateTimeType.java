package ai.datasqrl.schema.type.basic;

import ai.datasqrl.schema.type.SqmlTypeVisitor;
import java.time.Instant;
import java.util.function.Function;

public class DateTimeType extends SimpleBasicType<Instant> {

    public static final DateTimeType INSTANCE = new DateTimeType();

    @Override
    public String getName() {
        return "DATETIME";
    }

    @Override
    protected Class<Instant> getJavaClass() {
        return Instant.class;
    }

    @Override
    protected Function<String, Instant> getStringParser() {
        return s -> Instant.parse(s);
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
        return visitor.visitDateTimeType(this, context);
    }
}