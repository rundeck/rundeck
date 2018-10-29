/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.storage.conf


import spock.lang.Specification
import org.rundeck.storage.data.MemoryTree
import spock.lang.Unroll

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

    def "cannot define two handlers with the same path"() {
        def sub1 = new SubPathTree(memory(), "/test1", true)
        def sub2 = new SubPathTree(memory(), "/test1", true)

        when:
            def tree1 = new TreeStack([sub1, sub2], memory())
        then:
            IllegalArgumentException e = thrown()
    }

    public MemoryTree memory() {
        new MemoryTree()
    }

    @Unroll
    def "longest handler subpath has precedence"() {
        given:

            def mem1 = memory()
            def sub1 = new SubPathTree(mem1, orderA, true)

            def mem2 = memory()
            def sub2 = new SubPathTree(mem2, orderB, true)
            def mem3 = memory()
            def tree1 = new TreeStack([sub1, sub2], mem3)

            tree1.createResource(expectA, dataWithText('a data'))
            tree1.createResource(expectB, dataWithText('b data'))
        expect:
            mem1.hasResource(expectA)
            !mem2.hasResource(expectA)
            !mem1.hasResource(expectB)
            mem2.hasResource(expectB)

        where:
            orderA          | orderB          | expectA                       | expectB
            '/test1/monkey' | '/test1'        | "/test1/monkey/applesauce"    | '/test1/monkey2/balogna/flea'
            '/test1'        | '/test1/monkey' | '/test1/monkey2/balogna/flea' | "/test1/monkey/applesauce"
    }


    def "create file in subtree root fails"() {
        def sub1 = new SubPathTree(new MemoryTree(), "/test1/monkey", false)
        def tree1 = new TreeStack([sub1], new MemoryTree())

        when:
            def result = tree1.createResource("/test1/monkey", dataWithText('monkey data'))

        then:
            IllegalArgumentException e = thrown()
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

    def "subtree ordering doesn't matter"() {
        def mem1 = new MemoryTree()
        def sub1 = new SubPathTree(mem1, "/test1/monkey", true)
        def mem2 = new MemoryTree()
        def sub2 = new SubPathTree(mem2, "/test1", true)
        def mem3 = new MemoryTree()
        def tree1 = new TreeStack([sub2, sub1], mem3)

        tree1.createResource("/test1/monkey/ringo", dataWithText('monkey data'))
        tree1.createResource("/test1/monkey2/balogna/flea", dataWithText('monkey data'))
        expect:
            mem1.hasResource("/test1/monkey/ringo")
            !mem2.hasResource("/test1/monkey/ringo")
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


    def "multiple subpaths listed as subdirs"() {
        given:
            def sub1 = new SubPathTree(new MemoryTree(), "/test1/monkey", true)
            def sub2 = new SubPathTree(new MemoryTree(), "/test1", true)
            def sub3 = new SubPathTree(new MemoryTree(), "/test2/extra/zingo/zango", true)
            def tree1 = new TreeStack([sub1, sub2, sub3], new MemoryTree())

            def listing = tree1.listDirectory("/")
            def listingx1 = tree1.listDirectorySubdirs("/")
        expect:
            listing.size() == 2
            listing.every { it.directory }
            listing.collect { it.path.path } contains 'test1'
            listing.collect { it.path.path } contains 'test2'

            listingx1.size() == 2
            listingx1.every { it.directory }
            listingx1.collect { it.path.path } contains 'test1'
            listingx1.collect { it.path.path } contains 'test2'
    }

    def "intermediate subpaths listed as subdirs"() {
        given:
            def sub1 = new SubPathTree(new MemoryTree(), "/test1/monkey", true)
            def sub2 = new SubPathTree(new MemoryTree(), "/test1", true)
            def sub3 = new SubPathTree(new MemoryTree(), "/test2/extra/zingo/zango", true)
            def tree1 = new TreeStack([sub1, sub2, sub3], new MemoryTree())

            def listing = tree1.listDirectory(test)
            def listingx1 = tree1.listDirectorySubdirs(test)
        expect:
            listing.size() == 1
            listing.every { it.directory }
            listing.collect { it.path.path } contains expected

            listingx1.size() == 1
            listingx1.every { it.directory }
            listingx1.collect { it.path.path } contains expected

        where:
            test           | expected
            '/test2'       | 'test2/extra'
            '/test2/extra' | 'test2/extra/zingo'
    }
    def "shadow dir in path is ignored"() {
        given:
            def sub1 = new SubPathTree(new MemoryTree(), "/test1/monkey", true)
            def sub2 = new SubPathTree(new MemoryTree(), "/test1", true)
            def tree1 = new TreeStack([sub1, sub2], new MemoryTree())
            sub2.createResource('/test1/monkey/roomba', dataWithText('test data'))
            def listing = tree1.listDirectory('/test1')
        expect:
            listing.size() == 1
            listing.first().path.name == 'monkey'
    }

    def "shadow resource in stack is ignored"() {
        given:
            def sub1 = new SubPathTree(new MemoryTree(), "/test1/monkey", true)
            def sub2 = new SubPathTree(new MemoryTree(), "/test1", true)
            def tree1 = new TreeStack([sub1, sub2], new MemoryTree())
            sub2.createResource('/test1/monkey', dataWithText('test data'))

        expect:
            !tree1.hasResource('/test1/monkey')
            tree1.hasDirectory('/test1/monkey')

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
