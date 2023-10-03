package rundeck.controllers

import grails.web.mime.MimeType

class ErrorController {
    static final String XML_500 = "<error>An internal server error occurred</error>"
    static final String JSON_500 = '{"error":"A server error occurred"}'
    static final String XML_405 = "<error>Method not allowed</error>"
    static final String JSON_405 = '{"error":"Method not allowed"}'
    static final String XML_404 = "<error>Not found</error>"
    static final String JSON_404 = '{"error":"Not found"}'

    def fiveHundred() {
        response.status = 500
        withFormat {
            html {
                response.contentType = MimeType.HTML.name
                render view:"/5xx"
            }
            xml {
                response.contentType = MimeType.XML.name
                render XML_500
            }
            json {
                response.contentType = MimeType.JSON.name
                render JSON_500
            }
        }
    }

    def notAllowed() {
        response.status = 405
        withFormat {
            html {
                response.contentType = MimeType.HTML.name
                render view:"/405"
            }
            xml {
                response.contentType = MimeType.XML.name
                render XML_405
            }
            json {
                response.contentType = MimeType.JSON.name
                render JSON_405
            }

        }
    }

    def notFound() {
        response.status = 404
        withFormat {
            html {
                response.contentType = MimeType.HTML.name
                render view:"/404"
            }
            xml {
                response.contentType = MimeType.XML.name
                render XML_404
            }
            json {
                response.contentType = MimeType.JSON.name
                render JSON_404
            }
        }
    }

}
