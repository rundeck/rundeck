package rundeck.controllers

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authorization.UserAndRoles
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.app.type.AuthorizingAppType
import org.rundeck.core.auth.web.RdAuthorizeApplicationType
import rundeck.*
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.UserService
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.annotation.Annotation

class UserControllerSpec extends Specification implements ControllerUnitTest<UserController>, DataTest {

    void setupSpec() {
        mockDomain Execution
        mockDomain User
        mockDomain AuthToken
        mockDomain ScheduledExecution
        mockDomain CommandExec
        mockDomain Workflow

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }
    def setup(){
        controller.apiService = Stub(ApiService)
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer)
        session.subject = new Subject()
    }

    protected void setupFormTokens(params) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }
    private <T extends Annotation> T getControllerMethodAnnotation(String name, Class<T> clazz) {
        artefactInstance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }

    @Unroll
    def "RdAuthorizeApplicationType required for endpoint #endpoint access #access type #type"() {
        given:
            def auth = getControllerMethodAnnotation(endpoint, RdAuthorizeApplicationType)
        expect:
            auth.type() == type
            auth.access() == access
        where:
            endpoint  | access                               | type
            'list'    | RundeckAccess.General.AUTH_APP_ADMIN | AuthConstants.TYPE_USER

    }


    @Unroll
    def "generate service token unauthorized"() {
        given:

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        def utilTagLib = mockTagLib(UtilityTagLib)
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> user
            getRoles() >> (['blah'] as Set)
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_) >> auth
        }
        controller.apiService = Mock(ApiService) {
            1 * generateUserToken(_, 60, user,_,_,_,_) >> {
                throw new Exception("Unauthorized blah")
            }
        }

        setupFormTokens(params)
        when:
        request.method = 'POST'
        params.login = login

        def result = controller.generateUserToken(time, unit, user, roles, name)

        then:
        response.status == 302
        flash.error == 'Unauthorized blah'
        response.redirectedUrl == "/user/profile?login=${login}"

        where:
        time | unit | user    | roles | login | name
        '1'  | 'm'  | 'admin' | 'a,b' | ''    | null
        '1'  | 'm'  | 'admin' | 'a,b' | ''    | 'foo'
    }

    @Unroll
    def "get info same user"() {
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> userToSearch
        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_) >> auth
        }
        controller.apiService = Mock(ApiService) {
            1 * requireVersion(_, _, 21) >> true
            0 * renderErrorXml(_, _) >> { HttpServletResponse response, Map error ->
                response.status = error.status
                null
            }
        }
        when:
        request.method='GET'
        request.format='xml'
        def result=controller.apiUserData()

        then:
        response.status==200
    }
    @Unroll
    def "get info different user as admin"(){
        given:
        def userToSearch = 'user'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'admin'
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        //request.content=('<contents>'+text+'</contents>').bytes
        def result=controller.apiUserData()

        then:
        response.status==200
    }
    @Unroll
    def "get info different user as non admin"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'user'
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> false

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            1 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        def result=controller.apiUserData()

        then:
        response.status==HttpServletResponse.SC_FORBIDDEN
    }

    @Unroll
    def "get info from unexistent user"(){
        given:
        def userToSearch = 'user'
        User u = new User(login: 'anotheruser')
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                0 * authorizeApplicationResourceAny(_,_,_) >> false

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            1 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        def result=controller.apiUserData()

        then:
        response.status==HttpServletResponse.SC_NOT_FOUND
    }

    @Unroll
    def "modify info same user using xml"(){
        given:
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '<?xml version="1.0" encoding="UTF-8"?><user><email>'+email+'</email></user>'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
            1 * parseJsonXmlWith(_,_,_) >> {HttpServletRequest request, HttpServletResponse response,
                Map<String, Closure> handlers ->
                handlers.xml(request.XML)
                true
            }
        }
        when:
        request.method='POST'
        request.format='xml'
        request.content=(text).bytes
        def result=controller.apiUserData()

        then:
        response.status==200
        User savedUser = User.findByLogin(userToSearch)
        savedUser.email == email
    }

    @Unroll
    def "modify info same user using json"(){
        given:
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '{email:\''+email+'\',firstName:\'The\', lastName:\'Admin\'}'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
            1 * parseJsonXmlWith(_,_,_) >> {HttpServletRequest request, HttpServletResponse response,
                                            Map<String, Closure> handlers ->
                handlers.json(request.JSON)
                true
            }
        }
        when:
        request.method='POST'
        request.format='json'
        request.content=(text).bytes
        def result=controller.apiUserData()

        then:
        response.status==200
        User savedUser = User.findByLogin(userToSearch)
        savedUser.email == email
    }


    @Unroll
    def "get user list xml"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.method='GET'
        request.format='xml'
        def result=controller.apiUserList()

        then:
        response.status==200
    }

    @Unroll
    def "get user list json"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorJson(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.method='GET'
        request.format='json'
        def result=controller.apiUserList()

        then:
        response.status==200
    }


    @Unroll
    def "get user list non admin"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'user'
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> false

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            1 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        def result=controller.apiUserList()

        then:
        response.status==HttpServletResponse.SC_FORBIDDEN
    }


    @Unroll
    def "get user list xml v27 response ok"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.api_version=27
        request.method='GET'
        request.format='xml'
        def result=controller.apiUserList()

        then:
        response.status==200
    }

    @Unroll
    def "get user list json v27 response ok"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,_) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,21) >> true
            0 * renderErrorJson(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.api_version=27
        request.method='GET'
        request.format='json'
        def result=controller.apiUserList()

        then:
        response.status==200
    }


    @Unroll
    def "addFilterPref session"() {
        given:
        def filterPref = 'nodes=local'

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        def utilTagLib = mockTagLib(UtilityTagLib)

        controller.userService = Mock(UserService) {
            1 * storeFilterPref(_,_) >> [user:[filterPref:filterPref]]
        }

        setupFormTokens(params)
        when:
        request.method = 'POST'
        params.filterpref = filterPref

        def result = controller.addFilterPref()

        then:
        session.filterPref==[nodes:'local']

    }

    def "get user roles json 30 response ok"(){
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getRoles()>>["admin","user"]
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubject(_) >> auth
            }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,30) >> true
            0 * renderErrorJson(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.api_version=30
        request.method='GET'
        request.format='json'
        controller.apiListRoles()

        then:
        response.status==200
    }

    @Unroll
    def "profile"() {
        setup:
        User user = new User(login: "admin")
        user.save()
        createAuthToken(user:user,type:null)
        createAuthToken(user:user,type: AuthTokenType.USER)
        createAuthToken(user:user,type: AuthTokenType.WEBHOOK)
        createAuthToken(user:user,creator:'admin',type: AuthTokenType.USER)
        def authCtx = Mock(UserAndRolesAuthContext)
        session.user='admin'
            controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
                1 * applicationType(_,AuthConstants.TYPE_USER)>>Mock(AuthorizingAppType){
                    1 * isAuthorized(RundeckAccess.General.APP_ADMIN) >> isAdmin
                    1 * getAuthContext()>>authCtx
                    0 * _(*_)
                }
            }
        when:
        params.login = "admin"
        def result = controller.profile()

        then:
        AuthToken.count() == 4
        result.tokenTotal == total
        result.tokenList.size() == total
        where:
            isAdmin | total
            true    | 3
            false   | 1
    }

    def "profile unauthorized"() {
        setup:
        User user = new User(login: "admin")
        user.save()
        createAuthToken(user:user,type:null)
        createAuthToken(user:user,type: AuthTokenType.USER)
        createAuthToken(user:user,type: AuthTokenType.WEBHOOK)
        controller.apiService = Stub(ApiService)

        controller.metaClass.unauthorizedResponse = { Object tst, String action, Object name, boolean fg -> false }
        controller.metaClass.notFoundResponse = { Object tst, String action, Object name, boolean fg -> false }
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * applicationType(_,AuthConstants.TYPE_USER)>>Mock(AuthorizingAppType){
                1 * authorize(RundeckAccess.General.APP_ADMIN)>>{
                    throw new UnauthorizedAccess('admin','system','resource')
                }
            }
        }
        controller.rundeckExceptionHandler=Mock(WebExceptionHandler)

        when:
            session.user='notadmin'
            params.login = "admin"
            def result = controller.profile()

        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_,_ as UnauthorizedAccess)
    }


    def "loadUsersList summary with last exec"() {
        given:
            UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
            def userToSearch = 'admin'
            def email = 'test@test.com'
            def text = '{email:\'' + email + '\',firstName:\'The\', lastName:\'Admin\'}'
            def lastSessionId = 'exampleSessionId01'
            User u = new User(login: userToSearch, lastSessionId: lastSessionId)
            u.save()

            ScheduledExecution job = new ScheduledExecution(
                    jobName: 'blue',
                    project: 'AProject',
                    groupPath: 'some/where',
                    description: 'a job',
                    argString: '-a b -c d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    retry: '1'
            )
            job.save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: userToSearch,
                    project: 'AProject'
            ).save()
            controller.userService = Mock(UserService) {
                isSessionIdRegisterEnabled() >> true
                findWithFilters(*_) >> [users: [u], totalRecords: 1]
            }

        when:
            params.includeExec=true
            def users = controller.loadUsersList()

        then:
            response.json.users
            response.json.users.find { it.login == userToSearch }
            response.json.users.find { it.login == userToSearch }.lastJob
            response.json.users.find { it.login == userToSearch }.lastSessionId == lastSessionId

    }

    def "loadUsersList summary with logged in status"() {
        given:
            UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
            def userToSearch = 'admin'
            def email = 'test@test.com'
            def text = '{email:\'' + email + '\',firstName:\'The\', lastName:\'Admin\'}'
            def lastSessionId = 'exampleSessionId02'
            User u = new User(login: userToSearch, lastSessionId: lastSessionId)
            u.save()

            ScheduledExecution job = new ScheduledExecution(
                    jobName: 'blue',
                    project: 'AProject',
                    groupPath: 'some/where',
                    description: 'a job',
                    argString: '-a b -c d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    retry: '1'
            )
            job.save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: userToSearch,
                    project: 'AProject'
            ).save()

            controller.userService = Mock(UserService) {
                isSessionIdRegisterEnabled() >> true
                findWithFilters(*_) >> [users: [u], totalRecords: 1]
                getLoginStatus(_)>>UserService.LogginStatus.LOGGEDIN.value
            }
        when:
            params.includeExec=true
            def users = controller.loadUsersList()


        then:
            response.json.users
            response.json.users.find { it.login == userToSearch }
            response.json.users.find { it.login == userToSearch }.lastJob
            response.json.users.find { it.login == userToSearch }.loggedStatus == UserService.LogginStatus.LOGGEDIN.value
            response.json.users.find { it.login == userToSearch }.lastSessionId == lastSessionId

    }

    def "loadUsersList summary with no session id"() {
        given:
            UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
            def userToSearch = 'admin'
            def email = 'test@test.com'
            def text = '{email:\'' + email + '\',firstName:\'The\', lastName:\'Admin\'}'
            def lastSessionId = 'exampleSessionId02'
            User u = new User(login: userToSearch, lastSessionId: lastSessionId)
            u.save()

            ScheduledExecution job = new ScheduledExecution(
                    jobName: 'blue',
                    project: 'AProject',
                    groupPath: 'some/where',
                    description: 'a job',
                    argString: '-a b -c d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    retry: '1'
            )
            job.save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: userToSearch,
                    project: 'AProject'
            ).save()

            controller.userService = Mock(UserService) {
                isSessionIdRegisterEnabled() >> false
                findWithFilters(*_) >> [users: [u], totalRecords: 1]
                getLoginStatus(_)>>UserService.LogginStatus.LOGGEDIN.value
            }
        when:
            params.includeExec=true
            def users = controller.loadUsersList()

        then:
            response.json.users
            response.json.users.find { it.login == userToSearch }
            response.json.users.find { it.login == userToSearch }.lastJob
            response.json.users.find { it.login == userToSearch }.loggedStatus == UserService.LogginStatus.LOGGEDIN.value
            response.json.users.find { it.login == userToSearch }.lastSessionId == null

    }

    def "countUserApiTokens does not include webhook tokens"() {
        given:
        User bob = new User(login:"bob")
        bob.save()
        new AuthToken(user:bob,type:AuthTokenType.USER,authRoles: "admin",token:Math.random().toString()).save()
        new AuthToken(user:bob,authRoles: "admin",token:Math.random().toString()).save()
        new AuthToken(user:bob,type:AuthTokenType.WEBHOOK,authRoles: "admin",token:Math.random().toString()).save()

        when:
        def tokenCount = controller.countUserApiTokens(bob)

        then:
        tokenCount == 2
    }

    private void createAuthToken(params) {
        AuthToken tk = new AuthToken()
        tk.authRoles = "admin"
        tk.token = Math.random().toString()
        params.each { k, v ->
            tk[k] = v
        }
        tk.save()
    }

    def "getSummaryPageConfig with no rundeck config values"() {
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.userService = Mock(UserService) {
            1 * getSummaryPageConfig() >> [
                    loggedOnly      :false,
                    showLoginStatus :false
            ]
        }

        when:
        def config = controller.getSummaryPageConfig()

        then:
        response.json.loggedOnly == false
        response.json.showLoginStatus == false
    }

    def "getSummaryPageConfig with rundeck config values"() {
        given:
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext)
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true

                1 * getAuthContextForSubject(_) >> auth
            }
        controller.userService = Mock(UserService) {
            1 * getSummaryPageConfig() >> [
                    loggedOnly      :true,
                    showLoginStatus :true
            ]
        }
        when:
        def config = controller.getSummaryPageConfig()

        then:
        response.json.loggedOnly == true
        response.json.showLoginStatus == true
    }

    def "authz required for register different username"() {
        given:
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * authorizeApplicationResourceAny(
                    _,
                    AuthConstants.RESOURCE_TYPE_SYSTEM,
                    [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
                ) >> false

                1 * getAuthContextForSubject(_) >> Mock(UserAndRolesAuthContext) {
                    getUserName() >> 'userA'
                }
            }
            params.login = 'userB'
        when:
            controller.register()
        then:
            response.status == 403
    }

    def "error sets loginErrorCode flash "(){
        given:
            if(code){
                flash.loginErrorCode=code
            }
        when:
            controller.error()
        then:
            flash.loginErrorCode!=null
            flash.loginErrorCode==expected
        where:
            code              | expected
            null              | 'invalid.username.and.password'
            'some.other.code' | 'some.other.code'
    }
}
