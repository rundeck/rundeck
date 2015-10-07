package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import rundeck.services.execution.ValueHolder

/**
 * Counts the total log lines written
 */
class LineCountingLogWriter extends FilterStreamingLogWriter implements ValueHolder<Long> {
    volatile Long value = 0

    LineCountingLogWriter(StreamingLogWriter writer) {
        super(writer)
    }

    @Override
    void addEvent(final LogEvent event) {
        getWriter().addEvent(event)
        if (event.eventType == LogUtil.EVENT_TYPE_LOG) {
            value+=event.message.split('\n').length
        }
    }

}
