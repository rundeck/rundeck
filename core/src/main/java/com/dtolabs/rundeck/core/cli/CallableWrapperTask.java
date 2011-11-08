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
* GenericTask.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 4:04:59 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.concurrent.Callable;

/**
 * CallableWrapperTask calls the Callable when the task is executed. The result of the Callable.call is available as the
 * resultObject property.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CallableWrapperTask extends Task {
    private Callable callable;
    private Object resultObject;

    public CallableWrapperTask(final Callable callable) {
        this.callable = callable;
    }

    @Override
    public void execute() throws BuildException {
        if (null == getCallable()) {
            throw new BuildException("CallableWrapperTask requires a Callable object");
        }
        try {
            resultObject = getCallable().call();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public Callable getCallable() {
        return callable;
    }

    public void setCallable(final Callable callable) {
        this.callable = callable;
    }

    public Object getResultObject() {
        return resultObject;
    }
}
