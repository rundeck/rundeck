package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.rundeck.plugins.migwiz.rba.RBAInstanceData
import org.rundeck.core.projects.ProjectArchiver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MigrationWizardService {

    @Autowired
    ProjectArchiver projectArchiver

    @Autowired
    IFramework framework

    // Validate jobs are RBA compliant and build a report.


    // Create export archive
    // Collect all jobs
    // Convert jobs to use Runner.

    // Create Project in RBA
    // Create Runner in RBA
    // Obtain archive with converted jobs.
    // Send archive.

    void migrateProjectToRBA(String project, RBAInstanceData instance, AuthContext authContext) {

        def projectObject = framework.getFrameworkProjectMgr().getFrameworkProject(project)

        def outputStream = new ByteArrayOutputStream()
        def options = [
            exportAll: true,
            exportJobs: true,
            exportExecutions: false,
            exportConfigs: true,
            exportReadmes: true,
            exportAcls: true,
            exportScm: true,
            stripJobRef: false,
            preserveuuid: true,
        ]


        projectArchiver.exportProjectArchiveToOutputStream(
            projectObject,
            framework,
            outputStream,
            options,
            authContext
        )


        // Validate instance is reachable
        // Create the archive
        // Create project in RBA
        // Send archive

    }

}
