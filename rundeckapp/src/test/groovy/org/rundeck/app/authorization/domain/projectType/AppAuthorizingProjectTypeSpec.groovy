package org.rundeck.app.authorization.domain.projectType

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.app.type.ProjectTypeIdentifier
import spock.lang.Specification

import javax.security.auth.Subject

class AppAuthorizingProjectTypeSpec extends Specification {
    def "is authorized correct auth resource"() {

        given:
            def projectName = 'aproject'
            def typeName = 'atype'
            def testActions = ['read', 'write']
            def processor = Mock(AuthContextProcessor)
            def subject = new Subject()
            def named = Mock(NamedAuthProvider)

            def ptypeid = Mock(ProjectTypeIdentifier) {
                getProject() >> projectName

                getType() >> typeName
            }

            def sut = new AppAuthorizingProjectType(
                processor,
                subject,
                named,
                ptypeid
            )
            def actions = Mock(AuthActions) {

                getActions() >> testActions
            }
            def authContext = Mock(UserAndRolesAuthContext)
        when:
            def result = sut.isAuthorized(actions)
        then:
            1 * processor.getAuthContextForSubjectAndProject(subject, projectName) >> authContext
            1 * processor.authorizeProjectResourceAny(
                authContext,
                [type: 'resource', kind: typeName],
                testActions,
                projectName
            ) >> isAuthorized
            result == isAuthorized
        where:
            isAuthorized << [true, false]
    }
}
