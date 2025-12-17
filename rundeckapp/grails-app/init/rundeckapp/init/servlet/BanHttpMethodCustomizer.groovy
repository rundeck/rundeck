package rundeckapp.init.servlet

import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.Callback
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

/**
 * Validates jetty configuration to avoid disallowed http methods and paths
 * 
 * Jetty 12 Migration Note: Uses Handler.Wrapper instead of HttpConfiguration.Customizer
 * because Customizers no longer have access to Response objects for sending error codes.
 */
class BanHttpMethodCustomizer implements JettyServerCustomizer {

    @Override
    void customize(Server server) {
        // Jetty 12: Use Handler.Wrapper to intercept and reject requests
        HttpMethodFilter filter = new HttpMethodFilter([HttpMethod.TRACE])
        filter.setHandler(server.getHandler())
        server.setHandler(filter)
    }
}

/**
 * Handler that intercepts and rejects banned HTTP methods
 * 
 * Jetty 12 requires Handler.Wrapper for request rejection because HttpConfiguration.Customizer
 * executes too early in the lifecycle (before Response is initialized) to send error codes.
 */
class HttpMethodFilter extends Handler.Wrapper {

    /**
     * Banned http methods
     */
    final List<HttpMethod> banMethods

    HttpMethodFilter(List<HttpMethod> methods) {
        super()
        this.banMethods = methods
    }

    @Override
    boolean handle(Request request, Response response, Callback callback) throws Exception {
        HttpMethod currentMethod = HttpMethod.fromString(request.getMethod())
        
        if (banMethods.contains(currentMethod)) {
            // Reject the request with 405 Method Not Allowed
            Response.writeError(request, response, callback, 405, "Method Not Allowed")
            return true // Request handled (rejected)
        }
        
        // Grails 7/Jetty 12: Don't call super.handle() here - let the wrapped handler handle it directly
        // This prevents ServletApiRequest.getRequest() NPE by maintaining proper request context
        Handler next = getHandler()
        if (next != null) {
            return next.handle(request, response, callback)
        }
        
        return false
    }

    @Override
    List<Handler> getHandlers() {
        // Return the wrapped handler(s) if any
        Handler wrappedHandler = getHandler()
        return wrappedHandler ? [wrappedHandler] : []
    }
}
