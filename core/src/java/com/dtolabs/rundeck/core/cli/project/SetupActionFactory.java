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
* ActionFactory.java
* 
* User: greg
* Created: Oct 19, 2007 5:35:29 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli.project;

import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import com.dtolabs.rundeck.core.common.Framework;


/**
 * ActionFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SetupActionFactory {

    /**
     * create CreateAction
     * @param main
     * @param framework
     * @param baseArgs
     * @param createArgs
     * @return
     */
    public static CreateAction createCreateAction(final CLIToolLogger main,
                                      final Framework framework,
                                      final BaseAction.BaseActionArgs baseArgs,
                                      final CreateAction.CreateActionArgs createArgs) {
        return new CreateAction(main, framework, baseArgs, createArgs);
    }



}
