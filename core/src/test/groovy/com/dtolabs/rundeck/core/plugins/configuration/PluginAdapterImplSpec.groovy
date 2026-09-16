package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.core.config.FeatureInfoService
import com.dtolabs.rundeck.plugins.descriptions.PluginOutput
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import spock.lang.Specification

class PluginAdapterImplSpec
    extends Specification {

    static class WithPluginOutput {
        @PluginProperty(title = "Environment Name", description = "the environment")
        @PluginOutput(name = "environmentName", description = "Exposed for conditional logic")
        String environmentName
    }

    static class WithRepeatedPluginOutput {
        @PluginProperty(title = "Multi")
        @PluginOutput(group = "data", name = "multiA", description = "first")
        @PluginOutput(group = "custom", name = "multiB", description = "second")
        String multi
    }

    static class WithoutPluginOutput {
        @PluginProperty(title = "Plain")
        String plain
    }

    def "propertyFromField reads @PluginOutput metadata"() {
        given:
            def adapter = new PluginAdapterImpl()
            def field = WithPluginOutput.getDeclaredField("environmentName")
            def annotation = field.getAnnotation(PluginProperty)

        when:
            Property property = adapter.propertyFromField(field, annotation)

        then:
            property.name == "environmentName"
            property.outputMetadata != null
            property.outputMetadata.size() == 1
            property.outputMetadata[0].group == "data"
            property.outputMetadata[0].name == "environmentName"
            property.outputMetadata[0].description == "Exposed for conditional logic"
    }

    def "propertyFromField reads multiple repeated @PluginOutput annotations"() {
        given:
            def adapter = new PluginAdapterImpl()
            def field = WithRepeatedPluginOutput.getDeclaredField("multi")
            def annotation = field.getAnnotation(PluginProperty)

        when:
            Property property = adapter.propertyFromField(field, annotation)

        then:
            property.outputMetadata != null
            property.outputMetadata.size() == 2
            property.outputMetadata[0].group == "data"
            property.outputMetadata[0].name == "multiA"
            property.outputMetadata[1].group == "custom"
            property.outputMetadata[1].name == "multiB"
    }

    def "propertyFromField returns null outputMetadata when no @PluginOutput present"() {
        given:
            def adapter = new PluginAdapterImpl()
            def field = WithoutPluginOutput.getDeclaredField("plain")
            def annotation = field.getAnnotation(PluginProperty)

        when:
            Property property = adapter.propertyFromField(field, annotation)

        then:
            property.outputMetadata == null
    }

    def "buildFieldProperties includes @PluginOutput metadata for the class"() {
        given:
            def adapter = new PluginAdapterImpl()

        when:
            List<Property> properties = adapter.buildFieldProperties(WithPluginOutput)

        then:
            def prop = properties.find { it.name == "environmentName" }
            prop != null
            prop.outputMetadata?.size() == 1
            prop.outputMetadata[0].name == "environmentName"
    }

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