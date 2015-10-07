package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Created by greg on 9/21/15.
 */
class NoopLogWriter implements StreamingLogWriter{
    @Override
    void openStream() throws IOException {

    }

    @Override
    void addEvent(final LogEvent event) {

    }

    @Override
    void close() {

    }
}
