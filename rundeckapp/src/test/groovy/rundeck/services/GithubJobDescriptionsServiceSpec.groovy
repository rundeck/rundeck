package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import spock.lang.Ignore
import spock.lang.Specification

class GithubJobDescriptionsServiceSpec extends Specification implements ServiceUnitTest<GithubJobDescriptionsService> {

    @Ignore
    def "test createOrUpdateFile"() {
        given: "A GithubJobDescriptionsService instance"

        and: "File details"
        def filePath = "test_file.txt"
        def commitMessage = "Test commit"
        def fileContent = "This is a test file content."

        // Github  API token (legacy, classical)
        def token = System.getenv("GITHUB_TOKEN")

        IRundeckProject projectProps = Mock(IRundeckProject) {
            getProperty('project.job-description-gen.storage.key') >> token
            getProperty('project.job-description-gen.storage.repo') >> "sample_rundeck_jobs"
            getProperty('project.job-description-gen.storage.owner') >> "mrdubr"
        }

        when: "createOrUpdateFile is called"
        def sha = service.createOrUpdateFile(projectProps, filePath, commitMessage, fileContent)

        then: "The SHA of the created or updated file is returned"
        sha != null
    }
}