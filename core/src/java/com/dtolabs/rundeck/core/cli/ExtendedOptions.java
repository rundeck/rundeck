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
* ExtendedOptions.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 30, 2010 4:54:01 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * ExtendedOptions extracts all options after "--" in the input argumeents
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExtendedOptions implements CLIToolOptions{
    private String[] extendedOptions;
    public void addOptions(final Options options) {
        //nothing to do here
    }

    public void parseArgs(final CommandLine cli, final String[] args) throws CLIToolOptionsException {
        //parse extended options after "--"
        int lastArg = -1;
        for (int i = 0 ; i < args.length ; i++) {
            if ("--".equals(args[i])) {
                lastArg = i;
                break;
            }
        }
        if (lastArg >= 0 && lastArg < args.length - 1) {
            extendedOptions = new String[args.length - lastArg - 1];
            System.arraycopy(args, lastArg + 1, extendedOptions, 0, args.length - lastArg - 1);
        }
    }

    public void validate(CommandLine cli, String[] original) throws CLIToolOptionsException {
    }

    public String[] getExtendedOptions() {
//        final String[] strings = new String[extendedOptions.length];
//        System.arraycopy(extendedOptions, 0, strings, 0, extendedOptions.length);
//        return strings;
        return null != extendedOptions ? extendedOptions.clone() : null;
    }
}
