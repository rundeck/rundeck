package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogWriter
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import rundeck.Execution
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.LogState

class LogFileStorageService {

    static transactional = false
    static final RundeckLogFormat rundeckLogFormat = new RundeckLogFormat()
    def frameworkService

    /**
     * Create a streaming log writer for the given execution.
     * @param e
     * @param logThreshold
     * @param defaultMeta
     * @return
     */
    StreamingLogWriter getLogFileWriterForExecution(Execution e, LogLevel logThreshold, Map<String, String> defaultMeta) {
        def path = generateFilekeyForExecution(e)
        File file = getFileForKey(path)

        if (!file.getParentFile().isDirectory()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IllegalStateException("Unable to create directories for storage: " + file)
            }
        }
        //TODO: if remote log storage plugin available, apply hook to call storeLogFile on close
        return new FSStreamingLogWriter(new FileOutputStream(file), logThreshold, defaultMeta, rundeckLogFormat)
    }

    File generateFilepathForExecution(Execution execution) {
        return getFileForKey(generateFilekeyForExecution(execution))
    }
    private File getFileForExecution(Execution execution) {
        return new File(execution.outputfilepath)
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
        new File(new File(frameworkService.rundeckbase, "var/logs/rundeck"), key )
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

    LogState getLogFileState(Execution e) {
        File file = getFileForExecution(e)
        if(file.exists()){
            return LogState.FOUND_LOCAL
        }
        //TODO: plugins for remote storage
        return LogState.NOT_FOUND
    }

    /**
     * Return an ExecutionLogFileReader containing state of logfile availability, and reader if available
     * @param e execution
     * @param performLoad if true, perform remote file transfer
     * @return
     */
    ExecutionLogReader requestLogFileReader(Execution e, performLoad = true) {
        def state = getLogFileState(e)
        def reader=null
        switch (state){
            case LogState.FOUND_LOCAL:
                reader = getLogReaderForExecution(e)
                break
            case LogState.FOUND_REMOTE:
                if(performLoad){
                    //TODO: start asynch remote load if state is not present
                    throw new IllegalStateException("Not implemented")
                }
                break
            case LogState.PENDING_LOCAL:
            case LogState.PENDING_REMOTE:
            case LogState.NOT_FOUND:
                reader=null
        }
        return new ExecutionLogReader(state: state, reader: reader)
    }

    /**
     * Store the log file for a completed execution.
     * @param e
     */
    private storeLogFile(Execution e) {
        throw new RuntimeException("TODO: not implemented")
    }
}
