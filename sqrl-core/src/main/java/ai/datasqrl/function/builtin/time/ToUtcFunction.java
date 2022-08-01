package ai.datasqrl.function.builtin.time;

import java.time.ZonedDateTime;
import java.util.List;
import org.apache.calcite.linq4j.tree.Types;
import org.apache.calcite.schema.ScalarFunction;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.InferTypes;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;

public class ToUtcFunction extends SqrlScalarFunction {

  static final ScalarFunction fnc = ScalarFunctionImpl.create(Types.lookupMethod(
      StdTimeLibraryImpl.class, "toUtc", ZonedDateTime.class));

  public ToUtcFunction() {
    super(
        new SqlIdentifier("TO_UTC", SqlParserPos.ZERO),
        SqlKind.OTHER,
        ReturnTypes.TIMESTAMP,
        InferTypes.ANY_NULLABLE,
        OperandTypes.operandMetadata(
            List.of(SqlTypeFamily.TIMESTAMP),
            typeFactory -> List.of(
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE),
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP)),
            i -> "arg" + i,
            i -> false),
        fnc,
        SqlFunctionCategory.USER_DEFINED_FUNCTION
    );
  }

  @Override
  public boolean isTimestampPreserving() {
    return true;
  }
}