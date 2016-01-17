package org.rundeck.storage.conf

import org.rundeck.storage.api.ContentMeta
import org.rundeck.storage.api.Path
import org.rundeck.storage.data.MemoryTree
import spock.lang.Specification

import static org.rundeck.storage.data.DataUtil.dataWithText

/**
 * ConverterTreeSpecification is ...
 * @author greg
 * @since 2014-02-21
 */
class ConverterTreeSpecification extends Specification {
    static class testConvert1 implements ContentConverter {
        boolean read=false
        boolean created=false
        boolean updated=false
        ContentMeta convertReadData(Path path, ContentMeta contents) {
            read=true
            dataWithText('read data',[testMeta:'filterReadData'])
        }

        ContentMeta convertCreateData(Path path, ContentMeta contents) {
            created=true
            dataWithText('create data', [testMeta: 'filterCreateData'])
        }

        ContentMeta convertUpdateData(Path path, ContentMeta content) {
            updated=true
            dataWithText('update data', [testMeta: 'filterUpdateData'])
        }
    }
    def "create resource calls filterCreateData"(){
        def conv1 = new testConvert1()
        def mem1 = new MemoryTree()
        def ct = new ConverterTree(mem1,conv1,null,null)
        when:
        def res1=ct.createResource('test1',dataWithText('write1',[testMeta: 'test1']))
        def res2=mem1.getResource('test1')
        then:
        conv1.created
        !conv1.updated
        !conv1.read
        !res1.directory
        res1.contents.meta.testMeta=='filterReadData'
        res2.contents.meta.testMeta=='filterCreateData'
    }

    def "update resource calls filterUpdateData"() {
        def conv1 = new testConvert1()
        def mem1 = new MemoryTree()
        def ct = new ConverterTree(mem1, conv1, null, null)
        when:
        def res1 = mem1.createResource('test1', dataWithText('write1', [testMeta: 'test1']))
        def res2 = ct.updateResource('test1', dataWithText('write2', [testMeta: 'test2']))
        def res3 = mem1.getResource('test1')
        then:
        !conv1.created
        conv1.updated
        !conv1.read
        !res2.directory
        res2.contents.meta.testMeta == 'filterReadData'
        res3.contents.meta.testMeta == 'filterUpdateData'
    }
    def "read resource calls filterReadData"() {
        def conv1 = new testConvert1()
        def mem1 = new MemoryTree()
        def ct = new ConverterTree(mem1, conv1, null, null)
        mem1.createResource('test1', dataWithText('write1', [testMeta: 'test1']))
        when:
        def res2 = ct.getResource('test1')
        //access contents to invoke read
        def contents = res2.contents
        then:
        !conv1.created
        !conv1.updated
        conv1.read
        !res2.directory
        contents.meta.testMeta == 'filterReadData'
    }
    def "read directory doesn't call filterReadData"() {
        def conv1 = new testConvert1()
        def mem1 = new MemoryTree()
        def ct = new ConverterTree(mem1, conv1, null, null)
        mem1.createResource('test1/blah', dataWithText('write1', [testMeta: 'test1']))
        when:
        def res2 = ct.listDirectory('test1')
        then:
        !conv1.created
        !conv1.updated
        !conv1.read
    }
}
