package rundeck.services

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class GithubJobDescriptionsServiceSpec extends Specification implements ServiceUnitTest<GithubJobDescriptionsService> {

    def "test createOrUpdateFile"() {
        given: "A GithubJobDescriptionsService instance"

        and: "File details"
        def filePath = "test_file.txt"
        def commitMessage = "Test commit"
        def fileContent = "This is a test file content."

        when: "createOrUpdateFile is called"
        def sha = service.createOrUpdateFile(filePath, commitMessage, fileContent)

        then: "The SHA of the created or updated file is returned"
        sha != null
    }
}