package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.server.plugins.tasks.action.JobRunTaskAction
import grails.test.mixin.TestFor
import org.rundeck.core.tasks.TaskManager
import org.rundeck.core.tasks.TaskTrigger
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JobRunTaskActionService)
class JobRunTaskActionServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "perform task action"() {
        given:
        def action = new JobRunTaskAction()
        action.jobId = jobId
        action.optionData = optionMap
        action.extraData = [
            filter: jobFilter,
            asUser: asUser
        ]


        def trigger = Mock(TaskTrigger)
        def map = [:]
        def userData = [:]
        def context = new RDTaskContext()
        context.authContext = Mock(UserAndRolesAuthContext)
        context.project = project

        def id = '123'
        service.jobStateService = Mock(JobStateService)

        def jobRef = Mock(JobReference)
        def execref = Mock(ExecutionReference) {
            getId() >> '999'
        }
        when:
        def result = service.performTaskAction(context, map, userData, [:], trigger, action, Mock(TaskManager))
        then:
        1 * service.jobStateService.jobForID(context.authContext, jobId, project) >> jobRef
        1 * service.jobStateService.startJob(context.authContext, jobRef, optionMap, jobFilter, asUser) >> execref
        0 * service.jobStateService._(*_)

        result.execId == '999'

        where:
        jobId | project    | optionMap | jobFilter | asUser
        'xyz' | 'testproj' | [a: 'b']  | 'asdf'    | 'auser'
        'xyz' | 'testproj' | [a: 'b']  | null      | 'auser'
        'xyz' | 'testproj' | [a: 'b']  | 'asdf'    | null
        'xyz' | 'testproj' | [a: 'b']  | null      | null
    }
}
