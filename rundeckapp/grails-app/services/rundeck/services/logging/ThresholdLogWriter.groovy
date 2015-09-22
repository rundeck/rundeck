package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used to log that a threshold was reached, and optionally truncate further output
 */
class ThresholdLogWriter extends FilterStreamingLogWriter {
    LoggingThreshold threshold
    final boolean truncate
    AtomicBoolean limitReached = new AtomicBoolean(false)

    ThresholdLogWriter(final StreamingLogWriter writer, final LoggingThreshold threshold) {
        super(writer)
        this.threshold = threshold
        this.truncate = threshold.isTruncateOnLimitReached()
    }

    @Override
    void addEvent(final LogEvent event) {
        def limit = limitReached.get()
        if (truncate && limit) {
            return
        }
        getWriter().addEvent(event)

        if (!limit && threshold.isThresholdExceeded() && limitReached.compareAndSet(false, true)) {
            getWriter().addEvent(LogUtil.logError("Log output limit exceeded: " + threshold.description))
        }
    }
}
