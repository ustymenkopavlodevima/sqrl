package ai.datasqrl.plan.local.transpile;

import ai.datasqrl.schema.Relationship;
import java.util.Optional;

public interface JoinBuilder {

  void addBaseTable(TablePath path, Optional<String> lastAlias);

  void addFirstRel(Relationship rel, String baseAlias, Optional<String> lastAlias);

  void append(Relationship rel, Optional<String> lastAlias);

  SqlJoinDeclaration build();
}