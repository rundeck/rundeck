package rundeckapp.init.servlet

import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
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
    Request customize(Request request, HttpFields.Mutable responseHeaders) {
        // Jetty 12 API: customize(Request, HttpFields.Mutable)
        HttpMethod currentMethod = HttpMethod.fromString(request.getMethod())
        if (banMethods.contains(currentMethod)) {
            Response.writeError(request, request.getResponse(), null, HttpStatus.METHOD_NOT_ALLOWED_405)
        }
        return request
    }
}
