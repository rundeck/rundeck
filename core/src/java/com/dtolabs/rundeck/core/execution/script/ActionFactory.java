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
* AtionFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 3:52:02 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionException;

/**
 * AtionFactory creates executable action given the input script context, framework, and listeners.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ActionFactory {

    /**
     * Create the action to perform, based on the properties configured for this instance
     *
     * @param context the script dispatch context
     * @param framework the framework
     * @param listener a BuildListener to use
     *
     * @return an action to perform with {@link IAction#doAction()}
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException if an error occurs
     */
    public static IAction createAction(final IDispatchedScript context, final Framework framework,
                                       final ExecutionListener listener) throws ExecutionException {

        if (null != context.getScript() || null != context.getServerScriptFilePath() || null != context
            .getScriptAsStream()) {
            /**
             * execute the script
             */
            return new ScriptfileAction(framework, context, listener);


        } else if (null != context.getArgs()) {
            /**
             * Run a command;
             */
            return new CommandAction(framework, context, listener);

        } else {
            throw new ExecutionException("No script or command specified");
        }
    }
}
