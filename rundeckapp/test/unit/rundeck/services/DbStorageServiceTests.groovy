package rundeck.services

import com.dtolabs.rundeck.core.storage.StorageUtil
import grails.test.mixin.*
import org.rundeck.storage.api.StorageException
import rundeck.Storage

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(DbStorageService)
@Mock(Storage)
class DbStorageServiceTests {

    void testHasResource() {
        assertNotNull new Storage(data: 'abc'.bytes, dir: '', name: 'abc', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes,dir: 'xyz', name:'abc',storageMeta: [abc:'xyz']).save(true)
        assertFalse(service.hasResource('xyz'))
        assertTrue(service.hasResource('xyz/abc'))
        assertTrue(service.hasResource('abc'))
    }

    protected Storage createStorage(Map props) {
        def s=new Storage()
        Storage.withSession {session->
            s.properties=props
            if(props.data){
                s.setData(props.data,session)
            }
        }
        return s.save(true)
    }

    void testHasPath() {
        assertNotNull new Storage(data: 'abc'.bytes,name: 'abc',dir: '',storageMeta: [abc:'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes,name: 'abc',dir: 'xyz',storageMeta: [abc:'xyz']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertTrue(service.hasPath(''))
        assertFalse(service.hasPath('xy'))
        assertTrue(service.hasPath('xyz'))
        assertTrue(service.hasPath('xyz/abc'))
        assertTrue(service.hasPath('xyz/monkey'))
        assertTrue(service.hasPath('xyz/monkey/tree'))
        assertTrue(service.hasPath('xyz/monkey/tree/banana.gif'))
        assertTrue(service.hasPath('abc'))
    }
    void testHasDirectory() {
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertTrue(service.hasDirectory(''))
        assertFalse(service.hasDirectory('xy'))
        assertTrue(service.hasDirectory('xyz'))
        assertFalse(service.hasDirectory('xyz/abc'))
        assertTrue(service.hasDirectory('xyz/monkey'))
        assertTrue(service.hasDirectory('xyz/monkey/tree'))
        assertFalse(service.hasDirectory('xyz/monkey/tree/banana.gif'))
        assertFalse(service.hasDirectory('abc'))
    }

    void testGetPath_dne() {
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz']).save(true)
        try {
            def path = service.getPath('xy')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetPath_exists() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)

        def res1 = service.getPath('abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('abc',res1.path.path)
        assertEquals([abc: 'xyz1'],res1.contents.meta)
        assertEquals('abc1',res1.contents.getInputStream().text)
    }
    void testGetPath_exists2() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)

        def res1 = service.getPath('xyz/abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('xyz/abc',res1.path.path)
        assertEquals([abc: 'xyz2'],res1.contents.meta)
        assertEquals('abc2',res1.contents.getInputStream().text)
    }
    void testGetPath_dir() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)

        def res1 = service.getPath('xyz')
        assertNotNull(res1)
        assertTrue(res1.directory)
        assertNull(res1.contents)
    }
    void testGetResource_dne() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource('xy')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_isdirectory() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource('xyz')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_ok() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.getResource('xyz/abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('xyz/abc', res1.path.path)
        assertEquals([abc: 'xyz2'], res1.contents.meta)
        assertEquals('abc2', res1.contents.getInputStream().text)
    }
    void testGetResource_ok2() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.getResource('abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('abc', res1.path.path)
        assertEquals([abc: 'xyz1'], res1.contents.meta)
        assertEquals('abc1', res1.contents.getInputStream().text)
    }
    void testCreateResource_exists() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.createResource('abc', StorageUtil.withStream(bytes('abc'),[abc:'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }
    void testCreateResource_ok() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.createResource('abc3', StorageUtil.withStream(bytes('abc'),[abc:'xyz3']))
        assertNotNull(res1)
        assertEquals('abc3',res1.path.path)
        assertEquals(false,res1.directory)
        assertEquals([abc:'xyz3'],res1.contents.meta)
        assertEquals('abc',res1.contents.getInputStream().text)

        Storage.withNewSession {

            def store1 = Storage.findByDirAndName('', 'abc3')
            assertNotNull(store1)
            assertEquals('', store1.dir)
            assertEquals('abc3', store1.name)
            assertEquals('abc3', store1.path)
            assertEquals([abc: 'xyz3'], store1.storageMeta)
            assertEquals('abc'.bytes, store1.data)
        }
    }

    void testUpdateResource_notfound() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.updateResource('abc2', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testUpdateResource_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.updateResource('abc', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
        assertNotNull(res1)
        assertEquals('abc', res1.path.path)
        assertEquals(false, res1.directory)
        assertEquals([abc: 'xyz3'], res1.contents.meta)
        assertEquals('abc', res1.contents.getInputStream().text)

        Storage.withNewSession {

            def store1 = Storage.findByDirAndName('', 'abc')
            assertEquals(store1, Storage.get(storage1.id))
            assertNotNull(store1)
            assertEquals('abc', store1.path)
            assertEquals('', store1.dir)
            assertEquals('abc', store1.name)
            assertEquals([abc: 'xyz3'], store1.storageMeta)
            assertEquals('abc'.bytes, store1.data)
        }
    }

    void testDeleteResource_notfound() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.deleteResource('abc2')
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testDeleteResource_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.deleteResource('abc')
        assertTrue(res1)
        def store1 = Storage.findByDirAndName('', 'abc')
        assertNull(store1)
        assertNull(Storage.get(storage1.id))
    }
    void testListDirectory_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectory('xyz')
        assertNotNull(res1)
        assertEquals(4, res1.size())
        def found1=res1.find{it.path.path=='xyz/abc'}
        def found2=res1.find{it.path.path=='xyz/abc3'}
        def found3=res1.find{it.path.path=='xyz/pyx'}
        def found4=res1.find{it.path.path=='xyz/monkey'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
        assertNotNull(found2)
        assertEquals(false, found2.directory)
        assertNotNull('xyz/pyx not found in '+res1.collect{it.path}.join("; "),found3)
        assertEquals(true, found3.directory)
        assertNotNull('xyz/pyx not found in '+res1.collect{it.path}.join("; "),found4)
        assertEquals(true, found4.directory)
    }
    void testListDirectory_root() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectory('')
        assertNotNull(res1)
        assertEquals(3, res1.size())
        def found1=res1.find{it.path.path=='abc'}
        def found2=res1.find{it.path.path=='xyz'}
        def found3=res1.find{it.path.path=='zinc'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
        assertNotNull(found2)
        assertEquals(true, found2.directory)
        assertNotNull('zing not found in '+res1.collect{it.path}.join("; "),found3)
        assertEquals(true, found3.directory)
    }
    void testListDirectorySubdirs_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectorySubdirs('xyz')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found3=res1.find{it.path.path=='xyz/pyx'}
        assertNotNull(found3)
        assertEquals(true, found3.directory)
        def found2=res1.find{it.path.path=='xyz/monkey'}
        assertNotNull(found2)
        assertEquals(true, found2.directory)
    }
    void testListDirectorySubdirs_root() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectorySubdirs('')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found3=res1.find{it.path.path=='xyz'}
        assertNotNull(found3)
        assertEquals(true, found3.directory)
        def found2=res1.find{it.path.path=='zinc'}
        assertNotNull(found2)
        assertEquals(true, found2.directory)
    }
    void testListDirectoryResources_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectoryResources('xyz')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found1=res1.find{it.path.path=='xyz/abc'}
        def found2=res1.find{it.path.path=='xyz/abc3'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
        assertNotNull(found2)
        assertEquals(false, found2.directory)
    }

    void testListDirectoryResources_root() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectoryResources('')
        assertNotNull(res1)
        assertEquals(1, res1.size())
        def found1=res1.find{it.path.path=='abc'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
    }


    InputStream bytes(String s) {
        return new ByteArrayInputStream(s.bytes)
    }
}
