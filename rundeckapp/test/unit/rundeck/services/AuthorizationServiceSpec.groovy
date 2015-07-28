package rundeck.services

import com.dtolabs.rundeck.core.authorization.AclRule
import com.dtolabs.rundeck.core.authorization.AclRuleSet
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.MultiAuthorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(AuthorizationService)
class AuthorizationServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "system authorization legacy"() {
        given:
        service.configStorageService = Mock(StorageManager) {
            1 * listDirPaths('acls/', ".*\\.aclpolicy") >> []
        }
        when:
        def auth = service.systemAuthorization

        then:
        auth != null
        auth instanceof MultiAuthorization
    }

    void "system authorization modern"() {
        given:
        service.configStorageService = Mock(StorageManager) {
            1 * listDirPaths('acls/', ".*\\.aclpolicy") >> []
        }
        service.rundeckFilesystemPolicyAuthorization = RuleEvaluator.createRuleEvaluator(new AclRuleSetImpl(new HashSet<AclRule>()))
        when:
        def auth = service.systemAuthorization

        then:
        auth != null
        auth instanceof RuleEvaluator
    }
}
