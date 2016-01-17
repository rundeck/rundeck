package rundeck.controllers

import com.dtolabs.rundeck.app.support.ProjectArchiveImportRequest
import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.ArchiveOptions
import rundeck.services.ProjectServiceException

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import org.apache.commons.fileupload.util.Streams
import org.springframework.web.multipart.MultipartHttpServletRequest
import com.dtolabs.rundeck.core.authentication.Group

class ProjectController extends ControllerBase{
    def frameworkService
    def projectService
    def apiService
    def authorizationService
    def static allowedMethods = [
            apiProjectConfigKeyDelete:['DELETE'],
            apiProjectConfigKeyPut:['PUT'],
            apiProjectConfigPut:['PUT'],
            apiProjectFilePut:['PUT'],
            apiProjectFileDelete:['DELETE'],
            apiProjectCreate:['POST'],
            apiProjectDelete:['DELETE'],
            apiProjectImport: ['PUT'],
            apiProjectAcls:['GET','POST','PUT','DELETE'],
            importArchive: ['POST'],
            delete: ['POST'],
    ]

    def index () {
        return redirect(controller: 'menu', action: 'jobs')
    }

    public def export(ProjectArchiveParams archiveParams){
        if (archiveParams.hasErrors()) {
            flash.errors = archiveParams.errors
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
        def project=params.project
        if (!project){
            return renderErrorView("Project parameter is required")
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (notFoundResponse(frameworkService.existsFrameworkProject(project), 'Project', project)) {
            return
        }

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT]),
                AuthConstants.ACTION_EXPORT, 'Project',project)) {
            return
        }
        def project1 = frameworkService.getFrameworkProject(project)

        def aclReadAuth=frameworkService.authorizeApplicationResourceAny(authContext,
                                                                         frameworkService.authResourceForProjectAcl(project),
                                                                         [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN])
        //temp file
        def outfile
        try {
            outfile = projectService.exportProjectToFile(project1,framework,null,aclReadAuth)
        } catch (ProjectServiceException exc) {
            return renderErrorView(exc.message)
        }
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        def dateStamp = dateFormater.format(new Date());
        //output the file as an attachment
        response.setContentType("application/zip")
        response.setHeader("Content-Disposition", "attachment; filename=\"${project}-${dateStamp}.rdproject.jar\"")

        outfile.withInputStream {instream->
            Streams.copy(instream,response.outputStream,false)
        }
        outfile.delete()
    }
    /**
     * Async version of export, acquires a token and redirects to exportWait
     * @param archiveParams
     * @return
     */
    public def exportPrepare(ProjectArchiveParams archiveParams){
        if (archiveParams.hasErrors()) {
            flash.errors = archiveParams.errors
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
        def project=params.project
        if (!project){
            return renderErrorView("Project parameter is required")
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (notFoundResponse(frameworkService.existsFrameworkProject(project), 'Project', project)) {
            return
        }

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT]),
                AuthConstants.ACTION_EXPORT, 'Project',project)) {
            return
        }
        def project1 = frameworkService.getFrameworkProject(project)

        //request token
        def aclReadAuth=frameworkService.authorizeApplicationResourceAny(authContext,
                                                                         frameworkService.authResourceForProjectAcl(project),
                                                                         [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN])
        def token = projectService.exportProjectToFileAsync(project1, framework, session.user, aclReadAuth)
        return redirect(action:'exportWait',params: [token:token,project:archiveParams.project])
    }
    /**
     * poll for archive export process completion using a token, responds in html or json
     * @param token
     * @return
     */
    public def exportWait(String token){
        if(!projectService.hasPromise(session.user,token)){
            return withFormat{
                html{
                    request.errorCode = 'request.error.notfound.message'
                    request.errorArgs = ["Export Request Token", token]
                    response.status = HttpServletResponse.SC_NOT_FOUND
                    request.titleCode = 'request.error.notfound.title'
                    return render(view: "/common/error",model:[:])
                }

                json{
                    render(contentType:'application/json'){
                        delegate.'token'=token
                        delegate.'notFound'=true
                    }
                }
            }
        }
        if(projectService.promiseError(session.user,token)){
            def errorMessage="Project export request failed: "+projectService.promiseError(session.user,token).message
            return withFormat{
                html{
                    request.errorMessage = errorMessage
                    response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    return render(view: "/common/error",model:[:])
                }
                json{
                    render(contentType:'application/json'){
                        delegate.'token'=token
                        delegate.'errorMessage'=errorMessage
                    }
                }
            }
        }
        File outfile = projectService.promiseReady(session.user,token)
        if(null!=outfile && params.download=='true'){
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            Date date=projectService.promiseRequestStarted(session.user,token)
            def dateStamp = dateFormater.format(null!=date?date:new Date());
            //output the file as an attachment
            response.setContentType("application/zip")
            response.setHeader("Content-Disposition", "attachment; filename=\"${params.project}-${dateStamp}.rdproject.jar\"")

            outfile.withInputStream {instream->
                Streams.copy(instream,response.outputStream,false)
            }
            projectService.releasePromise(session.user,token)
        }else{
            def percentage = projectService.promiseSummary(session.user,token).percent()
            withFormat{
                html{
                    render(view: "/menu/wait",model:[token:token,ready:null!=outfile,percentage:percentage])
                }
                json{
                    render(contentType:'application/json'){
                        delegate.'token'=token
                        delegate.ready=null!=outfile
                        delegate.'percentage'=percentage
                    }
                }
            }

        }
    }

    public def importArchive(ProjectArchiveParams archiveParams){
        withForm{
        if(archiveParams.hasErrors()){
            flash.errors=archiveParams.errors
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
        def project = params.project?:params.name
        if (!project) {
            return renderErrorView("Project parameter is required")
        }

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (notFoundResponse(frameworkService.existsFrameworkProject(project), 'Project', project)) {
            return
        }

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT]),
                AuthConstants.ACTION_IMPORT, 'Project', project)) {
            return
        }
        AuthContext appContext = frameworkService.getAuthContextForSubject(session.subject)
        //verify acl create access requirement
        if (archiveParams.importACL &&
                unauthorizedResponse(
                    frameworkService.authorizeApplicationResourceAny(
                            appContext,
                            frameworkService.authResourceForProjectAcl(project),
                            [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]
                    ),
                    AuthConstants.ACTION_CREATE,
                    "ACL for Project",
                    project
                )
        ) {
            return null
        }

        def project1 = frameworkService.getFrameworkProject(project)

        //uploaded file
        if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("zipFile")
            if (!file || file.empty) {
                flash.error = message(code:"no.file.was.uploaded")
                return redirect(controller: 'menu', action: 'admin', params: [project: project])
            }
            Framework framework = frameworkService.getRundeckFramework()
            def result = projectService.importToProject(
                    project1,
                    framework,
                    authContext,
                    file.getInputStream(),
                    archiveParams

            )


            if(result.success){
                if(result.execerrors){
                    flash.message=message(code:"archive.jobs.imported.some.executions.could.not.be.imported")
                }else{
                    flash.message=message(code:"archive.successfully.imported")
                }
            }else{
                flash.error=message(code:"failed.to.import.some.jobs")
                flash.joberrors=result.joberrors
            }
            if(result.execerrors){
                flash.execerrors=result.execerrors
            }
            if(result.aclerrors){
                flash.aclerrors=result.aclerrors
            }
            return redirect(controller: 'menu',action: 'admin',params:[project:project])
        }
        }.invalidToken {
            flash.error = g.message(code:'request.error.invalidtoken.message')
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
    }

    def delete (ProjectArchiveParams archiveParams){
        withForm{
        if (archiveParams.hasErrors()) {
            flash.errors = archiveParams.errors
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
        def project = params.project
        if (!project) {
            request.error = "Project parameter is required"
            return render(view: "/common/error")
        }
        Framework framework = frameworkService.getRundeckFramework()
        if (!frameworkService.existsFrameworkProject(project)) {
            response.setStatus(404)
            request.error = g.message(code: 'scheduledExecution.project.invalid.message', args: [project])
            return render(view: "/common/error")
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(project),
                [AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_DELETE])) {
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_DELETE,
                    "Project", params.project])
            return render(view: "/common/error")
        }
        def project1 = frameworkService.getFrameworkProject(project)

        def result = projectService.deleteProject(project1, framework,authContext,session.user)
        if (!result.success) {
            log.error("Failed to delete project: ${result.error}")
            flash.error = result.error
            return redirect(controller: 'menu', action: 'admin', params: [project: project])
        }
        flash.message = 'Deleted project: ' + project
        return redirect(controller: 'menu', action: 'home')
        }.invalidToken {
            flash.error= g.message(code: 'request.error.invalidtoken.message')
            return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
        }
    }

    /**
     * Render project XML result using a builder
     * @param pject framework project object
     * @param delegate builder delegate for response
     * @param hasConfigAuth true if 'configure' action is allowed
     * @param vers api version requested
     */
    private def renderApiProjectXml (def pject, delegate, hasConfigAuth=false, vers=1){
        Map data = basicProjectDetails(pject)
        def pmap = vers < ApiRequestFilters.V11 ? [:] : [url: data.url]
        delegate.'project'(pmap) {
            name(data.name)
            description(data.description)
            if (vers < ApiRequestFilters.V11) {
                if (pject.hasProperty("project.resources.url")) {
                    resources {
                        providerURL(pject.getProperty("project.resources.url"))
                    }
                }
            } else if (hasConfigAuth) {
                //include config data
                renderApiProjectConfigXml(pject,delegate)
            }
        }
    }
    /**
     * Render project config XML content using a builder
     * @param pject framework project object
     * @param delegate builder delegate for response
     */
    private def renderApiProjectConfigXml (def pject, delegate){
        delegate.'config' {
            frameworkService.loadProjectProperties(pject).each { k, v ->
                delegate.'property'(key: k, value: v)
            }
        }
    }

    /**
     * Render project JSON result using a builder
     * @param pject framework project object
     * @param delegate builder delegate for response
     * @param hasConfigAuth true if 'configure' action is allowed
     * @param vers api version requested
     */
    private def renderApiProjectJson (def pject, delegate, hasConfigAuth=false, vers=1){
        Map data=basicProjectDetails(pject)
        delegate.url = data.url
        delegate.name = data.name
        delegate.description = data.description
        def ctrl=this
        if(hasConfigAuth){
            delegate.config {
                ctrl.renderApiProjectConfigJson(pject,delegate)
            }
        }
    }

    private Map basicProjectDetails(def pject) {
        [
                url:generateProjectApiUrl(pject.name),
                name:pject.name,
                description : pject.hasProperty('project.description') ? pject.getProperty('project.description') : ''
        ]
    }

    /**
     * Render project config JSON content using a builder
     * @param pject framework project object
     * @param delegate builder delegate for response
     */
    private def renderApiProjectConfigJson (def pject, delegate){
        frameworkService.loadProjectProperties(pject).each { k, v ->
            delegate."${k}" = v
        }
    }


    /**
     * Generate absolute api URL for the project
     * @param projectName
     * @return
     */
    private String generateProjectApiUrl(String projectName) {
        g.createLink(absolute: true, uri: "/api/${ApiRequestFilters.API_CURRENT_VERSION}/project/${projectName}")
    }

    /**
     * API: /api/11/projects
     */
    def apiProjectList(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def projlist = frameworkService.projects(authContext)
        withFormat{

            xml{
                apiService.renderSuccessXml(request, response) {
                    delegate.'projects'(count: projlist.size()) {
                        projlist.sort { a, b -> a.name <=> b.name }.each { pject ->
                            //don't include config data
                            renderApiProjectXml(pject, delegate, false, request.api_version)
                        }
                    }
                }
            }
            json{
                return render(contentType: 'application/json'){
                        def builder = delegate
                        projlist.sort { a, b -> a.name <=> b.name }.each { pject ->
                            //don't include config data
                            builder.'element'(basicProjectDetails(pject))
                        }
                }
            }
        }

    }

    /**
     * API: /api/11/project/NAME
     */
    def apiProjectGet(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!params.project) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(params.project),
                [AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN])) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read', 'Project', params.project]])
        }
        def exists = frameworkService.existsFrameworkProject(params.project)
        if (!exists) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])
        }
        def configAuth= frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(params.project),
                [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN])
        def pject = frameworkService.getFrameworkProject(params.project)
        def ctrl=this
        withFormat{
            xml{

                apiService.renderSuccessXml(request, response) {
                    if(request.api_version<ApiRequestFilters.V11){
                        delegate.'projects'(count: 1) {
                            renderApiProjectXml(pject, delegate, configAuth, request.api_version)
                        }
                    }else{
                        renderApiProjectXml(pject, delegate, configAuth, request.api_version)
                    }
                }
            }
            json{
                return render(contentType: 'application/json'){
                    ctrl.renderApiProjectJson(pject, delegate, configAuth, request.api_version)
                }
            }
        }
    }


    def apiProjectCreate() {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V11)) {
            return
        }
        //allow Accept: header, but default to the request format
        def respFormat = apiService.extractResponseFormat(request,response,['xml','json'])
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceTypeAll(authContext, 'project',
                [AuthConstants.ACTION_CREATE])) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [AuthConstants.ACTION_CREATE, "Rundeck", "Project"],
                            format:respFormat
                    ])
        }

        def project = null
        def description = null
        Map config = null

        //parse request format
        def succeeded = apiService.parseJsonXmlWith(request,response, [
                xml: { xml ->
                    project = xml?.name[0]?.text()
                    description = xml?.description[0]?.text()
                    config = [:]
                    xml?.config?.property?.each {
                        config[it.'@key'.text()] = it.'@value'.text()
                    }
                },
                json: { json ->
                    project = json?.name?.toString()
                    description = json?.description?.toString()
                    config = json?.config
                }
        ])
        if(!succeeded){
            return
        }
        if( description){
            if(config && !config['project.description']){
                config['project.description']=description
            }else if(!config){
                config=[('project.description'):description]
            }
        }

        if (!project) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.invalid.request",
                            args: ["Project 'name' is required"],
                            format: respFormat
                    ])
        }
        def exists = frameworkService.existsFrameworkProject(project)
        if (exists) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    code: 'api.error.item.alreadyexists',
                    args: ['project', project],
                    format: respFormat
            ])
        }
        def proj
        def errors
        (proj,errors)=frameworkService.createFrameworkProject(project,new Properties(config))
        if(errors){
            return apiService.renderErrorFormat(response,[status:HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: errors.join('; '),format: respFormat])
        }
        switch(respFormat) {
            case 'xml':
                apiService.renderSuccessXml(HttpServletResponse.SC_CREATED,request, response) {
                    renderApiProjectXml(proj,delegate,true,request.api_version)
                }
                break
            case 'json':
                render(status: HttpServletResponse.SC_CREATED, contentType: 'application/json') {
                    renderApiProjectJson(proj, delegate, true, request.api_version)
                }
                break
        }
    }

    def apiProjectDelete(){
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V11)) {
            return
        }
        String project = params.project
        if (!project) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.required",
                            args: ['project']
                    ])
        }
        Framework framework = frameworkService.getRundeckFramework()
        if (!frameworkService.existsFrameworkProject(project)) {

            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_NOT_FOUND,
                            code: "api.error.item.doesnotexist",
                            args: ['Project',project]
                    ])
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(project),
                [AuthConstants.ACTION_DELETE,AuthConstants.ACTION_ADMIN])) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [AuthConstants.ACTION_DELETE, "Project",project]
                    ])
        }
        def project1 = frameworkService.getFrameworkProject(project)

        def result = projectService.deleteProject(project1, framework, authContext, session.user)
        if (!result.success) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            message: result.error,
                    ])
        }
        //success
        render(status:  HttpServletResponse.SC_NO_CONTENT)
    }
    /**
     * support project/NAME/config and project/NAME/acl endpoints: validate project and appropriate authorization,
     * return null if invalid and a response has already been sent.
     * @param action action to require
     * @return FrameworkProject for the project
     */
    private def validateProjectConfigApiRequest(String action){
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V11)) {
            return
        }
        String project = params.project
        if (!project) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.required",
                            args: ['project']
                    ])
            return null
        }
        if (!frameworkService.existsFrameworkProject(project)) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_NOT_FOUND,
                            code: "api.error.item.doesnotexist",
                            args: ['Project', project]
                    ])
            return null
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(project),
                [action, AuthConstants.ACTION_ADMIN])) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [action, "Project", project]
                    ])
            return null
        }
        return frameworkService.getFrameworkProject(project)
    }
    /**
     * support project/NAME/config and project/NAME/acl endpoints: validate project and appropriate authorization,
     * return null if invalid and a response has already been sent.
     * @param action action to require
     * @return FrameworkProject for the project
     */
    private def validateProjectAclApiRequest(String action){
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V11)) {
            return
        }
        String project = params.project
        if (!project) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.required",
                            args: ['project']
                    ])
            return null
        }
        if (!frameworkService.existsFrameworkProject(project)) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_NOT_FOUND,
                            code: "api.error.item.doesnotexist",
                            args: ['Project', project]
                    ])
            return null
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProjectAcl(project),
                [action, AuthConstants.ACTION_ADMIN])) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: "api.error.item.unauthorized",
                            args: [action, "ACL for Project", project]
                    ])
            return null
        }
        return frameworkService.getFrameworkProject(project)
    }
    def apiProjectConfigGet(){
        def proj=validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if(!proj){
            return
        }
        //render project config only

        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json','text'], 'xml')
        switch (respFormat) {
            case 'text':
                response.setContentType("text/plain")
                def props=proj.getProjectProperties() as Properties
                props.store(response.outputStream,request.forwardURI)
                response.outputStream.close()
                break
            case 'xml':
                apiService.renderSuccessXml(request, response) {
                    renderApiProjectConfigXml(proj, delegate)
                }
                break
            case 'json':
                render(contentType: 'application/json') {
                    renderApiProjectConfigJson(proj, delegate)
                }
                break
        }
    }
    /**
     * /api/14/project/NAME/acl/* endpoint
     */
    def apiProjectAcls() {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V14)) {
            return
        }

        def project = validateProjectAclApiRequest(ApiService.HTTP_METHOD_ACTIONS[request.method])
        if (!project) {
            return
        }
        if(params.path && !(params.path ==~ /^[^\/]+.aclpolicy$/ )){
            def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.invalid',
                    args:[params.path,'path','Must refer to a file ending in .aclpolicy'],
                    format:respFormat
            ])
        }
        def projectFilePath = "acls/${params.path?:''}"
        switch (request.method) {
            case 'POST':
            case 'PUT':
                apiProjectAclsPutResource(project,projectFilePath,request.method=='POST')
                break
            case 'GET':
                apiProjectAclsGetResource(project,projectFilePath,'acls/')
                break
            case 'DELETE':
                apiProjectAclsDeleteResource(project,projectFilePath)
                break
        }
    }
    private def apiProjectAclsPutResource(IRundeckProject project,String projectFilePath,boolean create) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','yaml','text'],request.format)


        def exists = project.existsFileResource(projectFilePath)
        if(create && exists) {
            //conflict
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    code  : 'api.error.item.alreadyexists',
                    args  : ['Project ACL Policy File', params.path + ' for project ' + project.name],
                    format: respFormat
            ]
            )
        }else if(!create && !exists){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code  : 'api.error.item.doesnotexist',
                    args  : ['Project ACL Policy File', params.path + ' for project ' + project.name],
                    format: respFormat
            ]
            )
        }

        def error = null
        String text = null
        if (request.format in ['yaml','text']) {
            try {
                text = request.inputStream.text
            } catch (Throwable e) {
                error = e.message
            }
        }else{
            def succeeded = apiService.parseJsonXmlWith(request,response,[
                    xml:{xml->
                        if(xml?.name()=='contents'){
                            text=xml?.text()
                        }else{
                            text = xml?.contents[0]?.text()
                        }
                    },
                    json:{json->
                        text = json?.contents
                    }
            ])
            if(!succeeded){
                error= "unexpected format: ${request.format}"
                return
            }
        }
        if(error){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: error,
                    format: respFormat
            ])
        }

        if(!text){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: "No content",
                    format: respFormat
            ])
        }

        //validate input
        Validation validation = authorizationService.validateYamlPolicy(project.name, params.path, text)
        if(!validation.valid){
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return withFormat{
                def j={
                    render(contentType:'application/json'){
                        apiService.renderJsonAclpolicyValidation(validation,delegate)
                    }
                }
                xml{
                    render(contentType: 'application/xml'){
                        apiService.renderXmlAclpolicyValidation(validation,delegate)
                    }
                }
                json j
                '*' j
            }
        }

        project.storeFileResource(projectFilePath,new ByteArrayInputStream(text.bytes))

        response.status=create ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK
        if(respFormat in ['yaml','text']){
            //write directly
            response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
            project.loadFileResource(projectFilePath,response.outputStream)
            response.outputStream.close()
        }else{
            def baos=new ByteArrayOutputStream()
            project.loadFileResource(projectFilePath,baos)
            withFormat{
                json{
                    render(contentType:'application/json'){
                        apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                    }
                }
                xml{
                    render(contentType: 'application/xml'){
                        apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                    }

                }
            }
        }
    }



    private def renderProjectAclHref(String project,String path) {
        createLink(absolute: true, uri: "/api/${ApiRequestFilters.API_CURRENT_VERSION}/project/$project/acl/$path")
    }

    /**
     * Get resource or dir listing for the specified project path
     * @param project project
     * @param projectFilePath path for the project file or dir
     * @param rmprefix prefix string for the path, to be removed from paths in dir listings
     * @return
     */
    private def apiProjectAclsGetResource(IRundeckProject project,String projectFilePath,String rmprefix) {
        def respFormat = apiService.extractResponseFormat(request, response, ['yaml','xml','json','text'],request.format)
        if(project.existsFileResource(projectFilePath)){
            if(respFormat in ['yaml','text']){
                //write directly
                response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
                project.loadFileResource(projectFilePath,response.outputStream)
                response.outputStream.close()
            }else{
                //render as json/xml with contents as string
                def baos=new ByteArrayOutputStream()
                project.loadFileResource(projectFilePath,baos)
                withFormat{
                    json{
                        render(contentType:'application/json'){
                            apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                        }
                    }
                    xml{
                        render(contentType: 'application/xml'){
                            apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                        }

                    }
                }
            }
        }else if(project.existsDirResource(projectFilePath) || projectFilePath==rmprefix){
            //list aclpolicy files in the dir
            def list=project.listDirPaths(projectFilePath).findAll{
                it ==~ /.*\.aclpolicy$/
            }
            withFormat{
                json{
                    render(contentType:'application/json'){
                        apiService.jsonRenderDirlist(
                                projectFilePath,
                                {p->apiService.pathRmPrefix(p,rmprefix)},
                                {p->renderProjectAclHref(project.getName(),apiService.pathRmPrefix(p,rmprefix))},
                                list,
                                delegate
                        )
                    }
                }
                xml{
                    render(contentType: 'application/xml'){
                        apiService.xmlRenderDirList(
                                projectFilePath,
                                {p->apiService.pathRmPrefix(p,rmprefix)},
                                {p->renderProjectAclHref(project.getName(),apiService.pathRmPrefix(p,rmprefix))},
                                list,
                                delegate
                        )
                    }

                }
            }
        }else{

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.path],
                    format: respFormat
            ])
        }

    }
    private def apiProjectAclsDeleteResource(IRundeckProject project,projectFilePath) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)

        def exists = project.existsFileResource(projectFilePath)
        if(!exists){

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code  : 'api.error.item.doesnotexist',
                    args  : ['Project ACL Policy File', params.path + ' for project ' + project.name],
                    format: respFormat
            ])
        }
        boolean done=project.deleteFileResource(projectFilePath)
        if(!done){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    message: "error",
                    format: respFormat
            ])
        }
        render(status: HttpServletResponse.SC_NO_CONTENT)
    }
    def apiProjectFilePut() {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V13)) {
            return
        }
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)
        if(!(params.filename in ['readme.md','motd.md'])){

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.filename],
                    format:respFormat
            ])
        }

        def error = null
        String text = null
        if (request.format in ['text']) {
            try {
                text = request.inputStream.text
            } catch (Throwable e) {
                error = e.message
            }
        }else{
            def succeeded = apiService.parseJsonXmlWith(request,response,[
                    xml:{xml->
                        if(xml?.name()=='contents'){
                            text=xml?.text()
                        }else{
                            text = xml?.contents[0]?.text()
                        }
                    },
                    json:{json->
                        text = json?.contents
                    }
            ])
            if(!succeeded){
                error= "unexpected format: ${request.format}"
            }
        }
        if(error){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: error,
                    format: respFormat
            ])
        }

        if(!text){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: "No content",
                    format: respFormat
            ])
        }

        project.storeFileResource(params.filename,new ByteArrayInputStream(text.bytes))

        if(respFormat in ['text']){
            //write directly
            response.setContentType("text/plain")
            project.loadFileResource(params.filename,response.outputStream)
            response.outputStream.close()
        }else{

            def baos=new ByteArrayOutputStream()
            project.loadFileResource(params.filename,baos)
            renderProjectFile(baos.toString(),request,response, respFormat)
        }
    }

    private def renderProjectFile(
            String contentString,
            HttpServletRequest request,
            HttpServletResponse response,
            String respFormat
    )
    {
        if (respFormat=='json') {
            render(contentType: 'application/json') {
                contents = contentString
            }
        }else{
            apiService.renderSuccessXml(request, response) {
                delegate.'contents' {
                    mkp.yieldUnescaped("<![CDATA[" + contentString.replaceAll(']]>', ']]]]><![CDATA[>') + "]]>")
                }
            }
        }
    }
    def apiProjectFileGet() {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V13)) {
            return
        }
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],'text')
        if(!(params.filename in ['readme.md','motd.md'])){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.filename],
                    format:respFormat
            ])
        }
        if(!project.existsFileResource(params.filename)){

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.filename],
                    format: respFormat
            ])
        }

        if(respFormat in ['text']){
            //write directly
            response.setContentType("text/plain")
            project.loadFileResource(params.filename,response.outputStream)
            response.outputStream.close()
        }else{

            def baos=new ByteArrayOutputStream()
            project.loadFileResource(params.filename,baos)
            renderProjectFile(baos.toString(),request,response, respFormat)
        }
    }
    def apiProjectFileDelete() {
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V13)) {
            return
        }
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'])
        if(!(params.filename in ['readme.md','motd.md'])){

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.filename],
                    format:respFormat
            ])
        }

        boolean done=project.deleteFileResource(params.filename)
        if(!done){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    message: "error",
                    format: respFormat
            ])
        }
        render(status: HttpServletResponse.SC_NO_CONTENT)
    }
    def apiProjectConfigPut() {
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json', 'text'])
        //parse config data
        def config=null
        def configProps=new Properties()
        if (request.format in ['text']) {
            def error=null
            try{
                configProps.load(request.inputStream)
            }catch (Throwable t){
                error=t.message
            }
            if(error){
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        message: error,
                        format: respFormat
                ])
            }
        }else{
            def succeed = apiService.parseJsonXmlWith(request, response, [
                    xml: { xml ->
                        config = [:]
                        xml?.property?.each {
                            config[it.'@key'.text()] = it.'@value'.text()
                        }
                    },
                    json: { json ->
                        config = json
                    }
            ])
            if(!succeed){
                return
            }
            configProps.putAll(config)
        }
        def result=frameworkService.setFrameworkProjectConfig(project.name,configProps)
        if(!result.success){
            return apiService.renderErrorFormat(response,[
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message:result.error,
                    format:respFormat
            ])
        }

        switch (respFormat) {
            case 'text':
                render(contentType: 'text/plain',text: project.propertyFile.text)
                break
            case 'xml':
                apiService.renderSuccessXml(request, response) {
                    renderApiProjectConfigXml(project, delegate)
                }
                break
            case 'json':
                render(contentType: 'application/json') {
                    renderApiProjectConfigJson(project, delegate)
                }
                break
        }

    }
    def apiProjectConfigKeyGet() {
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def key = apiService.restoreUriPath(request, params.keypath)
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json','text'],'text')
        def properties = frameworkService.loadProjectProperties(project)
        if(null==properties.get(key)){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['property',key],
                    format:respFormat
            ])
        }
        def value = properties.get(key)
        switch (respFormat) {
            case 'text':
                render (contentType: 'text/plain', text: value)
                break
            case 'xml':
                apiService.renderSuccessXml(request, response) {
                    property(key:key,value:value)
                }
                break
            case 'json':
                render(contentType: 'application/json') {
                    delegate.'key'=key
                    delegate.'value'= value
                }
                break
        }
    }
    def apiProjectConfigKeyPut() {
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def key = apiService.restoreUriPath(request, params.keypath)
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json', 'text'])
        def value=null
        if(request.format in ['text']){
           value = request.inputStream.text
        }else{
            def succeeded = apiService.parseJsonXmlWith(request,response,[
                    xml:{xml->
                        value = xml?.'@value'?.text()
                    },
                    json:{json->
                        value = json?.value
                    }
            ])
            if(!succeeded){
                return
            }
        }
        if(!value){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_BAD_REQUEST,
                    code:'api.error.invalid.request',
                    args:["value was not specified"],
                    format:respFormat
            ])
        }

        def result=frameworkService.updateFrameworkProjectConfig(project.name,new Properties([(key): value]),null)
        if(!result.success){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message:result.error,
                    format: respFormat
            ])
        }
        def properties = frameworkService.loadProjectProperties(project)
        def resultValue= properties.get(key)

        switch (respFormat) {
            case 'text':
                render(contentType: 'text/plain', text: resultValue)
                break
            case 'xml':
                apiService.renderSuccessXml(request, response) {
                    property(key: key, value: resultValue)
                }
                break
            case 'json':
                render(contentType: 'application/json') {
                    delegate.'key' = key
                    delegate.'value' = resultValue
                }
                break
        }
    }
    def apiProjectConfigKeyDelete() {
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if (!project) {
            return
        }
        def key = apiService.restoreUriPath(request, params.keypath)
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json','text'],'xml')

        def result=frameworkService.removeFrameworkProjectConfigProperties(project.name,[key] as Set)
        if (!result.success) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: result.error,
                    format: respFormat
            ])
        }
        render(status: HttpServletResponse.SC_NO_CONTENT)
    }

    def apiProjectExport(){
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_EXPORT)
        if (!project) {
            return
        }
        def framework = frameworkService.rundeckFramework
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        def dateStamp = dateFormater.format(new Date());
        response.setContentType("application/zip")
        response.setHeader("Content-Disposition", "attachment; filename=\"${project.name}-${dateStamp}.rdproject.jar\"")

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def aclReadAuth=frameworkService.authorizeApplicationResourceAny(authContext,
                                                                         frameworkService.authResourceForProjectAcl(project.name),
                                                                         [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN])
        ArchiveOptions options=new ArchiveOptions(all: true)
        if(params.executionIds){
            options.all=false
            options.executionsOnly=true
            options.parseExecutionsIds(params.executionIds)
        }
        projectService.exportProjectToOutputStream(project, framework,response.outputStream,null,aclReadAuth,options)
    }

    def apiProjectImport(ProjectArchiveParams archiveParams){
        if (!apiService.requireApi(request, response)) {
            return
        }
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_IMPORT)
        if (!project) {
            return
        }
        if(!apiService.requireRequestFormat(request,response,['application/zip'])){
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'], 'xml')
        if(archiveParams.hasErrors()){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request',
                    args: [archiveParams.errors.allErrors.collect{g.message(error: it)}.join("; ")],
                    format:respFormat
            ])
        }
        def framework = frameworkService.rundeckFramework

        AuthContext appContext = frameworkService.getAuthContextForSubject(session.subject)
        //uploaded file

        //verify acl access requirement
        if (archiveParams.importACL &&
                !frameworkService.authorizeApplicationResourceAny(
                        appContext,
                        frameworkService.authResourceForProjectAcl(project.name),
                        [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]
                )
        ) {

            apiService.renderErrorFormat(response,
                                         [
                                                 status: HttpServletResponse.SC_FORBIDDEN,
                                                 code  : "api.error.item.unauthorized",
                                                 args  : [AuthConstants.ACTION_CREATE, "ACL for Project", project]
                                         ]
            )
            return null
        }
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        def stream = request.getInputStream()
        def len = request.getContentLength()
        if(0==len){
            return apiService.renderErrorFormat(response,[
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request',
                    args: ['No content'],
                    format:respFormat
            ])
        }


        def result = projectService.importToProject(
                project,
                framework,
                authContext,
                stream,
                archiveParams
        )
        switch (respFormat) {
            case 'json':
                render(contentType: 'application/json'){
                    import_status=result.success?'successful':'failed'
                    if (!result.success) {
                        //list errors
                        delegate.'errors'=result.joberrors
                    }

                    if(result.execerrors){
                        delegate.'execution_errors'=result.execerrors
                    }

                    if(result.aclerrors){
                        delegate.'acl_errors'=result.aclerrors
                    }
                }
                break;
            case 'xml':
                apiService.renderSuccessXml(request, response) {
                    delegate.'import'(status: result.success ? 'successful' : 'failed'){
                        if(!result.success){
                            //list errors
                            delegate.'errors'(count: result.joberrors.size()){
                                result.joberrors.each{
                                    delegate.'error'(it)
                                }
                            }
                        }
                        if(result.execerrors){
                            delegate.'executionErrors'(count: result.execerrors.size()){
                                result.execerrors.each{
                                    delegate.'error'(it)
                                }
                            }
                        }
                        if(result.aclerrors){
                            delegate.'aclErrors'(count: result.aclerrors.size()){
                                result.aclerrors.each{
                                    delegate.'error'(it)
                                }
                            }
                        }
                    }
                }
                break;

        }
    }
}
