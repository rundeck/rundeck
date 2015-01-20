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
* ExecTaskParameterGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 19, 2010 6:35:34 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.common.INodeEntry;

import java.io.File;

/**
 * ExecTaskParameterGenerator is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ExecTaskParameterGenerator {
    /**
     *
     * @param nodeentry node
     * @param command command
     * @param scriptfile scriptfile
     * @param args args
     *
     * @return Generate the {@link com.dtolabs.rundeck.core.execution.script.ExecTaskParameters}
     *
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException if an error occurs
     */
    ExecTaskParameters generate(final INodeEntry nodeentry, final boolean command, final File scriptfile,
                                final String[] args) throws ExecutionException;
}
