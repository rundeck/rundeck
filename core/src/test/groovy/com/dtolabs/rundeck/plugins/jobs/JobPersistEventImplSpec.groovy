package com.dtolabs.rundeck.plugins.jobs

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import spock.lang.Specification

/**
 * Tests for {@link JobPersistEventImpl}
 */
class JobPersistEventImplSpec extends Specification {

    def "copy constructor copies job execution settings"() {
        given:
        def origin = JobPersistEventImpl.builder()
                .jobName('job1')
                .projectName('proj1')
                .userName('admin')
                .nodeFilter('name: a')
                .timeout('2h30m')
                .retry('3')
                .nodeThreadcount('4')
                .multipleExecutions(true)
                .maxMultipleExecutions('5')
                .build()

        when:
        def copy = new JobPersistEventImpl(origin)

        then:
        copy == origin
        copy.timeout == '2h30m'
        copy.retry == '3'
        copy.nodeThreadcount == '4'
        copy.multipleExecutions
        copy.maxMultipleExecutions == '5'
    }

    def "legacy constructor leaves job execution settings unset"() {
        when:
        def event = new JobPersistEventImpl('job1', 'proj1', null, null, 'admin', 'name: a')

        then:
        event.jobName == 'job1'
        event.timeout == null
        event.retry == null
        event.nodeThreadcount == null
        event.multipleExecutions == null
        event.maxMultipleExecutions == null
    }

    def "interface defaults return null for implementations without job execution settings"() {
        given:
        JobPersistEvent event = new LegacyEvent()

        when:
        def copy = new JobPersistEventImpl(event)

        then:
        copy.jobName == 'job1'
        copy.timeout == null
        copy.retry == null
        copy.nodeThreadcount == null
        copy.multipleExecutions == null
        copy.maxMultipleExecutions == null
    }

    /**
     * Implementation written against the original interface, relying on the default methods.
     */
    static class LegacyEvent implements JobPersistEvent {
        String jobName = 'job1'
        String projectName = 'proj1'
        SortedSet<JobOption> options = null
        INodeSet nodes = null
        String userName = 'admin'
        String nodeFilter = null
    }
}
