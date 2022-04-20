package ai.datasqrl.parse;

import ai.datasqrl.config.error.ErrorCollector;
import ai.datasqrl.config.error.ErrorMessage;
import ai.datasqrl.parse.tree.ScriptNode;
import ai.datasqrl.parse.ParsingOptions.DecimalLiteralTreatment;
import ai.datasqrl.parse.tree.SqrlStatement;

/**
 * Pasers scripts or statements
 */
public class SqrlParser {

  private final SqlParser parser;
  private final ParsingOptions parsingOptions;

  public SqrlParser(ErrorCollector errorCollector) {
    parser = new SqlParser(new SqlParserOptions());
    parsingOptions = ParsingOptions.builder()
        .setWarningConsumer((e)->
            errorCollector.add(new ErrorMessage.Implementation(e.getWarning(), null, null)))
        .setDecimalLiteralTreatment(DecimalLiteralTreatment.AS_DOUBLE)
        .build();
  }

  public ScriptNode parse(String script) {
    return parser.createScript(script, parsingOptions);
  }

  public static SqrlParser newParser(ErrorCollector errorCollector) {
    return new SqrlParser(errorCollector);
  }

  public SqrlStatement parseStatement(String statement) {
    return (SqrlStatement)parser.createStatement(statement, parsingOptions);
  }
}