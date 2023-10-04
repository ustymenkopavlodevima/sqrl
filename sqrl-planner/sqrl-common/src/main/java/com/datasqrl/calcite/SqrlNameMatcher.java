package com.datasqrl.calcite;

import com.datasqrl.canonicalizer.Name;
import com.datasqrl.canonicalizer.NameCanonicalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.validate.SqlNameMatcher;
import org.apache.calcite.sql.validate.SqlNameMatchers;

/**
 * Allows for choosing the most recent canonicalize column in a table.
 * e.g. NAME -> name$1
 */
@AllArgsConstructor
public class SqrlNameMatcher implements SqlNameMatcher {
  NameCanonicalizer canonicalizer;

  final SqlNameMatcher delegate = SqlNameMatchers.withCaseSensitive(false);

  @Override
  public boolean isCaseSensitive() {
    return false;
  }

  @Override
  public boolean matches(String s, String s1) {
    return delegate.matches(s, s1);
  }

  @Override
  public <K extends List<String>, V> V get(Map<K, V> map, List<String> list, List<String> list1) {
    return delegate.get(map, list, list1);
  }

  @Override
  public String bestString() {
    return delegate.bestString();
  }

  @Override
  public RelDataTypeField field(RelDataType relDataType, String s) {
    RelDataTypeField f = relDataType.getField(s, false, false);
    if (f != null) {
      return f;
    }

    String name = getLatestVersion(canonicalizer, relDataType.getFieldNames(), s);
    if (name == null) {
      return null;
    }
    int i = indexOf(relDataType.getFieldNames(), name);
    return relDataType.getFieldList().get(i);
  }

  @Override
  public int indexOf(Iterable<String> names, String name) {
    List<String> n = new ArrayList<>();
    names.iterator().forEachRemaining(n::add);

    List<Name> names1 = n.stream().map(na->canonicalizer.name(na))
        .collect(Collectors.toList());

    if (names1.contains(canonicalizer.name(name))) {
      return n.indexOf(name);
    }

    String versioned = getLatestVersion(canonicalizer, n, name);

    return n.indexOf(versioned);
  }

  @Override
  public int frequency(Iterable<String> iterable, String s) {
    return delegate.frequency(iterable, s);
  }

  @Override
  public Set<String> createSet() {
    return delegate.createSet();
  }


  public static String getLatestVersion(NameCanonicalizer canonicalizer, Collection<String> functionNames, String prefix) {
    //todo: use name comparator
    Set<String> functionNamesCanon = functionNames.stream()
        .map(f-> canonicalizer.name(f).getCanonical())
        .collect(Collectors.toSet());
    String prefixCanon = canonicalizer.name(prefix).getCanonical();

    Pattern pattern = Pattern.compile("^"+Pattern.quote(prefixCanon) + "\\$(\\d+)");
    int maxVersion = -1;

    String name = null;
    for (String function : functionNamesCanon) {
      Matcher matcher = pattern.matcher(function);
      if (matcher.find()) {
        int version = Integer.parseInt(matcher.group(1));
        maxVersion = Math.max(maxVersion, version);
        if (maxVersion == version) {
          name = matcher.group();
        }
      }
    }

    return maxVersion != -1 ? name : null;
  }
}