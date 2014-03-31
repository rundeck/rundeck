package org.rundeck.storage.conf

import org.rundeck.storage.data.DataUtil
import spock.lang.Specification
import org.rundeck.storage.data.MemoryTree

import static org.rundeck.storage.api.PathUtil.asPath
import static org.rundeck.storage.api.PathUtil.asPath
import static org.rundeck.storage.api.PathUtil.asPath
import static org.rundeck.storage.api.PathUtil.asPath
import static org.rundeck.storage.api.PathUtil.asPath
import static org.rundeck.storage.data.DataUtil.dataWithText


/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/4/14
 * Time: 5:02 PM
 */
class TreeStackSpecification extends Specification {
    def "path check"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem, "test/path", true)
        expect:
        TreeStack.matchesPath(asPath("test/path"),sub)
        TreeStack.matchesPath(asPath("test/path/asdf"), sub)
        TreeStack.matchesPath(asPath("test/path/asdf/dxx"), sub)
        !TreeStack.matchesPath(asPath("test/asdf"), sub)
        !TreeStack.matchesPath(asPath("something/else"), sub)
    }
    def "parent path check"() {
        given:
        def mem = new MemoryTree()
        def sub = new SubPathTree(mem, "test/path", true)
        expect:
        TreeStack.hasParentPath(asPath("test"),sub)
        !TreeStack.hasParentPath(asPath("test/path"), sub)
        !TreeStack.hasParentPath(asPath("test/path/asdf"), sub)
        !TreeStack.hasParentPath(asPath("test/path/asdf/dxx"), sub)
        !TreeStack.hasParentPath(asPath("test/asdf"), sub)
        !TreeStack.hasParentPath(asPath("something/else"), sub)
    }
    def "tree handler handles sub tree resources"(){
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub1], mem3)
        when:
        def res1=tree1.createResource("/test1/monkey", dataWithText('monkey data'))
        def res2 =tree1.createResource("/test1/monkey2/balogna/flea", dataWithText('monkey data'))
        def res3 =tree1.createResource("/test2/monkey", dataWithText('monkey2 data'))
        then:
        null!=res1
        res1.path.path=='test1/monkey'
        res2.path.path=='test1/monkey2/balogna/flea'
        res3.path.path=='test2/monkey'
        mem1.hasResource("/test1/monkey")
        mem1.hasResource("/test1/monkey2/balogna/flea")
        !mem1.hasResource("/test2/monkey")

        !mem3.hasResource("/test1/monkey2/balogna/flea")
        !mem3.hasResource("/test1/monkey")
        mem3.hasResource("/test2/monkey")
    }
    def "if multiple treehandlers match, first one is used"(){
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1", true)
        def mem2 = new MemoryTree()
        def sub2 = new SubPathTree(mem2, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub1, sub2], mem3)

        tree1.createResource("/test1/monkey", dataWithText('monkey data'))
        tree1.createResource("/test1/monkey2/balogna/flea", dataWithText('monkey data'))
        expect:
        mem1.hasResource("/test1/monkey")
        !mem2.hasResource("/test1/monkey")
        mem1.hasResource("/test1/monkey2/balogna/flea")
        !mem2.hasResource("/test1/monkey2/balogna/flea")
    }
    def "more specific subtree match"(){
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1/monkey", true)
        def mem2 = new MemoryTree()
        def sub2 = new SubPathTree(mem2, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub1, sub2], mem3)

        tree1.createResource("/test1/monkey", dataWithText('monkey data'))
        tree1.createResource("/test1/monkey2/balogna/flea", dataWithText('monkey data'))
        expect:
        mem1.hasResource("/test1/monkey")
        !mem2.hasResource("/test1/monkey")
        !mem1.hasResource("/test1/monkey2/balogna/flea")
        mem2.hasResource("/test1/monkey2/balogna/flea")
    }
    def "subtrees are listed as dirs in the parent"(){
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1/monkey", true)
        def mem2 = new MemoryTree()
        def sub2 = new SubPathTree(mem2, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub1, sub2], mem3)

        def listing=tree1.listDirectory("/")
        def listingx1=tree1.listDirectorySubdirs("/")
        def listing2=tree1.listDirectory("/test1")
        def listingx2=tree1.listDirectorySubdirs("/test1")
        expect:
        listing.size()==1
        listing.first().directory
        listing.first().path.path=='test1'

        listingx1.size()==1
        listingx1.first().directory
        listingx1.first().path.path=='test1'

        listing2.size() == 1
        listing2.first().directory
        listing2.first().path.path == 'test1/monkey'

        listingx2.size() == 1
        listingx2.first().directory
        listingx2.first().path.path == 'test1/monkey'
    }
    def "subtrees exist as dirs in the parent"(){
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1/monkey", true)
        def mem2 = new MemoryTree()
        def sub2 = new SubPathTree(mem2, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub1, sub2], mem3)

        expect:
        tree1.hasDirectory('test1')
        tree1.hasDirectory('test1/monkey')
    }
}
