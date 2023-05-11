/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.controllers

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.converters.JSON
import grails.core.GrailsApplication
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.app.api.model.ApiErrorResponse
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.user.LoginStatus
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.app.data.providers.v1.TokenDataProvider
import org.rundeck.app.data.providers.v1.UserDataProvider
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeApplicationType
import org.rundeck.util.Sizes
import rundeck.Execution
import rundeck.commandObjects.RdUserCommandObject
import rundeck.data.paging.RdPageable
import rundeck.services.UserService

import javax.servlet.http.HttpServletResponse

@Controller
class UserController extends ControllerBase{

    private static final int DEFAULT_USER_PAGE_SIZE = 100
    private static final int DEFAULT_TOKEN_PAGE_SIZE = 50

    UserService userService
    GrailsApplication grailsApplication
    def configurationService
    TokenDataProvider tokenDataProvider
    UserDataProvider userDataProvider

    static allowedMethods = [
            addFilterPref      : 'POST',
            store              : 'POST',
            update             : 'POST',
            clearApiToken      : 'POST',
            generateUserToken  : 'POST',
            renderUsertoken    : 'POST',
            removeExpiredTokens: 'POST',
            apiUserData        : ['GET','POST'],
            apiUserList        : 'GET'
    ]

    def index = {
        redirect(action:"login")
    }

    def error() {
        if(!flash.loginErrorCode){
            flash.loginErrorCode = 'invalid.username.and.password'
        }
        return render(view:'login')
    }

    def logout = {
        session.invalidate()
        return redirect(action: 'loggedout')
    }

    def loggedout(){
        if(configurationService.getBoolean("security.authorization.preauthenticated.redirectLogout", false)) {
            final URI redirectUrl = new URI(configurationService.getString("security.authorization.preauthenticated.redirectUrl"))
            if (redirectUrl.isAbsolute()) {
                return redirect(url: redirectUrl)
            } else {
                return redirect(url: grailsApplication.config.getProperty("grails.serverURL", String.class) + redirectUrl)
            }
        }
    }

    def login = {
        if (session.user) {
            redirect(controller:'menu', action:'index')
        }
    }

    def denied={
        response.setStatus(403)
        renderErrorView('Access denied')
    }
    def deniedFragment={
        response.setStatus(403)
        renderErrorFragment('Access denied')
    }

    @RdAuthorizeApplicationType(
        type = AuthConstants.TYPE_USER,
        access = RundeckAccess.General.AUTH_APP_ADMIN
    )
    def list(){
        [users:userDataProvider.listAllOrderByLogin()]
    }

    def profile() {
        //check auth to view profile
        //default to current user profile
        if (!params.login) {
            params.login = session.user
        }
        def authorizingAppType = getAuthorizingApplicationType(AuthConstants.TYPE_USER)
        if(params.login != session.user){
            authorizingAppType.authorize(RundeckAccess.General.APP_ADMIN)
        }
        boolean tokenAdmin = authorizingAppType.isAuthorized(RundeckAccess.General.APP_ADMIN)
        def authContext = authorizingAppType.authContext

        def userExists = userService.validateUserExists(params.login)
        if (!userExists && params.login == session.user) {
            //redirect to profile edit page, so user can setup their profile
            flash.message = "Please fill out your profile"
            return redirect(action: 'register')
        }
        if (notFoundResponse(userExists, 'User', params['login'])) {
            return
        }

        RdUser u = userService.findOrCreateUser(params.login)
        def tokenTotal = tokenAdmin ? tokenDataProvider.countTokensByType(AuthenticationToken.AuthTokenType.USER) :
                tokenDataProvider.countTokensByCreatorAndType(u.login, AuthenticationToken.AuthTokenType.USER)

        int max = (params.max && params.max.isInteger()) ? params.max.toInteger() :
                configurationService.getInteger(
                        "gui.user.profile.paginatetoken.max.per.page",
                        DEFAULT_TOKEN_PAGE_SIZE)

        int offset = (params.offset && params.offset.isInteger()) ? params.offset.toInteger() : 0

        if(offset >= tokenTotal) {
            def diff = (tokenTotal % max)
            if( diff == 0 && tokenTotal > 0) {
                diff = max
            }
            offset = tokenTotal - diff
        }

        def pageable = new RdPageable(offset: offset, max: max).withOrder("dateCreated","desc")
        def tokenList = tokenAdmin ? tokenDataProvider.findAllTokensByType(AuthenticationToken.AuthTokenType.USER, pageable) :
                tokenDataProvider.findAllUserTokensByCreator(u.login, pageable)

        params.max = max
        params.offset = offset

        [
                user              : u,
                authRoles         : authContext.getRoles(),
                tokenMaxExpiration: apiService.maxTokenDurationConfig(),
                tokenAdmin        : tokenAdmin,
                tokenList         : tokenList,
                tokenTotal        : tokenTotal,
                max               : max,
                offset            : offset
        ]
    }
    def create={
        render(view:'register',model:[user: userDataProvider.buildUser(),newuser:true])
    }
    def register(){
        UserAndRolesAuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def user = params.login?:auth.username
        if (user != auth.username) {
            if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                    auth,
                    AuthConstants.RESOURCE_TYPE_SYSTEM,
                    [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
                ),
                AuthConstants.ACTION_ADMIN,
                'user',
                user
            )) {
                return
            }
        }
        def userExists = userDataProvider.validateUserExists(user)
        if(userExists){
           return redirect(action:'edit')
        }
        def u = userDataProvider.buildUser(login: user)

        def model=[user: u,newRegistration:true]
        return model
    }
    public def store(RdUserCommandObject user){
        withForm{
        if(user.hasErrors()){
            flash.errors=user.errors
            return render(view: 'edit', model: [user: user,newuser:params.newuser])
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(
            user.login == session.user ||
            rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_USER,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
            ), AuthConstants.ACTION_ADMIN, 'Users',
                user.login)) {
            return
        }

        def userExists = userDataProvider.validateUserExists(user.login)
        if(userExists){
            request.errorCode = 'api.error.item.alreadyexists'
            request.errorArgs = ['User profile', user.login]
            return renderErrorView([:])
        }
        def createdUser = userDataProvider.createUserWithProfile(user.login, user.lastName, user.firstName, user.email)

        if(!createdUser.isSaved){
            flash.error = "Error updating user"
            flash.errors = createdUser.errors
            return render(view:'edit',model:[user:createdUser.user])
        }
        flash.message="User profile updated: ${user.login}"
        return redirect(action:'profile',params:[login: user.login])
        }.invalidToken {
            flash.error = g.message(code: 'request.error.invalidtoken.message')
            return render(view: 'edit', model: [user: user])
        }
    }

    @Post(uris=['/user/info/{username}'])
    @Operation(
        method='POST',
        summary='Modify user profile',
        description='''Modify the user profile data for another user.

Authorization required: `app_admin` for `system` resource, if not the current user.

Since: v21''',
        tags = ['user'],
        parameters=[
            @Parameter(
                name = 'username',
                in = ParameterIn.PATH,
                required = true,
                description = 'Username, for a different user',
                schema = @Schema(type = 'string')
            )
        ],
        requestBody = @RequestBody(
          description='Request content',
            content=@Content(
                mediaType=MediaType.APPLICATION_JSON,
                schema=@Schema(type='object'),
                examples = @ExampleObject('''{
    "firstName":"Name",
    "lastName":"LastName",
    "email":"user@server.com"
}''')
            )
        ),
        responses=[
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/403'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/404'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/200'
            )
        ]

    )
    protected def apiOtherUserDataPost_docs(){}

    @Post(uris=['/user/info'])
    @Operation(
        method='POST',
        summary='Modify user profile',
        description='''Modify the user profile data for current user.

Since: v21''',
        tags = ['user'],
        requestBody = @RequestBody(
          description='Request content',
            content=@Content(
                mediaType=MediaType.APPLICATION_JSON,
                schema=@Schema(type='object'),
                examples = @ExampleObject('''{
    "firstName":"Name",
    "lastName":"LastName",
    "email":"user@server.com"
}''')
            )
        ),
        responses=[
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/403'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/404'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/200'
            )
        ]

    )
    protected def apiUserDataPost_docs(){}

    @Get(uris=['/user/info'])
    @Operation(
        method='GET',
        summary='Get User Profile',
        description='''Get the user profile data for current user.

Since: v21''',
        tags=['user'],
        responses=[
            @ApiResponse(responseCode='403',description = 'Unauthorized',content=@Content(mediaType=MediaType.APPLICATION_JSON,schema=@Schema(implementation = ApiErrorResponse))),
            @ApiResponse(responseCode='404',description = 'Not found',content=@Content(mediaType=MediaType.APPLICATION_JSON,schema=@Schema(implementation = ApiErrorResponse))),
            @ApiResponse(responseCode='200',description = 'User Profile Data',
                content=@Content(
                    mediaType=MediaType.APPLICATION_JSON,schema=@Schema(type='object'),
                    examples = @ExampleObject('''{
  "login": "username",
  "firstName": "first name",
  "lastName": "last name",
  "email": "email@domain"
}''')
                )
            )
        ]

    )
    protected def apiUserData_docs(){}

    @Get(uris=['/user/info/{username}'])
    @Operation(
        method='GET',
        summary='Get User Profile',
        description='''Get the user profile data for another user.

Authorization required: `app_admin` for `system` resource, if not the current user.

Since: v21''',
        tags=['user'],
        parameters=[
            @Parameter(
                name = 'username',
                in = ParameterIn.PATH,
                required = true,
                description = 'Username, for a different user',
                schema = @Schema(type = 'string')
            )
        ],
        responses=[
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/403'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/404'
            ),
            @ApiResponse(
                ref = '#/paths/~1user~1info/get/responses/200'
            )
        ]
    )
    def apiUserData(){
        if (!apiService.requireVersion(request, response, ApiVersions.V21)) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'])
        UserAndRolesAuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def username = params.username?:auth.username
        if(username!=auth.username){
            //requiere admin privileges
            if(!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                    auth,
                    AuthConstants.RESOURCE_TYPE_SYSTEM,
                    [ AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
            )){
                def errorMap= [status: HttpServletResponse.SC_FORBIDDEN, code: 'request.error.unauthorized.message', args: ['get info','from other','User.']]

                withFormat {
                    json {
                        return apiService.renderErrorJson(response, errorMap)
                    }
                    xml {
                        return apiService.renderErrorXml(response, errorMap)
                    }
                    '*' {
                        return apiService.renderErrorXml(response, errorMap)
                    }
                }
                return
            }
        }
        def userExists = userService.validateUserExists(username)
        if(!userExists){
            def errorMap= [status: HttpServletResponse.SC_NOT_FOUND, code: 'request.error.notfound.message', args: ['User',username]]
            withFormat {
                xml {
                    return apiService.renderErrorXml(response, errorMap)
                }
                json {
                    return apiService.renderErrorJson(response, errorMap)
                }
                '*' {
                    return apiService.renderErrorXml(response, errorMap)
                }
            }
            return
        }
        RdUser u = userDataProvider.findByLogin(username)
        if (request.method == 'POST'){
            def config
            def succeed = apiService.parseJsonXmlWith(request, response, [
                    xml: { xml ->
                        config = [:]
                            config.email=xml?.email?.text()
                            config.firstName=xml?.firstName?.text()
                            config.lastName=xml?.lastName?.text()
                    },
                    json: { json ->
                        config = json
                    }
            ])
            if(!succeed){
                return
            }
            String lastName = (config.containsKey("lastName")) ? config.lastName : u.lastName
            String firstName = (config.containsKey("firstName")) ? config.firstName : u.firstName
            String email = (config.containsKey("email")) ? config.email : u.email
            def updateResponse = userDataProvider.updateUserProfile(username, lastName, firstName, email)
            if(!updateResponse.isSaved){
                def errorMsg= updateResponse.errors.allErrors.collect { g.message(error:it) }.join(";")
                return apiService.renderErrorFormat(response,[
                        status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        message:errorMsg,
                        format:respFormat
                ])
            }
        }
        withFormat {
            def xmlClosure = {
                delegate.'user' {
                    login(u.login)
                    firstName(u.firstName)
                    lastName(u.lastName)
                    email(u.email)
                }
            }
            xml {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
            json {
                return apiService.renderSuccessJson(response) {
                    delegate.login=u.login
                    delegate.firstName=u.firstName
                    delegate.lastName=u.lastName
                    delegate.email=u.email
                }
            }
            '*' {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
        }
    }

    @Get(uri='/user/roles')
    @Operation(
        method = 'GET',
        summary = 'List Authorized Roles',
        description = '''Get a list of the authenticated user's roles.

Since: v30''',
        tags = ['user', 'authorization'],
        responses = [
            @ApiResponse(
                responseCode = '200',
                description = '''Success response, with a list of roles.''',
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = 'object'),
                    examples = @ExampleObject('''{
    "roles":["admin","user"]
}''')
                )
            )
        ]
    )
    def apiListRoles() {
        if (!apiService.requireVersion(request, response, ApiVersions.V30)) {
            return
        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        withFormat {
            def xmlClosure = {
                delegate.'roles' {
                    authContext.getRoles().each { role ->
                        delegate.'role'(role)
                    }
                }
            }
            xml {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
            json {
                return apiService.renderSuccessJson(response) {
                    delegate.roles=authContext.getRoles()
                }
            }
            '*' {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
        }
    }

    @Get(uri='/user/list')
    @Operation(
        method = 'GET',
        summary = 'List users',
        description = '''Get a list of all the users.

Authorization required: `app_admin` for `system` resource

Since: v21''',
        tags = ['user'],
        responses = [
            @ApiResponse(
                responseCode = '200',
                description = '''Success Response, with a list of users.

For APIv27+, the results will contain additional fields:
* `created` creation date
* `updated` updated date
* `lastJob` last job execution
* `tokens` number of API tokens
''',
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(type = 'object')),
                    examples = @ExampleObject('''[{
    "login":"user",
    "firstName":"Name",
    "lastName":"LastName",
    "email":"user@server.com",
    "created": "2017-10-01T09:00:20Z",
    "updated": "2018-08-24T13:53:02Z",
    "lastJob": "2018-08-28T13:31:00Z",
    "tokens": 1
},
{
    "login":"admin",
    "firstName":"Admin",
    "lastName":"Admin",
    "email":"admin@server.com",
    "created": "2016-07-17T18:42:00Z",
    "updated": "2018-08-24T13:53:00Z",
    "lastJob": "2018-08-28T13:31:00Z",
    "tokens": 6
}]''')
                )
            )
        ]
    )
    def apiUserList(){
        if (!apiService.requireVersion(request, response, ApiVersions.V21)) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'])
        UserAndRolesAuthContext auth = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        if(!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                AuthConstants.RESOURCE_TYPE_SYSTEM,
                [ AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        )){
            def errorMap= [status: HttpServletResponse.SC_FORBIDDEN, code: 'request.error.unauthorized.message', args: ['get info','from other','User.']]

            withFormat {
                json {
                    return apiService.renderErrorJson(response, errorMap)
                }
                xml {
                    return apiService.renderErrorXml(response, errorMap)
                }
                '*' {
                    return apiService.renderErrorXml(response, errorMap)
                }
            }
            return
        }
        def users = []
        if(request.api_version >= ApiVersions.V27){
            def userList = [:]
            userDataProvider.listAllOrderByLogin().each {
                def obj = [:]
                obj.login = it.login
                obj.firstName = it.firstName
                obj.lastName = it.lastName
                obj.email = it.email
                obj.created = it.dateCreated
                obj.updated = it.lastUpdated
                def lastExec = Execution.lastExecutionByUser(it.login).list()
                if(lastExec?.size()>0){
                    obj.lastJob = lastExec.get(0).dateStarted
                }
                def tokenList = tokenDataProvider.findAllByUser(it.id.toString())
                obj.tokens = tokenList?.size()
                userList.put(it.login, obj)
            }
            userList.each{k,v -> users<<v}
        }else{
            users = userDataProvider.findAll()
        }


        withFormat {
            def xmlClosure = {
                    users.each { u ->
                        delegate.'user' {
                            login(u.login)
                            firstName(u.firstName)
                            lastName(u.lastName)
                            email(u.email)
                            if(request.api_version >= ApiVersions.V27){
                                created(u.created)
                                updated(u.updated)
                                lastJob(u.lastJob)
                                tokens(u.tokens)
                            }
                        }
                    }
            }
            xml {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
            json {
                return apiService.renderSuccessJson(response) {
                    users.each {
                        def u
                        if(request.api_version >= ApiVersions.V27){
                            u = it
                        }else{
                            u = [login: it.login, firstName: it.firstName, lastName: it.lastName, email: it.email]
                        }
                        element(u)
                    }
                }
            }
            '*' {
                return apiService.renderSuccessXml(request, response, xmlClosure)
            }
        }

    }

    def loadUsersList() {
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_USER,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
            ),
                AuthConstants.ACTION_ADMIN, 'User', 'accounts'
        )) {
            return
        }

        boolean loggedOnly = params.getBoolean('loggedOnly', false)
        boolean includeExec = params.getBoolean('includeExec', false)

        def filters = [:]
        if (params.loginFilter && !params.loginFilter.trim().isEmpty()) {
            filters.login = params.loginFilter.trim()
        }
        if (userService.isSessionIdRegisterEnabled()) {
            if (params.sessionFilter && !params.sessionFilter.trim().isEmpty()) {
                filters.lastSessionId = params.sessionFilter.trim()
            }
        }
        if (params.hostNameFilter && !params.hostNameFilter.trim().isEmpty()) {
            filters.lastLoggedHostName = params.hostNameFilter.trim()
        }
        def offset = params.getInt('offset', 0)

        int max = configurationService.getInteger(
                "gui.user.summary.max.per.page",
                DEFAULT_USER_PAGE_SIZE
        )

        def result = userService.findWithFilters(loggedOnly, filters, offset, max)

        def userList = []
        result.users.each {
            def obj = [:]
            obj.login = it.login
            obj.firstName = it.firstName
            obj.lastName = it.lastName
            obj.email = it.email
            obj.created = it.dateCreated
            obj.updated = it.lastUpdated
            if(includeExec){
                def lastExec = Execution.lastExecutionDateByUser(it.login).get()
                if (lastExec) {
                    obj.lastJob = lastExec
                }
            }
            obj.tokens = tokenDataProvider.countTokensByUser(it.id.toString())
            obj.loggedStatus = userService.getLoginStatus(it)
            obj.lastHostName = it.lastLoggedHostName
            if (userService.isSessionIdRegisterEnabled()) {
                obj.lastSessionId = it.lastSessionId
            }
            obj.loggedInTime = it.lastLogin
            if (result.showLoginStatus && loggedOnly && obj.loggedStatus.equals(LoginStatus.LOGGEDIN.value)) {
                userList.add(obj)
            } else {
                userList.add(obj)
            }
        }
        render(
                contentType: 'application/json', text:
                (
                        [
                                users           : userList,
                                totalRecords    : result.totalRecords,
                                offset          : offset,
                                maxRows         : max,
                                sessionIdEnabled: userService.isSessionIdRegisterEnabled(),
                                showLoginStatus : result.showLoginStatus
                        ]
                ) as JSON
        )
    }

    def getSummaryPageConfig(){
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_USER,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
            ),
                AuthConstants.ACTION_ADMIN, 'User', 'accounts'
        )) {
            return
        }
        def result = userService.getSummaryPageConfig()
        render(
                contentType: 'application/json', text:
                (
                        [
                                loggedOnly      : result.loggedOnly,
                                showLoginStatus : result.showLoginStatus
                        ]
                ) as JSON
        )
    }

    public def update (RdUserCommandObject user) {
        withForm{
        if (user.hasErrors()) {
            flash.errors = user.errors
            return render(view: 'edit', model: [user: user])
        }
        //check auth to view profile
        //default to current user profile
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if(unauthorizedResponse(params.login == session.user || rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            AuthConstants.RESOURCE_TYPE_USER,
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        ), AuthConstants.ACTION_ADMIN,'User',params.login)){
            return
        }
        RdUser u = userDataProvider.findByLogin(params.login)
        if(notFoundResponse(u,'User',params.login)){
            return
        }

        def updateResponse = userDataProvider.updateUserProfile(params.login, params.lastName, params.firstName, params.email)

        if(!updateResponse.isSaved){
            request.error = "Error updating user"
            request.errors = updateResponse.errors
            return render(view:'edit',model:[user:u])
        }
        flash.message="User profile updated: ${params.login}"
        return redirect(action:'profile',params:[login:params.login])
        }.invalidToken {
            flash.error = g.message(code: 'request.error.invalidtoken.message')
            return render(view: 'edit', model: [user: user])
        }
    }

    def cancel(){
        return redirect(action:'profile')
    }
    def edit(){
        def model=profile()
        return model
    }



    def generateUserToken(
            String tokenTime,
            String tokenTimeUnit,
            String tokenUser,
            String tokenRoles,
            String tokenName) {
        boolean valid=false

        withForm{
            valid=true
            g.refreshFormTokensHeader()
        }.invalidToken{
            response.status=HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        def tokenTimeString = null
        if (tokenTime && tokenTimeUnit) {
            tokenTimeString = tokenTime + tokenTimeUnit
        }

        if (tokenTimeString && !Sizes.validTimeDuration(tokenTimeString)) {
            def result = [result: false, error: "Duration format was not valid"]
            return renderTokenGenerateResult(result, params.login)
        }
        Integer tokenDurationSeconds = tokenTimeString ? Sizes.parseTimeDuration(tokenTimeString) : 0
        //check auth to edit profile
        //default to current user profile
        def result = [:]
        try {
            AuthenticationToken token = apiService.generateUserToken(
                    authContext,
                    tokenDurationSeconds,
                    tokenUser ?: params.login,
                    AuthenticationToken.parseAuthRoles(tokenRoles),
                    true,
                    AuthenticationToken.AuthTokenType.USER,
                    tokenName
            )
            result = [result: true, apitoken: token.clearToken, tokenid: token.uuid]
        } catch (Exception e) {
            e.printStackTrace()
            result = [result: false, error: e.getCause()?.message ?: e.message]
        }
        return renderTokenGenerateResult(result, params.login)
    }

    def renderUsertoken(String tokenid) {
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        boolean valid = false

        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        def adminAuth = apiService.hasTokenAdminAuth(authContext)

        try {
            AuthenticationToken token = adminAuth ?
                    apiService.findTokenId(tokenid) :
                    apiService.findUserTokenId(authContext.username, tokenid)
            if (!token) {
                renderTokenGenerateResult([result: false, error: 'Not Found'], authContext.username)
            }
            renderTokenGenerateResult([result: true, apitoken: token.token, tokenid: token.uuid], authContext.username)
        } catch (Exception e) {
            renderTokenGenerateResult([result: false, error: e.message], authContext.username)
        }
    }

    private Map renderTokenGenerateResult(Map result, String login) {
        if (result.error) {
            log.error result.error
        }
        withFormat {
            html {
                flash.error = result.error
                redirect(controller: 'user', action: 'profile', params: [login: login])
            }
            json {
                render(result as JSON)
            }
        }
    }

    def renderApiToken() {
        def login = params.login
        def result
        def user
        def token
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (!adminAuth && login != authContext.username) {
            def error = "Unauthorized: retrieve token for ${login}"
            log.error error
            return renderErrorFragment(error)
        }
        user = userService.findOrCreateUser(login)
        if (!user) {
            def error = "Couldn't find user: ${login}"
            log.error error
            result = [result: false, error: error]
        } else {
            def t = adminAuth ?
                    (apiService.findTokenId(params.tokenid)) :
                    apiService.findUserTokenId(login, params.tokenid);
            if (t) {
                token = t
            } else {
                def error = "Couldn't find user auth token: ${params.token}"
                log.error error
                result = [result: false, error: error]
            }
        }
        if(user && token){
            return render(template: 'token',model: [user:user,token:token]);
        }else{
            return renderErrorFragment(result.error)
        }

    }

    def removeExpiredTokens() {
        boolean valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        def login = params.login
        def deleteall = params.deleteall?.toBoolean()
        def result
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (unauthorizedResponse(
                login == authContext.username || adminAuth,
                AuthConstants.ACTION_ADMIN,
                'API Tokens for user: ',
                params.login
        )) {
            return
        }
        if (unauthorizedResponse(
                !deleteall || adminAuth,
                AuthConstants.ACTION_ADMIN,
                'API Tokens for ',
                "All users"
        )) {
            return
        }

        def total = deleteall ? apiService.removeAllExpiredTokens() : apiService.removeAllExpiredTokens(login)

        if (total > 0) {
            flash.message = "Removed $total expired tokens"
        } else {
            flash.error = "No expired tokens found"
        }
        def done = false
        withFormat {
            html {

            }
            json {
                done = true
                render(result as JSON)
            }
        }
        if (done) {
            return
        }

        def redirParams = [login: login]
        if (params.tokenPagingMax) {
            redirParams.max = params.tokenPagingMax
        }
        if (params.tokenPagingOffset) {
            redirParams.offset = params.tokenPagingOffset
        }

        return redirect(controller: 'user', action: 'profile', params: redirParams)
    }
    def clearApiToken(RdUserCommandObject user) {
        boolean valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: request.error] as JSON)
                }
            }
        }
        if (!valid) {
            return
        }
        if (user.hasErrors()) {
            request.errors = user.errors
            withFormat {
                html {
                    return render(view: 'profile', model: [user: user])
                }
                json {
                    render([error: 'Invalid input'] as JSON)
                }
            }

        }
        def login = user.login
        def result
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)
        if (!params.token && !params.tokenid) {
            def error = "Parameter tokenid required"
            log.error error
            result = [result: false, error: error]
        } else if (params.No == 'No') {
            return redirect(controller: 'user', action: 'profile', params: [login: login])
        } else if (!adminAuth && login != authContext.username) {
            def error = "Unauthorized: admin role required"
            log.error error
            result = [result: false, error: error]
        } else {

            def userExists = userService.validateUserExists(login)
            if (!userExists) {
                def error = "Couldn't find user: ${login}"
                log.error error
                result=[result: false, error: error]
            }else{
                def findtoken = params.tokenid ?: params.token
                AuthenticationToken found = null
                if (adminAuth) {
                    //admin can delete any token
                    found = params.token ?
                            tokenDataProvider.tokenLookup(params.token) :
                            apiService.findTokenId(params.tokenid)
                } else {
                    //users can delete owned token
                    found = params.token ?
                            apiService.findByTokenAndCreator(params.token, login) :
                            apiService.findUserTokenId(login, params.tokenid)
                }

                if(!found){
                    def error = "Couldn't find token ${findtoken} for user ${login}"
                    log.error error
                    result = [result: false, error: error]
                } else {
                    apiService.removeToken(found)
                    result = [result: true]
                }
            }
        }
        def done=false
        withFormat {
            html{

            }
            json{
                done=true
                render(result as JSON)
            }
        }
        if(done){
            return
        }

        if (result.error) {
            flash.error = result.error
        }
        return redirect(controller: 'user', action: 'profile', params: [
                login: login,
                max: params.tokenPagingMax,
                offset: params.tokenPagingOffset
        ])

    }
    def setDashboardPref={
        def RdUser u = userService.findOrCreateUser(session.user)
        def list=params.dpref.split(",").collect{Integer.parseInt(it)}
        (1..4).each{
            if(!list.contains(it)){
                return renderErrorView("Invalid pref order: ${params.dpref}")
            }
        }
        u.dashboardPref=params.dpref
        u.save()

        render(contentType:"application/json"){
            delegate.result "success"
            delegate.dashboard u.dashboardPref
        }
    }

    def addFilterPref={
        withForm{
            def result = userService.storeFilterPref(session.user,params.filterpref)
            if(result.error){
                return renderErrorFragment(result.error)
            }
            session.filterPref=userService.parseKeyValuePref(result.user?.filterPref)
            def storedpref=result.storedpref

            //include new request tokens as headers in response
            g.refreshFormTokensHeader()

            render(contentType:"application/json"){
                delegate.result "success"
                delegate.filterpref storedpref
            }

        }.invalidToken{
            response.status= HttpServletResponse.SC_BAD_REQUEST
            render(contentType: "application/json") {
                delegate.result  "error"
                delegate.message g.message(code:'request.error.invalidtoken.message')
            }
        }
    }

}
