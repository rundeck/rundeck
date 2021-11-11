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

package rundeck.services


import com.dtolabs.rundeck.app.internal.logging.LogEventBufferManager
import com.dtolabs.rundeck.app.internal.logging.LogFlusher
import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.utils.ThreadBoundLogOutputStream
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import com.dtolabs.rundeck.server.plugins.services.StreamingLogReaderPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogWriterPluginProviderService
import rundeck.Execution
import rundeck.WorkflowStep
import rundeck.services.logging.DisablingLogWriter
import org.rundeck.app.services.ExecutionFile

import org.rundeck.app.services.ExecutionFileProducer
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogWriter
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.LineCountingLogWriter
import rundeck.services.logging.LoggingThreshold
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter
import rundeck.services.logging.NodeCountingLogWriter
import rundeck.services.logging.ProducedExecutionFile
import rundeck.services.logging.StepLabellingStreamingLogWriter
import rundeck.services.logging.ThresholdLogWriter

import java.nio.charset.Charset

class LoggingService implements ExecutionFileProducer {

    public static final String LOG_FILE_FILETYPE = 'rdlog'


    FrameworkService frameworkService
    LogFileStorageService logFileStorageService
    def pluginService
    def StreamingLogWriterPluginProviderService streamingLogWriterPluginProviderService
    def StreamingLogReaderPluginProviderService streamingLogReaderPluginProviderService
    def grailsLinkGenerator
    def configurationService

    public boolean isLocalFileStorageEnabled() {
        boolean fileDisabled = configurationService.getBoolean("execution.logs.localFileStorageEnabled",false)
        boolean readerPluginConfigured = getConfiguredStreamingReaderPluginName()
        return !(fileDisabled && readerPluginConfigured)
    }

    @Override
    String getExecutionFileType() {
        LOG_FILE_FILETYPE
    }

    @Override
    boolean isExecutionFileGenerated() {
        false
    }

    @Override
    ExecutionFile produceStorageFileForExecution(final ExecutionReference e) {
        File file = getLogFileForExecution e
        new ProducedExecutionFile(localFile: file, fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE)
    }

    @Override
    boolean isCheckpointable() {
        true
    }

    @Override
    ExecutionFile produceStorageCheckpointForExecution(final ExecutionReference e) {
        produceStorageFileForExecution e
    }

    /**
     * Create an ExecutionLogWriter
     * @param execution execution
     * @param level log level
     * @param defaultMeta default metadata
     * @param threshold optional logging threshold
     * @return ExecutionLogWriter
     */
    public ExecutionLogWriter openLogWriter(
            Execution execution,
            LogLevel level,
            Map<String, String> defaultMeta,
            LoggingThreshold threshold = null
    )
    {
        List<StreamingLogWriter> plugins = []
        def names = listConfiguredStreamingWriterPluginNames()
        if (names) {
            Map<String, Object> jobcontext = new HashMap<>(
                ExecutionService.exportContextForExecution(execution, grailsLinkGenerator)
            )
            def labels = [:]
            execution.workflow?.commands?.eachWithIndex { WorkflowStep entry, int index ->
                if (entry.description) {
                    labels["${index + 1}"] = entry.description
                }
            }
            log.debug("Configured log writer plugins: ${names}")
            boolean enableLabels = configurationService.getBoolean("execution.logs.plugins.streamingWriterStepLabelsEnabled", false)

            names.each { name ->
                def result = pluginService.configurePlugin(
                        name,
                        streamingLogWriterPluginProviderService,
                        frameworkService.getFrameworkPropertyResolver(execution.project),
                        PropertyScope.Instance
                )
                if (null == result || null == result.instance) {
                    log.error("Failed to load StreamingLogWriter plugin named ${name}")
                    return
                }
                def plugin = result.instance
                try {
                    plugin.initialize(jobcontext)
                    plugins << DisablingLogWriter.create(
                        enableLabels ? new StepLabellingStreamingLogWriter(plugin, labels) : plugin,
                        "StreamingLogWriter(${name})"
                    )
                } catch (Throwable e) {
                    log.error("Failed to initialize plugin ${name}: " + e.message)
                    log.debug("Failed to initialize plugin ${name}: " + e.message, e)
                }

            }
        }
        def outfilepath = null
        if (plugins.size() < 1 || isLocalFileStorageEnabled()) {
            plugins << DisablingLogWriter.create(
                    logFileStorageService.getLogFileWriterForExecution(
                    execution,
                    defaultMeta,
                    threshold?.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE)
                    ),
                    "FSStreamingLogWriter(execution:${execution.id})"
            )
            outfilepath = logFileStorageService.getFileForExecutionFiletype(execution, LOG_FILE_FILETYPE, false, false)
        } else {
            log.debug("File log writer disabled for execution ${execution.id}")
        }

        def multiWriter = new MultiLogWriter(plugins)
        //add watchers for thresholds if present
        def nodeWatcher = threshold?.watcherForType(LoggingThreshold.LINES_PER_NODE)
        if (nodeWatcher) {
            def countLogger = new NodeCountingLogWriter(multiWriter)
            nodeWatcher.watch(countLogger)
            multiWriter = countLogger
        }
        def linesWatcher = threshold?.watcherForType(LoggingThreshold.TOTAL_LINES)
        if (linesWatcher) {
            def countLogger = new LineCountingLogWriter(multiWriter)
            linesWatcher.watch(countLogger)
            multiWriter = countLogger
        }
        def loglevelWriter = new LoglevelThresholdLogWriter(multiWriter, level)
        if (threshold) {
            loglevelWriter = new ThresholdLogWriter(loglevelWriter, threshold)
        }
        def writer = new ExecutionLogWriter(loglevelWriter)
        if (outfilepath) {
            //file path support
            writer.filepath = outfilepath
        }
        return writer
    }

    /**
     * Return the log file for the execution
     */
    public File getLogFileForExecution(Execution execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, LOG_FILE_FILETYPE, false, false)
    }

    /**
     * Return the log file for the execution
     */
    public File getLogFileForExecution(ExecutionReference execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, LOG_FILE_FILETYPE,  false)
    }

    String getConfiguredStreamingReaderPluginName() {
        String streamingReaderPlugin = configurationService.getString("execution.logs.streamingReaderPlugin")
        if (streamingReaderPlugin) {
            return streamingReaderPlugin
        }
        null
    }

    List<String> listConfiguredStreamingWriterPluginNames() {
        String value = configurationService.getString("execution.logs.streamingWriterPlugins")
        if (value) {
            return value?.split(/,\s*/) as List
        }
        []
    }

    def Map listStreamingReaderPlugins() {
        return pluginService.listPlugins(StreamingLogReaderPlugin, streamingLogReaderPluginProviderService)
    }

    def Map listStreamingWriterPlugins() {
        return pluginService.listPlugins(StreamingLogWriterPlugin, streamingLogWriterPluginProviderService)
    }

    public ExecutionLogReader getLogReader(Execution execution) {
        def pluginName = getConfiguredStreamingReaderPluginName()
        if (pluginName) {
            HashMap<String, String> jobcontext = ExecutionService.exportContextForExecution(
                    execution,
                    grailsLinkGenerator
            )
            log.debug("Using log reader plugin ${pluginName}")
            def result = pluginService.configurePlugin(
                    pluginName,
                    streamingLogReaderPluginProviderService,
                    frameworkService.getFrameworkPropertyResolver(execution.project),
                    PropertyScope.Instance
            )
            if (result != null && result.instance != null) {
                def plugin = result.instance
                try {
                    if (plugin.initialize(jobcontext)) {
                        return new ExecutionLogReader(state: ExecutionFileState.AVAILABLE, reader: plugin)
                    } else {
                        return new ExecutionLogReader(state: ExecutionFileState.WAITING, reader: null)
                    }
                } catch (Throwable e) {
                    log.error("Failed to initialize reader plugin ${pluginName}: " + e.message)
                    log.debug("Failed to initialize reader plugin ${pluginName}: " + e.message, e)
                }
            } else {
                log.error("Failed to create reader plugin ${pluginName}")
            }
        }

        if (pluginName) {
            log.error("Falling back to local file storage log reader")
        }
        return logFileStorageService.requestLogFileReader(execution, LOG_FILE_FILETYPE)
    }

    public OutputStream createLogOutputStream(
            StreamingLogWriter logWriter,
            LogLevel level,
            Contextual listener,
            LogFlusher flusherWorkflowListener,
            Charset charset=null
    )
    {
        def stream = new ThreadBoundLogOutputStream(
                logWriter.&addEvent,
                charset,
                { Charset charset1 ->
                    LogEventBufferManager.createManager(level,listener, charset1)
                }
        )
        flusherWorkflowListener?.logOut=stream
        return stream
    }
}
