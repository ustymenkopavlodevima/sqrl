package ai.datasqrl.plan.calcite.util;

import ai.datasqrl.plan.calcite.SqrlOperatorTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.codehaus.commons.nullanalysis.Nullable;

public class SqlNodeUtil {

  public static SqlNode and(@Nullable SqlNode... exprs) {
    if (exprs == null) {
      return null;
    }
    return and(Stream.of(exprs).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  public static SqlNode and(List<SqlNode> expressions) {
    if (expressions.size() == 0) {
      return null;
    } else if (expressions.size() == 1) {
      return expressions.get(0);
    } else if (expressions.size() == 2) {
      return new SqlBasicCall(SqrlOperatorTable.AND,
          new SqlNode[]{
              expressions.get(0),
              expressions.get(1)
          },
          SqlParserPos.ZERO);
    }

    return new SqlBasicCall(SqrlOperatorTable.AND,
        new SqlNode[]{
            expressions.get(0),
            and(expressions.subList(1, expressions.size()))
        },
        SqlParserPos.ZERO);
  }
}