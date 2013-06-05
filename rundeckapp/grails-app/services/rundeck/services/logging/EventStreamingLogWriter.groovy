package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/29/13
 * Time: 4:36 PM
 */
class EventStreamingLogWriter extends FilterStreamingLogWriter {
    Closure onAddEvent
    Closure onClose
    Closure onOpenStream

    EventStreamingLogWriter(StreamingLogWriter writer) {
        super(writer)
    }

    @Override
    void openStream() throws IOException {
        super.openStream()
        if (null != onOpenStream) {
            onOpenStream.call()
        }
    }

    @Override
    void close() {
        super.close()
        if (null != onClose) {
            onClose.call()
        }
    }

    void onClose(Closure closure) {
        this.onClose = closure
    }

    void onAddEvent(Closure closure) {
        this.onAddEvent = closure
    }

    void onOpenStream(Closure closure) {
        this.onOpenStream = closure
    }

    @Override
    void addEvent(LogEvent event) {
        super.addEvent(event)
        if (null != onAddEvent) {
            onAddEvent.call(event)
        }
    }
}
