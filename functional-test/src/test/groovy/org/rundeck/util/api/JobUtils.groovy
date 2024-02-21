package org.rundeck.util.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.Execution
import org.rundeck.tests.functional.api.ResponseModels.Job
import org.rundeck.util.container.RdClient

import java.util.concurrent.TimeUnit

class JobUtils {

    static def executeJobWithArgs = (String jobId, RdClient client, String args) -> {
        return client.doPostWithoutBody("/job/${jobId}/run?argString=${args}")
    }

    static executeJobWithArgsInvalidMethod = (String jobId, RdClient client, String args) -> {
        return client.doGetAcceptAll("/job/${jobId}/run?argString=${args}")
    }

    static executeJobLaterWithArgsAndRuntime = (
            String jobId,
            RdClient client,
            String args,
            String runtime) -> {
        return client.doPostWithoutBody("/job/${jobId}/run?argString=${args}&runAtTime=${runtime}")
    }

    static def executeJobWithOptions = (jobId, RdClient client, Object options) -> {
        return client.doPost("/job/${jobId}/run", options)
    }

    static executeJobLaterWithArgsInvalidMethod = (
            String jobId,
            RdClient client,
            String args,
            String runtime) -> {
        return client.doGetAcceptAll("/job/${jobId}/run?argString=${args}&runAtTime=${runtime}")
    }

    static def executeJob = (jobId, RdClient client) -> {
        return client.doPost("/job/${jobId}/run", "{}")
    }

    static def createJob(
            final String project,
            final String jobDefinitionXml,
            RdClient client
    ) {
        final String CREATE_JOB_ENDPOINT = "/project/${project}/jobs/import"
        return client.doPostWithRawText(CREATE_JOB_ENDPOINT, "application/xml", jobDefinitionXml)
    }

    static def generateExecForEnabledXmlTest(String jobName){
        return  "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    static def generateScheduledJobsXml(String jobname){
        return "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobname}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <schedule>\n" +
                "        <time hour='*' seconds='*' minute='0/20' />\n" +
                "        <month month='*' />\n" +
                "        <year year='*' />\n" +
                "      </schedule>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    static Job getJobDetailsById(final String jobId, final ObjectMapper mapper, RdClient client){
        def jobInfo = client.doGetAcceptAll("/job/${jobId}")
        List<Job> jobs = mapper.readValue(jobInfo.body().string(), mapper.getTypeFactory().constructCollectionType(List.class, Job.class))
        return jobs[0]
    }

    static Execution waitForExecutionToBe(
            String state,
            String executionId,
            ObjectMapper mapper,
            RdClient client,
            int iterationGap,
            int timeout
    ){
        Execution executionStatus
        def execDetail = client.doGet("/execution/${executionId}")
        executionStatus = mapper.readValue(execDetail.body().string(), Execution.class)
        long initTime = System.currentTimeMillis()
        while(executionStatus.status != state){
            if ((System.currentTimeMillis() - initTime) >= TimeUnit.SECONDS.toMillis(timeout)) {
                throw new InterruptedException("Timeout reached (${timeout} seconds).")
            }
            def transientExecutionResponse = client.doGet("/execution/${executionId}")
            executionStatus = mapper.readValue(transientExecutionResponse.body().string(), Execution.class)
            if( executionStatus.status == state ) break
            Thread.sleep(iterationGap)
        }
        return executionStatus
    }

    /**
     * Generates a temporary file containing the provided job definition and returns its path.
     *
     * @param jobDefinition The job definition content to be written to the temporary file.
     * @param format The format of the job definition content.
     * @return The path of the generated temporary file.
     */
    static def generateFileToImport(String jobDefinition, String format) {
        def tempFile = File.createTempFile("temp", ".${format}")
        tempFile.text = jobDefinition
        tempFile.deleteOnExit()
        tempFile.path
    }


    /**
     * Takes the job file and replaces all the words that start with 'xml-' using the provided args to upload it to the desired project.
     *
     * @param fileName The name of the file to import into test-files resources dir.
     * @param args     Optional arguments as a Map. If not provided, default values will be used.
     *                 Available keys:
     *                 - "project-name": Name of the project (default: PROJECT_NAME)
     *                 - "job-name": Name of the job (default: "job-test")
     *                 - "job-group-name": Name of the job group (default: "group-test")
     *                 - "job-description-name": Name of the job description (default: "description-test")
     *                 - "args": Arguments (default: "echo hello there")
     *                 - "2-args": Secondary arguments (default: "echo hello there 2")
     *                 - "uuid": UUID for the job (default: generated UUID)
     * @return The path of the updated temporary XML file.
     */
    static def updateJobFileToImport = (String fileName, String projectName, Map args = [:]) -> {
        if (args.isEmpty()) {
            args = [
                    "project-name": projectName,
                    "job-name": "job-test",
                    "job-group-name": "group-test",
                    "job-description-name": "description-test",
                    "args": "echo hello there",
                    "2-args": "echo hello there 2",
                    "uuid": UUID.randomUUID().toString()
            ]
        }
        def pathXmlFile = getClass().getResource("/test-files/${fileName}").getPath()
        def xmlProjectContent = new File(pathXmlFile).text
        args.each { k, v ->
            xmlProjectContent = xmlProjectContent.replaceAll("xml-${k as String}", v as String)
        }
        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlProjectContent
        tempFile.deleteOnExit()
        tempFile.path
    }

    /**
     * Imports a job file into a specified project.
     * This method posts the XML job file to the server for the specified or default project name.
     *
     * @param projectName The name of the project into which the job file is to be imported.
     *                    If null, a default project name (PROJECT_NAME) is used.
     * @param pathXmlFile The file path of the XML job file to be imported.
     * @return A Map representation of the JSON response body if the import is successful.
     *         The method checks for a successful response and a 200 HTTP status code.
     * @throws IllegalArgumentException if the pathXmlFile parameter is not provided.
     */
    static def jobImportFile = (String projectName, String filePath, RdClient client) -> {
        URL resourceUrl = getClass().getResource(filePath)
        def pathXmlFile = resourceUrl ? resourceUrl.getPath() : filePath
        def responseImport = client.doPost("/project/${projectName}/jobs/import", new File(pathXmlFile), "application/xml")
        responseImport.successful
        responseImport.code() == 200

        return new ObjectMapper().readValue(responseImport.body().string(), Map.class)
    }
}
