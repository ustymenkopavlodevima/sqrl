package ai.dataeng.sqml.schema2.name;

import java.io.Serializable;
import java.util.Locale;

/**
 * Produces the canonical version of a field name
 */
@FunctionalInterface
public interface NameCanonicalizer extends Serializable {

    String getCanonical(String name);

    NameCanonicalizer LOWERCASE_ENGLISH = name -> name.trim().toLowerCase(Locale.ENGLISH);

    NameCanonicalizer AS_IS = name -> name.trim();

    NameCanonicalizer SYSTEM = LOWERCASE_ENGLISH;

}
