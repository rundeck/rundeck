package rundeck.controllers


import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.services.ProjectServiceException

import java.text.SimpleDateFormat
import org.apache.commons.fileupload.util.Streams
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.zip.ZipInputStream
import com.dtolabs.rundeck.core.authentication.Group

class ProjectController {
    def frameworkService
    def projectService
    def static allowedMethods = [
            importArchive: ['POST'],
    ]

    def index () {
        return redirect(controller: 'menu', action: 'jobs')
    }

    def export={
        def project=params.name
        if (!project){
            request.error = "Project parameter is required"
            return render(template: "/common/error")
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.existsFrameworkProject(project, framework)) {
            request.error = g.message(code: 'scheduledExecution.project.invalid.message', args: [project])
            return render(template: "/common/error")
        }

        if(!frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], [AuthConstants.ACTION_ADMIN])){
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_ADMIN, "Project", params.name])
            return render(template: "/common/error")
        }
        def project1 = frameworkService.getFrameworkProject(project, framework)

        //temp file
        def outfile
        try {
            outfile = projectService.exportProjectToFile(project1,framework)
        } catch (ProjectServiceException exc) {
            request.error = exc.message
            return render(template: "/common/error")
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
        def project = params.name
        if (!project) {
            request.error = "Project parameter is required"
            return render(template: "/common/error")
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.existsFrameworkProject(project, framework)) {
            request.error = g.message(code: 'scheduledExecution.project.invalid.message', args: [project])
            return render(template: "/common/error")
        }

        if (!frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], [AuthConstants.ACTION_ADMIN])) {
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_ADMIN, "Project", params.name])
            return render(template: "/common/error")
        }
        def project1 = frameworkService.getFrameworkProject(project, framework)

        //uploaded file
        if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("zipFile")
            if (!file || file.empty) {
                flash.message = "No file was uploaded."
                return
            }
            String roleList = request.subject.getPrincipals(Group.class).collect {it.name}.join(",")
            def result=projectService.importToProject(project1,session.user,roleList,framework,new ZipInputStream(file.getInputStream()),params.import)

            if(result.success){
                flash.message="Archive successfully imported"
            }else{
                flash.error="Failed to import some jobs"
                flash.joberrors=result.joberrors
            }
            return redirect(controller: 'menu',action: 'admin')
        }
    }
}
