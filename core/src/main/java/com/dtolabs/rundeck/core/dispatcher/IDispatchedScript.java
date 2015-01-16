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
* IDispatchedScript.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 22, 2010 10:08:53 AM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * IDispatchedScript describes the parameters for a dispatch invocation sent to the Dispatcher
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface IDispatchedScript extends IDispatchedExecution {

    /**
     * Get the framework project name
     * @return project name
     */
    public String getFrameworkProject();

    /**
     * Get the full script
     * @return the script string
     */
    public String getScript();

    /**
     * Get an InputStream that can provide the full script
     * @return the inputstream
     */
    public InputStream getScriptAsStream();

    /**
     * Get the server-local script path
     * @return server-side script path
     */
    public String getServerScriptFilePath();

    /**
     * Get the script URL
     * @return URL
     */
    public String getScriptURLString();

}
