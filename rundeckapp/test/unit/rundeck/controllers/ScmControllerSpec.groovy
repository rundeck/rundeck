package rundeck.controllers

import grails.test.mixin.TestFor
import rundeck.services.FrameworkService
import rundeck.services.ScmService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ScmController)
class ScmControllerSpec extends Specification {


    void "scm action cancel redirects to jobs page"() {
        given:
        controller.frameworkService = Mock(FrameworkService)
        controller.scmService = Mock(ScmService)

        when:
        request.method = 'POST'
        params.cancel = 'Cancel'
        controller.performActionSubmit('export', 'test1', 'asdf')

        then:
        1 * controller.frameworkService.authorizeApplicationResourceAny(*_) >> true
        1 * controller.scmService.projectHasConfiguredPlugin(*_) >> true

        response.status == 302
        response.redirectedUrl == '/scheduledExecution/index?project=test1'
    }
}
