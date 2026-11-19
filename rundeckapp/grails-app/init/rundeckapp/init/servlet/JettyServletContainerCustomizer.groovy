/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rundeckapp.init.servlet

import com.dtolabs.rundeck.core.init.CustomWebAppInitializer
import org.eclipse.jetty.jmx.MBeanContainer
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HostHeaderCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.ee10.webapp.AbstractConfiguration
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.core.env.PropertyResolver
import rundeck.services.feature.FeatureService

import java.lang.management.ManagementFactory

/**
 * Customize embedded jetty
 */
class JettyServletContainerCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {
    /**
     * Set of init parameters to set in the web app context
     */
    Map<String, String> initParams = [:]
    Boolean useForwardHeaders
    String serverUrl
    FeatureService featureService
    /**
     * Concurrent request cap installed by {@link JettyQosCustomizer}.
     * {@code 0} (the default) installs no handler.
     */
    int qosMaxRequests = 0
    /**
     * Seconds a request may stay suspended before {@code 503} with {@code Retry-After}.
     */
    int qosMaxSuspendSeconds = 60
    /**
     * Servlet context path, so {@link JettyQosCustomizer} exempts only the real health path.
     */
    String qosContextPath = ''

    /**
     * Reads the QoS settings the {@code jettyServletCustomizer} bean binds.
     *
     * @param config application configuration
     * @return {@code qosMaxRequests} ({@code rundeck.web.jetty.qos.maxRequests}, default 0),
     *         {@code qosMaxSuspendSeconds} ({@code rundeck.web.jetty.qos.maxSuspendSeconds}, default 60)
     *         and {@code qosContextPath} ({@code server.servlet.context-path}, default empty)
     */
    static Map<String, Object> qosProperties(PropertyResolver config) {
        return [
                qosMaxRequests      : config.getProperty('rundeck.web.jetty.qos.maxRequests', Integer, 0),
                qosMaxSuspendSeconds: config.getProperty('rundeck.web.jetty.qos.maxSuspendSeconds', Integer, 60),
                qosContextPath      : config.getProperty('server.servlet.context-path', String, '')
        ] as Map<String, Object>
    }

    @Override
    void customize(final JettyServletWebServerFactory factory) {
        factory.addServerCustomizers(new JettyServerCustomizer() {
            @Override
            void customize(Server server) {
                MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
                server.addEventListener(mbContainer);
                server.addBean(mbContainer);
                for (Handler handler : server.getHandlers()) {
                    if (handler instanceof ContextHandler) {
                        ((ContextHandler) handler).setMaxFormKeys(2000)
                    }
                }
            }
        })
        factory.addServerCustomizers(new EmptySegmentCompactingCustomizer())
        // Grails 7/Jetty 12: BanHttpMethodCustomizer disabled temporarily to debug ServletApiRequest NPE
        // TODO: Re-enable HTTP method filtering once ServletAPI issue is resolved
        if(serverUrl && featureService.featurePresent("setServerUrlOnNoHostHeader", false)) {
            factory.addServerCustomizers(new RundeckHostHeaderCustomizer(serverUrl: serverUrl))
        }
        if (qosMaxRequests > 0) {
            factory.addServerCustomizers(new JettyQosCustomizer(
                    maxRequests: qosMaxRequests,
                    maxSuspendSeconds: qosMaxSuspendSeconds,
                    contextPath: qosContextPath
            ))
        }
        factory.addConfigurations(new JettyConfigPropsInitParameterConfiguration(initParams))
        factory.useForwardHeaders=useForwardHeaders
        applySecureSessionCookieDefault(factory)
    }

    /**
     * Auto-enables the {@code Secure} flag on the session cookie when the configured
     * {@code grails.serverURL} uses the {@code https} scheme, unless the operator has
     * already set {@code server.servlet.session.cookie.secure} explicitly.
     * <p>
     * Skipped when {@code useForwardHeaders} is enabled: in that mode {@code grails.serverURL}
     * describes the public URL of an upstream TLS-terminating proxy, not the scheme this Jetty
     * listener itself serves, so it is not a reliable signal here. Forcing {@code Secure} in that
     * case would cause the session cookie to be silently dropped by clients (e.g. rundeck-cli)
     * connecting directly to Rundeck's own plain-HTTP port, breaking session/cookie-based
     * authentication for them (rundeck/rundeck#10584).
     * @param factory the servlet web server factory being customized
     */
    void applySecureSessionCookieDefault(final JettyServletWebServerFactory factory) {
        def cookie = factory.session?.cookie
        if (cookie != null && cookie.secure == null && !useForwardHeaders && isHttpsUrl(serverUrl)) {
            cookie.secure = true
        }
    }

    /**
     * @param url a URL string, may be null
     * @return true if the URL scheme is {@code https} (case-insensitive)
     */
    static boolean isHttpsUrl(final String url) {
        url != null && url.trim().toLowerCase().startsWith("https://")
    }
}

/**
 * Set init params for the WebAppContext
 */
class JettyConfigPropsInitParameterConfiguration extends AbstractConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(this)
    Map<String, String> initParams = [:]

    JettyConfigPropsInitParameterConfiguration(final Map<String, String> initParams) {
        super(new Builder())  // Jetty 12 requires explicit super() call
        this.initParams = initParams
    }

    @Override
    void preConfigure(final WebAppContext context) throws Exception {
        super.preConfigure(context)
        if(!initParams){
            initParams = [:]
        }
        for (String key : initParams.keySet()) {
            context.setInitParameter(key, initParams[key])
        }
        //Call custom initialization code
        try {
            def jettyCustomizers = getJettyCustomizers()
            jettyCustomizers.each { customizer ->
               LOG.debug("Customizing jetty with: ${customizer.class.canonicalName}")
               customizer.customizeWebAppContext(context)
            }
        } catch(Exception ex) {
            LOG.error("Unable to configure embedded container with custom client authenticator or login service code",ex)
            throw ex
        }
    }

    def getJettyCustomizers() {
        return ServiceLoader.load(CustomWebAppInitializer)
    }
}
