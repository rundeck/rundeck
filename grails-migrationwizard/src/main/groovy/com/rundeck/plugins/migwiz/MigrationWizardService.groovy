package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.net.api.RundeckClient
import com.dtolabs.rundeck.net.model.ProjectImportStatus
import com.dtolabs.rundeck.net.model.ProjectInfo
import com.rundeck.plugins.migwiz.rba.RBAInstanceData
import okhttp3.RequestBody
import org.rundeck.core.projects.ProjectArchiver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MigrationWizardService {

    private static final RBA_BASE_DOMAIN = "stg.runbook.pagerduty.cloud"

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
            archiveFile = File.createTempFile("export-${projectName}", ".jar")
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


            RundeckClient rundeckClient = new RundeckClient(instance.url, instance.token)

            def projectResponse = rundeckClient.createProject(ProjectInfo.builder()
                .name(projectName)
                .build()
            )

            if(!projectResponse.successful) {
                throw new IllegalStateException("Failed to create project: ${projectResponse.errorBody()}")
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

            return archiveResponse.body()
        }
        finally {
            if(archiveFile) {
                archiveFile.delete()
            }
        }
    }

}
