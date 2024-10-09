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

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import spock.lang.Shared
import spock.lang.Specification
import testhelpers.MinioContainer
import testhelpers.MinioTestUtils

class ObjectStoreDirectAccessDirectorySourceTest extends Specification {
    static MinioClient mClient

    @Shared
    public MinioContainer minio = new MinioContainer()


    @Shared
    ObjectStoreDirectAccessDirectorySource directory

    def setupSpec() {
        minio.start()
        mClient = minio.client()

        String bucket = "direct-access-dir-bucket"
        if (!mClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            mClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
        directory = new ObjectStoreDirectAccessDirectorySource(mClient, bucket)

        MinioTestUtils.ifNotExistAdd(mClient,bucket,"rootfile.test","data", [:])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"etc/framework.properties","data",["description":"rundeck framework property file"])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"server/config/rundeck-config.properties","data",["description":"main config properties"])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"server/config/realm.properties","data",["description":"security data"])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"server/logs/server.log","data",[:])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"server/data/grailsdb.mv.db","data",[:])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"server/random.file","data",[:])
        MinioTestUtils.ifNotExistAdd(mClient,bucket,"tobedeleted/delete.me","data",[:])
    }

    void cleanupSpec() {
        minio.stop()
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
}
