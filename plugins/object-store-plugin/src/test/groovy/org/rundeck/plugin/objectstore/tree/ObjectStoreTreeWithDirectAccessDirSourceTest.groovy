/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.plugin.objectstore.tree

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.utils.Streams
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import org.rundeck.plugin.objectstore.directorysource.ObjectStoreDirectAccessDirectorySource
import org.rundeck.plugin.objectstore.directorysource.ObjectStoreDirectorySource
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.PathUtil
import spock.lang.Shared
import spock.lang.Specification
import testhelpers.MinioContainer
import testhelpers.MinioTestUtils

class ObjectStoreTreeWithDirectAccessDirSourceTest extends Specification {
    String configBucket = "test-config-bucket-das"
    ObjectStoreTree store
    ObjectStoreDirectorySource directorySource
    static MinioClient mClient

    @Shared
    public MinioContainer minio = new MinioContainer()

    void setupSpec() {
        minio.start()
        mClient = minio.client()
    }

    void setup() {
        //directorySource = new ObjectStoreMemoryDirectorySource(mClient, configBucket)
        directorySource = new ObjectStoreDirectAccessDirectorySource(mClient, configBucket)
        store = new ObjectStoreTree(mClient, configBucket, directorySource)
    }

    void cleanupSpec() {
        minio.stop()
    }

    def "Init"() {
        when:
        String testInitBucket = "test-init-config-bucket-das"
        new ObjectStoreTree(mClient, testInitBucket, directorySource)

        then:
        BucketExistsArgs args = BucketExistsArgs.builder()
                .bucket(testInitBucket)
                .build();
        mClient.bucketExists(args)
    }

    def "HasPath should fail for missing resource"() {
        expect:
        !store.hasPath("does/not/exist")
    }

    def "HasResource"() {
        setup:
        String key = "path1/myresource_has_resource_test.properties"
        ifNotExistAdd(key,"prop1=val1")

        expect:
        store.hasResource(key)
    }

    def "HasDirectory"() {
        setup:
        String key = "hasdirectory/isadir/file.txt"
        ifNotExistAdd(key,"prop1=val1")

        expect:
        store.hasDirectory("hasdirectory/isadir")
    }

    def "HasDirectory returns false for file resource"() {
        setup:
        String key = "hasdirectory/file.txt"
        ifNotExistAdd(key,"prop1=val1")

        expect:
        !store.hasDirectory("hasdirectory/file.txt")
    }


    def "GetPath"() {
        setup:
        String expected = "This is the resource"
        ifNotExistAdd("aresource",expected)

        when:
        def obj = store.getPath("aresource")

        then:
        obj.contents.inputStream.text == expected
        obj.contents.contentLength == expected.getBytes().length

    }

    def "GetResource"() {
        setup:
        String expected = "Resourceful"
        ifNotExistAdd("resource2",expected)

        when:
        def obj = store.getPath("resource2")

        then:
        obj.contents.inputStream.text == expected
        obj.contents.contentLength == expected.getBytes().length
    }

    def "ListDirectoryResources"() {
        setup:
        ifNotExistAdd("list-test/file1","content1")
        ifNotExistAdd("list-test/file2","content2")
        ifNotExistAdd("list-test/file3","content3")
        ifNotExistAdd("list-test/subdir/file4","content4")
        when:
        def items = store.listDirectoryResources(PathUtil.asPath("list-test"))
        then:
        items.size() == 3
        assert items.any { it.path.path == "list-test/file1" && it.path.name == "file1" }
    }

    def "ListDirectoryResources Root in subdir"() {
        setup:
        ifNotExistAdd("list-dr/sub1/sub2/file1","content1")
        ifNotExistAdd("list-dr/sub1/sub2/file2","content2")
        ifNotExistAdd("list-dr/sub1/sub2/file3","content3")
        ifNotExistAdd("list-dr/sub1/sub2/subdir/file4","content4")
        when:
        def items = store.listDirectoryResources(PathUtil.asPath("list-dr/sub1/sub2"))

        then:
        items.size() == 3
    }

    def "ListDirectory"() {
        setup:
        ifNotExistAdd("list-dir-test/file1","content1")
        ifNotExistAdd("list-dir-test/file2","content2")
        ifNotExistAdd("list-dir-test/file3","content3")
        ifNotExistAdd("list-dir-test/subdir/file4","content4")
        ifNotExistAdd("list-dir-test/subdir1/file","content5")
        ifNotExistAdd("list-dir-test/subdir1/file2","content52")
        ifNotExistAdd("list-dir-test/subdir2/subsub/fileZ","content6")
        when:
        def items = store.listDirectory("list-dir-test")
        then:
        items.size() == 6
        assert items.any { it.path.path == "list-dir-test/file1" && it.path.name == "file1" && !it.isDirectory() }
        assert items.any { it.path.name == "file2" && !it.isDirectory() }
        assert items.any { it.path.name == "file3" && !it.isDirectory() }
        assert items.any { it.path.path == "list-dir-test/subdir" && it.path.name == "subdir" && it.isDirectory() }
        assert items.any { it.path.name == "subdir1" && it.isDirectory() }
        assert items.any { it.path.name == "subdir2" && it.isDirectory() }

    }

    def "ListDirectorySubdirs"() {
        setup:
        ifNotExistAdd("list-subdir-test/file1","content1")
        ifNotExistAdd("list-subdir-test/file2","content2")
        ifNotExistAdd("list-subdir-test/file3","content3")
        ifNotExistAdd("list-subdir-test/subdir/file4","content4")
        ifNotExistAdd("list-subdir-test/subdir1/file","content5")
        ifNotExistAdd("list-subdir-test/subdir1/file2","content52")
        ifNotExistAdd("list-subdir-test/subdir2/subsub/file","content6")
        when:
        def items = store.listDirectorySubdirs("list-subdir-test")
        then:
        items.size() == 3
        assert items.any { it.path.path == "list-subdir-test/subdir" && it.path.name == "subdir" }
        assert items.any { it.path.path == "list-subdir-test/subdir1" && it.path.name == "subdir1" }
        assert items.any { it.path.name == "subdir2" }
    }

    def "DeleteResource"() {
        setup:
        ifNotExistAdd("delete-me","content1")
        when:
        store.deleteResource("delete-me")

        StatObjectArgs statArgs = StatObjectArgs.builder()
                .bucket(configBucket)
                .object("delete-me")
                .build()
        mClient.statObject(statArgs)
        then:
        thrown(ErrorResponseException)
    }

    def "CreateResource"() {
        when:
        String key = "framework.properties"
        String expected = "prop1=val1"
        !store.hasResource(key)
        store.createResource(key,createContent(expected))
        def actualResource = store.getResource(key)

        then:
        actualResource.contents.inputStream.text == expected
        actualResource.contents.meta[StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH]
        actualResource.contents.meta[StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME]
        actualResource.contents.meta[StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME]
    }

    def "UpdateResource"() {
        setup:
        ifNotExistAdd("update-test","initial content")
        when:
        store.updateResource("update-test",createContent("updated content"))
        def obj = store.getResource("update-test")
        then:
        obj.contents.inputStream.text == "updated content"
    }

    def "directoryFilter"() {
        expect:
        !("dir1/file" ==~ store.nestedSubDirCheck())
        "dir1/file/subdir" ==~ store.nestedSubDirCheck()
        "dir1/file/subdir/multiple/subdirs/file" ==~ store.nestedSubDirCheck()
    }


    private void ifNotExistAdd(String key, String content) {
        Map meta = [:]
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH] = content.bytes.length.toString()
        try {
            mClient.statObject(StatObjectArgs.builder().bucket(configBucket).object(key).build())
            directorySource.updateEntry(key, meta)
        } catch (ErrorResponseException erex) {
            if (erex.response().code() == 404) {
                ByteArrayInputStream inStream = new ByteArrayInputStream(content.bytes)
                PutObjectArgs putArgs = PutObjectArgs.builder()
                        .bucket(configBucket)
                        .object(key)
                        .stream(inStream, content.bytes.length, -1)
                        .headers(meta)
                        .build()
                mClient.putObject(putArgs)
                directorySource.updateEntry(key, meta)
            }
        }
    }

    private BaseStreamResource createContent(String content) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes())
        Map meta = [:]
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH] = content.bytes.length.toString()
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME] = new Date().toString()
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME] = new Date().toString()
        meta["my-random-custom-property"] = "random"
        return new BaseStreamResource(meta, new HasInputStream() {
            @Override
            InputStream getInputStream() throws IOException {
                return inputStream
            }

            @Override
            long writeContent(final OutputStream outputStream) throws IOException {
                return Streams.copyStream(inputStream, outputStream)
            }
        })
    }
}
