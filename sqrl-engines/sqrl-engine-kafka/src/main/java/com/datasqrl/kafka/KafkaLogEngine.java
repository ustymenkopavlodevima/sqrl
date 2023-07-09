package com.datasqrl.kafka;

import com.datasqrl.canonicalizer.NamePath;
import com.datasqrl.engine.EngineCapability;
import com.datasqrl.engine.EnginePhysicalPlan;
import com.datasqrl.engine.ExecutionEngine;
import com.datasqrl.engine.ExecutionResult;
import com.datasqrl.engine.ExecutionResult.Message;
import com.datasqrl.engine.log.Log;
import com.datasqrl.engine.log.LogEngine;
import com.datasqrl.engine.pipeline.ExecutionPipeline;
import com.datasqrl.error.ErrorCollector;
import com.datasqrl.error.NotYetImplementedException;
import com.datasqrl.io.DataSystemDiscovery;
import com.datasqrl.io.DataSystemDiscovery.Base;
import com.datasqrl.io.ExternalDataType;
import com.datasqrl.io.tables.TableConfig;
import com.datasqrl.io.tables.TableSchema;
import com.datasqrl.io.tables.TableSink;
import com.datasqrl.plan.global.PhysicalDAGPlan.LogStagePlan;
import com.datasqrl.plan.global.PhysicalDAGPlan.StagePlan;
import com.datasqrl.plan.global.PhysicalDAGPlan.StageSink;
import com.datasqrl.schema.TableSchemaExporterFactory;
import com.datasqrl.schema.UniversalTable;
import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.tools.RelBuilder;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.internals.Topic;

@Slf4j
public class KafkaLogEngine extends ExecutionEngine.Base implements LogEngine {

  private final TableConfig config;
  private final TableSchemaExporterFactory schemaFactory;

  public KafkaLogEngine(TableConfig config, TableSchemaExporterFactory schemaFactory) {
    super(KafkaLogEngineFactory.ENGINE_NAME, Type.LOG, EnumSet.noneOf(EngineCapability.class));
    this.config = config;
    this.schemaFactory = schemaFactory;
  }

  @SneakyThrows
  @Override
  public CompletableFuture<ExecutionResult> execute(EnginePhysicalPlan plan,
      ErrorCollector errors) {
    CreateTopicsResult result;
    log.info("Connecting to admin");
    try (Admin admin = Admin.create(
        Map.of("bootstrap.servers", config.getConfig()
            .getSubConfig("connector").asString("bootstrap.servers").get()))) {
      KafkaPhysicalPlan kafkaPhysicalPlan = (KafkaPhysicalPlan) plan;
      log.info("Creating topics");
      result = admin.createTopics(kafkaPhysicalPlan.getTopics());
      try {//debug
        result.all().get(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.error("Could not create topics.");
      }
      log.info("Topics created: " + admin.listTopics().names().get());
    }


    return result.all().toCompletionStage().toCompletableFuture()
        .thenApply(f->new ExecutionResult.Message("COMPLETE"));
  }

  @Override
  public EnginePhysicalPlan plan(StagePlan plan, List<StageSink> inputs, ExecutionPipeline pipeline,
      RelBuilder relBuilder, TableSink errorSink) {
    Preconditions.checkArgument(plan instanceof LogStagePlan);
    return new KafkaPhysicalPlan(this.config.getConfig(),
        ((LogStagePlan) plan).getLogs().stream()
            .map(log -> (KafkaTopic)log)
            .map(t->new NewTopic(t.getTopicName(), Optional.empty(), Optional.empty()))
            .collect(Collectors.toList()));
  }

  @Override
  public Log createLog(String logId, UniversalTable schema) {
    String topicName = sanitizeName(logId);
    Preconditions.checkArgument(Topic.isValid(topicName), "Not a valid topic name: %s", topicName);
    TableSchema tblSchema = schemaFactory.convert(schema);
    TableConfig.Builder tblBuilder = DataSystemDiscovery.Base.copyGeneric(config, schema.getName(), topicName, ExternalDataType.source_and_sink);
    TableConfig logConfig = tblBuilder.build();
    NamePath path = schema.getName().toNamePath();
    return new KafkaTopic(topicName,
        logConfig.initializeSource(path, tblSchema),
        logConfig.initializeSink(path, Optional.of(tblSchema)));
  }

  static String sanitizeName(String logId) {
    String sanitizedName = logId;
    for (char invalidChar : REPLACE_CHARS) {
      sanitizedName = sanitizedName.replace(invalidChar,REPLACE_WITH);
    }
    return sanitizedName;
  }

  public static final char[] REPLACE_CHARS = {'$'};
  public static final char REPLACE_WITH = '-';

}