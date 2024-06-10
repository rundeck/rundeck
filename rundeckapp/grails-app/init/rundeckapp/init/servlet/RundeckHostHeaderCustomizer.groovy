package rundeckapp.init.servlet

import org.eclipse.jetty.server.HostHeaderCustomizer
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

class RundeckHostHeaderCustomizer implements JettyServerCustomizer {
    String serverUrl

    @Override
    void customize(Server server) {
        server.connectors.each {connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).httpConfiguration
            config.addCustomizer(new HostHeaderCustomizer(serverUrl))
        }
    }
}