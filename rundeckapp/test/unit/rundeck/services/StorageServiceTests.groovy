package rundeck.services

import com.dtolabs.rundeck.core.storage.AuthStorageTree
import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(StorageService)
class StorageServiceTests {
/**
 * utility method to mock a class
 */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }
    void testHasResource() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            hasResource{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assert (service.hasResource(null,'abc/123'))
    }
    void testHasPath() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            hasPath{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assert service.hasPath(null,'abc/123')
    }
    void testGetResource() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            getPath{ctx,path->
                assertEquals("abc/123",path.path)
                null
            }
        }
        assertNull(service.getResource(null, 'abc/123'))
    }
    void testListDir() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            listDirectory{ctx,path->
                assertEquals("abc/123",path.path)
                [] as Set
            }
        }
        assertEquals([] as Set,service.listDir(null, 'abc/123'))
    }
    void testDelResource() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            deleteResource{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assertEquals(true,service.delResource(null, 'abc/123'))
    }
    void testCreateResource() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            createResource{ctx,path,content->
                assertEquals("abc/123",path.path)
                assertNotNull(content.meta)
                assertEquals('data',content.getInputStream().text)
                null
            }
        }
        service.createResource(null, 'abc/123',[:],stream('data'))
    }
    void testUpdateResource() {
        service.authRundeckStorageTree=mockWith(AuthStorageTree){
            updateResource{ctx,path,content->
                assertEquals("abc/123",path.path)
                assertNotNull(content.meta)
                assertEquals('data', content.getInputStream().text)
                null
            }
        }
        service.updateResource(null, 'abc/123',[:],stream('data'))
    }

    InputStream stream(String s) {
        return new ByteArrayInputStream(s.bytes);
    }
}
