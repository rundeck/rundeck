package rundeck.services

import com.dtolabs.rundeck.core.storage.StorageUtil
import grails.test.mixin.*
import org.codehaus.groovy.grails.plugins.codecs.SHA1Codec
import org.rundeck.storage.api.StorageException
import rundeck.Storage

@TestFor(DbStorageService)
@Mock(Storage)
class DbStorageServiceTests {
    void setUp() {
        mockCodec(SHA1Codec)
    }

    void testHasResource() {
        def storage = new Storage(data: 'abc'.bytes, dir: '', name: 'abc', storageMeta: [abc: 'xyz'])
        storage.validate()
        assertNotNull(storage.errors.allErrors.collect{it.toString()}.join("; "), storage.save(true))
        assertNotNull new Storage(data: 'abc'.bytes,dir: 'xyz', name:'abc',storageMeta: [abc:'xyz']).save(true)
        assertFalse(service.hasResource(null,'xyz'))
        assertTrue(service.hasResource(null,'xyz/abc'))
        assertTrue(service.hasResource(null,'abc'))
    }
    void testHasResource_Ns() {
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes, dir: '', name: 'abc',
                storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes, dir: 'xyz', name: 'abc',
                storageMeta: [abc: 'xyz']).save(true)
        assertFalse(service.hasResource(null,'xyz'))
        assertFalse(service.hasResource(null,'xyz/abc'))
        assertFalse(service.hasResource(null,'abc'))
        assertFalse(service.hasResource('zyx', 'xyz'))
        assertTrue(service.hasResource('zyx', 'xyz/abc'))
        assertTrue(service.hasResource('zyx', 'abc'))
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
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes,name: 'abc',dir: '',
                storageMeta: [abc:'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes,name: 'abc',dir: 'xyz',storageMeta: [abc:'xyz']).save(true)
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes,name: 'abc2',dir: 'xyz',
                storageMeta: [abc:'xyz']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)

        assertTrue(service.hasPath(null,''))
        assertTrue(service.hasPath(null,'/'))
        assertTrue(service.hasPath('zyx',''))
        assertTrue(service.hasPath('zyx','/'))
        assertFalse(service.hasPath(null,'xy'))
        assertTrue(service.hasPath(null,'xyz'))
        assertTrue(service.hasPath(null,'xyz/abc'))
        assertFalse(service.hasPath(null,'xyz/abc2'))
        assertFalse(service.hasPath('zyx','xyz/abc'))
        assertTrue(service.hasPath('zyx','xyz/abc2'))
        assertTrue(service.hasPath(null,'xyz/monkey'))
        assertTrue(service.hasPath(null,'xyz/monkey/tree'))
        assertTrue(service.hasPath(null,'xyz/monkey/tree/banana.gif'))
        assertTrue(service.hasPath(null,'abc'))
        assertTrue(service.hasPath('zyx','abc'))
    }
    void testHasPathEmptyDB() {
        assertTrue(service.hasPath(null,''))
        assertTrue(service.hasPath(null,'/'))
        assertTrue(service.hasPath('zyx',''))
        assertTrue(service.hasPath('zyx','/'))
        assertFalse(service.hasPath(null,'xy'))
        assertFalse(service.hasPath(null,'xyz'))
        assertFalse(service.hasPath(null,'xyz/abc'))
        assertFalse(service.hasPath(null,'xyz/abc2'))
        assertFalse(service.hasPath('zyx','xyz/abc'))
        assertFalse(service.hasPath('zyx','xyz/abc2'))
        assertFalse(service.hasPath(null,'xyz/monkey'))
        assertFalse(service.hasPath(null,'xyz/monkey/tree'))
        assertFalse(service.hasPath(null,'xyz/monkey/tree/banana.gif'))
        assertFalse(service.hasPath(null,'abc'))
        assertFalse(service.hasPath('zyx','abc'))
    }
    void testHasDirectory() {
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(namespace: 'zyx', data: 'abc'.bytes, name: 'abc', dir: 'xyz2',
                storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)

        assertTrue(service.hasDirectory(null,''))
        assertTrue(service.hasDirectory('zyx',''))
        assertFalse(service.hasDirectory(null,'xy'))
        assertFalse(service.hasDirectory('zyx','xy'))
        assertTrue(service.hasDirectory(null,'xyz'))
        assertFalse(service.hasDirectory('zyx','xyz'))
        assertFalse(service.hasDirectory(null,'xyz2'))
        assertTrue(service.hasDirectory('zyx','xyz2'))
        assertFalse(service.hasDirectory(null,'xyz/abc'))
        assertFalse(service.hasDirectory('zyx','xyz/abc'))
        assertTrue(service.hasDirectory(null,'xyz/monkey'))
        assertFalse(service.hasDirectory('zyx','xyz/monkey'))
        assertTrue(service.hasDirectory(null,'xyz/monkey/tree'))
        assertFalse(service.hasDirectory('zyx','xyz/monkey/tree'))
        assertFalse(service.hasDirectory(null,'xyz/monkey/tree/banana.gif'))
        assertFalse(service.hasDirectory('zyx','xyz/monkey/tree/banana.gif'))
        assertFalse(service.hasDirectory(null,'abc'))
        assertFalse(service.hasDirectory('zyx','abc'))
    }

    void testGetPath_dne() {
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz']).save(true)
        assertNotNull new Storage(data: 'abc'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz']).save(true)
        try {
            def path = service.getPath(null,'xy')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetPath_exists() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)

        def res1 = service.getPath(null,'abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('abc',res1.path.path)
        assertEquals([abc: 'xyz1'],res1.contents.meta)
        assertEquals('abc1',res1.contents.getInputStream().text)
    }
    void testGetPath_exists2() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)

        def res1 = service.getPath(null,'xyz/abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('xyz/abc',res1.path.path)
        assertEquals([abc: 'xyz2'],res1.contents.meta)
        assertEquals('abc2',res1.contents.getInputStream().text)
    }
    void testGetPath_dir() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)

        def res1 = service.getPath(null,'xyz')
        assertNotNull(res1)
        assertTrue(res1.directory)
        assertNull(res1.contents)
    }
    void testGetResource_dne() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource(null,'xy')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_ns_dne() {
        assertNotNull new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource(null,'abc')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_ns_dne2() {
        assertNotNull new Storage( data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource('other','abc')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_isdirectory() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def path = service.getResource(null,'xyz')
            fail("expected exception")
        } catch (StorageException e) {
        }
    }
    void testGetResource_ok() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        def res1 = service.getResource(null,'xyz/abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('xyz/abc', res1.path.path)
        assertEquals([abc: 'xyz2'], res1.contents.meta)
        assertEquals('abc2', res1.contents.getInputStream().text)
    }
    void testGetResource_ns_ok() {
        assertNotNull new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        def res1 = service.getResource('other','xyz/abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('xyz/abc', res1.path.path)
        assertEquals([abc: 'xyz2'], res1.contents.meta)
        assertEquals('abc2', res1.contents.getInputStream().text)
    }
    void testGetResource_ok2() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        def res1 = service.getResource(null,'abc')
        assertNotNull(res1)
        assertFalse(res1.directory)
        assertEquals('abc', res1.path.path)
        assertEquals([abc: 'xyz1'], res1.contents.meta)
        assertEquals('abc1', res1.contents.getInputStream().text)
    }
    void testGetResource_ok_blankns() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        def res1 = service.getResource('','abc')
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
            def res1 = service.createResource(null,'abc', StorageUtil.withStream(bytes('abc'),[abc:'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testCreateResource_ns_ok() {
        def s1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:
                true)
        assertNotNull s1
        assertNotNull s1.id
        def s2 = new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush: true)
        assertNotNull s2
        assertNotNull s2.id
        def c2 = Storage.createCriteria()
        def list2 = c2.list {
            and {
                isNull('namespace')
            }
        }
        assertEquals(2, list2.size())
        def res1 = service.createResource('other', 'abc', StorageUtil.withStream(bytes('abc2'), [abc: 'xyz3']))
        assertEquals('abc', res1.path.path)
        assertEquals(false, res1.directory)
        assertEquals([abc: 'xyz3'], res1.contents.meta)
        assertEquals('abc2', res1.contents.getInputStream().text)
        c2 = Storage.createCriteria()
        list2 = c2.list {
            and {
                isNull('namespace')
            }
        }
        assertEquals(2, list2.size())
        Storage.withNewSession {

        def list3 = Storage.findAllByNamespace('other')
        assertEquals(1,list3.size())
        def list = Storage.list()
        assertEquals(3, list.size())
        list3 = Storage.findAllByNamespaceIsNullAndNameAndDir('abc','')
        assertEquals(1, list3.size())
        def store0 = list3[0]
        store0.refresh()
        assertNotNull(store0)
        assertEquals(null, store0.getNamespace())
        assertEquals('', store0.dir)
        assertEquals('abc', store0.name)
        assertEquals('abc', store0.path)
        assertEquals([abc: 'xyz1'], store0.storageMeta)
        assertEquals('abc1'.bytes, store0.data)
        def store1 = Storage.findByNamespaceAndDirAndName('other', '', 'abc')
        assertNotNull(store1)
        assertEquals('other', store1.namespace)
        assertEquals('', store1.dir)
        assertEquals('abc', store1.name)
        assertEquals('abc', store1.path)
        assertEquals([abc: 'xyz3'], store1.storageMeta)
        assertEquals('abc2'.bytes, store1.data)
        }
    }

    void testCreateResource_ok() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.createResource(null,'abc3', StorageUtil.withStream(bytes('abc'),[abc:'xyz3']))
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
            def res1 = service.updateResource(null,'abc2', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testUpdateResource_notfound_ns() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.updateResource('other','abc2', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }
    void testUpdateResource_notfound_ns2() {
        assertNotNull new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(namespace: 'other', data: 'abc2'.bytes, name: 'abc', dir: 'xyz',
                storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.updateResource('other','abc2', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testUpdateResource_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.updateResource(null,'abc', StorageUtil.withStream(bytes('abc'), [abc: 'xyz3']))
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
    void testUpdateResource_ok_ns() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        def storage2 = new Storage(namespace: 'other', data: 'abc2'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage2
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.updateResource('other','abc', StorageUtil.withStream(bytes('abc3'), [abc: 'xyz3']))
        assertNotNull(res1)
        assertEquals('abc', res1.path.path)
        assertEquals(false, res1.directory)
        assertEquals([abc: 'xyz3'], res1.contents.meta)
        assertEquals('abc3', res1.contents.getInputStream().text)

        Storage.withNewSession {

            def store1 = Storage.findByNamespaceAndDirAndName(null,'', 'abc')
            assertEquals(store1, Storage.get(storage1.id))
            assertNotNull(store1)
            assertEquals(null, store1.namespace)
            assertEquals('abc', store1.path)
            assertEquals('', store1.dir)
            assertEquals('abc', store1.name)
            assertEquals([abc: 'xyz1'], store1.storageMeta)
            assertEquals('abc1'.bytes, store1.data)
            def store2 = Storage.findByNamespaceAndDirAndName('other','', 'abc')
            assertEquals(store2.id, storage2.id)
            assertNotNull(store2)
            assertEquals('other', store2.namespace)
            assertEquals('abc', store2.path)
            assertEquals('', store2.dir)
            assertEquals('abc', store2.name)
            assertEquals([abc: 'xyz3'], store2.storageMeta)
            assertEquals('abc3'.bytes, store2.data)
        }
    }

    void testDeleteResource_notfound() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.deleteResource(null,'abc2')
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testDeleteResource_notfound_ns() {
        assertNotNull new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull new Storage(namespace: 'other', data: 'abc2'.bytes, name: 'abc', dir: 'xyz',
                storageMeta: [abc: 'xyz2']).save(true)
        try {
            def res1 = service.deleteResource('other','abc')
            fail("expected error")
        } catch (StorageException e) {
        }
    }

    void testDeleteResource_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        def res1 = service.deleteResource(null,'abc')
        assertTrue(res1)
        def store1 = Storage.findByNamespaceAndDirAndName(null,'', 'abc')
        assertNull(store1)
        assertNull(Storage.get(storage1.id))
    }
    void testDeleteResource_ns_ok() {
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        def storage2 = new Storage(namespace: 'other', data: 'abc2'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull storage2
        def res1 = service.deleteResource('other','abc')
        assertTrue(res1)
        def store1 = Storage.findByNamespaceAndDirAndName(null,'', 'abc')
        assertNotNull(store1)
        assertNotNull(Storage.get(storage1.id))
        assertEquals(storage1.id,store1.id)
        def store2 = Storage.findByNamespaceAndDirAndName('other','', 'abc')
        assertNull(store2)
        assertNull(Storage.get(storage2.id))
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
        def res1 = service.listDirectory(null,'xyz')
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
    void testListDirectory_ns_ok() {
        def storage1 = new Storage(namespace:'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other', data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other', data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectory(null,'xyz')
        def res2 = service.listDirectory('other','xyz')
        assertNotNull(res1)
        assertNotNull(res2)
        assertEquals(2, res1.size())
        assertEquals(2, res2.size())

        assertNotNull res1.find{it.path.path=='xyz/abc'}
        assertNull res1.find{it.path.path=='xyz/abc3'}
        assertNull res1.find{it.path.path=='xyz/pyx'}
        assertNotNull res1.find{it.path.path=='xyz/monkey'}


        assertNull res2.find{it.path.path=='xyz/abc'}
        assertNotNull res2.find{it.path.path=='xyz/abc3'}
        assertNotNull res2.find{it.path.path=='xyz/pyx'}
        assertNull res2.find{it.path.path=='xyz/monkey'}


        def found1=res1.find{it.path.path=='xyz/abc'}
        def found2=res2.find{it.path.path=='xyz/abc3'}
        def found3=res2.find{it.path.path=='xyz/pyx'}
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
        def res1 = service.listDirectory(null,'')
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
    void testListDirectory_root_ns() {
        def storage1 = new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectory(null,'')
        assertNotNull(res1)
        assertEquals(0, res1.size())
        res1 = service.listDirectory('other','')
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
        def res1 = service.listDirectorySubdirs(null,'xyz')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found3=res1.find{it.path.path=='xyz/pyx'}
        assertNotNull(found3)
        assertEquals(true, found3.directory)
        def found2=res1.find{it.path.path=='xyz/monkey'}
        assertNotNull(found2)
        assertEquals(true, found2.directory)
    }
    void testListDirectorySubdirs_ns_ok() {
        def storage1 = new Storage(namespace: 'other',data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectorySubdirs(null,'xyz')
        assertNotNull(res1)
        assertEquals(0, res1.size())
        res1 = service.listDirectorySubdirs('other','xyz')
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
        def res1 = service.listDirectorySubdirs(null,'')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found3=res1.find{it.path.path=='xyz'}
        assertNotNull(found3)
        assertEquals(true, found3.directory)
        def found2=res1.find{it.path.path=='zinc'}
        assertNotNull(found2)
        assertEquals(true, found2.directory)
    }
    void testListDirectorySubdirs_ns_root() {
        def storage1 = new Storage(namespace: 'other',data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(true)

        def res1 = service.listDirectorySubdirs(null,'')
        assertNotNull(res1)
        assertEquals(0, res1.size())

        res1 = service.listDirectorySubdirs('other','')
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
        def res1 = service.listDirectoryResources(null,'xyz')
        assertNotNull(res1)
        assertEquals(2, res1.size())
        def found1=res1.find{it.path.path=='xyz/abc'}
        def found2=res1.find{it.path.path=='xyz/abc3'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
        assertNotNull(found2)
        assertEquals(false, found2.directory)
    }
    void testListDirectoryResources_ns_ok() {
        Storage.list().each{it.delete(flush: true)}
        def storage1 = new Storage(namespace: 'other',data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        assertEquals(5,Storage.list().size())
        Storage.list().each{
            assertEquals('other',it.namespace)
        }
        def res1 = service.listDirectoryResources(null,'xyz')
        assertNotNull(res1)
        assertEquals(0, res1.size())
        res1 = service.listDirectoryResources('other','xyz')
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
        def res1 = service.listDirectoryResources(null,'')
        assertNotNull(res1)
        assertEquals(1, res1.size())
        def found1=res1.find{it.path.path=='abc'}
        assertNotNull(found1)
        assertEquals(false,found1.directory)
    }

    void testListDirectoryResources_ns_root() {
        def storage1 = new Storage(namespace: 'other',data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(true)
        assertNotNull storage1
        assertNotNull new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(true)
        assertNotNull new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'def', dir: 'xyz/pyx',
                storageMeta: [abc: 'xyz3']).save(true)
        def res1 = service.listDirectoryResources(null,'')
        assertNotNull(res1)
        assertEquals(0, res1.size())
        res1 = service.listDirectoryResources('other','')
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
