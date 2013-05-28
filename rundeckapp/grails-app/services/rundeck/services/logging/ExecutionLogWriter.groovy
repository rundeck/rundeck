package rundeck.services.logging

import com.dtolabs.rundeck.app.internal.logging.LogOutputStream
import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * Log writer which contains a list of other writers
 */
class ExecutionLogWriter extends FilterStreamingLogWriter {
    public static final Logger log = Logger.getLogger(ExecutionLogWriter.class)

    /**
     *
     */
    File filepath

    ExecutionLogWriter(StreamingLogWriter writer) {
        super(writer)
    }
// utility methods
    void logError(String message) {
        addEntry(LogUtil.logError(message))
    }

    void log(String message) {
        addEntry(LogUtil.logNormal(message))
    }

    void logDebug(String message) {
        addEntry(LogUtil.logDebug(message))
    }

    void logWarn(String message) {
        addEntry(LogUtil.logWarn(message))
    }

    void logVerbose(String message) {
        addEntry(LogUtil.logVerbose(message))
    }
}
