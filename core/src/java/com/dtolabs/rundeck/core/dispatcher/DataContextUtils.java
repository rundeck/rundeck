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
import com.dtolabs.rundeck.core.tasks.net.ExtSSHExec;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
        if (null == data || data.isEmpty()) {
            return args;
        }
        if(null==args || args.length<1){
            return args;
        }
        final String[] newargs = new String[args.length];

        for (int i = 0 ; i < args.length ; i++) {
            final String arg = args[i];
            newargs[i] = replaceDataReferences(arg, data);
        }

        return newargs;
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
        final Pattern p = Pattern.compile("\\$\\{([^\\s.]+)\\.([^\\s}]+)\\}");
        final Matcher m = p.matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String key = m.group(1);
            final String nm = m.group(2);
            if (null!=key && null!=nm && null!=data.get(key) && null!= data.get(key).get(nm)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(escapeShell(data.get(key).get(nm))));
            }else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
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
     * Escape characters meaningful to bash shell unless the string is already surrounded in single quotes
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
     * Copies the source file to a temp file, replacing the @key.X@ tokens with the values from the data context
     *
     * @param sourceFile  source file path
     * @param dataContext input data context
     * @param framework   the framework
     *
     * @return the token replaced temp file, or null if an error occurs.
     */
    public static File replaceTokensInFile(final String sourceFile, final Map<String, Map<String, String>> dataContext,
                                           final Framework framework) throws IOException {
        return replaceTokensInFile(new File(sourceFile), dataContext, framework);
    }
    /**
     * Copies the source file to a temp file, replacing the @key.X@ tokens with the values from the data context
     *
     * @param sourceFile  source file 
     * @param dataContext input data context
     * @param framework   the framework
     *
     * @return the token replaced temp file, or null if an error occurs.
     */
    public static File replaceTokensInFile(final File sourceFile, final Map<String, Map<String, String>> dataContext,
                                           final Framework framework) throws IOException {
        //use ReplaceTokens to replace tokens within the file
        final ReplaceTokens replaceTokens = new ReplaceTokens(new InputStreamReader(new FileInputStream(sourceFile)));
        final Map<String, String> toks = flattenDataContext(dataContext);
        configureReplaceTokens(toks, replaceTokens);
        final File temp = ScriptfileUtils.writeScriptTempfile(framework, replaceTokens);
        ScriptfileUtils.setExecutePermissions(temp);
        return temp;
    }

    /**
     * Copies the source file to a temp file, replacing the @key.X@ tokens with the values from the data context
     *
     * @param script  source file path
     * @param dataContext input data context
     * @param framework   the framework
     *
     * @return the token replaced temp file, or null if an error occurs.
     */
    public static File replaceTokensInScript(final String script, final Map<String, Map<String, String>> dataContext,
                                           final Framework framework) throws IOException {
        //use ReplaceTokens to replace tokens within the content
        final Reader read = new StringReader(script);
        final ReplaceTokens replaceTokens = new ReplaceTokens(read);
        final Map<String, String> toks = flattenDataContext(dataContext);
        configureReplaceTokens(toks, replaceTokens);
        final File temp = ScriptfileUtils.writeScriptTempfile(framework, replaceTokens);
        ScriptfileUtils.setExecutePermissions(temp);
        return temp;
    }
    /**
     * Copies the source stream to a temp file, replacing the @key.X@ tokens with the values from the data context
     *
     * @param stream  source stream
     * @param dataContext input data context
     * @param framework   the framework
     *
     * @return the token replaced temp file, or null if an error occurs.
     */
    public static File replaceTokensInStream(final InputStream stream, final Map<String, Map<String, String>> dataContext,
                                           final Framework framework) throws IOException {

        //use ReplaceTokens to replace tokens within the stream
        final ReplaceTokens replaceTokens = new ReplaceTokens(new InputStreamReader(stream));
        final Map<String, String> toks = flattenDataContext(dataContext);
        configureReplaceTokens(toks, replaceTokens);
        final File temp = ScriptfileUtils.writeScriptTempfile(framework, replaceTokens);
        ScriptfileUtils.setExecutePermissions(temp);
        return temp;
    }

    /**
     * Flattens the data context into a simple key/value pair, using a "." separator for keys.
     *
     * @param dataContext
     *
     * @return
     */
    public static Map<String, String> flattenDataContext(final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> res = new HashMap<String, String>();
        if(null!=dataContext){
            for (final String s : dataContext.keySet()) {
                final String p = s + ".";
                final Map<String, String> map = dataContext.get(s);
                if(null!=map){
                    for (final String s1 : map.keySet()) {
                        if(null!=map.get(s1)){
                            res.put(p + s1, map.get(s1));
                        }
                    }
                }
            }
        }
        return res;
    }


    /**
     * Configure the ReplaceTokens for use by filterchain or straight use.  Adds tokens for "X" for each key in the
     * options data, uses the '@' begin/end tokens.
     *
     * @param data          input options
     * @param replaceTokens ReplaceTokens object.
     */
    public static void configureReplaceTokens(final Map<String, String> data, final ReplaceTokens replaceTokens) {
        replaceTokens.setBeginToken('@');
        replaceTokens.setEndToken('@');
        for (final String s : data.keySet()) {
            final ReplaceTokens.Token token = new ReplaceTokens.Token();
            token.setKey(s);
            token.setValue(data.get(s));
            replaceTokens.addConfiguredToken(token);
        }
    }

    /**
     * Convert option keys into environment variable names. Convert to uppercase and prepend "RD_"
     *
     * @param options the input options
     * @param prefix
     *
     * @return map of environment variable names to values, or null if options was null
     */
    public static Map<String, String> generateEnvVarsFromData(final Map<String, String> options, final String prefix) {
        if (null == options) {
            return null;
        }
        final HashMap<String, String> envs = new HashMap<String, String>();
        for (final String key : options.keySet()) {
            if (null != key && null != options.get(key)) {
                envs.put(generateEnvVarName(prefix + "." + key), options.get(key));
            }
        }
        return envs;
    }

    /**
     * Add embedded env elements for any included context data for the script
     *
     * @param execTask execTask
     */
    public static void addEnvVarsFromContextForExec(final ExecTask execTask,
                                                    final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> environment = generateEnvVarsFromContext(dataContext);

        if (null != environment) {
            for (final String key : environment.keySet()) {
                if (null != key && null != environment.get(key)) {
                    final Environment.Variable env = new Environment.Variable();
                    env.setKey(key);
                    env.setValue(environment.get(key));
                    execTask.addEnv(env);
                }
            }
        }
    }

    /**
     * add Env elements to pass environment variables to the ExtSSHExec
     *
     * @param environment environment variables
     * @param sshexecTask task
     */
    public static void addEnvVars( final ExtSSHExec sshexecTask, final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> environment = generateEnvVarsFromContext(dataContext);
        if (null != environment) {
            for (final String key : environment.keySet()) {
                if (null != key && null != environment.get(key)) {
                    final Environment.Variable env = new Environment.Variable();
                    env.setKey(key);
                    env.setValue(environment.get(key));
                    sshexecTask.addEnv(env);
                }
            }
        }
    }

    /**
     * Generate a set of key value pairs to use for environment variables, from the context data set
     */
    public static Map<String, String> generateEnvVarsFromContext(final Map<String, Map<String, String>> dataContext) {
        final Map<String, String> context = new HashMap<String, String>();
        if (null != dataContext) {
            for (final String dataKey : dataContext.keySet()) {

                final Map<String, String> data = dataContext.get(dataKey);
                final Map<String, String> envs = generateEnvVarsFromData(data, dataKey);
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
        if(null!=nodeentry){
            data.put("name", notNull(nodeentry.getNodename()));
            data.put("hostname", notNull(nodeentry.getHostname()));
            data.put("os-name", notNull(nodeentry.getOsName()));
            data.put("os-version", notNull(nodeentry.getOsVersion()));
            data.put("os-arch", notNull(nodeentry.getOsArch()));
            data.put("os-family", notNull(nodeentry.getOsFamily()));
            data.put("username", notNull(nodeentry.getUsername()));
            data.put("description", notNull(nodeentry.getDescription()));
            data.put("tags", null != nodeentry.getTags() ? join(nodeentry.getTags(), ",") : "");
            data.put("type", notNull(nodeentry.getType()));
            //include setting data
            if(null!=nodeentry.getSettings()){
                for (final String name : nodeentry.getSettings().keySet()) {
                    if(null!=nodeentry.getSettings().get(name)) {
                        data.put("setting." + name, notNull(nodeentry.getSettings().get(name)));
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
