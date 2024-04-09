package org.rundeck.tests.functional.api.job

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JobsSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "Listing RunDeck Jobs for project test"() {
        when:
            def response = doGet("/project/${PROJECT_NAME}/jobs")
        then:
            verifyAll {
                response.successful
            }
    }

    def "Test query with match filter and exact filter"() {
        setup:
            def newProject = "test-jobs-query"
            setupProject(newProject)
            def yaml = """
                    -
                      project: test
                      loglevel: INFO
                      sequence:
                        keepgoing: false
                        strategy: node-first
                        commands:
                        - exec: echo hello there
                      description: 'test-jobs.sh script'
                      name: test-jobs
                      group: api/test-jobs
                    """
            def yaml1 = """
                    -
                      project: test
                      loglevel: INFO
                      sequence:
                        keepgoing: false
                        strategy: node-first
                        commands:
                        - exec: echo hello there
                      description: 'test-jobs.sh script'
                      name: test-jobs another job
                      group: api/test-jobs/sub-group
                    """
            def yaml2 = """
                    -
                      project: test
                      loglevel: INFO
                      sequence:
                        keepgoing: false
                        strategy: node-first
                        commands:
                        - exec: echo hello there
                      description: 'test-jobs.sh script'
                      name: test-jobs top level
                    """
            def path = JobUtils.generateFileToImport(yaml, "yaml")
            def path1 = JobUtils.generateFileToImport(yaml1, "yaml")
            def path2 = JobUtils.generateFileToImport(yaml2, "yaml")
            def multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("xmlBatch", new File(path).name, RequestBody.create(new File(path), MultipartBody.FORM))
                    .build()
            def multipartBody1 = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(path1).name, RequestBody.create(new File(path1), MultipartBody.FORM))
                .build()
            def multipartBody2 = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(path2).name, RequestBody.create(new File(path2), MultipartBody.FORM))
                .build()
        when:
            def responseImport = client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
            def responseImport1 = client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody1)
            def responseImport2 = client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody2)
        then:
            verifyAll {
                responseImport.successful
                responseImport1.successful
                responseImport2.successful
            }
            testJobQuery "project=${newProject}&jobFilter=test-jobs&groupPath=api/test-jobs", 2
            testJobQuery "project=${newProject}&jobFilter=test-jobs&groupPathExact=api/test-jobs", -1
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs&groupPath=api/test-jobs", -1
            testJobQuery "project=${newProject}&groupPath=api/test-jobs", 2
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs", 2
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs+another+job&groupPathExact=api/test-jobs/sub-group", 2
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs", -1
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs+another&groupPathExact=api/test-jobs", 0
            testJobQuery "project=${newProject}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs/sub-group", 0
            testJobQuery "project=${newProject}&jobFilter=test-jobs&groupPathExact=-", -1
        cleanup:
            deleteProject(newProject)
    }

    /**
     * Executes a test jobs query with optional parameters and verifies the response.
     *
     * @param xargs Optional query parameters in the form of a query string.
     * @param expect Optional parameter to specify the expected number of test executions.
     */
    void testJobQuery(String xargs = null, Integer expect = null) {
        def url = "/project/${PROJECT_NAME}/jobs"
        def response = doGet(xargs ? "${url}?${xargs}" : url)
        def itemCount = jsonValue(response.body(), List).size()
        verifyAll {
            response.successful
            response.code() == 200
            if (expect != null && itemCount != 0)
                itemCount == expect
        }
    }

}
