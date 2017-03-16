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

import com.dtolabs.rundeck.core.authorization.AuthContext
import org.rundeck.util.Sizes
import rundeck.AuthToken
import rundeck.User

import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.filters.ApiRequestFilters

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
            apiTokenList  : ['GET'],
            apiTokenCreate: ['GET']
    ]
    def invalid = {
        return apiService.renderErrorXml(response,[code:'api.error.invalid.request',args:[request.forwardURI],status:HttpServletResponse.SC_NOT_FOUND])
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
    private renderToken(AuthToken oldtoken){
        withFormat {
            xml {
                apiService.renderSuccessXml(request, response) {
                    delegate.token(id: oldtoken.token, user: oldtoken.user.login, roles:oldtoken.authRoles, expiration:oldtoken.expiration)
                }
            }
            json {
                render(contentType: 'application/json') {
                    delegate.id = oldtoken.token
                    delegate.user = oldtoken.user.login
                    delegate.roles = oldtoken.authRolesSet()
                    delegate.expiration = oldtoken.expiration
                }

            }
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

        if (!frameworkService.authorizeApplicationResourceType(
                authContext,
                AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN
        )) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_ADMIN, 'Rundeck', 'User account'])
        }
        def tokenlist
        if (params.user) {
            def user = User.findByLogin(params.user)
            tokenlist = user ? AuthToken.findAllByUser(user) : []
        } else {
            tokenlist = AuthToken.list()
        }

        withFormat {
            xml {
                apiService.renderSuccessXml(request, response) {
                    def attrs = [count: tokenlist.size()]
                    if (params.user) {
                        attrs.user = params.user
                    } else {
                        attrs.allusers = 'true'
                    }
                    tokens(attrs) {
                        tokenlist.each { AuthToken token ->
                            delegate.token(
                                    id: token.token,
                                    user: token.user.login,
                                    roles: token.authRoles,
                                    expiration: token.expiration
                            )
                        }
                    }
                }
            }
            json {
                render(contentType: 'application/json') {
                    array {
                        tokenlist.each { AuthToken token ->
                            delegate.element(
                                    id: token.token,
                                    user: token.user.login,
                                    roles: token.authRolesSet(),
                                    expiration: token.expiration
                            )
                        }
                    }
                }

            }
        }
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
        String tokenuser = params.user
        def roles = null
        def tokenDuration = null
        def errors = []
        boolean tokenRolesV19Enabled = request.api_version > ApiRequestFilters.V18

        def parsed = apiService.parseJsonXmlWith(request, response, [
                json: { data ->
                    if (!params.user) {
                        tokenuser = data.user
                        if (!tokenuser) {
                            errors << " json: expected 'user' property"
                        }
                    }
                    roles = data.roles
                    tokenDuration = data.duration
                    if (!roles && tokenRolesV19Enabled) {
                        errors << " json: expected 'roles' property"
                    }
                },
                xml : { xml ->
                    if (!params.user) {
                        tokenuser = xml.'@user'.text()
                        if (!tokenuser) {
                            errors << " xml: expected 'user' attribute"
                        }
                    }
                    roles = xml.'@roles'.text()
                    tokenDuration = xml.'@duration'.text()
                    if (!roles && tokenRolesV19Enabled) {
                        errors << " xml: expected 'roles' attribute"
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
        if (request.api_version <= ApiRequestFilters.V18) {
            roles = 'api_token_group'
            tokenDuration = null
        }
        if (roles instanceof String) {
            roles = AuthToken.parseAuthRoles(roles)
        } else if (roles instanceof Collection) {
            roles = new HashSet(roles)
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
                    roles
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
        renderToken(token)
    }

    /**
     * /api/11/token/$token
     */
    def apiTokenManage() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN)) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [AuthConstants.ACTION_ADMIN, 'Rundeck', 'User account']
            ])
        }
        if(!apiService.requireParametersFormat(params,response,['token'])){
            return
        }

        AuthToken oldtoken = AuthToken.findByToken(params.token)
        if (!apiService.requireExistsFormat(response,oldtoken,['Token',params.token])) {
            return
        }

        switch (request.method){
            case 'GET':
                return renderToken(oldtoken)
                break;
            case 'DELETE':
                def findtoken=params.token
                def login=oldtoken.user.login
                def oldAuthRoles = oldtoken.authRoles
                oldtoken.delete(flush: true)
                log.info("EXPIRE TOKEN ${findtoken} for User ${login} with roles: ${oldAuthRoles}")
                return render(status: HttpServletResponse.SC_NO_CONTENT)
                break;
        }
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
        if (request.api_version < ApiRequestFilters.V14 && !(response.format in ['all','xml'])) {
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
                            apiversion(ApiRequestFilters.API_CURRENT_VERSION)
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
                            apiversion=(ApiRequestFilters.API_CURRENT_VERSION)
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
