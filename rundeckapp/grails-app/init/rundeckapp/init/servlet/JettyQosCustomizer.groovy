package rundeckapp.init.servlet

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpFields
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
 * budget runs out they receive {@code 503} with {@code Retry-After}. The health probes
 * ({@code /monitoring/health} and its {@code liveness}, {@code readiness} and {@code startup}
 * paths, exact match, bodiless {@code GET}/{@code HEAD} only) skip the cap, so a stalled
 * request type cannot starve them. The handler wraps the whole server, so the match
 * uses the canonical request path prefixed with the servlet {@link #contextPath}.
 * <p>
 * {@code maxRequests} of {@code 0} or less installs nothing. The cap stays off until an
 * instance has a measured thread-pool size to set it from.
 * <p>
 * Requests the server makes to itself (for example remote option URLs that point at this
 * same instance) also take a permit while their caller holds one, so size the cap well
 * above 1 or such a request can wait on its own caller until the suspend budget expires.
 */
@CompileStatic
class JettyQosCustomizer implements JettyServerCustomizer {

    /**
     * Health endpoint. Matched on the canonical path, with or without a context prefix.
     */
    static final String HEALTH_PATH = '/monitoring/health'

    /**
     * Probe paths exempt from the cap, relative to the context path. Exact match only.
     */
    static final List<String> PROBE_PATHS = Collections.unmodifiableList([
            HEALTH_PATH,
            HEALTH_PATH + '/liveness',
            HEALTH_PATH + '/readiness',
            HEALTH_PATH + '/startup'
    ])

    /**
     * Concurrent request cap; {@code 0} or less disables it. Self-calls count against it too.
     */
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
        if (maxSuspendSeconds <= 0) {
            // Jetty treats a zero suspend as unlimited, so over-cap requests would never expire.
            throw new IllegalArgumentException(
                    "rundeck.web.jetty.qos.maxSuspendSeconds must be > 0 when the cap is enabled, was ${maxSuspendSeconds}")
        }
        HealthExemptQoSHandler qos = new HealthExemptQoSHandler(maxSuspendSeconds, contextPath)
        qos.maxRequestCount = maxRequests
        qos.maxSuspend = Duration.ofSeconds(maxSuspendSeconds)
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
    private final String ctxPrefix

    /**
     * @param retryAfterSeconds value of the {@code Retry-After} header on a suspend timeout
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     */
    HealthExemptQoSHandler(int retryAfterSeconds, String contextPath) {
        this.retryAfterSeconds = retryAfterSeconds
        this.ctxPrefix = contextPrefix(contextPath)
        // This handler sits outside the webapp context, so match the full canonical path.
        exclude(new Predicate<Request>() {
            @Override
            boolean test(Request request) {
                return isProbeMethod(request?.method) &&
                        !hasBody(request?.headers) &&
                        matchesProbePath(request?.httpURI?.canonicalPath, ctxPrefix)
            }
        })
    }

    /**
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     * @return the context path without a trailing slash, e.g. {@code /rundeck} or empty for root
     */
    static String contextPrefix(String contextPath) {
        String ctx = contextPath ?: ''
        return ctx.endsWith('/') ? ctx.substring(0, ctx.length() - 1) : ctx
    }

    /**
     * @param path canonical request path, including the servlet context path
     * @param contextPath servlet context path; {@code null}, empty or {@code /} for root
     * @return {@code true} only for the exact probe paths under that context
     */
    static boolean isHealthPath(String path, String contextPath) {
        return matchesProbePath(path, contextPrefix(contextPath))
    }

    /**
     * @param method HTTP method
     * @return {@code true} for {@code GET} and {@code HEAD}, the only methods probes use
     */
    static boolean isProbeMethod(String method) {
        return method == 'GET' || method == 'HEAD'
    }

    /**
     * A request that declares a body is never exempt: the webapp may read it (multipart
     * parsing, for one) and a slow upload would hold a worker outside the cap.
     *
     * @param headers request headers
     * @return {@code true} for a positive {@code Content-Length}, any {@code Transfer-Encoding}
     *         or a multipart {@code Content-Type}
     */
    static boolean hasBody(HttpFields headers) {
        if (headers == null) {
            return false
        }
        String contentType = headers.get(HttpHeader.CONTENT_TYPE)
        return headers.getLongField(HttpHeader.CONTENT_LENGTH) > 0 ||
                headers.contains(HttpHeader.TRANSFER_ENCODING) ||
                contentType?.toLowerCase(Locale.ROOT)?.startsWith('multipart/')
    }

    /**
     * @param path canonical request path
     * @param ctxPrefix context path from {@link #contextPrefix(String)}
     * @return {@code true} when the path is one of {@link JettyQosCustomizer#PROBE_PATHS}
     *         under the context, with at most one trailing slash
     */
    static boolean matchesProbePath(String path, String ctxPrefix) {
        if (path == null || !path.startsWith(ctxPrefix)) {
            return false
        }
        String rel = path.substring(ctxPrefix.length())
        if (rel.length() > 1 && rel.endsWith('/')) {
            rel = rel.substring(0, rel.length() - 1)
        }
        return JettyQosCustomizer.PROBE_PATHS.contains(rel)
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
