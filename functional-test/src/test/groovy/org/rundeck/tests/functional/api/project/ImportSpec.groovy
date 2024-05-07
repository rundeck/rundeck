package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.jobs.Job
import org.rundeck.util.api.responses.jobs.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient

@APITest
class ImportSpec extends BaseContainer {
    public static final String RESOURCE_ARCHIVE_TEST_README_DIR = "/projects-import/archive-test-readme"
    public static final String RESOURCE_ARCHIVE_TEST_DIR = "/projects-import/archive-test"

    def "test-project-import-readme-motd"(){
        given:
        def client = getClient()
        String projectName = "motdTest"
        Object projectJsonMap = [
                "name": projectName
        ]
        def responseProject = client.doPost("/projects", projectJsonMap)
        assert responseProject.successful
        def archiveJar = createArchiveJarFile(
            projectName,
            new File(
                getClass().getResource(RESOURCE_ARCHIVE_TEST_README_DIR).getPath()
            )
        )
        def responseImport = client.doPut(
                "/project/${projectName}/import?jobUuidOption=remove&importConfig=true",
                archiveJar)
        responseImport.successful

        when: "We try to read readme content"
        def readmeResponse = client.doGetAcceptAll("/project/${projectName}/readme.md")
        assert readmeResponse.successful
        String readmeContent = readmeResponse.body().string()

        then: "The content: "
        readmeContent != null
        readmeContent.contains("this is a readme file")

        when: "We try to read motd content"
        def motdResponse = client.doGetAcceptAll("/project/${projectName}/motd.md")
        assert motdResponse.successful
        String motdContent = motdResponse.body().string()

        then: "The content: "
        motdContent != null
        motdContent.contains("this is a message of the day")

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-import"(){
        given:
        File tmpjar = createArchiveJarFile(projectName, new File(getClass().getResource(RESOURCE_ARCHIVE_TEST_DIR).getPath()))
        def mapper = new ObjectMapper()
        def client = getClient()

        String projectName = "APIImportTest"
        Object projectJsonMap = [
                "name": projectName,
                "description": "APIImportTest",
        ]

        String projectName1 = "APIImportTest1"
        Object projectJsonMap1 = [
                "name": projectName1,
                "description": "APIImportTest1",
        ]

        def responseProject = client.doPost("/projects", projectJsonMap)
        def responseProject1 = client.doPost("/projects", projectJsonMap1)

        assert responseProject.successful
        assert responseProject1.successful

        when: "we import the test zip to project $projectName"
        def responseImport1 = client.doPut(
                "/project/${projectName}/import?jobUuidOption=preserve",
                tmpjar)
        responseImport1.successful

        then: "we must have 3 jobs and 6 execs"
        assertJobCountForProject(
                projectName,
                3,
                client,
                mapper
        )
        assertExecsCountForProject(
                projectName,
                6,
                client,
                mapper
        )

        when: "We import the archive to test project 2"
        def responseImport2 = client.doPut(
                "/project/${projectName1}/import?jobUuidOption=preserve",
                tmpjar)

        then: "Won't succeed because the jobUuidOption, no jobs imported"
        Object parsedResponse = mapper.readValue(responseImport2.body().string(), Object.class)
        parsedResponse.import_status == "failed"
        !parsedResponse.successful
        assertJobCountForProject(
                projectName1,
                0,
                client,
                mapper
        )
        assertExecsCountForProject(
                projectName1,
                3,
                client,
                mapper
        )

        when: "We import the archive to test project 2 with valid jobUuidOption"
        def responseImport3 = client.doPut(
            "/project/${projectName1}/import?jobUuidOption=remove",
            tmpjar)

        then: "Jobs imported, executions duplicated"
        Object parsedResponse1 = mapper.readValue(responseImport3.body().string(), Object.class)
        parsedResponse1.import_status == "successful"
        parsedResponse1.successful
        assertJobCountForProject(
                projectName1,
                3,
                client,
                mapper
        )
        assertExecsCountForProject(
                projectName1,
                9,
                client,
                mapper
        )

        when: "We import the archive to test project 2 without import execs"
        def responseImport4 = client.doPut(
                "/project/${projectName1}/import?importExecutions=false&jobUuidOption=remove",
                tmpjar)

        then: "Import succeeds"
        Object parsedResponse2 = mapper.readValue(responseImport4.body().string(), Object.class)
        parsedResponse2.import_status == "successful"
        parsedResponse2.successful
        assertJobCountForProject(
                projectName1,
                3,
                client,
                mapper
        )
        assertExecsCountForProject(
                projectName1,
                9,
                client,
                mapper
        )

        cleanup:
        deleteProject(projectName)
        deleteProject(projectName1)
    }

    def assertJobCountForProject(
            final String projectName,
            final int count,
            final RdClient client,
            final ObjectMapper mapper
    ) {
        def jobsResponse = client.doGetAcceptAll("/project/${projectName}/jobs")
        assert jobsResponse.successful
        List<Job> jobsInProject = mapper.readValue(jobsResponse.body().string(), ArrayList<Job>.class)
        assert jobsInProject.size() == count
        return true
    }

    def assertExecsCountForProject(
            final String projectName,
            final int count,
            final RdClient client,
            final ObjectMapper mapper
    ) {
        def execsResponse = client.doGetAcceptAll("/project/${projectName}/executions")
        assert execsResponse.successful
        JobExecutionsResponse parsedResponse = mapper.readValue(execsResponse.body().string(), JobExecutionsResponse.class)
        List<Execution> execsList = parsedResponse.executions as List<Execution>
        assert execsList.size() == count
        return true
    }

}
