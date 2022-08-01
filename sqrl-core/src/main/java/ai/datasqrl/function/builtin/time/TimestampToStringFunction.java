package ai.datasqrl.function.builtin.time;

import java.time.Instant;
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

public class TimestampToStringFunction extends SqrlScalarFunction {

  static final ScalarFunction fnc = ScalarFunctionImpl.create(Types.lookupMethod(
      StdTimeLibraryImpl.class, "timestampToString", Instant.class));

  public TimestampToStringFunction() {
    super(
        new SqlIdentifier("TIMESTAMP_TO_STRING", SqlParserPos.ZERO),
        SqlKind.OTHER,
        ReturnTypes.VARCHAR_2000_NULLABLE,
        InferTypes.ANY_NULLABLE,
        OperandTypes.operandMetadata(
            List.of(SqlTypeFamily.TIMESTAMP),
            typeFactory -> List.of(
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP)),
            i -> "arg" + i,
            i -> false),
        fnc,
        SqlFunctionCategory.USER_DEFINED_FUNCTION
    );
  }
}