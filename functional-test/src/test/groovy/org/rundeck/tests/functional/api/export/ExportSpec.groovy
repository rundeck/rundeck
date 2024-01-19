package org.rundeck.tests.functional.api.export

import okhttp3.Headers
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared

@APITest
class ExportSpec extends BaseContainer {

    @Shared String jobId

    def setupSpec() {
        startEnvironment()
        setupProject()
        def path = updateFile("job-template-common.xml")
        jobId = jobImportFile(path).succeeded[0].id
    }

    def "export single job in jobs.json format (format param) and (Accept header)"() {
        when:
             def data = doGet("/job/${jobId}?format=json")
        then:
            verifyAll {
                data.successful
                data.code() == 200
                def json = jsonValue(data.body(), List)
                json.size() == 1
                json[0].id == jobId
            }
    }

    def "export job with wrong ID"() {
        when:
            def data = doGet("/job/9000")
        then:
            verifyAll {
                !data.successful
                data.code() == 404
                def json = jsonValue(data.body())
                json.message == "Job ID does not exist: 9000"
            }
    }

    def "export single job in unsupported format (Accept header)"() {
        when:
            Headers headers = new Headers.Builder()
                .add("Accept", "text/csv")
                .build()
            client.apiVersion = version
            def data = client.doGetAddHeaders("/job/${jobId}", headers)
        then:
            verifyAll {
                !data.successful
                data.code() == 415
                def json = jsonValue(data.body())
                json.errorCode == "api.error.item.unsupported-format"
                json.message == "The format is not valid: csv"
            }
        where:
            version << [45, 43]
    }

    def "export single job in unsupported format (format param)"() {
        when:
            def data = doGet("/job/${jobId}?format=csv")
        then:
        verifyAll {
            !data.successful
            data.code() == 415
            def json = jsonValue(data.body())
            json.errorCode == "api.error.item.unsupported-format"
            json.message == "The format is not valid: csv"
        }
    }

    def "export single job in jobs.xml format (format param)"() {
        when:
            def data = doGet("/job/${jobId}?format=xml")
        then:
            verifyAll {
                data.successful
                data.code() == 200
                def xml = validateXml(data.body().string())
                xml == "OK"
            }
    }

    def "export single job in jobs.xml format (Accept header)"() {
        when:
            Headers headers = new Headers.Builder()
                    .add("Accept", "application/xml")
                    .build()
            client.apiVersion = version
            def data = client.doGetAddHeaders("/job/${jobId}", headers)
        then:
            verifyAll {
                data.successful
                data.code() == 200
                def xml = validateXml(data.body().string())
                xml == "OK"
            }
        where:
            version << [45, 43]
    }

    def "export single job in jobs.yaml format (format param)"() {
        when:
            def data = doGet("/job/${jobId}?format=yaml")
        then:
            verifyAll {
                data.successful
                data.code() == 200
                def yaml = validateYaml(data.body().string())
                yaml == "OK"
            }
    }

    def "export single job in jobs.yaml format (Accept header)"() {
        when:
            Headers headers = new Headers.Builder()
                    .add("Accept", "application/yaml")
                    .build()
            client.apiVersion = version
            def data = client.doGetAddHeaders("/job/${jobId}", headers)
        then:
            verifyAll {
                data.successful
                data.code() == 200
                def yaml = validateYaml(data.body().string())
                yaml == "OK"
            }
        where:
            version << [45, 43]
    }

    def validateXml(String xmlContent) {
        if (!xmlContent.contains('<') || !xmlContent.contains('>')) {
            return "ERROR: Response was not valid XML"
        }
        if (!xmlContent.contains('<joblist>')) {
            return "ERROR: Response did not contain expected result"
        }
        def jobMatches = xmlContent =~ /(?s)<job>.*?<\/job>/
        if (jobMatches) {
            String jobIdAux = ""
            String jobUuid = ""
            for (jobMatch in jobMatches) {
                def idMatch = jobMatch =~ /<id>(.*?)<\/id>/
                def uuidMatch = jobMatch =~ /<uuid>(.*?)<\/uuid>/
                if (idMatch && uuidMatch) {
                    jobIdAux = idMatch[0][1]
                    jobUuid = uuidMatch[0][1]
                }
            }
            def countJobId = jobIdAux.tokenize().findAll { it == jobId } size()
            return (countJobId == 1 && jobUuid == jobId) ?
                    "OK" :
                    "Wrong job count: ${countJobId}, or wrong found id: ${jobUuid}"
        }
    }

    def validateYaml(String yamlContent) {
        print yamlContent
        if (!yamlContent.contains(':') || !yamlContent.contains('\n')) {
            return "ERROR: Response was not valid YAML"
        }
        def yamlMap = [:]
        yamlContent.split('\n').each { line ->
            def parts = line.trim().split(':', 2)
            if (parts.size() == 2) {
                yamlMap[parts[0].trim()] = parts[1].trim()
            }
        }
        if (!yamlMap.containsKey('name')) {
            return "ERROR: Response did not contain expected result"
        }
        String jobIdAux = yamlMap['id']
        String jobUuid = yamlMap['uuid']
        return (jobIdAux == jobId && jobUuid == jobId) ?
                "OK" :
                "Wrong job id: ${jobUuid}"
    }

}
