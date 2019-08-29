package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ProjectPluginService)
class ProjectPluginServiceSpec extends Specification {

    def "get project default job plugin types"() {
        given:
        def project = Mock(IRundeckProject) {
            getProjectProperties() >> [
                    (ProjectPluginService.CONF_PROJECT_ENABLED + 'typeA'): 'true',
                    (ProjectPluginService.CONF_PROJECT_ENABLED + 'typeB'): 'true',
                    (ProjectPluginService.CONF_PROJECT_ENABLED + 'typeC'): 'false',
            ]
        }
        when:
        def result = service.getProjectDefaultProjectPluginTypes(project)
        then:
        result == (['typeA', 'typeB'] as Set)

    }

}
