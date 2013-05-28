package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * A StreamingLogWriter which writes to a list of multiple writers.
 * Each writer's {@link StreamingLogWriter#openStream(java.util.Map)} will be called, but
 * if an error occurs it will not be enabled.
 * Each enabled writer will then be passed events via addEvent and
 * closed via close().
 */
class MultiLogWriter implements StreamingLogWriter {
    public static final Logger log = Logger.getLogger(MultiLogWriter.class)
    private List<StreamingLogWriter> writers
    private List<StreamingLogWriter> enabledWriters

    MultiLogWriter (List<StreamingLogWriter> writers) {
        this.writers = new ArrayList<StreamingLogWriter>(writers)
        this.enabledWriters = []
    }

    @Override
    void openStream(Map<String, ? extends Object> context) {
        writers.each { plugin ->
            try {
                plugin.openStream(context)
                enabledWriters << plugin
            } catch (IOException e) {
                log.error("Cannot open stream: " + e.message, e)
            } catch (RuntimeException e) {
                log.error("Cannot open stream: " + e.message, e)
            }
        }
    }

    @Override
    void addEntry(LogEvent event) {
        enabledWriters.each { plugin ->
            try {
                plugin.addEntry(event)
            } catch (Throwable e) {
                log.error("failed addEvent for plugin: " + e.message, e)
            }
        }
    }

    @Override
    void close() {
        enabledWriters*.close()
    }
}
