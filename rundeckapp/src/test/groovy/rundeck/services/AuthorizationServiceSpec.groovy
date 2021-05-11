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
import com.dtolabs.rundeck.core.authorization.*
import grails.events.bus.EventBus
import grails.testing.services.ServiceUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.rundeck.app.acl.AclPolicyFile
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import spock.lang.Specification

class AuthorizationServiceSpec extends Specification implements ServiceUnitTest<AuthorizationService>{

    def setup() {
        def configService = Stub(ConfigurationService) {
            getString('authorizationService.sourceCache.spec', _) >> 'refreshAfterWrite=2m'
        }
        defineBeans {
            configurationService(InstanceFactoryBean, configService)
        }
    }


    void "system authorization modern"() {
        given:
        service.aclStorageFileManager = Mock(ContextACLManager) {
            1 * listStoredPolicyFiles(AppACLContext.system()) >> []
        }
        service.rundeckFilesystemPolicyAuthorization = RuleEvaluator.createRuleEvaluator(new AclRuleSetImpl(new HashSet<AclRule>()),{})
        when:
        def auth = service.getAuthorizationForSubject(null)

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
            service.aclStorageFileManager = Mock(ContextACLManager) {
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
        def auth = service.getAuthorizationForSubject(null)

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
            service.aclStorageFileManager = Mock(ContextACLManager) {
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

    def "loadStoredPolicies file dne"() {
        given: "paths list incorrectly includes a file that does not exist"
            def ctxt = AppACLContext.system()
            service.aclStorageFileManager = Mock(ContextACLManager) {
                _ * listStoredPolicyFiles(ctxt) >> ["file1.aclpolicy"]
                _ * existsPolicyFile(ctxt, 'file1.aclpolicy') >> false
            }
        when: "load policies"
            def auth = service.loadStoredPolicies()
        then: "no exception"
            auth != null
            def rules = auth.getRuleSet().rules
            rules.size() == 0
    }

    def "loadStoredPolicies project file dne"() {
        given: "paths list incorrectly includes a file that does not exist"
            def ctxt = AppACLContext.project('proj1')
            service.aclStorageFileManager = Mock(ContextACLManager) {
                _ * listStoredPolicyFiles(ctxt) >> ["file1.aclpolicy"]
                _ * existsPolicyFile(ctxt, 'file1.aclpolicy') >> false
            }
        when: "load policies"
            def auth = service.loadStoredPolicies('proj1')
        then: "no exception"
            auth != null
            def rules = auth.getRuleSet().rules
            rules.size() == 0
    }

    def "clean caches sends acl.modified event"() {
        given:
            def key = AuthorizationService.SourceKey.forContext(context, path)
            service.targetEventBus = Mock(EventBus)
        when:
            service.cleanCaches(key)
        then:
            1 * service.eventBus.notify(
                'acl.modified', {
                it[0].context == context
                it[0].path == path
            }
            )

        where:
            context                        | path
            AppACLContext.system()         | "a/path"
            AppACLContext.project('aproj') | "another/path"
    }
}
