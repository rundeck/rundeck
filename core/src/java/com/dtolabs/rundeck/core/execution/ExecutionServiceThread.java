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
* ExecutionServiceThread.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jul 2, 2010 3:25:44 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

/**
 * ExecutionServiceThread can execute an item via the ExecutionService.  Provides retrieval of any ExecutionException
 * thrown by the ExecutionService, and provides a boolean indicating whether the execution succeeded or failed.  The
 * {@link #abort()} method interrupts the executing thread, and additionally sets an 'aborted' boolean, which can be
 * checked during loops (e.g. within {@link com.dtolabs.rundeck.core.cli.DefaultNodeDispatcher#executeNodedispatch(org.apache.tools.ant.Project,
 * java.util.Collection, int, boolean, FailedNodesListener, com.dtolabs.rundeck.core.tasks.controller.node.IExecutableTaskFactory)}
 * ) to monitor whether the execution should stop.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExecutionServiceThread extends Thread {
    ExecutionService eservice;
    ExecutionItem eitem;
    volatile boolean success = false;
    private volatile boolean aborted = false;
    volatile Exception thrown;
    volatile Object resultObject;
    volatile ExecutionResult result;

    public ExecutionServiceThread(final ExecutionService eservice, final ExecutionItem eitem) {
        this.eservice = eservice;
        this.eitem = eitem;
    }

    public void run() {
        if (null == this.eservice || null == this.eitem) {
            throw new IllegalStateException("project or execution detail not instantiated");
        }
        try {
            result = eservice.executeItem(eitem);
            success = result.isSuccess();
            if (null != result.getException()) {
                thrown = result.getException();
            }
            if (null != result.getResultObject()) {
                resultObject = result.getResultObject();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            thrown = e;
        }
    }


    public void abort() {
        if (isAlive()) {
            aborted = true;
            interrupt();
        }
    }

    public boolean isSuccessful() {
        return success;
    }

    public Exception getException() {
        return thrown;
    }

    public boolean isAborted() {
        return aborted;
    }
}
