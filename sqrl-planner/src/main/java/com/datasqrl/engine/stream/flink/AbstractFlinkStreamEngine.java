/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.engine.stream.flink;

import static com.datasqrl.engine.EngineFeature.STANDARD_STREAM;

import com.datasqrl.calcite.SqrlFramework;
import com.datasqrl.config.SqrlConfig;
import com.datasqrl.engine.EngineFeature;
import com.datasqrl.engine.ExecutionEngine;
import com.datasqrl.engine.pipeline.ExecutionPipeline;
import com.datasqrl.engine.stream.StreamEngine;
import com.datasqrl.engine.stream.flink.plan.FlinkStreamPhysicalPlan;
import com.datasqrl.error.ErrorCollector;
import com.datasqrl.plan.global.PhysicalDAGPlan.StagePlan;
import com.datasqrl.plan.global.PhysicalDAGPlan.StageSink;
import com.datasqrl.plan.global.PhysicalDAGPlan.StreamStagePlan;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.functions.FunctionDefinition;

@Slf4j
public abstract class AbstractFlinkStreamEngine extends ExecutionEngine.Base implements
    StreamEngine {

  public static final EnumSet<EngineFeature> FLINK_CAPABILITIES = STANDARD_STREAM;

  @Getter
  private final SqrlConfig config;

  public AbstractFlinkStreamEngine(SqrlConfig config) {
    super(FlinkEngineFactory.ENGINE_NAME, Type.STREAM, FLINK_CAPABILITIES);
    this.config = config;
  }

  @Override
  public boolean supports(FunctionDefinition function) {
    return true;
  }

  @Override
  public FlinkStreamPhysicalPlan plan(StagePlan stagePlan,
      List<StageSink> inputs, ExecutionPipeline pipeline, SqrlFramework framework,
      ErrorCollector errorCollector) {
    Preconditions.checkArgument(inputs.isEmpty());
    Preconditions.checkArgument(stagePlan instanceof StreamStagePlan);
    StreamStagePlan plan = (StreamStagePlan) stagePlan;

    return new FlinkStreamPhysicalPlan(plan);
  }

  @Override
  public void close() throws IOException {
  }

}
