/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.Optional;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ScriptConfiguration {

  public static final String PROPERTY = "script";

  @NonNull @NotEmpty
  String main;
  String graphql;

  @JsonIgnore
  public Optional<String> getOptGraphQL() {
    return Optional.ofNullable(graphql)
        .filter(gql -> !gql.isEmpty());
  }

}