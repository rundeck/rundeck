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

package org.rundeck.storage.data.file

import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.data.DataUtil
import spock.lang.Specification
import spock.lang.Unroll

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/18/14
 * Time: 11:27 AM
 */
class FileTreeSpecification extends Specification {
    File testDir
    def setup(){
        testDir=new File("build/filetree-tests")
        testDir.mkdirs()
    }
    def cleanup(){
        testDir.delete()
    }

    def "notfound resource causes exception"() {
        def dir = new File(testDir,"root1")
        def ft = FileTreeUtil.forRoot(dir,DataUtil.contentFactory())
        when:
        ft.getResource(PathUtil.asPath('doesnotexist'))
        then:
        thrown(StorageException)
    }

    @Unroll
    def "operation #operation invalid path"() {
        def dir = new File(testDir, "root1")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        when:
        ft."$operation"(PathUtil.asPath('some/../path'))
        then:
        StorageException e = thrown()
        e.message.contains('Invalid path')

        where:
        operation                | _
        'getPath'                | _
        'getResource'            | _
        'hasDirectory'           | _
        'hasPath'                | _
        'hasResource'            | _
        'listDirectory'          | _
        'listDirectoryResources' | _
        'listDirectorySubdirs'   | _
    }


    def "basic constructor creates dirs"() {
        def dir = new File(testDir,"root1")
        def ft = FileTreeUtil.forRoot(dir,DataUtil.contentFactory())
        expect:
        dir.exists()
        dir.isDirectory()
        new File(dir,"meta").exists()
        new File(dir,"meta").isDirectory()
        new File(dir,"content").exists()
        new File(dir,"content").isDirectory()
    }
    def "store basic resource"(){
        def dir = new File(testDir, "root1")
        def contentdir = new File(dir,"content")
        def metadir = new File(testDir, "root1/meta")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        def expectedDataFile = new File(contentdir, "test/a/bc/mydata.txt")
        def expectedMetaFile = new File(metadir, "test/a/bc/mydata.txt")
        expectedDataFile.deleteOnExit()
        expectedMetaFile.deleteOnExit()
        when:
        def resource=ft.createResource("test/a/bc/mydata.txt",
                DataUtil.withText("sample text",[monkey:'blister','Content-Type':'text/plain'],DataUtil.contentFactory()))
        then:
        resource.path.path=='test/a/bc/mydata.txt'
        resource.contents.meta.monkey=='blister'
        resource.contents.meta['Content-Type']=='text/plain'

        expectedDataFile.exists()
        expectedDataFile.isFile()
        expectedDataFile.text=='sample text'

        expectedMetaFile.exists()
        expectedMetaFile.isFile()
        expectedMetaFile.text.contains('"Content-Type":"text/plain"')
        expectedMetaFile.text.contains('"monkey":"blister"')
        expectedDataFile.delete()
        expectedMetaFile.delete()
    }

    @Unroll
    def "store via #operation invalid path"() {
        def dir = new File(testDir, "root1")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        when:
        ft."$operation"(
            PathUtil.asPath('some/../path'),
            DataUtil.withText(
                "sample text",
                [monkey: 'blister', 'Content-Type': 'text/plain'],
                DataUtil.contentFactory()
            )
        )
        then:
        StorageException e = thrown()
        e.message.contains('Invalid path')
        where:
        operation        | _
        'createResource' | _
        'updateResource' | _
    }
    def "store mixed metadata"(){
        def dir = new File(testDir, "root1")
        def contentdir = new File(dir,"content")
        def metadir = new File(testDir, "root1/meta")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        def expectedDataFile = new File(contentdir, "test/a/bc/mydata.txt")
        def expectedMetaFile = new File(metadir, "test/a/bc/mydata.txt")
        expectedDataFile.deleteOnExit()
        expectedMetaFile.deleteOnExit()
        when:
        def resource=ft.createResource("test/a/bc/mydata.txt",
                DataUtil.withText("sample text",[monkey:123,'Content-Type':'text/plain'],DataUtil.contentFactory()))
        then:
        resource.path.path=='test/a/bc/mydata.txt'
        resource.contents.meta.monkey=='123'
        resource.contents.meta['Content-Type']=='text/plain'

        expectedDataFile.exists()
        expectedDataFile.isFile()
        expectedDataFile.text=='sample text'

        expectedMetaFile.exists()
        expectedMetaFile.isFile()
        expectedMetaFile.text.contains('"Content-Type":"text/plain"')
        expectedMetaFile.text.contains('"monkey":"123"')
        expectedDataFile.delete()
        expectedMetaFile.delete()
    }

    def "Get resource does not exist"() {
        def dir = new File(testDir, "root1")
        def contentdir = new File(dir, "content")
        def metadir = new File(dir, "meta")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        def expectedDataFile = new File(contentdir, "test/a/bc/mydata.txt")
        def expectedMetaFile = new File(metadir, "test/a/bc/mydata.txt")
        expectedDataFile.deleteOnExit()
        expectedMetaFile.deleteOnExit()
        def resource = ft.createResource(
            "test/a/bc/mydata.txt",
            DataUtil.withText(
                "sample text",
                [monkey: 'blister', 'Content-Type': 'text/plain'],
                DataUtil.contentFactory()
            )
        )
        when:
        def result = ft.getResource(path)
        then:
        StorageException e = thrown()
        e.message.contains("Path does not exist: ${path}")
        expectedDataFile.delete()
        expectedMetaFile.delete()

        where:
        path                      | _
        'test/a/bc/wrongfile.txt' | _
    }

    def "Get resource is a dir"() {
        def dir = new File(testDir, "root1")
        def contentdir = new File(dir, "content")
        def metadir = new File(dir, "meta")
        def ft = FileTreeUtil.forRoot(dir, DataUtil.contentFactory())
        def expectedDataFile = new File(contentdir, "test/a/bc/mydata.txt")
        def expectedMetaFile = new File(metadir, "test/a/bc/mydata.txt")
        expectedDataFile.deleteOnExit()
        expectedMetaFile.deleteOnExit()
        def resource = ft.createResource(
            "test/a/bc/mydata.txt",
            DataUtil.withText(
                "sample text",
                [monkey: 'blister', 'Content-Type': 'text/plain'],
                DataUtil.contentFactory()
            )
        )
        when:
        def result = ft.getResource(path)
        then:
        StorageException e = thrown()
        e.message.contains("Failed to read resource at path: ${path}: is a directory")

        where:
        path        | _
        'test/a/bc' | _
    }
}
