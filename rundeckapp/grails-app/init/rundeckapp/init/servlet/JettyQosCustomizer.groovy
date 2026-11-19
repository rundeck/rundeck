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
import java.util.function.Predicate

/**
 * Caps concurrent requests on every path except the monitoring health endpoints.
 * <p>
 * Requests over the cap are suspended and do not hold a Jetty thread. When the suspend
 * budget runs out they receive {@code 503} with {@code Retry-After}. Liveness, readiness
 * and startup all live under {@code /monitoring/health} and skip the cap, so a stalled
 * request type cannot starve the probes. The handler wraps the whole server, so the match
 * uses the canonical request path prefixed with the servlet {@link #contextPath}.
 * <p>
 * {@code maxRequests} of {@code 0} or less installs nothing. The cap stays off until an
 * instance has a measured thread-pool size to set it from.
 */
@CompileStatic
class JettyQosCustomizer implements JettyServerCustomizer {

    /**
     * Health endpoint. Matched on the canonical path, with or without a context prefix.
     */
    static final String HEALTH_PATH = '/monitoring/health'

    int maxRequests = 0
    int maxSuspendSeconds = 60
    /**
     * Servlet context path ({@code server.servlet.context-path}); empty or {@code /} for root.
     */
    String contextPath = ''

    @Override
    void customize(Server server) {
        if (maxRequests <= 0 || server.handler == null) {
            return
        }
        HealthExemptQoSHandler qos = new HealthExemptQoSHandler(maxSuspendSeconds, contextPath)
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
    private final String healthPath

    /**
     * @param retryAfterSeconds value of the {@code Retry-After} header on a suspend timeout
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     */
    HealthExemptQoSHandler(int retryAfterSeconds, String contextPath) {
        this.retryAfterSeconds = retryAfterSeconds
        this.healthPath = healthPathFor(contextPath)
        // This handler sits outside the webapp context, so match the full canonical path.
        exclude(new Predicate<Request>() {
            @Override
            boolean test(Request request) {
                return matchesHealthPath(request?.httpURI?.canonicalPath, healthPath)
            }
        })
    }

    /**
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     * @return the health path under that context, e.g. {@code /rundeck/monitoring/health}
     */
    static String healthPathFor(String contextPath) {
        String ctx = contextPath ?: ''
        if (ctx.endsWith('/')) {
            ctx = ctx.substring(0, ctx.length() - 1)
        }
        return ctx + JettyQosCustomizer.HEALTH_PATH
    }

    /**
     * @param path canonical request path, including the servlet context path
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     * @return {@code true} only for {@code <contextPath>/monitoring/health} and paths under it
     */
    static boolean isHealthPath(String path, String contextPath) {
        return matchesHealthPath(path, healthPathFor(contextPath))
    }

    /**
     * @param path canonical request path
     * @param health full health path, from {@link #healthPathFor(String)}
     * @return {@code true} when the path is the health path or below it
     */
    static boolean matchesHealthPath(String path, String health) {
        if (path == null) {
            return false
        }
        return path == health || path.startsWith(health + '/')
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
