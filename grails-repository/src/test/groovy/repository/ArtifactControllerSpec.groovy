package repository

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.core.auth.AuthConstants
import spock.lang.Specification;


class ArtifactControllerSpec extends Specification implements ControllerUnitTest<ArtifactController> {

    AuthContextProcessor mockAuthContextProcessor
    AuthContext mockAuthContext

    def setup() {
        mockAuthContextProcessor = Mock(AuthContextProcessor)
        mockAuthContext = Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor = mockAuthContextProcessor
    }

    void "test index action with authorized user"() {
        given: "an authorized user"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN, _) >> true

        when: "index action is called"
        controller.index()

        then: "the index view is rendered"
        view == "/artifact/index"
        response.status == 200
    }

    void "test index action with unauthorized user"() {
        given: "an unauthorized user"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN, _) >> false

        when: "index action is called"
        controller.index()

        then: "unauthorized error is returned"
        response.status == 400
        response.json.error == "You are not authorized to perform this action"
    }

    void "test authorized method with PLUGIN READ permission"() {
        given: "user with PLUGIN READ permission"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN, _) >> true

        when: "authorized method is called"
        def result = controller.authorized(AuthConstants.RESOURCE_TYPE_PLUGIN, AuthConstants.ACTION_READ)

        then: "authorization succeeds"
        result == true
    }


    void "test authorized method fails for insufficient permissions"() {
        given: "user without sufficient permissions"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, _, _) >> false

        when: "authorized method is called"
        def result = controller.authorized(AuthConstants.RESOURCE_TYPE_PLUGIN, AuthConstants.ACTION_READ)

        then: "authorization fails"
        result == false
    }

    void "test authorized method builds correct action list for non-admin actions"() {
        given: "user with specific action permission"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, _, _) >> true

        when: "authorized method is called with READ action"
        controller.authorized(AuthConstants.RESOURCE_TYPE_PLUGIN, AuthConstants.ACTION_READ)

        then: "correct actions list is used"
        1 * mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN, AuthConstants.ACTION_READ])
    }

    void "test authorized method builds correct action list for admin actions"() {
        given: "user with admin permission"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, _, _) >> true

        when: "authorized method is called with ADMIN action"
        controller.authorized(AuthConstants.RESOURCE_TYPE_SYSTEM, AuthConstants.ACTION_ADMIN)

        then: "only admin actions are checked"
        1 * mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN])
    }

    void "test specifyUnauthorizedError sets correct response"() {
        when: "specifyUnauthorizedError is called"
        controller.specifyUnauthorizedError()

        then: "correct error response is set"
        response.status == 400
        response.json.error == "You are not authorized to perform this action"
    }

    void "test index action verifies correct permissions"() {
        given: "a user"
        mockAuthContextProcessor.getAuthContextForSubject(_) >> mockAuthContext
        mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN, _) >> true

        when: "index action is called"
        controller.index()

        then: "correct authorization check is performed"
        1 * mockAuthContextProcessor.authorizeApplicationResourceAny(mockAuthContext, AuthConstants.RESOURCE_TYPE_PLUGIN,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN, AuthConstants.ACTION_READ])
    }
}