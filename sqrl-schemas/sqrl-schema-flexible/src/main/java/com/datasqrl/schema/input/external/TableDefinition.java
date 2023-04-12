/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.schema.input.external;

import com.datasqrl.io.tables.SchemaDefinition;
import java.util.List;

public class TableDefinition extends AbstractElementDefinition implements
    SchemaDefinition {

  public TableDefinition() {

  }

  public static boolean PARTIAL_SCHEMA_DEFAULT = true;

  public String schema_version;

  public Boolean partial_schema;

  public List<FieldDefinition> columns;
  public List<String> tests;

}
