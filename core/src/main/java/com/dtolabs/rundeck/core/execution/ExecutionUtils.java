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
* ExecutionUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 3, 2010 5:30:48 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.Constants;

/**
 * ExecutionUtils is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExecutionUtils {

    /**
     * Get message loglevel string for the integer value
     *
     * @param level    integer level
     * @param defLevel default string to return if integer doesn't match
     *
     * @return loglevel string, or the default value
     */
    public static String getMessageLogLevel(final int level, final String defLevel) {
        switch (level) {
            case (Constants.ERR_LEVEL):
                return Constants.MSG_ERR;
            case (Constants.DEBUG_LEVEL):
                return Constants.MSG_DEBUG;
            case (Constants.INFO_LEVEL):
                return Constants.MSG_INFO;
            case (Constants.VERBOSE_LEVEL):
                return Constants.MSG_VERBOSE;
            case (Constants.WARN_LEVEL):
                return Constants.MSG_WARN;
            default:
                return defLevel;
        }
    }
}
