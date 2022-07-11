package rundeck.services.logging

import rundeck.ScheduledExecution
import rundeck.services.execution.ValueHolder
import spock.lang.Specification

class LoggingThresholdSpec extends Specification {

    class tHolder implements ValueHolder<Long> {
        Long value
    }

    void "test both job and global limit are null"() {
        given:
        def jobLimitMap    = null
        def globalLimitMap = null
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = null
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold == null
    }

    void "test null global output limit"() {
        given:
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("100KB")
        def globalLimitMap = null
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.maxValue == 102400
    }

    void "test null job output limit"() {
        given:
        def jobLimitMap    = null
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.maxValue == 102400
    }

    void "test different units of output limit"() {
        given:
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("100/node")
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.type == LoggingThreshold.TOTAL_FILE_SIZE
        threshold.maxValue == 102400
    }

    void "test minimum output limit"() {
        given:
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("200KB")
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.maxValue == 102400
    }

    void "test limit action with job limit null"() {
        given:
        def jobLimitMap    = null
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.action == globalLimitAction
    }

    void "test limit action with global limit null"() {
        given:
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("100KB")
        def globalLimitMap = null
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.action == jobLimitAction
    }

    void "test limit action with global action as truncate"() {
        given:
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("100KB")
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitAction    = LoggingThreshold.ACTION_HALT
        def globalLimitAction = LoggingThreshold.ACTION_TRUNCATE
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.action == jobLimitAction
    }

    void "test limit action with global action as halt"() {
        given:
        def jobLimitMap    = ScheduledExecution.parseLogOutputThreshold("100KB")
        def globalLimitMap = ScheduledExecution.parseLogOutputThreshold("100KB")
        def jobLimitAction    = LoggingThreshold.ACTION_TRUNCATE
        def globalLimitAction = LoggingThreshold.ACTION_HALT
        when:
        def jobThreshold    = LoggingThreshold.fromMap(jobLimitMap, jobLimitAction)
        def globalThreshold = LoggingThreshold.fromMap(globalLimitMap, globalLimitAction)
        def threshold       = LoggingThreshold.createMinimum(jobThreshold, globalThreshold)
        then:
        threshold.action == globalLimitAction
    }

    void "test From Map Blank"() {
        when:
        def thresholdBlank = LoggingThreshold.fromMap([:], "blah")
        def thresholdNull = LoggingThreshold.fromMap(null, "blah")
        then:
        thresholdBlank == null
        thresholdNull  == null
    }

    void "test From Map No Max Value"() {
        when:
        def threshold = LoggingThreshold.fromMap([value: 1], "blah")
        then:
        threshold == null
    }

    void "test From Map Max Size"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "blah")
        then:
        threshold.type     == LoggingThreshold.TOTAL_FILE_SIZE
        threshold.action   == 'blah'
        threshold.maxValue == 1
    }

    void "test From Map Max Lines"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxLines: 1], "blah")
        then:
        threshold.type     == LoggingThreshold.TOTAL_LINES
        threshold.action   == 'blah'
        threshold.maxValue == 1
    }

    void "test From Map Max Lines per Node False"() {
        when:
        def threshold = LoggingThreshold.fromMap(maxLines: 1, perNode: false, "blah")
        then:
        threshold.type     == LoggingThreshold.TOTAL_LINES
        threshold.action   == 'blah'
        threshold.maxValue == 1
    }

    void "test From Map Max Node Lines"() {
        when:
        def threshold = LoggingThreshold.fromMap(maxLines: 1, perNode: true, "blah")
        then:
        threshold.type     == LoggingThreshold.LINES_PER_NODE
        threshold.action   == 'blah'
        threshold.maxValue == 1
    }

    void "test Action Halt"() {
        when:
        def threshold = LoggingThreshold.fromMap(maxLines: 1, perNode: true, "halt")
        then:
        threshold.truncateOnLimitReached == false
        threshold.haltOnLimitReached     == true
    }

    void "test Action Truncate"() {
        when:
        def threshold = LoggingThreshold.fromMap(maxLines: 1, perNode: true, "truncate")
        then:
        threshold.truncateOnLimitReached == true
        threshold.haltOnLimitReached     == false
    }

    void "test Watcher For Type Lines Per Node"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxLines: 1, perNode: true], "truncate")
        then:
        threshold != null
        threshold.watcherForType("blah")                     == null
        threshold.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE) == null
        threshold.watcherForType(LoggingThreshold.TOTAL_LINES)     == null
        threshold.watcherForType(LoggingThreshold.LINES_PER_NODE)  != null
    }

    void "test Watcher For Type Max Size"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        then:
        threshold != null
        threshold.watcherForType("blah")                     == null
        threshold.watcherForType(LoggingThreshold.TOTAL_FILE_SIZE) != null
        threshold.watcherForType(LoggingThreshold.TOTAL_LINES)     == null
        threshold.watcherForType(LoggingThreshold.LINES_PER_NODE)  == null
    }

    void "test get Threshold Value noWatcher"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        then:
        threshold != null
        threshold.value == -1
    }

    void "test get Threshold Value watcher"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        def holder = new tHolder(value: 0)
        threshold.watch(holder)
        then:
        holder.value    == 0
        threshold.value == 0
    }

    void "test is Threshold Exceeded less than false"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 2], "truncate")
        def holder = new tHolder(value: 1)
        threshold.watch(holder)
        then:
        holder.value    == 1
        threshold.value == 1
        threshold.isThresholdExceeded() == false
    }

    void "test is Threshold Exceeded equals false"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        def holder = new tHolder(value: 1)
        threshold.watch(holder)
        then:
        holder.value    == 1
        threshold.value == 1
        threshold.isThresholdExceeded() == false
    }

    void "test is Threshol dExceeded true"() {
        when:
        def threshold = LoggingThreshold.fromMap([maxSizeBytes: 1], "truncate")
        def holder = new tHolder(value: 2)
        threshold.watch(holder)
        then:
        holder.value    == 2
        threshold.value == 2
        threshold.isThresholdExceeded() == true
    }

    void "evaluate the minimum maxvalue"() {
        when:
        def thresholdGlobal = LoggingThreshold.fromMap([maxSizeBytes: 1, type: "size", description: "global"], "truncate")
        def thresholdLocal = LoggingThreshold.fromMap([maxSizeBytes: 2, type: "size", description: "local"], "truncate")
        def threshold = LoggingThreshold.createMinimum(thresholdLocal, thresholdGlobal)
        then:
        threshold.maxValue == 1
        threshold.type == "size"
    }
}
