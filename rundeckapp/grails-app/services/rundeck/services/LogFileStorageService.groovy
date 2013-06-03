package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.core.common.Framework
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
    LogFileStoragePluginProviderService logFileStoragePluginProviderService
    PluginService pluginService
    def frameworkService
    def grailsApplication
    def AsyncTaskExecutor logFileTaskExecutor

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
        ExecutionLogState remoteNotFound = null

        if (null != plugin && local != LogFileState.AVAILABLE) {
            /**
             * If plugin exists, assume NOT_FOUND is actually pending
             */
            remoteNotFound= ExecutionLogState.PENDING_REMOTE
            try {
                def newremote=plugin.getState()
                remote=newremote
            } catch (Throwable e) {
                def pluginName = getConfiguredPluginName()
                log.error("Failed to get state of file storage plugin ${pluginName}: " + e.message)
                log.debug("Failed to get state of file storage plugin ${pluginName}: " + e.message, e)
            }
        }
        def state = ExecutionLogState.forFileStates(local, remote, remoteNotFound)

        log.error("getLogFileState ${state} forFileStates: ${local}, ${remote}")
        return state
    }

    /**
     * Create and initialize the log file storage plugin for this execution, or return null
     * @param execution
     * @return
     */
    private LogFileStoragePlugin getConfiguredPluginForExecution(Execution execution) {
        def jobcontext = ExecutionService.exportContextForExecution(execution)
        def plugin = getConfiguredPlugin(jobcontext)
        plugin
    }

    private String getConfiguredPluginName(){
        grailsApplication.config.rundeck?.execution?.logs?.fileStoragePlugin
    }

    /**
     * Create and initialize the log file storage plugin for the context, or return null
     * @param context
     * @return
     */
    private LogFileStoragePlugin getConfiguredPlugin(Map context){
        def pluginName=getConfiguredPluginName()
        if (pluginName) {
            log.debug("Using log file storage plugin ${pluginName}")

            try {
                def plugin = pluginService.getPlugin(pluginName,logFileStoragePluginProviderService)
                if (plugin != null) {
                    //TODO: configure plugin from properties
//                    pluginService.configurePlugin(pluginName, projectName, framework, logFileStoragePluginProviderService)
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
        if(e.dateCompleted == null && e.dateStarted != null){
            //execution is running
            if (frameworkService.isClusterModeEnabled() && e.serverNodeUUID != frameworkService.getServerUUID()) {
                //execution on another rundeck server, we have to wait until it is complete
                return new ExecutionLogReader(state: ExecutionLogState.PENDING_REMOTE, reader: null)
            }else if(!e.outputfilepath){
                //no filepath defined: execution started, hasn't created output file yet.
                return new ExecutionLogReader(state: ExecutionLogState.WAITING, reader: null)
            }
        }
        def plugin= getConfiguredPluginForExecution(e)
        def state = getLogFileState(e, plugin)
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
        def file=new File(getFilepathForExecution(execution))
        newstate.future = logFileTaskExecutor.submit({
            log.error("LogFileStorage: start request for ${key}")
            if(!retrieveLogFile(file, plugin)){
                log.error("LogFileStorage: failed retrieval request for ${key}")
            }
            logFileRetrievalRequests.remove(key)
            log.error("LogFileStorage: finish reqest for ${key}")
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
        try{
            file.withInputStream { input ->
                storage.storeLogFile(input)
                success = true
            }
        }catch (Throwable e) {
            log.error("Failed store log file: ${e.message}", e)
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
        try {
            tempfile.withOutputStream {out->
                storage.retrieveLogFile(out)
            }
            if(!file.getParentFile().isDirectory()){
                if(!file.getParentFile().mkdirs()){
                    log.error("Unable to create directories for log file: ${file}")
                }
            }
            if(!tempfile.renameTo(file)){
                log.error("Failed to move log file to location: ${file}")
            }else{
                success = true
            }

        } catch (Throwable t) {
            log.error("Failed retrieve log file: ${t.message}", t)
        }
        if(!success){
            tempfile.delete()
        }
        return success
    }
}
