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
package org.apache.calcite.sql;

import java.util.Locale;
/**
 * Copied from Calcite: - Added temporal join
 */

/**
 * Enumerates the types of join.
 */
public enum JoinType implements Symbolizable {
  /**
   * Inner join.
   */
  INNER,

  /**
   * Full outer join.
   */
  FULL,

  /**
   * Cross join (also known as Cartesian product).
   */
  CROSS,

  /**
   * Left outer join.
   */
  LEFT,

  /**
   * Right outer join.
   */
  RIGHT,

  /**
   * Left semi join.
   *
   * <p>Not used by Calcite; only in Babel's Hive dialect.
   */
  LEFT_SEMI_JOIN,

  /**
   * Comma join: the good old-fashioned SQL <code>FROM</code> clause,
   * where table expressions are specified with commas between them, and
   * join conditions are specified in the <code>WHERE</code> clause.
   */
  COMMA,

  /**
   * SQRL temporal join
   */
  TEMPORAL,
  INTERVAL,
  DEFAULT,
  IMPLICIT,
  LEFT_TEMPORAL,
  LEFT_INTERVAL,
  RIGHT_TEMPORAL,
  RIGHT_INTERVAL,
  ;

  /** Lower-case name. */
  public final String lowerName = name().toLowerCase(Locale.ROOT);

  /**
   * Returns whether a join of this type may generate NULL values on the
   * left-hand side.
   */
  public boolean generatesNullsOnLeft() {
    return this == RIGHT || this == FULL;
  }

  /**
   * Returns whether a join of this type may generate NULL values on the
   * right-hand side.
   */
  public boolean generatesNullsOnRight() {
    return this == LEFT || this == FULL;
  }
}
