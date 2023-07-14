package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.providers.GormExecutionDataProvider
import org.springframework.context.MessageSource
import rundeck.data.execution.RdExecution
import rundeck.data.paging.RdPageable
import rundeck.services.RdExecutionService
import spock.lang.Specification

class RdExecutionControllerSpec extends Specification implements ControllerUnitTest<RdExecutionController>, DataTest {

    def setup() {
        controller.rdExecutionService = Mock(RdExecutionService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.messageSource = Mock(MessageSource)
    }

    def "test get method"() {
        when:
        controller.get()

        then:
        1 * controller.rdExecutionService.getExecutionByUuid(_) >> new RdExecution()
        response.contentType == "application/json;utf-8"
        response.status == 200
    }

    def "test delete method"() {
        given:
        def executionId = "123"
        params.id = executionId

        when:
        controller.delete()

        then:
        1 * controller.rdExecutionService.getExecutionByUuid(executionId) >> new RdExecution()
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceAny(_, _, _) >> true
        1 * controller.rdExecutionService.delete(executionId) >> new GormExecutionDataProvider.DeleteExecutionResult()
        response.contentType == "application/json;utf-8"
        response.status == 200
    }

    def "test listForJob method"() {
        given:
        def jobId = "job-uuid"
        def pageable = new RdPageable()
        pageable.max = 10
        pageable.offset = 1
        params.id = jobId
        params.max = pageable.max
        params.offset = pageable.offset

        when:
        controller.listForJob()

        then:
        1 * controller.rdExecutionService.listExecutionsForJob(jobId, pageable) >> []
        response.contentType == "application/json;utf-8"
        response.status == 200
    }
}
