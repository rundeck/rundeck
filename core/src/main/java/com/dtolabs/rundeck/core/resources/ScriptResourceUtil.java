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

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.proxy.DefaultSecretBundle;
import com.dtolabs.rundeck.core.execution.proxy.SecretBundle;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.core.utils.MapData;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;
import org.rundeck.storage.api.Resource;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class for executing an external script and parsing the output in a particular format, returning INodeSet
 * data.
 */
class ScriptResourceUtil {
    public static INodeSet executeScript(
            final File scriptfile,
            final String scriptargs,
            final String[] scriptargsarray,
            final String scriptinterpreter,
            final String pluginname,
            final Map<String, Map<String, String>> dataContext,
            final String fileformat,
            final Framework framework,
            final String project,
            final Logger logger,
            final boolean interpreterArgsQuoted
    ) throws
            ResourceModelSourceException
    {
        /*
        String dirstring = null;
        dirstring = plugin.get
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring) {
            workingdir = new File(dirstring);
        }*/

        File workingdir = null;

        int result = -1;
        boolean success = false;
        Streams.StreamCopyThread errthread = null;
        Streams.StreamCopyThread outthread = null;

        final File destinationTempFile;
        try {
            destinationTempFile = File.createTempFile("script-plugin", ".resources");
            destinationTempFile.deleteOnExit();
        } catch (IOException e) {
            throw new ResourceModelSourceException(e);
        }
        logger.info("Tempfile: " + destinationTempFile.getAbsolutePath());
        final Process exec;
        try {
            if (null != scriptinterpreter) {
                exec = execShellScript(
                        logger,
                        workingdir,
                        scriptfile,
                        scriptargs,
                        scriptargsarray,
                        dataContext,
                        dataContext,
                        scriptinterpreter,
                        pluginname,
                        interpreterArgsQuoted
                );
            } else {
                exec = execScript(
                        logger,
                        workingdir,
                        scriptfile,
                        scriptargs,
                        scriptargsarray,
                        dataContext,
                        dataContext,
                        pluginname
                );
            }
        } catch (IOException e) {
            throw new ResourceModelSourceException("Script execution could not start: " + e.getMessage(), e);
        }
        try {
            exec.getOutputStream().close();
            try (FileOutputStream fileOutputStream = new FileOutputStream(destinationTempFile)) {
                errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
                outthread = Streams.copyStreamThread(exec.getInputStream(), fileOutputStream);
                errthread.start();
                outthread.start();
                result = exec.waitFor();
                System.err.flush();
                fileOutputStream.flush();
                errthread.join();
                outthread.join();
                exec.getErrorStream().close();
                exec.getInputStream().close();
                success = 0 == result;
            }
        } catch (InterruptedException e) {
            logger.error("[" + pluginname + "]: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException e) {
            logger.error("[" + pluginname + "]: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        logger.debug("[" + pluginname + "]: result code: " + result + ", success: " + success);
        if (null != outthread && null != outthread.getException()) {
            logger.error(
                    "[" + pluginname + "]: stream copy error: " + outthread.getException().getMessage(),
                    outthread.getException()
            );
        }
        if (null != errthread && null != errthread.getException()) {
            logger.error(
                    "[" + pluginname + "]: stream copy error: " + errthread.getException().getMessage(),
                    errthread.getException()
            );
        }
        try {
            if (!success) {
                throw new ResourceModelSourceException("Script execution failed with result: " + result);
            }

            if (destinationTempFile.isFile() && destinationTempFile.length() > 0) {
                try {
                    return FileResourceModelSource.parseFile(destinationTempFile, fileformat, framework,
                                                             project
                    );
                } catch (ConfigurationException e) {
                    throw new ResourceModelSourceException(e);
                }
            } else {
                throw new ResourceModelSourceException("Script output was empty");
            }
        } finally {
            if (!destinationTempFile.delete()) {
                logger.warn(
                        "[" + pluginname + "]: could not delete temp file: " + destinationTempFile.getAbsolutePath());
            }
        }
    }

    /**
     * Execute a process by invoking a specified shell command and passing the arguments to the shell.
     *
     * @param logger         logger
     * @param workingdir     working dir
     * @param scriptfile     file
     * @param scriptargs     arguments to the shell
     * @param scriptargsarr
     * @param envContext     Environment variable context
     * @param newDataContext context data to replace in the scriptargs
     * @param interpreter    the remote shell script, which will be split on whitespace
     * @param logName        name of plugin to use in logging
     * @param interpreterArgsQuoted if true, quote the file+args as a single argument to the interpreter
     * @return process
     * @throws IOException on io error
     */
    static Process execShellScript(
            final Logger logger,
            final File workingdir,
            final File scriptfile,
            final String scriptargs,
            final String[] scriptargsarr,
            final Map<String, Map<String, String>> envContext,
            final Map<String, Map<String, String>> newDataContext,
            final String interpreter,
            final String logName,
            final boolean interpreterArgsQuoted
    ) throws IOException {

        final ProcessBuilder processBuilder = buildProcess(workingdir, scriptfile, scriptargs, scriptargsarr, envContext,
                                                           newDataContext, interpreter, interpreterArgsQuoted);
        logger.info("[" + logName + "] executing: " + processBuilder.command());
        return processBuilder.start();
    }

    /**
     * Build a ProcessBuilder to invoke a specified shell command and passing the arguments to the shell.
     *
     * @param workingdir     working dir
     * @param scriptfile     file
     * @param scriptargs     arguments to the shell
     * @param envContext     Environment variable context
     * @param newDataContext context data to replace in the scriptargs
     * @param interpreter    the remote shell script, which will be split on whitespace
     * @param interpreterArgsQuoted if true, quote the file+args as a single argument to the interpreter
     * @return process builder
     */
    static ProcessBuilder buildProcess(final File workingdir, final File scriptfile, final String scriptargs,
                                       final Map<String, Map<String, String>> envContext,
                                       final Map<String, Map<String, String>> newDataContext,
                                       final String interpreter, final boolean interpreterArgsQuoted) {
        return buildProcess(workingdir,
                            scriptfile,
                            scriptargs,
                            null,
                            envContext,
                            newDataContext,
                            interpreter,
                            interpreterArgsQuoted);
    }

    /**
     * Build a ProcessBuilder to invoke a specified shell command and passing the arguments to the shell.
     *
     * @param workingdir            working dir
     * @param scriptfile            file
     * @param scriptargs            arguments to the shell
     * @param envContext            Environment variable context
     * @param newDataContext        context data to replace in the scriptargs
     * @param interpreter           the remote shell script, which will be split on whitespace
     * @param interpreterArgsQuoted if true, quote the file+args as a single argument to the interpreter
     *
     * @return process builder
     */
    static ProcessBuilder buildProcess(
            final File workingdir,
            final File scriptfile,
            final String scriptargs,
            final String[] scriptargsarr,
            final Map<String, Map<String, String>> envContext,
            final Map<String, Map<String, String>> newDataContext,
            final String interpreter,
            final boolean interpreterArgsQuoted
    )
    {
        final ArrayList<String> shells = new ArrayList<String>();
        if (null != interpreter) {
            shells.addAll(Arrays.asList(interpreter.split(" ")));
        }

        //use script-copy attribute and replace datareferences
        if (null != scriptargs || null!=scriptargsarr) {
            if (interpreterArgsQuoted) {
                final String newargs = null != scriptargs ? DataContextUtils.replaceDataReferencesInString(
                        scriptargs,
                        newDataContext
                ) : DataContextUtils.join(Arrays.asList(DataContextUtils.replaceDataReferencesInArray(
                        scriptargsarr,
                        newDataContext
                )), " ");
                shells.add(scriptfile.getAbsolutePath() + " " + newargs);
            } else {
                shells.add(scriptfile.getAbsolutePath());
                shells.addAll(Arrays.asList(DataContextUtils.replaceDataReferencesInArray(
                        null!=scriptargsarr?scriptargsarr:scriptargs.split(" "),
                        newDataContext
                )));
            }
        } else {
            shells.add(scriptfile.getAbsolutePath());
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(shells).directory(workingdir);
        final Map<String, String> environment = processBuilder.environment();
        //create system environment variables from the data context
        if (null != envContext) {
            environment.putAll(DataContextUtils.generateEnvVarsFromContext(envContext));
        }

        return processBuilder;
    }

    /**
     * Execute a process directly with some arguments
     *
     * @param logger         logger
     * @param workingdir     working dir
     * @param scriptfile     file
     * @param scriptargs     arguments to the shell
     * @param scriptargsarr
     * @param envContext     Environment variable context
     * @param newDataContext context data to replace in the scriptargs
     * @param logName        name of plugin to use in logging
     * @return process
     */
    static Process execScript(
            final Logger logger, final File workingdir, final File scriptfile,
            final String scriptargs,
            final String[] scriptargsarr,
            final Map<String, Map<String, String>> envContext,
            final Map<String, Map<String, String>> newDataContext,
            final String logName
    ) throws IOException
    {
        ExecParams execArgs = buildExecParams(scriptfile, scriptargs, scriptargsarr, envContext, newDataContext);
        String[] args = execArgs.getArgs();
        String[] envarr = execArgs.getEnvarr();
        final Runtime runtime = Runtime.getRuntime();
        logger.info("[" + logName + "] executing: " + StringArrayUtil.asString(args, " "));
        return runtime.exec(args, envarr, workingdir);
    }

    static ExecParams buildExecParams(final File scriptfile, final String scriptargs,
                                      final Map<String, Map<String, String>> envContext,
                                      final Map<String, Map<String, String>> newDataContext) {
        return buildExecParams(scriptfile, scriptargs, null, envContext, newDataContext);
    }

    static ExecParams buildExecParams(final File scriptfile, final String scriptargs, final String[] scriptargsarr,
                                      final Map<String, Map<String, String>> envContext,
                                      final Map<String, Map<String, String>> newDataContext) {
        final ArrayList<String> list = new ArrayList<String>();
        list.add(scriptfile.getAbsolutePath());
        if (null != scriptargsarr && scriptargsarr.length > 0) {
            list.addAll(Arrays.asList(DataContextUtils.replaceDataReferencesInArray(scriptargsarr, newDataContext)));
        }else if (null != scriptargs && !"".equals(scriptargs)) {
            list.addAll(Arrays.asList(DataContextUtils.replaceDataReferencesInArray(scriptargs.split(" "), newDataContext)));
        }
        final String[] args = list.toArray(new String[list.size()]);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(envContext);
        final ArrayList<String> envlist = new ArrayList<>();
        for (final Map.Entry<String, String> entry : envMap.entrySet()) {
            envlist.add(entry.getKey() + "=" + entry.getValue());
        }
        final String[] envarr = envlist.toArray(new String[envlist.size()]);


        return new ExecParams(args, envarr);
    }

    static class ExecParams {
        private String[] args;
        private String[] envarr;

        ExecParams(String[] args, String[] envarr) {
            this.args = args;
            this.envarr = envarr;
        }

        public String[] getArgs() {
            return args;
        }

        public String[] getEnvarr() {
            return envarr;
        }

    }

    public static List<String> generateListStoragePath(Description pluginDesc, DataContext dataContext, Map<String, Object> configuration){
        List<String> listStoragePath = new ArrayList<>();

        Map<String, Object> expanded =
                DataContextUtils.replaceDataReferences(
                        configuration,
                        dataContext
                );

        Map<String, String> data = MapData.toStringStringMap(expanded);
        for (Property property : pluginDesc.getProperties()) {
            String name = property.getName();
            String propValue = data.get(name);
            if (null == propValue) {
                continue;
            }
            Map<String, Object> renderingOptions = property.getRenderingOptions();
            if (renderingOptions != null) {
                Object conversion = renderingOptions.get(StringRenderingConstants.VALUE_CONVERSION_KEY);
                if (StringRenderingConstants.ValueConversion.STORAGE_PATH_AUTOMATIC_READ.equalsOrString(conversion)) {
                    listStoragePath.add(propValue);
                }
            }
        }
        return listStoragePath;
    }

    public static SecretBundle generateBundle(Description pluginDesc, DataContext dataContext, StorageTree storageTree, Map<String, Object> configuration, Logger logger){

        List<String> listStoragePaths = generateListStoragePath(pluginDesc, dataContext, configuration);
        DefaultSecretBundle bundle = new DefaultSecretBundle();

        for (String propValue : listStoragePaths) {
            Resource<ResourceMeta> r = storageTree.getResource(propValue);
            if(r != null) {
                try( ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){
                    r.getContents().writeContent(byteArrayOutputStream);
                    bundle.addSecret(propValue, byteArrayOutputStream.toByteArray());
                } catch (IOException iex) {
                    throw new RuntimeException(String.format("IOException Unable to add secret value to secret bundle for: %s",propValue));
                }
            }
        }
        return bundle;
    }
}