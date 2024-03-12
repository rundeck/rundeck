package org.rundeck.util.common.projects

import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.RdClient

class ProjectUtils {

    /**
     * Creates projects with scheduled jobs.
     *
     * @param suffixProjectName The suffix to be added to project names.
     * @param projectsCount The number of projects to create.
     * @param jobsCountsPerProject The number of jobs to schedule per project.
     * @param client The RdClient object used to interact with the Rundeck API.
     * @throws RuntimeException If failed to create a project.
     */
    public static def createProjectsWithJobsScheduled = (String suffixProjectName, int projectsCount, int jobsCountsPerProject, RdClient client) -> {
        (1..projectsCount).each {it ->
            def projectName = "${suffixProjectName}-${it}".toString()
            def getProject = client.doGet("/project/${projectName}")
            if (getProject.code() == 404) {
                def post = client.doPost("/projects", [name: projectName])
                if (!post.successful) {
                    throw new RuntimeException("Failed to create project: ${post.body().string()}")
                }
            }
            (1..jobsCountsPerProject).each {it2 ->
                def pathFile = JobUtils.updateJobFileToImport("api-test-executions-running-scheduled.xml", projectName)
                def imported = JobUtils.jobImportFile(projectName, pathFile, client)
            }
        }
    }

}
