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
    static void createProjectsWithJobsScheduled(String suffixProjectName, int projectsCount, int jobsCountsPerProject, RdClient client) {
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
                Thread.sleep(2000)
            }
        }
    }

    /**
     * Retrieves the count of running executions for a given project, waiting until the count exceeds a specified value or a timeout occurs.
     *
     * @param projectName The name of the project to query. Must not be null.
     * @param valueMoreThan The threshold value for the count of running executions. Must be greater than zero.
     * @param client The RdClient instance used to make HTTP requests. Must not be null.
     * @return True if the count of running executions exceeds the specified value within the timeout period, false otherwise.
     * @throws RuntimeException if fetching running executions fails or if a timeout occurs.
     */
    static def projectCountExecutions = (String projectName, int valueMoreThan, RdClient client) -> {
        def startTime = System.currentTimeMillis()
        def timeout = 10000
        def pollingInterval = 1000
        while (true) {
            def response = client.doGet("/project/${projectName}/executions/running?includePostponed=true")
            if (!response.successful) {
                throw new RuntimeException("Failed to get running executions: ${response.body().string()}")
            }
            def valueCount = client.jsonValue(response.body(), Map).paging.count
            if (valueCount > valueMoreThan) {
                return Boolean.TRUE
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new RuntimeException("Timeout: No running executions found within ${timeout} milliseconds.")
            }
            sleep pollingInterval
        }
    }

}
