package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.common.ProjectManager
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.NotFound
import spock.lang.Specification

import javax.security.auth.Subject

class AppAuthorizingProjectSpec extends Specification {
    def "get resource throws NotFound if missing"() {
        given:
            def acp = Mock(AuthContextProcessor)
            def named = Mock(NamedAuthProvider)
            def mgr = Mock(ProjectManager)
            def subject = new Subject()
            def project = 'aproject'
            def sut = new AppAuthorizingProject(acp, subject, named, project, mgr)
        when:
            def res = sut.getResource()
        then:
            NotFound notFound = thrown()
            1 * mgr.existsFrameworkProject('aproject') >> false
    }
}
