package org.rundeck.tests.functional.api.job

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Tests for the jobs API endpoint
 */
@APITest
class JobsSpec extends BaseContainer {

    static final def newProject = UUID.randomUUID().toString()
    static List<String> createdJobIds

    def setupSpec() {
        startEnvironment()
        setupProject(newProject)

        createdJobIds = createJobs()
    }

    def cleanupSpec() {
        deleteProject(newProject)
    }

    def "Listing jobs for a project that has no jobs"() {
        when:
        def aProject = UUID.randomUUID().toString()
        setupProject(aProject)

        then:
        JobUtils.getJobsForProject(getClient(), aProject).isEmpty()
    }

    def "Listing jobs for a project"() {
        expect:
        JobUtils.getJobsForProject(getClient(), newProject).size() == 3
    }

    def "Test job query params"() {
        expect:
        JobUtils.getJobsForProject(getClient(), newProject, query).size() == expectedSize

        where:
        query                                                                         | expectedSize
        null                                                                          | 3
        "jobFilter=test-jobs&groupPath=api/test-jobs"                                 | 2
        "jobFilter=test-jobs&groupPathExact=api/test-jobs"                            | 1
        "jobExactFilter=test-jobs&groupPath=api/test-jobs"                            | 1
        "groupPath=api/test-jobs"                                                     | 2
        "jobExactFilter=test-jobs&groupPathExact=api/test-jobs"                       | 1
        "jobExactFilter=test-jobs+another+job&groupPathExact=api/test-jobs/sub-group" | 1
        "jobExactFilter=test-jobs+another&groupPathExact=api/test-jobs"               | 0
        "jobExactFilter=test-jobs&groupPathExact=api/test-jobs/sub-group"             | 0
        "jobFilter=test-jobs&groupPathExact=-"                                        | 1
        "idlist=fakeId"                                                               | 0
        "idlist=${createdJobIds.join(',')}"                                           | 3
        "scheduledFilter=false"                                                       | 3
        "max=2"                                                                       | 2
        "offset=3&max=10"                                                             | 0
    }

    /***
     * Creates jobs and returns the ids
     */
    def createJobs() {

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
        [client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody),
        client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody1),
        client.doPostWithMultipart("/project/${newProject}/jobs/import?format=yaml&dupeOption=skip", multipartBody2)].collect {
             MAPPER.readValue(it.body().string(), Map).succeeded[0].id as String
        }
    }
}
