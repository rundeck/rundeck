package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import rundeck.services.execution.ValueHolder

/**
 * Created by greg on 9/21/15.
 */
class ThresholdLogWriterTest extends GroovyTestCase {
    class testLogWriter extends NoopLogWriter {
        List<String> messages = []

        @Override
        void addEvent(final LogEvent event) {
            messages << event.message
        }
    }

    class longValueHolder implements ValueHolder<Long> {
        Long value
    }


    public testLogOutputOnThresholdReached() {
        def t = new LoggingThreshold()
        t.description = 'thresholddesc'
        t.maxValue = 1
        def valueHolder = new longValueHolder()
        valueHolder.value = 1
        t.valueHolder = valueHolder
        def logger = new testLogWriter()
        def w = new ThresholdLogWriter(logger, t)

        w.addEvent(LogUtil.logNormal("message1"))
        valueHolder.value = 2 //exceed threshold
        w.addEvent(LogUtil.logNormal("message2"))
        w.addEvent(LogUtil.logNormal("message3"))
        w.addEvent(LogUtil.logNormal("message4"))
        w.addEvent(LogUtil.logNormal("message5"))

        assertEquals(6, logger.messages.size())

        assertEquals(
                ["message1", "message2", "Log output limit exceeded: thresholddesc", "message3", "message4", "message5"],
                logger.messages
        )
    }

    public testTruncate() {
        def t = new LoggingThreshold()
        t.description = 'thresholddesc'
        t.maxValue = 1
        t.action = LoggingThreshold.ACTION_TRUNCATE
        def valueHolder = new longValueHolder()
        valueHolder.value = 1
        t.valueHolder = valueHolder
        def logger = new testLogWriter()
        def w = new ThresholdLogWriter(logger, t)

        w.addEvent(LogUtil.logNormal("message1"))
        valueHolder.value = 2 //exceed threshold
        w.addEvent(LogUtil.logNormal("message2"))
        w.addEvent(LogUtil.logNormal("message3"))
        w.addEvent(LogUtil.logNormal("message4"))
        w.addEvent(LogUtil.logNormal("message5"))

        assertEquals(3, logger.messages.size())
        assertEquals(["message1", "message2", "Log output limit exceeded: thresholddesc"], logger.messages)

    }
}
