package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.filters.ApiRequestFilters
import rundeck.services.ProjectServiceException

import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import org.apache.commons.fileupload.util.Streams
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.zip.ZipInputStream
import com.dtolabs.rundeck.core.authentication.Group

class ProjectController extends ControllerBase{
    def frameworkService
    def projectService
    def apiService
    def static allowedMethods = [
            importArchive: ['POST'],
            delete: ['POST'],
            apiProjectImport:['PUT']
    ]

    def index () {
        return redirect(controller: 'menu', action: 'jobs')
    }

    def export={
        def project=params.project?:params.name
        if (!project){
            return renderErrorView("Project parameter is required")
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

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

        //temp file
        def outfile
        try {
            outfile = projectService.exportProjectToFile(project1,framework)
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

    def importArchive={
        def project = params.project?:params.name
        if (!project) {
            return renderErrorView("Project parameter is required")
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

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

        def project1 = frameworkService.getFrameworkProject(project)

        //uploaded file
        if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("zipFile")
            if (!file || file.empty) {
                flash.message = "No file was uploaded."
                return
            }
            String roleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
            def result=projectService.importToProject(project1,session.user,roleList,framework,authContext, file.getInputStream(),params.import)

            if(result.success){
                flash.message="Archive successfully imported"
            }else{
                flash.error="Failed to import some jobs"
                flash.joberrors=result.joberrors
            }
            return redirect(controller: 'menu',action: 'admin',params:[project:project])
        }
    }

    def delete = {
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

        def result = projectService.deleteProject(project1, framework)
        if (!result.success) {
            log.error("Failed to delete project: ${result.error}")
            flash.error = result.error
            return redirect(controller: 'menu', action: 'admin', params: [project: project])
        }
        flash.message = 'Deleted project: ' + project
        return redirect(controller: 'menu', action: 'home')
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

        def result = projectService.deleteProject(project1, framework)
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
     * support project/NAME/config endpoints: validate project and appropriate authorization,
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
    def apiProjectConfigGet(){
        def proj=validateProjectConfigApiRequest(AuthConstants.ACTION_CONFIGURE)
        if(!proj){
            return
        }
        //render project config only

        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json','text'], 'xml')
        switch (respFormat) {
            case 'text':
                render(contentType: 'text/plain') {
                    response.outputStream<< proj.propertyFile.text
                }
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
            log.error("parsed: ${configProps}")
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
        response.setHeader("Content-Disposition", "attachment; filename=\"${project}-${dateStamp}.rdproject.jar\"")

        projectService.exportProjectToOutputStream(project, framework,response.outputStream)
    }

    def apiProjectImport(){
        def project = validateProjectConfigApiRequest(AuthConstants.ACTION_IMPORT)
        if (!project) {
            return
        }
        if(!apiService.requireRequestFormat(request,response,['application/zip'])){
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'], 'xml')
        def framework = frameworkService.rundeckFramework
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        //uploaded file
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

        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

        def importOptions = [
                executionImportBehavior: Boolean.parseBoolean(params.importExecutions?:'true') ? 'import' : 'skip',
                jobUUIDBehavior: params.jobUuidOption?:'preserve'
        ]
        def result = projectService.importToProject(project, session.user, roleList, framework, authContext,
                stream, importOptions)
        switch (respFormat){
            case 'json':
                render(contentType: 'application/json'){
                    import_status=result.success?'successful':'failed'
                    if (!result.success) {
                        //list errors
                        delegate.'errors'=result.joberrors
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
                    }
                }
                break;

        }
    }
}
