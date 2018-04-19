/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services.events

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import groovy.transform.ToString
import rundeck.Execution
import rundeck.ScheduledExecution

/**
 * @author greg
 * @since 2/17/17
 */
@ToString(includeNames = true)
class ExecutionPrepareEvent {
    Execution execution
    ScheduledExecution job
    StepExecutionContext context
    Map<String,String> options
}
