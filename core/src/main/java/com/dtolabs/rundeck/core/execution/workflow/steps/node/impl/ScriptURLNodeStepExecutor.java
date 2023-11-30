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
* ScriptURLNodeStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/2/12 2:37 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.UpdateUtils;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdaterBuilder;
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.Converter;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ScriptURLNodeStepExecutor is a NodeStepExecutor for executing a script retrieved from a URL.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptURLNodeStepExecutor implements NodeStepExecutor {
    public static final Logger logger                      = LoggerFactory.getLogger(ScriptURLNodeStepExecutor.class.getName());
    public static final String SERVICE_IMPLEMENTATION_NAME = "script-url";

    private static final int DEFAULT_TIMEOUT = 30;
    private static final boolean USE_CACHE = true;
    public static final String UTF_8 = "UTF-8";

    private File cacheDir;

    private Framework framework;
    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    public ScriptURLNodeStepExecutor(Framework framework) {
        this.framework = framework;
        cacheDir = new File(Constants.getBaseVar(framework.getBaseDir().getAbsolutePath())
                            + "/cache/ScriptURLNodeStepExecutor");
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

    public ScriptFileNodeStepUtils getScriptUtils() {
        return scriptUtils;
    }

    public void setScriptUtils(ScriptFileNodeStepUtils scriptUtils) {
        this.scriptUtils = scriptUtils;
    }

    static enum Reason implements FailureReason{
        /**
         * Failed to download required URL
         */
        URLDownloadFailure
    }

    public NodeStepResult executeNodeStep(
            final StepExecutionContext context,
            final NodeStepExecutionItem item,
            final INodeEntry node
    ) throws NodeStepException
    {
        //TODO: remove class
        final ScriptURLCommandExecutionItem script = (ScriptURLCommandExecutionItem) item;
        File destinationTempFile = downloadURLToTempFile(context, node, script);
        if (!USE_CACHE) {
            destinationTempFile.deleteOnExit();
        }
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }

        if(expandTokens) {
            expandTokens = script.isExpandTokenInScriptFile();
        }

        return scriptUtils.executeScriptFile(
                context,
                node,
                null,
                destinationTempFile.getAbsolutePath(),
                null,
                script.getFileExtension(),
                script.getArgs(),
                script.getScriptInterpreter(),
                script.getInterpreterArgsQuoted(),
                framework.getExecutionService(),
                expandTokens
        );
    }

    private File downloadURLToTempFile(
            final StepExecutionContext context,
            final INodeEntry node,
            final ScriptURLCommandExecutionItem script
    ) throws NodeStepException
    {
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            throw new RuntimeException("Unable to create cachedir: " + cacheDir.getAbsolutePath());
        }
        //create node context for node and substitute data references in command
        WFSharedContext sharedContext = new WFSharedContext(context.getSharedDataContext());
        sharedContext.merge(ContextView.global(), context.getDataContextObject());
        sharedContext.merge(
                ContextView.node(node.getNodename()),
                new BaseDataContext("node", DataContextUtils.nodeData(node))
        );


        final String finalUrl = expandUrlString(script.getURLString(), sharedContext, node.getNodename());
        final URL url;
        try {
            url = new URL(finalUrl);
        } catch (MalformedURLException e) {
            throw new NodeStepException(e, StepFailureReason.ConfigurationFailure, node.getNodename());
        }
        if(null!=context.getExecutionListener()){
            context.getExecutionListener().log(4, "Requesting URL: " + url.toExternalForm());
        }

        String cleanUrl = url.toExternalForm().replaceAll("^(https?://)([^:@/]+):[^@/]*@", "$1$2:****@");
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
                    Reason.URLDownloadFailure,
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

}
