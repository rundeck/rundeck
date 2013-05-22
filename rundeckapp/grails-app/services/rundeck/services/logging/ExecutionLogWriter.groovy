package rundeck.services.logging

import com.dtolabs.rundeck.app.internal.logging.LogOutputStream
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Log writer which contains a list of other writers
 */
class ExecutionLogWriter implements StreamingLogWriter{
    private List<StreamingLogWriter> pluginWriters

    File filepath

    ExecutionLogWriter(List<StreamingLogWriter> pluginWriters) {
        this.pluginWriters = pluginWriters
    }

    @Override
    void addEntry(LogEvent entry) {
        pluginWriters*.addEntry(entry)
    }

    @Override
    void close() {
        pluginWriters*.close()
    }
    // utility methods
    void logError(String message){
        addEntry(LogUtil.logError(message))
    }
    void log(String message){
        addEntry(LogUtil.logNormal(message))
    }
    void logDebug(String message){
        addEntry(LogUtil.logDebug(message))
    }
    void logWarn(String message){
        addEntry(LogUtil.logWarn(message))
    }
    void logVerbose(String message){
        addEntry(LogUtil.logVerbose(message))
    }
}
