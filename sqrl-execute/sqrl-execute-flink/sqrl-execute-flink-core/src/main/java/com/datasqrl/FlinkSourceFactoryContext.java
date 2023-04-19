package com.datasqrl;

import com.datasqrl.config.SourceFactoryContext;
import com.datasqrl.io.formats.FormatConfiguration;
import com.datasqrl.io.tables.TableConfig;
import java.util.UUID;
import lombok.Value;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Value
public class FlinkSourceFactoryContext implements SourceFactoryContext {
  StreamExecutionEnvironment env;
  String flinkName;
  FormatConfiguration formatConfig;
  TableConfig tableConfig;
  UUID uuid;
}