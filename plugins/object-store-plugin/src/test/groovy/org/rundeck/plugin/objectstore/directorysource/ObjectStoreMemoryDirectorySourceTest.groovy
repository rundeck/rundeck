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
package org.rundeck.plugin.objectstore.directorysource

import io.minio.MakeBucketArgs
import io.minio.MinioClient
import spock.lang.Shared
import spock.lang.Specification
import testhelpers.MinioContainer
import testhelpers.MinioTestUtils

class ObjectStoreMemoryDirectorySourceTest extends Specification {

    static MinioClient mClient
    ObjectStoreMemoryDirectorySource directory

    @Shared
    public MinioContainer minio = new MinioContainer()

    def setupSpec() {
        minio.start()
        mClient = minio.client()
    }

    void cleanupSpec() {
        minio.stop()
    }

    def setup() {
        directory = new ObjectStoreMemoryDirectorySource(mClient, "memory-dir-bucket")
        directory.updateEntry("rootfile.test",[:])
        directory.updateEntry("etc/framework.properties",["description":"rundeck framework property file"])
        directory.updateEntry("server/config/rundeck-config.properties",["description":"main config properties"])
        directory.updateEntry("server/config/realm.properties",["description":"security data"])
        directory.updateEntry("server/logs/server.log",[:])
        directory.updateEntry("server/data/grailsdb.mv.db",[:])
        directory.updateEntry("server/random.file",[:])
        directory.updateEntry("tobedeleted/delete.me",[:])
    }

    def "Init no bucket"() {
        when:
        ObjectStoreMemoryDirectorySource sync = new ObjectStoreMemoryDirectorySource(mClient, "non-existant")

        then:
        sync.root.isEmpty()
    }

    def "Init resync"() {
        when:
        String bucket = "mem-sync"
        MakeBucketArgs args = MakeBucketArgs.builder()
                .bucket(bucket)
                .build();
        mClient.makeBucket(args)
        MinioTestUtils.ifNotExistAdd(mClient, bucket, "arootfile.file", "root", [:])
        MinioTestUtils.ifNotExistAdd(mClient, bucket, "dir/dir.file", "file", [:])
        MinioTestUtils.ifNotExistAdd(mClient, bucket, "dir/subdir/file1.file", "file", [name:"file1"])
        MinioTestUtils.ifNotExistAdd(mClient, bucket, "dir/subdir/file2.file", "file", [name:"file2"])
        ObjectStoreMemoryDirectorySource sync = new ObjectStoreMemoryDirectorySource(mClient, bucket)

        then:
        !sync.root.isEmpty()
        sync.checkResourceExists("arootfile.file")
        sync.checkResourceExists("dir/dir.file")
        sync.root.getSubdir("dir").getSubdir("subdir").getEntry("file1.file").meta.name == "file1"
        sync.root.getSubdir("dir").getSubdir("subdir").getEntry("file2.file").meta.name == "file2"
    }

    def "CheckPathExists"() {
        expect:
        directory.checkPathExists("etc/framework.properties")
        directory.checkPathExists("server/config")
        !directory.checkPathExists("this/does/not/exist")
        !directory.checkPathExists("server/config/nonexistantfile")
    }

    def "CheckResourceExists"() {
        expect:
        directory.checkResourceExists("etc/framework.properties")
        !directory.checkResourceExists("server/config")
    }

    def "CheckPathExistsAndIsDirectory"() {
        expect:
        directory.checkPathExistsAndIsDirectory("server/config")
        !directory.checkPathExistsAndIsDirectory("etc/framework.properties")
    }

    def "GetEntryMetadata"() {
        expect:
        directory.getEntryMetadata("etc/framework.properties").description == "rundeck framework property file"
    }

    def "ListSubDirectoriesAt"() {
        when:
        def subdirs = directory.listSubDirectoriesAt("server")
        then:
        subdirs.size() == 3
        assert subdirs.any { it.path.name == "config" && it.path.path == "server/config" && it.directory }
        assert subdirs.any { it.path.name == "data" && it.path.path == "server/data" && it.directory }
        assert subdirs.any { it.path.name == "logs" && it.path.path == "server/logs" && it.directory }
    }

    def "ListSubDirectoriesAtRoot"() {
        when:
        def subdirs = directory.listSubDirectoriesAt("")
        then:
        subdirs.size() == 3
        assert subdirs.any { it.path.name == "etc" && it.directory }
        assert subdirs.any { it.path.name == "server" && it.directory }
        assert subdirs.any { it.path.name == "tobedeleted" && it.directory }
    }

    def "ListResourceEntriesAt"() {
        when:
        def entries = directory.listResourceEntriesAt("server/config")
        then:
        entries.size() == 2
        assert entries.any { it.path.name == "realm.properties" && it.contents.meta.description == "security data" }
        assert entries.any { it.path.name == "rundeck-config.properties" && it.contents.meta.description == "main config properties" }
    }

    def "ListResourceEntries At Root"() {
        when:
        def entries = directory.listResourceEntriesAt("")
        then:
        entries.size() == 1
        entries[0].path.name == "rootfile.test"
    }

    def "UpdateEntry"() {
        expect:
        directory.root.listEntryNames().contains("rootfile.test")
        directory.root.getSubdir("etc").listEntryNames().contains("framework.properties")
    }

    def "DeleteEntry"() {
        when:
        directory.deleteEntry("tobedeleted/delete.me")
        then:
        !directory.checkResourceExists("tobedeleted/delete.me")
        !directory.checkPathExistsAndIsDirectory("tobedeleted")
    }

}
