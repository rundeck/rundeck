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
* Log4JCLIToolLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 24, 2010 3:39:39 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.log4j.Logger;

/**
 * Log4JCLIToolLogger uses Log4j Logger implementation
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class Log4JCLIToolLogger implements CLIToolLogger {
    private Logger logger ;

    /**
     * Constructor
     * @param logger Log4j logger
     */
    public Log4JCLIToolLogger(final Logger logger){
        this.logger=logger;
    }

    public void log(final String message) {
        logger.info(message);
    }

    public void error(final String message) {
        logger.error(message);
    }

    public void warn(final String message) {
        logger.warn(message);
    }

    public void verbose(final String message) {
        logger.debug(message);
    }

    public void debug(final String message) {
        logger.trace(message);
    }
}
