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
//        minio.start()
        mClient = minio.client()
        String bucket = "direct-access-dir-bucket"
        if(!mClient.bucketExists(bucket)) mClient.makeBucket(bucket)
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
        subdirs[0].path.name == "config"
        subdirs[0].path.path == "server/config"
        subdirs[0].directory
        subdirs[1].path.name == "data"
        subdirs[1].path.path == "server/data"
        subdirs[1].directory
        subdirs[2].path.name == "logs"
        subdirs[2].path.path == "server/logs"
        subdirs[2].directory
    }

    def "ListSubDirectoriesAtRoot"() {
        when:
        def subdirs = directory.listSubDirectoriesAt("")
        then:
        subdirs.size() == 3
        subdirs[0].path.name == "etc"
        subdirs[0].directory
        subdirs[1].path.name == "server"
        subdirs[1].directory
        subdirs[2].path.name == "tobedeleted"
        subdirs[2].directory
    }

    def "ListResourceEntriesAt"() {
        when:
        def entries = directory.listResourceEntriesAt("server/config")
        then:
        entries.size() == 2
        entries[0].path.name == "realm.properties"
        entries[0].contents.meta.description == "security data"
        entries[1].path.name == "rundeck-config.properties"
        entries[1].contents.meta.description == "main config properties"
    }

    def "ListResourceEntries At Root"() {
        when:
        def entries = directory.listResourceEntriesAt("")
        then:
        entries.size() == 1
        entries[0].path.name == "rootfile.test"
    }
}
