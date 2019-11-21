package rundeck.controllers

class ErrorController {

    def fiveHundred() {
        response.status = 500
        withFormat {
            xml {
                render "<error>An internal server error occurred</error>"
            }
            json {
                render '{"error":"A server error occurred"}'
            }
            all {
                render view:"error"
            }
        }
    }

    def notAllowed() {
        response.status = 405
        withFormat {
            xml {
                render "<error>Method not allowed</error>"
            }
            json {
                render '{"error":"A server error occurred"}'
            }
            all {
                render view:"405"
            }
        }
    }

    def notFound() {
        response.status = 404
        withFormat {
            xml {
                render "<error>Not found</error>"
            }
            json {
                render '{"error":"Not found"}'
            }
            all {
                render view:"404"
            }
        }
    }

}
