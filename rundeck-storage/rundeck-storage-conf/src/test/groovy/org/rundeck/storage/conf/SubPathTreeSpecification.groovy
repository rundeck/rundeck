package org.rundeck.storage.conf

import spock.lang.Specification
import org.rundeck.storage.data.MemoryTree
import org.rundeck.storage.api.Resource

import static org.rundeck.storage.data.DataUtil.dataWithText
import static org.rundeck.storage.api.PathUtil.asPath

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/4/14
 * Time: 5:03 PM
 */
class SubPathTreeSpecification extends Specification {

    def "path check"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        expect:
        sub.matchesPath(asPath("test/path"))
        sub.matchesPath(asPath("test/path/asdf"))
        sub.matchesPath(asPath("test/path/asdf/dxx"))
        !sub.matchesPath(asPath("test/asdf"))
        !sub.matchesPath(asPath("something/else"))
    }
    def "createResource fullPath=true then path is the same in underlying storage"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        when:
        def res = sub.createResource("test/path/abc/def", dataWithText('data'))
        def res2 = mem.createResource("test/path/abc/def2", dataWithText('data2'))
        then:
        res.path.path=='test/path/abc/def'
        res==mem.getResource("test/path/abc/def")
        res2.path.path=='test/path/abc/def2'
        res2==sub.getResource("test/path/abc/def2")
    }
    def "createResource fullPath=false then path prefix is removed in underlying storage"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",false)
        when:
        def res=sub.createResource("test/path/abc/def", dataWithText('test data'))
        def res2=mem.createResource("abc/def2", dataWithText('test data2'))
        then:
        res.path.path=='test/path/abc/def'
        res2.path.path == 'abc/def2'
        mem.hasResource("abc/def")
        !mem.hasResource("test/path/abc/def")
        sub.hasResource("test/path/abc/def")

        mem.hasResource("abc/def2")
        !mem.hasResource("test/path/abc/def2")
        sub.hasResource("test/path/abc/def2")
    }
    def "hasDirectory fullPath=true then path prefix is the same"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        when:
        def res=sub.createResource("test/path/abc/def", dataWithText('test data'))
        def res2=mem.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res3=mem.createResource("abc3/def", dataWithText('test data2'))
        then:
        sub.hasDirectory("test/path/abc")
        sub.hasDirectory("test/path/abc2")
        !sub.hasDirectory("test/path/abc3")
        sub.hasDirectory("test/path")
        mem.hasDirectory("test/path/abc")
        mem.hasDirectory("test/path/abc2")
        !mem.hasDirectory("test/path/abc3")
        mem.hasDirectory("test/path")
    }
    def "hasDirectory fullPath=false then path prefix is removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",false)
        when:
        def res = sub.createResource("test/path/abc/def", dataWithText( 'test data'))
        def res2 = mem.createResource("test/path/abc2/def", dataWithText( 'test data2'))
        def res3 = mem.createResource("abc3/def", dataWithText('test data2'))
        then:
        sub.hasDirectory("test/path/abc")
        !sub.hasDirectory("test/path/abc2")
        sub.hasDirectory("test/path/abc3")
        sub.hasDirectory("test/path")

        !mem.hasDirectory("test/path/abc")
        mem.hasDirectory("abc")
        mem.hasDirectory("test/path/abc2")
        !mem.hasDirectory("test/path/abc3")
        mem.hasDirectory("test/path")
    }
    def "getResource fullPath=true then path prefix is not removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        when:
        def res2 = mem.createResource("test/path/abc2/def", dataWithText( 'test data2'))
        def res3 = mem.createResource("abc3/def", dataWithText('test data3'))
        then:
        null!=sub.getResource("test/path/abc2/def")
        !sub.hasResource("test/path/abc3/def")
    }
    def "getResource fullPath=false then path prefix is removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",false)
        when:
        def res2 = sub.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res3 = mem.createResource("abc3/def", dataWithText( 'test data3'))
        then:
        null!=sub.getResource("test/path/abc2/def")
        null!=sub.getResource("test/path/abc3/def")
    }
    def "listResourceDirectory fullPath=true then path prefix is not removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        def res2 = sub.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res3 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
        def res4 = mem.createResource("abc2/def3", dataWithText('test data3'))
        when:
        Set<Resource> list=sub.listDirectoryResources("test/path/abc2")
        Set<String> names=list.path*.name
        Set<String> paths=list.path*.path
        then:
        list.size()==2
        'def' in names
        'def2' in names
        'test/path/abc2/def' in paths
        'test/path/abc2/def2' in paths
    }
    def "listResourceDirectory fullPath=false then path prefix is removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",false)
        when:
        def res2 = sub.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res3 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
        def res4 = mem.createResource("abc2/def3", dataWithText( 'test data3'))
        Set<Resource> list = sub.listDirectoryResources("test/path/abc2")
        Set<String> names = list.path*.name
        Set<String> paths = list.path*.path
        then:
        list.size() == 2
        'def' in names
        'def3' in names
        'test/path/abc2/def' in paths
        'test/path/abc2/def3' in paths
    }
    def "deleteResource fullPath=true then path prefix is retained"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem,"test/path",true)
        def res2 = sub.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res4 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
        def res5 = mem.createResource("abc2/def3", dataWithText('test data3'))
        when:
        boolean deleted=sub.deleteResource("test/path/abc2/def")
        boolean deleted2=sub.deleteResource("test/path/abc2/def2")
        boolean deleted3=sub.deleteResource("test/path/abc2/def3")
        then:
        deleted
        deleted2
        !deleted3
        !sub.hasPath("test/path/abc2/def")
        !sub.hasPath("test/path/abc2/def2")
        !sub.hasPath("test/path/abc2/def3")
        !mem.hasPath("test/path/abc2/def")
        !mem.hasPath("abc2/def")
        !mem.hasPath("test/path/abc2/def2")
        !mem.hasPath("abc2/def2")
        !mem.hasPath("test/path/abc2/def3")
        mem.hasPath("abc2/def3")
    }
    def "deleteResource fullPath=false then path prefix is removed"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem, "test/path", false)
        def res2 = sub.createResource("test/path/abc2/def", dataWithText('test data2'))
        def res4 = mem.createResource("test/path/abc2/def2", dataWithText('test data3'))
        def res5 = mem.createResource("abc2/def3", dataWithText('test data3'))
        when:
        boolean deleted = sub.deleteResource("test/path/abc2/def")
        boolean deleted2 = sub.deleteResource("test/path/abc2/def2")
        boolean deleted3 = sub.deleteResource("test/path/abc2/def3")
        then:
        deleted
        !deleted2
        deleted3
        !sub.hasPath("test/path/abc2/def")
        !sub.hasPath("test/path/abc2/def2")
        !sub.hasPath("test/path/abc2/def3")
        !mem.hasPath("test/path/abc2/def")
        !mem.hasPath("abc2/def")
        mem.hasPath("test/path/abc2/def2")
        !mem.hasPath("abc2/def2")
        !mem.hasPath("test/path/abc2/def3")
        !mem.hasPath("abc2/def3")
    }
}
