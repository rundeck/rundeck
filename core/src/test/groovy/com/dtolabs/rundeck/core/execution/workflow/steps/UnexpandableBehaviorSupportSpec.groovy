package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.data.UnexpandableBehavior
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification

class UnexpandableBehaviorSupportSpec extends Specification {

    def "static unexpandableBehavior metadata is honored"() {
        given:
        Description description = DescriptionBuilder.builder()
                .name('test')
                .property(PropertyBuilder.builder()
                        .string('script')
                        .blankIfUnexpandable(true)
                        .unexpandableBehavior(UnexpandableBehavior.PRESERVE_BASH)
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(description).script == UnexpandableBehavior.PRESERVE_BASH
    }

    def "without metadata uses blankIfUnexpandable"() {
        given:
        Description description = DescriptionBuilder.builder()
                .name('test')
                .property(PropertyBuilder.builder()
                        .string('script')
                        .blankIfUnexpandable(false)
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(description).script == UnexpandableBehavior.PRESERVE
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

    def "From takes precedence over static unexpandableBehavior"() {
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
                        .unexpandableBehavior(UnexpandableBehavior.PRESERVE)
                        .unexpandableBehaviorFrom('unexpandableMode')
                        .build())
                .build()

        expect:
        UnexpandableBehaviorSupport.buildBehaviorMap(
                description, true, [unexpandableMode: 'preserveBash']).script == UnexpandableBehavior.PRESERVE_BASH
    }
}
