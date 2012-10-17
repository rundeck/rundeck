

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import java.util.zip.ZipOutputStream
import ProjectServiceException
import java.text.SimpleDateFormat
import org.apache.commons.fileupload.util.Streams
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.zip.ZipInputStream
import com.dtolabs.rundeck.core.authentication.Group
import java.util.jar.JarOutputStream

class ProjectController {
    def frameworkService
    def projectService

    def index (){
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

        if(!frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], [AuthConstants.ACTION_READ])){
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_READ, "Project", params.id])
            return render(template: "/common/error")
        }
        def project1 = frameworkService.getFrameworkProject(project, framework)

        //temp file
        def outfile= projectService.exportProjectToFile(project1,framework)
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

        if (!frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], [AuthConstants.ACTION_READ])) {
            response.setStatus(403)
            request.error = g.message(code: 'api.error.item.unauthorized', args: [AuthConstants.ACTION_READ, "Project", params.id])
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
            projectService.importToProject(project1,session.user,roleList,framework,new ZipInputStream(file.getInputStream()))

            flash.message="Archive successfully imported"
            return redirect(controller: 'menu',action: 'admin')
        }
    }
}
