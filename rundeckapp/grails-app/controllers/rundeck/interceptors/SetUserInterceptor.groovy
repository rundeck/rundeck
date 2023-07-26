package rundeck.interceptors

import com.dtolabs.rundeck.core.authentication.Group
import org.rundeck.app.authentication.Token
import com.dtolabs.rundeck.core.authentication.Username
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.rundeck.app.access.InterceptorHelper
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.AuthenticationToken.AuthTokenType
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import org.rundeck.web.infosec.AuthorizationRoleSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import webhooks.Webhook

import javax.security.auth.Subject
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import java.util.stream.Collectors
import java.util.stream.IntStream

class SetUserInterceptor {
    public static final String RUNNER_RQ_ATTRIB = "runnerRq"
    @Autowired
    ApplicationContext applicationContext
    InterceptorHelper interceptorHelper
    UserService userService
    ApiService apiService
    ConfigurationService configurationService

    int order = HIGHEST_PRECEDENCE + 30

    SetUserInterceptor() {
        matchAll().excludes(controller: 'user', action: '(logout|login|error|loggedout)')
                  //.excludes(controller: ~/$STATIC_ASSETS/)
        //The documentation seems to indicate that adding the above line should exclude
        // controllers matching that pattern but in practice it appears to
        //let them through, which is not desirable, so instead we do a manual match exclusion(see line 33)

    }
    boolean before() {
        if (interceptorHelper.matchesAllowedAsset(controllerName, request)) {
            return true
        }
        if (request.pathInfo?.startsWith("/error")) {
            return true
        }
        if (request.api_version &&
            request.remoteUser &&
            !(configurationService.getBoolean("security.apiCookieAccess.enabled",false))){
            //disallow api access via normal login
            request.invalidApiAuthentication=true
            return false
        }
        def authtoken = params.authtoken? Webhook.cleanAuthToken(params.authtoken) : request.getHeader('X-RunDeck-Auth-Token')

        if (request.userPrincipal && session.user!=request.userPrincipal.name) {
            session.user = request.userPrincipal.name

            Subject subject=createAuthSubject(request)

            request.subject = subject
            session.subject = subject
        } else if(request.remoteUser && session.subject &&
                configurationService.getBoolean("security.authorization.preauthenticated.enabled",false)){
            // Preauthenticated mode is enabled, handle upstream role changes
            Subject subject = createAuthSubject(request)
            request.subject = subject
            session.subject = subject
        } else if(request.remoteUser && session.subject &&
                !configurationService.getBoolean("security.authorization.preauthenticated.enabled",false)) {
            request.subject = session.subject
        } else if (request.api_version && !session.user && authtoken) {
            //allow authentication token to be used
            boolean webhookType = controllerName == "webhook" && actionName == "post"

            AuthenticationToken foundToken = lookupToken(authtoken, servletContext, webhookType)
            Set<String> roles = lookupTokenRoles(foundToken, servletContext)
            String user = foundToken?.getOwnerName()

            if (user){
                session.user = user
                request.authenticatedToken=authtoken
                request.authenticatedUser=user
                def subject = new Subject();
                subject.principals << new Username(user)

                roles.each{role->
                    subject.principals << new Group(role.trim());
                }
                if(foundToken){
                    subject.principals.add(new Token(foundToken.uuid, foundToken.type))
                }

                request.subject = subject
                session.subject = subject
            }else{
                request.subject=null
                session.subject=null
                session.user=null
                if(authtoken){
                    request.invalidAuthToken = "Token:" + AuthenticationToken.printable(authtoken)
                }
                request.authenticatedToken = null
                request.authenticatedUser = null
                request.invalidApiAuthentication = true
                if(authtoken){
                    log.error("Invalid API token used: ${AuthenticationToken.printable(authtoken)}");
                }else{
                    log.error("Unauthenticated API request");
                }
            }
        } else if (!request.remoteUser) {
            //unauthenticated request to an action
            if(request.api_version) {
                //api unauth response handled by AuthorizationInterceptor
                request.invalidApiAuthentication = true
            }else{
                response.status = 403
                request.errorCode = 'request.authentication.required'
                render view: '/common/error.gsp'
                return false
            }
        }
        def requiredRoles = getRequiredRolesFromProps()
        def allowedRoles = [] as List<String>
        if( requiredRoles.size() ){
            def requestRoles = request?.subject?.principals?.findAll { it instanceof Group } as List<Group>
            requiredRoles.forEach {
                requestRoles.forEach {group ->
                    if( it == group.getName() ){
                        allowedRoles << group
                    }
                }
            }
            if( !allowedRoles.size() ){
                log.error("User ${request.remoteUser} must have an allowed role to log in.")
                SecurityContextHolder.clearContext()
                request.logout()
                response.status = 403
                flash.loginErrorCode = 'user.not.allowed'
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
    @CompileStatic
    AuthenticationToken lookupToken(String authtoken, ServletContext context, boolean webhookType) {
        if(!authtoken){
            return null
        }
        if (context.getAttribute("TOKENS_FILE_PROPS")) {
            Properties tokens = (Properties) context.getAttribute("TOKENS_FILE_PROPS")
            if(log.traceEnabled) log.trace("checking static tokens: ${tokens}")
            if (tokens[authtoken]) {
                def userLine = tokens[authtoken]
                def user = userLine.toString().split(",")[0]

                // create token facade.
                AuthenticationToken fileToken = fileTokenAdapter(
                        user,
                        authtoken
                )

                log.debug("loginCheck found user ${fileToken.ownerName} via tokens file, token: ${fileToken.printableToken}");
                return fileToken
            }
        }

        AuthenticationToken tokenobj = null
        if(webhookType) {
            tokenobj = apiService.tokenLookupWithType(authtoken,AuthTokenType.WEBHOOK)
        } else if(request.getAttribute(RUNNER_RQ_ATTRIB)) {
            tokenobj = apiService.tokenLookupWithType(authtoken, AuthTokenType.RUNNER)
        } else {
            tokenobj = apiService.tokenLookup(authtoken)
        }

        if (tokenobj) {
            if (AuthenticationToken.tokenIsExpired(tokenobj)) {
                log.debug("loginCheck token is expired ${tokenobj?.getOwnerName()}, ${tokenobj}");
                return null
            }
            log.debug("loginCheck found user ${tokenobj?.getOwnerName()} via DB, ${tokenobj}");
            return tokenobj
        }
        null
    }

    /**
     * Look up the given authToken and return the associated roles, or null
     * @param authtoken
     * @param context
     * @return
     */
    @CompileStatic
    private Set<String> lookupTokenRoles(AuthenticationToken authtoken, ServletContext context) {
        if(!authtoken){
            return null
        }
        if (context.getAttribute("TOKENS_FILE_PROPS")) {
            Properties tokens = (Properties) context.getAttribute("TOKENS_FILE_PROPS")
            if (tokens[authtoken.getToken()]) {
                List<String> roles = []
                def userLine = tokens[authtoken.getToken()]
                if(userLine.toString().split(",").length>1){
                    roles = userLine.toString().split(",").drop(1) as List
                }
                log.debug("loginCheck found roles ${roles} via tokens file, token: ${authtoken.printableToken}");
                return roles.stream().collect(Collectors.toSet());
            }
        }

        Set<String> tokenRoles = authtoken.getAuthRolesSet()
        if (tokenRoles && !tokenRoles.isEmpty()) {
            log.debug("loginCheck found roles ${tokenRoles} via DB, ${authtoken}");
            return tokenRoles
        }
        null
    }

    /**
     * Creates a token facade to manage tokens loaded from properties.
     * @param uuid UUID for the token
     * @param owner Owner username
     * @param token Token value
     * @return Token interface adapter.
     */
    private static AuthenticationToken fileTokenAdapter(
            final String owner,
            final String token
    ) {
        return new SimpleTokenBuilder()
        .setToken(token)
        .setUuid(token)
        .setCreator(owner)
        .setOwnerName(owner)
    }

    /**
     * Get the required roles to authenticate with Rundeck's Server
     * from properties if there's any, and return a list with them.
     *
     * */
    @CompileStatic
    private List<String> getRequiredRolesFromProps(){
        def rolesFromProps = [] as List<String>
        def requiredSingularRole = configurationService.getString("security.requiredRole","")
        if( requiredSingularRole ){
            rolesFromProps << requiredSingularRole
        }
        def requiredPluralRoles = configurationService.getString("security.requiredRoles","")
        if( requiredPluralRoles ){
            List<String> allowedHostnames = requiredPluralRoles.split(",").collect( it -> it.trim())
            allowedHostnames.stream().filter {position -> !position.isEmpty()}
            .forEach {rolesFromProps << it}
        }
        return rolesFromProps
    }

}
