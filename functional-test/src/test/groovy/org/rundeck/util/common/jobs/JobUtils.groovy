package org.rundeck.util.common.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import okhttp3.Headers
import okhttp3.Response
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.api.responses.jobs.Job
import org.rundeck.util.api.responses.jobs.JobDetails
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.common.WaitUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.container.RdClient

import java.time.Duration
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function

@Slf4j
class JobUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()

    static final String DUPE_OPTION_SKIP = "skip"

    static final String DUPE_OPTION_UPDATE = "update"

    static final String DUPE_OPTION_DEFAULT = "create"

    static final String CONTENT_TYPE_DEFAULT = "application/xml"

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

    /**
     * Creates a job from a job definition.
     * @param project name of the project to create the job in.
     * @param jobDefinition the definition of the job in the format defined by `contentType`.
     * @param client rundeck client to use for the request.
     * @param (Optional) contentType of the job definition. Defaults to 'application/xml'.
     * @param (Optional) failedJobsHandler handles the scenario when the job creation has failed. Defaults to throwing an exception.
     * @return
     */
    static CreateJobResponse createJob(
            final String project,
            final String jobDefinition,
            RdClient client,
            String contentType = 'application/xml',
            Consumer<CreateJobResponse> failedJobsHandler = { List<Object> failedJobs -> throw new Exception("Some jobs failed on import: " + failedJobs) } ) {
        final String CREATE_JOB_ENDPOINT = "/project/${project}/jobs/import"
        Response responseImport = client.doPostWithRawText(CREATE_JOB_ENDPOINT, contentType, jobDefinition)

        // Throws an exception if the import failed
        if (responseImport.code() != 200) {
            throw new IllegalArgumentException("Job import failed: ${responseImport} with body: ${responseImport?.body()?.string()}");
        }

        def data = OBJECT_MAPPER.readValue(responseImport.body().string(), CreateJobResponse.class)
        validateJobsImportAllSuccess(data, failedJobsHandler)
        return data
    }

    static def generateScheduledExecutionXml(String jobName){
        return  "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <multipleExecutions>true</multipleExecutions>\n" +
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

    static def generateScheduledJobsXml(String jobname, String schedule = "<time hour='*' seconds='*' minute='0/20' />", ZoneId tz = ZoneId.systemDefault()){
        return "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobname}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <multipleExecutions>true</multipleExecutions>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <schedule>\n" +
                "        ${schedule}\n" +
                "        <month month='*' />\n" +
                "        <year year='*' />\n" +
                "      </schedule>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "      <timeZone>${tz.toString()}</timeZone>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    static JobDetails getJobDetailsById(final String jobId, final ObjectMapper mapper, RdClient client){
        def jobInfo = client.doGetAcceptAll("/job/${jobId}")
        List<JobDetails> jobs = mapper.readValue(jobInfo.body().string(), mapper.getTypeFactory().constructCollectionType(List.class, JobDetails.class))
        return jobs[0]
    }


    /**
     * Waits for the execution to be in the expected state.
     *
     * @param expectedStates The expected state.
     * @param executionId The execution ID to query.
     * @param client The RdClient instance to perform the HTTP request.
     * @param timeout The maximum duration to wait for the execution to reach the expected state.
     * @param checkPeriod The time to wait between each check.
     * @return The Execution object representing the execution status.
     * @throws InterruptedException if the timeout is reached before the execution reaches the expected state.
     */
    static Execution waitForExecution(
        String state,
        String executionId,
        RdClient client,
        Duration timeout = WaitingTime.MODERATE,
        Duration checkPeriod = WaitingTime.LOW
    ) {
        return waitForExecution([state], executionId, client, timeout, checkPeriod)
    }

    /**
     * Waits for the execution to be in one of the expected states.
     *
     * @param expectedStates A list of expected states.
     * @param executionId The execution ID to query.
     * @param client The RdClient instance to perform the HTTP request.
     * @param timeout The maximum duration to wait for the execution to reach the expected state.
     * @param checkPeriod The time to wait between each check.
     * @return The Execution object representing the execution status.
     * @throws InterruptedException if the timeout is reached before the execution reaches the expected state.
     */
    @TypeChecked
    static Execution waitForExecution(
        Collection<String> expectedStates,
        String executionId,
        RdClient client,
        Duration timeout = WaitingTime.MODERATE,
        Duration checkPeriod = WaitingTime.LOW
    ) {
        Function<Execution, Boolean> resourceAcceptanceEvaluator = { Execution e -> expectedStates.contains(e?.status) }

        Closure<String> acceptanceFailureOutputProducer = { String id ->
            def execOutput = callSilently { getExecutionOutputText(id, client) }
            return "Execution output was: \n${execOutput}\n".toString()
        }

        return WaitUtils.waitForResource(executionId,
                { String id -> ExecutionUtils.Retrievers.executionById(client, id).get() },
                resourceAcceptanceEvaluator,
                acceptanceFailureOutputProducer,
                timeout,
                checkPeriod)
    }

    /**
     * Waits for the execution to be in one of the expected states.
     *
     * @param executionIds The execution IDs to wait for.
     * @param expectedStates A list of expected states.
     * @param client The RdClient instance to perform the HTTP request.
     * @param timeout The maximum duration to wait for the execution to reach the expected state.
     * @param checkPeriod The time to wait between each check.
     * @return The Execution object representing the execution status.
     * @throws InterruptedException if the timeout is reached before the execution reaches the expected state.
     */
    @TypeChecked
    static Map<String, Execution> waitForManyExecutionToFinish(
            Collection<String> executionIds,
            RdClient client,
            Duration timeout = WaitingTime.MODERATE,
            Duration checkPeriod = WaitingTime.LOW) {
        return WaitUtils.waitForAllResources(executionIds,
                { String id -> ExecutionUtils.Retrievers.executionById(client, id).get() },
                ExecutionUtils.Verifiers.executionFinished() as Function<Execution, Boolean>,
                timeout,
                checkPeriod)
    }

    /**
     * Calls the provided closure and returns its result, discarding any exception that may occur.
     * @param closure The closure to call.
     * @return The result of the closure, or null if an exception occurred.
     */
    private static <T> T callSilently(Closure<T> closure) {
        try {
            return closure.call()
        } catch (Exception e) {
            log.error("Error calling closure: ${e.message}", e)
            return null
        }
    }

    /**
     * Retrieves the execution output for the specified execution ID.
     * @param execId The execution ID to query.
     * @param client The RdClient instance to perform the HTTP request.
     * @param lastLinesCount The number of last lines to retrieve. Defaults to 0 (unlimited).
     * @return The ExecutionOutput object representing the execution output.
     */
    static ExecutionOutput getExecutionOutput(String execId, RdClient client, int lastLinesCount = 0) {
        def execOutputResponse = client.doGetAcceptAll("/execution/${execId}/output?lastlines=${lastLinesCount}")
        ExecutionOutput execOutput = OBJECT_MAPPER.readValue(execOutputResponse.body().string(), ExecutionOutput.class)
        return execOutput
    }

    /**
     * Retrieves the execution output the specified execution ID as plain a text string.
     * @param execId The execution ID to query.
     * @param client The RdClient instance to perform the HTTP request.
     * @param lastLinesCount The number of last lines to retrieve. Defaults to 0 (unlimited).
     * @return The execution output as a plain text string.
     */
    static String getExecutionOutputText(String execId, RdClient client, int lastLinesCount = 0) {
        def execOutputResponse = client.doGetAddHeaders("/execution/${execId}/output?lastlines=${lastLinesCount}",
            Headers.of("Accept", "text/plain"))
        return execOutputResponse.body().string()
    }


    static ScmJobStatusResponse waitForJobStatusToBe(
            String jobId,
            GitScmApiClient gitScmApiClient,
            int iterationGap,
            int timeout
    ) {
        ScmJobStatusResponse jobStatus;
        long initTime = System.currentTimeMillis();
        jobStatus = gitScmApiClient.callGetJobStatus(jobId).response;

        while (jobStatus.commit == null) {
            jobStatus = gitScmApiClient.callGetJobStatus(jobId).response;
            if ((System.currentTimeMillis() - initTime) >= TimeUnit.SECONDS.toMillis(timeout)) {
                throw new InterruptedException("Timeout reached (${timeout} seconds), the job SCM status was empty.");
            }

            Thread.sleep(iterationGap);
        }

        return jobStatus;
    }

    /**
     *  Deletes the job or throws on failure
     * @param jobId
     * @param client
     */
    static void deleteJob(String jobId, RdClient client) {
        def response = client.doDelete("/job/$jobId")
        if (!response.isSuccessful()) {
            throw new IllegalArgumentException("API call was unsuccessful: ${response}  with body: ${response.body().string()}")
        }
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
     * Takes the job template file and does textual replacement for the words that start with the 'xml-'  prefix.
     * The default replacement arguments are used unless an overriden value is provided.
     *
     * @param fileName The name of the template file from the `test-files` resources dir.
     * @param projectName project name to be used in the job file. Can be overriden in the argsOverrides.
     * @param argsOverrides  argument keys-values that override the defaults.
     *                 Available keys:
     *                 - "project-name": Name of the project (default: projectName parameter)
     *                 - "job-name": Name of the job (default: "job-test")
     *                 - "job-group-name": Name of the job group (default: "group-test")
     *                 - "job-description-name": Name of the job description (default: "description-test")
     *                 - "args": Arguments (default: "echo hello there")
     *                 - "2-args": Secondary arguments (default: "echo hello there 2")
     *                 - "uuid": UUID for the job (default: generated UUID)
     *                 - "execution-enabled": Execution enabled flag (default: "true")
     *                 - "schedule-enabled": Schedule enabled flag (default: "true")
     *                 -  "schedule-crontab": Quartz schedule crontab (default: every 4 seconds )
     *                 - "node-filter-include": Node filter include (default: ".*")
     *                 - "node-filter-exclude": Node filter exclude (default: "")
     *                 - "dispatch-rank-order": Dispatch rank order (default: "ascending")
     *                 - "opt1-required": Is job option 1 required (default: "true")
     *                 - "opt2-required": Is job option 2 required (default: "true")
     * @return The path of the updated temporary XML file.
     */
    static def updateJobFileToImport = (String fileName, String projectName, Map argsOverrides = [:]) -> {
        final DEFAULT_ARGS = [
                "project-name": projectName,
                "job-name": "job-test",
                "job-group-name": "group-test",
                "job-description-name": "description-test",
                "args": "echo hello there",
                "2-args": "echo hello there 2",
                "uuid": UUID.randomUUID().toString(),
                "execution-enabled": "true",
                "schedule-enabled": "true",
                "schedule-crontab": "*/4 * * ? * * *",
                "node-filter-include": ".*",
                "ode-filter-exclude": "",
                "dispatch-rank-order": "ascending",
                "opt1-required": "true",
                "opt2-required": "true"
        ].asImmutable()

        // Overrides defaults
        def args = DEFAULT_ARGS + argsOverrides

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
     * Executes the handler if at least one job import fails
     * @param data
     * @param failedJobsHandler
     */
    private static void validateJobsImportAllSuccess(data,
                                                     Consumer<CreateJobResponse> failedJobsHandler = { List<Object> failedJobs -> throw new Exception("Some jobs failed on import: " + failedJobs) } ) {
        if(data?.failed?.size()>0) {
            failedJobsHandler.accept(data?.failed)
        }
    }

    /**
     * Imports an XML job file from a local resource into a specified project.
     *
     * @param projectName The name of the project into which the job file is to be imported.
     * @param resourcePath The resource path
     * @param client      The RdClient object used to perform the HTTP request.
     * @param dupeOption  (Optional) The duplicate option for handling existing jobs. Defaults to DUPE_OPTION_DEFAULT.
     * @param contentType (Optional) The content type of the request. Defaults to CONTENT_TYPE_DEFAULT.
     * @return A Map representation of the JSON response body if the import is successful.
     *         The method checks for a successful response and a 200 HTTP status code.
     * @throws IllegalArgumentException if the imports fails
     */
    static def jobImportFile = (String projectName, String resourcePath, RdClient client, String dupeOption = DUPE_OPTION_DEFAULT, String contentType = CONTENT_TYPE_DEFAULT) -> {
        URL resourceUrl = getClass().getResource(resourcePath)
        def fileWithPath = resourceUrl != null ? resourceUrl.getPath() : resourcePath
        return jobImportFromFile(projectName, new File(fileWithPath), client, dupeOption, contentType)
    }

    /**
     * Imports a  job file into a specified project.
     *
     * @param projectName The name of the project into which the job file is to be imported.
     * @param resourceFileWithPath The resource path
     * @param client      The RdClient object used to perform the HTTP request.
     * @param dupeOption  (Optional) The duplicate option for handling existing jobs. Defaults to DUPE_OPTION_DEFAULT.
     * @param contentType (Optional) The content type of the request. Defaults to CONTENT_TYPE_DEFAULT.
     * @return A Map representation of the JSON response body if the import is successful.
     *         The method checks for a successful response and a 200 HTTP status code.
     * @throws IllegalArgumentException if the imports fails
     */
    private static def jobImportFromFile(
        String projectName,
        File resourceFileWithPath,
        RdClient client,
        String dupeOption = DUPE_OPTION_DEFAULT,
        String contentType = CONTENT_TYPE_DEFAULT
    ) {
        def responseImport = client.doPost("/project/${projectName}/jobs/import?dupeOption=${dupeOption}", resourceFileWithPath, contentType);

        // Check if the import was successful and return the response body as a Map
        if (responseImport.code() != 200) {
            // Throw an exception if the import failed
            throw new IllegalArgumentException("Job import failed: ${responseImport} with body: ${responseImport?.body()?.string()}");
        }
        def data = OBJECT_MAPPER.readValue(responseImport.body().string(), Map.class)
        validateJobsImportAllSuccess(data)
        return data
    }

    /**
     * Imports a YAML job file into a specified project.
     *
     * @param projectName The name of the project into which the job file is to be imported.
     * @param pathXmlFile The file path
     * @param client      The RdClient object used to perform the HTTP request.
     * @param dupeOption  (Optional) The duplicate option for handling existing jobs. Defaults to DUPE_OPTION_DEFAULT.
     * @return A Map representation of the JSON response body if the import is successful.
     *         The method checks for a successful response and a 200 HTTP status code.
     * @throws IllegalArgumentException if the imports fails
     */
    static def jobImportYamlFile(String projectName, String pathYamlFile, RdClient client, String dupeOption = DUPE_OPTION_DEFAULT) {
        return jobImportFromFile(projectName, new File(pathYamlFile), client, dupeOption, 'application/yaml')
    }

    /**
     * Returns Jobs in the project.
     * @param client
     * @param projectName name of the project
     * @param queryString query string to append to the request
     * @return jobs
     * @throws IllegalArgumentException if the job listing API call fails
     */
    static final Collection<Job> getJobsForProject(RdClient client, String projectName, String queryString = null) {
        def jobsResponse = client.doGetAcceptAll("/project/${projectName}/jobs"  + (queryString ? "?${queryString}" : ""))

        if (!jobsResponse.isSuccessful()) {
            throw new IllegalArgumentException("Job listing failed: ${jobsResponse} with body: ${jobsResponse?.body()?.string()}");
        }

        OBJECT_MAPPER.readValue(jobsResponse.body().string(), ArrayList<Job>.class)
    }

}
