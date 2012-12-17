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
* StepExecutionResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 11:48 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;


import com.dtolabs.rundeck.core.execution.HasSourceResult;
import com.dtolabs.rundeck.core.execution.StatusResult;


/**
 * StepExecutionResultImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionResultImpl implements StepExecutionResult, HasSourceResult {
    private boolean success;
    private Exception exception;
    private StatusResult sourceResult;

    public StepExecutionResultImpl(boolean success) {
        this.success = success;
    }

    public StepExecutionResultImpl(boolean success, Exception exception) {
        this.success = success;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof StepExecutionResultImpl)) { return false; }

        StepExecutionResultImpl result = (StepExecutionResultImpl) o;

        if (success != result.success) { return false; }
        if (exception != null ? !exception.equals(result.exception) : result.exception != null) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StepExecutionResultImpl{" +
               "success=" + success +
               ", exception=" + exception +
               '}';
    }

    public StatusResult getSourceResult() {
        return sourceResult;
    }

    public void setSourceResult(StatusResult sourceResult) {
        this.sourceResult = sourceResult;
    }
}
