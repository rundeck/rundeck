package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import rundeck.Execution
import rundeck.ScheduledExecution
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class MicrometerExecutionMetricsServiceSpec extends Specification {

    MicrometerExecutionMetricsService service
    MeterRegistry meterRegistry
    ConfigurationService configurationService
    FrameworkService frameworkService
    ExecutionService executionService

    def setup() {
        meterRegistry = new SimpleMeterRegistry()
        configurationService = Mock(ConfigurationService)
        frameworkService = Mock(FrameworkService)
        executionService = Mock(ExecutionService)

        service = new MicrometerExecutionMetricsService()
        service.meterRegistry = meterRegistry
        service.configurationService = configurationService
        service.frameworkService = frameworkService
        service.executionService = executionService
    }

    private Execution execution(Map props) {
        new Execution([
            project     : 'p1',
            status      : 'true',
            cancelled   : false,
            willRetry   : false,
            timedOut    : false,
            dateStarted : new Date(1000L),
            dateCompleted: new Date(6000L),
        ] + props)
    }

    void "recordExecution increments counter and records duration timer with project/status tags"() {
        given:
            def exec = execution(project: project, status: rawStatus, cancelled: cancelled, timedOut: timedOut)

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.get('rundeck.executions').tag('project', project).tag('status', status).counter().count() == 1.0d
            meterRegistry.get('rundeck.execution.duration').tag('project', project).tag('status', status).timer().count() == 1L

        where:
            project | rawStatus | cancelled | timedOut | status
            'p1'    | 'true'    | false     | false    | 'succeeded'
            'p1'    | 'false'   | false     | false    | 'failed'
            'p2'    | 'false'   | true      | false    | 'aborted'
            'p2'    | 'false'   | false     | true     | 'timedout'
    }

    void "recordExecution skips the duration timer when start or completed timestamp is missing"() {
        given:
            // dateCompleted == null means Execution.getExecutionState() reports 'running' regardless
            // of the started timestamp -- only the "no timer" behavior is under test here.
            def exec = execution(dateStarted: startedDate, dateCompleted: completedDate)

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.meters.find { it.id.name == 'rundeck.executions' } != null
            meterRegistry.meters.find { it.id.name == 'rundeck.execution.duration' } == null

        where:
            startedDate  | completedDate
            null         | new Date(6000L)
            new Date(1000L) | null
    }

    void "recordExecution is a no-op when execution is null"() {
        given:
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordExecution(null)

        then:
            0 * configurationService.getBoolean(*_)
            meterRegistry.meters.size() == registrySize
    }

    void "recordExecution never emits execution_id or user tags, and omits job_name when the job dimension flag is disabled (default)"() {
        given:
            def exec = execution([:])

        when:
            service.recordExecution(exec)

        then:
            // meterRegistry.find(...).tag(k, '_').counter()/.timer() returns null when no meter
            // carries that tag key at all -- this stays on the Search API (proven safe above),
            // instead of reflecting over the concrete Timer/Counter via meter.id.tags.
            meterRegistry.find('rundeck.executions').tag('execution_id', '_').counter() == null
            meterRegistry.find('rundeck.executions').tag('job_name', '_').counter() == null
            meterRegistry.find('rundeck.executions').tag('user', '_').counter() == null
            meterRegistry.find('rundeck.executions').tag('node', '_').counter() == null
            meterRegistry.find('rundeck.execution.duration').tag('execution_id', '_').timer() == null
            meterRegistry.find('rundeck.execution.duration').tag('job_name', '_').timer() == null
    }

    void "recordExecution adds job_id and job_name tags when the job dimension flag is enabled and the execution is scheduled"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def job = new ScheduledExecution(uuid: 'job-uuid-1', jobName: 'my-job', groupPath: null)
            def exec = execution(scheduledExecution: job)

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.get('rundeck.executions')
                .tag('project', 'p1').tag('status', 'succeeded').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .counter().count() == 1.0d
            meterRegistry.get('rundeck.execution.duration')
                .tag('project', 'p1').tag('status', 'succeeded').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .timer().count() == 1L
    }

    void "recordExecution excludes job_id and job_name tags for ad-hoc executions even when the job dimension flag is enabled"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def exec = execution(scheduledExecution: null)

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.get('rundeck.executions').tag('project', 'p1').tag('status', 'succeeded').counter().count() == 1.0d
            meterRegistry.find('rundeck.executions').tag('job_id', '_').counter() == null
            meterRegistry.find('rundeck.executions').tag('job_name', '_').counter() == null
    }

    void "recordExecution excludes the job_id and job_name tags when the job dimension flag is disabled (default)"() {
        given:
            def job = new ScheduledExecution(uuid: 'job-uuid-1', jobName: 'my-job')
            def exec = execution(scheduledExecution: job)

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.get('rundeck.executions').tag('project', 'p1').tag('status', 'succeeded').counter().count() == 1.0d
            meterRegistry.find('rundeck.executions').tag('job_id', 'job-uuid-1').counter() == null
            meterRegistry.find('rundeck.executions').tag('job_name', 'my-job').counter() == null
    }

    void "recordStepNodeSeconds records a timer with project/status tags"() {
        given:
            def exec = execution([:])

        when:
            service.recordStepNodeSeconds(exec, 42L)

        then:
            meterRegistry.get('rundeck.execution.step_node_duration')
                .tag('project', 'p1').tag('status', 'succeeded')
                .timer().count() == 1L
    }

    void "recordStepNodeSeconds is a no-op when stepNodeSeconds is null"() {
        given:
            def exec = execution([:])
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordStepNodeSeconds(exec, null)

        then:
            meterRegistry.meters.size() == registrySize
    }

    void "recordStepNodeSeconds is a no-op when execution is null"() {
        given:
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordStepNodeSeconds(null, 42L)

        then:
            meterRegistry.meters.size() == registrySize
    }

    void "recordStepNodeSeconds adds job_id and job_name tags when the job dimension flag is enabled and the execution is scheduled"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def job = new ScheduledExecution(uuid: 'job-uuid-1', jobName: 'my-job', groupPath: null)
            def exec = execution(scheduledExecution: job)

        when:
            service.recordStepNodeSeconds(exec, 42L)

        then:
            meterRegistry.get('rundeck.execution.step_node_duration')
                .tag('project', 'p1').tag('status', 'succeeded').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .timer().count() == 1L
    }

    void "recordStepNodeCount records a distribution summary with project/status tags"() {
        given:
            def exec = execution([:])

        when:
            service.recordStepNodeCount(exec, 7L)

        then:
            meterRegistry.get('rundeck.execution.step_node_count')
                .tag('project', 'p1').tag('status', 'succeeded')
                .summary().totalAmount() == 7.0d
    }

    void "recordStepNodeCount is a no-op when stepNodeCount is null"() {
        given:
            def exec = execution([:])
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordStepNodeCount(exec, null)

        then:
            meterRegistry.meters.size() == registrySize
    }

    void "recordStepNodeCount is a no-op when execution is null"() {
        given:
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordStepNodeCount(null, 7L)

        then:
            meterRegistry.meters.size() == registrySize
    }

    void "recordStepNodeCount adds job_id and job_name tags when the job dimension flag is enabled and the execution is scheduled"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def job = new ScheduledExecution(uuid: 'job-uuid-1', jobName: 'my-job', groupPath: null)
            def exec = execution(scheduledExecution: job)

        when:
            service.recordStepNodeCount(exec, 7L)

        then:
            meterRegistry.get('rundeck.execution.step_node_count')
                .tag('project', 'p1').tag('status', 'succeeded').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .summary().totalAmount() == 7.0d
    }

    void "recordExecutionStart increments the running gauge, recordExecution decrements it back to zero"() {
        given:
            def exec = execution([:])

        when:
            service.recordExecutionStart(exec)

        then:
            meterRegistry.get('rundeck.executions.running').tag('project', 'p1').gauge().value() == 1.0d

        when:
            service.recordExecution(exec)

        then:
            meterRegistry.get('rundeck.executions.running').tag('project', 'p1').gauge().value() == 0.0d
    }

    void "recordExecutionStart tracks job_id/job_name on the running gauge when the job dimension flag is enabled"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def job = new ScheduledExecution(uuid: 'job-uuid-1', jobName: 'my-job')
            def exec = execution(scheduledExecution: job)

        when:
            service.recordExecutionStart(exec)

        then:
            meterRegistry.get('rundeck.executions.running')
                .tag('project', 'p1').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .gauge().value() == 1.0d
    }

    void "recordExecutionStart is a no-op when execution is null"() {
        given:
            def registrySize = meterRegistry.meters.size()

        when:
            service.recordExecutionStart(null)

        then:
            meterRegistry.meters.size() == registrySize
    }

    void "onJobChanged removes job_id-tagged meters, including the running gauge, when a job is deleted"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def deletedJob = new ScheduledExecution(uuid: 'job-uuid-1')
            def keptJob = new ScheduledExecution(uuid: 'job-uuid-2')
            service.recordExecutionStart(execution(scheduledExecution: deletedJob))
            service.recordExecution(execution(scheduledExecution: deletedJob))
            service.recordStepNodeSeconds(execution(scheduledExecution: deletedJob), 42L)
            service.recordStepNodeCount(execution(scheduledExecution: deletedJob), 7L)
            service.recordExecution(execution(scheduledExecution: keptJob))
            def event = Stub(JobChangeEvent) {
                getEventType() >> JobChangeEvent.JobChangeEventType.DELETE
                getJobReference() >> Stub(JobRevReference) {
                    getId() >> 'job-uuid-1'
                }
            }

        when:
            service.onJobChanged(event)

        then:
            meterRegistry.find('rundeck.executions').tag('job_id', 'job-uuid-1').counter() == null
            meterRegistry.find('rundeck.execution.duration').tag('job_id', 'job-uuid-1').timer() == null
            meterRegistry.find('rundeck.execution.step_node_duration').tag('job_id', 'job-uuid-1').timer() == null
            meterRegistry.find('rundeck.execution.step_node_count').tag('job_id', 'job-uuid-1').summary() == null
            meterRegistry.find('rundeck.executions.running').tag('job_id', 'job-uuid-1').gauge() == null
            meterRegistry.get('rundeck.executions').tag('job_id', 'job-uuid-2').counter().count() == 1.0d
    }

    void "onJobChanged is a no-op for non-DELETE job change events"() {
        given:
            configurationService.getBoolean(MicrometerExecutionMetricsService.JOB_DIMENSION_ENABLED_PROPERTY, false) >> true
            def job = new ScheduledExecution(uuid: 'job-uuid-1')
            service.recordExecution(execution(scheduledExecution: job))
            def event = Stub(JobChangeEvent) {
                getEventType() >> JobChangeEvent.JobChangeEventType.MODIFY
            }

        when:
            service.onJobChanged(event)

        then:
            meterRegistry.get('rundeck.executions').tag('job_id', 'job-uuid-1').counter().count() == 1.0d
    }

    void "initialize registers rundeck.system.info and rundeck.execution.mode.active gauges"() {
        given:
            // GrailsApplication.getMetadata() defaults to null on a bare Mock -- exercises the
            // null-safe fallback to 'unknown' tags, since grails.util.Metadata has no public
            // constructor usable from a unit test.
            service.grailsApplication = Mock(grails.core.GrailsApplication)
            frameworkService.getServerUUID() >> 'uuid-1'
            executionService.getExecutionsAreActive() >> true

        when:
            service.initialize()

        then:
            def infoGauge = meterRegistry.get('rundeck.system.info')
                .tag('version', 'unknown')
                .tag('build', 'unknown')
                .tag('node_uuid', 'uuid-1')
                .gauge()
            infoGauge.value() == 1.0d

            meterRegistry.get('rundeck.execution.mode.active').gauge().value() == 1.0d
    }

    void "recordStepUsage buckets node counts into the fixed node_bucket set"() {
        given:
            def exec = execution([:])
            def breakdown = ['1': new StepNodeUsageEntry(5_000L, 'exec-command', true, nodeCount)]

        when:
            service.recordStepUsage(exec, breakdown)

        then:
            meterRegistry.get('rundeck.execution.step.nodes')
                .tag('plugin_type', 'exec-command')
                .tag('node_bucket', bucket)
                .summary().count() == 1L

        where:
            nodeCount || bucket
            0L        || '0'
            1L        || '1'
            2L        || '2-10'
            10L       || '2-10'
            11L       || '11-100'
            100L      || '11-100'
            101L      || '100+'
            5000L     || '100+'
    }

    void "recordStepUsage maps a null pluginType and null isNodeStep to unknown"() {
        given:
            def exec = execution([:])
            def breakdown = ['1': new StepNodeUsageEntry(1_000L, null, null, 3L)]

        when:
            service.recordStepUsage(exec, breakdown)

        then:
            meterRegistry.get('rundeck.execution.step.nodes')
                .tag('plugin_type', 'unknown')
                .tag('node_step', 'unknown')
                .tag('node_bucket', '2-10')
                .summary().count() == 1L
    }

    void "recordStepUsage tags node_step from isNodeStep"() {
        given:
            def exec = execution([:])
            def breakdown = ['1': new StepNodeUsageEntry(1_000L, 'script', isNodeStep, 1L)]

        when:
            service.recordStepUsage(exec, breakdown)

        then:
            meterRegistry.get('rundeck.execution.step.nodes')
                .tag('node_step', expected)
                .summary().count() == 1L

        where:
            isNodeStep || expected
            true       || 'true'
            false      || 'false'
    }

    void "recordStepUsage aggregates steps sharing a plugin type and bucket into one series"() {
        given:
            def exec = execution([:])
            def breakdown = [
                '1': new StepNodeUsageEntry(1_000L, 'exec-command', true, 4L),
                '2': new StepNodeUsageEntry(2_000L, 'exec-command', true, 6L),
                '3': new StepNodeUsageEntry(4_000L, 'script', true, 4L),
            ]

        when:
            service.recordStepUsage(exec, breakdown)

        then: "the two exec-command steps land on one series, summing their node counts"
            def summary = meterRegistry.get('rundeck.execution.step.nodes')
                .tag('plugin_type', 'exec-command')
                .tag('node_bucket', '2-10')
                .summary()
            summary.count() == 2L
            summary.totalAmount() == 10.0d

        and: "script keeps its own series"
            meterRegistry.get('rundeck.execution.step.nodes')
                .tag('plugin_type', 'script')
                .summary().count() == 1L

        and: "durations accumulate at nanosecond precision rather than rounding each to zero"
            meterRegistry.get('rundeck.execution.step.duration')
                .tag('plugin_type', 'exec-command')
                .timer().totalTime(TimeUnit.NANOSECONDS) == 3_000.0d
    }

    void "recordStepUsage carries no project or job_id dimension"() {
        given:
            def exec = execution(project: 'p1')
            def breakdown = ['1': new StepNodeUsageEntry(1_000L, 'script', true, 1L)]

        when:
            service.recordStepUsage(exec, breakdown)

        then: "the cardinality argument for these meters is that project is absent"
            meterRegistry.find('rundeck.execution.step.nodes').tag('project', 'p1').summary() == null
            meterRegistry.find('rundeck.execution.step.duration').tag('project', 'p1').timer() == null
            meterRegistry.get('rundeck.execution.step.nodes').summary().count() == 1L
    }

    void "recordStepUsage is a no-op for a null or empty breakdown and a null execution"() {
        when:
            service.recordStepUsage(execution([:]), null)
            service.recordStepUsage(execution([:]), [:])
            service.recordStepUsage(null, ['1': new StepNodeUsageEntry(1L, 'script', true, 1L)])

        then:
            noExceptionThrown()
            meterRegistry.find('rundeck.execution.step.nodes').summary() == null
    }

    void "recordStepUsage leaves the pre-existing summed step_node meters untouched"() {
        given:
            def exec = execution([:])
            def breakdown = [
                '1': new StepNodeUsageEntry(1_000L, 'exec-command', true, 4L),
                '2': new StepNodeUsageEntry(2_000L, 'script', true, 6L),
            ]

        when: "both the old summed path and the new per-step path run, as ExecutionService does"
            service.recordStepNodeSeconds(exec, 7L)
            service.recordStepNodeCount(exec, 10L)
            service.recordStepUsage(exec, breakdown)

        then: "the summed meters keep their project tag, their names and a single sample each"
            meterRegistry.get('rundeck.execution.step_node_count')
                .tag('project', 'p1').tag('status', 'succeeded')
                .summary().with { it.count() == 1L && it.totalAmount() == 10.0d }
            meterRegistry.get('rundeck.execution.step_node_duration')
                .tag('project', 'p1').tag('status', 'succeeded')
                .timer().count() == 1L

        and: "and the new per-step meters are separate series"
            meterRegistry.get('rundeck.execution.step.nodes').summary() != null
    }

    /** Records one step at the given node count and returns the node_bucket tag it landed on. */
    private String bucketFor(long nodeCount) {
        service.recordStepUsage(execution([:]), ['1': new StepNodeUsageEntry(1_000L, 'script', true, nodeCount)])
        meterRegistry.get('rundeck.execution.step.nodes').summary().getId().getTag('node_bucket')
    }

    void "node_bucket labels are derived from the configured bounds"() {
        given:
            configurationService.getString(MicrometerExecutionMetricsService.NODE_BUCKETS_PROPERTY, _) >> config

        expect:
            bucketFor(nodeCount) == bucket

        where:
            config     | nodeCount || bucket
            // default, reproducing the previously hardcoded bands exactly
            '1,10,100' | 1L        || '1'
            '1,10,100' | 2L        || '2-10'
            '1,10,100' | 10L       || '2-10'
            '1,10,100' | 11L       || '11-100'
            '1,10,100' | 100L      || '11-100'
            '1,10,100' | 101L      || '100+'
            // a first bound above 1 opens with a range rather than a bare '1'
            '10,100'   | 1L        || '1-10'
            '10,100'   | 10L       || '1-10'
            '10,100'   | 11L       || '11-100'
            '10,100'   | 500L      || '100+'
            // arbitrary edges
            '1,5,10'   | 1L        || '1'
            '1,5,10'   | 5L        || '2-5'
            '1,5,10'   | 6L        || '6-10'
            '1,5,10'   | 11L       || '10+'
            // a single bound degenerates to two bands
            '50'       | 50L       || '1-50'
            '50'       | 51L       || '50+'
    }

    void "an unconfigured property silently uses the default bands"() {
        given: "configurationService returns null, as it does when nothing is set"
            configurationService.getString(_, _) >> null

        expect:
            bucketFor(5L) == '2-10'
    }

    void "unusable node_bucket configuration falls back to the default without throwing"() {
        given:
            configurationService.getString(MicrometerExecutionMetricsService.NODE_BUCKETS_PROPERTY, _) >> config

        when: "a count that only the default bands would label '2-10'"
            def bucket = bucketFor(5L)

        then:
            noExceptionThrown()
            bucket == '2-10'

        where:
            config << [
                '1,abc,10',   // not a number
                '100,10,1',   // descending
                '1,10,10',    // not strictly ascending
                '1,-5,10',    // below 1
                '0,10',       // below 1
                '',           // explicitly blank
                '   ',        // whitespace only
                '1,,10',      // empty element
                'nonsense',
            ]
    }

    void "more bands than the maximum are rejected in favour of the default"() {
        given: "21 ascending bounds, one over the cap"
            configurationService.getString(MicrometerExecutionMetricsService.NODE_BUCKETS_PROPERTY, _) >> (1..21).join(',')

        expect:
            bucketFor(5L) == '2-10'
    }

    void "exactly the maximum number of bands is accepted"() {
        given:
            configurationService.getString(MicrometerExecutionMetricsService.NODE_BUCKETS_PROPERTY, _) >> (1..20).join(',')

        expect: "the open-ended final band comes from the 20th bound, not the default"
            bucketFor(25L) == '20+'
    }

    void "the resolved bands are parsed once and cached across calls"() {
        given:
            def exec = execution([:])

        when: "three steps are recorded"
            3.times {
                service.recordStepUsage(exec, ['1': new StepNodeUsageEntry(1_000L, 'script', true, 5L)])
            }

        then: "the property is read once, not once per step"
            1 * configurationService.getString(MicrometerExecutionMetricsService.NODE_BUCKETS_PROPERTY, _) >> '1,10,100'

        and:
            meterRegistry.get('rundeck.execution.step.nodes').tag('node_bucket', '2-10').summary().count() == 3L
    }
}
