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

import java.util.HashMap;
import java.util.Map;


/**
 * StepExecutionResultImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StepExecutionResultImpl implements StepExecutionResult, HasSourceResult {
    private boolean success;
    private Throwable exception;
    private StatusResult sourceResult;
    private Map<String, Object> resultData;
    private Map<String, Object> failureData;
    private FailureReason failureReason;
    private String failureMessage;

    public StepExecutionResultImpl() {
        this.success = true;
        resultData = new HashMap<String, Object>();
        failureData = new HashMap<String, Object>();
    }

    public StepExecutionResultImpl(Throwable exception, FailureReason failureReason, String failureMessage) {
        this();
        this.success=false;
        this.exception = exception;
        this.failureReason=failureReason;
        this.failureMessage=failureMessage;
    }
    public static StepExecutionResultImpl wrapStepException(StepException e) {
        return new StepExecutionResultImpl(e, e.getFailureReason(), e.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Throwable getException() {
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
//        if (sourceResult != null ? !sourceResult.equals(result.sourceResult) : result.sourceResult != null) {
//            return false;
//        }
        if (failureData != null ? !failureData.equals(result.failureData)
                                : result.failureData != null) {
            return false;
        }
        if (resultData != null ? !resultData.equals(result.resultData) : result.resultData != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
//        result = 31 * result + (sourceResult != null ? sourceResult.hashCode() : 0);
        result = 31 * result + (resultData != null ? resultData.hashCode() : 0);
        result = 31 * result + (failureData != null ? failureData.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (success ) {
            return (null != sourceResult ?  sourceResult.toString() : "Step successful");
        }else  {
            return failureReason + ": " + failureMessage;
        }
    }

    public StatusResult getSourceResult() {
        return sourceResult;
    }

    public void setSourceResult(StatusResult sourceResult) {
        this.sourceResult = sourceResult;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }

    public Map<String, Object> getFailureData() {
        return failureData;
    }

    public void setFailureData(Map<String, Object> failureData) {
        this.failureData = failureData;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
