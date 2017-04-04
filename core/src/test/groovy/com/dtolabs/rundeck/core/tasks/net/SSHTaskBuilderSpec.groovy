/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.tasks.net

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.utils.FileUtils
import org.apache.tools.ant.Project
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author greg
 * @since 4/3/17
 */
class SSHTaskBuilderSpec extends Specification {
    Path testDir
    Path projectsDir
    Path copyDir
    Path destDir

    def setup() {
        testDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test")
        projectsDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-projects")
        copyDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test-files")
        destDir = Files.createTempDirectory("CopyFileNodeStepPluginSpec-test-dest")

    }

    def cleanup() {
        FileUtils.deleteDir(testDir.toFile())
        FileUtils.deleteDir(projectsDir.toFile())
        FileUtils.deleteDir(copyDir.toFile())
        FileUtils.deleteDir(destDir.toFile())
    }

    Map<String, File> makeDirFiles(Path dir, List<String> paths = []) {
        int i = 0
        def files = [:]

        for (String path : paths) {
            File file1 = new File(dir.toFile(), path)
            file1.getParentFile().mkdirs()
            file1.text = "$i test file"
            files[path] = file1
            i++
        }
        files
    }

    def "build multi scp just files"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def project = new Project()
        def basedir = copyDir.toFile()
        def filesMap = makeDirFiles(copyDir, [
                'test1.txt',
                'file2.txt',
                'sub1/test3.xml',
                'sub1/sub2/test4.blah'
        ]
        )

        def files = copyfiles.collect { filesMap[it] }
        def remotePath = "monkey/test"
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'bob'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyStoragePath() >> 'keys/fake/path'
            getPrivateKeyStorageData() >> {
                new ByteArrayInputStream('data'.bytes)
            }
        }
        def listener = Mock(ExecutionListener)

        when:
        def result = SSHTaskBuilder.buildMultiScp(
                node,
                project,
                basedir,
                files,
                remotePath,
                nodeAuthentication,
                0,
                listener
        );
        then:
        result != null
        result instanceof ExtScp
        ExtScp built = (ExtScp) result
        built.getIfaceToDir() == 'bob@ahostname:monkey/test'
        built.getIfaceFileSets() != null
        built.getIfaceFileSets().size() == 1
        built.getIfaceFileSets()[0].getDir() == basedir
        built.getIfaceFileSets()[0].size() == files.size()
        def resfiles = built.getIfaceFileSets().collect { it.iterator().collect { it.file } }.flatten() as Set
        resfiles == files as Set

        where:
        copyfiles                                                            | _
        ['test1.txt']                                                        | _
        ['test1.txt', 'file2.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah'] | _

    }

    def "build multi scp with dirs"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def project = new Project()
        def basedir = copyDir.toFile()
        def filesMap = makeDirFiles(copyDir, [
                'test1.txt',
                'file2.txt',
                'sub1/test3.xml',
                'sub1/sub2/test4.blah'
        ]
        )

        def files = [dirname ? new File(basedir, dirname) : basedir]

        def remotePath = "monkey/test"
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'bob'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyStoragePath() >> 'keys/fake/path'
            getPrivateKeyStorageData() >> {
                new ByteArrayInputStream('data'.bytes)
            }
        }
        def listener = Mock(ExecutionListener)

        when:
        def result = SSHTaskBuilder.buildMultiScp(
                node,
                project,
                basedir,
                files,
                remotePath,
                nodeAuthentication,
                0,
                listener
        );
        then:
        result != null
        result instanceof ExtScp
        ExtScp built = (ExtScp) result
        built.getIfaceToDir() == 'bob@ahostname:monkey/test'
        built.getIfaceFileSets() != null
        built.getIfaceFileSets().size() == 1
        built.getIfaceFileSets()[0].getDir() == basedir
        built.getIfaceFileSets()[0].size() == expect.size()
        def resfiles = built.getIfaceFileSets().collect { it.iterator().collect { it.file } }.flatten() as Set
        def expfiles = expect.collect { filesMap[it] } as Set
        resfiles == expfiles

        where:
        dirname     | expect
        'sub1'      | ['sub1/test3.xml', 'sub1/sub2/test4.blah']
        'sub1/sub2' | ['sub1/sub2/test4.blah']
        null        | ['test1.txt', 'file2.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah']
    }

    def "build multi scp with files and dirs"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def project = new Project()
        def basedir = copyDir.toFile()
        def filesMap = makeDirFiles(copyDir, [
                'test1.txt',
                'file2.txt',
                'sub1/test3.xml',
                'sub1/sub2/test4.blah'
        ]
        )

        def files = dirnames ? dirnames.collect { new File(basedir, it) } : []
        files.addAll names.collect { filesMap[it] }

        def remotePath = "monkey/test"
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'bob'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyStoragePath() >> 'keys/fake/path'
            getPrivateKeyStorageData() >> {
                new ByteArrayInputStream('data'.bytes)
            }
        }
        def listener = Mock(ExecutionListener)

        when:
        def result = SSHTaskBuilder.buildMultiScp(
                node,
                project,
                basedir,
                files,
                remotePath,
                nodeAuthentication,
                0,
                listener
        );
        then:
        result != null
        result instanceof ExtScp
        ExtScp built = (ExtScp) result
        built.getIfaceToDir() == 'bob@ahostname:monkey/test'
        built.getIfaceFileSets() != null
        built.getIfaceFileSets().size() == 1
        built.getIfaceFileSets()[0].getDir() == basedir
        built.getIfaceFileSets()[0].size() == expect.size()
        def resfiles = built.getIfaceFileSets().collect { it.iterator().collect { it.file } }.flatten() as Set
        def expfiles = expect.collect { filesMap[it] } as Set
        resfiles == expfiles

        where:
        names         | dirnames      | expect
        []            | ['sub1']      | ['sub1/test3.xml', 'sub1/sub2/test4.blah']
        ['test1.txt'] | ['sub1']      | ['test1.txt', 'sub1/test3.xml', 'sub1/sub2/test4.blah']
        ['test1.txt'] | ['sub1/sub2'] | ['test1.txt', 'sub1/sub2/test4.blah']
    }
}
