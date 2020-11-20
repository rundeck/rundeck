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
import com.dtolabs.rundeck.core.storage.ResourceMeta
import grails.testing.services.ServiceUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.acl.ACLFileManager
import org.rundeck.app.acl.AclPolicyFile
import org.rundeck.app.acl.AppACLContext
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class AuthorizationServiceSpec extends Specification implements ServiceUnitTest<AuthorizationService>{

    void "system authorization legacy"() {
        given:
        service.aclFileManagerService = Mock(AclFileManagerService) {
            1 * listStoredPolicyFiles(AppACLContext.system()) >> []
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
        service.aclFileManagerService = Mock(AclFileManagerService) {
            1 * listStoredPolicyFiles(AppACLContext.system()) >> []
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
            service.aclFileManagerService = Mock(AclFileManagerService) {
                1 * listStoredPolicyFiles(AppACLContext.system()) >> ['test.aclpolicy']
                1 * existsPolicyFile(AppACLContext.system(),'test.aclpolicy')>>true
                1 * getAclPolicy(AppACLContext.system(),'test.aclpolicy')>>Mock(AclPolicyFile){
                    1 * getModified() >> new Date()
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

        when:
        def auth = service.systemAuthorization

        then:
            auth != null
            auth instanceof TimedAuthorization
            auth instanceof AclRuleSetSource
            auth.ruleSet.rules.size()==2
            auth.ruleSet.rules.containsAll(rules1)
    }


    void "create project authorization"(){
        given:
            def paths=[
                "file1.aclpolicy"
            ]
            def ctxt = AppACLContext.project(project)
            service.aclFileManagerService = Mock(AclFileManagerService) {
                _ * listStoredPolicyFiles(ctxt) >> paths
                _ * existsPolicyFile(ctxt,'file1.aclpolicy') >> true
                _ * getAclPolicy(ctxt,'file1.aclpolicy') >> Mock(AclPolicyFile) {
                    getInputStream() >> new ByteArrayInputStream(
                        (
                            '{ description: \'\', \n' +
                            'by: { username: \'test\' }, \n' +
                            'for: { resource: [ { equals: { kind: \'zambo\' }, allow: \'x\' } ] } }'
                        ).bytes
                    )
                    getModified() >> new Date()
                }
            }


        when:
            def auth=service.loadStoredProjectAuthorization(project)

        then:
            auth!=null
            auth instanceof LoggingAuthorization
            auth.authorization instanceof RuleEvaluator
            def rules=((RuleEvaluator)auth.authorization).getRuleSet().rules
            rules.size()==1
            def rulea=rules.first()
            rulea.allowActions==['x'] as Set
            rulea.description==''
            !rulea.containsMatch
            rulea.equalsMatch
            !rulea.regexMatch
            !rulea.subsetMatch
            rulea.resourceType=='resource'
            rulea.regexResource==null
            rulea.containsResource==null
            rulea.subsetResource==null
            rulea.equalsResource==[kind:'zambo']
            rulea.username=='test'
            rulea.group==null
            rulea.environment!=null
            rulea.environment.key=='project'
            rulea.environment.value=='test1'
            rulea.sourceIdentity=='[project:test1]file1.aclpolicy[1][type:resource][rule: 1]'
        where:
            project='test1'
    }
}
