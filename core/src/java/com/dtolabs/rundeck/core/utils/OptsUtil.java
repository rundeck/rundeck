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
* OptsUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Sep 20, 2010 10:18:42 AM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OptsUtil is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class OptsUtil {

    /**
     * Bursts input string that may have emebedded quoted text into a String array.
     * Single quoted text is preserved as a single token
     * @param argLine input string
     * @return String array of parsed tokens
     */
    public static String[] burst(final String argLine) {
        if(null==argLine) {
            throw new NullPointerException("input cannot be null");
        }
        final List<String> tokens = new ArrayList<String>();
        // look for patterns like:
        //     -arg1 one -whitespaces 'string with white space'
        final String ARG_PATTERN =
           "'([^'\\\\]*(\\\\.[^'\\\\]*)*)' ?|([^ ]+) ?| ";

        final Pattern arg = Pattern.compile(ARG_PATTERN);
        final Matcher match = arg.matcher(argLine);
        while (match.find()) {

//        for (int offset = 0; arg.match(argLine, offset);) {
            // print the field (0=null, 1=quoted, 3=unquoted)
//            final int n = arg.getParenCount()-1;
            if(null!=match.group(3)){
                tokens.add(match.group(3));
            }else if (null!=match.group(1)) {
                //matched a quoted string with escapes
                //replace all escaped chars with the regular versions
                final String ms = match.group(1);
                final String value = ms.replaceAll("\\\\([\\\\'])", "$1");
                tokens.add(value);
            }


        }

        return tokens.toArray(new String[tokens.size()]);
    }

}
