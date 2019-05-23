package rundeck

import com.rundeck.repository.client.RepositoryClient
import com.rundeck.repository.definition.RepositoryDefinition
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class RepositoryTagLibSpec extends Specification implements TagLibUnitTest<RepositoryTagLib> {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        setup:
        def mockRepoClient = Mock(RepositoryClient) {
            listRepositories() >> { [
                    new RepositoryDefinition(repositoryName: "One",enabled: true),
                    new RepositoryDefinition(repositoryName: "Two",enabled: false)
            ] }
        }
        tagLib.repoClient = mockRepoClient
        expect:
            tagLib.listRepos() == "<li>One</li>"
    }
}
