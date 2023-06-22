package org.rundeck.app.authorization.domain.projectType

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.RundeckAccess
import spock.lang.Specification

import javax.security.auth.Subject

class AppProjectTypeAuthorizingProviderSpec extends Specification {
    def "getAuthorizingResource with resolver"() {
        given:
            def sut = new AppProjectTypeAuthorizingProvider()
            sut.namedAuthProvider = Mock(NamedAuthProvider)
            sut.rundeckAuthContextProcessor = Mock(AuthContextProcessor)
            def resolver = Mock(ResIdResolver)
            def subject = new Subject()
        when:
            def result = sut.getAuthorizingResource(subject, resolver)
        then:
            1 * resolver.idForType(RundeckAccess.Project.TYPE) >> 'ProjectName'
            1 * resolver.idForType(RundeckAccess.ProjectType.TYPE) >> 'restype'
            result != null
            result instanceof AppAuthorizingProjectType
            AppAuthorizingProjectType auth = (AppAuthorizingProjectType) result
            auth.project == 'ProjectName'
            auth.resourceIdent == 'restype'

    }
}
