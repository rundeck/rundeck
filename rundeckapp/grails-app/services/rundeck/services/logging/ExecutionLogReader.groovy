package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.StreamingLogReader

/**
 * Contains a log reader, state and any error code
 */
class ExecutionLogReader extends LogFileLoader {
    StreamingLogReader reader
}
