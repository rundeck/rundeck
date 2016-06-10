package com.dtolabs.rundeck.core.common

import com.dtolabs.rundeck.core.utils.IPropertyLookup
import spock.lang.Specification

/**
 * Created by greg on 6/10/16.
 */
class FrameworkBaseSpec extends Specification {
    def "project globals"() {
        given:
        def project = Mock(IRundeckProject) {
            1 * getProperties() >> projProps
        }
        def mgr = Mock(ProjectManager) {
            1 * getFrameworkProject('test') >> project
        }
        def lookup = Mock(IPropertyLookup)
        def services = Mock(IFrameworkServices)
        def nodes = Mock(IFrameworkNodes)
        def base = new FrameworkBase(mgr, lookup, services, nodes)

        when:
        def result = base.getProjectGlobals('test')
        then:

        result == expected

        where:
        projProps                                              | expected
        [:]                                                    | [:]
        ['framework.globals.a': 'b']                           | [a: 'b']
        ['project.globals.a': 'b']                             | [a: 'b']
        ['framework.globals.a': 'b', 'project.globals.a': 'c'] | [a: 'c']
        ['framework.globals.': 'b']                            | [:]
    }
}
