package ai.datasqrl.validate.scopes;

import ai.datasqrl.schema.Table;
import ai.datasqrl.validate.Namespace;
import lombok.Value;

public class ExpressionScope implements ValidatorScope {

  @Override
  public Namespace getNamespace() {
    return null;
  }
}
