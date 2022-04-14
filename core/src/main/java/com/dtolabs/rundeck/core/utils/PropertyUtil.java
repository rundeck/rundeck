/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;


import org.apache.tools.ant.Project;

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
     * simple state for parsing "${token}" for expansion
     */
    static private enum State {
        /**
         * init state
         */
        NoToken(null),
        /**
         * "$" seen
         */
        Dollar(NoToken),
        /**
         * "${" seen
         */
        DollarLeftbracket(Dollar),
        /**
         * "${prop" seen
         */
        PropertyName(DollarLeftbracket),
        /**
         * "${prop}" seen
         */
        Final(PropertyName);

        /**
         * allow transition from this previous state
         */
        final State allowed;

        State(State allowed) {
            this.allowed = allowed;
        }

        /**
         * @param token input string
         * @return next state for input token
         */
        State newStateForToken(String token) {
            if ("$".equals(token)) {
                return transitionTo(Dollar);
            } else if ("{".equals(token)) {
                return transitionTo(DollarLeftbracket);
            } else if ("}".equals(token)) {
                return transitionTo(Final);
            } else {
                return transitionTo(PropertyName);
            }
        }

        /**
         * @param previous from state
         * @return true if this state accepts transition from previous state
         */
        boolean allowedFrom(State previous) {
            return null == this.allowed || this.allowed == previous;
        }

        /**
         * Transition to the next state
         * @param next next state
         * @return next state if acceptable to transition there, otherwise None
         */
        State transitionTo(State next) {
            if (next.allowedFrom(this)) {
                return next;
            }
            return NoToken;
        }
    }

    /**
     * parse the given keyString and expand each reference to a property from the given map object.
     */
    private static String lineExpand(String keyString, Map properties) {
        if (resolvesToLiteral(keyString)) {
            return keyString;
        }
        // look for ${<propName>}'s
        StringTokenizer keyStringTokenizer = new StringTokenizer(keyString, "${}", true);

        StringBuilder output = new StringBuilder();
        State state = State.NoToken;
        String propName = null;
        StringBuilder tokenbuff = new StringBuilder();

        while (keyStringTokenizer.hasMoreTokens()) {
            String nextToken = keyStringTokenizer.nextToken();
            tokenbuff.append(nextToken);
            State newState = state.newStateForToken(nextToken);
            if (newState == State.PropertyName) {
                propName = nextToken;
            } else if (newState == State.Final) {
                String expVal = (String) properties.get(propName);
                if (expVal != null) {
                    tokenbuff = new StringBuilder(expVal);
                }
                propName = null;
                newState = State.NoToken;
            }
            if (newState == State.NoToken) {
                output.append(tokenbuff);
                tokenbuff = new StringBuilder();
            }
            state = newState;
        }

        output.append(tokenbuff);

        return output.toString();
    }

    /**
     * determine if the provided keyString contains any references to properties
     */
    private static boolean resolvesToLiteral(String keyString) {
        return !keyString.contains("${");
    }

    public static class PropertyUtilException extends RuntimeException {

        public PropertyUtilException(String msg) {
            super(msg);
        }
    }


}
