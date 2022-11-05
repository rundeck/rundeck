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

package com.dtolabs.rundeck.core.jobs;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.SortedSet;

@Data
@Builder(toBuilder = true)
public class JobLifecycleStatusImpl
    implements JobLifecycleStatus {
    
    private boolean successful;
    private String errorMessage;
    private boolean useNewValues;
    private Map optionsValues;
    private SortedSet<JobOption> options;
    private boolean useNewMetadata;
    private Map newExecutionMetadata;
}
