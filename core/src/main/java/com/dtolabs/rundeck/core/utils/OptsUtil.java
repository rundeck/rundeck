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

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * OptsUtil is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class OptsUtil {
    /**
     * Bursts input string that may have emebedded quoted text into a String array. Single quoted text is preserved as a
     * single token
     *
     * @param argLine input string
     *
     * @return String array of parsed tokens
     */
    public static String[] burst(final String argLine) {
        return QuotedStringTokenizer.tokenizeToArray(argLine);
    }

    public static String join(String first, String[] args) {
        return join(first, Arrays.asList(args));
    }

    public static String join(String[] args) {
        return join(Arrays.asList(args));
    }

    public static String join(String first, List<String> args) {
        List<String> strings = new ArrayList<String>(args.size() + 1);
        strings.add(first);
        strings.addAll(args);
        return join(strings);
    }

    public static String join(Collection<String> args) {
        StringBuilder sb = new StringBuilder();
        for (String str : args) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(escapeChars(str));
        }
        return sb.toString();
    }

    private static final char DBL_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char[] CSV_SEARCH_CHARS = new char[]{' ', DBL_QUOTE, SINGLE_QUOTE, CharUtils.CR, CharUtils.LF};

    public static String escapeChars(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        escapeChars(stringBuilder, str);
        return stringBuilder.toString();
    }

    public static void escapeChars(StringBuilder out, String str) {
        if (StringUtils.containsNone(str, CSV_SEARCH_CHARS) && !"".equals(str)) {
            if (str != null) {
                out.append(str);
            }
            return;
        }
        out.append(DBL_QUOTE);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == DBL_QUOTE) {
                out.append(DBL_QUOTE); // escape double quote
            }
            out.append(c);
        }
        out.append(DBL_QUOTE);
    }

}
