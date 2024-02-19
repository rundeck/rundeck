package org.rundeck.tests.functional.api.export_import

import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.JobUtils
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
                    .add("xmlBatch", new File(path).text)
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
                    .add("xmlBatch", new File(path).text)
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

    def "jobs-import-jobref-renamed"() {
        setup:
        def job = """
                <joblist>
                  <job>
                    <context>
                      <options preserveOrder='true'>
                        <option name='Time' required='true' />
                      </options>
                    </context>
                    <defaultTab>summary</defaultTab>
                    <description></description>
                    <executionEnabled>true</executionEnabled>
                    <group>Manage</group>
                    <id>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</id>
                    <loglevel>INFO</loglevel>
                    <name>RefMe</name>
                    <nodeFilterEditable>false</nodeFilterEditable>
                    <scheduleEnabled>true</scheduleEnabled>
                    <sequence keepgoing='false' strategy='node-first'>
                      <command>
                        <exec>sleep \${option.Time}</exec>
                      </command>
                    </sequence>
                    <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</uuid>
                  </job>
                  <job>
                    <context>
                      <options preserveOrder='true'>
                        <option name='Foo' />
                      </options>
                    </context>
                    <defaultTab>summary</defaultTab>
                    <description></description>
                    <executionEnabled>true</executionEnabled>
                    <group>Manage</group>
                    <id>a6d88d66-920b-492b-b7c4-1a22da67333b</id>
                    <loglevel>INFO</loglevel>
                    <name>Wrapper</name>
                    <nodeFilterEditable>false</nodeFilterEditable>
                    <scheduleEnabled>true</scheduleEnabled>
                    <sequence keepgoing='false' strategy='node-first'>
                      <command>
                        <jobref name='RefMe'>
                          <arg line='-Time \${option.Foo}' />
                          <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af8</uuid>
                        </jobref>
                      </command>
                    </sequence>
                    <uuid>a6d88d66-920b-492b-b7c4-1a22da67333b</uuid>
                  </job>
                </joblist>
            """
        def path = JobUtils.generateFileToImport(job, "xml")
        when:
        def result = jobImportFile(PROJECT_NAME, path)
        then:
        verifyAll {
            result.succeeded[0].id == "28a1fc62-92a1-4a45-bbc3-02a8b0f46af8"
            result.succeeded[1].id == "a6d88d66-920b-492b-b7c4-1a22da67333b"
        }
    }

    def "jobs-import-job-ref-validated-false"() {
        setup:
        def job = """
                    <joblist>
                      <job>
                        <context>
                          <options preserveOrder='true'>
                            <option name='Foo' />
                          </options>
                        </context>
                        <defaultTab>summary</defaultTab>
                        <description></description>
                        <executionEnabled>true</executionEnabled>
                        <group>Manage</group>
                        <id>a6d88d66-920b-492b-b7c4-1a22da67333b</id>
                        <loglevel>INFO</loglevel>
                        <name>Wrapper</name>
                        <nodeFilterEditable>false</nodeFilterEditable>
                        <scheduleEnabled>true</scheduleEnabled>
                        <sequence keepgoing='false' strategy='node-first'>
                          <command>
                            <jobref name='RefMe'>
                              <arg line='-Time \${option.Foo}' />
                              <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af0</uuid>
                            </jobref>
                          </command>
                        </sequence>
                        <uuid>a6d88d66-920b-492b-b7c4-1a22da67333c</uuid>
                      </job>
                    </joblist>
                """
        def path = JobUtils.generateFileToImport(job, "xml")
        when:
        def responseImport = client.doPost("/project/${PROJECT_NAME}/jobs/import?validateJobref=false", new File(path), "application/xml")
        def result = client.jsonValue(responseImport.body(), Map)
        then:
        verifyAll {
            responseImport.successful
            responseImport.code() == 200
            result.succeeded[0].id == "a6d88d66-920b-492b-b7c4-1a22da67333c"
        }
    }

    def "jobs-import-job-ref-validated-true"() {
        setup:
        def job = """
                    <joblist>
                      <job>
                        <context>
                          <options preserveOrder='true'>
                            <option name='Foo' />
                          </options>
                        </context>
                        <defaultTab>summary</defaultTab>
                        <description></description>
                        <executionEnabled>true</executionEnabled>
                        <group>Manage</group>
                        <id>a6d88d66-920b-492b-b7c4-1a22da67333b</id>
                        <loglevel>INFO</loglevel>
                        <name>Wrapper</name>
                        <nodeFilterEditable>false</nodeFilterEditable>
                        <scheduleEnabled>true</scheduleEnabled>
                        <sequence keepgoing='false' strategy='node-first'>
                          <command>
                            <jobref name='RefMe'>
                              <arg line='-Time \${option.Foo}' />
                              <uuid>28a1fc62-92a1-4a45-bbc3-02a8b0f46af0</uuid>
                            </jobref>
                          </command>
                        </sequence>
                        <uuid>a6d88d66-920b-492b-b7c4-1a22da67333c</uuid>
                      </job>
                    </joblist>
                    """
        def path = JobUtils.generateFileToImport(job, "xml")
        when:
        def responseImport = client.doPost("/project/${PROJECT_NAME}/jobs/import?validateJobref=true", new File(path), "application/xml")
        def result = client.jsonValue(responseImport.body(), Map)
        then:
        verifyAll {
            responseImport.successful
            responseImport.code() == 200
            result.failed.size() == 1
        }
    }

    def "import RunDeck Jobs in json format (multipart file)"() {
        setup:
            def path = generatePathJson()
        when:
            def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(path).name, RequestBody.create(new File(path), MultipartBody.FORM))
                .build()
            def responseImport = client.doPostWithMultipart("/project/${PROJECT_NAME}/jobs/import?format=json", multipartBody)
            def result = client.jsonValue(responseImport.body(), Map)
        then:
            verifyAll {
                responseImport.successful
                responseImport.code() == 200
                result.succeeded.size() == 1
            }
    }

    def "import RunDeck Jobs in json format (urlencode)"() {
        setup:
            def path = generatePathJson()
            FormBody formBody = new FormBody.Builder()
                .add("xmlBatch", new File(path).text)
                .build()
            def responseImport = client.doPostWithFormData("/project/${PROJECT_NAME}/jobs/import?format=json", formBody)
        when:
            def result = client.jsonValue(responseImport.body(), Map)
        then:
            verifyAll {
                responseImport.successful
                responseImport.code() == 200
                result.succeeded.size() == 1
            }
    }

    def "import RunDeck Jobs in yaml format (multipart file)"() {
        when:
            def path = generatePathYaml()
            def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(path).name, RequestBody.create(new File(path), MultipartBody.FORM))
                .build()
            def responseImport = client.doPostWithMultipart("/project/${PROJECT_NAME}/jobs/import?format=yaml", multipartBody)
            def result = client.jsonValue(responseImport.body(), Map)
        then:
            verifyAll {
                responseImport.successful
                responseImport.code() == 200
                result.succeeded.size() == 1
            }
    }

    def "import RunDeck Jobs in yaml format (urlencode)"() {
        when:
            def path = generatePathYaml()
            FormBody formBody = new FormBody.Builder()
                .add("xmlBatch", new File(path).text)
                .build()
            def responseImport = client.doPostWithFormData("/project/${PROJECT_NAME}/jobs/import?format=yaml", formBody)
            def result = client.jsonValue(responseImport.body(), Map)
        then:
            verifyAll {
                responseImport.successful
                responseImport.code() == 200
                result.succeeded.size() == 1
            }
    }

    def generatePathJson() {
        def job = """
                                [
                                   {
                                      "name":"cli job3",
                                      "group":"api-test/job-import",
                                      "description":"",
                                      "loglevel":"INFO",
                                      "context":{
                                          "project":"test"
                                      },
                                      "dispatch":{
                                        "threadcount":1,
                                        "keepgoing":true
                                      },
                                      "sequence":{
                                        "commands":[
                                          {
                                            "exec":"echo hello there"
                                          }
                                        ]
                                      }
                                   }
                                ]
                                """
        JobUtils.generateFileToImport(job, "json")
    }

    def generatePathXml() {
        def xml = """
                <joblist>
                    <job>
                        <name>cli job</name>
                        <group>api-test</group>
                        <description></description>
                        <loglevel>INFO</loglevel>
                        <context>
                            <project>test</project>
                        </context>
                        <dispatch>
                            <threadcount>1</threadcount>
                            <keepgoing>true</keepgoing>
                        </dispatch>
                        <sequence>
                            <command>
                                <exec>echo hello there</exec>
                            </command>
                        </sequence>
                    </job>
                </joblist>
                """
        JobUtils.generateFileToImport(xml, "xml")
    }

    def generatePathYaml() {
        def yaml = """
                -
                  project: test
                  loglevel: INFO
                  sequence:
                    keepgoing: false
                    strategy: node-first
                    commands:
                    - exec: echo hello there
                  description: '\$0'
                  group: api-test
                  name: test import yaml
                """
        JobUtils.generateFileToImport(yaml, "yaml")
    }
}
