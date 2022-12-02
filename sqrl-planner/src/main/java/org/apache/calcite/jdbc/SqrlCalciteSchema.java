/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.jdbc;

import ai.datasqrl.schema.Relationship;
import ai.datasqrl.schema.SQRLTable;
import org.apache.calcite.schema.Schema;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class SqrlCalciteSchema extends SimpleCalciteSchema {
  public SqrlCalciteSchema(Schema schema) {
    super(null, schema, "");
  }

  public List<SQRLTable> getRootTables() {
    return getTableNames().stream()
        .map(name -> getTable(name, false).getTable())
        .filter(t->t instanceof SQRLTable)
        .map(t->(SQRLTable) t)
        .collect(Collectors.toList());
  }

  public List<SQRLTable> getAllTables() {
    Set<SQRLTable> tables = new HashSet<>(getRootTables());
    Stack<SQRLTable> iter = new Stack<>();
    iter.addAll(tables);

    while (!iter.isEmpty()) {
      SQRLTable table = iter.pop();
      if (table == null) continue;
      List<SQRLTable> relationships = table.getFields().getAccessibleFields().stream()
          .filter(f-> f instanceof Relationship)
          .map(f->((Relationship)f).getToTable())
          .collect(Collectors.toList());

      for (SQRLTable rel : relationships) {
        if (!tables.contains(rel)) {
          iter.add(rel);
          tables.add(rel);
        }
      }
    }

    return tables.stream()
        .filter(f->f != null)
        .collect(Collectors.toList());
  }
}