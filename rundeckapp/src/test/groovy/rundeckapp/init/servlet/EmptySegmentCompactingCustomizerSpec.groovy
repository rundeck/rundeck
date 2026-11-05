package rundeckapp.init.servlet

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.http.HttpTester
import org.eclipse.jetty.server.LocalConnector
import org.eclipse.jetty.server.Server
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Runs the customizer against a real embedded Jetty server with a servlet context, so both the connector URI
 * compliance check and the servlet request ambiguity check are exercised.
 */
class EmptySegmentCompactingCustomizerSpec extends Specification {

    @AutoCleanup('stop')
    Server server
    LocalConnector connector

    def setup() {
        server = new Server()
        connector = new LocalConnector(server)
        server.addConnector(connector)
        def context = new ServletContextHandler('/')
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                resp.writer.print(req.requestURI)
            }
        }), '/*')
        server.handler = context
        new EmptySegmentCompactingCustomizer().customize(server)
        server.start()
    }

    private HttpTester.Response get(String path) {
        HttpTester.parseResponse(connector.getResponse("GET ${path} HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"))
    }

    @Unroll
    def "empty path segments are compacted before reaching the servlet: #path"() {
        when:
        def response = get(path)

        then:
        response.status == 200
        response.content == expected

        where:
        path                    | expected
        '//api/50/projects'     | '/api/50/projects'
        '/api//50///projects'   | '/api/50/projects'
        '/api/50/projects'      | '/api/50/projects'
        '//api/50/projects?a=b' | '/api/50/projects'
    }

    @Unroll
    def "other ambiguous URIs are still rejected: #path"() {
        expect:
        get(path).status == 400

        where:
        path << ['/api/%2e%2e/etc', '/api/a%2fb', '//api/%2e%2e/etc']
    }
}
