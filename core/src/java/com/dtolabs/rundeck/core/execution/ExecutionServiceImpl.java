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
* ExecutionServiceImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:35:12 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;

import java.util.*;

/**
 * ExecutionServiceImpl implements the ExecutionService interface, and creates Executor instances to
 * perform the ExecutionItems.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
class ExecutionServiceImpl extends BaseExecutionService{
    private ExecutionListener listener;

    ExecutionServiceImpl(final Map<Class<? extends ExecutionItem>, Class<? extends Executor>> mapping,
                         final HashMap<Class<? extends ExecutionItem>, Executor> executors,
                         final Framework framework) {
        super(mapping, executors, framework);
    }


    public ExecutionResult executeItem(ExecutionContext context,final ExecutionItem item) throws ExecutionException {
        final Executor exec = executorForItem(item);
        return  exec.executeItem(item,getListener(),this,framework);
    }

    public ExecutionListener getListener() {
        return listener;
    }

    public void setListener(final ExecutionListener listener) {
        this.listener = listener;
    }

}
