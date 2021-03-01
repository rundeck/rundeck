package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder
import com.dtolabs.rundeck.core.authorization.AclRuleSetAuthorization
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import spock.lang.Specification

import javax.security.auth.Subject

class BaseAuthContextProviderSpec extends Specification {

    def "getAuthContextForSubject calls authorizationService"() {
        given:
            def service = new BaseAuthContextProvider()
            Subject subject = mkSubject()

            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())


            service.authorizationService = Mock(AuthorizationService) {
                1 * getAuthorizationForSubject(
                    {
                        it.username == 'auser'
                        it.roles == ['agroup', 'bgroup'].toSet()
                    }
                ) >> Mock(AclRuleSetAuthorization) {
                    getRuleSet() >> rules1
                }
                0 * _(*_)
            }

        when:
            def result = service.getAuthContextForSubject(subject)
        then:
            result
            result.username == 'auser'
            result.roles.containsAll(['agroup', 'bgroup'])
            result.authorization.ruleSet == rules1

    }

    public Subject mkSubject() {
        def subject = new Subject()
        subject.principals.add(new Username('auser'))
        subject.principals.add(new Group('agroup'))
        subject.principals.add(new Group('bgroup'))
        subject
    }

    def "getAuthContextForSubjectAndProject calls authorizationService"() {
        given:
            def service = new BaseAuthContextProvider()
            Subject subject = mkSubject()

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

        when:
            def result = service.getAuthContextForSubjectAndProject(subject, project)
        then:
            result
            result.username == 'auser'
            result.roles.containsAll(['agroup', 'bgroup'])
            result.authorization.ruleSet == rules1

    }

    def "getAuthContextWithProject calls authorizationService"() {
        given:
            def service = new BaseAuthContextProvider()
            def orig = Mock(UserAndRolesAuthContext)
            def newContext = Mock(UserAndRolesAuthContext)

            def project = 'AProject'
            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())

            def authz = Mock(AclRuleSetAuthorization) {
                getRuleSet() >> rules1
            }
            service.authorizationService = Mock(AuthorizationService) {
                1 * getProjectAuthorizationForSubject(
                    orig,
                    project
                ) >> authz
                0 * _(*_)
            }

        when:
            def result = service.getAuthContextWithProject(orig, project)
        then:
            result == newContext
            1 * orig.combineWith(authz) >> newContext

    }


    def "getAuthContextForUserAndRolesAndProject calls authorizationService"() {
        given:
            def service = new BaseAuthContextProvider()
            def user = 'auser'
            def roles = ['agroup', 'bgroup']

            def project = 'AProject'
            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())
            def rules2 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('2').build()].toSet())


            def authz1 = Mock(AclRuleSetAuthorization) {
                getRuleSet() >> rules1
            }
            def authz2 = Mock(AclRuleSetAuthorization) {
                getRuleSet() >> rules2
            }
            service.authorizationService = Mock(AuthorizationService) {
                1 * getAuthorizationForSubject(
                    {
                        it.username == user
                        it.roles == roles.toSet()
                    }
                ) >> authz1
                1 * getProjectAuthorizationForSubject(
                    {
                        it.username == user
                        it.roles == roles.toSet()
                    }, project
                )>> authz2
                0 * _(*_)

            }

        when:
            def result = service.getAuthContextForUserAndRolesAndProject(user, roles, project)
        then:
            result
            result.username == 'auser'
            result.roles.containsAll(['agroup', 'bgroup'])
            result.authorization.ruleSet.rules == rules1.rules+rules2.rules

    }

    def "getAuthContextForUserAndRoles calls authorizationService"() {
        given:
            def service = new BaseAuthContextProvider()
            def user = 'auser'
            def roles = ['arole', 'brole']

            def rules1 = new AclRuleSetImpl([AclRuleBuilder.builder().sourceIdentity('1').build()].toSet())

            def authz = Mock(AclRuleSetAuthorization) {
                getRuleSet() >> rules1
            }
            service.authorizationService = Mock(AuthorizationService) {
                1 * getAuthorizationForSubject(
                    {
                        it.username == user
                        it.roles == roles.toSet()
                    }
                ) >> authz
                0 * _(*_)
            }

        when:
            def result = service.getAuthContextForUserAndRoles(user, roles)
        then:
            result
            result.username == user
            result.roles.containsAll(roles)
            result.authorization.ruleSet == rules1

    }
}
