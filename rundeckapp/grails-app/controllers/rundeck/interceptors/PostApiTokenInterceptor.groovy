package rundeck.interceptors


class PostApiTokenInterceptor {

    int order = HIGHEST_PRECEDENCE + 105

    PostApiTokenInterceptor() {
        matchAll().excludes(controller:'user',action:'logout')
    }

    boolean before() { true }

    boolean after() {
        if(request?.authenticatedToken && session && session?.user){
            session.user=null
            request.subject=null
            session.subject=null
        }
        return true
    }

    void afterView() {
        // no-op
    }
}
