package rundeck.interceptors

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.transform.PackageScope
import org.rundeck.web.infosec.AuthorizationRoleSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import webhooks.Webhook

import javax.security.auth.Subject
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest

class SetUserInterceptor {
    @Autowired
    ApplicationContext applicationContext

    UserService userService
    def messageSource
    int order = HIGHEST_PRECEDENCE + 30

    SetUserInterceptor() {
        matchAll().excludes(controller: 'user', action: '(logout|login|error|loggedout)')
                  //.excludes(controller: ~/$STATIC_ASSETS/)
        //The documentation seems to indicate that adding the above line should exclude
        // controllers matching that pattern but in practice it appears to
        //let them through, which is not desirable, so instead we do a manual match exclusion(see line 33)

    }
    boolean before() {
        if (InterceptorHelper.matchesStaticAssets(controllerName, request)) {
            return true
        }
        if (request.pathInfo == "/error") {
            //response.status = 200
            return true
        }
        if (request.api_version && request.remoteUser && !(grailsApplication.config.rundeck?.security?.apiCookieAccess?.enabled in ['true',true])){
            //disallow api access via normal login
            request.invalidApiAuthentication=true
            return false
        }
        if (request.remoteUser && session.user!=request.remoteUser) {
            session.user = request.remoteUser

            Subject subject=createAuthSubject(request)

            request.subject = subject
            session.subject = subject
        } else if(request.remoteUser && session.subject && grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in ['true',true]){
            // Preauthenticated mode is enabled, handle upstream role changes
            Subject subject = createAuthSubject(request)
            request.subject = subject
            session.subject = subject
        } else if(request.remoteUser && session.subject && grailsApplication.config.rundeck.security.authorization.preauthenticated.enabled in ['false',false]) {
            request.subject = session.subject
        } else if (request.api_version && !session.user ) {
            //allow authentication token to be used
            def authtoken = params.authtoken? Webhook.cleanAuthToken(params.authtoken) : request.getHeader('X-RunDeck-Auth-Token')
            boolean webhookType = controllerName == "webhook" && actionName == "post"
            String user = lookupToken(authtoken, servletContext, webhookType)
            List<String> roles = lookupTokenRoles(authtoken, servletContext)

            if (user){
                session.user = user
                request.authenticatedToken=authtoken
                request.authenticatedUser=user
                def subject = new Subject();
                subject.principals << new Username(user)

                roles.each{role->
                    subject.principals << new Group(role.trim());
                }

                request.subject = subject
                session.subject = subject
            }else{
                request.subject=null
                session.subject=null
                session.user=null
                if(authtoken){
                    request.invalidAuthToken = "Token:" + AuthToken.printable(authtoken)
                }
                request.authenticatedToken = null
                request.authenticatedUser = null
                request.invalidApiAuthentication = true
                if(authtoken){
                    log.error("Invalid API token used: ${AuthToken.printable(authtoken)}");
                }else{
                    log.error("Unauthenticated API request");
                }
            }
        } else if (!request.remoteUser) {
            //unauthenticated request to an action
            response.status = 403
            request.errorCode = 'request.authentication.required'
            render view: '/common/error.gsp'
            return false
        }
        def requiredRole = grailsApplication.config.rundeck.security.requiredRole
        if(!requiredRole.isEmpty()) {
            if(!request?.subject?.principals?.findAll { it instanceof Group }?.any { it.name == requiredRole }) {
                log.error("User ${request.remoteUser} must have role: ${requiredRole} to log in.")
                SecurityContextHolder.clearContext()
                request.logout()
                response.status = 403
                flash.loginerror = messageSource.getMessage("user.not.allowed",null,null)
                render view: '/user/login.gsp'
                return false
            }
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    private Subject createAuthSubject(HttpServletRequest request) {
        def principal = request.userPrincipal
        def subject = new Subject();
        subject.principals << new Username(principal.name)

        //find AuthorizationRoleSource instances
        Map<String,AuthorizationRoleSource> type = applicationContext.getBeansOfType(AuthorizationRoleSource)
        def roleset = new HashSet<String>(userService.getUserGroupSourcePluginRoles(principal.name))
        type.each {name,AuthorizationRoleSource source->
            if(source.enabled) {
                def roles = source.getUserRoles(principal.name, request)
                if(roles){
                    roleset.addAll(roles)
                    log.debug("Accepting user role list from bean ${name} for ${principal.name}: ${roles}")
                }else{
                    log.debug("Empty role list from bean ${name} for ${principal.name}")
                }
            }else {
                log.debug("Role source not enabled, bean ${name}")
            }
        }
        subject.principals.addAll(roleset.collect{new Group(it)})
        def user = userService.findOrCreateUser(principal.name)
        session.filterPref=UserService.parseKeyValuePref(user?.filterPref)

        subject
    }

    /**
     * Look up the given authToken and return the associated username, or null
     * @param authtoken
     * @param context
     * @return
     */
    @PackageScope
    String lookupToken(String authtoken, ServletContext context, boolean webhookType) {
        if(!authtoken){
            return null
        }
        if (context.getAttribute("TOKENS_FILE_PROPS")) {
            Properties tokens = (Properties) context.getAttribute("TOKENS_FILE_PROPS")
            if(log.traceEnabled) log.trace("checking static tokens: ${tokens}")
            if (tokens[authtoken]) {
                def userLine = tokens[authtoken]
                def user = userLine.toString().split(",")[0]
                log.debug("loginCheck found user ${user} via tokens file, token: ${AuthToken.printable(authtoken)}");
                return user
            }
        }

        AuthToken tokenobj = null
        if(webhookType) {
            tokenobj = AuthToken.findByTokenAndType(authtoken,AuthTokenType.WEBHOOK)
        } else {
            tokenobj = AuthToken.createCriteria().get {
                eq("token",authtoken)
                or {
                    eq("type", AuthTokenType.USER)
                    isNull("type")
                }
            }
        }

        if (tokenobj) {
            if (tokenobj.tokenIsExpired()) {
                log.debug("loginCheck token is expired ${tokenobj?.user}, ${tokenobj}");
                return null
            }
            User user = tokenobj?.user
            log.debug("loginCheck found user ${user.login} via DB, ${tokenobj}");
            return user.login
        }
        null
    }

    /**
     * Look up the given authToken and return the associated roles, or null
     * @param authtoken
     * @param context
     * @return
     */
    private List<String> lookupTokenRoles(String authtoken, ServletContext context) {
        if(!authtoken){
            return null
        }
        List<String> roles = []
        if (context.getAttribute("TOKENS_FILE_PROPS")) {
            Properties tokens = (Properties) context.getAttribute("TOKENS_FILE_PROPS")
            if (tokens[authtoken]) {
                def userLine = tokens[authtoken]
                if(userLine.toString().split(",").length>1){
                    roles = userLine.toString().split(",").drop(1) as List
                }
                log.debug("loginCheck found roles ${roles} via tokens file, token: ${AuthToken.printable(authtoken)}");
                return roles
            }
        }
        AuthToken tokenobj = authtoken ? AuthToken.findByToken(authtoken) : null
        if (tokenobj) {
            roles = tokenobj?.authRoles?.split(",") as List
            log.debug("loginCheck found roles ${roles} via DB, ${tokenobj}");
            return roles
        }
        null
    }

}
