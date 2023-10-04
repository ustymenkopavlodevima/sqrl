/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Copied from flink as we incrementally phase out flink code for sqrl code
package com.datasqrl.flink.function;

import com.datasqrl.calcite.Dialect;
import com.datasqrl.calcite.type.TypeFactory;
import org.apache.calcite.rel.core.Aggregate.AggCallBinding;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCallBinding;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.validate.SqrlSqlValidator;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.functions.FunctionDefinition;
import org.apache.flink.table.planner.calcite.FlinkTypeFactory;
import org.apache.flink.table.planner.functions.inference.OperatorBindingCallContext;
import org.apache.flink.table.types.inference.CallContext;
import org.apache.flink.table.types.inference.TypeInference;
import org.apache.flink.table.types.logical.LogicalType;

import javax.annotation.Nullable;
import org.apache.flink.table.types.logical.utils.LogicalTypeUtils;

import static com.datasqrl.flink.function.FlinkTypeUtil.unwrapTypeFactory;
import static org.apache.flink.table.types.inference.TypeInferenceUtil.*;

public class FlinkSqlReturnTypeInference implements SqlReturnTypeInference {

  private final FlinkTypeFactory flinkTypeFactory;
  private final DataTypeFactory dataTypeFactory;

  private final FunctionDefinition definition;

  private final TypeInference typeInference;

  public FlinkSqlReturnTypeInference(
      FlinkTypeFactory flinkTypeFactory, DataTypeFactory dataTypeFactory,
      FunctionDefinition definition,
      TypeInference typeInference) {
    this.flinkTypeFactory = flinkTypeFactory;
    this.dataTypeFactory = dataTypeFactory;
    this.definition = definition;
    this.typeInference = typeInference;
  }

  @Override
  public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
    final CallContext callContext =
        new OperatorBindingCallContext(
            dataTypeFactory,
            definition,
            adaptBinding(opBinding,  unwrapTypeFactory(opBinding)),
            extractExpectedOutputType(opBinding));
    try {
      return inferReturnTypeOrError(flinkTypeFactory, unwrapTypeFactory(opBinding), callContext);
    } catch (ValidationException e) {
      throw createInvalidCallException(callContext, e);
    } catch (Throwable t) {
      throw createUnexpectedException(callContext, t);
    }
  }

  private SqlOperatorBinding adaptBinding(SqlOperatorBinding opBinding, TypeFactory typeFactory) {
    if (opBinding instanceof SqlCallBinding) {
      SqlCallBinding sqlCallBinding = (SqlCallBinding) opBinding;
      return FlinkOperandMetadata.adaptCallBinding(sqlCallBinding, flinkTypeFactory);
    } else if (opBinding instanceof AggCallBinding) {
      AggCallBinding sqlCallBinding = (AggCallBinding) opBinding;
      return FlinkOperandMetadata.adaptCallBinding(sqlCallBinding, flinkTypeFactory, typeFactory);
    } else if (opBinding instanceof RexCallBinding) {
      RexCallBinding sqlCallBinding = (RexCallBinding) opBinding;
      return FlinkOperandMetadata.adaptCallBinding(sqlCallBinding, flinkTypeFactory, typeFactory);

    }

    return opBinding;
  }

  // --------------------------------------------------------------------------------------------

  private @Nullable RelDataType extractExpectedOutputType(SqlOperatorBinding opBinding) {
    if (opBinding instanceof SqlCallBinding) {
      final SqlCallBinding binding = (SqlCallBinding) opBinding;
      final SqrlSqlValidator validator =
          (SqrlSqlValidator) binding.getValidator();
      return validator.getExpectedOutputType(binding.getCall()).orElse(null);
    }
    return null;
  }

  private RelDataType inferReturnTypeOrError(
      FlinkTypeFactory typeFactory, TypeFactory sqrlTypeFactory, CallContext callContext) {

    final LogicalType inferredType =
        inferOutputType(callContext, typeInference.getOutputTypeStrategy())
            .getLogicalType();
    return sqrlTypeFactory.translateToSqrlType(Dialect.FLINK,
        typeFactory.createFieldTypeFromLogicalType(inferredType));
  }
}