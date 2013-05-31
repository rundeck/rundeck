package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.LogFileStorage
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.services.LogFileStoragePluginProviderService
import org.springframework.core.task.AsyncTaskExecutor
import rundeck.Execution
import rundeck.services.logging.EventStreamingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

class LogFileStorageService {

    static transactional = false
    static final RundeckLogFormat rundeckLogFormat = new RundeckLogFormat()
    def RundeckPluginRegistry rundeckPluginRegistry
    LogFileStoragePluginProviderService logFileStoragePluginProviderService
    def frameworkService
    def grailsApplication
    def AsyncTaskExecutor logFileTaskExecutor

    def LogFileStoragePlugin getPlugin(String name) {
        def bean = rundeckPluginRegistry?.loadPluginByName(name, logFileStoragePluginProviderService)
        if (bean != null) {
            return (LogFileStoragePlugin) bean
        }
        log.error("LogFileStoragePlugin not found: ${name}")
        return bean
    }

    def Map validatePluginConfig(String name, Map config) {
        def Map pluginDesc = getPluginDescriptor(name)
        if (pluginDesc && pluginDesc.description instanceof Description) {
            return frameworkService.validateDescription(pluginDesc.description, '', config)
        } else {
            return null
        }
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def Map getPluginDescriptor(String name) {
        def bean = rundeckPluginRegistry?.loadPluginDescriptorByName(name, logFileStoragePluginProviderService)
        if (bean) {
            return (Map) bean
        }
        log.error("LogFileStoragePlugin not found: ${name}")
        return null
    }

    private LogFileStoragePlugin configurePlugin(String name, Map configuration) {
        def bean = rundeckPluginRegistry?.configurePluginByName(name, logFileStoragePluginProviderService, configuration)
        if (bean) {
            return (LogFileStoragePlugin) bean
        }
        log.error("LogFileStoragePlugin not found: ${name}")
        return null
    }

    def Map listPlugins() {
        def plugins = [:]
        plugins = rundeckPluginRegistry?.listPluginDescriptors(LogFileStoragePlugin, logFileStoragePluginProviderService)
        //clean up name of any Groovy plugin without annotations that ends with "LogFileStoragePlugin"
        plugins.each { key, Map plugin ->
            def desc = plugin.description
            if (desc && desc instanceof Map) {
                if (desc.name.endsWith("LogFileStoragePlugin")) {
                    desc.name = desc.name.replaceAll(/LogFileStoragePlugin$/, '')
                }
            }
        }
//        System.err.println("listed plugins: ${plugins}")

        plugins
    }
    /**
     * Create a streaming log writer for the given execution.
     * @param e
     * @param logThreshold
     * @param defaultMeta
     * @return
     */
    StreamingLogWriter getLogFileWriterForExecution(Execution e, Map<String, String> defaultMeta) {
        def path = generateFilekeyForExecution(e)
        File file = getFileForKey(path)

        if (!file.getParentFile().isDirectory()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IllegalStateException("Unable to create directories for storage: " + file)
            }
        }
        //TODO: if remote log storage plugin available, apply hook to call storeLogFile on close
        def fsWriter = new FSStreamingLogWriter(new FileOutputStream(file), defaultMeta, rundeckLogFormat)
        def plugin = getConfiguredPluginForExecution(e)
        if(null!=plugin){
            fsWriter=new EventStreamingLogWriter(fsWriter)
            fsWriter.onOpenStream {
                log.error("onOpenStream called for streaming writer")
            }
            fsWriter.onClose {
                log.error("onClose called for streaming writer")
                storeLogFileAsync(file,plugin)
            }
        }
        return fsWriter
    }

    File generateFilepathForExecution(Execution execution) {
        return getFileForKey(generateFilekeyForExecution(execution))
    }

    private File getFileForExecution(Execution execution) {
        return execution.outputfilepath?new File(execution.outputfilepath):null
    }

    private static String generateFilekeyForExecution(Execution execution) {
        if (execution.scheduledExecution) {
            return "${execution.project}/job/${execution.scheduledExecution.generateFullName()}/logs/${execution.id}.rdlog"
        } else {
            return "${execution.project}/run/logs/${execution.id}.rdlog"
        }
    }

    private static String getFilepathForExecution(Execution execution) {
        return execution.outputfilepath
    }

    private File getFileForKey(String key) {
        new File(new File(frameworkService.rundeckbase, "var/logs/rundeck"), key)
    }

    private StreamingLogReader getLogReaderForExecution(Execution e) {
        return getLogReaderForFile(getFileForExecution(e))
    }

    private static StreamingLogReader getLogReaderForFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file)
        }
        return new FSStreamingLogReader(file, "UTF-8", rundeckLogFormat);
    }

    /**
     * Return the state of the log file for the execution, and query a storage plugin if defined
     * @param execution
     * @param plugin
     * @return state of the execution log
     */
    private ExecutionLogState getLogFileState(Execution execution, LogFileStoragePlugin plugin) {
        File file = getFileForExecution(execution)
        LogFileState local = (file!=null && file.exists() )? LogFileState.AVAILABLE : LogFileState.NOT_FOUND
        LogFileState remote = LogFileState.NOT_FOUND
        if (null != plugin && local != LogFileState.AVAILABLE) {
            try {
                def newremote=plugin.getState()
                remote=newremote
            } catch (Throwable e) {
                def pluginName = getConfiguredPluginName()
                log.error("Failed to get state of file storage plugin ${pluginName}: " + e.message)
                log.debug("Failed to get state of file storage plugin ${pluginName}: " + e.message, e)
            }
        }
        def state = ExecutionLogState.forFileStates(local, remote)

        log.error("getLogFileState ${state} forFileStates: ${local}, ${remote}")
        return state
    }

    private LogFileStoragePlugin getConfiguredPluginForExecution(Execution execution) {
        def jobcontext = ExecutionService.exportContextForExecution(execution)
        def plugin = getConfiguredPlugin(jobcontext)
        plugin
    }

    private String getConfiguredPluginName(){
        grailsApplication.config.rundeck?.execution?.logs?.fileStoragePlugin
    }
    private LogFileStoragePlugin getConfiguredPlugin(Map context){
        def pluginName=getConfiguredPluginName()
        if (pluginName) {
            log.debug("Using log file storage plugin ${pluginName}")

            try {
                def plugin = getPlugin(pluginName)
                if (plugin != null) {
                    //TODO: configure plugin from properties
                    plugin.initialize(context)
                    return plugin
                }
            } catch (Throwable e) {
                log.error("Failed to initialize reader plugin ${pluginName}: " + e.message)
                log.debug("Failed to initialize reader plugin ${pluginName}: " + e.message, e)
            }
        }
        return null
    }
    /**
     * Return an ExecutionLogFileReader containing state of logfile availability, and reader if available
     * @param e execution
     * @param performLoad if true, perform remote file transfer
     * @return
     */
    ExecutionLogReader requestLogFileReader(Execution e, performLoad = true) {
        if (!e.outputfilepath && e.dateCompleted == null && e.dateStarted != null) {
            //assume pending write locally
            //todo: compare server uuid
            return new ExecutionLogReader(state: ExecutionLogState.PENDING_LOCAL, reader: null)
        }
        def plugin= getConfiguredPluginForExecution(e)
        def state = getLogFileState(e, plugin)
//        def requestState = logFileRetrievalRequestState(e)
        def reader = null
        switch (state) {
            case ExecutionLogState.AVAILABLE:
                reader = getLogReaderForExecution(e)
                break
            case ExecutionLogState.AVAILABLE_REMOTE:
                if (performLoad) {
                    state = requestLogFileRetrieval(e, plugin)
                }else {
                    def requeststate = logFileRetrievalRequestState(e)
                    if(null!=requeststate) {
                        state = requeststate
                    }
                }
        }
        log.error("file state: ${state}")
        return new ExecutionLogReader(state: state, reader: reader)
    }

    private ConcurrentHashMap<String, Object> logFileStorageRequests = new ConcurrentHashMap<String, Object>()
    private ConcurrentHashMap<String, Map> logFileRetrievalRequests = new ConcurrentHashMap<String, Map>()
//    private ConcurrentHashMap<String, LogFileState> logFileRetrievalStates = new ConcurrentHashMap<String, LogFileState>()

    private String logFileRetrievalKey(Execution execution){
        return execution.id.toString()
    }

    private ExecutionLogState logFileRetrievalRequestState(Execution execution) {
        def key = logFileRetrievalKey(execution)
        def pending = logFileRetrievalRequests.get(key)
        if (pending != null) {
            log.error("logFileRetrievalRequestState, already pending: ${pending.state}")
            //request already in progress
            return pending.state
        }
        return null
    }
    private ExecutionLogState requestLogFileRetrieval(Execution execution, LogFileStorage plugin){
        def key=logFileRetrievalKey(execution)
        Map newstate = new ConcurrentHashMap([state: ExecutionLogState.PENDING_LOCAL])
        def pending=logFileRetrievalRequests.putIfAbsent(key, newstate)
        if(pending!=null){
            log.error("requestLogFileRetrieval, already pending: ${pending.state}")
            //request already in progress
            return pending.state
        }
        log.error("requestLogFileRetrieval, start a new request...")
//        logFileRetrievalStates.remove(key)
        def file=new File(getFilepathForExecution(execution))
        newstate.future = logFileTaskExecutor.submit({
            if(!retrieveLogFile(file, plugin)){
                log.error("LogFileStorage: failed retrieval request for ${key}")
            }
//            logFileRetrievalStates.put(key, success ? LogFileState.AVAILABLE : LogFileState.NOT_FOUND)
            logFileRetrievalRequests.remove(key)
        })
        return ExecutionLogState.PENDING_LOCAL
    }

    /**
     * Store the log file for a completed execution using the storage method
     * @param execution
     * @param storage plugin that is already initialized
     */
    private Future storeLogFileAsync(File file, LogFileStorage storage) {
        return logFileTaskExecutor.submit({
            log.error("Start asynch task, store log file")
            storeLogFile(file, storage)
        })
    }
    /**
     * Store the log file for a completed execution using the storage method
     * @param execution
     * @param storage plugin that is already initialized
     */
    private Boolean storeLogFile(File file, LogFileStorage storage) {
        log.error("Start task, store log file")
        def success = false
        file.withInputStream { input ->
            storage.storeLogFile(input)
            success = true
        }
        if (success) {
            file.deleteOnExit()
            //TODO: mark file to be cleaned up in future
        }
        log.error("Finish task, store log file: ${success}")
        return success
    }

    /**
     * Retrieves a log file for the given execution using a storage method
     * @param execution
     * @param storage plugin that is already initialized
     * @return
     */
    private Boolean retrieveLogFile(File file, LogFileStorage storage){
        def tempfile = File.createTempFile("temp-storage","logfile")
        tempfile.deleteOnExit()
        def success=false
        tempfile.withOutputStream {out->
            storage.retrieveLogFile(out)
            success=true
        }
        if(success){
           tempfile.renameTo(file)
        }else{
            tempfile.delete()
        }
        return success
    }
}
