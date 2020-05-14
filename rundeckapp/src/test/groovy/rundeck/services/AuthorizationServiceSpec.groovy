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

import com.codahale.metrics.Meter
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.AclRule
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl
import com.dtolabs.rundeck.core.authorization.AclRuleSetSource
import com.dtolabs.rundeck.core.authorization.LoggingAuthorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import grails.testing.services.ServiceUnitTest
import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class AuthorizationServiceSpec extends Specification implements ServiceUnitTest<AuthorizationService>{

    void "system authorization legacy"() {
        given:
        service.configStorageService = Mock(StorageManager) {
            1 * listDirPaths('acls/', ".*\\.aclpolicy") >> []
        }
        when:
        def auth = service.systemAuthorization

        then:
        auth != null
        auth instanceof LoggingAuthorization
        auth.authorization instanceof RuleEvaluator
    }

    void "system authorization modern"() {
        given:
        service.configStorageService = Mock(StorageManager) {
            1 * listDirPaths('acls/', ".*\\.aclpolicy") >> []
        }
        service.rundeckFilesystemPolicyAuthorization = RuleEvaluator.createRuleEvaluator(new AclRuleSetImpl(new HashSet<AclRule>()),{})
        when:
        def auth = service.systemAuthorization

        then:
        auth != null
        auth instanceof LoggingAuthorization
        auth.authorization instanceof RuleEvaluator
    }
    void "system authorization modern with timer"() {
        given:
            service.metricService=Mock(MetricService){
                1 * withTimer(_,_,_)>>{
                    it[2].call()
                }
                2 * meter(_,_)>>Mock(Meter)
                2 * timer(_,_)>>Mock(Timer)
            }
            def rules1 = [AclRuleBuilder.builder().sourceIdentity('1').build()].toSet()
            service.rundeckFilesystemPolicyAuthorization = RuleEvaluator.createRuleEvaluator(new AclRuleSetImpl(rules1), {})
            service.configStorageService = Mock(ConfigStorageService){
                1 * listDirPaths('acls/', ".*\\.aclpolicy") >> ['acls/test.aclpolicy']
                1 * existsFileResource('acls/test.aclpolicy')>>true
                1 * getFileResource('acls/test.aclpolicy')>>Mock(Resource){
                    1 * getContents()>> Mock(ResourceMeta){
                        getModificationTime() >> new Date()
                        1 * getInputStream() >> new ByteArrayInputStream('''
description: test
for:
  job:
    - allow: ['*']
by:
    group: ['test']
context:
    application: rundeck
'''.bytes)
                    }
                }
            }

        when:
        def auth = service.systemAuthorization

        then:
            auth != null
            auth instanceof TimedAuthorization
            auth instanceof AclRuleSetSource
            auth.ruleSet.rules.size()==2
            auth.ruleSet.rules.containsAll(rules1)
    }
}
