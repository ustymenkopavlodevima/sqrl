package com.datasqrl.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class EngineConfigImpl implements PackageJson.EngineConfig {

  @Getter
  SqrlConfig sqrlConfig;

  final String ENGINE_NAME_KEY = "type";

  @Override
  public String getEngineName() {
    return sqrlConfig.asString(ENGINE_NAME_KEY).get();
  }

  @Override
  public Map<String, Object> toMap() {
    return SqrlConfigUtil.toMap(sqrlConfig,
        Function.identity(), List.of());
  }

//  public EngineFactoryInterface discoverFactory() {
//    return ServiceLoaderDiscovery.get(EngineFactoryInterface.class,
//        EngineFactoryInterface::getEngineName,
//        sqrlConfig.asString(ENGINE_NAME_KEY).get());
//  }
}