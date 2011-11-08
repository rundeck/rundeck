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
* TaskCallable.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 4:11:02 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.concurrent.Callable;

/**
 * TaskCallable is a Callable that executes an Ant Task.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class TaskCallable implements Callable {
    private Task task;

    public TaskCallable(final Task task) {
        this.task = task;
    }

    public Object call() throws BuildException {
        task.execute();
        return null;
    }

    @Override
    public String toString() {
        return "TaskCallable: task{" + task + "}";
    }

    public Task getTask(){
        return task;
    }
}
