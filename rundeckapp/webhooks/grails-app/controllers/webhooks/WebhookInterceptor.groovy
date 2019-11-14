package webhooks


class WebhookInterceptor {

    int order = HIGHEST_PRECEDENCE + 20

    boolean before() {
        //A webhook must be a post, null action here indicates an unmatched HTTP method used
        if(!actionName) {
            response.setHeader("Allow","POST")
            response.status = 405
            return false
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
