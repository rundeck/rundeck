package rundeck.services

import com.dtolabs.rundeck.core.storage.ResourceMeta
import grails.testing.services.ServiceUnitTest
import org.rundeck.storage.api.ContentMeta
import org.rundeck.storage.api.Resource
import spock.lang.Specification

class AclManagerServiceSpec extends Specification implements ServiceUnitTest<AclManagerService> {

    def setup() {
    }

    def cleanup() {
    }

    void "list paths"() {
        given:
            def paths = ['acls/test.aclpolicy', 'acls/test2.aclpolicy']
            service.configStorageService = Mock(ConfigStorageService)
        when:
            def result = service.listStoredPolicyPaths()

        then:
            1 * service.configStorageService.listDirPaths('acls/', '.*\\.aclpolicy') >> paths
            result == paths
    }

    void "list files"() {
        given:
            def paths = ['acls/test.aclpolicy', 'acls/test2.aclpolicy']
            def files = ['test.aclpolicy', 'test2.aclpolicy']
            service.configStorageService = Mock(ConfigStorageService)
        when:
            def result = service.listStoredPolicyFiles()

        then:
            1 * service.configStorageService.listDirPaths('acls/', '.*\\.aclpolicy') >> paths
            result == files
    }

    void "exists file"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
        when:
            def result = service.existsPolicyFile(name)
        then:
            1 * service.configStorageService.existsFileResource("acls/$name") >> expect
            result == expect
        where:
            name             | expect
            'test.aclpolicy' | true
            'dne.aclpolicy'  | false
    }

    def "get contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
        when:
            def result = service.getPolicyFileContents(name)

        then:
            1 * service.configStorageService.getFileResource("acls/$name") >> Mock(Resource) {
                getContents() >> Mock(ResourceMeta) {
                    getInputStream() >> {
                        return new ByteArrayInputStream("$name".bytes)
                    }
                }
            }
            result == name
        where:
            name = 'test.aclpolicy'
    }

    def "load contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            def os = new ByteArrayOutputStream()

        when:
            def result = service.loadPolicyFileContents(name, os)

        then:
            1 * service.configStorageService.loadFileResource("acls/$name", os) >> 123L
            result == 123L
        where:
            name = 'test.aclpolicy'
    }

    def "get acl policy"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            Date date = new Date()
            Date created = new Date(123L)
            def name = 'test.aclpolicy'
            def text = 'content text'
            def data = text.bytes
            def bais = new ByteArrayInputStream(data)
        when:
            def result = service.getAclPolicy(name)
            result.text //have to read this here first, otherwise it seems the lazy read interferes with spock mocking
        then:
            1 * service.configStorageService.getFileResource("acls/$name") >> Stub(Resource) {
                getContents() >> Stub(ResourceMeta) {
                    getInputStream() >> bais
                    getModificationTime() >> date
                    getCreationTime() >> created
                }
            }
            result
            result.text == text
            result.name == name
            result.modified == date
            result.created == created
    }

    def "store contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            def os = new ByteArrayOutputStream()

        when:
            def result = service.storePolicyFileContents(name, text)

        then:
            1 * service.configStorageService.writeFileResource("acls/$name", { it.text == text }, _) >> Mock(Resource)
            result == text.length()
        where:
            name = 'test.aclpolicy'
            text = 'text content'
    }

}
