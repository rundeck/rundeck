package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.Constants
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
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.utils.Converter
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.commons.codec.binary.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@CompileStatic
class ScriptURLNodeStepExecutor {
    static Logger logger = LoggerFactory.getLogger(ScriptURLNodeStepExecutor.class);

    private final String scriptInterpreter;
    private final boolean interpreterArgsQuoted;
    private final String fileExtension;
    private final String argString;
    private final String adhocFilepath;
    private final boolean expandTokenInScriptFile;


    ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    PluginStepContext context
    FileCopierUtil.ContentModifier modifier
    private File cacheDir;
    private static final int DEFAULT_TIMEOUT = 30;
    private static final boolean USE_CACHE = true;
    public static final String UTF_8 = "UTF-8";
    URLDownloader downloader = new Downloader();

    ScriptURLNodeStepExecutor(
        PluginStepContext context,
        String scriptInterpreter,
        boolean interpreterArgsQuoted,
        String fileExtension,
        String argString,
        String adhocFilepath,
        boolean expandTokenInScriptFile,
        FileCopierUtil.ContentModifier modifier
    ) {
        this.scriptInterpreter = scriptInterpreter
        this.interpreterArgsQuoted = interpreterArgsQuoted
        this.fileExtension = fileExtension
        this.argString = argString
        this.adhocFilepath = adhocFilepath
        this.expandTokenInScriptFile = expandTokenInScriptFile
        this.context = context
        this.modifier = modifier

        cacheDir = new File(
            Constants.getBaseVar(context.getFramework().getFilesystemFramework().getBaseDir().getAbsolutePath())
                + "/cache/ScriptURLNodeStepExecutor");
    }

    void executeScriptURL(INodeEntry entry, InputStream inputStream) {
        File destinationTempFile = downloader.downloadURLToTempFile(adhocFilepath, cacheDir, context, entry);
        if (!USE_CACHE) {
            destinationTempFile.deleteOnExit();
        }
        boolean expandTokens = true;
        if (context.getIFramework().getPropertyLookup().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(
                context.getIFramework().getPropertyLookup().getProperty(
                    "execution.script.tokenexpansion.enabled"
                )
            );
        }

        final String[] args;
        if (null != argString) {
            args = OptsUtil.burst(argString);
        } else {
            args = new String[0];
        }

        if(expandTokens) {
            expandTokens = expandTokenInScriptFile;
        }

        StepExecutionContext stepExecutionContext = context.getExecutionContext() as StepExecutionContext
        final ExecutionService executionService = context.getIFramework().getExecutionService();

        NodeStepResult nodeExecutorResult = scriptUtils.executeScriptFile(
                stepExecutionContext,
                entry,
                null,
                destinationTempFile.getAbsolutePath(),
                null,
                fileExtension,
                args,
                scriptInterpreter,
                inputStream,
                interpreterArgsQuoted,
                executionService,
                expandTokens,
                modifier
        );

        Util.handleFailureResult(nodeExecutorResult, entry)
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

    interface URLDownloader {
        File downloadURLToTempFile(
            final String urlString,
            final File cacheDir,
            final PluginStepContext context,
            final INodeEntry node
        ) throws NodeStepException;
    }

    static class Downloader implements URLDownloader {

        File downloadURLToTempFile(
            final String urlString,
            final File cacheDir,
            final PluginStepContext context,
            final INodeEntry node
        ) throws NodeStepException {
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


            final String finalUrl = expandUrlString(urlString, sharedContext, node.getNodename());
            final URL url;
            try {
                url = new URL(finalUrl);
            } catch (MalformedURLException e) {
                throw new NodeStepException(e, StepFailureReason.ConfigurationFailure, node.getNodename());
            }
            if (null != context.getExecutionContext().getExecutionListener()) {
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
                        Reason.URLDownloadFailure,
                        node.getNodename()
                    )
                } else {
                    logger.error(
                        "Error requesting URL script: " + cleanUrl + ": " + e.getMessage(), e
                    )
                }
            }
            return destinationTempFile;
        }
    }


    public static final Converter<String, String> urlQueryEncoder = s -> {
        try {
            return URLEncoder.encode(s.toString(),UTF_8).replace("+","%20");
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
    @CompileDynamic
    static String expandUrlString(
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
                ))
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
