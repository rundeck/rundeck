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
* LoglevelOptions.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 30, 2010 4:46:38 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import static org.apache.tools.ant.Project.*;
import static org.apache.tools.ant.Project.MSG_WARN;
import static org.apache.tools.ant.Project.MSG_ERR;

import java.util.Map;
import java.util.HashMap;

/**
 * LoglevelOptions provides a loglevel option
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class LoglevelOptions implements CLIToolOptions {
    protected int currentLogLevel = MSG_INFO;
    final Map<String, Integer> LOG_LEVELS = new HashMap<String, Integer>();
    final Map<Integer, String> LOG_LEVELS_REV = new HashMap<Integer, String>();
    public static final String LOGLEVEL_OPTION = "l";
    public static final String LEVEL_LONG = "level";
    public static final String VERBOSE_OPTION = "v";
    public static final String DEBUG_OPTION = "V";
    public static final String DEBUG = "debug";
    public static final String VERBOSE = "verbose";
    public static final String INFO = "info";
    public static final String WARNING = "warning";
    public static final String ERROR = "error";

    public void addOptions(final Options options) {
        options.addOption(LOGLEVEL_OPTION, LEVEL_LONG, true, "log level. debug|verbose|info|warning|error");

        LOG_LEVELS.put(DEBUG, MSG_DEBUG);
        LOG_LEVELS_REV.put(MSG_DEBUG, DEBUG);
        LOG_LEVELS.put(VERBOSE, MSG_VERBOSE);
        LOG_LEVELS_REV.put(MSG_VERBOSE, VERBOSE);
        LOG_LEVELS.put(INFO, MSG_INFO);
        LOG_LEVELS_REV.put(MSG_INFO, INFO);
        LOG_LEVELS.put(WARNING, MSG_WARN);
        LOG_LEVELS_REV.put(MSG_WARN, WARNING);
        LOG_LEVELS.put(ERROR, MSG_ERR);
        LOG_LEVELS_REV.put(MSG_ERR, ERROR);
    }

    public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        if (cli.hasOption(LOGLEVEL_OPTION)) {
            final String argLevel = cli.getOptionValue(LOGLEVEL_OPTION);

            if (!LOG_LEVELS.containsKey(argLevel)) {
                throw new CLIToolOptionsException("unrecognized log level: " + argLevel);
            }
            currentLogLevel = LOG_LEVELS.get(argLevel);
        }

        if (cli.hasOption(VERBOSE_OPTION)) {
            currentLogLevel = MSG_VERBOSE;
        }
        if (cli.hasOption(DEBUG_OPTION)) {
            currentLogLevel = MSG_DEBUG;
        }
    }

    public void validate(CommandLine cli, String[] original) throws CLIToolOptionsException {
    }

    public int getLogLevel() {
        return currentLogLevel;
    }
}
