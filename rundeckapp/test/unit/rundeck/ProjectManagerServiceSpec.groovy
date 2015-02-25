package rundeck

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import grails.test.mixin.TestFor
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProjectManagerService)
class ProjectManagerServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "merge properties no conflict"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        3==result.size()
        "123"==result.getProperty("abc")
        "456"==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties override"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("abc","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "789"==result.getProperty("abc")
        "456"==result.getProperty("def")

    }

    void "merge properties remove prefix"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties remove prefix multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }

    void "merge properties remove multiple prefixes multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("abcdef","488")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de','ab']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        1==result.size()
        null==result.getProperty("abc")
        null==result.getProperty("abcdef")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }

    void "storage exists test"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            hasResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.existsProjectFileResource("test1","my-resource")
        !service.existsProjectFileResource("test1","not-my-resource")
    }
    void "storage read test"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            getResource("projects/test1/my-resource") >> resStub
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.readProjectFileResource("test1","my-resource",baos)

        then:
        6==len
        'abcdef'==baos.toString()
    }
    void "storage read does not exist"(){
        given:

        service.storage=Stub(StorageTree){
            getResource("projects/test1/my-resource") >> {
                throw StorageException.readException(PathUtil.asPath('projects/test1/my-resource'), "does not exist")
            }
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.readProjectFileResource("test1","my-resource",baos)

        then:
        StorageException e = thrown()
        e.message.contains('does not exist')
    }
    void "storage create test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            createResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.createProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage update test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            updateResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.updateProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage write test new resource"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> false
            createResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage write test existing resource"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            updateResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage delete test existing resource"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            deleteResource("projects/test1/my-resource") >> true
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test missing resource"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test existing resource fails"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            deleteResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        !result
    }
}
