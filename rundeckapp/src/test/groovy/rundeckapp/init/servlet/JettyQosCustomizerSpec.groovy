package rundeckapp.init.servlet

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.QoSHandler
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The request cap stays off unless configured, and health probes bypass it.
 */
class JettyQosCustomizerSpec extends Specification {

    def "maxRequests 0 installs no QoSHandler and leaves the handler chain unchanged"() {
        given:
        JettyServletContainerCustomizer container = new JettyServletContainerCustomizer(
                qosMaxRequests: 0,
                qosMaxSuspendSeconds: 60
        )
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()
        container.customize(factory)
        Server server = new Server()
        ServletContextHandler original = new ServletContextHandler('/')
        server.handler = original

        when:
        factory.serverCustomizers.each { it.customize(server) }

        then:
        factory.serverCustomizers.every { !(it instanceof JettyQosCustomizer) }
        server.handler.is(original)
    }

    def "maxRequests above zero installs a QoSHandler for every path except health"() {
        given:
        JettyServletContainerCustomizer container = new JettyServletContainerCustomizer(
                qosMaxRequests: 4,
                qosMaxSuspendSeconds: 15
        )
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory()
        container.customize(factory)
        JettyQosCustomizer qosCustomizer = factory.serverCustomizers.find { it instanceof JettyQosCustomizer } as JettyQosCustomizer
        Server server = new Server()
        ServletContextHandler original = new ServletContextHandler('/')
        server.handler = original

        when:
        qosCustomizer.customize(server)
        QoSHandler qos = server.handler as QoSHandler

        then:
        qosCustomizer != null
        server.handler instanceof HealthExemptQoSHandler
        qos.maxRequestCount == 4
        qos.maxSuspend == Duration.ofSeconds(15)
        qos.handler.is(original)
    }

    @Unroll
    def "health path #path under context #ctx is exempt=#exempt"() {
        expect:
        HealthExemptQoSHandler.isHealthPath(path, ctx) == exempt

        where:
        path                                    | ctx        | exempt
        '/monitoring/health'                    | ''         | true
        '/monitoring/health/liveness'           | ''         | true
        '/monitoring/health/readiness/'         | '/'        | true
        '/rundeck/monitoring/health/startup'    | '/rundeck' | true
        '/monitoring/healthx'                   | ''         | false
        '/api/x/monitoring/health'              | ''         | false
        '/'                                     | ''         | false
        null                                    | ''         | false
        '/monitoring/health'                    | '/rundeck' | false
    }

    @Unroll
    def "only #method counts as a probe method: #probe"() {
        expect:
        HealthExemptQoSHandler.isProbeMethod(method) == probe

        where:
        method | probe
        'GET'  | true
        'HEAD' | true
        'POST' | false
        'PUT'  | false
        null   | false
    }

    @Unroll
    def "maxSuspendSeconds=#secs is rejected when the cap is enabled"() {
        given:
        Server server = new Server()
        server.handler = new ServletContextHandler('/')

        when:
        new JettyQosCustomizer(maxRequests: 2, maxSuspendSeconds: secs).customize(server)

        then:
        thrown(IllegalArgumentException)

        where:
        secs << [0, -1]
    }

    def "health answers while non-health requests are past the cap"() {
        expect:
        healthStaysUp('/')
    }

    def "health answers under a non-root context path while the cap is full"() {
        expect:
        healthStaysUp('/rundeck')
    }

    private static void healthStaysUp(String contextPath) {
        CountDownLatch entered = new CountDownLatch(2)
        CountDownLatch release = new CountDownLatch(1)
        Server server = new Server()
        ServerConnector connector = new ServerConnector(server)
        connector.port = 0
        server.addConnector(connector)
        server.handler = context(entered, release, contextPath)
        new JettyQosCustomizer(maxRequests: 2, maxSuspendSeconds: 1, contextPath: contextPath).customize(server)
        server.start()
        int port = connector.localPort
        HttpClient client = HttpClient.newHttpClient()
        String prefix = contextPath == '/' ? '' : contextPath

        try {
            CountDownLatch finished = new CountDownLatch(2)
            List<CompletableFuture<HttpResponse<String>>> slow = (1..4).collect {
                CompletableFuture<HttpResponse<String>> f =
                        client.sendAsync(request(port, "${prefix}/slow/x"), HttpResponse.BodyHandlers.ofString())
                // Count down on a dependent stage; the source future is already done when it runs.
                f.whenComplete { r, e -> finished.countDown() }
                f
            }
            assert entered.await(5, TimeUnit.SECONDS)

            long started = System.nanoTime()
            HttpResponse<String> liveness = client.send(
                    request(port, "${prefix}/monitoring/health/liveness"),
                    HttpResponse.BodyHandlers.ofString())
            HttpResponse<String> readiness = client.send(
                    request(port, "${prefix}/monitoring/health/readiness"),
                    HttpResponse.BodyHandlers.ofString())
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L

            // The two over-cap requests expire after maxSuspendSeconds while the latch holds the rest.
            assert finished.await(8, TimeUnit.SECONDS)
            List<CompletableFuture<HttpResponse<String>>> expired = slow.findAll { it.isDone() }
            List<HttpResponse<String>> rejected = expired.collect { it.get() }
            release.countDown()
            List<HttpResponse<String>> served = (slow - expired).collect { it.get(5, TimeUnit.SECONDS) }

            assert liveness.statusCode() == 200
            assert readiness.statusCode() == 200
            assert liveness.body() == 'UP'
            assert readiness.body() == 'UP'
            assert elapsedMs < 1000
            assert rejected.size() == 2
            assert rejected.every { it.statusCode() == 503 && it.headers().firstValue('retry-after').orElse('') == '1' }
            assert served.size() == 2
            assert served.every { it.statusCode() == 200 && it.body() == 'ok' }
        } finally {
            release.countDown()
            server.stop()
        }
    }

    private static ServletContextHandler context(CountDownLatch entered, CountDownLatch release, String contextPath) {
        ServletContextHandler context = new ServletContextHandler(contextPath)
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                entered.countDown()
                release.await(15, TimeUnit.SECONDS)
                resp.writer.print('ok')
            }
        }), '/slow/*')
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                resp.writer.print('UP')
            }
        }), '/monitoring/health/*')
        context
    }

    private static HttpRequest request(int port, String path) {
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:${port}${path}")).GET().build()
    }
}
