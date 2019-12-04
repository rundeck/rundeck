package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.AuthToken
import rundeck.User
import spock.lang.Specification

class RundeckAuthTokenManagerServiceSpec extends Specification
        implements ServiceUnitTest<RundeckAuthTokenManagerService>, DataTest {


    def setup() {
        mockDomain(AuthToken)
        mockDomain(User)
    }

    def "importWebhookToken"() {
        given:
            service.apiService = Mock(ApiService)
            def auth = Mock(UserAndRolesAuthContext)
            def token = '123'
            def user = 'auser'
            def roles = new HashSet(['a', 'b', 'c'])
        when:
            service.importWebhookToken(auth, token, user, roles)
        then:
            1 * service.apiService.createUserToken(auth, 0, token, user, roles, false, true) >> true
    }

    def cleanup() {
    }

}
