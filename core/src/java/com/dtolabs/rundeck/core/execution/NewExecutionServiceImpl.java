/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* NewExecutionServiceImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 11:19 AM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream;

import java.io.OutputStream;
import java.util.*;

/**
 * NewExecutionServiceImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NewExecutionServiceImpl implements ExecutionService{
    private final Framework framework;
    private ExecutionListener listener;

    public NewExecutionServiceImpl(Framework framework) {
        this.framework = framework;
    }

    public ExecutionResult executeItem(ExecutionContext context, ExecutionItem item) throws ExecutionException {
        final CommandInterpreter commandInterpreter;
        final NodeDispatcher nodeDispatcher;
        try {
            commandInterpreter = framework.getCommandInterpreterForItem(item);
            nodeDispatcher = framework.getNodeDispatcherForContext(context);
        } catch (ExecutionServiceException e) {
            throw new ExecutionException(e);
        }

        //bind System printstreams to the thread
        final ThreadBoundOutputStream threadBoundSysOut = ThreadBoundOutputStream.bindSystemOut();
        final ThreadBoundOutputStream threadBoundSysErr = ThreadBoundOutputStream.bindSystemErr();

        //get outputstream for reformatting destination
//        final OutputStream origout = threadBoundSysOut.getThreadStream();
//        final OutputStream origerr = threadBoundSysErr.getThreadStream();

        boolean success=false;
        Exception exception=null;
        DispatcherResult result=null;
        try {
            result= nodeDispatcher.dispatch(context, commandInterpreter, item);
            success=result.isSuccess();
        } catch (DispatcherException e) {
            exception=e;
        } finally {
            threadBoundSysOut.removeThreadStream();
            threadBoundSysErr.removeThreadStream();
        }

        return new BaseExecutionResult(result, success, exception);
    }

    public ExecutionListener getListener() {
        return null;
    }

    void setListener(ExecutionListener listener) {
        this.listener = listener;
    }
}
