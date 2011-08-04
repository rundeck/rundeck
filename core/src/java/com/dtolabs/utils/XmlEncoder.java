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
 * XmlEncoder.java
 *
 * Created on January 25, 2004
 * $Id: XmlEncoder.java 7781 2008-02-08 00:39:08Z gschueler $
 */

package com.dtolabs.utils;

/**
 * Provides a basic encode/decode methods to handle characters harmful to XML documents.
 *
 * @author <a href="mailto:alexh@hoho.local">Alex Honor</a>
 * @version 1.0
 */
public class XmlEncoder {

    /**
     * Takes input string and replaces all occurrences of the following
     * characters with the escaped counterparts: &, <, >, ', \t, \n, \r, "
     *
     * @param s a <code>String</code> input value
     * @return a <code>String</code> output value
     */
    public static String encode(String s) {        
        s = replace(s, "&", "&amp;"); 
        s = replace(s, "<", "&lt;");
        s = replace(s, ">", "&gt;");
        s = replace(s, "'", "&apos;");
        s = replace(s, "\t", "&#9;");
        s = replace(s, "\n", "&#xA;");
	s = replace(s, "\r", "&#xD;");
        return replace(s, "\"", "&quot;");
    }

    /**
     * Takes input string and replaces all occurrences of the escaped
     * charcter sequences and returns back 'harmful' XML characters.
     * @param s a <code>String</code> input value
     * @return a <code>String</code> output value
     */
    public static String decode(String s) {        
        s = replace(s, "&amp;", "&"); 
        s = replace(s, "&lt;", "<");
        s = replace(s, "&gt;", ">");
        s = replace(s, "&apos;", "'");
        s = replace(s, "&#9;", "\t");
        s = replace(s, "&#xA;", "\n");
	s = replace(s, "&#xD", "\r");
        return replace(s, "&quot;", "\"");
    }

    /**
     * Utility method to make substitutions
     *
     * @param s a <code>String</code> value
     * @param oldString a <code>String</code> value
     * @param newString a <code>String</code> value
     * @return a <code>String</code> value
     */
    private static String replace(String s, String oldString, String newString) {
        StringBuffer sb = new StringBuffer();
        int length = oldString.length();
        int pos = s.indexOf(oldString);
        int lastPos = 0;
        while (pos >= 0) {
            sb.append(s.substring(lastPos, pos)).append(newString);
            lastPos = pos + length;
            pos = s.indexOf(oldString, lastPos);
        }
        return sb.append(s.substring(lastPos, s.length())).toString();
    }           
}