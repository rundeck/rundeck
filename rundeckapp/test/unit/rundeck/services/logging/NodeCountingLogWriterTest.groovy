package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import junit.framework.Assert

/**
 * Created by greg on 9/21/15.
 */
class NodeCountingLogWriterTest extends GroovyTestCase {

    public testCounter() {
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
