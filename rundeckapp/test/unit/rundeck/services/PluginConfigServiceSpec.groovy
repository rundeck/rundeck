package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import rundeck.services.scm.ScmPluginConfig
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(PluginConfigService)
class PluginConfigServiceSpec extends Specification {


    def "loadScmConfig dne"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)

        when:
        def config = service.loadScmConfig('test1', 'a/path', integration)


        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.existsFileResource('a/path') >> false
        config == null

        where:
        integration | _
        'import'    | _
        'export'    | _

    }

    def "loadScmConfig exists"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)
        def propertiesString = prefix + '.config.a=b\n' + prefix + '.something=another\n'
        when:
        def config = service.loadScmConfig('test1', 'a/path', prefix)

        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.existsFileResource('a/path') >> true
        1 * project.loadFileResource('a/path', _) >> { args ->
            args[1].write(propertiesString.bytes)
            propertiesString.length()
        }
        config != null
        config.prefix == prefix
        config.config == [a: 'b']
        config.getSetting('something') == 'another'


        where:
        prefix   | _
        'import' | _
        'export' | _

    }
    def "storeconfig"(){
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)
        def props = [
                'a.b':'c',
                'a.config.d':'e'
        ] as Properties
        def config = new ScmPluginConfig(props, 'a.')

        when:
        service.storeConfig(config, 'test1', 'a/path')

        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.storeFileResource('a/path', _)

    }
}
