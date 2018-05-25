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

import com.dtolabs.rundeck.app.api.tokens.ListTokens
import com.dtolabs.rundeck.app.api.tokens.RemoveExpiredTokens
import com.dtolabs.rundeck.app.api.tokens.Token
import com.dtolabs.rundeck.core.authorization.AuthContext
import org.rundeck.util.Sizes
import rundeck.AuthToken

import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.app.api.ApiVersions

/**
 * Contains utility actions for API access and responses
 */
class ApiController extends ControllerBase{
    def defaultAction = "invalid"
    def quartzScheduler
    def frameworkService
    def apiService
    def userService
    def configurationService

    static allowedMethods = [
            apiTokenList         : ['GET'],
            apiTokenCreate       : ['POST'],
            apiTokenRemoveExpired: ['POST']
    ]
    def invalid = {
        return apiService.renderErrorFormat(response,[code:'api.error.invalid.request',args:[request.forwardURI],status:HttpServletResponse.SC_NOT_FOUND])
    }
    /**
     * Respond with a 400 error and information about new endpoint location
     * @return
     */
    def endpointMoved() {
        return apiService.renderErrorFormat(
                response,
                [
                        code: 'api.error.endpoint.moved',
                        args: [
                                request.forwardURI,
                                params.moved_to
                        ],
                        status: HttpServletResponse.SC_BAD_REQUEST
                ]
        )
    }

    /**
     * API endpoints
     */
    /**
     * Return true if grails configuration allows given feature, or '*' features
     * @param name
     * @return
     */
    private boolean featurePresent(def name){
        def splat=grailsApplication.config.feature?.incubator?.getAt('*') in ['true',true]
        return splat || (grailsApplication.config.feature?.incubator?.getAt(name) in ['true',true])
    }
    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    private void toggleFeature(def name, boolean enable){
        grailsApplication.config.feature?.incubator?.putAt(name, enable)
    }
    /**
     * Feature toggle api endpoint for development mode
     */
    def featureToggle={
        if (!apiService.requireApi(request, response)) {
            return
        }
        def respond={
            render(contentType: 'text/plain', text: featurePresent(params.featureName) ? 'true' : 'false')
        }
        if(params.featureName){
            if(request.method=='GET'){
                respond()
            } else if (request.method=='PUT'){
                toggleFeature(params.featureName, request.inputStream.text=='true')
                respond()
            }else if(request.method=='POST'){
                toggleFeature(params.featureName, true)
                respond()
            }else if(request.method=='DELETE'){
                toggleFeature(params.featureName, false)
                respond()
            }
        }else{
            response.contentType='text/plain'
            response.outputStream.withWriter('UTF-8') { w ->
                grailsApplication.config.feature?.incubator?.each { k, v ->
                    w << "${k}:${v in [true, 'true']}\n"
                }
            }
            response.outputStream.close()
        }
    }
    /**
     * /api/11/token/$token
     */
    def apiTokenManage() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['token'])) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)

        if (request.api_version < ApiVersions.V19 && !adminAuth) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_ADMIN, 'Rundeck', 'User account'])
        }

        //admin: search by token ID then token value
        //user: search for token ID owned by user
        AuthToken oldtoken = adminAuth ?
                (apiService.findTokenId(params.token) ?: apiService.findUserTokenValue(params.token)) :
                apiService.findUserTokenId(authContext.username, params.token)


        if (!apiService.requireExistsFormat(response, oldtoken, ['Token', params.token])) {
            return
        }

        switch (request.method) {
            case 'GET':
                return respond(new Token(oldtoken), [formats: ['xml', 'json']])
                break;
            case 'DELETE':
                apiService.removeToken(oldtoken)
                return render(status: HttpServletResponse.SC_NO_CONTENT)
                break;
        }
    }

    /**
     * GET /api/11/tokens/$user?
     */
    def apiTokenList() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        def adminAuth = apiService.hasTokenAdminAuth(authContext)

        if (request.api_version < ApiVersions.V19 && !adminAuth) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_ADMIN, 'Rundeck', 'User account'])
        }

        if (!adminAuth && params.user && params.user != authContext.username) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_ADMIN, 'User', params.user])
        }
        def tokenlist
        if (params.user) {
            tokenlist = apiService.findUserTokensCreator(params.user)
        } else if (!adminAuth) {
            tokenlist = apiService.findUserTokensCreator(authContext.username)
        } else {
            tokenlist = AuthToken.list()
        }
        def apiv19 = request.api_version >= ApiVersions.V19
        def data = new ListTokens(params.user, !params.user, tokenlist.collect { new Token(it, apiv19) })

        respond(data, [formats: ['xml', 'json']])
    }

    /**
     * POST /api/11/tokens/$user?
     * @return
     */
    def apiTokenCreate() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
            //parse input json or xml
        String tokenuser = params.user ?: authContext.username
        def roles = null
        def tokenDuration = null
        def errors = []
        boolean tokenRolesV19Enabled = request.api_version >= ApiVersions.V19

        if (tokenRolesV19Enabled || request.getHeader("Content-Type")) {
            def parsed = apiService.parseJsonXmlWith(request, response, [
                    json: { data ->
                        if (!params.user) {
                            tokenuser = data.user
                            if (!tokenuser) {
                                errors << " json: expected 'user' property"
                            }
                        }
                        if (tokenRolesV19Enabled) {
                            roles = data.roles
                            tokenDuration = data.duration
                            if (!roles) {
                                errors << " json: expected 'roles' property"
                            }
                        }
                    },
                    xml : { xml ->
                        if (!params.user) {
                            tokenuser = xml.'@user'.text()
                            if (!tokenuser) {
                                errors << " xml: expected 'user' attribute"
                            }
                        }
                        if (tokenRolesV19Enabled) {
                            roles = xml.'@roles'.text()
                            tokenDuration = xml.'@duration'.text()
                            if (!roles) {
                                errors << " xml: expected 'roles' attribute"
                            }
                        }
                    }
            ]
            )
            if (!parsed) {
                return
            }
            if (errors) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.invalid.request',
                        args  : ["Format was not valid." + errors.join(" ")]
                ]
                )
            }
        }
        if (!tokenRolesV19Enabled) {
            roles = 'api_token_group'
            tokenDuration = null
        }
        Set<String> rolesSet=null
        if (roles instanceof String) {
            rolesSet = AuthToken.parseAuthRoles(roles)
        } else if (roles instanceof Collection) {
            rolesSet = new HashSet(roles)
        }
        if (rolesSet.size() == 1 && rolesSet.contains('*')) {
            rolesSet = null
        }
        AuthToken token

        Integer tokenDurationSeconds = tokenDuration ? Sizes.parseTimeDuration(tokenDuration) : 0
        if (tokenDuration && !Sizes.validTimeDuration(tokenDuration)) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.parameter.invalid',
                    args  : [tokenDuration, "duration", "Format was not valid"]
            ]
            )
        }
        try {
            token = apiService.generateUserToken(
                    authContext,
                    tokenDurationSeconds ?: null,
                    tokenuser,
                    rolesSet
            )
        } catch (Exception e) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.invalid.request',
                    args  : [e.message]
            ]
            )
        }
        response.status = HttpServletResponse.SC_CREATED
        respond(new Token(token), [formats: ['xml', 'json']])
    }

    /**
     * /api/19/tokens/$user?/removeExpired
     */
    def apiTokenRemoveExpired() {
        if (!apiService.requireVersion(request, response, ApiVersions.V19)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def adminAuth = apiService.hasTokenAdminAuth(authContext)

        if (!apiService.requireParameters(params, response, ['user'])) {
            return
        }
        def user = params.user
        def alltokens = user == '*'

        if (!adminAuth && alltokens) {
            return apiService.renderUnauthorized(
                    response,
                    [AuthConstants.ACTION_ADMIN, 'API Tokens for ', "All users"]
            )
        }
        if (!adminAuth && user != authContext.username) {
            return apiService.renderUnauthorized(
                    response,
                    [AuthConstants.ACTION_ADMIN, 'API Tokens for user: ', params.user]
            )
        }

        def resultCount = alltokens ?
                apiService.removeAllExpiredTokens() :
                apiService.removeAllExpiredTokens(user)

        respond(
                new RemoveExpiredTokens(count: resultCount, message: "Removed $resultCount expired tokens"),
                [formats: ['json', 'xml']]
        )
    }
    /**
     * /api/1/system/info: display stats and info about the server
     */
    def apiSystemInfo={
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                AuthConstants.ACTION_READ)) {
            return apiService.renderErrorXml(response,[status:HttpServletResponse.SC_FORBIDDEN, code: 'api.error.item.unauthorized', args: ['Read System Info', 'Rundeck', ""]])
        }
        Date nowDate=new Date();
        String nodeName= servletContext.getAttribute("FRAMEWORK_NODE")
        String appVersion= grailsApplication.metadata['app.version']
        String sUUID= frameworkService.getServerUUID()
        double load= ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        int processorsCount= ManagementFactory.getOperatingSystemMXBean().availableProcessors
        String osName= ManagementFactory.getOperatingSystemMXBean().name
        String osVersion= ManagementFactory.getOperatingSystemMXBean().version
        String osArch= ManagementFactory.getOperatingSystemMXBean().arch
        String javaVendor=System.properties['java.vendor']
        String javaVersion=System.properties['java.version']
        String vmName=ManagementFactory.getRuntimeMXBean().vmName
        String vmVendor=ManagementFactory.getRuntimeMXBean().vmVendor
        String vmVersion=ManagementFactory.getRuntimeMXBean().vmVersion
        long durationTime=ManagementFactory.getRuntimeMXBean().uptime
        Date startupDate = new Date(nowDate.getTime()-durationTime)
        int threadActiveCount=Thread.activeCount()
        boolean executionModeActive=configurationService.executionModeActive
        def metricsJsonUrl = createLink(uri: '/metrics/metrics?pretty=true',absolute: true)
        def metricsThreadDumpUrl = createLink(uri: '/metrics/threads',absolute: true)
        def metricsHealthcheckUrl = createLink(uri: '/metrics/healthcheck',absolute: true)
        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorXml(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response){
                    if(apiService.doWrapXmlResponse(request)){
                        delegate.'success' {
                            delegate.'message'("System Stats for Rundeck ${appVersion} on node ${nodeName}")
                        }
                    }
                    delegate.'system'{
                        timestamp(epoch:nowDate.getTime(),unit:'ms'){
                            datetime(g.w3cDateValue(date:nowDate))
                        }
                        rundeck{
                            version(appVersion)
                            build(grailsApplication.metadata['build.ident'])
                            node(nodeName)
                            base(servletContext.getAttribute("RDECK_BASE"))
                            apiversion(ApiVersions.API_CURRENT_VERSION)
                            serverUUID(sUUID)
                        }
                        executions(active:executionModeActive,executionMode:executionModeActive?'active':'passive')
                        os {
                            arch(osArch)
                            name(osName)
                            version(osVersion)
                        }
                        jvm {
                            name(vmName)
                            vendor(javaVendor)
                            version(javaVersion)
                            implementationVersion(vmVersion)
                        }
                        stats{
                            uptime(duration:durationTime,unit: 'ms'){
                                since(epoch: startupDate.getTime(),unit:'ms'){
                                    datetime(g.w3cDateValue(date: startupDate))
                                }
                            }
            //                errorCount('12')
        //                    requestCount('12')
                            cpu{
                                loadAverage(unit:'percent',load)
                                processors(processorsCount)
                            }
                            memory(unit:'byte'){
                                max(Runtime.getRuntime().maxMemory())
                                free(Runtime.getRuntime().freeMemory())
                                total(Runtime.getRuntime().totalMemory())
                            }
                            scheduler{
                                running(quartzScheduler.getCurrentlyExecutingJobs().size())
                                threadPoolSize(quartzScheduler.getMetaData().threadPoolSize)
                            }
                            threads{
                                active(threadActiveCount)
                            }
                        }
                        metrics(href:metricsJsonUrl,contentType:'application/json')
                        threadDump(href:metricsThreadDumpUrl,contentType:'text/plain')
                        healthcheck(href:metricsHealthcheckUrl,contentType:'application/json')
                    }
                }

            }
            json{

                return apiService.renderSuccessJson(response){
                    delegate.'system'={
                        timestamp={
                            epoch=nowDate.getTime()
                            unit='ms'
                            datetime=g.w3cDateValue(date:nowDate)
                        }
                        rundeck={
                            version=(appVersion)
                            build=(grailsApplication.metadata['build.ident'])
                            node=(nodeName)
                            base=(servletContext.getAttribute("RDECK_BASE"))
                            apiversion=(ApiVersions.API_CURRENT_VERSION)
                            serverUUID=(sUUID)
                        }
                        executions={
                            active=executionModeActive
                            executionMode=executionModeActive?'active':'passive'
                        }
                        os= {
                            arch=(osArch)
                            name=(osName)
                            version=(osVersion)
                        }
                        jvm= {
                            name=(vmName)
                            vendor=(javaVendor)
                            version=(javaVersion)
                            implementationVersion=(vmVersion)
                        }
                        stats={
                            uptime={
                                duration=durationTime
                                unit= 'ms'
                                since={
                                    epoch= startupDate.getTime()
                                    unit='ms'
                                    datetime=(g.w3cDateValue(date: startupDate))
                                }
                            }
                            cpu={
                                loadAverage=[unit:'percent',average:load]
                                processors=(processorsCount)
                            }
                            memory={
                                unit='byte'
                                max=(Runtime.getRuntime().maxMemory())
                                free=(Runtime.getRuntime().freeMemory())
                                total=(Runtime.getRuntime().totalMemory())
                            }
                            scheduler={
                                running=(quartzScheduler.getCurrentlyExecutingJobs().size())
                                threadPoolSize=(quartzScheduler.getMetaData().threadPoolSize)
                            }
                            threads={
                                active=(threadActiveCount)
                            }
                        }
                        metrics=[href:metricsJsonUrl,contentType:'application/json']
                        threadDump=[href:metricsThreadDumpUrl,contentType:'text/plain']
                        healthcheck=[href:metricsHealthcheckUrl,contentType:'application/json']
                    }
                }
            }
        }
    }
}
