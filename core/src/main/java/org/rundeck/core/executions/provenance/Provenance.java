/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.core.executions.provenance;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;

/**
 * Provenance of an execution
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//@JsonSubTypes({
//                      @JsonSubTypes.Type(value = GenericProvenance.class, name = "generic"),
//                      @JsonSubTypes.Type(value = ExecutionFollowupProvenance.class, name = "execution"),
//                      @JsonSubTypes.Type(value = StepPluginProvenance.class, name = "step-plugin"),
//                      @JsonSubTypes.Type(value = ExecutionFollowupProvenance.class, name = "plugin")
//              })
public interface Provenance<T> {
    T getData();
}
