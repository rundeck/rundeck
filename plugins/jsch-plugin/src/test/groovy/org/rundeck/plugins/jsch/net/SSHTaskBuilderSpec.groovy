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

package org.rundeck.plugins.jsch.net

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.utils.FileUtils
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Location
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.optional.ssh.ScpToMessage
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

/**
 * ExtScp test double that records the host the task would connect to and aborts before any network I/O.
 */
class HostCapturingScp extends ExtScp {
    String capturedHost

    @Override
    protected Session openSession() throws JSchException {
        capturedHost = getHost()
        throw new JSchException('test-abort: no real connection')
    }
}

/**
 * ExtScp test double that captures the prepared upload message instead of transferring anything.
 */
class MessageCapturingScp extends ExtScp {
    ScpToMessage captured
    boolean sessionOpened

    @Override
    protected Session openSession() throws JSchException {
        sessionOpened = true
        return null
    }

    @Override
    protected void sendMessage(final ScpToMessage message) {
        captured = message
    }
}

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
        built.getIfaceRemotePath() == 'monkey/test'
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
        built.getIfaceRemotePath() == 'monkey/test'
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
        built.getIfaceRemotePath() == 'monkey/test'
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

    def "buildScp with timeouts"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def project = new Project()
        def basedir = copyDir.toFile()
        def filesMap = makeDirFiles(copyDir, [
                'test1.txt'
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
            getConnectTimeout() >> 40000
            getCommandTimeout() >> 50000
        }
        def listener = Mock(ExecutionListener)

        when:
        def result = SSHTaskBuilder.buildScp(
                node,
                project,
                remotePath,
                files.get(0),
                nodeAuthentication,
                0,
                listener
        )

        then:

        result != null
        result instanceof ExtScp
        ExtScp built = (ExtScp) result

        built.getCommandTimeout()==50000
        built.getConnectTimeout()==40000

        where:
        copyfiles                                                            | _
        ['test1.txt']                                                        | _
    }

    def "buildMultiScp with timeouts"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def project = new Project()
        def basedir = copyDir.toFile()
        def filesMap = makeDirFiles(copyDir, [
                'test1.txt'
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
            getConnectTimeout() >> 40000
            getCommandTimeout() >> 50000
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

        built.getCommandTimeout()==50000
        built.getConnectTimeout()==40000

        where:
        copyfiles                                                            | _
        ['test1.txt']                                                        | _

    }

    def "buildSsh with bind address"() {
        given:
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
            getAttributes() >> [:]
        }
        def project = new Project()

        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'bob'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyStoragePath() >> 'keys/fake/path'
            getPrivateKeyStorageData() >> {
                new ByteArrayInputStream('data'.bytes)
            }
            getBindAddress() >> "192.168.0.120"
        }
        def listener = Mock(ExecutionListener)

        String[] command=["ls"]
        def datacontext=[:]

        when:
        def result = SSHTaskBuilder.build(
                node,
                command,
                project,
                datacontext,
                nodeAuthentication,
                0,
                listener
        );
        then:
        result != null
        result instanceof ExtSSHExec
        ExtSSHExec built = (ExtSSHExec) result

        built.getBindAddress() == "192.168.0.120"

    }

    def "windows remote path with colon does not corrupt scp host"() {
        given:
        def scp = new HostCapturingScp()
        def keyfile = Files.createTempFile(testDir, 'key', 'file').toFile()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'bob'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyfilePath() >> keyfile.absolutePath
        }
        SSHTaskBuilder.buildScp(
                scp,
                node,
                new Project(),
                'C:\\WINDOWS\\TEMP\\test.bat',
                sourceFile,
                nodeAuthentication,
                0,
                Mock(ExecutionListener)
        )

        when:
        scp.execute()

        then:
        thrown(BuildException)
        scp.capturedHost == 'ahostname'
    }

    SSHTaskBuilder.SSHConnectionInfo storageKeyAuth(String username = 'bob') {
        Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> username
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyStoragePath() >> 'keys/fake/path'
            getPrivateKeyStorageData() >> {
                new ByteArrayInputStream('data'.bytes)
            }
        }
    }

    def "single file remote path reaches scp transfer verbatim"() {
        given:
        def scp = new MessageCapturingScp()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildScp(scp, node, new Project(), remotePath, sourceFile, storageKeyAuth(), 0, Mock(ExecutionListener))

        when:
        scp.execute()

        then:
        scp.captured != null
        scp.captured.remotePath == remotePath
        scp.captured.localFile == sourceFile
        scp.host == 'ahostname'

        where:
        remotePath                    | _
        'C:\\WINDOWS\\TEMP\\test.bat' | _
        '/tmp/rundeck/test.sh'        | _
        '/tmp/with:colon/test.sh'     | _
        'monkey/test'                 | _
    }

    def "multi scp remote path reaches scp transfer verbatim"() {
        given:
        def scp = new MessageCapturingScp()
        def basedir = copyDir.toFile()
        def files = makeDirFiles(copyDir, ['test1.txt', 'sub1/test3.xml']).values().toList()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildMultiScp(scp, node, new Project(), basedir, files, 'C:\\WINDOWS\\TEMP', storageKeyAuth(), 0, Mock(ExecutionListener))

        when:
        scp.execute()

        then:
        scp.captured != null
        scp.captured.remotePath == 'C:\\WINDOWS\\TEMP'
        scp.captured.localFile == null
        scp.host == 'ahostname'
    }

    def "recursive scp remote path reaches scp transfer verbatim"() {
        given:
        def scp = new MessageCapturingScp()
        makeDirFiles(copyDir, ['test1.txt', 'sub1/test3.xml'])
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildRecursiveScp(scp, node, new Project(), 'C:\\WINDOWS\\TEMP', copyDir.toFile(), storageKeyAuth(), 0, Mock(ExecutionListener))

        when:
        scp.execute()

        then:
        scp.captured != null
        scp.captured.remotePath == 'C:\\WINDOWS\\TEMP'
        scp.captured.localFile == null
        scp.host == 'ahostname'
    }

    def "execute falls back to ant behavior when remotePath unset"() {
        given:
        def scp = new HostCapturingScp()
        def keyfile = Files.createTempFile(testDir, 'key', 'file').toFile()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        scp.setProject(new Project())
        scp.setFailonerror(true)
        scp.setKeyfile(keyfile.absolutePath)
        scp.setLocalFile(sourceFile.absolutePath)
        scp.setRemoteTofile('bob@ahostname:/some/path')

        when:
        scp.execute()

        then:
        thrown(BuildException)
        scp.capturedHost == 'ahostname'
    }

    def "execute failure honours failonerror and sets location"() {
        given:
        def scp = new HostCapturingScp()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildScp(scp, node, new Project(), '/tmp/test.sh', sourceFile, storageKeyAuth(), 0, Mock(ExecutionListener))
        def location = new Location('jsch-scp-test', 7, 1)
        scp.setLocation(location)

        when:
        scp.execute()

        then:
        def e = thrown(BuildException)
        e.location == location
        e.cause instanceof JSchException
    }

    def "execute failure is logged when failonerror is false"() {
        given:
        def scp = new HostCapturingScp()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildScp(scp, node, new Project(), '/tmp/test.sh', sourceFile, storageKeyAuth(), 0, Mock(ExecutionListener))
        scp.setFailonerror(false)

        when:
        scp.execute()

        then:
        notThrown(BuildException)
        scp.capturedHost == 'ahostname'
    }

    def "execute with remotePath and no localFile or filesets fails"() {
        given:
        def scp = new MessageCapturingScp()
        scp.setProject(new Project())
        scp.setFailonerror(true)
        scp.setRemotePath('/tmp/test.sh')

        when:
        scp.execute()

        then:
        def e = thrown(BuildException)
        e.message.contains('localFile')
        scp.captured == null
    }

    def "buildScp with private key sets connection params and raw remote path"() {
        given:
        def built = new ExtScp()
        def keyfile = Files.createTempFile(testDir, 'key', 'file').toFile()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def project = new Project()
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'testusername'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.privateKey
            getPrivateKeyfilePath() >> keyfile.absolutePath
        }

        when:
        SSHTaskBuilder.buildScp(built, new NodeEntryImpl('hostname', 'nodename'), project, '/test/path', sourceFile, nodeAuthentication, 0, Mock(ExecutionListener))

        then:
        built.ifaceRemotePath == '/test/path'
        built.host == 'hostname'
        built.port == 22
        built.keyfile == keyfile.absolutePath
        built.userInfo.passphrase == ''
        built.userInfo.password == null
        built.userInfo.name == 'testusername'
        !built.verbose
        built.failonerror
        built.userInfo.trust
        built.knownhosts == null
        built.project == project
    }

    def "buildScp with password auth"() {
        given:
        def built = new ExtScp()
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> 'testusername'
            getAuthenticationType() >> SSHTaskBuilder.AuthenticationType.password
            getPassword() >> 'passwordValue'
        }

        when:
        SSHTaskBuilder.buildScp(built, new NodeEntryImpl('hostname', 'nodename'), new Project(), '/test/path', sourceFile, nodeAuthentication, 0, Mock(ExecutionListener))

        then:
        built.ifaceRemotePath == '/test/path'
        built.host == 'hostname'
        built.keyfile == null
        built.userInfo.password == 'passwordValue'
        built.userInfo.name == 'testusername'
        built.failonerror
        built.userInfo.trust
    }

    def "buildScp fails when username not set"() {
        given:
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()
        def nodeAuthentication = Mock(SSHTaskBuilder.SSHConnectionInfo) {
            getUsername() >> null
            getAuthenticationType() >> authType
            getPassword() >> 'passwordValue'
        }

        when:
        SSHTaskBuilder.buildScp(new ExtScp(), new NodeEntryImpl('hostname', 'nodename'), new Project(), '/test/path', sourceFile, nodeAuthentication, 0, Mock(ExecutionListener))

        then:
        def e = thrown(SSHTaskBuilder.BuilderException)
        e.message == 'username was not set'

        where:
        authType << [SSHTaskBuilder.AuthenticationType.privateKey, SSHTaskBuilder.AuthenticationType.password]
    }

    def "buildScp fails when sourceFile not set"() {
        when:
        SSHTaskBuilder.buildScp(new ExtScp(), new NodeEntryImpl('hostname', 'nodename'), new Project(), '/test/path', null, storageKeyAuth('testusername'), 0, Mock(ExecutionListener))

        then:
        def e = thrown(SSHTaskBuilder.BuilderException)
        e.message == 'sourceFile was not set'
    }

    def "buildScp fails when remotePath not set"() {
        given:
        def sourceFile = Files.createTempFile(testDir, 'src', 'file').toFile()

        when:
        SSHTaskBuilder.buildScp(new ExtScp(), new NodeEntryImpl('hostname', 'nodename'), new Project(), null, sourceFile, storageKeyAuth('testusername'), 0, Mock(ExecutionListener))

        then:
        def e = thrown(SSHTaskBuilder.BuilderException)
        e.message == 'remotePath was not set'
    }

    def "buildRecursiveScp fails when remotePath not set"() {
        when:
        SSHTaskBuilder.buildRecursiveScp(new ExtScp(), new NodeEntryImpl('hostname', 'nodename'), new Project(), null, copyDir.toFile(), storageKeyAuth('testusername'), 0, Mock(ExecutionListener))

        then:
        def e = thrown(SSHTaskBuilder.BuilderException)
        e.message == 'remotePath was not set'
    }

    def "buildMultiScp fails when remotePath not set"() {
        given:
        def files = makeDirFiles(copyDir, ['test1.txt']).values().toList()

        when:
        SSHTaskBuilder.buildMultiScp(new ExtScp(), new NodeEntryImpl('hostname', 'nodename'), new Project(), copyDir.toFile(), files, null, storageKeyAuth('testusername'), 0, Mock(ExecutionListener))

        then:
        def e = thrown(SSHTaskBuilder.BuilderException)
        e.message == 'remotePath was not set'
    }

    def "execute does not open a session when filesets match no files"() {
        given:
        def scp = new MessageCapturingScp()
        def node = Mock(INodeEntry) {
            extractHostname() >> 'ahostname'
        }
        SSHTaskBuilder.buildRecursiveScp(scp, node, new Project(), '/tmp/dest', copyDir.toFile(), storageKeyAuth(), 0, Mock(ExecutionListener))

        when:
        scp.execute()

        then:
        notThrown(BuildException)
        !scp.sessionOpened
        scp.captured == null
    }
}
