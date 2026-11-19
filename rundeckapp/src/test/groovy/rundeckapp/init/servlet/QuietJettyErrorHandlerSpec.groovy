package rundeckapp.init.servlet

import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.util.BufferUtil
import spock.lang.Specification

class QuietJettyErrorHandlerSpec extends Specification {

    def "html error page omits exception text and stack"() {
        given:
        QuietJettyErrorHandler handler = new QuietJettyErrorHandler()
        StringWriter writer = new StringWriter()
        HttpServletRequest request = Stub(HttpServletRequest) {
            getAttribute(_) >> new IllegalStateException("max length exceeded: 26214400\n\tat org.eclipse.jetty.server.Request")
            getRequestURI() >> "/menu/acls/save"
        }

        when:
        handler.writeErrorPageMessage(request, writer, 500, "bad multipart", "/menu/acls/save")
        String body = writer.toString()

        then:
        body.contains("500")
        body.contains("/menu/acls/save")
        body.contains("Server Error")
        !body.contains("bad multipart")
        !body.contains("max length")
        !body.contains("IllegalStateException")
        !body.contains("CAUSED BY")
        !handler.showStacks
        !handler.showServlet
    }

    def "bad message body does not include the parser reason"() {
        given:
        QuietJettyErrorHandler handler = new QuietJettyErrorHandler()

        when:
        String text = BufferUtil.toString(handler.badMessageError(400, "bad multipart", HttpFields.build()))

        then:
        text.contains("400")
        text.contains("Bad Request")
        !text.contains("bad multipart")
        !text.contains("<pre>")
    }

    def "install keeps existing error pages and disables stacks"() {
        given:
        WebAppContext context = new WebAppContext()
        ErrorPageErrorHandler original = new ErrorPageErrorHandler()
        original.addErrorPage(500, "/error/500")
        context.errorHandler = original

        when:
        JettyServletContainerCustomizer.installQuietErrorHandler(context)

        then:
        context.errorHandler instanceof QuietJettyErrorHandler
        !((QuietJettyErrorHandler) context.errorHandler).showStacks
        ((ErrorPageErrorHandler) context.errorHandler).errorPages.containsValue("/error/500")
    }
}
