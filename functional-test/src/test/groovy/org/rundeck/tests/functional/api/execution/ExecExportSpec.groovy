package org.rundeck.tests.functional.api.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.project.ProjectCreateResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient
import org.testcontainers.shaded.org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@APITest
class ExecExportSpec extends BaseContainer{

    static final int KILOBYTE=1024
    static final String EXECUTION_EXT = ".xml"
    static final String EXECUTION_PREFIX = "execution-"
    static final String OUTPUT_EXT = ".rdlog"
    static final String OUTPUT_PREFIX = "output-"
    static final String STATE_EXT = ".state.json"
    static final String STATE_PREFIX = "state-"

    def "test-project-export-executions"(){
        given:
        def client = getClient()
        def projectName = "testExportExecs"
        def tempFilePath = Files.createTempDirectory("testExportTemp")
        Object testProperties = [
                "name": projectName
        ]
        def mapper = new ObjectMapper()

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        when: "BEGIN: upload job, run twice"
        def jobXml = "<joblist>\n" +
                    "     <job>\n" +
                    "        <name>cli job</name>\n" +
                    "        <group>api-test</group>\n" +
                    "        <description></description>\n" +
                    "        <loglevel>INFO</loglevel>\n" +
                    "        <dispatch>\n" +
                    "          <threadcount>1</threadcount>\n" +
                    "          <keepgoing>true</keepgoing>\n" +
                    "        </dispatch>\n" +
                    "        <sequence>\n" +
                    "          <command>\n" +
                    "          <exec>echo hi</exec>\n" +
                    "          </command>\n" +
                    "        </sequence>\n" +
                    "     </job>\n" +
                    "  </joblist>"

        def created = JobUtils.createJob(projectName, jobXml, client)
        assert created.successful

        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
        def jobId = jobCreatedResponse.succeeded[0]?.id

        then:
        jobId != null

        when: "run 1"
        def jobRun1 = JobUtils.executeJob(jobId, client)
        assert jobRun1.successful

        Execution exec1 = mapper.readValue(jobRun1.body().string(), Execution.class)

        Execution JobExecutionStatus1 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                exec1.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        JobExecutionStatus1.status == ExecutionStatus.SUCCEEDED.state

        when: "run 2"
        def jobRun2 = JobUtils.executeJob(jobId, client)
        assert jobRun2.successful

        Execution exec2 = mapper.readValue(jobRun2.body().string(), Execution.class)

        Execution JobExecutionStatus2 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                exec2.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        JobExecutionStatus2.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: Export specifying executions, 2"
        def tempDir = tempFilePath.toString() + "/test"
        if( !Files.exists(Paths.get(tempDir)) ){
            Files.createDirectories(Paths.get(tempDir))
        }
        def outPath = tempDir + "/export.zip"
        requestFileToTemp(
                client,
                "/project/${projectName}/export?executionIds=${JobExecutionStatus1.id},${JobExecutionStatus2.id}",
                outPath
        )

        assert Files.list(Paths.get(tempDir)).findAny().isPresent()

        new FileInputStream(outPath).withCloseable {
            extractStream(
                    tempDir + "/extracted_execs",
                    it
            )
        }

        def pathToUnzippedExportDir = Paths.get(tempDir + "/extracted_execs")
        assert Files.exists(pathToUnzippedExportDir)

        def pathToExecs = pathToUnzippedExportDir.toString() + "/rundeck-${projectName}/executions"
        assert Files.exists(Paths.get(pathToExecs))

        def archivesCheckForTwoExecs = assertArchiveContents(Paths.get(pathToExecs), [exec1.id, exec2.id] as List<String>)

        then:
        archivesCheckForTwoExecs

        when: "TEST: Export specifying executions, 1"
        def tempDir2 = tempFilePath.toString() + "/test2"
        if( !Files.exists(Paths.get(tempDir2)) ){
            Files.createDirectories(Paths.get(tempDir2))
        }
        def outPath2 = tempDir2 + "/export2.zip"
        requestFileToTemp(
                client,
                "/project/${projectName}/export?executionIds=${JobExecutionStatus1.id}",
                outPath2
        )

        assert Files.list(Paths.get(tempDir2)).findAny().isPresent()

        new FileInputStream(outPath2).withCloseable {
            extractStream(
                    tempDir2 + "/extracted_execs2",
                    it
            )
        }

        def pathToUnzippedExportDir2 = Paths.get(tempDir2 + "/extracted_execs2")
        assert Files.exists(pathToUnzippedExportDir2)

        def pathToExecs2 = pathToUnzippedExportDir2.toString() + "/rundeck-${projectName}/executions"
        assert Files.exists(Paths.get(pathToExecs2))

        def archivesCheckForOneExec = assertArchiveContents(Paths.get(pathToExecs2), [exec1.id] as List<String>)

        then:
        archivesCheckForOneExec

        when: "TEST: Export specifying executions, 1 (another one)"
        def tempDir3 = tempFilePath.toString() + "/test3"
        if( !Files.exists(Paths.get(tempDir3)) ){
            Files.createDirectories(Paths.get(tempDir3))
        }
        def outPath3 = tempDir3 + "/export3.zip"
        requestFileToTemp(
                client,
                "/project/${projectName}/export?executionIds=${JobExecutionStatus2.id}",
                outPath3
        )

        assert Files.list(Paths.get(tempDir3)).findAny().isPresent()

        new FileInputStream(outPath3).withCloseable {
            extractStream(
                    tempDir3 + "/extracted_execs3",
                    it
            )
        }

        def pathToUnzippedExportDir3 = Paths.get(tempDir3 + "/extracted_execs3")
        assert Files.exists(pathToUnzippedExportDir3)

        def pathToExecs3 = pathToUnzippedExportDir3.toString() + "/rundeck-${projectName}/executions"
        assert Files.exists(Paths.get(pathToExecs3))

        then:
        assertArchiveContents(Paths.get(pathToExecs3), [exec2.id] as List<String>)

        cleanup:
        FileUtils.deleteDirectory(new File(tempDir))
        FileUtils.deleteDirectory(new File(tempDir2))
        FileUtils.deleteDirectory(new File(tempDir3))
        deleteProject(projectName)
    }

    def assertArchiveContents(
            final Path archivesPath,
            final List<String> execsId
    ){
        // Listing execs
        def execsInBundle = Files.list(archivesPath).findAll{
            it -> it.fileName.toString().endsWith(EXECUTION_EXT)
        }

        // Listing outputs
        def outputInBundle = Files.list(archivesPath).findAll{
            it -> it.fileName.toString().endsWith(OUTPUT_EXT)
        }

        // Listing outputs
        def stateInBundle = Files.list(archivesPath).findAll{
            it -> it.fileName.toString().endsWith(STATE_EXT)
        }

        execsId.each {
            id -> {
                def execution = "${EXECUTION_PREFIX}${id}$EXECUTION_EXT"
                def log = "${OUTPUT_PREFIX}${id}$OUTPUT_EXT"
                def state = "${STATE_PREFIX}${id}$STATE_EXT"

                assert Paths.get("${archivesPath.toString()}/${execution}") in execsInBundle
                assert Paths.get("${archivesPath.toString()}/${log}") in outputInBundle
                assert Paths.get("${archivesPath.toString()}/${state}") in stateInBundle
            }
        }
        true
    }

    def requestFileToTemp(
            final RdClient client,
            final String endpoint,
            final String outPath
    ){
        def archiveCall = client.doGet(endpoint)
        if( archiveCall.successful ){
            def stream = archiveCall.body().byteStream()
            def out = new FileOutputStream(outPath)
            try{
                out << stream
            }catch (Exception e){
                e.printStackTrace()
            } finally {
                stream.close()
                stream.close()
            }
        }
    }

    static void extractStream(String destDir, InputStream inputStream){
        new ZipInputStream(inputStream).withCloseable {zipInputStream -> {
            try {
                File checkDir = new File(destDir)
                if( !checkDir.exists() ){
                    checkDir.mkdirs()
                }
                ZipEntry zipEntry
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String newFileName = zipEntry.getName()
                    File destFile = new File(destDir, newFileName)

                    if (zipEntry.isDirectory()) {
                        destFile.mkdirs()
                    } else {
                        File parent = destFile.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs()
                        }

                        FileOutputStream fos = new FileOutputStream(destFile);
                        byte[] buffer = new byte[KILOBYTE]
                        int bytesRead
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.close()
                    }
                    zipInputStream.closeEntry()
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }}
    }

}
