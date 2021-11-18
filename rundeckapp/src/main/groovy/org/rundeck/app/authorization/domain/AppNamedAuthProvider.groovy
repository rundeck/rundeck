package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthActions
import org.rundeck.core.auth.access.NamedAuthDefinition
import org.rundeck.core.auth.access.NamedAuthProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@CompileStatic
class AppNamedAuthProvider implements NamedAuthProvider, ApplicationContextAware {
    ApplicationContext applicationContext

    @Override
    AuthActions getNamedAuth(final String group, final String name) {
        def definitions = getAuthDefinitions(group)
        return definitions?.get(name)
    }

    Map<String, AuthActions> getAuthDefinitions(final String group) {
        Map<String, AuthActions> map = new HashMap<>()
        def beans = applicationContext.getBeansOfType(NamedAuthDefinition)
        def provider = beans.values().find { it.groupName == group }
        if (provider) {
            map.putAll(provider.definitions)
        }
        ServiceLoader.load(NamedAuthDefinition.class).each {
            map.putAll(it.definitions)
        }
        return map
    }
}
