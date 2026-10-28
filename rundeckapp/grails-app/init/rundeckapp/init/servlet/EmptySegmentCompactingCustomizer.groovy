package rundeckapp.init.servlet

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpURI
import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.URIUtil
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer

/**
 * Restores the Jetty 9 behavior of tolerating empty path segments (e.g. {@code //api/50/projects}).
 * <p>
 * Jetty 12 rejects them with {@code 400 Ambiguous URI empty segment}, both in the connector URI compliance
 * check and again when creating the servlet request. This customizer relaxes the compliance mode for
 * {@link UriCompliance.Violation#AMBIGUOUS_EMPTY_SEGMENT} only, and compacts the request path
 * ({@code //} to {@code /}) before it reaches any handler, so the servlet layer and URL mappings only ever
 * see a normal path. All other ambiguous URI violations (encoded dot segments, separators, etc.) are still
 * rejected.
 */
@CompileStatic
class EmptySegmentCompactingCustomizer implements JettyServerCustomizer, HttpConfiguration.Customizer {

    @Override
    void customize(Server server) {
        server.connectors.each { connector ->
            HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory)?.httpConfiguration
            if (config) {
                config.uriCompliance = allowEmptySegment(config.uriCompliance)
                config.addCustomizer(this)
            }
        }
    }

    /**
     * @param compliance current URI compliance mode
     * @return the same mode additionally allowing empty path segments
     */
    static UriCompliance allowEmptySegment(UriCompliance compliance) {
        compliance.with("${compliance.name}_EMPTY_SEGMENT", UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT)
    }

    @Override
    Request customize(Request request, HttpFields.Mutable responseHeaders) {
        HttpURI uri = request.httpURI
        if (!uri.hasViolation(UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT)) {
            return request
        }
        // re-setting the path re-parses it and clears the empty segment violation
        HttpURI compacted = HttpURI.build(uri).path(URIUtil.compactPath(uri.path)).asImmutable()
        return new Request.Wrapper(request) {
            @Override
            HttpURI getHttpURI() {
                compacted
            }
        }
    }
}
