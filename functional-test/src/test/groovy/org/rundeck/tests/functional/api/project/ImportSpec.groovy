package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.jobs.Job
import org.rundeck.util.api.responses.jobs.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient
import org.rundeck.util.common.WaitingTime

import java.nio.file.Files

@APITest
class ImportSpec extends BaseContainer {
    public static final String RESOURCE_ARCHIVE_TEST_README_DIR = "/projects-import/archive-test-readme"
    public static final String RESOURCE_ARCHIVE_TEST_DIR = "/projects-import/archive-test"
    // Default framework.logs.dir for the OSS docker test image (docker/official/etc/framework.properties)
    public static final String DEFAULT_FRAMEWORK_LOGS_DIR = "/home/rundeck/var/logs"

    def "test-project-import-readme-motd"(){
        given:
        def client = getClient()
        String projectName = "motdTest"
        Object projectJsonMap = [
                "name": projectName
        ]
        def responseProject = client.post("/projects", projectJsonMap)
        def archiveJar = createArchiveJarFile(
            projectName,
            new File(
                getClass().getResource(RESOURCE_ARCHIVE_TEST_README_DIR).getPath()
            )
        )
        client.put(
            "/project/${projectName}/import?jobUuidOption=remove&importConfig=true",
            archiveJar,
            'application/zip'
        )

        when: "We try to read readme content"
        String readmeContent
        try(def readmeResponse = client.doGetAcceptAll("/project/${projectName}/readme.md")) {
            assert readmeResponse.successful
            readmeContent = readmeResponse.body().string()
        }

        then: "The content: "
        readmeContent != null
        readmeContent.contains("this is a readme file")

        when: "We try to read motd content"
        String motdContent
        try(def motdResponse = client.doGetAcceptAll("/project/${projectName}/motd.md")) {
            assert motdResponse.successful
            motdContent = motdResponse.body().string()
        }

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

        def responseProject = post("/projects", projectJsonMap, Map)
        def responseProject1 = post("/projects", projectJsonMap1, Map)

        when: "we import the test zip to project $projectName"
        def responseImport1 = client.put(
                "/project/${projectName}/import?jobUuidOption=preserve",
                tmpjar,
                'application/zip')

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
        Map parsedResponse = client.put(
                "/project/${projectName1}/import?jobUuidOption=preserve",
                tmpjar,
                'application/zip')

        then: "Won't succeed because the jobUuidOption, no jobs imported"
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
        Map parsedResponse1 = client.put(
            "/project/${projectName1}/import?jobUuidOption=remove",
            tmpjar,
            'application/zip')

        then: "Jobs imported, executions duplicated"
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
        Map parsedResponse2 = client.put(
                "/project/${projectName1}/import?importExecutions=false&jobUuidOption=remove",
                tmpjar,
                'application/zip')

        then: "Import succeeds"
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

    /**
     * Regression test for RUN-4950 / HackerOne #3960872: reproduces the reported PoC - a
     * project-import archive whose execution XML forges an outputfilepath ("/etc/passwd") that
     * is not backed by any file actually present in the archive. Import must still report
     * success (matching the original report), but the imported execution's output must never
     * disclose the forged file's content.
     */
    /**
     * Regression test for RUN-4950 / HackerOne #3960872: reproduces the reported PoC - a
     * project-import archive whose execution XML forges an outputfilepath pointing at another,
     * real project's execution log file that is not backed by any file present in the archive
     * itself. Import must still report success (matching the original report), but the imported
     * execution's output must never disclose the victim project's real log content.
     *
     * The forged path must resolve, after getFileForExecutionFiletype's extension substitution
     * (strip the trailing extension, append the requested filetype), to the victim file's actual
     * path - so it is built from a real victim execution's id with a ".rdlog" suffix, rather than
     * an arbitrary file whose transformed path would never be read regardless of the fix.
     */
    def "test-project-import-forged-outputfilepath-not-disclosed"(){
        given: "a victim project with a real execution log containing a unique marker"
        String victimProject = "crosslogvictim"
        String attackerProject = "crosslogattacker"
        String marker = "cross-project-secret-" + UUID.randomUUID().toString()
        post("/projects", ["name": victimProject], Map)
        def adhoc = post("/project/${victimProject}/run/command?exec=echo+${marker}", Map)
        String victimExecId = adhoc.execution.id.toString()
        waitForExecutionFinish(victimExecId, WaitingTime.EXCESSIVE)

        and: "a forged archive for a different project referencing that real log file by path, absent from the archive"
        String forgedPath = "${DEFAULT_FRAMEWORK_LOGS_DIR}/rundeck/${victimProject}/run/logs/${victimExecId}.rdlog"
        File archiveDir = Files.createTempDirectory("cross-project-log-poc").toFile()
        File execDir = new File(archiveDir, "rundeck-${attackerProject}/executions")
        execDir.mkdirs()
        new File(execDir, "execution-9001.xml").text = """<executions>
  <execution id='9001'>
    <dateStarted>2014-03-06T18:45:25Z</dateStarted>
    <dateCompleted>2014-03-06T18:45:27Z</dateCompleted>
    <status>true</status>
    <outputfilepath>${forgedPath}</outputfilepath>
    <failedNodeList />
    <succeededNodeList>dignan</succeededNodeList>
    <abortedby />
    <cancelled>false</cancelled>
    <argString />
    <loglevel>INFO</loglevel>
    <doNodedispatch>false</doNodedispatch>
    <executionType>user</executionType>
    <project>${attackerProject}</project>
    <user>admin</user>
    <workflow keepgoing='false' strategy='node-first'>
      <command>
        <exec>echo hi this is a test project</exec>
      </command>
    </workflow>
  </execution>
</executions>"""
        File tmpjar = createArchiveJarFile(attackerProject, archiveDir)
        post("/projects", ["name": attackerProject], Map)

        when: "we import the forged archive into the attacker project"
        Map parsedResponse = client.put(
                "/project/${attackerProject}/import?importExecutions=true&jobUuidOption=remove",
                tmpjar,
                'application/zip')

        then: "import still reports success, matching the original report"
        parsedResponse.successful

        when: "we look up the imported execution and read its output"
        def mapper = new ObjectMapper()
        def execsResponse = client.doGetAcceptAll("/project/${attackerProject}/executions")
        assert execsResponse.successful
        JobExecutionsResponse execsParsed = mapper.readValue(execsResponse.body().string(), JobExecutionsResponse.class)
        execsResponse.close()
        String execId = execsParsed.executions[0].id

        def outputResponse = doRequest("/execution/${execId}/output?format=text") {
            it.header 'Accept', 'text/plain'
        }
        String outputBody = outputResponse.body()?.string()

        then: "the victim project's real log content is never disclosed through the attacker project's output API"
        !(outputBody?.contains(marker))

        cleanup:
        deleteProject(attackerProject)
        deleteProject(victimProject)
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
