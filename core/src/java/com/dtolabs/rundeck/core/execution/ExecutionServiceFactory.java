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
* ExecutionServiceMgr.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:06:47 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;

/**
 * ExecutionServiceFactory creates ExecutionServices.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExecutionServiceFactory {

    private ExecutionServiceFactory() {
    }

    public static ExecutionService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(ExecutionService.SERVICE_NAME)) {
            final ExecutionService service = new ExecutionServiceImpl(framework);
            framework.setService(ExecutionService.SERVICE_NAME, service);
            return service;
        }
        return (ExecutionService) framework.getService(ExecutionService.SERVICE_NAME);
    }

}
