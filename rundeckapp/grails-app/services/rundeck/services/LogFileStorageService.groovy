package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.logging.LogFileStorage
import com.dtolabs.rundeck.core.logging.LogFileStorageException
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.services.LogFileStoragePluginProviderService
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.task.AsyncTaskExecutor
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.services.logging.EventStreamingLogWriter
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class LogFileStorageService implements InitializingBean{

    static transactional = false
    static final RundeckLogFormat rundeckLogFormat = new RundeckLogFormat()
    LogFileStoragePluginProviderService logFileStoragePluginProviderService
    PluginService pluginService
    def frameworkService
    def AsyncTaskExecutor logFileTaskExecutor
    def executorService

    /**
     * Scheduled executor for retries
     */
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1)
    /**
     * Queue of log storage requests
     */
    private BlockingQueue<Map> storageRequests = new LinkedBlockingQueue<Map>()
    /**
     * Queue of log retrieval requests
     */
    private BlockingQueue<Map> retrievalRequests = new LinkedBlockingQueue<Map>()
    /**
     * Currently running requests
     */
    private Queue<Map> running = new ConcurrentLinkedQueue<Map>()
    /**
     * Map of log retrieval actions
     */
    private ConcurrentHashMap<String, Map> logFileRetrievalRequests = new ConcurrentHashMap<String, Map>()
    private ConcurrentHashMap<String, Map> logFileRetrievalResults = new ConcurrentHashMap<String, Map>()
    @Override
    void afterPropertiesSet() throws Exception {
        def pluginName = getConfiguredPluginName()
        if(!pluginName){
            //System.err.println("LogFileStoragePlugin not configured, disabling...")
            return
        }
        logFileTaskExecutor.execute( new TaskRunner<Map>(storageRequests,{ Map task ->
            runStorageRequest(task)
        }))
        logFileTaskExecutor.execute( new TaskRunner<Map>(retrievalRequests,{ Map task ->
            runRetrievalRequest(task)
        }))
    }
    List getCurrentRetrievalRequests(){
        return new ArrayList(retrievalRequests)
    }
    List getCurrentStorageRequests(){
        return new ArrayList(storageRequests)
    }
    List getCurrentRequests(){
        return new ArrayList(running)
    }
    Map getCurrentRetrievalResults(){
        return new HashMap<String,Map>(logFileRetrievalResults)
    }
    /**
     * Run a storage request task, and if it fails submit a retry depending on the configured retry count and delay
     * @param task
     */
    void runStorageRequest(Map task){
        int retry = getConfiguredStorageRetryCount()
        int delay = getConfiguredStorageRetryDelay()
        running << task
        int count=task.count?:0;
        task.count = ++count
        log.debug("Storage request [ID#${task.id}] (attempt ${count} of ${retry})...")
        def success = storeLogFile(task.file, task.storage, task.id)
        if (!success && count < retry) {
            log.debug("Storage request [ID#${task.id}] was not successful, retrying in ${delay} seconds...")
            running.remove(task)
            queueLogStorageRequest(task, delay)
        } else if (!success) {
            log.error("Storage request [ID#${task.id}] FAILED ${retry} attempts, giving up")
            running.remove(task)
        } else {
            //use executorService to run within hibernate session
            executorService.execute {
                log.debug("executorService saving storage request status...")
                task.request.completed = success
                task.request.save(flush: true)
                running.remove(task)
                log.debug("Storage request [ID#${task.id}] complete.")
            }
        }
    }

    /**
     * Run retrieval request task with no retries, executes in the logFileTaskExecutor threadpool
     * @param task
     */
    private void runRetrievalRequest(Map task) {

        logFileTaskExecutor.execute{
            running << task
            def result = retrieveLogFile(task.file, task.storage,task.id)
            def success=result.success
            if (!success) {
                log.error("LogFileStorage: failed retrieval request for ${task.id}")
                def cache = [
                        state: result.error ? LogFileState.ERROR : LogFileState.NOT_FOUND,
                        time: new Date(),
                        count: task.count ? task.count + 1 : 1,
                        errorCode: 'execution.log.storage.retrieval.ERROR',
                        errorData: [task.name, result.error],
                        error: result.error
                ]

                logFileRetrievalResults.put(task.id, task + cache)
            }else{
                logFileRetrievalResults.remove(task.id)
            }
            logFileRetrievalRequests.remove(task.id)
            running.remove(task)
        }
    }
    /**
     * Return the configured retry count
     * @return
     */
    int getConfiguredStorageRetryCount() {
        def count = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStorage?.storageRetryCount ?: 0
        if(count instanceof String){
            count = count.toInteger()
        }
        count > 0 ? count : 1
    }
    /**
     * Return the configured retry delay in seconds
     * @return
     */
    int getConfiguredStorageRetryDelay() {
        def delay = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStorage?.storageRetryDelay ?: 0
        if (delay instanceof String) {
            delay = delay.toInteger()
        }
        delay > 0 ? delay : 60
    }
    /**
     * Return the configured retry count
     * @return
     */
    int getConfiguredRetrievalRetryCount() {
        def count = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStorage?.retrievalRetryCount ?: 0
        if(count instanceof String){
            count = count.toInteger()
        }
        count > 0 ? count : 3
    }
    /**
     * Return the configured retry delay in seconds
     * @return
     */
    int getConfiguredRetrievalRetryDelay() {
        def delay = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStorage?.retrievalRetryDelay ?: 0
        if (delay instanceof String) {
            delay = delay.toInteger()
        }
        delay > 0 ? delay : 60
    }
    /**
     * Return the configured remote pending delay in seconds
     * @return
     */
    int getConfiguredRemotePendingDelay() {
        def delay = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStorage?.remotePendingDelay ?: 0
        if (delay instanceof String) {
            delay = delay.toInteger()
        }
        delay > 0 ? delay : 120
    }

    /**
     * Return the configured plugin name
     * @return
     */
    String getConfiguredPluginName() {
        def plugin = ConfigurationHolder.config.rundeck?.execution?.logs?.fileStoragePlugin
        return (plugin instanceof String) ? plugin : null
    }
    /**
     * Create a streaming log writer for the given execution.
     * @param e execution
     * @param defaultMeta default metadata for logging
     * @param resolver @return
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
        def plugin = getConfiguredPluginForExecution(e, frameworkService.getFrameworkPropertyResolver(e.project))
        if(null!=plugin) {
            LogFileStorageRequest request = new LogFileStorageRequest(execution: e, pluginName: getConfiguredPluginName(), completed: false)
            request.save()
            fsWriter = new EventStreamingLogWriter(fsWriter)
            fsWriter.onClose {
                storeLogFileAsync(e.id.toString(), file, plugin, request)
            }
        }
        return fsWriter
    }
    /**
     * Resume log storage requests for the given serverUUID, or null for unspecified
     * @param serverUUID
     */
    void resumeIncompleteLogStorage(String serverUUID){
        def incomplete = LogFileStorageRequest.findAllByCompleted(false)
        log.debug("resumeIncompleteLogStorage: incomplete count: ${incomplete.size()}, serverUUID: ${serverUUID}")
        //use a slow start to process backlog storage requests
        def delayInc = getConfiguredStorageRetryDelay()
        def delay = delayInc
        incomplete.each{ LogFileStorageRequest request ->
            Execution e = request.execution
            if (serverUUID == e.serverNodeUUID) {
                log.info("re-queueing incomplete log storage request for execution ${e.id}")
                def path = generateFilekeyForExecution(e)
                File file = getFileForKey(path)
                def plugin = getConfiguredPluginForExecution(e, frameworkService.getFrameworkPropertyResolver(e.project))
                if(null!=plugin){
                    //re-queue storage request
                    storeLogFileAsync(e.id.toString(),file, plugin, request, delay)
                    delay += delayInc
                }else{
                    log.error("cannot re-queue incomplete log storage request for execution ${e.id}, plugin was not available: ${getConfiguredPluginName()}")
                }
            }
        }
    }

    /**
     * Return the File for the execution log
     * @param execution
     * @return
     */
    File generateFilepathForExecution(Execution execution) {
        return getFileForKey(generateFilekeyForExecution(execution))
    }

    /**
     * Return the local file path for the log file for the execution, which may be different than the stored
     * outputfilepath on the current server node
     * @param execution
     * @return
     */
    private File getFileForExecution(Execution execution) {
        if (frameworkService.isClusterModeEnabled() && (execution.serverNodeUUID != frameworkService.getServerUUID())) {
            //execution on another rundeck server, generate a local filepath
            return generateFilepathForExecution(execution)
        }
        return execution.outputfilepath?new File(execution.outputfilepath): generateFilepathForExecution(execution)
    }

    /**
     * Generate a relative path for log file of the given execution
     * @param execution
     * @return
     */
    private static String generateFilekeyForExecution(Execution execution) {
        if (execution.scheduledExecution) {
            return "${execution.project}/job/${execution.scheduledExecution.generateFullName()}/logs/${execution.id}.rdlog"
        } else {
            return "${execution.project}/run/logs/${execution.id}.rdlog"
        }
    }

    /**
     * Return the local File for the given key
     * @param key
     * @return
     */
    private File getFileForKey(String key) {
        new File(new File(frameworkService.rundeckBase, "var/logs/rundeck"), key)
    }

    /**
     * REturn a new file log reader for the execution log file
     * @param e
     * @return
     */
    private StreamingLogReader getLogReaderForExecution(Execution e) {
        return getLogReaderForFile(getFileForExecution(e))
    }

    /**
     * Return a new File log reader for rundeck file format
     * @param file
     * @return
     */
    private static StreamingLogReader getLogReaderForFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file)
        }
        return new FSStreamingLogReader(file, "UTF-8", rundeckLogFormat);
    }

    /**
     * Determine the state of the log file, based on the local file and previous or current plugin requests
     * @param execution
     * @param plugin
     * @return state of the execution log
     */
    private Map getLogFileState(Execution execution, LogFileStoragePlugin plugin) {
        File file = getFileForExecution(execution)
        def key = logFileRetrievalKey(execution)

        //check local file
        LogFileState local = (file!=null && file.exists() )? LogFileState.AVAILABLE : LogFileState.NOT_FOUND
        if(local == LogFileState.AVAILABLE) {
            return [state: ExecutionLogState.AVAILABLE]
        }

        LogFileState remote = null

        //consider the state to be PENDING_REMOTE if not found, but we are within a pending grace period
        // after execution completed
        ExecutionLogState remoteNotFound = ExecutionLogState.PENDING_REMOTE
        long pendingDelay = TimeUnit.MILLISECONDS.convert(getConfiguredRemotePendingDelay(), TimeUnit.SECONDS)
        if (execution.dateCompleted != null &&  ((System.currentTimeMillis() - execution.dateCompleted.time) > pendingDelay)) {
            //report NOT_FOUND if not found after execution completed and after the pending grace period
            remoteNotFound = ExecutionLogState.NOT_FOUND
        }
        String errorCode=null
        List errorData=null

        //check results cache
        def previous = getRetrievalCacheResult(key)
        if (previous != null ) {
            //retrieval result is fresh within the cache
            remote = previous.state
            errorCode = previous.errorCode
            errorData = previous.errorData
            def state = ExecutionLogState.forFileStates(local, remote, remoteNotFound)
            log.debug("getLogFileState(${execution.id},${plugin}) (CACHE): ${state} forFileStates: ${local}, ${remote}")
            return [state: state, errorCode: errorCode, errorData: errorData]
        }

        //check active request
        if (null == remote) {
            def requeststate = logFileRetrievalRequestState(execution)
            if (null != requeststate) {
                log.debug("getLogFileState(${execution.id},${plugin}) (RUNNING): ${requeststate}")
                //retrieval request is already running
                return [state: requeststate]
            }
        }

        //query plugin to see if it is available
        if (null == remote && null != plugin) {
            /**
             * If plugin exists, assume NOT_FOUND is actually pending
             */
            def errorMessage=null
            try {
                def newremote = plugin.isAvailable() ? LogFileState.AVAILABLE : LogFileState.NOT_FOUND
                remote = newremote
            } catch (Throwable e) {
                def pluginName = getConfiguredPluginName()
                log.error("Failed to get state of file storage plugin ${pluginName}: " + e.message)
                log.debug("Failed to get state of file storage plugin ${pluginName}: " + e.message, e)
                errorCode = 'execution.log.storage.state.ERROR'
                errorMessage = e.message
                errorData = [pluginName, errorMessage]
                remote = LogFileState.ERROR

            }
            if (remote != LogFileState.AVAILABLE) {
                cacheRetrievalState(key, remote, 0, errorMessage)
            }
        }
        def state = ExecutionLogState.forFileStates(local, remote, remoteNotFound)

        log.debug("getLogFileState(${execution.id},${plugin}): ${state} forFileStates: ${local}, ${remote}")
        return [state: state, errorCode: errorCode, errorData: errorData]
    }

    /**
     * Get a previous retrieval cache result, if it is not expired, or has no more retries
     * @param key
     * @return task result
     */
    Map getRetrievalCacheResult(String key) {
        def previous = logFileRetrievalResults.get(key)
        if (previous != null && isResultCacheItemFresh(previous)) {
            log.debug("getRetrievalCacheResult, previous result still cached: ${previous}")
            //retry delay is not expired
            return previous
        } else if (previous != null && !isResultCacheItemAllowedRetry(previous)) {
            //no more retries
            log.warn("getRetrievalCacheResult, reached max retry count of ${previous.count} for ${key}, not retrying")
            return previous
        }else if(previous!=null){
            log.warn("getRetrievalCacheResult, expired cache result: ${previous}")
        }
        return null
    }

    Map cacheRetrievalState(String key, LogFileState state, int count, String error = null) {
        def name= getConfiguredPluginName();
        def cache = [
                id:key,
                name:name,
                state: state,
                time: new Date(),
                count: count,
        ]
        if (error) {
            cache.errorCode = 'execution.log.storage.retrieval.ERROR'
            cache.errorData = [name, error]
            cache.error = error
        }
        def previous = logFileRetrievalResults.put(key, cache)
        if (null != previous) {
            log.warn("cacheRetrievalState: replacing cached state for ${key}: ${previous}")
            cache.count=previous.count
        }else{
            log.debug("cacheRetrievalState: cached state for ${key}: ${cache}")
        }
        return cache;
    }

    /**
     * Create and initialize the log file storage plugin for this execution, or return null
     * @param execution
     * @param resolver @return
     */
    private LogFileStoragePlugin getConfiguredPluginForExecution(Execution execution, PropertyResolver resolver) {
        def jobcontext = ExecutionService.exportContextForExecution(execution)
        def plugin = getConfiguredPlugin(jobcontext, resolver)
        plugin
    }

    /**
     * Create and initialize the log file storage plugin for the context, or return null
     * @param context
     * @param resolver @return
     */
    private LogFileStoragePlugin getConfiguredPlugin(Map context, PropertyResolver resolver){
        def pluginName=getConfiguredPluginName()
        if (!pluginName) {
            return null
        }
        log.debug("Using log file storage plugin ${pluginName}")
        def plugin
        try {
            plugin= pluginService.configurePlugin(pluginName, logFileStoragePluginProviderService, resolver, PropertyScope.Instance)
        } catch (Throwable e) {
            log.error("Failed to create LogFileStoragePlugin '${pluginName}': ${e.class.name}:" + e.message)
            log.debug("Failed to create LogFileStoragePlugin '${pluginName}': ${e.class.name}:" + e.message, e)
        }
        if (plugin != null) {
            try {
                plugin.initialize(context)
                return plugin
            } catch (Throwable e) {
                log.error("Failed to initialize LogFileStoragePlugin '${pluginName}': ${e.class.name}: " + e.message)
                log.debug("Failed to initialize LogFileStoragePlugin '${pluginName}': ${e.class.name}: " + e.message, e)
            }
        }
        return null
    }
    /**
     * Return an ExecutionLogFileReader containing state of logfile availability, and reader if available
     * @param e execution
     * @param performLoad if true, perform remote file transfer
     * @param resolver @return
     */
    ExecutionLogReader requestLogFileReader(Execution e, boolean performLoad = true) {
        //handle cases where execution is still running or just started
        //and the file may not be available yet
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
        def plugin= getConfiguredPluginForExecution(e, frameworkService.getFrameworkPropertyResolver(e.project))

        //check the state via local file, cache results, and plugin
        def result = getLogFileState(e, plugin)
        def state = result.state
        def reader = null
        switch (state) {
            case ExecutionLogState.AVAILABLE:
                reader = getLogReaderForExecution(e)
                break
            case ExecutionLogState.AVAILABLE_REMOTE:
                if (performLoad) {
                    state = requestLogFileRetrieval(e, plugin)
                }
        }
        log.debug("requestLogFileRetrieval(${e.id},${performLoad}): ${state}")
        return new ExecutionLogReader(state: state, reader: reader, errorCode: result.errorCode, errorData: result.errorData)

    }

    /**
     * Return a key to identify a request
     * @param execution
     * @return
     */
    private static String logFileRetrievalKey(Execution execution){
        return execution.id.toString()
    }

    /**
     * Get the state of any existing retrieval request, or null if none exists
     * @param execution the execution
     * @return state of the log file
     */
    private ExecutionLogState logFileRetrievalRequestState(Execution execution) {
        def key = logFileRetrievalKey(execution)
        def pending = logFileRetrievalRequests.get(key)
        if (pending != null) {
            log.debug("logFileRetrievalRequestState, already pending: ${pending.state}")
            //request already in progress
            return pending.state
        }
        return null
    }

    /**
     * Request a log file be retrieved, and return the current state of the execution log. If a request for the
     * same file has already been submitted, it will not be duplicated.
     * @param execution execution object
     * @param plugin storage method
     * @return state of the log file
     */
    private ExecutionLogState requestLogFileRetrieval(Execution execution, LogFileStorage plugin){
        def key=logFileRetrievalKey(execution)
        def file = getFileForExecution(execution)
        Map newstate = [state: ExecutionLogState.PENDING_LOCAL, file: file, storage: plugin, id: key, name: getConfiguredPluginName(),count:0]
        def previous = logFileRetrievalResults.get(key)
        if(previous!=null){
            newstate.count=previous.count
        }
        def pending=logFileRetrievalRequests.putIfAbsent(key, newstate)
        if(pending!=null){
            log.debug("requestLogFileRetrieval, already pending for ${key}: ${pending.state}")
            //request already started
            return pending.state
        }
        //remove previous result
        logFileRetrievalResults.remove(key)
        log.debug("requestLogFileRetrieval, queueing a new request (attempt ${newstate.count+1}) for ${key}...")
        retrievalRequests<<newstate
        return ExecutionLogState.PENDING_LOCAL
    }

    /**
     * Return true if the retrieval task result cache time is within the retry delay
     * @param previous
     * @return
     */
     boolean isResultCacheItemFresh(Map previous){
        int retryDelay = getConfiguredRetrievalRetryDelay()
        long ms = TimeUnit.MILLISECONDS.convert(retryDelay, TimeUnit.SECONDS).longValue()
        Date cacheTime = previous.time
        return System.currentTimeMillis() < (cacheTime.time + ms)
    }

    /**
     * Return true if the retrieval task result retry count is within the max retries
     * @param previous
     * @return
     */
     boolean isResultCacheItemAllowedRetry(Map previous){
        int retryCount = getConfiguredRetrievalRetryCount()
        return previous.count < retryCount
    }
    /**
     * Asynchronously start a request to store a log file for a completed execution using the storage method
     * @param id the request id
     * @param file the file to store
     * @param storage the storage method
     * @param executionLogStorage the persisted object that records the result
     * @param delay seconds to delay the request
     */
    private void storeLogFileAsync(String id, File file, LogFileStorage storage, LogFileStorageRequest executionLogStorage, int delay=0) {
        queueLogStorageRequest([id: id, file: file, storage: storage, request: executionLogStorage], delay)
    }

    /**
     * Queue the request to store a log file
     * @param execution
     * @param storage plugin that is already initialized
     */
    private void queueLogStorageRequest(Map task, int delay=0) {
        if(delay>0){
            scheduledExecutor.schedule({
                queueLogStorageRequest(task)
            }, delay, TimeUnit.SECONDS)
        }else{
            storageRequests<<task
        }
    }
    /**
     * Store the log file for a completed execution using the storage method
     * @param execution
     * @param storage plugin that is already initialized
     */
    private Boolean storeLogFile(File file, LogFileStorage storage, String ident) {
        log.debug("Storage request [ID#${ident}], start")
        def success = false
        Date lastModified = new Date(file.lastModified())
        long length = file.length()
        try{
            file.withInputStream { input ->
                success = storage.store(input,length,lastModified)
            }
        }catch (Throwable e) {
            log.error("Storage request [ID#${ident}] error: ${e.message}")
            log.debug("Storage request [ID#${ident}] error: ${e.message}", e)
        }
        if (success) {
            file.deleteOnExit()
            //TODO: mark file to be cleaned up in future
        }
        log.debug("Storage request [ID#${ident}], finish: ${success}")
        return success
    }

    /**
     * Retrieves a log file for the given execution using a storage method
     * @param execution
     * @param storage plugin that is already initialized
     * @return Map containing success: true/false, and error: String indicating the error if there was one
     */
    private Map retrieveLogFile(File file, LogFileStorage storage, String ident){
        def tempfile = File.createTempFile("temp-storage","logfile")
        tempfile.deleteOnExit()
        def success=false
        def psuccess=false
        def errorMessage=null
        try {
            tempfile.withOutputStream { out ->
                try {
                    psuccess = storage.retrieve(out)
                } catch (LogFileStorageException e) {
                    errorMessage=e.message
                }
            }
            if(psuccess) {
                if (!file.getParentFile().isDirectory()) {
                    if (!file.getParentFile().mkdirs()) {
                        errorMessage="Failed to create directories for file: ${file}"
                    }
                }
                if (!tempfile.renameTo(file)) {
                    errorMessage = "Failed to move temp file to location: ${file}"
                } else {
                    success = true
                }
            }
            log.debug("Retrieval request [ID#${ident}], result: ${success}, error? ${errorMessage}")

        } catch (Throwable t) {
            errorMessage = "Failed retrieve log file: ${t.message}"
            log.debug("Retrieval request [ID#${ident}]: Failed retrieve log file: ${t.message}", t)
        }
        if(!success){
            log.error("Retrieval request [ID#${ident}] error: ${errorMessage}")
            tempfile.delete()
        }
        return [success: success, error: errorMessage]
    }
}
