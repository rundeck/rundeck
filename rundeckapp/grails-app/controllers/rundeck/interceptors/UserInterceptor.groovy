package rundeck.interceptors


class UserInterceptor {

    int order = HIGHEST_PRECEDENCE + 5

    def configurationService

    boolean before() {
        String redirectUri = configurationService.getString("login.redirectUri")
        println "redirect uri: " +redirectUri
        println "action name: " +actionName
        if(redirectUri && actionName == "login") {
            println "setting redirect"
            response.sendRedirect(redirectUri)
            return false
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
