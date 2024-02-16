package org.rundeck.tests.functional.api.export_import

import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobsImportSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "import RunDeck Jobs in jobs.xml format (multipart request)"() {
        when:
            def path = generatePathXml()
            def multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("xmlBatch", new File(path).name, RequestBody.create(new File(path), MultipartBody.FORM))
                    .build()
            def responseImport = client.doPostWithMultipart("/project/${PROJECT_NAME}/jobs/import", multipartBody)
        then:
            verifyAll {
                responseImport.code() == 200
                def json = jsonValue(responseImport.body(), Map)
                json.succeeded.size() == 1
                json.failed.size() == 0
                json.skipped.size() == 0
                json.succeeded[0].name == "cli job"
                json.succeeded[0].group == "api-test"
                json.succeeded[0].project == PROJECT_NAME
            }
    }

    def "import RunDeck Jobs in jobs.xml format (urlencode)"() {
        when:
            def path = generatePathXml()
            FormBody formBody = new FormBody.Builder()
                    .addEncoded("xmlBatch", new File(path).text)
                    .build()
            def responseImport = client.doPostWithFormData("/project/${PROJECT_NAME}/jobs/import", formBody)
        then:
            verifyAll {
                responseImport.code() == 200
                def json = jsonValue(responseImport.body(), Map)
                json.succeeded.size() == 1
                json.failed.size() == 0
                json.skipped.size() == 0
                json.succeeded[0].name == "cli job"
                json.succeeded[0].group == "api-test"
                json.succeeded[0].project == PROJECT_NAME
            }
    }

    def "import Jobs in yaml content with fileformat param and json response"() {
        when:
            Headers headers = new Headers.Builder()
                .add("Accept", "application/json")
                .build()
            def path = generatePathYaml()
            def responseImport = client.doPost("/project/${PROJECT_NAME}/jobs/import?fileformat=yaml", new File(path).text, "application/yaml", headers)
        then:
            verifyAll {
                responseImport.code() == 200
                def json = jsonValue(responseImport.body(), Map)
                json.succeeded.size() == 1
                json.failed.size() == 0
                json.skipped.size() == 0
            }
    }

    def "import Jobs in XML content with fileformat param and json response"() {
        when:
            Headers headers = new Headers.Builder()
                    .add("Accept", "application/json")
                    .build()
            def path = generatePathXml()
            def responseImport = client.doPost("/project/${PROJECT_NAME}/jobs/import?fileformat=xml", new File(path).text, "application/xml", headers)
        then:
            verifyAll {
                responseImport.code() == 200
                def json = jsonValue(responseImport.body(), Map)
                json.succeeded.size() == 1
                json.failed.size() == 0
                json.skipped.size() == 0
            }
    }

    def "/jobs/import with invalid format"() {
        when:
            def path = generatePathXml()
            FormBody formBody = new FormBody.Builder()
                    .addEncoded("xmlBatch", new File(path).text)
                    .build()
            def responseImport = client.doPostWithFormData("/project/${PROJECT_NAME}/jobs/import?format=DNEformat", formBody)
        then:
            verifyAll {
                responseImport.code() == 415
                def json = jsonValue(responseImport.body(), Map)
                json.errorCode == "api.error.jobs.import.format.unsupported"
                json.message == "The specified format is not supported: DNEformat"
            }
    }

    def "/jobs/import without expected file content"() {
        when:
            def multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("xmlBatch", "z")
                    .build()
            def responseImport = client.doPostWithMultipart("/project/${PROJECT_NAME}/jobs/import", multipartBody)
        then:
            verifyAll {
                responseImport.code() == 400
                def json = jsonValue(responseImport.body(), Map)
                json.errorCode == "api.error.jobs.import.missing-file"
                json.message == "No file was uploaded"
            }
    }

    def "/jobs/import multipart without xmlBatch param"() {
        when:
            def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("", "")
                .build()
            def responseImport = client.doPostWithMultipart("/project/${PROJECT_NAME}/jobs/import", multipartBody)
        then:
            verifyAll {
                responseImport.code() == 400
                def json = jsonValue(responseImport.body(), Map)
                json.errorCode == "api.error.parameter.required"
                json.message == "parameter \"xmlBatch\" is required"
            }
    }

    def "/jobs/import form without xmlBatch param"() {
        when:
            FormBody formBody = new FormBody.Builder()
                    .build()
            def responseImport = client.doPostWithFormData("/project/${PROJECT_NAME}/jobs/import", formBody)
        then:
            verifyAll {
                responseImport.code() == 400
                def json = jsonValue(responseImport.body(), Map)
                json.errorCode == "api.error.parameter.required"
                json.message == "parameter \"xmlBatch\" is required"
            }
    }

    def generatePathXml() {
        def xmlJob = "<joblist>\n" +
                "   <job>\n" +
                "      <name>cli job</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <context>\n" +
                "          <project>xml-project-name</project>\n" +
                "      </context>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>xml-args</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def xmlJobAux = xmlJob.replaceAll('xml-args', 'echo hello there')
                .replaceAll('xml-project-name', PROJECT_NAME)
        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlJobAux
        tempFile.deleteOnExit()
        tempFile.path
    }

    def generatePathYaml() {
        def xmlJob = "-\n" +
                "  project: test\n" +
                "  loglevel: INFO\n" +
                "  sequence:\n" +
                "    keepgoing: false\n" +
                "    strategy: node-first\n" +
                "    commands:\n" +
                "    - exec: echo hello there\n" +
                "  description: '\$0'\n" +
                "  group: api-test\n" +
                "  name: test import yaml"

        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlJob
        tempFile.deleteOnExit()
        tempFile.path
    }

}
