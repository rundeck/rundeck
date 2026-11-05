package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.core.execution.workflow.OutputContext
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification

class PluginOutputCaptureSpec extends Specification {

    static class PluginWithOutputOnlyField {
        String outputResult
    }

    static class PluginWithThrowingToString {
        Object outputResult = new Object() {
            @Override
            String toString() {
                throw new RuntimeException("boom")
            }
        }
    }

    static class PluginWithOneThrowingAndOneOkField {
        Object outputResult = new PluginWithThrowingToString().outputResult
        String environmentName = "production"
    }

    def "captures an output-only property's value from the field, ignoring a same-named config entry"() {
        given:
        def outputContext = Mock(OutputContext)
        def property = PropertyBuilder.builder()
                                       .string("outputResult")
                                       .title("outputResult")
                                       .outputMetadata([new PluginOutputMetadata("data", "outputResult", "desc")])
                                       .outputOnly(true)
                                       .build()
        def description = DescriptionBuilder.builder().name("someplugin").property(property).build()
        def plugin = new PluginWithOutputOnlyField(outputResult: "FROM_FIELD")
        // Simulates a job authored via API/YAML with a `configuration` entry that happens to
        // match the output-only property's name -- since the property has no @PluginProperty,
        // this can never be a legitimate configured value and must not shadow the field's value.
        def config = [outputResult: "FROM_CONFIG_SHOULD_BE_IGNORED"]

        when:
        PluginOutputCapture.captureOutputMetadataValues(outputContext, description, plugin, config)

        then:
        1 * outputContext.addOutput("data", "outputResult", "FROM_FIELD")
    }

    def "does not propagate a failure resolving one property's value, and captures no output for it"() {
        given:
        def outputContext = Mock(OutputContext)
        def property = PropertyBuilder.builder()
                                       .string("outputResult")
                                       .title("outputResult")
                                       .outputMetadata([new PluginOutputMetadata("data", "outputResult", "desc")])
                                       .outputOnly(true)
                                       .build()
        def description = DescriptionBuilder.builder().name("someplugin").property(property).build()
        // The field is found via reflection fine, but its value's toString() throws -- this must
        // be swallowed (logged, not propagated), since capture is best-effort metadata and must
        // never turn an already-successful step into a failure.
        def plugin = new PluginWithThrowingToString()

        when:
        PluginOutputCapture.captureOutputMetadataValues(outputContext, description, plugin, [:])

        then:
        noExceptionThrown()
        0 * outputContext.addOutput(*_)
    }

    def "continues capturing remaining properties after one property's resolution throws"() {
        given:
        def outputContext = Mock(OutputContext)
        def throwingProperty = PropertyBuilder.builder()
                                               .string("outputResult")
                                               .title("outputResult")
                                               .outputMetadata([new PluginOutputMetadata("data", "outputResult", "desc")])
                                               .outputOnly(true)
                                               .build()
        def okProperty = PropertyBuilder.builder()
                                         .string("environmentName")
                                         .title("environmentName")
                                         .outputMetadata([new PluginOutputMetadata("data", "environmentName", "desc")])
                                         .outputOnly(true)
                                         .build()
        def description = DescriptionBuilder.builder()
                                             .name("someplugin")
                                             .property(throwingProperty)
                                             .property(okProperty)
                                             .build()

        when:
        PluginOutputCapture.captureOutputMetadataValues(
                outputContext,
                description,
                new PluginWithOneThrowingAndOneOkField(),
                [:]
        )

        then:
        noExceptionThrown()
        1 * outputContext.addOutput("data", "environmentName", "production")
        0 * outputContext.addOutput("data", "outputResult", _)
    }
}
