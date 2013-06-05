package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Filters log events and includes only those below a LogLevel threshold
 */
class LoglevelThresholdLogWriter extends FilterStreamingLogWriter {
    LogLevel threshold

    LoglevelThresholdLogWriter(StreamingLogWriter writer, LogLevel threshold) {
        super(writer)
        this.threshold = threshold
    }

    @Override
    void addEvent(LogEvent event) {
        if (event.loglevel.belowThreshold(threshold)) {
            getWriter().addEvent(event)
        }
    }
}
