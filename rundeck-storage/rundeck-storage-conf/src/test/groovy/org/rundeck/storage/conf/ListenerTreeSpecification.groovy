package org.rundeck.storage.conf

import org.rundeck.storage.api.ContentMeta
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import org.rundeck.storage.data.MemoryTree
import spock.lang.Specification

import static org.rundeck.storage.data.DataUtil.dataWithText

/**
 * ListenerTreeSpecification is ...
 * @author greg
 * @since 2014-03-14
 */
class ListenerTreeSpecification extends Specification {
    static class testListener1 extends BaseListener {
        List<String> events = []

        @Override
        void didGetPath(Path path, Resource resource) {
            events << 'didGetPath:' + path + ":" + (null != resource)
        }

        @Override
        void didGetResource(Path path, Resource resource) {
            events << 'didGetResource:' + path + ":" + (null != resource)
        }

        @Override
        void didListDirectoryResources(Path path, Set<Resource> contents) {
            events << 'didListDirectoryResources:' + path + ":" + contents.size()
        }

        @Override
        void didListDirectory(Path path, Set<Resource> contents) {
            events << 'didListDirectory:' + path + ":" + contents.size()
        }

        @Override
        void didListDirectorySubdirs(Path path, Set<Resource> contents) {
            events << 'didListDirectorySubdirs:' + path + ":" + (null != contents ? contents.size() : 0)
        }

        @Override
        void didDeleteResource(Path path, boolean success) {
            events << 'didDeleteResource:' + path + ":" + success
        }

        void didCreateResource(Path path, ContentMeta content, Resource contents) {
            events << 'didCreateResource:' + path + ":" + (null != content)
        }

        void didUpdateResource(Path path, ContentMeta content, Resource contents) {
            events << 'didUpdateResource:' + path + ":" + (null != content)
        }
    }

    def "events are called"() {
        def listen = new testListener1()
        def mem1 = new MemoryTree()
        def ct = new ListenerTree(mem1, listen, null, null)
        when:
        ct.createResource('test1', dataWithText('write1', [testMeta: 'test1']))
        ct.getPath('test1')
        ct.getResource('test1')
        ct.updateResource('test1', dataWithText('write2', [testMeta: 'test1']))
        ct.deleteResource('test1')
        def iter = listen.events.iterator()
        then:
        iter.next() == 'didCreateResource:test1:true'
        iter.next() == 'didGetPath:test1:true'
        iter.next() == 'didGetResource:test1:true'
        iter.next() == 'didUpdateResource:test1:true'
        iter.next() == 'didDeleteResource:test1:true'
    }

    def "events are called2"() {
        def listen = new testListener1()
        def mem1 = new MemoryTree()
        def ct = new ListenerTree(mem1, listen, null, null)
        when:
        ct.createResource('test1/test2', dataWithText('write1', [testMeta: 'test1']))
        ct.getPath('test1')
        ct.listDirectory('test1')
        ct.listDirectoryResources('test1')
        ct.listDirectorySubdirs('test1')
        def iter = listen.events.iterator()
        then:
        iter.next() == 'didCreateResource:test1/test2:true'
        iter.next() == 'didGetPath:test1:true'
        iter.next() == 'didListDirectory:test1:1'
        iter.next() == 'didListDirectoryResources:test1:1'
        iter.next() == 'didListDirectorySubdirs:test1:0'
    }

}
