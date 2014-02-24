package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
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
                frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project', name: project],
                        [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project',project)) {
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
                frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project', name: project],
                        [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project', project)) {
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
            def result=projectService.importToProject(project1,session.user,roleList,framework,authContext,new ZipInputStream(file.getInputStream()),params.import)

            if(result.success){
                flash.message="Archive successfully imported"
            }else{
                flash.error="Failed to import some jobs"
                flash.joberrors=result.joberrors
            }
            return redirect(controller: 'menu',action: 'admin',params:[project:project])
        }
    }
    /**
     * Render project info result using a builder
     */
    def renderApiProject = { pject, delegate ->
        delegate.'project'(href: generateProjectApiUrl(pject.name)) {
            name(pject.name)
            description(pject.hasProperty('project.description') ? pject.getProperty('project.description') : '')
            if (pject.hasProperty("project.resources.url")) {
                resources {
                    providerURL(pject.getProperty("project.resources.url"))
                }
            }
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
        return apiService.renderSuccessXml(response) {
            delegate.'projects'(count: projlist.size()) {
                projlist.each { pject ->
                    renderApiProject(pject, delegate)
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
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        if (!frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project', name: params.project], ['read'])) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read', 'Project', params.project]])
        }
        def exists = frameworkService.existsFrameworkProject(params.project)
        if (!exists) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])
        }
        def pject = frameworkService.getFrameworkProject(params.project)
        return apiService.renderSuccessXml(response) {
            delegate.'projects'(count: 1) {
                renderApiProject(pject, delegate)
            }
        }
    }

    def apiProjectCreate() {

    }
    def apiProjectDelete(){

    }
    def apiProjectConfig(){

    }
}
