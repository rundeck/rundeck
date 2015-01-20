package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.ThreadBoundLogOutputStream
import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import com.dtolabs.rundeck.server.plugins.services.StreamingLogReaderPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogWriterPluginProviderService
import rundeck.Execution
import rundeck.services.logging.DisablingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogWriter
import rundeck.services.logging.ExecutionLogState
import rundeck.services.logging.LoglevelThresholdLogWriter
import rundeck.services.logging.MultiLogWriter

class LoggingService {

    static final String LOG_FILE_FILETYPE ="rdlog"
    FrameworkService frameworkService
    LogFileStorageService logFileStorageService
    def pluginService
    def StreamingLogWriterPluginProviderService streamingLogWriterPluginProviderService
    def StreamingLogReaderPluginProviderService streamingLogReaderPluginProviderService
    def grailsApplication
    def grailsLinkGenerator

    public boolean isLocalFileStorageEnabled(){
        boolean fileDisabled = grailsApplication.config?.rundeck?.execution?.logs?.localFileStorageEnabled in ['false', false]
        boolean readerPluginConfigured= getConfiguredStreamingReaderPluginName()
        return !(fileDisabled && readerPluginConfigured)
    }

    public ExecutionLogWriter openLogWriter(Execution execution, LogLevel level, Map<String, String> defaultMeta) {
        List<StreamingLogWriter> plugins=[]
        def names = listConfiguredStreamingWriterPluginNames()
        if (names) {
            HashMap<String, String> jobcontext = ExecutionService.exportContextForExecution(execution,grailsLinkGenerator)
            log.debug("Configured log writer plugins: ${names}")
            names.each { name ->
                def result = pluginService.configurePlugin(name, streamingLogWriterPluginProviderService,
                        frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
                if (null == result || null==result.instance) {
                    log.error("Failed to load StreamingLogWriter plugin named ${name}")
                    return
                }
                def plugin=result.instance
                try {
                    plugin.initialize(jobcontext)
                    plugins << DisablingLogWriter.create(plugin, "StreamingLogWriter(${name})")
                } catch (Throwable e) {
                    log.error("Failed to initialize plugin ${name}: " + e.message)
                    log.debug("Failed to initialize plugin ${name}: " + e.message, e)
                }

            }
        }
        def outfilepath=null
        if (plugins.size() < 1 || isLocalFileStorageEnabled()) {
            plugins << logFileStorageService.getLogFileWriterForExecution(execution, defaultMeta)
            outfilepath = logFileStorageService.getFileForExecutionFiletype(execution, LOG_FILE_FILETYPE, false)
        }else{
            log.debug("File log writer disabled for execution ${execution.id}")
        }

        def multiWriter = new MultiLogWriter(plugins)
        def thresholdWriter = new LoglevelThresholdLogWriter(multiWriter, level)
        def writer = new ExecutionLogWriter(thresholdWriter)
        if(outfilepath){
            //file path support
            writer.filepath = outfilepath
        }
        return writer
    }

    /**
     * Return the log file for the execution
     */
    public File getLogFileForExecution(Execution execution) {
        logFileStorageService.getFileForExecutionFiletype(execution, LOG_FILE_FILETYPE, true)
    }

    String getConfiguredStreamingReaderPluginName() {
        if (grailsApplication.config?.rundeck?.execution?.logs?.streamingReaderPlugin) {
            return grailsApplication.config?.rundeck?.execution?.logs?.streamingReaderPlugin.toString()
        }
        null
    }

    List<String> listConfiguredStreamingWriterPluginNames() {
        if(grailsApplication.config?.rundeck?.execution?.logs?.streamingWriterPlugins){
            return grailsApplication.config?.rundeck?.execution?.logs?.streamingWriterPlugins.toString().split(/,\s*/) as List
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
        if(pluginName){
            HashMap<String, String> jobcontext = ExecutionService.exportContextForExecution(execution,grailsLinkGenerator)
            log.debug("Using log reader plugin ${pluginName}")
            def result = pluginService.configurePlugin(pluginName, streamingLogReaderPluginProviderService,
                    frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
            if (result != null && result.instance != null) {
                def plugin = result.instance
                try {
                    if (plugin.initialize(jobcontext)) {
                        return new ExecutionLogReader(state: ExecutionLogState.AVAILABLE, reader: plugin)
                    } else {
                        return new ExecutionLogReader(state: ExecutionLogState.WAITING, reader: null)
                    }
                } catch (Throwable e) {
                    log.error("Failed to initialize reader plugin ${pluginName}: " + e.message)
                    log.debug("Failed to initialize reader plugin ${pluginName}: " + e.message, e)
                }
            } else {
                log.error("Failed to create reader plugin ${pluginName}")
            }
        }

        if(pluginName){
            log.error("Falling back to local file storage log reader")
        }
        return logFileStorageService.requestLogFileReader(execution, LOG_FILE_FILETYPE)
    }

    public OutputStream createLogOutputStream(StreamingLogWriter logWriter, LogLevel level, Contextual listener) {
        return new ThreadBoundLogOutputStream(logWriter, level, listener)
    }
}
