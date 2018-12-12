package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(NodeStatusService)
class NodeStatusServiceSpec extends Specification {

    def "project status nodecache delay"(String confval, long expected) {
        given:
        def properties = [:]
        if (null != confval) {
            properties['project.nodeStatusCache.delay'] = confval
        }
        def config = new PropsConfig(name: 'test1', properties: properties, projectProperties: properties)

        when:
        def result = service.nodeStatusCacheConfig(config)

        then:
        result == expected

        where:
        confval | expected
        '10'    | 10000
        null    | 30000
    }

    class PropsConfig implements IRundeckProjectConfig {
        Map<String, String> properties
        Map<String, String> projectProperties
        String name
        Date configLastModifiedTime


        @Override
        boolean hasProperty(final String key) {
            projectProperties.containsKey(key)
        }

        @Override
        String getProperty(final String property) {
            projectProperties.get(property)
        }
    }
}
