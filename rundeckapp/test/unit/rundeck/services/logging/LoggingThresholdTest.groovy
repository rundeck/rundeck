package rundeck.services.logging

import org.junit.Assert
import rundeck.services.execution.ValueHolder

/**
 * Created by greg on 9/21/15.
 */
class LoggingThresholdTest extends GroovyTestCase {
    void testFromMapBlank() {
        Assert.assertNull LoggingThreshold.fromMap([:], "blah")
        Assert.assertNull LoggingThreshold.fromMap(null, "blah")
    }

    void testFromMapNoMaxValue() {
        Assert.assertNull LoggingThreshold.fromMap([value: 1], "blah")
    }

    void testFromMapMaxSize() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "blah")
        Assert.assertNotNull t
        Assert.assertEquals(LoggingThreshold.TOTAL_FILE_SIZE, t.type)
        Assert.assertEquals('blah', t.action)
        Assert.assertEquals(1, t.maxValue)
    }

    void testFromMapMaxLines() {
        def t = LoggingThreshold.fromMap([maxLines: 1], "blah")
        Assert.assertNotNull t
        Assert.assertEquals(LoggingThreshold.TOTAL_LINES, t.type)
        Assert.assertEquals('blah', t.action)
        Assert.assertEquals(1, t.maxValue)
    }

    void testFromMapMaxLines_perNodeFalse() {
        def t = LoggingThreshold.fromMap([maxLines: 1, perNode: false], "blah")
        Assert.assertNotNull t
        Assert.assertEquals(LoggingThreshold.TOTAL_LINES, t.type)
        Assert.assertEquals('blah', t.action)
        Assert.assertEquals(1, t.maxValue)
    }

    void testFromMapMaxNodeLines() {
        def t = LoggingThreshold.fromMap([maxLines: 1, perNode: true], "blah")
        Assert.assertNotNull t
        Assert.assertEquals(LoggingThreshold.LINES_PER_NODE, t.type)
        Assert.assertEquals('blah', t.action)
        Assert.assertEquals(1, t.maxValue)
    }

    void testActionHalt() {
        def t = LoggingThreshold.fromMap([maxLines: 1, perNode: true], "halt")
        Assert.assertNotNull t
        Assert.assertFalse(t.truncateOnLimitReached)
        Assert.assertTrue(t.haltOnLimitReached)
    }

    void testActionTruncate() {
        def t = LoggingThreshold.fromMap([maxLines: 1, perNode: true], "truncate")
        Assert.assertNotNull t
        Assert.assertFalse(t.haltOnLimitReached)
        Assert.assertTrue(t.truncateOnLimitReached)
    }

    void testWatcherForTypeLinesPerNode() {
        def t = LoggingThreshold.fromMap([maxLines: 1, perNode: true], "truncate")
        Assert.assertNotNull t
        Assert.assertNull t.watcherForType("blah")
        Assert.assertNull t.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE)
        Assert.assertNull t.watcherForType(LoggingThreshold.TOTAL_LINES)
        Assert.assertNotNull t.watcherForType(LoggingThreshold.LINES_PER_NODE)
    }

    void testWatcherForTypeTotalLines() {
        def t = LoggingThreshold.fromMap([maxLines: 1], "truncate")
        Assert.assertNotNull t
        Assert.assertNull t.watcherForType("blah")
        Assert.assertNull t.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE)
        Assert.assertNotNull t.watcherForType(LoggingThreshold.TOTAL_LINES)
        Assert.assertNull t.watcherForType(LoggingThreshold.LINES_PER_NODE)
    }

    void testWatcherForTypeMaxSize() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        Assert.assertNotNull t
        Assert.assertNull t.watcherForType("blah")
        Assert.assertNotNull t.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE)
        Assert.assertNull t.watcherForType(LoggingThreshold.TOTAL_LINES)
        Assert.assertNull t.watcherForType(LoggingThreshold.LINES_PER_NODE)
    }

    void testgetThresholdValue_noWatcher() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        Assert.assertNotNull t
        Assert.assertEquals(-1, t.value)
    }
    class tHolder implements ValueHolder<Long>{
        Long value
    }
    void testgetThresholdValue_watcher() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        def holder = new tHolder(value: 0)
        Assert.assertEquals(0,holder.value)
        t.watch(holder)
        Assert.assertEquals(0, t.value)
    }
    void testisThresholdExceeded_lessthan_false() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 2], "truncate")
        def holder = new tHolder(value: 1)
        Assert.assertEquals(1,holder.value)
        t.watch(holder)
        Assert.assertEquals(1, t.value)
        Assert.assertFalse(t.isThresholdExceeded())
    }
    void testisThresholdExceeded_equals_false() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        def holder = new tHolder(value: 1)
        Assert.assertEquals(1,holder.value)
        t.watch(holder)
        Assert.assertEquals(1, t.value)
        Assert.assertFalse(t.isThresholdExceeded())
    }
    void testisThresholdExceeded_true() {
        def t = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        t.watch(new tHolder(value:2))
        Assert.assertEquals(2, t.value)
        Assert.assertTrue(t.isThresholdExceeded())
    }
}
