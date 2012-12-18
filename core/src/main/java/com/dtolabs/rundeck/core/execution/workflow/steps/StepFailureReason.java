/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* StepFailureReason.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/18/12 11:04 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

/**
* Failure causes for workflow steps
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
public enum StepFailureReason implements FailureReason{
    /**
     * A misconfiguration caused a failure
     */
    ConfigurationFailure,
    /**
     * A process was interrupted
     */
    Interrupted,
    /**
     * An IO error
     */
    IOFailure,
    /**
     * Plugin failed
     */
    PluginFailed,
    /**
     * Cause was not identified
     */
    Unknown,
}
