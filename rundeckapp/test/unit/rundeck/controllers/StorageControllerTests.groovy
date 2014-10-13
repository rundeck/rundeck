package rundeck.controllers

import com.dtolabs.rundeck.app.support.StorageParams
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import grails.test.mixin.*
import org.junit.*
import org.rundeck.storage.api.ContentMeta
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.StorageService

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(StorageController)
class StorageControllerTests {
/**
 * utility method to mock a class
 */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }
    @Test
    void apiGetResource_notfound() {
        params.resourcePath='abc'
        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubject{subject-> null }
        }
        controller.storageService=mockWith(StorageService){
            hasPath{ctx,path-> false }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }

        def result=controller.apiGetResource()
        assertEquals(404,response.status)
    }

    @Test
    void apiGetResource_foundContent() {
        params.resourcePath='abc'

        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubject{subject-> null }
        }
        def mContent = mockWith(ContentMeta) {
            getMeta(2..2){->
                ['Rundeck-content-type': 'test/data']
            }
            writeContent {OutputStream out->
                def bytes = "data1".bytes
                out.write(bytes)
                return (long)bytes.length
            }
        }
        def mRes = mockWith(Resource) {
            isDirectory{-> false }
            getContents(2..2){-> mContent }
        }
        controller.storageService=mockWith(StorageService){
            hasPath{ctx,path-> true }
            getResource{ctx,path-> mRes }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }

        def result=controller.apiGetResource()
        assertEquals(200,response.status)
        assertEquals('test/data',response.contentType)
        assertEquals('data1',response.text)
    }
    class TestRes implements Resource<ResourceMeta>{
        ResourceMeta contents
        boolean directory
        Path path
    }
    @Test
    void apiGetResource_foundDirectory() {
        params.resourcePath='abc'

        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubject{subject-> null }
        }

        def mRes1 = new TestRes(contents:StorageUtil.withStream(new ByteArrayInputStream("data1".bytes),
                ['Rundeck-content-type': 'test/data']),directory:false,path:PathUtil.asPath("abc/test1"))
        def mRes2 = new TestRes(contents: StorageUtil.withStream(new ByteArrayInputStream("data2".bytes),
                ['Rundeck-content-type': 'test/data']), directory: false, path: PathUtil.asPath("abc/test2"))
        def mRes = new TestRes(directory: true, path: PathUtil.asPath("abc"))

        controller.storageService=mockWith(StorageService){
            hasPath{ctx,path-> true }
            getResource{ctx,path-> mRes }
            listDir{ctx,path->
                [mRes1, mRes2] as Set
            }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }

        def result=controller.apiGetResource()
        assertEquals(200,response.status)
        assertEquals('application/json;charset=UTF-8',response.contentType)
        assertEquals('abc', response.json.path)
        assertEquals('directory', response.json.type)
        assertNotNull(response.json.url)
        assertEquals(2, response.json.resources.size())
        assertEquals('abc/test1', response.json.resources[0].path)
        assertEquals('file', response.json.resources[0].type)
        assertEquals('test1', response.json.resources[0].name)

    }
    @Test
    void apiPostResource_conflictFile(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> true }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        def result=controller.apiPostResource()
        assertEquals(409,response.status)
    }
    @Test
    void apiPostResource_conflictDir(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> false }
            hasPath { ctx,path -> true }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        def result=controller.apiPostResource()
        assertEquals(409,response.status)
    }
    @Test
    void apiPostResource_ok(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        def mRes2 = new TestRes(contents: StorageUtil.withStream(new ByteArrayInputStream("data2".bytes),
                ['Rundeck-content-type': 'test/data']), directory: false, path: PathUtil.asPath("abc/test2"))
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> false }
            hasPath { ctx,path -> false }
            createResource{ctx,path,meta,stream->
                mRes2
            }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        response.format='json'
        def result=controller.apiPostResource()
        assertEquals(201,response.status)
        assertEquals('application/json;charset=UTF-8',response.contentType)
        assertEquals('file',response.json.type)
        assertEquals('abc/test2',response.json.path)
        assertEquals('test2',response.json.name)
        assertEquals('test/data',response.json.meta['Rundeck-content-type'])
    }
    @Test
    void apiPostResource_exception(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        def mRes2 = new TestRes(contents: StorageUtil.withStream(new ByteArrayInputStream("data2".bytes),
                ['Rundeck-content-type': 'test/data']), directory: false, path: PathUtil.asPath("abc/test2"))
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> false }
            hasPath { ctx,path -> false }
            createResource{ctx,path,meta,stream->
                throw StorageException.createException(PathUtil.asPath("abc/test2"),"failed")
            }
        }
        controller.apiService=mockWith(ApiService){
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat{resp,map->
                assertEquals(500,map.status)
                assertEquals("failed",map.message)
            }
        }
        params.resourcePath = 'abc'
        response.format='json'
        def result=controller.apiPostResource()
    }
    @Test
    void apiPutResource_notfound(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> false }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        def result=controller.apiPutResource()
        assertEquals(404,response.status)
    }
    @Test
    void apiPutResource_ok(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        def mRes2 = new TestRes(contents: StorageUtil.withStream(new ByteArrayInputStream("data2".bytes),
                ['Rundeck-content-type': 'test/data']), directory: false, path: PathUtil.asPath("abc/test2"))
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> true }
            updateResource{ctx,path,meta,stream->
                mRes2
            }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        response.format='json'
        def result=controller.apiPutResource()
        assertEquals(200,response.status)
        assertEquals('application/json;charset=UTF-8',response.contentType)
        assertEquals('file',response.json.type)
        assertEquals('abc/test2',response.json.path)
        assertEquals('test2',response.json.name)
        assertEquals('test/data',response.json.meta['Rundeck-content-type'])
    }
    @Test
    void apiPutResource_exception(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        def mRes2 = new TestRes(contents: StorageUtil.withStream(new ByteArrayInputStream("data2".bytes),
                ['Rundeck-content-type': 'test/data']), directory: false, path: PathUtil.asPath("abc/test2"))
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx,path -> true }
            updateResource{ctx,path,meta,stream->
                throw StorageException.createException(PathUtil.asPath("abc/test2"),"failed")
            }
        }
        controller.apiService=mockWith(ApiService){
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat{resp,map->
                assertEquals(500,map.status)
                assertEquals("failed",map.message)
            }
        }
        params.resourcePath = 'abc'
        response.format='json'
        def result=controller.apiPutResource()
    }
    @Test
    void apiDeleteResource_notfound(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx, path -> false }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat { resp, map ->
                assertEquals(404, map.status)
                assertEquals("api.error.item.doesnotexist", map.code)
            }
        }
        params.resourcePath = 'abc'
        response.format = 'json'
        def result = controller.apiDeleteResource()
    }
    @Test
    void apiDeleteResource_success(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx, path -> true }
            delResource { ctx, path -> true }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
        }
        params.resourcePath = 'abc'
        def result = controller.apiDeleteResource()
        assertEquals(204,response.status)
    }
    @Test
    void apiDeleteResource_failure(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx, path -> true }
            delResource { ctx, path -> false }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat { resp, map ->
                assertEquals(500, map.status)
                assertEquals("Resource was not deleted: abc/test1", map.message)
            }
        }
        params.resourcePath = 'abc/test1'
        def result = controller.apiDeleteResource()
    }
    @Test
    void apiDeleteResource_exception(){
        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { subject -> null }
        }
        controller.storageService = mockWith(StorageService) {
            hasResource { ctx, path -> true }
            delResource { ctx, path ->
                throw StorageException.deleteException(PathUtil.asPath("abc/test2"), "failed")
            }
        }
        controller.apiService = mockWith(ApiService) {
            requireApi(1..1) { req, resp -> true }
            renderErrorFormat { resp, map ->
                assertEquals(500, map.status)
                assertEquals("failed", map.message)
            }
        }
        params.resourcePath = 'abc/test1'
        def result = controller.apiDeleteResource()
    }

    @Test
    void storageParamsValidationBasic(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/bonanza'
        assertTrue(params.validate())
    }
    @Test
    void storageParamsValidation_allowdotdot(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/bonanza..double'
        assertTrue(params.validate())
    }
    @Test
    void storageParamsValidation_allowdotdot2(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/bonanza/..double'
        assertTrue(params.validate())
    }
    @Test
    void storageParamsValidation_allowdotdot3(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/bonanza../double'
        assertTrue(params.validate())
    }
    @Test
    void storageParamsValidation_invaliddotdot(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/../bonanza'
        assertFalse("should not allow /../",params.validate())
    }
    @Test
    void storageParamsValidation_invalidspace(){
        def params=new StorageParams()
        params.resourcePath='keys/monkey/ bonanza'
        assertFalse(params.validate())
    }
}
