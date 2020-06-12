package org.rundeck.storage.conf

import org.rundeck.storage.api.Resource
import org.rundeck.storage.data.MemoryTree
import spock.lang.Specification

import static org.rundeck.storage.data.DataUtil.dataWithText

class PrefixPathTreeSpec extends Specification {

    def "createResource then path prefix is added in underlying storage"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res=sub.createResource("abc/def", dataWithText('test data'))
            def res2=mem.createResource("test/path/abc/def2", dataWithText('test data2'))
        then:
            res.path.path=='abc/def'
            res2.path.path == 'test/path/abc/def2'
            mem.hasResource("test/path/abc/def")
            !mem.hasResource("abc/def")
            sub.hasResource("abc/def")

            mem.hasResource("test/path/abc/def2")
            !mem.hasResource("abc/def2")
            sub.hasResource("abc/def2")
    }

    def "hasDirectory then path prefix is removed"() {
        given:
            def mem = new MemoryTree()
            def sup = new PrefixPathTree(mem, "test/path")
        when:
            def res = sup.createResource("abc/def", dataWithText( 'test data'))
            def res2 = mem.createResource("abc2/def", dataWithText( 'test data2'))
            def res3 = mem.createResource("test/path/abc3/def", dataWithText('test data2'))
        then:

            sup.hasDirectory("abc")
            !sup.hasDirectory("abc2")
            sup.hasDirectory("abc3")
            !sup.hasDirectory("test/path")

            !mem.hasDirectory("abc")
            mem.hasDirectory("test/path/abc")
            mem.hasDirectory("abc2")
            !mem.hasDirectory("abc3")
            mem.hasDirectory("test/path")
    }

    def "getResource then path prefix is added"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.createResource("abc2/def", dataWithText('test data2'))
            def res3 = mem.createResource("test/path/abc3/def", dataWithText( 'test data3'))
        then:
            null!=sub.getResource("abc2/def")
            null!=sub.getResource("abc3/def")
    }
    def "hasPath root and backing tree is empty"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.hasPath("/")
        then:
            res2
    }
    def "hasPath root and backing tree has resource"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.hasPath("/")
        then:
            res2
    }
    def "hasResource root is false and backing tree is empty"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.hasResource("/")
        then:
            !res2
    }
    def "hasResource root is false and backing tree has resource"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.hasResource("/")
        then:
            !res2
    }
    def "hasDirectory root is true and backing tree is empty"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.hasDirectory("/")
        then:
            res2
    }
    def "hasDirectory root is true and backing tree has resource"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.hasDirectory("/")
        then:
            res2
    }
    def "getResource root and backing tree is empty is illegal argument"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.getResource("/")
        then:
            IllegalArgumentException e = thrown()
            e!=null
    }
    def "getResource root and backing tree has resource is illegal argument"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.getResource("test/path")
        then:
            IllegalArgumentException e = thrown()
            e!=null
    }
    def "getPath root and backing tree is empty is a directory"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.getPath("/")
        then:
            res2!=null
            res2.isDirectory()
    }
    def "getPath root and backing tree has resource is a directory"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.getPath("/")
        then:
            res2!=null
            res2.isDirectory()
    }
    def "listDirectory root and backing tree is empty is empty list"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.listDirectory("/")
        then:
            res2!=null
            res2.size()==0
    }
    def "listDirectory root and backing tree has resource is empty list"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def memres = mem.createResource("test/path", dataWithText( 'test data2'))
        when:
            def res2 = sub.listDirectory("/")
        then:
            res2!=null
            res2.size()==0
    }

    def "listResourceDirectory then path prefix is removed"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
        when:
            def res2 = sub.createResource("abc2/def", dataWithText('test data2'))
            def res3 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
            def res4 = mem.createResource("abc2/def3", dataWithText( 'test data3'))
            Set<Resource> list = sub.listDirectoryResources("abc2")
            Set<String> names = list.path*.name
            Set<String> paths = list.path*.path
        then:
            list.size() == 2
            'def' in names
            'def2' in names
            'abc2/def' in paths
            'abc2/def2' in paths
    }

    def "deleteResource then path prefix is removed"() {
        given:
            def mem = new MemoryTree()
            def sub = new PrefixPathTree(mem, "test/path")
            def res2 = sub.createResource("abc2/def", dataWithText('test data2'))
            def res4 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
            def res5 = mem.createResource("abc2/def3", dataWithText('test data3'))
        when:
            boolean deleted = sub.deleteResource("abc2/def")
            boolean deleted2 = sub.deleteResource("abc2/def2")
            boolean deleted3 = sub.deleteResource("abc2/def3")
        then:
            deleted
            deleted2
            !deleted3
            !sub.hasPath("abc2/def")
            !sub.hasPath("abc2/def2")
            !sub.hasPath("abc2/def3")
            !mem.hasPath("test/path/abc2/def")
            !mem.hasPath("abc2/def")
            !mem.hasPath("test/path/abc2/def2")
            !mem.hasPath("abc2/def2")
            !mem.hasPath("test/path/abc2/def3")
            mem.hasPath("abc2/def3")
    }
}
