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
* HelpOptions.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 3:31:29 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * HelpOptions adds -h/--help options.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class HelpOptions implements CLIToolOptions {
    private boolean helpOption;


    /**
     * short option string for usage help
     */
    public static final String HELP_OPTION = "h";

    /**
     * long option string for usage help
     */
    public static final String HELP_OPTION_LONG = "help";

    public void addOptions(final Options options) {
        /*
        * Declare the commandline options
        */
        options.addOption(HELP_OPTION, HELP_OPTION_LONG, false,
            "Print this help message");

    }

    public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        if (cli.hasOption(HELP_OPTION)) {
            helpOption = true;
        }
    }

    public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        if (helpOption) {
            throw new CLIToolOptionsException("");
        }
    }

    public boolean isHelpOption() {
        return helpOption;
    }
}
