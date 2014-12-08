package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
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

    def invalid = {
        return apiService.renderErrorXml(response,[code:'api.error.invalid.request',args:[request.forwardURI],status:HttpServletResponse.SC_NOT_FOUND])
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
            render(contentType: 'text/plain'){
                grailsApplication.config.feature?.incubator?.each{k,v->
                    out<<"${k}:${v in [true,'true']}\n"
                }
            }
        }
    }
    private renderToken(AuthToken oldtoken){
        withFormat {
            xml {
                apiService.renderSuccessXml(request, response) {
                    delegate.token(id: oldtoken.token, user: oldtoken.user.login)
                }
            }
            json {
                render(contentType: 'application/json') {
                    delegate.id = oldtoken.token
                    delegate.user = oldtoken.user.login
                }

            }
        }
    }
    /*
     * /api/11/tokens/$user?
     */
    def apiTokenList() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!frameworkService.authorizeApplicationResourceType(authContext, AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN)){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_FORBIDDEN,
                    code:'api.error.item.unauthorized',
                    args:[AuthConstants.ACTION_ADMIN,'Rundeck','User account']
            ])
        }
        if(request.method=='POST'){
            //parse input json or xml
            def tokenuser=params.user
            if (!params.user) {
                def errormsg="Format was not valid."
                def parsed=apiService.parseJsonXmlWith(request,response,[
                        json:{data->
                            tokenuser=data.user
                            if(!tokenuser) {
                                errormsg += " json: expected 'user' property"
                            }
                        },
                        xml:{xml->
                            tokenuser=xml.'@user'.text()
                            if (!tokenuser) {
                                errormsg += " xml: expected 'user' attribute"
                            }
                        }
                ])
                if(!parsed){
                    return
                }
                if (!tokenuser) {
                    return apiService.renderErrorFormat(response, [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: 'api.error.invalid.request',
                            args: [errormsg]
                    ])
                }
            }

            //create token for a user
            def User u = userService.findOrCreateUser(tokenuser)
            def token = apiService.generateAuthToken(u)
            response.status=HttpServletResponse.SC_CREATED
            return renderToken(token)
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
                    def attrs=[count: tokenlist.size()]
                    if(params.user){
                        attrs.user=params.user
                    }else{
                        attrs.allusers='true'
                    }
                    tokens(attrs) {
                        tokenlist.each { AuthToken token ->
                            delegate.token(id: token.token, user: token.user.login)
                        }
                    }
                }
            }
            json {
                render(contentType: 'application/json') {
                    array {
                        tokenlist.each { AuthToken token ->
                            delegate.element(id: token.token, user: token.user.login)
                        }
                    }
                }

            }
        }
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
        def metricsJsonUrl = createLink(uri: '/metrics/metrics?pretty=true',absolute: true)
        def metricsThreadDumpUrl = createLink(uri: '/metrics/threads',absolute: true)
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
                    }
                    threads{
                        active(threadActiveCount)
                    }
                }
                metrics(href:metricsJsonUrl,contentType:'text/json')
                threadDump(href:metricsThreadDumpUrl,contentType:'text/plain')
            }
        }
    }
}
