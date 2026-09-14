package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Verifies the shape of the {@code job.serverUrl} context variable as seen by a running job step.
 * The value must not end with a slash so that job steps can concatenate it with an absolute path
 * ({@code ${job.serverUrl}/api/50/projects}) without producing a double slash, which Jetty 12 rejects
 * with {@code 400 Ambiguous URI empty segment}.
 */
@APITest
class JobServerUrlContextSpec extends BaseContainer {

    private static final String PROJECT_NAME = "job-serverurl-context-test"
    private static final String OUTPUT_MARKER = "serverUrl="

    def setupSpec() {
        setupProject(PROJECT_NAME)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
    }

    def "job.serverUrl has no trailing slash and is safe to concatenate with an absolute path"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def path = JobUtils.updateJobFileToImport(
                "job-template-common.xml",
                PROJECT_NAME,
                // the helper applies this through String.replaceAll, so the dollar must be escaped
                // for the replacement string to keep the literal ${job.serverUrl} context expression
                ["job-name": jobName, "args": "echo ${OUTPUT_MARKER}\\\${job.serverUrl}"]
        )
        def jobId = JobUtils.createJob(PROJECT_NAME, new File(path).text, client).succeeded.first().id.toString()

        when:
        def runJob = runJobGetOutput(jobId, ["options": [opt1: "z", opt2: "a"]])
        def serverUrl = runJob.output.entries
                .collect { it.log }
                .find { it?.startsWith(OUTPUT_MARKER) }
                ?.substring(OUTPUT_MARKER.length())

        then:
        runJob.execution.status == 'succeeded'
        serverUrl
        !serverUrl.endsWith('/')
        // absolute URL with non-empty path segments only, so appending /api/... never yields a double slash
        // (covers both plain and context-path deployments)
        serverUrl ==~ /https?:\/\/[^\/]+(\/[^\/]+)*/
    }
}
