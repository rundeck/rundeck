package org.rundeck.util.common.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.jobs.JobExecutionsResponse
import org.rundeck.util.container.RdClient

class ExecutionUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    static final List<String> EXECUTION_FINISH_STATUSES = [
            'aborted',
            'failed',
            'succeeded',
            'timedout',
            'other'].asImmutable()

    static class Verifiers {
        /**
         * Returns a closure that verifies if an execution passed into it is finished.
         * @return
         */
        static final Closure<Boolean> executionFinished() {
            // The verifier is untyped to be compatible with pre-existing code that uses Maps instead of typed objects
            return {
                EXECUTION_FINISH_STATUSES.contains(it?.status)
            }
        }


        /**
         * Returns a closure that verifies if an execution passed into it is running.
         * @return
         */
        static final Closure<Boolean> executionRunning() {
            // The verifier is untyped to be compatible with pre-existing code that uses Maps instead of typed objects
            return {
                ["running"].contains(it?.status)
            }
        }

    }

    static class Retrievers {
        /**
         * Returns a closure that retrieves an execution by its id.
         * @param client
         * @param executionId
         * @return
         */
        static final Closure<Execution> executionById(RdClient client, String executionId) {
            { ->
                def r = client.doGet("/execution/${executionId}")
                r?.successful ? OBJECT_MAPPER.readValue(r.body()?.string(), Execution.class) : null
            }
        }

        /**
         * Returns a closure that retrieves executions for a project.
         * @param client
         * @param projectName name of the project or * for all projects
         * @param queryString query string to append to the request
         * @return a closure that lists all executions for the project
         */
        static final Closure<List<Execution>> executionsForProject(RdClient client, String projectName, String queryString = null) {
            { ->
                def execsResponse = client.doGetAcceptAll("/project/${projectName}/executions" + (queryString ? "?${queryString}" : ""))
                JobExecutionsResponse parsedResponse = OBJECT_MAPPER.readValue(execsResponse.body().string(), JobExecutionsResponse.class)
                return parsedResponse.executions.isEmpty() ? [] : parsedResponse.executions as List<Execution>
            }
        }

        /**
         * Returns a closure that retrieves executions for a job id.
         * @param client
         * @param jobId id of the job
         * @param queryString query string to append to the request
         * @return a closure that lists all executions for the job
         */
        static final Closure<List<Execution>> executionsForJobId(RdClient client, String jobId, String queryString = null) {
            { ->
                def execsResponse = client.doGetAcceptAll("/job/${jobId}/executions" + (queryString ? "?${queryString}" : ""))
                JobExecutionsResponse parsedResponse = OBJECT_MAPPER.readValue(execsResponse.body().string(), JobExecutionsResponse.class)
                return parsedResponse.executions.isEmpty() ? [] : parsedResponse.executions as List<Execution>
            }
        }

    }
}
