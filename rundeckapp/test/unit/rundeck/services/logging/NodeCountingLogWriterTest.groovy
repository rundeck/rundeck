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
class NodeCountingLogWriterTest {

    @Test
    void testCounter() {
        def counter = new NodeCountingLogWriter(new NoopLogWriter())
        Assert.assertEquals(0, counter.value)
        counter.addEvent(LogUtil.logError("",[node:'a']))
        Assert.assertEquals(1, counter.value)

        counter.addEvent(LogUtil.logError("monkey",[node:'b']))
        Assert.assertEquals(1, counter.value)

        counter.addEvent(LogUtil.logError("alpha\nbeta",[node:'a']))
        Assert.assertEquals(3, counter.value)
    }
}
