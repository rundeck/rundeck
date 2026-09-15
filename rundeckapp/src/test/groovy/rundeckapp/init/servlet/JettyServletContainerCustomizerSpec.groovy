package rundeckapp.init.servlet

import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory
import spock.lang.Specification
import spock.lang.Unroll

class JettyServletContainerCustomizerSpec extends Specification {

    def "applySecureSessionCookieDefault enables secure flag when serverUrl is https and cookie secure is unset"() {
        given:
        JettyServletContainerCustomizer customizer = new JettyServletContainerCustomizer(
                serverUrl: 'https://rundeck.example.com/rundeck'
        )
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()

        when:
        customizer.applySecureSessionCookieDefault(factory)

        then:
        factory.settings.session.cookie.secure == true
    }

    def "applySecureSessionCookieDefault leaves cookie secure unset when serverUrl is http"() {
        given:
        JettyServletContainerCustomizer customizer = new JettyServletContainerCustomizer(
                serverUrl: 'http://rundeck.example.com/rundeck'
        )
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()

        when:
        customizer.applySecureSessionCookieDefault(factory)

        then:
        factory.settings.session.cookie.secure == null
    }

    def "applySecureSessionCookieDefault does not override an explicit secure=false setting"() {
        given:
        JettyServletContainerCustomizer customizer = new JettyServletContainerCustomizer(
                serverUrl: 'https://rundeck.example.com/rundeck'
        )
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()
        factory.settings.session.cookie.secure = false

        when:
        customizer.applySecureSessionCookieDefault(factory)

        then:
        factory.settings.session.cookie.secure == false
    }

    def "applySecureSessionCookieDefault is a no-op when serverUrl is not set"() {
        given:
        JettyServletContainerCustomizer customizer = new JettyServletContainerCustomizer(serverUrl: null)
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()

        when:
        customizer.applySecureSessionCookieDefault(factory)

        then:
        factory.settings.session.cookie.secure == null
    }

    @Unroll
    def "isHttpsUrl(#url) == #expected"() {
        expect:
        JettyServletContainerCustomizer.isHttpsUrl(url) == expected

        where:
        url                                     | expected
        'https://rundeck.example.com/rundeck'   | true
        'HTTPS://rundeck.example.com/rundeck'   | true
        'http://rundeck.example.com/rundeck'    | false
        null                                    | false
        ''                                      | false
    }
}
