package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.core.config.FeatureInfoService
import spock.lang.Specification

class PluginAdapterImplSpec
    extends Specification {

    def "map properties with feature flag scope"() {
        given:
            def adapter = new PluginAdapterImpl()
            adapter.featureInfoService = Mock(FeatureInfoService) {
                featurePresent(_) >> {
                    !!features[it[0]]
                }
            }
            def prop =
                PropertyUtil.string(name, null, null, false, null, null, PropertyScope.FeatureFlag, ropts)
            def resolver = Mock(PropertyResolver)
        when:
            def result = adapter.mapProperties(resolver, [prop], PropertyScope.InstanceOnly)
        then:
            result[name] == expected
            0 * resolver.resolvePropertyValue(*_)
        where:
            name       | features          | ropts                                                      | expected
            'someFlag' | [:]               | null                                                       | false
            'someFlag' | [someFlag: false] | null                                                       | false
            'someFlag' | [someFlag: true]  | null                                                       | true
            'other'    | [someFlag: true]  | null                                                       | false
            'other'    | [someFlag: false] | null                                                       | false
            'other'    | [someFlag: true]  | [(StringRenderingConstants.FEATURE_FLAG_NAME): 'someFlag'] | true
            'other'    | [someFlag: false] | [(StringRenderingConstants.FEATURE_FLAG_NAME): 'someFlag'] | false


    }

}