package rundeck.services

import com.dtolabs.rundeck.app.internal.logging.LogOutputStream
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import rundeck.Execution
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogWriter

class LoggingService {

    FrameworkService frameworkService
    LogFileStorageService logFileStorageService

    def configure() {
    }

    public ExecutionLogWriter openLogWriter(Execution execution, LogLevel level, Map<String, String> defaultMeta) {
        //todo: load streaming writer plugins
        def pluginWriters = []
        //TODO: configuration to disable file storage
        pluginWriters << logFileStorageService.getLogFileWriterForExecution(execution, level, defaultMeta)
        def writer = new ExecutionLogWriter(pluginWriters)
        //file path support
        writer.filepath=logFileStorageService.generateFilepathForExecution(execution)
        return writer
    }

    public ExecutionLogReader getLogReader(Execution execution) {
        //TODO: configuration to use plugin instead of file system
        ExecutionLogReader reader = logFileStorageService.requestLogFileReader(execution)

        return reader
    }

    public OutputStream createLogOutputStream(StreamingLogWriter logWriter, LogLevel level){
        return new LogOutputStream(logWriter, level)
    }
}
