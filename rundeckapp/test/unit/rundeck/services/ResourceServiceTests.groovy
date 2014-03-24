package rundeck.services

import com.dtolabs.rundeck.core.storage.AuthResourceTree
import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ResourceService)
class ResourceServiceTests {
/**
 * utility method to mock a class
 */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }
    void testHasResource() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            hasResource{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assert (service.hasResource(null,'abc/123'))
    }
    void testHasPath() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            hasPath{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assert service.hasPath(null,'abc/123')
    }
    void testGetResource() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            getPath{ctx,path->
                assertEquals("abc/123",path.path)
                null
            }
        }
        assertNull(service.getResource(null, 'abc/123'))
    }
    void testListDir() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            listDirectory{ctx,path->
                assertEquals("abc/123",path.path)
                [] as Set
            }
        }
        assertEquals([] as Set,service.listDir(null, 'abc/123'))
    }
    void testDelResource() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            deleteResource{ctx,path->
                assertEquals("abc/123",path.path)
                true
            }
        }
        assertEquals(true,service.delResource(null, 'abc/123'))
    }
    void testCreateResource() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            createResource{ctx,path,content->
                assertEquals("abc/123",path.path)
                assertNotNull(content.meta)
                assertEquals('data',content.readContent().text)
                null
            }
        }
        service.createResource(null, 'abc/123',[:],stream('data'))
    }
    void testUpdateResource() {
        service.authRundeckResourceTree=mockWith(AuthResourceTree){
            updateResource{ctx,path,content->
                assertEquals("abc/123",path.path)
                assertNotNull(content.meta)
                assertEquals('data', content.readContent().text)
                null
            }
        }
        service.updateResource(null, 'abc/123',[:],stream('data'))
    }

    InputStream stream(String s) {
        return new ByteArrayInputStream(s.bytes);
    }
}
