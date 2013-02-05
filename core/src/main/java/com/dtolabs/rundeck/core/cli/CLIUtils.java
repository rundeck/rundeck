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
* CLIUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 8:43:34 AM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLIUtils provides utility functions
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CLIUtils {
    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     *
     *         At this point in time, default behavior is "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args) {
        return generateArgline(scriptpath, args, " ", true);
    }

    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     * @param unsafe     whether to use backwards-compatible, known-insecure quoting
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args, final Boolean unsafe) {
        return generateArgline(scriptpath, args, " ", unsafe);
    }

    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     * @param separator  character to use to separate arguments
     * @param unsafe     whether to use backwards-compatible, known-insecure quoting
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args, final String separator, final Boolean unsafe) {
        final StringBuffer sb = new StringBuffer();
        final ArrayList<String> list = new ArrayList<String>();
        if (null != scriptpath) {
            list.add(scriptpath);
        }
        if (null != args) {
            list.addAll(Arrays.asList(args));
        }
        for (final String arg : list) {
            if(null==arg){
                continue;
            }
            if (sb.length() > 0) {
                sb.append(separator);
            }
            if(unsafe) {
                /* DEPRECATED SECURITY RISK: Exists for backwards compatibility only. */
                if (arg.indexOf(" ") >= 0 && !(0 == arg.indexOf("'") && (arg.length() - 1) == arg.lastIndexOf("'"))) {
                    sb.append("'").append(arg).append("'");
                } else {
                    sb.append(arg);
                }
            } else {
                sb.append("'");
                sb.append(arg.replace("'", "'\"'\"'"));
                sb.append("'");
            }
        }
        return sb.toString();
    }

    /**
     * Split the argline string into a list of strings, each quoted string will be a single element in the list.
     *
     * @param argline
     *
     * @return argline with single-quote quoted strings additionally quoted with double quotes
     */
    public static List<String> splitArgLine(final String argline) {
        final ArrayList<String> sb1 = new ArrayList<String>();

        final String sqre = "(?:^'|\\s*')([^\\']+)(?:'$|'\\s*)";
        final String dqre = "(?:^\"|\\s*\")([^\\\"]+)(?:\"$|\"\\s*)";
        final String nospac = "(?:^|\\s*)(\\S+)(?:$|\\s*)";
        final Matcher matcher = Pattern.compile(sqre + "|" + dqre + "|" + nospac).matcher(argline);
        while(matcher.find()) {
            if(matcher.group(1)!=null){
                sb1.add(matcher.group(1));
            }else if(matcher.group(2)!=null){
                sb1.add(matcher.group(2));
            }else if(matcher.group(3)!=null){
                sb1.add(matcher.group(3));
            }
        }
        return sb1;
    }
}
