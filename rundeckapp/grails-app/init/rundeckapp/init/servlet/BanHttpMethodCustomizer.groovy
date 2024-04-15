package rundeckapp.init.servlet

import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HostHeaderCustomizer
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

/**
 * Validates jetty configuration to avoid disallowed http methods and paths
 */
class BanHttpMethodCustomizer implements JettyServerCustomizer {

    @Override
    void customize(Server server) {

        server.connectors.each {connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).httpConfiguration
            config.addCustomizer(new BanHttpCustomizer([HttpMethod.TRACE]))
        }
    }
}

class RundeckHostHeaderCustomizer implements JettyServerCustomizer {
    String serverUrl
    int serverPort

    RundeckHostHeaderCustomizer(String serverUrl) {
        this.serverUrl = serverUrl
    }

    RundeckHostHeaderCustomizer(String serverUrl, int serverPort) {
        this.serverUrl = serverUrl
        this.serverPort = serverPort
    }

    @Override
    void customize(Server server) {
        server.connectors.each {connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).httpConfiguration
            config.addCustomizer(new HostHeaderCustomizer(serverUrl, serverPort))
        }
    }
}

/**
 * Includes and validates paths and http methods banned for security reasons
 */
class BanHttpCustomizer implements HttpConfiguration.Customizer {

    /**
     * Banned http methods
     */
    final List<HttpMethod> banMethods

    BanHttpCustomizer(List<HttpMethod> methods) {
        this.banMethods = methods
    }

    @Override
    void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        HttpMethod currentMethod = HttpMethod.fromString(request.method)
        if (banMethods.contains(currentMethod)) {
            request.handled = true
            request.response.status = HttpStatus.METHOD_NOT_ALLOWED_405
        }
    }
}
