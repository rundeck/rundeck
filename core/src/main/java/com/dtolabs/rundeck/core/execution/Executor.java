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
* Executor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:40:23 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;

/**
 * Executor is an interface for implementations that can handle ExecutionItems, must have either a no-arg constructor, in which case the ExecutionService will call {@link #executeItem(ExecutionItem, ExecutionListener, ExecutionService, com.dtolabs.rundeck.core.common.Framework)},
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface Executor {


    /**
     * Execute the item and return the result.
     *
     * @param item item
     * @param listener listener
     * @param service exec service instance
     * @param framework framework
     * @return result
     */
    public ExecutionResult executeItem(ExecutionItem item, ExecutionListener listener, ExecutionService service, Framework framework) throws ExecutionException;
}
