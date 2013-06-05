package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
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
        addEvent(LogUtil.logError(message))
    }

    void log(String message) {
        addEvent(LogUtil.logNormal(message))
    }

    void logDebug(String message) {
        addEvent(LogUtil.logDebug(message))
    }

    void logWarn(String message) {
        addEvent(LogUtil.logWarn(message))
    }

    void logVerbose(String message) {
        addEvent(LogUtil.logVerbose(message))
    }
}
