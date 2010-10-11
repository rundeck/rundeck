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
* DispatchedScriptExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:54:49 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.script.ActionFactory;
import com.dtolabs.rundeck.core.execution.script.IAction;

/**
 * DispatchedScriptExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class DispatchedScriptExecutor implements Executor {

    /**
     * Constructor used by ExecutionService for creation
     * @param framework framework
     * @param executionService execservice
     */
    public DispatchedScriptExecutor() {
    }

    public ExecutionResult executeItem(final ExecutionItem item, final ExecutionListener listener,
                                       final ExecutionService executionService,
                                       final Framework framework) throws
        ExecutionException {
        if (!(item instanceof DispatchedScriptExecutionItem)) {
            throw new ExecutionException("Incorrect item type: " + item.getClass().getName());
        }
        final DispatchedScriptExecutionItem executionItem = (DispatchedScriptExecutionItem) item;
        return executeDispatchedScript(executionItem.getDispatchedScript(), listener,framework);
    }

    private ExecutionResult executeDispatchedScript(final IDispatchedScript context, final ExecutionListener listener,
                                                    final Framework framework) throws
        ExecutionException {
        final IAction action = ActionFactory.createAction(context, framework, listener);
        boolean success = false;
        Exception exception = null;
        try {
            action.doAction();
            success = true;
        } catch (Exception e) {
            exception = e;
        }
        if (success) {
            return BaseExecutionResult.createSuccess(null);
        } else {
            return BaseExecutionResult.createFailure(exception);
        }
    }

}
