/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* BaseExecutionResult.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 11:04:10 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;

/**
 * BaseExecutionResult implements ExecutionResult and provides factory methods for creating success or failure results
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* @version $Revision$
*/
public class BaseExecutionResult implements ExecutionResult{
    DispatcherResult resultObject;
    boolean success;
    Exception exception;

    BaseExecutionResult(final DispatcherResult resultObject, final boolean success, final Exception exception) {
        this.resultObject = resultObject;
        this.success = success;
        this.exception = exception;
    }

    /**
     * Create a Success result with an object
     *
     * @param success true if successful
     * @param object result object
     *
     * @return success result containing the object
     */
    public static ExecutionResult create(final boolean success, final DispatcherResult object) {
        return new BaseExecutionResult(object, success, null);
    }

    /**
     * Create a Success result with an object
     * @param object result object
     * @return success result containing the object
     */
    public static ExecutionResult createSuccess(final DispatcherResult object) {
        return new BaseExecutionResult(object, true, null);
    }

    /**
     * Create a failure result with an exception
     * @param exception exception
     * @return failure result containing the exception
     */
    public static ExecutionResult createFailure(final Exception exception) {
        return new BaseExecutionResult(null, false, exception);
    }

    public boolean isSuccess() {
        return success;
    }

    public Exception getException() {
        return exception;
    }

    public DispatcherResult getResultObject() {
        return resultObject;
    }

    @Override
    public String toString() {
        return "BaseExecutionResult{" +
               "resultObject=" + resultObject +
               ", success=" + success +
               ", exception=" + exception +
               '}';
    }
}
