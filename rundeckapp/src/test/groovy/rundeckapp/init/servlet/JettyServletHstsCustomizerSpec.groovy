package rundeckapp.init.servlet

import org.eclipse.jetty.server.ServerConnector
import spock.lang.Specification
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.HttpConfiguration

import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer




class JettyServletHstsCustomizerSpec extends Specification {

    def "customize jetty server to add hsts header to static assets"() {

        given:"a server"
        Server server = new Server()
        HttpConfiguration httpsConfig = new HttpConfiguration()
        httpsConfig.securePort = 443
        ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpsConfig))
        server.setConnectors(new ServerConnector[] { httpConnector })
        JettyServletHstsCustomizer jettyServletHstsCustomizer = new JettyServletHstsCustomizer(stsMaxAgeSeconds,stsIncludeSubdomains)

        when:"applying a customization"
        jettyServletHstsCustomizer.customize(server)

        then:"server should be obtained with customization values applied"
        def secureRequestCustomizer = httpsConfig.getCustomizer(SecureRequestCustomizer)
        stsMaxAgeSeconds == secureRequestCustomizer.stsMaxAge
        stsIncludeSubdomains == secureRequestCustomizer.stsIncludeSubDomains

        where:
        stsMaxAgeSeconds | stsIncludeSubdomains
        31536000l        | true
        31536000l        | false
        12345            | true
    }

    def "evaluate if ssl is enabled"(){

        given:"an HttpConfiguration with a secure port enabled"
        HttpConfiguration httpsConfig = new HttpConfiguration()
        httpsConfig.securePort = 443
        def stsMaxAgeSeconds = 31536000l
        def stsIncludeSubdomains = true
        JettyServletHstsCustomizer jettyServletHstsCustomizer = new JettyServletHstsCustomizer(stsMaxAgeSeconds,stsIncludeSubdomains)

        when:"evaluating if isSslEnabled"
        def isSslEnabled= jettyServletHstsCustomizer.checkSSL(httpsConfig)

        then:"isSslEnabled should be true"
        isSslEnabled
    }


}