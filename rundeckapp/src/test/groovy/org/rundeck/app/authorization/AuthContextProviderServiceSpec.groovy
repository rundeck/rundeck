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

    def "getAuthContextForSubjectAndProject calls authorizationService"() {
        given:
            def service = new AuthContextProviderService()
            def subject = new Subject()
            subject.principals.add(new Username('auser'))
            subject.principals.add(new Group('agroup'))
            subject.principals.add(new Group('bgroup'))

            def project = 'AProject'
            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())


            service.authorizationService = Mock(AuthorizationService) {
                1 * getProjectAuthorizationForSubject(
                    {
                        it.username == 'auser'
                        it.roles == ['agroup', 'bgroup'].toSet()
                    }, project
                ) >> Mock(AclRuleSetAuthorization) {
                    getRuleSet() >> rules1
                }
                0 * _(*_)
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
            result.authorization.ruleSet==rules1

    }
}
