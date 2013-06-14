package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * A StreamingLogWriter which writes to a list of multiple writers.
 * Each writer's {@link StreamingLogWriter#openStream()} will be called, but
 * if an error occurs it will not be enabled.
 * Each enabled writer will then be passed events via addEvent and
 * closed via close().
 */
class MultiLogWriter implements StreamingLogWriter {
    public static final Logger log = Logger.getLogger(MultiLogWriter.class)
    List<StreamingLogWriter> writers

    MultiLogWriter (List<StreamingLogWriter> writers) {
        this.writers = new ArrayList<StreamingLogWriter>(writers)
    }

    @Override
    void openStream() {
        writers*.openStream()
    }

    @Override
    void addEvent(LogEvent event) {
        writers*.addEvent(event)
    }

    @Override
    void close() {
        writers*.close()
    }
}
