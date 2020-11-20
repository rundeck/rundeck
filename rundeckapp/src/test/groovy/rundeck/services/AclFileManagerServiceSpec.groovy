package rundeck.services

import com.dtolabs.rundeck.core.storage.ResourceMeta
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.acl.AppACLContext
import org.rundeck.storage.api.Resource
import spock.lang.Specification
import spock.lang.Unroll

class AclFileManagerServiceSpec extends Specification implements ServiceUnitTest<AclFileManagerService> {

    def setup() {
    }

    def cleanup() {
    }

    @Unroll
    void "list policy files"() {
        given:
            def files = ['test.aclpolicy', 'test2.aclpolicy']
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
        when:
            def result = service.listStoredPolicyFiles(ctx)

        then:
            1 * service.configStorageService.listDirPaths(base, '.*\\.aclpolicy') >> paths

            result == files
        where:
            ctx                               | base | paths
            AppACLContext.system()            | 'acls/' | ['acls/test.aclpolicy', 'acls/test2.aclpolicy']
            AppACLContext.project('aproject') | 'projects/aproject/acls/' | ['projects/aproject/acls/test.aclpolicy', 'projects/aproject/acls/test2.aclpolicy']
    }


    @Unroll
    void "exists file"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
        when:
            def result = service.existsPolicyFile(ctx,name)
        then:
            1 * service.configStorageService.existsFileResource("${base}${name}") >> expect
            result == expect
        where:
            name             | expect | ctx                               | base
            'test.aclpolicy' | true   | AppACLContext.system()            | 'acls/'
            'test.aclpolicy' | true   | AppACLContext.project('aproject') | 'projects/aproject/acls/'
            'dne.aclpolicy'  | false  | AppACLContext.system()            | 'acls/'
            'dne.aclpolicy'  | false  | AppACLContext.project('aproject') | 'projects/aproject/acls/'

    }

    @Unroll
    def "get contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
        when:
            def result = service.getPolicyFileContents(ctx,name)

        then:
            1 * service.configStorageService.getFileResource("${base}${name}") >> Mock(Resource) {
                getContents() >> Mock(ResourceMeta) {
                    getInputStream() >> {
                        return new ByteArrayInputStream("$name".bytes)
                    }
                }
            }
            result == name
        where:
            name             | ctx                               | base
            'test.aclpolicy' | AppACLContext.system()            | 'acls/'
            'test.aclpolicy' | AppACLContext.project('aproject') | 'projects/aproject/acls/'
    }

    def "load contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
            def os = new ByteArrayOutputStream()

        when:
            def result = service.loadPolicyFileContents(ctx,name, os)

        then:
            1 * service.configStorageService.loadFileResource("${base}${name}", os) >> 123L
            result == 123L
        where:
            name             | ctx                               | base
            'test.aclpolicy' | AppACLContext.system()            | 'acls/'
            'test.aclpolicy' | AppACLContext.project('aproject') | 'projects/aproject/acls/'
    }

    def "get acl policy"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
            Date date = new Date()
            Date created = new Date(123L)
            def text = 'content text'
            def data = text.bytes
            def bais = new ByteArrayInputStream(data)
        when:
            def result = service.getAclPolicy(ctx,name)
            def tresult=result.inputStream.text //have to read this here first, otherwise it seems the lazy read interferes with spock mocking
        then:
            1 * service.configStorageService.getFileResource("${base}${name}") >> Stub(Resource) {
                getContents() >> Stub(ResourceMeta) {
                    getInputStream() >> bais
                    getModificationTime() >> date
                    getCreationTime() >> created
                }
            }
            result
            tresult == text
            result.name == name
            result.modified == date
            result.created == created
        where:
            name             | ctx                               | base
            'test.aclpolicy' | AppACLContext.system()            | 'acls/'
            'test.aclpolicy' | AppACLContext.project('aproject') | 'projects/aproject/acls/'
    }

    def "store contents"() {
        given:
            service.configStorageService = Mock(ConfigStorageService)
            service.afterPropertiesSet()
            def os = new ByteArrayOutputStream()

        when:
            def result = service.storePolicyFileContents(ctx,name, text)

        then:
            1 * service.configStorageService.writeFileResource("${base}${name}", { it.text == text }, _) >> Mock(Resource)
            result == text.length()
        where:
            name             | ctx                               | base
            'test.aclpolicy' | AppACLContext.system()            | 'acls/'
            'test.aclpolicy' | AppACLContext.project('aproject') | 'projects/aproject/acls/'

            text = 'text content'
    }

}
