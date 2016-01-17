package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created by greg on 9/21/15.
 */
@RunWith(JUnit4)
class LineCountingLogWriterTest extends GroovyTestCase {

    @Test
    void testCounter() {
        def counter = new LineCountingLogWriter(new NoopLogWriter())
        Assert.assertEquals(0, counter.value)
        counter.addEvent(LogUtil.logError(""))
        Assert.assertEquals(1, counter.value)
        counter.addEvent(LogUtil.logError("monkey"))
        Assert.assertEquals(2, counter.value)
        counter.addEvent(LogUtil.logError("alpha\nbeta"))
        Assert.assertEquals(4, counter.value)
    }
}
