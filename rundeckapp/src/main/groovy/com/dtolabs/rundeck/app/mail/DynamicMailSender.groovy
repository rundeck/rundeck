package com.dtolabs.rundeck.app.mail

import grails.core.GrailsApplication
import grails.events.annotation.Subscriber
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.grails.events.AppEvents
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import javax.mail.Session


/**
 * DynamicMailSender is a JavaMailSender that can be reconfigured at runtime.
 * It subscribes to the rundeck.configuration.change event and will create a new JavaMailSender
 * when the grails.mail configuration changes.
 *
 */
@CompileStatic
@Slf4j
class DynamicMailSender implements JavaMailSender, ApplicationContextAware {
    @Delegate
    JavaMailSender delegate
    JavaMailSender original

    @Autowired
    GrailsApplication grailsApplication

    ApplicationContext applicationContext

    @CompileDynamic
    def updateMailSender() {
        if (!original) {
            original = delegate
        }
        def config = grailsApplication.config.getProperty('grails.mail', Map)
        delegate = constructMailSender(config)
    }

    @Subscriber(AppEvents.APP_CONFIG_CHANGED)
    @CompileDynamic
    def configurationChanged(def event) {
        Collection<String> keys = event.data
        if (keys.any { it.startsWith('grails.mail') }) {
            updateMailSender()
        }
    }

    JavaMailSender constructMailSender(Map config) {
        def impl = new JavaMailSenderImpl()
        impl.with {
            if (config.host) {
                host = config.host
            } else if (!config.jndiName) {
                def envHost = System.getenv()['SMTP_HOST']
                if (envHost) {
                    host = envHost
                } else {
                    host = "localhost"
                }
            }

            if (config.encoding) {
                defaultEncoding = config.encoding
            } else if (!config.jndiName) {
                defaultEncoding = "utf-8"
            }

            if (config.jndiName) {
                session = findMailSession()
            }
            if (config.port) {
                port = asInt(config.port)
            }
            if (config.username) {
                username = config.username
            }
            if (config.password) {
                password = config.password
            }
            if (config.protocol) {
                protocol = config.protocol
            }
            if (config.props instanceof Map && config.props) {
                javaMailProperties = flattenMapToProperties((Map) config.props)
            }
        }
        return impl
    }

    static int asInt(Object o) {
        if (o instanceof Integer) {
            return o
        } else {
            return Integer.parseInt(o.toString())
        }
    }

    Session findMailSession() {
        try {
            return applicationContext.getBean('mailSession', Session)
        } catch (NoSuchBeanDefinitionException ex) {
            return null
        }
    }

    static Properties flattenMapToProperties(Map<String, Object> props) {
        Properties properties = new Properties()
        flattenMapToProperties(props, '', properties)
        return properties
    }

    static void flattenMapToProperties(Map<String, Object> map, String prefix, Properties properties) {
        for (String key : map.keySet()) {
            def value = map.get(key)
            if (value instanceof Map) {
                flattenMapToProperties(value, prefix + key + '.', properties)
            } else {
                properties.put(prefix + key, value.toString())
            }
        }
    }
}
