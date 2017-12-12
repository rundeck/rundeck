/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
