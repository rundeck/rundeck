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

/*
* DataContextUtils.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 16, 2010 6:27:40 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.DataOutput;
import com.dtolabs.rundeck.core.utils.Converter;
import lombok.Data;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataContextUtils provides methods for using a set of context data to substitute property references, generate
 * environment variables, and expand tokens in a file.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SharedDataContextUtils {
    public static final String PROPERTY_REF_REGEX = "\\$\\{([^\\s.]+)\\.([^\\s}]+)\\}";
    public static final String PROPERTY_VIEW_VAR_NODE_CHAR = "@";
    public static final String PROPERTY_VIEW_VAR_FINAL_CHAR = "}";
    /**
     * Match a variable in the form "1:group.key@node"
     */
    public static final String PROPERTY_VIEW_KEY_REGEX = "(?:(?<STEP>\\d+):)?(?<GROUP>[^\\s.]+)\\." +
                                                         "(?<KEY>[^\\s" +
                                                         PROPERTY_VIEW_VAR_NODE_CHAR +
                                                         "]+)" +
                                                         "(?:" +
                                                         PROPERTY_VIEW_VAR_NODE_CHAR +
                                                         "(?<QUAL>[^\\s" + PROPERTY_VIEW_VAR_FINAL_CHAR + "]+)" +
                                                         ")?";

    public static final String PROPERTY_SCRIPT_VAR_NODE_CHAR = "/";
    /**
     * Match a variable in the form "1:group.key/node"
     */
    public static final String PROPERTY_SCRIPT_TEMPLATE_KEY_REGEX = "(?:(?<STEP>\\d+):)?(?<GROUP>[^\\s.]+)\\." +
                                                                    "(?<KEY>[^\\s" +
                                                                    PROPERTY_SCRIPT_VAR_NODE_CHAR +
                                                                    "]+)" +
                                                                    "(?:" +
                                                                    PROPERTY_SCRIPT_VAR_NODE_CHAR +
                                                                    "(?<QUAL>[^\\s" +
                                                                    PROPERTY_VIEW_VAR_FINAL_CHAR +
                                                                    "]+)" +
                                                                    ")?";
    public static final Pattern PROPERTY_VAR_PATTERN = Pattern.compile("^" + PROPERTY_VIEW_KEY_REGEX + "$");
    public static final Pattern PROPERTY_SCRIPT_TEMPLATE_PATTERN = Pattern.compile("^" +
                                                                                   PROPERTY_SCRIPT_TEMPLATE_KEY_REGEX +
                                                                                   "$");
//    public static final String PROPERTY_VIEW_REF_REGEX = "\\$\\{" + PROPERTY_VIEW_KEY_REGEX + "\\}";
    public static final String PROPERTY_VIEW_REF_REGEX = "\\$\\{([^\\s}]+)\\}";
    public static final Pattern PROPERTY_REF_PATTERN = Pattern.compile(PROPERTY_REF_REGEX);
    private static final Pattern PROPERTY_VIEW_REF_PATTERN = Pattern.compile(PROPERTY_VIEW_REF_REGEX);


    /**
     *
     * @return A new output context
     */
    public static ReadableSharedContext outputContext(ContextView defaultView) {
        return new DataOutput(defaultView);
    }

    /**
     * @return A new context
     */
    public static WFSharedContext sharedContext() {
        return new WFSharedContext();
    }

    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param input             input string
     * @param data              data context map
     * @param converter         converter to encode/convert the expanded values
     * @param failOnUnexpanded  true to fail if a reference is not found
     * @param blankIfUnexpanded true to use blank if a reference is not found
     *
     * @return string with values substituted, or original string
     */
    public static <T extends ViewTraverse<T>> String replaceDataReferences(
            final String input,
            final MultiDataContext<T, DataContext> data,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            boolean failOnUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        if (null == data || null == input) {
            return input;
        }
        final Matcher m = PROPERTY_VIEW_REF_PATTERN.matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String variableRef = m.group(1);
            String value = expandVariable(
                    data,
                    viewMap,
                    SharedDataContextUtils::parseArgStringVariable,
                    variableRef
            );
            if (null != value) {
                if (null != converter) {
                    value = converter.convert(value);
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            } else if (failOnUnexpanded) {
                throw new DataContextUtils.UnresolvedDataReferenceException(input, m.group());
            } else if (blankIfUnexpanded) {
                m.appendReplacement(sb, "");
            } else {
                value = m.group(0);
                if (null != converter) {
                    value = converter.convert(value);
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final BiFunction<Integer, String, T> viewMap,
            final Function<String, VariableRef> parser,
            final String variableref
    )
    {
        //expand variable reference into step,group,key,node
        VariableRef variableRef = parser.apply(variableref);
        if (null == variableRef) {
            return null;
        }
        String step = variableRef.getStep();
        String group = variableRef.getGroup();
        String key = variableRef.getKey();
        String qual = variableRef.getNode();
        return expandVariable(data, viewMap, step, group, key, qual);
    }

     static VariableRef parseScriptTemplateVariable(String variableref) {
        final Matcher m = PROPERTY_SCRIPT_TEMPLATE_PATTERN.matcher(variableref);
        if (!m.matches()) {
            return null;
        }
        String step = m.group("STEP");
        String group = m.group("GROUP");
        String key = m.group("KEY");
        String qual = m.group("QUAL");
        return new VariableRef(variableref, step, group, key, qual);
    }

     static VariableRef parseArgStringVariable(String variableref) {
        final Matcher m = PROPERTY_VAR_PATTERN.matcher(variableref);
        if (!m.matches()) {
            return null;
        }
        String step = m.group("STEP");
        String group = m.group("GROUP");
        String key = m.group("KEY");
        String qual = m.group("QUAL");
        return new VariableRef(variableref, step, group, key, qual);
    }

    public static <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final BiFunction<Integer, String, T> viewMap,
            final String step,
            final String group,
            final String key,
            final String node
    )
    {
        Integer t = null;
        if (null != step) {
            try {
                t = Integer.parseInt(step);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        T view = viewMap.apply(t, node);
        boolean strict = null != step || null != node;
        return data.resolve(view, strict, group, key, null);
    }



    /**
     * Escape characters meaningful to bash shell unless the string is already surrounded in single quotes
     *
     * @param s string
     *
     * @return escaped string
     */
    public static String escapeShell(final String s) {
        if(null==s){
            return s;
        }
        if (s.startsWith("'") && s.endsWith("'")) {
            return s;
        } else if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.replaceAll("([\\\\`])", "\\\\$1");
        }
        return s.replaceAll("([&><|;\\\\`])", "\\\\$1");
    }

    /**
     * Escape characters meaningful to windows unless the string is already surrounded in single quotes
     *
     * @param s string
     *
     * @return escaped string
     */
    public static String escapeWindowsShell(final String s) {
        if (null == s) {
            return s;
        }
        if (s.startsWith("'") && s.endsWith("'")) {
            return s;
        } else if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.replaceAll("([`^])", "^$1");
        }
        return s.replaceAll("([&><|;^`])", "^$1");
    }


    /**
     * Copies the source file to a file, replacing the @key.X@ tokens with the values from the data
     * context
     *
     * @param script      source file path
     * @param dataContext input data context
     * @param framework   the framework
     * @param style       line ending style
     * @param destination destination file, or null to create a temp file
     *
     * @return the token replaced temp file, or null if an error occurs.
     *
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInScript(
            final String script,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination,
            final String nodeName
    )
            throws IOException
    {
        if (null == script) {
            throw new NullPointerException("script cannot be null");
        }
        //use ReplaceTokens to replace tokens within the content
        final Reader read = new StringReader(script);

        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(
                read,
                variable -> SharedDataContextUtils.expandVariable(
                        dataContext,
                        (stepNum, nodeCtx) -> ContextView.nodeStep(stepNum, nodeCtx != null ? nodeCtx : nodeName),
//                        DataContextUtils::parseArgStringVariable,
                        SharedDataContextUtils::parseScriptTemplateVariable,
                        variable
                ),
                true,
                '@',
                '@'
        );
        final File temp;
        if (null != destination) {
            ScriptfileUtils.writeScriptFile(null, null, replaceTokens, style, destination);
            temp = destination;
        } else {
            if (null == framework) {
                throw new NullPointerException("framework cannot be null");
            }
            temp = ScriptfileUtils.writeScriptTempfile(framework, replaceTokens, style);
        }
        ScriptfileUtils.setExecutePermissions(temp);
        return temp;
    }



    /**
     * Copies the source stream to a temp file or specific destination, replacing the @key.X@ tokens
     * with the values from the data context
     *
     * @param stream      source stream
     * @param dataContext input data context
     * @param framework   the framework
     * @param style       script file line ending style to use
     * @param destination destination file
     *
     * @return the token replaced temp file, or null if an error occurs.
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInStream(
            final InputStream stream,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination,
            final String nodeName
    )
            throws IOException
    {

        //use ReplaceTokens to replace tokens within the stream
        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(
                new InputStreamReader(stream),
                variable -> SharedDataContextUtils.expandVariable(
                        dataContext,
                        (stepNum, nodeCtx) -> ContextView.nodeStep(stepNum, nodeCtx != null ? nodeCtx : nodeName),
//                        DataContextUtils::parseArgStringVariable,
                        SharedDataContextUtils::parseScriptTemplateVariable,
                        variable
                ),
                true,
                '@',
                '@'
        );
        final File temp;
        if (null != destination) {
            ScriptfileUtils.writeScriptFile(null, null, replaceTokens, style, destination);
            temp = destination;
        } else {
            temp = ScriptfileUtils.writeScriptTempfile(framework, replaceTokens, style);
        }
        ScriptfileUtils.setExecutePermissions(temp);
        return temp;
    }


    @Data
    private static class VariableRef {
        private final String variableref;
        private final String step;
        private final String group;
        private final String key;
        private final String node;


    }
}
