package ai.datasqrl.parse;

import ai.datasqrl.config.error.ErrorCode;
import lombok.Getter;
import org.apache.calcite.sql.parser.SqlParserPos;

@Getter
public class SqrlAstException extends RuntimeException {

  private final ErrorCode errorCode;
  private final SqlParserPos pos;
  private final String message;

  public SqrlAstException(ErrorCode errorCode, SqlParserPos pos, String message) {
    super(message);
    this.errorCode = errorCode;
    this.pos = pos;
    this.message = message;
  }
}