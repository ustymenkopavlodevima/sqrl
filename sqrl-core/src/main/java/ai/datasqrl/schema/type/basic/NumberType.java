package ai.datasqrl.schema.type.basic;

import ai.datasqrl.schema.type.SqmlTypeVisitor;
import java.util.Collections;
import java.util.Set;

public class NumberType extends AbstractBasicType<Float> {

    public static final NumberType INSTANCE = new NumberType();

    @Override
    public String getName() {
        return "NUMBER";
    }

    @Override
    public TypeConversion<Float> conversion() {
        return new Conversion();
    }

    public static class Conversion implements TypeConversion<Float> {

        public Conversion() {
        }

        @Override
        public Set<Class> getJavaTypes() {
            return Collections.EMPTY_SET;
        }

        public Float convert(Object o) {
            return FloatType.Conversion.convertInternal(o);
        }
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
        return visitor.visitNumberType(this, context);
    }
}