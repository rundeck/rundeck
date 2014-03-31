package rundeck



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Storage)
class StorageTests {

    void testGetPath() {
        assertEquals('abc',new Storage(dir:'',name: 'abc').path)
        assertEquals('xyz/abc',new Storage(dir:'xyz',name: 'abc').path)
        assertEquals('xyz/elf/abc',new Storage(dir:'xyz/elf',name: 'abc').path)
    }
    void testsetPath() {
        def s=new Storage()
        s.path='abc'
        assertEquals('abc',s.name)
        assertEquals('',s.dir)

        s.path='abc/xyz/monkey'
        assertEquals('monkey',s.name)
        assertEquals('abc/xyz',s.dir)
    }
}
