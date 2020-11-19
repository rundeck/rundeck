package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder
import com.dtolabs.rundeck.core.authorization.AclRuleSetAuthorization
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import spock.lang.Specification

import javax.security.auth.Subject

class AuthContextProviderServiceSpec extends Specification {

    def "getAuthContextForSubjectAndProject merges system/project auth rules"() {
        given:
            def service = new AuthContextProviderService()
            def subject = new Subject()
            subject.principals.add(new Username('auser'))
            subject.principals.add(new Group('agroup'))
            subject.principals.add(new Group('bgroup'))

            def project = 'AProject'
            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())
            def rules2 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('2').build()].toSet())


            service.authorizationService = Mock(AuthorizationService) {
                getSystemAuthorization() >> Mock(AclRuleSetAuthorization) {
                    getRuleSet() >> rules1
                }
                loadStoredProjectAuthorization(project)>> Mock(AclRuleSetAuthorization) {
                    getRuleSet() >> rules2
                }
            }
            service.frameworkService=Mock(FrameworkService){
                existsFrameworkProject(project)>>true
            }

        when:
            def result = service.getAuthContextForSubjectAndProject(subject, project)
        then:
            result
            result.username == 'auser'
            result.roles.containsAll(['agroup', 'bgroup'])
            result.authorization.ruleSet.rules.containsAll(rules1.rules)
            result.authorization.ruleSet.rules.containsAll(rules2.rules)

    }
}
