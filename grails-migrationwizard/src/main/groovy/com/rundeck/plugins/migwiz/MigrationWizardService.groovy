package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.net.api.RundeckClient
import com.dtolabs.rundeck.net.model.ProjectImportStatus
import com.dtolabs.rundeck.net.model.ProjectInfo
import com.rundeck.plugins.migwiz.rba.RBAInstanceData
import groovy.util.logging.Slf4j
import okhttp3.RequestBody
import org.rundeck.core.projects.ProjectArchiver
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class MigrationWizardService {

    @Autowired
    ProjectArchiver projectArchiver

    @Autowired
    IFramework framework

    // TODO Validate jobs are RBA compliant and build a report.

    // TODO Create Runner in RBA
    // TODO Convert jobs to use Runner.


    /**
     * Imports a local project to a cloud Runbook Automation instance.
     * @param projectName
     * @param instance
     * @param authContext
     * @return
     */
    ProjectImportStatus migrateProjectToRBA(String projectName, RBAInstanceData instance, AuthContext authContext) {

        File archiveFile;

        try {
            archiveFile = createTempArchive(projectName)
            archiveFile.deleteOnExit()

            def project = framework.getFrameworkProjectMgr().getFrameworkProject(projectName)

            def options = [
                project         : projectName,
                exportAll       : false,
                exportJobs      : true,
                exportExecutions: false,
                exportConfigs   : true,
                exportReadmes   : true,
                exportAcls      : true,
                exportScm       : true,
                preserveuuid    : true,
                // TODO: exportComponents
            ]

            // Create the archive
            archiveFile.withOutputStream { outputStream ->
                projectArchiver.exportProjectArchiveToOutputStream(
                    project,
                    framework,
                    outputStream,
                    options,
                    authContext
                )
            }

            // Project Config
            RundeckClient rundeckClient = new RundeckClient(instance.url, instance.token)

            def projectExists = rundeckClient.getProject(projectName)
            if (!projectExists.successful) {

                def projectResponse = rundeckClient.createProject(ProjectInfo.builder()
                    .name(projectName)
                    .build()
                )

                if (!projectResponse.successful) {
                    throw new IllegalStateException("Failed to create project: ${projectResponse.errorBody().string()}")
                }

            }

            // Send archive
            def archiveResponse = rundeckClient.importProjectArchive(
                projectName,
                "preserve",
                false,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                [:],
                RequestBody.create(archiveFile, RundeckClient.MEDIA_TYPE_ZIP)
            )

            if (!archiveResponse.successful) {
                throw new IllegalStateException("Failed to import project archive: ${archiveResponse.errorBody()}")
            }

            def archiveStatus = archiveResponse.body()
            if (!archiveStatus.successful) {
                throw new IllegalStateException("Failed to import project archive: ${archiveStatus.toString()}")
            }

            return archiveStatus
        }
        finally {
            if (archiveFile) {
                archiveFile.delete()
            }
        }
    }

    /**
     * Create a temp file fir a project.
     */
    File createTempArchive(String projectName) {
//        def f = new File("/Users/alberto/IdeaProjects/RundeckCore/tmp/mw/export-${projectName}.jar")
//        return f
        return File.createTempFile("export-${projectName}", ".jar")
    }

}
