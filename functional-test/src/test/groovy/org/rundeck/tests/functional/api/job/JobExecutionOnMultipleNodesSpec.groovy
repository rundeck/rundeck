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
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
              <nodefilters>
                    <filter>.*</filter>
               </nodefilters>
              <executionEnabled>true</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(TEST_PROJECT, jobXml, client)

        then:
        response.successful
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        def completedJob = runJobAndWait(jobId)
        // Ensure it was ran on the local node and two nodes added by the setup
        getOrderedNodesListExecutedOn(completedJob).size() == 3
    }

    def "Create a job that executes on nodes that were not excluded"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
              </dispatch>
               <nodefilters>
                   <filter>.*</filter>
                   <filterExclude>tags: "executor-test"</filterExclude>
               </nodefilters>
              <executionEnabled>true</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(TEST_PROJECT, jobXml, client)

        then:
        response.successful
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        def completedJob = runJobAndWait(jobId)
        // Should run the local node only since the filter excluded the nodes added by the setup
        getOrderedNodesListExecutedOn(completedJob).size() == 1
    }

    def "Create a job that executes on nodes in the descending order"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
                <rankOrder>descending</rankOrder>
              </dispatch>
               <nodefilters>
                   <filter>tags: "executor-test"</filter>
               </nodefilters>
              <executionEnabled>true</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(TEST_PROJECT, jobXml, client)

        then:
        response.successful
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        def completedJob = runJobAndWait(jobId)
        getOrderedNodesListExecutedOn(completedJob) == ["ssh-node", "password-node"]
    }

    def "Create a job that executes on nodes in the ascending order"() {
        given:
        def jobName = UUID.randomUUID().toString()
        def jobXml = """
        <joblist>
           <job>
              <name>${jobName}</name>
              <group>api-test</group>
              <description></description>
              <loglevel>INFO</loglevel>
              <multipleExecutions>true</multipleExecutions>
              <dispatch>
                <threadcount>1</threadcount>
                <keepgoing>true</keepgoing>
                <rankOrder>ascending</rankOrder>
              </dispatch>
               <nodefilters>
                   <filter>tags: "executor-test"</filter>
               </nodefilters>
              <executionEnabled>true</executionEnabled>
              <sequence>
                <command>
                  <exec>echo 0</exec>
                </command>
              </sequence>
           </job>
        </joblist>"""

        when:
        def response = JobUtils.createJob(TEST_PROJECT, jobXml, client)

        then:
        response.successful
        def jobId = MAPPER.readValue(response.body().string(), CreateJobResponse.class).getSucceeded().get(0).id
        def completedJob = runJobAndWait(jobId)
        getOrderedNodesListExecutedOn(completedJob) == ["password-node", "ssh-node"]
    }

    private static List<String> getOrderedNodesListExecutedOn(Map completedJob) {
        completedJob.entries.inject([], { result, x -> result.add(x.node); return result })
    }
}
