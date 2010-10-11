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
* AbstractAction.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 3:28:42 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import org.apache.log4j.Logger;

/**
 * AbstractAction is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
abstract class AbstractAction extends BaseAction implements IAction {
    public static final Logger logger = Logger.getLogger(AbstractAction.class.getName());
    private IDispatchedScript context;
    private Framework framework;
    AbstractAction(final Framework framework, final IDispatchedScript context) {
        this.framework = framework;
        this.context = context;
        if(null==context) {
            throw new NullPointerException("null context");
        }
        if(null==context.getNodeSet()) {
            throw new IllegalArgumentException("dispatched script context requires nodeset");
        }
    }



    Framework getFramework(){
        return framework;
    }
    /**
     * do the action
     */
    public abstract void doAction();

    protected IDispatchedScript getContext() {
        return context;
    }
}
