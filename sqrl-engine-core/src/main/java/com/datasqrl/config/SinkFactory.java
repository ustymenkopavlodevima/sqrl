/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.config;

import com.datasqrl.io.DataSystemConnectorConfig;
import com.datasqrl.plan.global.OptimizedDAG.EngineSink;
import com.datasqrl.plan.global.OptimizedDAG.ExternalSink;
import com.datasqrl.plan.global.OptimizedDAG.WriteSink;

public interface SinkFactory<ENGINE_SINK> {
  String getEngine();
  String getSinkName();

  ENGINE_SINK create(WriteSink sink, DataSystemConnectorConfig config);
}