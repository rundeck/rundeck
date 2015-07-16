package rundeck.services

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.controllers.ScheduledExecutionController
import spock.lang.Specification

/**
 * Created by greg on 6/24/15.
 */
@TestFor(ScheduledExecutionService)
@Mock([Workflow, ScheduledExecution, CommandExec, Notification])
class ScheduledExecutionServiceSpec extends Specification {
    def "blank email notification"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(_) >> true
        }

        when:
        def params = [jobName       : 'monkey1',
                      project       : 'testProject',
                      description   : 'blah',
                      adhocExecution: false,

                      workflow      : new Workflow(
                              threadcount: 1,
                              keepgoing: true,
                              commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                      ),
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, 'test', 'test', null)
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS)

    }
    def "blank webhook notification"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject(_) >> true
        }

        when:
        def params = [jobName       : 'monkey1',
                      project       : 'testProject',
                      description   : 'blah',
                      adhocExecution: false,

                      workflow      : new Workflow(
                              threadcount: 1,
                              keepgoing: true,
                              commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                      ),
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, 'test', 'test', null)
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_URL)

    }
}
