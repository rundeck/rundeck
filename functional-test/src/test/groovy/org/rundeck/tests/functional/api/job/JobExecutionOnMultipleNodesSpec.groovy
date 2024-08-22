package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JobExecutionOnMultipleNodesSpec extends BaseContainer {

    public static final String TEST_PROJECT = "core-jsch-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"
    private static final MAPPER = new ObjectMapper()

    def setupSpec() {
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath() + "/keys"

        loadKeysForNodes(keyPath, TEST_PROJECT, NODE_KEY_PASSPHRASE, NODE_USER_PASSWORD, USER_VAULT_PASSWORD)

        setupProjectArchiveDirectory(TEST_PROJECT,
                new File(getClass().getResource(TEST_SSH_ARCHIVE_DIR).getPath()),
                ["importConfig"      : "true",
                 "importACL"         : "true",
                 "importNodesSources": "true",
                 "jobUuidOption"     : "preserve"])
        //wait for ansible node to be available
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }

    def "Create a job that executes on all nodes"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                TEST_PROJECT,
                ["job-name"           : jobName,
                 "args"               : "echo 0",
                 "node-filter-include": ".*",
                ])

        when:
        def response = JobUtils.createJob(TEST_PROJECT, new File(path).text, client)

        then:
        response.successful

        when:
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        Object optionsJson = ["options": [opt1: "z", opt2: "a"]]
        def completedJob = runJobAndWait(jobId, optionsJson)

        then:
        // Ensure it was ran on the local node and two nodes added by the setup
        getOrderedNodesListExecutedOn(completedJob).size() == 3
    }

    def "Create a job that executes on nodes that were not excluded"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                TEST_PROJECT,
                ["job-name"           : jobName,
                 "args"               : "echo 0",
                 "node-filter-exclude": "tags: \"executor-test\"",
                ])

        when:
        def response = JobUtils.createJob(TEST_PROJECT, new File(path).text, client)

        then:
        response.successful

        when:
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        Object optionsJson = ["options": [opt1: "z", opt2: "a"]]
        def completedJob = runJobAndWait(jobId, optionsJson)

        then:
        // Should run the local node only since the filter excluded the nodes added by the setup
        getOrderedNodesListExecutedOn(completedJob).size() == 1
    }

    def "Create a job that executes on nodes in the descending order"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                TEST_PROJECT,
                ["job-name"           : jobName,
                 "args"               : "echo 0",
                 "node-filter-include": "tags: \"executor-test\"",
                 "dispatch-rank-order": "descending",
                ])

        when:
        def response = JobUtils.createJob(TEST_PROJECT, new File(path).text, client)

        then:
        response.successful

        when:
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        Object optionsJson = ["options": [opt1: "z", opt2: "a"]]
        def completedJob = runJobAndWait(jobId, optionsJson)

        then:
        getOrderedNodesListExecutedOn(completedJob) == ["ssh-node", "password-node"]

    }

    def "Create a job that executes on nodes in the ascending order"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                TEST_PROJECT,
                ["job-name"           : jobName,
                 "args"               : "echo 0",
                 "node-filter-include": "tags: \"executor-test\"",
                 "dispatch-rank-order": "ascending",
                ])

        when:
        def response = JobUtils.createJob(TEST_PROJECT, new File(path).text, client)

        then:
        response.successful

        when:
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        Object optionsJson = ["options": [opt1: "z", opt2: "a"]]
        def completedJob = runJobAndWait(jobId, optionsJson)

        then:
        getOrderedNodesListExecutedOn(completedJob) == ["password-node", "ssh-node"]
    }

    private static List<String> getOrderedNodesListExecutedOn(Map completedJob) {
        completedJob.entries.inject([], { result, x -> result.add(x.node); return result })
    }
}
