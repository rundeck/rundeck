/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rundeck.app.data.model.v1.job.workflow;

import java.util.Map;

/**
 * Represents a single conditional definition with a key, operator, and value.
 * Used to define conditions that can be evaluated during workflow execution.
 */
public interface ConditionalDefinition {
    /**
     * Get the key to evaluate (e.g., "option.env", "step.output", etc.)
     * @return The key string
     */
    String getKey();

    /**
     * Get the operator to use for comparison
     * Supported operators: ==, !=, >, <, >=, <=, contains, matches
     * @return The operator string
     */
    String getOperator();

    /**
     * Get the value to compare against
     * @return The value object (can be String, Number, Boolean, etc.)
     */
    Object getValue();

    Map toMap();


}

