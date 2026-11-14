/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rundeckapp.init.servlet

import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler
import org.eclipse.jetty.ee10.webapp.AbstractConfiguration
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.BufferUtil

import java.nio.ByteBuffer

/**
 * Jetty error page that reports the status and request URI only.
 * Exception text, causes, servlet names, and stack traces stay in the server log.
 */
@CompileStatic
class QuietJettyErrorHandler extends ErrorPageErrorHandler {

    QuietJettyErrorHandler() {
        setShowStacks(false)
        setShowServlet(false)
        setShowMessageInTitle(false)
    }

    @Override
    protected void writeErrorPageMessage(HttpServletRequest request, Writer writer, int code, String message, String uri)
            throws IOException {
        String status = Integer.toString(code)
        writer.write("<h2>HTTP ERROR ")
        writer.write(status)
        writer.write("</h2>\n<table>\n")
        writeRow(writer, "URI", escape(uri))
        writeRow(writer, "STATUS", status)
        writeRow(writer, "MESSAGE", escape(HttpStatus.getMessage(code)))
        writer.write("</table>\n")
    }

    @Override
    protected void writeErrorPageStacks(HttpServletRequest request, Writer writer) throws IOException {
    }

    @Override
    protected void writeErrorPlain(HttpServletRequest request, PrintWriter writer, int code, String message) {
        writer.write("HTTP ERROR ")
        writer.write(Integer.toString(code))
        writer.write(' ')
        writer.write(HttpStatus.getMessage(code))
        writer.write("\n")
        writer.write("URI: ")
        writer.write(request.getRequestURI() ?: "")
        writer.write("\n")
    }

    @Override
    protected void writeErrorJson(HttpServletRequest request, PrintWriter writer, int code, String message) {
        writer.write("{\n")
        writer.write("\"status\": \"")
        writer.write(Integer.toString(code))
        writer.write("\",\n\"message\": \"")
        writer.write(HttpStatus.getMessage(code))
        writer.write("\"\n}")
    }

    @Override
    ByteBuffer badMessageError(int status, String reason, HttpFields.Mutable fields) {
        if (HttpStatus.hasNoBody(status)) {
            return BufferUtil.EMPTY_BUFFER
        }
        fields.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_HTML_8859_1.asString())
        String body = "<h1>HTTP ERROR " + status + "</h1><p>" + HttpStatus.getMessage(status) + "</p>"
        return BufferUtil.toBuffer(body)
    }

    private static void writeRow(Writer writer, String label, String value) throws IOException {
        writer.write("<tr><th>")
        writer.write(label)
        writer.write(":</th><td>")
        writer.write(value == null ? "-" : value)
        writer.write("</td></tr>\n")
    }

    private static String escape(String value) {
        if (value == null) {
            return null
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}

/**
 * Installs {@link QuietJettyErrorHandler} after Spring Boot has registered error pages,
 * copying those pages onto the quiet handler.
 */
@CompileStatic
class QuietJettyErrorPageConfiguration extends AbstractConfiguration {

    QuietJettyErrorPageConfiguration() {
        super(new Builder())
    }

    @Override
    void configure(WebAppContext context) throws Exception {
        JettyServletContainerCustomizer.installQuietErrorHandler(context)
    }
}
