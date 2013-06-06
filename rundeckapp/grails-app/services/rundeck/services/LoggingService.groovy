package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.LogOutputStream
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
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

    FrameworkService frameworkService
    LogFileStorageService logFileStorageService
    def pluginService
    def StreamingLogWriterPluginProviderService streamingLogWriterPluginProviderService
    def StreamingLogReaderPluginProviderService streamingLogReaderPluginProviderService
    def grailsApplication

    def configure() {
    }

    public boolean isLocalFileStorageEnabled(){
        boolean fileDisabled = grailsApplication.config.rundeck?.execution?.logs?.localFileStorageEnabled in ['false', false]
        boolean readerPluginConfigured= getConfiguredStreamingReaderPluginName()
        return !(fileDisabled && readerPluginConfigured)
    }

    public ExecutionLogWriter openLogWriter(Execution execution, LogLevel level, Map<String, String> defaultMeta) {
        List<StreamingLogWriter> plugins=[]
        def names = listConfiguredStreamingWriterPluginNames()
        if (names) {
            HashMap<String, String> jobcontext = ExecutionService.exportContextForExecution(execution)
            log.debug("Configured log writer plugins: ${names}")
            names.each { name ->
                def plugin = pluginService.configurePlugin(name, streamingLogWriterPluginProviderService,
                        frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
                if (null == plugin) {
                    log.error("Failed to load StreamingLogWriter plugin named ${name}")
                    return
                }
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
            outfilepath = logFileStorageService.generateFilepathForExecution(execution)
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

    private String getConfiguredStreamingReaderPluginName() {
        if (grailsApplication.config.rundeck?.execution?.logs?.streamingReaderPlugin) {
            return grailsApplication.config.rundeck?.execution?.logs?.streamingReaderPlugin.toString()
        }
        null
    }

    private List<String> listConfiguredStreamingWriterPluginNames() {
        if(grailsApplication.config?.rundeck?.execution?.logs?.streamingWriterPlugins){
            return grailsApplication.config?.rundeck?.execution?.logs?.streamingWriterPlugins.toString().split(/,\s*/) as List
        }
        []
    }

    public ExecutionLogReader getLogReader(Execution execution) {
        def pluginName = getConfiguredStreamingReaderPluginName()
        if(pluginName){
            HashMap<String, String> jobcontext = ExecutionService.exportContextForExecution(execution)
            log.debug("Using log reader plugin ${pluginName}")
            boolean ready=false
            try {
                def plugin = pluginService.configurePlugin(pluginName, streamingLogReaderPluginProviderService,
                        frameworkService.getFrameworkPropertyResolver(execution.project), PropertyScope.Instance)
                if (plugin != null) {
                    ready=plugin.initialize(jobcontext)
                    if(ready){
                        return new ExecutionLogReader(state: ExecutionLogState.AVAILABLE, reader: plugin)
                    }else{
                        return new ExecutionLogReader(state: ExecutionLogState.WAITING, reader: null)
                    }
                }
            } catch (Throwable e) {
                log.error("Failed to initialize reader plugin ${pluginName}: " + e.message)
                log.debug("Failed to initialize reader plugin ${pluginName}: " + e.message, e)
            }
        }

        if(pluginName){
            log.error("Falling back to local file storage log reader")
        }
        return logFileStorageService.requestLogFileReader(execution)
    }

    public OutputStream createLogOutputStream(StreamingLogWriter logWriter, LogLevel level) {
        return new LogOutputStream(logWriter, level)
    }
}
