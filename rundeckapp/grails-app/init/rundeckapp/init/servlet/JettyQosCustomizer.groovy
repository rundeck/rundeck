package rundeckapp.init.servlet

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.QoSHandler
import org.eclipse.jetty.util.Callback
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

import java.time.Duration

/**
 * Caps concurrent requests on every path except the monitoring health endpoints.
 * <p>
 * Requests over the cap are suspended and do not hold a Jetty thread. When the suspend
 * budget runs out they receive {@code 503} with {@code Retry-After}. Liveness, readiness
 * and startup all live under {@code /monitoring/health} and skip the cap, so a stalled
 * request type cannot starve the probes.
 * <p>
 * {@code maxRequests} of {@code 0} or less installs nothing. The cap stays off until an
 * instance has a measured thread-pool size to set it from.
 */
@CompileStatic
class JettyQosCustomizer implements JettyServerCustomizer {

    /**
     * Exact health path and the servlet spec for everything under it.
     */
    static final String HEALTH_PATH = '/monitoring/health'
    static final String HEALTH_PATH_CHILDREN = '/monitoring/health/*'

    int maxRequests = 0
    int maxSuspendSeconds = 60

    @Override
    void customize(Server server) {
        if (maxRequests <= 0 || server.handler == null) {
            return
        }
        HealthExemptQoSHandler qos = new HealthExemptQoSHandler(maxSuspendSeconds)
        qos.maxRequestCount = maxRequests
        qos.maxSuspend = Duration.ofSeconds(Math.max(maxSuspendSeconds, 0))
        qos.handler = server.handler
        server.handler = qos
    }
}

/**
 * {@link QoSHandler} that does not count health checks and adds {@code Retry-After}
 * when a suspended request expires.
 * <p>
 * {@link #getHandlers()} is restated here because Groovy does not inherit the default
 * method on {@link Handler.Singleton} through this Jetty type.
 */
@CompileStatic
class HealthExemptQoSHandler extends QoSHandler {
    private final int retryAfterSeconds

    HealthExemptQoSHandler(int retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds
        excludePath(JettyQosCustomizer.HEALTH_PATH, JettyQosCustomizer.HEALTH_PATH_CHILDREN)
    }

    @Override
    List<Handler> getHandlers() {
        Handler next = getHandler()
        return next == null ? Collections.<Handler>emptyList() : Collections.singletonList(next)
    }

    @Override
    protected void failSuspended(Request request, Response response, Callback callback, int status, Throwable failure) {
        int responseStatus = status > 0 ? status : HttpStatus.SERVICE_UNAVAILABLE_503
        response.setStatus(responseStatus)
        int retryAfter = retryAfterSeconds > 0 ? retryAfterSeconds : 0
        response.headers.put(HttpHeader.RETRY_AFTER, Integer.toString(retryAfter))
        response.write(true, null, callback)
    }
}
