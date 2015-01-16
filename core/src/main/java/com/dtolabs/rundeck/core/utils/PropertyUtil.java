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

package com.dtolabs.rundeck.core.utils;


import org.apache.tools.ant.Project;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * expands nested property references within a provided string
 */
public class PropertyUtil {

    /**
     * expand a given Properties object and return a new one.
     * This will process each key to a given Properties object, get its value
     * and expand it.  Each value may contain references to other keys within this
     * given Properties object, and if so, all keys and their expanded keyValues will be resolved
     * into a new Properties object that will be returned.
     * @return properties
     * @param properties input
     */
    public static Properties expand(final Map properties) {
        final Properties expandedProperties = new Properties();
        for (final Object o : properties.entrySet()) {
            final Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final String keyValue = (String) entry.getValue();
            final String expandedKeyValue = expand(keyValue, properties);
            expandedProperties.setProperty(key, expandedKeyValue);
        }

        return expandedProperties;
    }

    /**
     * expand a keyString that may contain references to properties
     * located in provided Properties object
     * @return expanded
     * @param keyString string
     *                  @param properties properties
     */
    public static String expand(String keyString, Properties properties) {
        return PropertyUtil.expand(keyString, (Map) properties);
    }

    /**
     * expand a keyString that may contain references other properties
     * located in provided Map object
     * NOTE:  this is a recursive method in case references to properties are
     * nested within another reference
     * @return expanded
     * @param keyString string
     *                  @param properties properties
     */
    public static String expand(String keyString, Map properties) {
        String expandedLine = lineExpand(keyString, properties);
        if (resolvesToLiteral(expandedLine) || expandedLine.equals(keyString)) {
            return expandedLine;
        }
        return expand(expandedLine, properties);
    }

    /**
     * expand a keyString that may contain referecnes to other properties
     * @param keyString string containing props
     * @param project Ant project
     * @return expanded string                    
     */
    public static String expand(String keyString, Project project) {
        return expand(keyString, project.getProperties());
    }

    /**
     * parse the given keyString and expand each reference to a property from the given
     * map object.
     */
    private static String lineExpand(String keyString, Map properties) {

        //System.out.println("DEBUG : expand(String, Properties), keyString: " + keyString);

        //if ("".equals(keyString))
        //throw new Exception("empty key string");

        if (resolvesToLiteral(keyString)) {
            //System.out.println("DEBUG : keyString: " + keyString + " is a literal");
            return keyString;
        }
        //System.out.println("DEBUG : expand(String, Properties), keyString: " + keyString + " does not resolve to literal");

        // look for ${<propName>}'s
        StringTokenizer keyStringTokenizer = new StringTokenizer(keyString, "${}", true);

        // return buffer
        StringBuffer sb = new StringBuffer();

        if (keyStringTokenizer.countTokens() == 1) {
            // we are done here since there is nothing but the final to be expanded string (no real
            // parsing necessary.


            //String rtnString = (String)properties.get(keyString);
            //if (null == rtnString)

            if (!properties.containsKey(keyString)) {
                //System.out.println("unresolved property key : " + keyString);
                return "";
            }
            return (String) properties.get(keyString);

            //throw new Exception("value for property: " + keyString + " is null");
            //System.out.println("quick and dirty, returning: " + rtnString);
            //return rtnString;
        }


        // set all token indicators to false
        boolean dollar = false;
        boolean lp = false;
        boolean rp = false;


        while (keyStringTokenizer.hasMoreTokens()) {
            //System.out.println("got a token");

            // the token matching <propName> in ${<propName>}
            String expTok;

            // seen all tokens, so reset them for next occurrance
            if (dollar == true && lp == true && rp == true) {
                dollar = false;
                lp = false;
                rp = false;
            }

            // get next token based on any of $, {, } characters
            String tok = keyStringTokenizer.nextToken();

            // all token indicators false, means we have a passthru token, so just append to return buffer
            if (dollar == false && lp == false && rp == false) {
                if (!tok.equals("$")) {
                    sb.append(tok);
                }
            }

            // seen ${, but not } => we should have a token here to expand
            if (dollar == true && lp == true && rp == false) {
                // the prop to expand
                if (!tok.equals("}")) {
                    expTok = tok;
                    // append getProperty result of expTok
                    String expVal = (String) properties.get(expTok);
                    if (expVal == null) {
                        //throw new Exception("token expTok " + expTok + " is null");
                        //System.out.println("token expTok \"" + expTok + "\" is null");
                        sb.append("${").append(expTok).append("}");
                    }else{
                        sb.append(expVal);
                    }
                } else {
                    // the } token is now encountered
                    rp = true;
                    continue;
                }
            }

            // ensure we don't see $$, {$, }$
            if (tok.equals("$")) {
                if (dollar == true) {
                    //throw new Exception("parsing error: $$ invalid");
                    throw new PropertyUtilException("parsing error: $$ invalid");
                }
                if (lp == true) {
                    //throw new Exception("parsing error: {$ invalid");
                    throw new PropertyUtilException("parsing error: {$ invalid");
                }
                if (rp == true) {
                    //throw new Exception("parsing error: }$ invalid");
                    throw new PropertyUtilException("parsing error: }$ invalid");
                }
                dollar = true;
                continue;
            }

            // ensure $ is before {, and no {{ or }{
            if (tok.equals("{")) {
                if (dollar == false) {
                    //throw new Exception("parsing error: $ symbol must occur before { symbol");
                    throw new PropertyUtilException("parsing error: $ symbol must occur before { symbol");
                }
                if (lp == true) {
                    //throw new Exception("parsing error: {{ invalid");
                    throw new PropertyUtilException("parsing error: {{ invalid");
                }
                if (rp == true) {
                    //throw new Exception("parsing error: }{ invalid");
                    throw new PropertyUtilException("parsing error: }{ invalid");
                }
                lp = true;
                continue;
            }

            // ensure ${ is before }, and no }{
            if (tok.equals("}")) {
                if (dollar == false) {
                    //throw new Exception("parsing error: $ symbol must occur before } symbol");
                    throw new PropertyUtilException("parsing error: $ symbol must occur before } symbol");
                }
                if (lp == false) {
                    //throw new Exception("parsing error: { symbol must occur before } symbol");
                    throw new PropertyUtilException("parsing error: { symbol must occur before } symbol");
                }
                if (rp == true) {
                    //throw new Exception("parsing error: }} invalid");
                    throw new PropertyUtilException("parsing error: }} invalid");
                }
                rp = true;
                continue;
            }

        }

        if (null == sb.toString() || "".equals(sb.toString()))
        //throw new Exception("null string return:" + keyString);
            throw new PropertyUtilException("null string return:" + keyString);

        //System.out.println("PropertyUtil.expand(), returning: " + sb.toString());
        return sb.toString();

    }

    /**
     * determine if the provided keyString contains any references to properties
     */
    private static boolean resolvesToLiteral(String keyString) {

        //System.out.println("DEBUG : resolvesToLiteral(), entering");

        if (keyString.indexOf("${") > -1) {
            //System.out.println("DEBUG : resolvesToLiteral(), returning false");
            return false;
        }
        //System.out.println("DEBUG : resolvesToLiteral(), returning true");
        return true;

    }

    public static class PropertyUtilException extends RuntimeException {

        public PropertyUtilException(String msg) {
            super(msg);
        }
    }


}
