package ai.dataeng.sqml.schema2.basic;

import ai.dataeng.sqml.schema2.Type;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BasicType<JavaType> extends Type {

    String getName();

    BasicType parentType();

    TypeConversion<JavaType> conversion();

    //TODO: replace by discovery pattern so that new datatype can be registered
    public static final BasicType[] ALL_TYPES = {BooleanType.INSTANCE, StringType.INSTANCE,
                                                 NumberType.INSTANCE, IntegerType.INSTANCE, FloatType.INSTANCE,
                                                 DateTimeType.INSTANCE, UuidType.INSTANCE
    };

    public static final BasicType ROOT_TYPE = StringType.INSTANCE;

    public static BasicType combine(@NonNull BasicType t1, @NonNull BasicType t2, boolean forced) {
        Set<BasicType> visitedTypes = new HashSet<>();
        visitedTypes.add(t2);
        BasicType parent = t1;
        while (parent!=null) {
            if (visitedTypes.contains(parent)) return parent;
            else visitedTypes.add(parent);
            parent = parent.parentType();
        }
        parent = t2.parentType();
        while (parent!=null) {
            if (visitedTypes.contains(parent)) return parent;
            parent = parent.parentType();
        }
        if (forced) return ROOT_TYPE; //coerce casting to the root type
        else return null;
    }

    public static BasicType combine(@NonNull BasicType t1, @NonNull BasicType t2) {
        return combine(t1,t2,false);
    }


//    public static final Multimap<BasicType,BasicType> TYPE_TREE =

    public static final Map<String,BasicType> ALL_TYPES_BY_NAME = Arrays.stream(ALL_TYPES)
            .collect(Collectors.toUnmodifiableMap(t -> t.getName().trim().toLowerCase(Locale.ENGLISH), Function.identity()));

    public static final Map<Class,BasicType> JAVA_TO_TYPE = Arrays.stream(ALL_TYPES).flatMap(t -> {
        Stream<Class> classes = t.conversion().getJavaTypes().stream();
        Stream<Pair<Class,BasicType>> s = classes.map(c -> new ImmutablePair<Class,BasicType>(c,t));
        return s;
    }).collect(Collectors.toUnmodifiableMap(Pair::getKey, Pair::getValue));

    public static BasicType getTypeByName(String name) {
        return ALL_TYPES_BY_NAME.get(name.trim().toLowerCase(Locale.ENGLISH));
    }

    public static BasicType inferType(Map<String,Object> originalComposite) {
        for (BasicType type : ALL_TYPES) {
            if (type.conversion().isInferredType(originalComposite)) return type;
        }
        return null;
    }

    public static BasicType inferType(String original) {
        for (BasicType type : ALL_TYPES) {
            if (type.conversion().isInferredType(original)) return type;
        }
        return null;
    }


}