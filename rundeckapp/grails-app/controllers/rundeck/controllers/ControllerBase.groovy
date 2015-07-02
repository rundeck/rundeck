package rundeck.controllers

import org.rundeck.web.infosec.HMacSynchronizerTokensHolder
import org.codehaus.groovy.grails.web.metaclass.InvalidResponseHandler
import org.codehaus.groovy.grails.web.metaclass.ValidResponseHandler
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.mvc.TokenResponseHandler
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.zip.GZIPOutputStream

/**
 * Mixin utility for controllers
 * @author greg
 * @since 2014-03-12
 */
class ControllerBase {
    def grailsApplication

    protected def withHmacToken(Closure valid){
        GrailsWebRequest request= (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        TokenResponseHandler handler
        if(isTokenValid(request)){
            resetToken(request)
            handler = new ValidResponseHandler(valid?.call())
        } else {
            handler = new InvalidResponseHandler()
        }

        request.request.setAttribute(TokenResponseHandler.KEY, handler)
        return handler
    }

    def resetToken(GrailsWebRequest request) {
        HMacSynchronizerTokensHolder holder = request.currentRequest.getSession(false)?.getAttribute(HMacSynchronizerTokensHolder.HOLDER)
        String tokenInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return

        holder.resetToken(tokenInRequest)
    }

    boolean isTokenValid(GrailsWebRequest request) {
        HMacSynchronizerTokensHolder holder = request.currentRequest.getSession(false)?.getAttribute(HMacSynchronizerTokensHolder.HOLDER)
        if (!holder) return false

        String tokenInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return false

        String timestampInRequest = request.params[HMacSynchronizerTokensHolder.TOKEN_TIMESTAMP]
        if (!timestampInRequest) return false

        long timestamp=0
        try{
            timestamp=Long.parseLong(timestampInRequest)
        }catch (NumberFormatException e){
            return false
        }

        try {
            return holder.isValid(timestamp, tokenInRequest)
        }
        catch (IllegalArgumentException) {
            return false
        }
    }
    def renderCompressed(HttpServletRequest request,HttpServletResponse response,String contentType, data){
        if(grailsApplication.config.rundeck?.ajax?.compression=='gzip'
                && request.getHeader("Accept-Encoding").contains("gzip")){
            response.setHeader("Content-Encoding","x-gzip")
            response.setHeader("Content-Type",contentType)
            def stream = new GZIPOutputStream(response.outputStream)
            stream.withWriter("UTF-8"){ it << data }
            stream.close()
        }else{
            return render(contentType:contentType,text:data)
        }
    }
/**
     * Send a Not Found response unless the test passes, return true if the response was sent.
     * @param test exists test
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected def notFoundResponse(Object test, String type, String name, boolean fragment = false) {
        if (!test) {
            renderNotfound(type, name, fragment)
        }
        return !test
    }

    /**
     * Send a Not Found response
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected void renderNotfound(String type, String name, boolean fragment = false) {
        request.errorCode = 'request.error.notfound.message'
        request.errorArgs = [type, name]
        response.status = HttpServletResponse.SC_NOT_FOUND
        request.titleCode = 'request.error.notfound.title'
        if (fragment) {
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }
    /**
     * Send an Unauthoried error response unless the test passes, return true if the response was sent.
     * @param test authorization test
     * @param action authorization action
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     * @return true if response was committed
     */
    protected def unauthorizedResponse(Object test, String action, String type, Object name = '',
                                       boolean fragment = false) {
        if (!test) {
            renderUnauthorized(action, type, name, fragment)
        }
        return !test
    }

    /**
     * Send an Unauthoried error response
     * @param action authorization action
     * @param type object type
     * @param name object name
     * @param fragment if true, render only the error message content, otherwise render a view
     */
    protected void renderUnauthorized(String action, String type, Object name, boolean fragment = false) {
        request.errorCode = 'request.error.unauthorized.message'
        request.errorArgs = [action, type, name]
        response.status = HttpServletResponse.SC_FORBIDDEN
        request.titleCode = 'request.error.unauthorized.title'
        if (fragment) {
            response.addHeader("X-Rundeck-Error-Message", g.message(code:request.errorCode,args:request.errorArgs))
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }
    /**
     * Send an Unauthoried error response
     * @param message message text
     * @param fragment if true, render only the error message content, otherwise render a view
     */
    protected void renderUnauthorized(String message, boolean fragment = false) {
        request.errorMessage = message
        response.status = HttpServletResponse.SC_FORBIDDEN
        request.titleCode = 'request.error.unauthorized.title'
        if (fragment) {
            response.addHeader("X-Rundeck-Error-Message", message)
            renderErrorFragment([:])
        } else {
            renderErrorView([:])
        }
    }

    /**
     * Send an error response view
     * @param message message text
     */
    protected def renderErrorView(String message) {
        request.errorMessage = message
        render(view: "/common/error")
    }
    /**
     * Send an error response
     * @param model error data
     */
    protected def renderErrorView(Map model) {
        render(view: "/common/error", model: model)
    }
    /**
     * Send an error response fragment
     * @param message message text
     */
    protected def renderErrorFragment(String message) {
        request.errorMessage = message
        render(template: "/common/errorFragment")
    }
    /**
     * Send an error response fragment
     * @param model data model
     */
    protected def renderErrorFragment(Map model) {
        render(template: "/common/errorFragment",model:model)
    }
}
