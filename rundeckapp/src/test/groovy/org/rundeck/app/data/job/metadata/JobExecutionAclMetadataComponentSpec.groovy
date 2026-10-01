package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.core.auth.AuthConstants
import rundeck.ScheduledExecution
import rundeck.services.data.IScheduledExecutionDataService
import spock.lang.Specification
import spock.lang.Unroll

class JobExecutionAclMetadataComponentSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains ScheduledExecution
    }

    def "getAvailableMetadataNames includes executionAclValid"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
        expect:
            sut.getAvailableMetadataNames() == ['executionAclValid'].toSet()
    }

    @Unroll
    def "validateExecutionAcl returns true without evaluating authorization when #reason"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            def se = new ScheduledExecution(
                project: 'AProject',
                user: 'someuser',
                scheduled: scheduled,
                executionEnabled: executionEnabled,
                scheduleEnabled: scheduleEnabled,
            )
        when:
            def result = sut.validateExecutionAcl(se)
        then:
            result
            0 * sut.rundeckAuthContextProcessor._
        where:
            scheduled | executionEnabled | scheduleEnabled | reason
            false     | true              | true            | 'job is not scheduled'
            true      | false             | true             | 'execution is disabled'
            true      | true              | false            | 'schedule is disabled'
    }

    def "validateExecutionAcl returns true when the stored owner is authorized to run the job"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = new ScheduledExecution(
                project: 'AProject',
                user: 'someuser',
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
            )
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> true
            }
        when:
            def result = sut.validateExecutionAcl(se)
        then:
            result
    }

    def "validateExecutionAcl returns false when the stored owner is not authorized to run the job"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = new ScheduledExecution(
                project: 'AProject',
                user: 'someuser',
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
            )
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> false
            }
        when:
            def result = sut.validateExecutionAcl(se)
        then:
            !result
    }

    def "bulk validateExecutionAcl builds one auth context per owner, not per job"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def sharedOwnerA = new ScheduledExecution(
                uuid: 'job-a', project: 'AProject', user: 'someuser',
                scheduled: true, executionEnabled: true, scheduleEnabled: true,
            )
            def sharedOwnerB = new ScheduledExecution(
                uuid: 'job-b', project: 'AProject', user: 'someuser',
                scheduled: true, executionEnabled: true, scheduleEnabled: true,
            )
            def notScheduled = new ScheduledExecution(
                uuid: 'job-c', project: 'AProject', user: 'someuser', scheduled: false,
            )
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                // two jobs, same owner and project: the context is built once
                1 * getAuthContextForUserAndRolesAndProject('someuser', _, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, sharedOwnerA, [AuthConstants.ACTION_RUN], 'AProject') >> false
                1 * authorizeProjectJobAll(authContext, sharedOwnerB, [AuthConstants.ACTION_RUN], 'AProject') >> true
            }
        when:
            def result = sut.validateExecutionAcl([sharedOwnerA, sharedOwnerB, notScheduled])
        then:
            result == ['job-a': false, 'job-b': true, 'job-c': true]
    }

    def "getMetadataForJobIds returns executionAclValid meta for a requested at-risk job"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = new ScheduledExecution(
                project: 'AProject',
                user: 'someuser',
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
            )
            sut.scheduledExecutionDataService = Mock(IScheduledExecutionDataService) {
                1 * findByUuid('job-1') >> se
            }
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> false
            }
        when:
            def result = sut.getMetadataForJobIds(['job-1'], 'AProject', ['*'].toSet(), authContext)
        then:
            result.size() == 1
            result['job-1'].size() == 1
            result['job-1'][0].name == 'executionAclValid'
            result['job-1'][0].data == [valid: false, user: 'someuser']
    }

    def "getMetadataForJobIds returns empty when executionAclValid was not requested"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            sut.scheduledExecutionDataService = Mock(IScheduledExecutionDataService)
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        when:
            def result = sut.getMetadataForJobIds(['job-1'], 'AProject', ['tags'].toSet(), Mock(UserAndRolesAuthContext))
        then:
            result.isEmpty()
            0 * sut.scheduledExecutionDataService._
            0 * sut.rundeckAuthContextProcessor._
    }

    def "getMetadataForJob for a JobDataSummary returns the meta (path used by the job meta endpoint)"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = atRiskJob()
            def summary = Mock(JobDataSummary) {
                _ * getUuid() >> 'job-1'
                _ * getProject() >> 'AProject'
            }
            sut.scheduledExecutionDataService = Mock(IScheduledExecutionDataService) {
                1 * findByUuid('job-1') >> se
            }
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> false
            }
        when:
            def result = sut.getMetadataForJob(summary, ['*'].toSet(), authContext)
        then:
            result.isPresent()
            result.get()[0].name == 'executionAclValid'
            result.get()[0].data == [valid: false, user: 'someuser']
    }

    def "getMetadataForJobs returns meta keyed by job id (path used by the jobs browse endpoint)"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = atRiskJob()
            def summary = Mock(JobDataSummary) {
                _ * getUuid() >> 'job-1'
                _ * getProject() >> 'AProject'
            }
            sut.scheduledExecutionDataService = Mock(IScheduledExecutionDataService) {
                1 * findByUuid('job-1') >> se
            }
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> false
            }
        when:
            def result = sut.getMetadataForJobs([summary], ['*'].toSet(), authContext)
        then:
            result['job-1'][0].name == 'executionAclValid'
            result['job-1'][0].data == [valid: false, user: 'someuser']
    }

    def "getMetadataForJobs returns empty for an empty job collection"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
        when:
            def result = sut.getMetadataForJobs([], ['*'].toSet(), Mock(UserAndRolesAuthContext))
        then:
            result.isEmpty()
    }

    private static ScheduledExecution atRiskJob() {
        new ScheduledExecution(
            project: 'AProject',
            user: 'someuser',
            scheduled: true,
            executionEnabled: true,
            scheduleEnabled: true,
        )
    }

    def "getMetadataForJob delegates to getMetadataForJobIds for a single id"() {
        given:
            def sut = new JobExecutionAclMetadataComponent()
            def authContext = Mock(UserAndRolesAuthContext)
            def se = new ScheduledExecution(
                project: 'AProject',
                user: 'someuser',
                scheduled: true,
                executionEnabled: true,
                scheduleEnabled: true,
            )
            sut.scheduledExecutionDataService = Mock(IScheduledExecutionDataService) {
                1 * findByUuid('job-1') >> se
            }
            sut.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForUserAndRolesAndProject('someuser', se.userRoles, 'AProject') >> authContext
                1 * authorizeProjectJobAll(authContext, se, [AuthConstants.ACTION_RUN], 'AProject') >> true
            }
        when:
            def result = sut.getMetadataForJob('job-1', 'AProject', ['*'].toSet(), authContext)
        then:
            result.isPresent()
            result.get()[0].name == 'executionAclValid'
            result.get()[0].data == [valid: true, user: 'someuser']
    }
}
