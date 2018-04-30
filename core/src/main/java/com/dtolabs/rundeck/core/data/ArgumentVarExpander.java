/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author greg
 * @since 5/31/17
 */
public class ArgumentVarExpander extends BaseVarExpander {
    public static final String PROPERTY_VIEW_VAR_NODE_CHAR = "@";
    public static final String PROPERTY_VIEW_VAR_GLOB_CHAR = "*";

    /**
     * Match a variable in the form "1:group.key@node"
     * Match a glob expansion in the form "1:group.key*(format)?"
     */
    public static final String PROPERTY_VIEW_KEY_REGEX =
            "^" +
            "(?:(?<STEP>\\d+):)?" +
            "(?<GROUP>[^\\s.]+)\\." +
            "(?<KEY>[^\\s"
            + PROPERTY_VIEW_VAR_NODE_CHAR
            + PROPERTY_VIEW_VAR_GLOB_CHAR
            + "]+)" +

            "(?:" +

            "(?:" +
            PROPERTY_VIEW_VAR_NODE_CHAR +
            "(?<QUAL>[^\\s]+)" +
            ")" +

            "|" +

            "(?<GLOB>" +
            Pattern.quote(PROPERTY_VIEW_VAR_GLOB_CHAR) +
            "[^\\s]*" +
            ")" +

            ")?"
            + "$";

    public static final Pattern PROPERTY_VAR_PATTERN = Pattern.compile(PROPERTY_VIEW_KEY_REGEX);

    public VariableRef parseVariable(String variableref) {
        final Matcher m = PROPERTY_VAR_PATTERN.matcher(variableref);
        if (!m.matches()) {
            return null;
        }
        String step = m.group("STEP");
        String group = m.group("GROUP");
        String key = m.group("KEY");
        String qual = m.group("QUAL");
        String glob = m.group("GLOB");
        String globstr = null;
        if (glob != null && glob.startsWith("*") && glob.length() > 1) {
            globstr = glob.substring(1);
        }
        return new VariableRef(variableref, step, group, key, qual, glob != null, globstr);
    }

}
