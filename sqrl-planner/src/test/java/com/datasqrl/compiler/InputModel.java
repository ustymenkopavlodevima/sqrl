package com.datasqrl.compiler;

import com.datasqrl.io.DataSystemConnectorConfig;
import lombok.Value;

public class InputModel {

  @Value
  public static class DataSource {
    String name;
    DataSystemConnectorConfig source;
  }
}