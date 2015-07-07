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
* DataContextUtils.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 16, 2010 6:27:40 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.utils.Converter;
import org.apache.commons.collections.Predicate;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataContextUtils provides methods for using a set of context data to substitute property references, generate
 * environment variables, and expand tokens in a file.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class DataContextUtils {
    /**
     * Prefix string used for all environment variable names
     */
    public static final String ENV_VAR_PREFIX = "RD_";
    public static final String PROPERTY_REF_REGEX = "\\$\\{([^\\s.]+)\\.([^\\s}]+)\\}";

    /**
     * Return a converter that can expand the property references within a string
     *
     * @param data property context data
     * @return a Converter to expand property values within a string
     */
    public static Converter<String,String> replaceDataReferencesConverter(final Map<String, Map<String, String>> data) {
        return replaceDataReferencesConverter(data, null, false);
    }

    /**
     * Return a converter that can expand the property references within a string
     * @param data property context data
     * @param converter secondary converter to apply to property values before replacing in a string
     * @param failOnUnexpanded if true, fail if a property value cannot be expanded
     * @return a Converter to expand property values within a string
     */
    public static Converter<String,String> replaceDataReferencesConverter(final Map<String, Map<String, String>> data,
            final Converter<String, String> converter, final boolean failOnUnexpanded){
        return new Converter<String, String>() {
            @Override
            public String convert(String s) {
                return replaceDataReferences(s,data, converter, failOnUnexpanded);
            }
        };
    }


    /**
     * evaluates to true if a string contains a property reference
     */
    public static final Predicate stringContainsPropertyReferencePredicate = new Predicate() {
        Pattern match = Pattern.compile(PROPERTY_REF_REGEX);
        @Override
        public boolean evaluate(Object o) {
            return ((String) o).contains("${") && match.matcher((String) o).matches();
        }
    };

    /**
     * A converter which replaces '${option.*}' with blank when replacing data references
     */
    public static final Converter<String,String> replaceMissingOptionsWithBlank = new Converter<String, String>() {
        Pattern optionPattern = Pattern.compile("^"+Pattern.quote("${option.")+"[^}\\s]+?"+Pattern.quote("}")+"$");
        @Override
        public String convert(String s) {
            return optionPattern.matcher(s).matches() ? "" : s;
        }
    };
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param args argument string array
     * @param data data context
     * @param converter converter
     * @param failIfUnexpanded true to fail if property is not found
     * @return string array with replaced embedded properties
     */
    public static String[] replaceDataReferences(final String[] args, final Map<String, Map<String, String>> data, Converter<String, String> converter, boolean failIfUnexpanded) {
        return replaceDataReferences(args, data, converter, failIfUnexpanded, false);
    }
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param args argument string array
     * @param data data context
     * @param converter converter
     * @param failIfUnexpanded true to fail if property is not found
     * @param blankIfUnexpanded true to use blank if property is not found
     * @return string array with replaced embedded properties
     */
    public static String[] replaceDataReferences(final String[] args, final Map<String, Map<String, String>> data,
            Converter<String, String> converter, boolean failIfUnexpanded, boolean blankIfUnexpanded) {
        if (null == data || data.isEmpty()) {
            return args;
        }
        if (null == args || args.length < 1) {
            return args;
        }
        final String[] newargs = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            newargs[i] = replaceDataReferences(arg, data, converter, failIfUnexpanded, blankIfUnexpanded);
        }

        return newargs;
    }
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param args argument string array
     * @param data data context
     *
     * @return string array with replaced embedded properties
     */
    public static String[] replaceDataReferences(final String[] args, final Map<String, Map<String, String>> data) {
        return replaceDataReferences(args, data, null, false);
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
    public static Map<String, Object> replaceDataReferences(final Map<String, Object> input,
                                                            final Map<String, Map<String, String>> data) {
        final HashMap<String, Object> output = new HashMap<String, Object>();
        for (final String s : input.keySet()) {
            Object o = input.get(s);
            output.put(s, replaceDataReferencesInObject(o, data));
        }
        return output;
    }
    private static Object replaceDataReferencesInObject(Object o, final Map<String, Map<String, String>> data){
        if (o instanceof String) {
            return replaceDataReferences((String) o, data);
        } else if (o instanceof Map) {
            Map<String, Object> sub = (Map<String, Object>) o;
            return replaceDataReferences(sub, data);
        } else if (o instanceof Collection) {
            ArrayList result = new ArrayList();
            Collection r = (Collection)o;
            for (final Object o1 : r) {
                result.add(replaceDataReferencesInObject(o1, data));
            }
            return result;
        }else{
            return o;
        }
    }

    /**
     * Return the resolved value from the context
     * @param data data context
     * @param group group name
     * @param key key name
     * @return resolved value or null
     */
    public static String resolve(
            final Map<String, Map<String, String>> data, final String group,
            final String key
    )
    {
        return resolve(data, group, key, null);
    }
    /**
     * Return the resolved value from the context
     * @param data data context
     * @param group group name
     * @param key key name
     * @param defaultValue default if the value is not resolvable
     * @return resolved value or default
     */
    public static String resolve(
            final Map<String, Map<String, String>> data, final String group,
            final String key,
            final String defaultValue
    )
    {
        return null != data && null != data.get(group) && null != data.get(group).get(key)
        ? data.get(group).get(key)
        : defaultValue;
    }
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param input input string
     * @param data  data context map
     *
     * @return string with values substituted, or original string
     */
    public static String replaceDataReferences(final String input, final Map<String, Map<String, String>> data) {
        return replaceDataReferences(input, data, null, false);
    }

    /**
     * Merge one context onto another by adding or replacing values.
     * @param targetContext the target of the merge
     *                @param newContext context to merge
     * @return merged data
     */
    public static Map<String, Map<String, String>> merge(final Map<String, Map<String, String>> targetContext,
                                                         final Map<String, Map<String, String>> newContext) {

        final HashMap<String, Map<String, String>> result = deepCopy(targetContext);
        for (final Map.Entry<String, Map<String, String>> entry : newContext.entrySet()) {
            if (!targetContext.containsKey(entry.getKey())) {
                result.put(entry.getKey(), new HashMap<String, String>());
            } else {
                result.put(entry.getKey(), new HashMap<String, String>(targetContext.get(entry.getKey())));
            }
            result.get(entry.getKey()).putAll(entry.getValue());
        }
        return result;
    }

    private static HashMap<String, Map<String, String>> deepCopy(Map<String, Map<String, String>> context) {
        HashMap<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        for (final Map.Entry<String, Map<String, String>> entry : context.entrySet()) {
            map.put(entry.getKey(), new HashMap<String, String>(entry.getValue()));
        }
        return map;
    }

    /**
     * Indicates that the value of a property reference could not be resolved.
     */
    public static class UnresolvedDataReferenceException extends RuntimeException{
        private String template;
        private String referenceName;

        public UnresolvedDataReferenceException(final String template, final String referenceName) {
            super("Property " + referenceName + " could not be resolved in template: " + template);
            this.template = template;
            this.referenceName = referenceName;
        }

        public String getTemplate() {
            return template;
        }

        public String getReferenceName() {
            return referenceName;
        }
    }
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     *
     * @param input input string
     * @param data  data context map
     *              @param converter converter to encode/convert the expanded values
     *
     * @param failOnUnexpanded true to fail if a reference is not found
     * @return string with values substituted, or original string
     */
    public static String replaceDataReferences(final String input, final Map<String, Map<String, String>> data,
                                               final Converter<String, String> converter, boolean failOnUnexpanded) {
        return replaceDataReferences(input, data, converter, failOnUnexpanded, false);
    }
    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     *
     * @param input input string
     * @param data  data context map
     *              @param converter converter to encode/convert the expanded values
     *
     * @param failOnUnexpanded true to fail if a reference is not found
     * @param blankIfUnexpanded true to use blank if a reference is not found
     *
     * @return string with values substituted, or original string
     */
    public static String replaceDataReferences(final String input, final Map<String, Map<String, String>> data,
                                               final Converter<String, String> converter, boolean failOnUnexpanded, boolean blankIfUnexpanded) {
        if(null==data || null==input){
            return input;
        }
        final Pattern p = Pattern.compile(PROPERTY_REF_REGEX);
        final Matcher m = p.matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String key = m.group(1);
            final String nm = m.group(2);
            if (null!=key && null!=nm && null!=data.get(key) && null!= data.get(key).get(nm)) {
                String value = data.get(key).get(nm);
                if (null != converter) {
                    value = converter.convert(value);
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }else if (failOnUnexpanded && null != key && null != nm && (null == data.get(key) || null == data.get(key)
                .get(nm))) {
                throw new UnresolvedDataReferenceException(input, m.group());
            }else if(blankIfUnexpanded) {
                m.appendReplacement(sb, "");
            } else {
                String value = m.group(0);
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
     * Copies the source file to a temp file, replacing the @key.X@ tokens with the values from the
     * data context
     *
     * @param sourceFile  source file
     * @param dataContext input data context
     * @param framework   the framework
     * @param style       line ending style
     *
     * @return the token replaced temp file, or null if an error occurs.
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInFile(
            final File sourceFile,
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style
    ) throws IOException
    {
        return replaceTokensInFile(sourceFile, dataContext, framework, style, null);
    }

    /**
     * Copies the source file to a destination file, replacing the @key.X@ tokens with the values
     * from the data context
     *
     * @param sourceFile  source file
     * @param dataContext input data context
     * @param framework   the framework
     * @param style       line ending style
     * @param destination destination file, or null to create a new temp file
     *
     * @return the token replaced file, or null if an error occurs.
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInFile(
            final File sourceFile,
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination
    ) throws IOException
    {
        //use ReplaceTokens to replace tokens within the file
        final Map<String, String> toks = flattenDataContext(dataContext);
        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(
                new InputStreamReader(
                        new FileInputStream
                                (sourceFile)
                ), toks, true, '@', '@'
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

    /**
     * Copies the source file to a temp file, replacing the @key.X@ tokens with the values from the
     * data context
     *
     * @param script      source file path
     * @param dataContext input data context
     * @param framework   the framework
     * @param style       line ending style
     *
     * @return the token replaced temp file, or null if an error occurs.
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInScript(
            final String script,
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style
    )
            throws IOException
    {
        return replaceTokensInScript(script, dataContext, framework, style, null);
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
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInScript(
            final String script,
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination
    )
            throws IOException
    {
        if (null == script) {
            throw new NullPointerException("script cannot be null");
        }
        if (null == framework) {
            throw new NullPointerException("framework cannot be null");
        }
        //use ReplaceTokens to replace tokens within the content
        final Reader read = new StringReader(script);
        final Map<String, String> toks = flattenDataContext(dataContext);
        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(read, toks, true, '@', '@');
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
    /**
     * Copies the source stream to a temp file, replacing the @key.X@ tokens with the values from the data context
     *
     * @param stream  source stream
     * @param dataContext input data context
     * @param framework   the framework
     * @param style script file line ending style to use
     *
     * @return the token replaced temp file, or null if an error occurs.
     * @throws java.io.IOException on io error
     */
    public static File replaceTokensInStream(
            final InputStream stream,
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style
    )
            throws IOException
    {
        return replaceTokensInStream(stream, dataContext, framework, style, null);
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
            final Map<String, Map<String, String>> dataContext,
            final Framework framework,
            final ScriptfileUtils.LineEndingStyle style,
            final File destination
    )
            throws IOException
    {

        //use ReplaceTokens to replace tokens within the stream
        final Map<String, String> toks = flattenDataContext(dataContext);
        final ReplaceTokenReader replaceTokens = new ReplaceTokenReader(
                new InputStreamReader(stream),
                toks,
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

    /**
     * @return Flattens the data context into a simple key/value pair, using a "." separator for keys.
     *
     * @param dataContext data
     */
    public static Map<String, String> flattenDataContext(final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> res = new HashMap<String, String>();
        if(null!=dataContext){
            for (final Map.Entry<String, Map<String, String>> entry : dataContext.entrySet()) {

                final String p = entry.getKey() + ".";
                final Map<String, String> map = entry.getValue();
                if(null!=map){
                    for (final Map.Entry<String, String> entry2 : map.entrySet()) {
                        if(null!=map.get(entry2.getKey())){
                            res.put(p + entry2.getKey(), entry2.getValue());
                        }
                    }
                }
            }
        }
        return res;
    }



    /**
     * Convert option keys into environment variable names. Convert to uppercase and prepend "RD_"
     *
     * @param options the input options
     * @param prefix prefix
     *
     * @return map of environment variable names to values, or null if options was null
     */
    public static Map<String, String> generateEnvVarsFromData(final Map<String, String> options, final String prefix) {
        if (null == options) {
            return null;
        }
        final HashMap<String, String> envs = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : options.entrySet()) {
            if (null != entry.getKey() && null != entry.getValue()) {
                envs.put(generateEnvVarName(prefix + "." + entry.getKey()), entry.getValue());
            }
        }
        return envs;
    }

    /**
     * Add embedded env elements for any included context data for the script
     *
     * @param dataContext data
     * @param execTask execTask
     */
    public static void addEnvVarsFromContextForExec(final ExecTask execTask,
                                                    final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> environment = generateEnvVarsFromContext(dataContext);

        if (null != environment) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String key = entry.getKey();
                if (null != key && null != entry.getValue()) {
                    final Environment.Variable env = new Environment.Variable();
                    env.setKey(key);
                    env.setValue(entry.getValue());
                    execTask.addEnv(env);
                }
            }
        }
    }

    /**
     * Can be configured with environment variables
     */
    public static interface EnvironmentConfigurable{

        /**
         * Add an environment variable
         * @param env env variable
         */
        void addEnv(Environment.Variable env);
    }
    /**
     * add Env elements to pass environment variables to the ExtSSHExec
     *
     * @param sshexecTask task
     * @param dataContext data
     */
    public static void addEnvVars( final EnvironmentConfigurable sshexecTask, final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> environment = generateEnvVarsFromContext(dataContext);
        if (null != environment) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String key = entry.getKey();
                if (null != key && null != entry.getValue()) {
                    final Environment.Variable env = new Environment.Variable();
                    env.setKey(key);
                    env.setValue(entry.getValue());
                    sshexecTask.addEnv(env);
                }
            }
        }
    }

    /**
     * @return Generate a set of key value pairs to use for environment variables, from the context data set
     * @param dataContext data
     */
    public static Map<String, String> generateEnvVarsFromContext(final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> context = new HashMap<String, String>();
        if (null != dataContext) {
            for (final Map.Entry<String, Map<String, String>> entry : dataContext.entrySet()) {
                final Map<String, String> envs = generateEnvVarsFromData(entry.getValue(), entry.getKey());
                if (null != envs) {
                    context.putAll(envs);
                }
            }
        }
        return context;
    }

    /**
     * Generate environment variable name from option name
     *
     * @param key key
     *
     * @return env var name
     */
    public static String generateEnvVarName(final String key) {
        return ENV_VAR_PREFIX + key.toUpperCase().replaceAll("[^a-zA-Z0-9_]", "_");
    }


    /**
     * Return a new context with appended data set
     *
     * @param key     data key
     * @param data    data content
     * @param context original context
     *
     * @return new context containing original data and the new dataset
     */
    public static Map<String, Map<String, String>> addContext(final String key, final Map<String, String> data,
                                                              final Map<String, Map<String, String>> context) {
        final Map<String, Map<String, String>> newdata = new HashMap<String, Map<String, String>>();
        if(null!=context){
            newdata.putAll(context);
        }
        newdata.put(key, data);
        return newdata;
    }

    /**
     * Generate a dataset for a INodeEntry
     * @param nodeentry node
     * @return dataset
     */
    public static Map<String, String> nodeData(final INodeEntry nodeentry) {
        final HashMap<String, String> data = new HashMap<String, String>();
        if(null!=nodeentry) {
            HashSet<String> skipProps = new HashSet<String>();
            skipProps.addAll(Arrays.asList("nodename", "osName", "osVersion", "osArch", "osFamily"));
            data.put("name", notNull(nodeentry.getNodename()));
            data.put("hostname", notNull(nodeentry.getHostname()));
            data.put("os-name", notNull(nodeentry.getOsName()));
            data.put("os-version", notNull(nodeentry.getOsVersion()));
            data.put("os-arch", notNull(nodeentry.getOsArch()));
            data.put("os-family", notNull(nodeentry.getOsFamily()));
            data.put("username", notNull(nodeentry.getUsername()));
            data.put("description", notNull(nodeentry.getDescription()));
            data.put("tags", null != nodeentry.getTags() ? join(nodeentry.getTags(), ",") : "");
            //include attributes data
            if (null != nodeentry.getAttributes()) {
                for (final String name : nodeentry.getAttributes().keySet()) {
                    if (null != nodeentry.getAttributes().get(name) && !data.containsKey(name) && !skipProps.contains(
                        name)) {

                        data.put(name, notNull(nodeentry.getAttributes().get(name)));
                    }
                }
            }
        }
        return data;
    }

    /**
     * Return the original string or an empty string if the input is null
     *
     * @param value original value
     *
     * @return a non-null string, substituting "" for null
     */
    private static String notNull(final String value) {
        return null != value ? value : "";
    }

    /**
     * Join a list of strings into a single string with a separator
     *
     * @param list      strings
     * @param separator separator
     *
     * @return joined string
     */
    public static String join(final Collection<String> list, final String separator) {
        final StringBuilder sb = new StringBuilder();
        for (final String s : list) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(s);
        }
        return sb.toString();
    }

}
