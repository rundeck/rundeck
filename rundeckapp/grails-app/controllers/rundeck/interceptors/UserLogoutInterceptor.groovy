package rundeck.interceptors

import rundeck.services.UserService


class UserLogoutInterceptor {

    UserService userService

    UserLogoutInterceptor(){
        match(controller: 'user', action: 'logout')
    }

    boolean before() {
        if(InterceptorHelper.matchesStaticAssets(controllerName, request)) return true
        userService.registerLogout(request.session.getAttribute('user'))
        return true
    }

    boolean after() {
        true
    }

    void afterView() {
        // no-op
    }
}
