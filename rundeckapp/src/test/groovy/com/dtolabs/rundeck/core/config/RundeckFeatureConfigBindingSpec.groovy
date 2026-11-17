package com.dtolabs.rundeck.core.config

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import spock.lang.Specification

/**
 * An explicit false for the webhook template cache must survive the same
 * Spring bind that ConfigurationService uses on refresh.
 */
class RundeckFeatureConfigBindingSpec extends Specification {

    def "webhook template cache stays on when the feature property is absent"() {
        when:
        RundeckConfigBase bound = bind([
                'rundeck.feature.webhooks.enabled': 'true',
        ])

        then:
        bound.feature.webhookTemplateCache.enabled == true
    }

    def "webhook template cache false survives binding"() {
        when:
        RundeckConfigBase bound = bind([
                'rundeck.feature.webhookTemplateCache.enabled': 'false',
        ])

        then:
        bound.feature.webhookTemplateCache.enabled == false
    }

    private static RundeckConfigBase bind(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment()
        environment.propertySources.addFirst(new MapPropertySource('test', properties))
        return Binder.get(environment).bind('rundeck', Bindable.of(RundeckConfigBase)).get()
    }
}
