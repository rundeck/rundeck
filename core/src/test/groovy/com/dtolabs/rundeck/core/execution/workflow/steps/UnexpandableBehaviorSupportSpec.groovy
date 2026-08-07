package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.data.UnexpandableBehavior
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification

class UnexpandableBehaviorSupportSpec extends Specification {

    def "without From uses blankIfUnexpandable"() {
        given:
        Description description = DescriptionBuilder.builder()
                .name('test')
                .property(PropertyBuilder.builder()
                        .string('script')
                        .blankIfUnexpandable(blankIf)
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(description).script == expected

        where:
        blankIf | expected
        true    | UnexpandableBehavior.BLANK
        false   | UnexpandableBehavior.PRESERVE
    }

    def "resolves unexpandableBehaviorFrom instance config"() {
        given:
        Description description = DescriptionBuilder.builder()
                .name('test')
                .property(PropertyBuilder.builder()
                        .select('unexpandableMode')
                        .values('blank', 'preserveBash')
                        .defaultValue('blank')
                        .build())
                .property(PropertyBuilder.builder()
                        .string('script')
                        .unexpandableBehaviorFrom('unexpandableMode')
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(
                description, true, [unexpandableMode: 'preserveBash']).script == UnexpandableBehavior.PRESERVE_BASH
        UnexpandableBehaviorSupport.buildBehaviorMap(
                description, true, [unexpandableMode: 'blank']).script == UnexpandableBehavior.BLANK
    }

    def "uses sibling default blank when From config missing"() {
        given:
        Description description = DescriptionBuilder.builder()
                .name('test')
                .property(PropertyBuilder.builder()
                        .select('unexpandableMode')
                        .values('blank', 'preserveBash')
                        .defaultValue('blank')
                        .build())
                .property(PropertyBuilder.builder()
                        .string('script')
                        .unexpandableBehaviorFrom('unexpandableMode')
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(description, true, [:]).script == UnexpandableBehavior.BLANK
    }
}
