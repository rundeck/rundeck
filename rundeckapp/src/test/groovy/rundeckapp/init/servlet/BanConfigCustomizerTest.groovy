package rundeckapp.init.servlet

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpTrace
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.embedded.jetty.JettyWebServer
import org.springframework.boot.web.server.WebServer
import org.springframework.boot.web.servlet.ServletContextInitializer
import spock.lang.Specification

class BanConfigCustomizerTest extends Specification {

    static final String SERVER_URL = "http://localhost"
    WebServer server

    def "Reject trace http method in root path"() {

        given:
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory("",port)
        ServletContextInitializer initializer = Mock(ServletContextInitializer)
        factory.addServerCustomizers(new BanConfigCustomizer())
        server = factory.getWebServer(initializer)
        server.start()

        when:
        HttpResponse response = initTraceRequest(port, path)

        then:
        response.statusLine.statusCode == statusCode
        response.getHeaders("Set-Cookie").length == 0

        where:
        port | path | statusCode
        9696 | "/"  | 405
    }

    def cleanup() {
        if (server) {
            server.stop()
        }
    }

    private HttpResponse initTraceRequest(int port, String path) {
        HttpClient httpClient = HttpClientBuilder.create().build()
        HttpTrace request = new HttpTrace("${SERVER_URL}:${port}${path}")
        return httpClient.execute(request)
    }
}
