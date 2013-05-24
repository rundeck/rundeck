package rundeck.services.logging

import com.dtolabs.rundeck.app.internal.logging.LogOutputStream
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * Log writer which contains a list of other writers
 */
class ExecutionLogWriter implements StreamingLogWriter{
    public static final Logger log = Logger.getLogger(ExecutionLogWriter.class)
    private List<StreamingLogWriter> pluginWriters
    private List<StreamingLogWriter> enabledWriters

    File filepath

    ExecutionLogWriter(List<StreamingLogWriter> pluginWriters) {
        this.pluginWriters = pluginWriters
        this.enabledWriters=[]
    }

    @Override
    void openStream(Map<String, ? extends Object> context) {
        pluginWriters.each{plugin->
            try{
                plugin.openStream(context)
                enabledWriters<<plugin
            }catch (IOException e) {
                log.error("Cannot open stream for plugin: " + e.message, e)
            }catch (RuntimeException e) {
                log.error("Cannot open stream for plugin: " + e.message, e)
            }
        }
    }

    @Override
    void addEntry(LogEvent entry) {
        enabledWriters.each{plugin->
            try {
                plugin.addEntry(entry)
            } catch (Throwable e) {
                log.error("failed addEvent for plugin: " + e.message, e)
            }
        }
    }

    @Override
    void close() {
        enabledWriters*.close()
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
