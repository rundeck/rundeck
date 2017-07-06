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
package com.dtolabs.rundeck.core.data;

import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.DataOutput;
import com.dtolabs.rundeck.core.utils.Converter;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
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
     * @param args              input string array
     * @param data              data context map
     * @param converter         converter to encode/convert the expanded values
     * @param failIfUnexpanded  true to fail if a reference is not found
     * @param blankIfUnexpanded true to use blank if a reference is not found
     *
     * @return string with values substituted, or original string
     */
    public static <T extends ViewTraverse<T>> String[] replaceDataReferences(
            final String[] args,
            final MultiDataContext<T, DataContext> data,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            boolean failIfUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        if (null == data) {
            return args;
        }
        if (null == args || args.length < 1) {
            return args;
        }
        final String[] newargs = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            newargs[i] = replaceDataReferences(
                    arg,
                    data,
                    currentContext,
                    viewMap,
                    converter,
                    failIfUnexpanded,
                    blankIfUnexpanded
            );
        }

        return newargs;
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
            final T currentContext,
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
        ArgumentVarExpander argExpander = new ArgumentVarExpander();
        while (m.find()) {
            final String variableRef = m.group(1);
            String value = argExpander.expandVariable(
                    data,
                    currentContext,
                    viewMap,
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
     * @param style       line ending style
     * @param destination destination file, or null to create a temp file
     *
     * @throws java.io.IOException on io error
     */
    public static void replaceTokensInScript(
            final String script,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination,
            final String nodeName
    )
            throws IOException
    {
        if (null == script) {
            throw new NullPointerException("script cannot be null");
        }
        replaceTokensInReader(
                new StringReader(script),
                dataContext,
                style,
                destination, nodeName
        );
    }



    /**
     * Copies the source stream to a temp file or specific destination, replacing the @key.X@ tokens
     * with the values from the data context
     *
     * @param stream      source stream
     * @param dataContext input data context
     * @param style       script file line ending style to use
     * @param destination destination file
     *
     * @throws java.io.IOException on io error
     */
    public static void replaceTokensInStream(
            final InputStream stream,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination,
            final String nodeName
    )
            throws IOException
    {
        if (null == stream) {
            throw new NullPointerException("stream cannot be null");
        }
        replaceTokensInReader(
                new InputStreamReader(stream),
                dataContext,
                style,
                destination, nodeName
        );
    }

    /**
     * Copies the source stream to a temp file or specific destination, replacing the @key.X@ tokens
     * with the values from the data context
     *
     * @param reader      reader
     * @param dataContext input data context
     * @param style       script file line ending style to use
     * @param destination destination file
     *
     * @throws java.io.IOException on io error
     */
    public static void replaceTokensInReader(
            final Reader reader,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination,
            final String nodeName
    )
            throws IOException
    {

        //use ReplaceTokens to replace tokens within the stream
        ScriptVarExpander scriptVarExpander = new ScriptVarExpander();
        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(
                reader,
                variable -> scriptVarExpander.expandVariable(
                        dataContext,
                        ContextView.node(nodeName),
                        ContextView::nodeStep,
                        variable
                ),
                true,
                '@',
                '@'
        );
        ScriptfileUtils.writeScriptFile(null, null, replaceTokens, style, destination);
    }

    /**
     * Recursively replace data references in the values in a map which contains either string, collection or Map
     * values.
     *
     * @param input input map
     * @param data  context data
     *
     * @return Map with all string values having references replaced
     */
    public static <T extends ViewTraverse<T>> Map<String, Object> replaceDataReferences(
            final Map<String, Object> input,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            final MultiDataContext<T, DataContext> data
    )
    {
        return replaceDataReferences(input, currentContext, viewMap, converter, data, false, false);
    }

    /**
     * Recursively replace data references in the values in a map which contains either string, collection or Map
     * values.
     *
     * @param input input map
     * @param data  context data
     *
     * @return Map with all string values having references replaced
     */
    public static <T extends ViewTraverse<T>> Map<String, Object> replaceDataReferences(
            final Map<String, Object> input,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            final MultiDataContext<T, DataContext> data,
            boolean failOnUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        final HashMap<String, Object> output = new HashMap<>();
        for (final String s : input.keySet()) {
            Object o = input.get(s);
            output.put(
                    s,
                    replaceDataReferencesInObject(o,
                                                  currentContext,
                                                  viewMap,
                                                  converter,
                                                  data,
                                                  failOnUnexpanded,
                                                  blankIfUnexpanded
                    )
            );
        }
        return output;
    }

    public static <T extends ViewTraverse<T>> Object replaceDataReferencesInObject(
            Object o,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            final MultiDataContext<T, DataContext> data
    )
    {
        return replaceDataReferencesInObject(o, currentContext, viewMap, converter, data, false, false);
    }

    public static <T extends ViewTraverse<T>> Object replaceDataReferencesInObject(
            Object o,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final Converter<String, String> converter,
            final MultiDataContext<T, DataContext> data,
            boolean failOnUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        if (o instanceof String) {
            return replaceDataReferences(
                    (String) o,
                    data,
                    currentContext,
                    viewMap,
                    converter,
                    failOnUnexpanded,
                    blankIfUnexpanded
            );
        } else if (o instanceof Map) {
            Map<String, Object> sub = (Map<String, Object>) o;
            return replaceDataReferences(
                    sub,
                    currentContext,
                    viewMap,
                    converter,
                    data,
                    failOnUnexpanded,
                    blankIfUnexpanded
            );
        } else if (o instanceof Collection) {
            ArrayList result = new ArrayList();
            for (final Object o1 : (Collection) o) {
                result.add(replaceDataReferencesInObject(
                        o1,
                        currentContext,
                        viewMap,
                        converter,
                        data,
                        failOnUnexpanded,
                        blankIfUnexpanded
                ));
            }
            return result;
        } else {
            return o;
        }
    }

}
