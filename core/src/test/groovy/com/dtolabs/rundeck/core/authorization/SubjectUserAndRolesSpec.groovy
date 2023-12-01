package com.dtolabs.rundeck.core.authorization

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import spock.lang.Specification

import javax.security.auth.Subject
import java.security.Principal

class SubjectUserAndRolesSpec extends Specification {
    static class OtherPrincipal implements Principal {
        String name
    }

    def "create"() {
        given:
            def subject = new Subject()
            subject.getPrincipals().addAll([
                new Username('auser'),
                new Group('agroup'),
                new Group('bgroup'),
                new OtherPrincipal(name: 'bob')
            ])
        when:
            def result = new SubjectUserAndRoles(subject)
        then:
            result.username == 'auser'
            result.roles == ['agroup', 'bgroup'].toSet()

    }
}
