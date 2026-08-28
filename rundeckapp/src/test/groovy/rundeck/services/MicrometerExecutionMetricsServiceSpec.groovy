package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import rundeck.Execution
import rundeck.ScheduledExecution
import spock.lang.Specification

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
            meterRegistry.get('rundeck.execution.step_node_seconds')
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
            meterRegistry.get('rundeck.execution.step_node_seconds')
                .tag('project', 'p1').tag('status', 'succeeded').tag('job_id', 'job-uuid-1').tag('job_name', 'my-job')
                .timer().count() == 1L
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
            meterRegistry.find('rundeck.execution.step_node_seconds').tag('job_id', 'job-uuid-1').timer() == null
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
}
