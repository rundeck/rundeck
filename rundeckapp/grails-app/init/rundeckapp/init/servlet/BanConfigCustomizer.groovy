package rundeckapp.init.servlet

import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

/**
 * Validates jetty configuration to avoid disallowed http methods and paths
 */
class BanConfigCustomizer implements JettyServerCustomizer {

    @Override
    void customize(Server server) {

        server.connectors.each {connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).httpConfiguration
            config.addCustomizer(new BanHttpCustomizer([HttpMethod.TRACE]))
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
    /**
     * Banned path
     */
    final String banPath = "/"

    BanHttpCustomizer(List<HttpMethod> methods) {
        this.banMethods = methods
    }

    @Override
    void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        HttpMethod currentMethod = HttpMethod.fromString(request.method)
        String currentPath = request.pathInfo
        if (banMethods.contains(currentMethod) && currentPath == banPath) {
            request.handled = true
            request.response.status = HttpStatus.METHOD_NOT_ALLOWED_405
        }
    }
}
