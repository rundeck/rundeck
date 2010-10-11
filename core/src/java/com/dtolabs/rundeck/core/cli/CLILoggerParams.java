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
* CLILoggerParams.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 8, 2010 12:50:46 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

/**
 * CLILoggerParams is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface CLILoggerParams {
    /**
     * Return true if debug logging is on
     * @return true for debug logging
     */
    public boolean isDebug();

    /**
     * Return true for verbose logging
     * @return true for verbose
     */
    public boolean isVerbose();

    /**
     * Return true if quiet logging is on
     * @return true for quiet logging
     */
    public boolean isQuiet();
}
