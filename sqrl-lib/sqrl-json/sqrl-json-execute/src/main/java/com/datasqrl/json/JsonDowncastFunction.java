package com.datasqrl.json;

import com.datasqrl.function.DowncastFunction;
import com.datasqrl.json.JsonFunctions.JsonToString;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

@AutoService(DowncastFunction.class)
public class JsonDowncastFunction implements DowncastFunction {
  @Override
  public Class getConversionClass() {
    return JsonNode.class;
  }

  @Override
  public String downcastFunctionName() {
    return JsonFunctions.JSON_TO_STRING.getFunctionName().getCanonical();
  }

  @Override
  public Class getDowncastClassName() {
    return JsonToString.class;
  }
}
