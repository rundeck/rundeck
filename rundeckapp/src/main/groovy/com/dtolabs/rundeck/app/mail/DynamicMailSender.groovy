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

import javax.annotation.PostConstruct
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

    /**
     * Initialize the delegate on bean creation to avoid null pointer exceptions
     */
    @PostConstruct
    void init() {
        updateMailSender()
    }

    @CompileDynamic
    def updateMailSender() {
        if (!original) {
            original = delegate
        }
        def config = grailsApplication.config.getProperty('grails.mail', Map)
        if (config == null) {
            config = [:]
        }
        delegate = constructMailSender(config)
    }

    /**
     * Handles application configuration change events.
     * <p>
     * When any configuration key that starts with {@code grails.mail} changes,
     * this method triggers a rebuild of the underlying {@link JavaMailSender}
     * delegate so that mail settings are updated at runtime.
     *
     * @param keys set of configuration keys that have changed
     */
    @Subscriber(AppEvents.APP_CONFIG_CHANGED)
    def configurationChanged(final Set<String> keys) {
        if (!keys) {
            return
        }
        if (keys.any { it.startsWith('grails.mail') }) {
            updateMailSender()
        }
    }

    /**
     * Constructs a {@link JavaMailSender} instance based on the provided configuration map.
     * <p>
     * The {@code config} map typically comes from the {@code grails.mail} configuration and
     * may contain the following optional keys:
     * </p>
     * <ul>
     *     <li><strong>host</strong> ({@code String}) – SMTP server host name. If not set and
     *     {@code jndiName} is not provided, the value of the {@code SMTP_HOST} environment
     *     variable is used when present, otherwise {@code "localhost"} is used.</li>
     *     <li><strong>port</strong> ({@code Integer} or {@code String}) – SMTP server port. If
     *     omitted, the {@link JavaMailSenderImpl} default port is used.</li>
     *     <li><strong>username</strong> ({@code String}) – SMTP authentication username. If
     *     omitted, the {@link JavaMailSenderImpl} default (no username) is used.</li>
     *     <li><strong>password</strong> ({@code String}) – SMTP authentication password. If
     *     omitted, the {@link JavaMailSenderImpl} default (no password) is used.</li>
     *     <li><strong>encoding</strong> ({@code String}) – Default message encoding. If not set
     *     and {@code jndiName} is not provided, this defaults to {@code "utf-8"}.</li>
     *     <li><strong>jndiName</strong> ({@code String}) – When present, indicates that a
     *     {@link javax.mail.Session} bean named {@code "mailSession"} should be obtained from
     *     the {@link org.springframework.context.ApplicationContext} via {@link #findMailSession()}.
     *     When {@code jndiName} is provided, the host and encoding defaults described above are
     *     not applied; any unset values remain at the {@link JavaMailSenderImpl} defaults.</li>
     *     <li><strong>protocol</strong> ({@code String}) – Mail transport protocol (for example,
     *     {@code "smtp"} or {@code "smtps"}). If omitted, the {@link JavaMailSenderImpl} default
     *     protocol is used.</li>
     *     <li><strong>props</strong> ({@code Map&lt;String, Object&gt;}) – Additional JavaMail
     *     properties. Nested maps are supported and are flattened into dot-separated property
     *     keys (for example, {@code [smtp:[auth:true]]} becomes {@code "smtp.auth"="true"}) via
     *     {@link #flattenMapToProperties(Map)} and assigned to {@code javaMailProperties}.</li>
     * </ul>
     * <p>
     * Any keys not provided in {@code config} leave the corresponding {@link JavaMailSenderImpl}
     * properties at their defaults.
     * </p>
     *
     * @param config mail configuration map, usually derived from {@code grails.mail}
     * @return a new {@link JavaMailSender} instance configured according to {@code config}
     */
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
                def mailSession = findMailSession()
                if (mailSession) {
                    session = mailSession
                }
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
