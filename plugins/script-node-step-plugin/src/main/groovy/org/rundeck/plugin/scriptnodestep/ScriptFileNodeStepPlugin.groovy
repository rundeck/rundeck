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

package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.UpdateUtils
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater
import com.dtolabs.rundeck.core.common.impl.URLFileUpdaterBuilder
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.data.MultiDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.BaseCommandExec
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLNodeStepExecutor
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.utils.Converter
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import org.apache.commons.codec.binary.Hex

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = ScriptFileNodeStepPlugin.PROVIDER_NAME)
@PluginDescription(title = "Script file or URL", description = "Verify and validate design", isHighlighted = true, order = 2)
public class ScriptFileNodeStepPlugin implements NodeStepPlugin, BaseCommandExec {
    public static final String PROVIDER_NAME = "script-file-node-step-plugin";

    @PluginProperty(title = "File Path or URL",
            description = "Path",
            required = true)
    String adhocFilepath;

    @PluginProperty(title = "Arguments",
            description = "Arguments",
            required = false)
    String argString;

    @PluginProperty(title = "Invocation String",
            description = "",
            required = false)
    String scriptInterpreter;

    @PluginProperty(title = "Expand variables in script file",
            description = "",
            required = false)
    Boolean expandTokenInScriptFile;

    @PluginProperty(title = "Quote arguments to script invocation string?",
            description = "",
            required = false)
    Boolean interpreterArgsQuoted;

    @PluginProperty(title = "File Extension",
            description = "",
            required = false)
    String fileExtension;

    private DefaultScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    private File cacheDir;
    private static final int DEFAULT_TIMEOUT = 30;
    private static final boolean USE_CACHE = true;
    public static final String UTF_8 = "UTF-8";

    @Override
    public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {

        if(adhocFilepath ==~ /^(?i:https?|file):.*$/) {
            executeScriptURL(context, configuration, entry);
        } else {
            executeScriptFile(context, configuration, entry);
        }
    }

    private void executeScriptFile(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) {
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }
        if(null != adhocFilepath){
            expandTokens = expandTokenInScriptFile;
        }

        String expandedVarsInURL = SharedDataContextUtils.replaceDataReferences(
                adhocFilepath,
                context.getExecutionContext().getSharedDataContext(),
                //add node name to qualifier to read node-data first
                ContextView.node(entry.getNodename()),
                ContextView::nodeStep,
                DataContextUtils.replaceMissingOptionsWithBlank,
                false,
                false
        );

        if( DataContextUtils.hasOptionsInString(expandedVarsInURL) ){
            Map<String, Map<String, String>> optionsContext = new HashMap();
            optionsContext.put("option", context.getDataContext().get("option"));
            expandedVarsInURL = DataContextUtils.replaceDataReferencesInString(expandedVarsInURL, optionsContext);
        }

        final String[] args;
        if (null != argString) {
            args = OptsUtil.burst(argString);
        } else {
            args = new String[0];
        }

        scriptUtils.executeScriptFile(
                context.getExecutionContext(),
                entry,
                null,
                expandedVarsInURL,
                null,
                fileExtension,
                args,
                scriptInterpreter,
                interpreterArgsQuoted,
                context.getFramework().getExecutionService(),
                expandTokens
        );
    }

    private void executeScriptURL(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) {
        File destinationTempFile = downloadURLToTempFile(context, entry);
        if (!USE_CACHE) {
            destinationTempFile.deleteOnExit();
        }
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }

        final String[] args;
        if (null != argString) {
            args = OptsUtil.burst(argString);
        } else {
            args = new String[0];
        }

        scriptUtils.executeScriptFile(
                context,
                entry,
                null,
                destinationTempFile.getAbsolutePath(),
                null,
                fileExtension,
                args,
                scriptInterpreter,
                interpreterArgsQuoted,
                context.getFramework().getStepExecutionService(),
                expandTokens
        );
    }

    private static String hashURL(final String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(url.getBytes(Charset.forName(UTF_8)));
            return new String(Hex.encodeHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return Integer.toString(url.hashCode());
    }

    static enum Reason implements FailureReason{
        /**
         * Failed to download required URL
         */
        URLDownloadFailure
    }

    private File downloadURLToTempFile(
            final PluginStepContext context,
            final INodeEntry node
    ) throws NodeStepException
    {
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            throw new RuntimeException("Unable to create cachedir: " + cacheDir.getAbsolutePath());
        }
        //create node context for node and substitute data references in command
        WFSharedContext sharedContext = new WFSharedContext(context.getExecutionContext().getSharedDataContext());
        sharedContext.merge(ContextView.global(), context.getDataContextObject());
        sharedContext.merge(
                ContextView.node(node.getNodename()),
                new BaseDataContext("node", DataContextUtils.nodeData(node))
        );


        final String finalUrl = expandUrlString(adhocFilepath, sharedContext, node.getNodename());
        final URL url;
        try {
            url = new URL(finalUrl);
        } catch (MalformedURLException e) {
            throw new NodeStepException(e, StepFailureReason.ConfigurationFailure, node.getNodename());
        }
        if(null!=context.getExecutionContext().getExecutionListener()){
            context.getExecutionContext().getExecutionListener().log(4, "Requesting URL: " + url.toExternalForm());
        }

        String cleanUrl = url.toExternalForm().replaceAll("^(https?://)([^:@/]+):[^@/]*@", "\$1\$2:****@");
        String tempFileName = hashURL(url.toExternalForm()) + ".temp";
        File destinationTempFile = new File(cacheDir, tempFileName);
        File destinationCacheData = new File(cacheDir, tempFileName + ".cache.properties");

        //update from URL if necessary
        final URLFileUpdaterBuilder urlFileUpdaterBuilder = new URLFileUpdaterBuilder()
                .setUrl(url)
                .setAcceptHeader("*/*")
                .setTimeout(DEFAULT_TIMEOUT);
        if (USE_CACHE) {
            urlFileUpdaterBuilder
                    .setCacheMetadataFile(destinationCacheData)
                    .setCachedContent(destinationTempFile)
                    .setUseCaching(true);
        }
        final URLFileUpdater updater = urlFileUpdaterBuilder.createURLFileUpdater();
        try {
            UpdateUtils.update(updater, destinationTempFile);

            logger.debug("Updated nodes resources file: " + destinationTempFile);
        } catch (UpdateUtils.UpdateException e) {
            if (!destinationTempFile.isFile() || destinationTempFile.length() < 1) {
                throw new NodeStepException(
                        "Error requesting URL Script: " + cleanUrl + ": " + e.getMessage(),
                        e,
                        ScriptURLNodeStepExecutor.Reason.URLDownloadFailure,
                        node.getNodename());
            } else {
                logger.error(
                        "Error requesting URL script: " + cleanUrl + ": " + e.getMessage(), e);
            }
        }
        return destinationTempFile;
    }

    public static final Converter<String, String> urlPathEncoder = s -> {
        try {
            return URLEncoder.encode(s,UTF_8).replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    };
    public static final Converter<String, String> urlQueryEncoder = s -> {
        try {
            return URLEncoder.encode(s,UTF_8).replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    };

    /**
     * Expand data references in a URL string, using proper encoding for path and query parts.
     * @param urlString url
     * @param dataContext multi context
     * @param nodename default node context
     * @return expanded string
     */
    public static String expandUrlString(
            final String urlString,
            final MultiDataContext<ContextView, DataContext> dataContext,
            final String nodename
    )
    {
        final int qindex = urlString.indexOf('?');
        final StringBuilder builder = new StringBuilder();

        builder.append(
                SharedDataContextUtils.replaceDataReferences(
                        qindex > 0 ? urlString.substring(0, qindex) : urlString,
                        dataContext,
                        ContextView.node(nodename),
                        ContextView::nodeStep,
                        DataContextUtils.replaceMissingOptionsWithBlank,
                        true,
                        false
                ));
        if (qindex > 0) {
            builder.append("?");
            if (qindex < urlString.length() - 1) {
                builder.append(SharedDataContextUtils.replaceDataReferences(
                        urlString.substring(qindex + 1),
                        dataContext,
                        ContextView.node(nodename),
                        ContextView::nodeStep,
                        urlQueryEncoder,
                        true,
                        false
                ));
            }

        }

        return builder.toString();
    }

    @Override
    String getAdhocRemoteString() {
        return null
    }

    @Override
    String getAdhocLocalString() {
        return null
    }

    @Override
    Boolean getAdhocExecution() {
        return null
    }
}
