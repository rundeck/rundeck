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
* DefaultCLIToolLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 24, 2010 3:33:29 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

/**
 * DefaultCLIToolLogger logs to System.out and System.err
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class DefaultCLIToolLogger implements CLIToolLogger{

    public void log(final String output) {
        System.out.println(output);
    }

    public void error(final String output) {
        System.err.println(output);
    }


    public void warn(final String output) {
        System.err.println(output);
    }

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    public void verbose(final String message) {
        System.err.println(message);
    }

    public void debug(final String message) {
        System.err.println(message);
    }
}
