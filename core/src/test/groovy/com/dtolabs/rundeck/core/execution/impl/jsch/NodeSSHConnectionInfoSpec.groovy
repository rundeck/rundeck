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

package com.dtolabs.rundeck.core.execution.impl.jsch

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import org.rundeck.storage.api.Resource
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 3/18/15.
 */
class NodeSSHConnectionInfoSpec extends Specification {
    Framework framework
    FrameworkProject testProject
    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject=framework.getFrameworkProjectMgr().createFrameworkProject('NodeSSHConnectionInfoTest')
    }
    def cleanup(){
        framework.getFrameworkProjectMgr().removeFrameworkProject('NodeSSHConnectionInfoTest')
    }
    def "get default authentication type"(){
        setup:
        INodeEntry node = new NodeEntryImpl("test1");
        framework.getFrameworkProjectMgr().removeFrameworkProject('NodeSSHConnectionInfoTest')
        framework.getFrameworkProjectMgr().createFrameworkProject('NodeSSHConnectionInfoTest')
        def info=new NodeSSHConnectionInfo(node,framework,Mock(ExecutionContext){
            getFrameworkProject()>>'NodeSSHConnectionInfoTest'
        })
        expect:
        info.getAuthenticationType()==SSHTaskBuilder.AuthenticationType.privateKey
    }

    private static void mergeProps(File path, Properties props){
        def oldprops=new Properties()
        path.withInputStream { oldprops.load(it) }
        oldprops.putAll(props)
        path.withOutputStream { oldprops.store(it,"") }
    }
    private static void removeProps(File path, Collection<String> props){
        def oldprops=new Properties()
        path.withInputStream { oldprops.load(it) }
        props.each{oldprops.remove(it)}
        path.withOutputStream { oldprops.store(it,"") }
    }

    def "resolve ssh-authentication"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            SSHTaskBuilder.AuthenticationType expected
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                null,
                null
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if(null != fwkVal){
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getAuthenticationType() == expected


        where:
        propname             | attrval      | projectVal   | fwkVal       | expected
        'ssh-authentication' | null         | null         | null         | SSHTaskBuilder.AuthenticationType.privateKey
        'ssh-authentication' | null         | null         | 'privateKey' | SSHTaskBuilder.AuthenticationType.privateKey
        'ssh-authentication' | null         | null         | 'password'   | SSHTaskBuilder.AuthenticationType.password
        'ssh-authentication' | "privateKey" | null         | null         | SSHTaskBuilder.AuthenticationType.privateKey
        'ssh-authentication' | "password"   | null         | null         | SSHTaskBuilder.AuthenticationType.password
        'ssh-authentication' | null         | "privateKey" | null         | SSHTaskBuilder.AuthenticationType.privateKey
        'ssh-authentication' | null         | "password"   | null         | SSHTaskBuilder.AuthenticationType.password
    }
    def "resolve ssh-key-storage-path"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            String expected,
            Map<String,Map<String,String>> dataContext
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                null,
                dataContext
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if(null != fwkVal){
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getPrivateKeyStoragePath() == expected


        where:
        propname               | attrval             | projectVal        | fwkVal         | expected  | dataContext
        'ssh-key-storage-path' | null                | null              | null           | null      | null
        'ssh-key-storage-path' | null                | null              | '/a/path'      | '/a/path' | null
        'ssh-key-storage-path' | null                | null              | '/a/${job.id}' | '/a/123'  | [job: [id: '123']]
        'ssh-key-storage-path' | null                | '/b/path'         | '/a/path'      | '/b/path' | null
        'ssh-key-storage-path' | null                | '/b/${node.name}' | '/a/${job.id}' | '/b/blah' | [node: [name: 'blah']]
        'ssh-key-storage-path' | '/c/path'           | '/b/path'         | '/a/path'      | '/c/path' | null
        'ssh-key-storage-path' | '/c/${option.test}' | '/b/${node.name}' | '/a/${job.id}' | '/c/abc'  | [option: [test: 'abc']]
    }
    def "resolve ssh-password-storage-path"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            String expected,
            Map<String,Map<String,String>> dataContext
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                null,
                dataContext
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if(null != fwkVal){
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getPasswordStoragePath() == expected


        where:
        propname                    | attrval             | projectVal        | fwkVal         | expected  | dataContext
        'ssh-password-storage-path' | null                | null              | null           | null      | null
        'ssh-password-storage-path' | null                | null              | '/a/path'      | '/a/path' | null
        'ssh-password-storage-path' | null                | null              | '/a/${job.id}' | '/a/123'  | [job: [id: '123']]
        'ssh-password-storage-path' | null                | '/c/path'         | null           | '/c/path' | null
        'ssh-password-storage-path' | null                | '/c/${node.name}' | null           | '/c/blah' | [node: [name: 'blah']]
        'ssh-password-storage-path' | '/b/path'           | null              | null           | '/b/path' | null
        'ssh-password-storage-path' | '/b/${option.test}' | null              | null           | '/b/abc'  | [option: [test: 'abc']]
    }
    def "resolve ssh-key-passphrase-storage-path"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            String expected,
            Map<String,Map<String,String>> dataContext
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                null,
                dataContext
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if(null != fwkVal){
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getPrivateKeyPassphraseStoragePath() == expected


        where:
        propname                          | attrval             | projectVal        | fwkVal         | expected  | dataContext
        'ssh-key-passphrase-storage-path' | null                | null              | null           | null      | null
        'ssh-key-passphrase-storage-path' | null                | null              | '/a/path'      | '/a/path' | null
        'ssh-key-passphrase-storage-path' | null                | null              | '/a/${job.id}' | '/a/123'  | [job: [id: '123']]
        'ssh-key-passphrase-storage-path' | null                | '/c/path'         | null           | '/c/path' | null
        'ssh-key-passphrase-storage-path' | null                | '/c/${node.name}' | null           | '/c/blah' | [node: [name: 'blah']]
        'ssh-key-passphrase-storage-path' | '/b/path'           | null              | null           | '/b/path' | null
        'ssh-key-passphrase-storage-path' | '/b/${option.test}' | null              | null           | '/b/abc'  | [option: [test: 'abc']]
    }
    def "resolve sudo-password-storage-path"(
            String prefix,
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            String expected,
            Map<String,Map<String,String>> dataContext
    )
    {
        setup:
        def projectPropName = "project.${prefix}${propname}".toString()
        def frameworkPropName = "framework.${prefix}${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                prefix+propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                null,
                dataContext
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if(null != fwkVal){
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getSudoPasswordStoragePath(prefix) == expected


        where:
        prefix  | propname                | attrval             | projectVal        | fwkVal         | expected  | dataContext

        'sudo-' | 'password-storage-path' | null                | null              | null           | null      | null
        'sudo-' | 'password-storage-path' | null                | null              | '/a/path'      | '/a/path' | null
        'sudo-' | 'password-storage-path' | null                | null              | '/a/${job.id}' | '/a/123'  | [job: [id: '123']]
        'sudo-' | 'password-storage-path' | null                | '/c/path'         | null           | '/c/path' | null
        'sudo-' | 'password-storage-path' | null                | '/c/${node.name}' | null           | '/c/blah' | [node: [name: 'blah']]
        'sudo-' | 'password-storage-path' | '/b/path'           | null              | null           | '/b/path' | null
        'sudo-' | 'password-storage-path' | '/b/${option.test}' | null              | null           | '/b/abc'  | [option: [test: 'abc']]


        'sudo2-' | 'password-storage-path' | null                | null              | null           | null      | null
        'sudo2-' | 'password-storage-path' | null                | null              | '/a/path'      | '/a/path' | null
        'sudo2-' | 'password-storage-path' | null                | null              | '/a/${job.id}' | '/a/123'  | [job: [id: '123']]
        'sudo2-' | 'password-storage-path' | null                | '/c/path'         | null           | '/c/path' | null
        'sudo2-' | 'password-storage-path' | null                | '/c/${node.name}' | null           | '/c/blah' | [node: [name: 'blah']]
        'sudo2-' | 'password-storage-path' | '/b/path'           | null              | null           | '/b/path' | null
        'sudo2-' | 'password-storage-path' | '/b/${option.test}' | null              | null           | '/b/abc'  | [option: [test: 'abc']]

    }

    def "resolve secure option ssh password"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            Map<String,Map<String,String>> privateDataContext,
            String expectedVal
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                privateDataContext,
                null
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if (null != fwkVal) {
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getPassword() == expectedVal

        where:
        propname              | attrval            | projectVal         | fwkVal             | privateDataContext             | expectedVal
        'ssh-password-option' | null               | null               | null               | null                           | null
        'ssh-password-option' | null               | null               | null               | [option: [sshPassword: 'abc']] | 'abc'
        'ssh-password-option' | 'option.xpassword' | null               | null               | null                           | null
        'ssh-password-option' | 'option.xpassword' | null               | null               | [option: [sshPassword: 'abc']] | null
        'ssh-password-option' | 'option.xpassword' | null               | null               | [option: [xpassword: 'def']]   | 'def'
        'ssh-password-option' | null               | 'option.ypassword' | null               | null                           | null
        'ssh-password-option' | null               | 'option.ypassword' | null               | [option: [sshPassword: 'abc']] | null
        'ssh-password-option' | null               | 'option.ypassword' | null               | [option: [ypassword: 'ghi']]   | 'ghi'
        'ssh-password-option' | null               | null               | 'option.zpassword' | null                           | null
        'ssh-password-option' | null               | null               | 'option.zpassword' | [option: [sshPassword: 'abc']] | null
        'ssh-password-option' | null               | null               | 'option.zpassword' | [option: [zpassword: 'jkl']]   | 'jkl'
    }

    def "resolve secure option ssh key passphrase"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            Map<String,Map<String,String>> privateDataContext,
            String expectedVal
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                privateDataContext,
                null
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if (null != fwkVal) {
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getPrivateKeyPassphrase() == expectedVal

        where:
        propname              | attrval            | projectVal         | fwkVal             | privateDataContext             | expectedVal
        'ssh-key-passphrase-option' | null               | null               | null               | null                           | null
        'ssh-key-passphrase-option' | null               | null               | null               | [option: [sshKeyPassphrase: 'abc']] | 'abc'
        'ssh-key-passphrase-option' | 'option.xpassword' | null               | null               | null                           | null
        'ssh-key-passphrase-option' | 'option.xpassword' | null               | null               | [option: [sshKeyPassphrase: 'abc']] | null
        'ssh-key-passphrase-option' | 'option.xpassword' | null               | null               | [option: [xpassword: 'def']]   | 'def'
        'ssh-key-passphrase-option' | null               | 'option.ypassword' | null               | null                           | null
        'ssh-key-passphrase-option' | null               | 'option.ypassword' | null               | [option: [sshKeyPassphrase: 'abc']] | null
        'ssh-key-passphrase-option' | null               | 'option.ypassword' | null               | [option: [ypassword: 'ghi']]   | 'ghi'
        'ssh-key-passphrase-option' | null               | null               | 'option.zpassword' | null                           | null
        'ssh-key-passphrase-option' | null               | null               | 'option.zpassword' | [option: [sshKeyPassphrase: 'abc']] | null
        'ssh-key-passphrase-option' | null               | null               | 'option.zpassword' | [option: [zpassword: 'jkl']]   | 'jkl'
    }
    def "resolve secure option sudo password"(
            String prefix,
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            Map<String,Map<String,String>> privateDataContext,
            String expectedVal
    )
    {
        setup:
        def projectPropName = "project.${prefix}${propname}".toString()
        def frameworkPropName = "framework.${prefix}${propname}".toString()

        NodeSSHConnectionInfo info = setupSecureOptionTest(
                prefix+propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName,
                privateDataContext,
                null
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if (null != fwkVal) {
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        info.getSudoPassword(prefix) == expectedVal

        where:

        prefix  | propname          | attrval            | projectVal         | fwkVal             | privateDataContext              | expectedVal
        'sudo-' | 'password-option' | null               | null               | null               | null                            | null
        'sudo-' | 'password-option' | null               | null               | null               | [option: [sudoPassword: 'abc']] | 'abc'
        'sudo-' | 'password-option' | 'option.xpassword' | null               | null               | null                            | null
        'sudo-' | 'password-option' | 'option.xpassword' | null               | null               | [option: [sudoPassword: 'abc2']] | null
        'sudo-' | 'password-option' | 'option.xpassword' | null               | null               | [option: [xpassword: 'def']]    | 'def'
        'sudo-' | 'password-option' | null               | 'option.ypassword' | null               | null                            | null
        'sudo-' | 'password-option' | null               | 'option.ypassword' | null               | [option: [sudoPassword: 'abc3']] | null
        'sudo-' | 'password-option' | null               | 'option.ypassword' | null               | [option: [ypassword: 'ghi']]    | 'ghi'
        'sudo-' | 'password-option' | null               | null               | 'option.zpassword' | null                            | null
        'sudo-' | 'password-option' | null               | null               | 'option.zpassword' | [option: [sudoPassword: 'abc4']] | null
        'sudo-' | 'password-option' | null               | null               | 'option.zpassword' | [option: [zpassword: 'jkl']]    | 'jkl'

        'sudo2-' | 'password-option' | null               | null               | null               | null                            | null
        'sudo2-' | 'password-option' | null               | null               | null               | [option: [sudo2Password: 'abc']] | 'abc'
        'sudo2-' | 'password-option' | 'option.xpassword' | null               | null               | null                            | null
        'sudo2-' | 'password-option' | 'option.xpassword' | null               | null               | [option: [sudo2Password: 'abc2']] | null
        'sudo2-' | 'password-option' | 'option.xpassword' | null               | null               | [option: [xpassword: 'def']]    | 'def'
        'sudo2-' | 'password-option' | null               | 'option.ypassword' | null               | null                            | null
        'sudo2-' | 'password-option' | null               | 'option.ypassword' | null               | [option: [sudo2Password: 'abc3']] | null
        'sudo2-' | 'password-option' | null               | 'option.ypassword' | null               | [option: [ypassword: 'ghi']]    | 'ghi'
        'sudo2-' | 'password-option' | null               | null               | 'option.zpassword' | null                            | null
        'sudo2-' | 'password-option' | null               | null               | 'option.zpassword' | [option: [sudo2Password: 'abc4']] | null
        'sudo2-' | 'password-option' | null               | null               | 'option.zpassword' | [option: [zpassword: 'jkl']]    | 'jkl'
    }

    def "sudo password storage data"( String prefix,
                                      String propname,
                                      String attrval,
                                      String expectedVal){
        setup:

        NodeSSHConnectionInfo info = setupStorageVal(
                prefix+propname,
                attrval,
                expectedVal
        )

        expect:
        info.getSudoPasswordStorageData(prefix) == expectedVal.bytes

        where:
        prefix   | propname                | attrval                | expectedVal
        'sudo-'  | 'password-storage-path' | '/some/file.password'  | 'testpassword'
        'sudo2-' | 'password-storage-path' | '/some/file2.password' | 'testpassword2'

    }
    def "ssh password storage data"(
                                      String propname,
                                      String attrval,
                                      String expectedVal){
        setup:

        NodeSSHConnectionInfo info = setupStorageVal(
                propname,
                attrval,
                expectedVal
        )

        expect:
        info.getPasswordStorageData() == ( null!=expectedVal?expectedVal.bytes:null)

        where:
        propname                    | attrval                | expectedVal
        'ssh-password-storage-path' | '/some/file.password'  | 'testpassword'
        'ssh-password-storage-path' | '/some/file2.password' | 'testpassword2'
        'ssh-password-storage-path' | null                   | null
        'wrong-attribute'           | '/some/file2.password' | null

    }

    def "private key passphrase storage data"(
                                      String propname,
                                      String attrval,
                                      String dataValue,
                                      String expectedVal){
        setup:

        NodeSSHConnectionInfo info = setupStorageVal(
                propname,
                attrval,
                dataValue
        )

        expect:
        info.getPrivateKeyPassphraseStorageData() == ( null!=expectedVal?expectedVal.bytes:null)

        where:
        propname                          | attrval                | dataValue       | expectedVal
        'ssh-key-passphrase-storage-path' | '/some/file.password'  | 'testpassword'  | 'testpassword'
        'ssh-key-passphrase-storage-path' | '/some/file2.password' | 'testpassword2' | 'testpassword2'
        'ssh-key-passphrase-storage-path' | null                   | 'testpassword'  | null
        'wrong-attribute'                 | '/some/file2.password' | 'testpassword'  | null

    }
    static String fwkSSHTimeoutPropA = JschNodeExecutor.FRAMEWORK_SSH_CONNECT_TIMEOUT_PROP
    static String fwkSSHTimeoutPropB = JschNodeExecutor.SSH_TIMEOUT_PROP

    @Unroll
    def "ssh connection timeout config"() {


        setup:
        def context = Mock(ExecutionContext) {
            getFrameworkProject() >> 'NodeSSHConnectionInfoTest'
        }
        def propName = JschNodeExecutor.NODE_ATTR_SSH_CONNECT_TIMEOUT_PROP
        def projectPropName = JschNodeExecutor.PROJ_PROP_CON_TIMEOUT
        def frameworkPropName = JschNodeExecutor.FWK_PROP_CON_TIMEOUT
//        def frameworkPropName2 = JschNodeExecutor.FRAMEWORK_SSH_CONNECT_TIMEOUT_PROP

        NodeEntryImpl node = setupProps(
                propName,
                nodepropval,
                projectPropName,
                projectpropval,
                [
                        (frameworkPropName)                                  : fwkpropval,
                        (JschNodeExecutor.FRAMEWORK_SSH_CONNECT_TIMEOUT_PROP): !useDeprecated ? fwkpropval2 : null,
                        (JschNodeExecutor.SSH_TIMEOUT_PROP)                  : useDeprecated ? fwkpropval2 : null
                ],
                )
        when:
        NodeSSHConnectionInfo info = new NodeSSHConnectionInfo(node, framework, context)

        then:
        info.getConnectTimeout() == expected

        where:

        nodepropval | projectpropval | fwkpropval | fwkpropval2 | useDeprecated | expected
        null        | null           | null       | null        | false         | 0l
        '123'       | null           | null       | null        | false         | 123l
        '123'       | '321'          | null       | null        | false         | 123l
        '123'       | '321'          | '456'      | null        | false         | 123l
        '123'       | '321'          | '456'      | '789'       | false         | 123l
        null        | '321'          | '456'      | '789'       | false         | 321L
        null        | null           | '456'      | '789'       | false         | 456L
        null        | null           | null       | '789'       | false         | 789L
        null        | null           | null       | null        | true          | 0l
        '123'       | null           | null       | null        | true          | 123l
        '123'       | '321'          | null       | null        | true          | 123l
        '123'       | '321'          | '456'      | null        | true          | 123l
        '123'       | '321'          | '456'      | '789'       | true          | 123l
        null        | '321'          | '456'      | '789'       | true          | 321L
        null        | null           | '456'      | '789'       | true          | 456L
        null        | null           | null       | '789'       | true          | 789L

    }

    private NodeEntryImpl setupProps(
            String propName,
            String nodepropval,
            String projectPropName,
            String projectpropval,
            Map<String, String> fwkProps
    )
    {
        INodeEntry node = new NodeEntryImpl("test1");
        if (nodepropval) {
            node.getAttributes().put(propName, nodepropval)
        }
        if (projectpropval) {
            def projprops = new Properties()
            projprops.put(projectPropName, projectpropval)
            def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')
            testProject.mergeProjectProperties(projprops, [] as Set)
            testProject.hasProperty(projectPropName)//trigger refresh
        }
        removeProps(
                new File(framework.getBaseDir(), "etc/framework.properties"),
                fwkProps.keySet()
        )
        if (fwkProps) {
            def pfwkProps = new Properties()
            fwkProps.each { k, v ->
                if (v) {
                    pfwkProps.setProperty(k, v)
                }
            }
            mergeProps(new File(framework.getBaseDir(), "etc/framework.properties"), pfwkProps)
        }
        node
    }

    @Unroll
    def "ssh command timeout config"() {
        setup:
        def context = Mock(ExecutionContext) {
            getFrameworkProject() >> 'NodeSSHConnectionInfoTest'
        }
        def propName = JschNodeExecutor.NODE_ATTR_SSH_COMMAND_TIMEOUT_PROP
        def projectPropName = JschNodeExecutor.PROJ_PROP_COMMAND_TIMEOUT
        def frameworkPropName = JschNodeExecutor.FWK_PROP_COMMAND_TIMEOUT
        def frameworkPropName2 = JschNodeExecutor.FRAMEWORK_SSH_COMMAND_TIMEOUT_PROP

        NodeEntryImpl node = setupProps(
                propName,
                nodepropval,
                projectPropName,
                projectpropval,
                [
                        (frameworkPropName) : fwkpropval,
                        (frameworkPropName2): fwkpropval2
                ],
                )
        when:
        NodeSSHConnectionInfo info = new NodeSSHConnectionInfo(node, framework, context)

        then:
        info.getCommandTimeout() == expected

        where:
        nodepropval | projectpropval | fwkpropval | fwkpropval2 | expected
        null        | null           | null       | null        | 0l
        '123'       | null           | null       | null        | 123l
        '123'       | '321'          | null       | null        | 123l
        '123'       | '321'          | '456'      | null        | 123l
        '123'       | '321'          | '456'      | '789'       | 123l
        null        | '321'          | '456'      | '789'       | 321L
        null        | null           | '456'      | '789'       | 456L
        null        | null           | null       | '789'       | 789L

    }

    private NodeSSHConnectionInfo setupStorageVal(
            String propname,
            String path,
            String storageVal
    )
    {
        INodeEntry node = new NodeEntryImpl("test1");
        node.getAttributes().put(propname, path)
        new NodeSSHConnectionInfo(
                node,
                framework,
                Mock(ExecutionContext) {
                    getFrameworkProject() >> 'NodeSSHConnectionInfoTest'
                    getStorageTree() >> Mock(StorageTree){
                        getResource(path) >> Mock(Resource){
                            getContents() >> Mock(ResourceMeta){
                                writeContent(_)>>{args->
                                    args[0].write(storageVal.bytes)
                                    storageVal.bytes.length
                                }
                            }
                        }
                    }
                }
        )
    }

    private  NodeSSHConnectionInfo setupSecureOptionTest(
            String propname,
            String attrval,
            String projectVal,
            String projectPropName,
            String fwkVal,
            String frameworkPropName,
            Map<String, Map<String, String>> privateDataContext,
            Map<String, Map<String, String>> dataContext
    )
    {
        INodeEntry node = new NodeEntryImpl("test1");
        node.getAttributes().put(propname, attrval)

        def removePrefixes = [] as Set
        def projprops = new Properties()


        if (null != projectVal) {
            projprops.put(projectPropName, projectVal)
        } else {
            removePrefixes << projectPropName
        }
        def framework = AbstractBaseTest.createTestFramework()
        framework.getFrameworkProjectMgr().removeFrameworkProject('NodeSSHConnectionInfoTest')
        framework.getFrameworkProjectMgr().createFrameworkProject('NodeSSHConnectionInfoTest')
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('NodeSSHConnectionInfoTest')
        testProject.mergeProjectProperties(projprops, removePrefixes)
        testProject.hasProperty(projectPropName)//trigger refresh


        if (null != fwkVal) {
            def fwkProps = new Properties()
            fwkProps.setProperty(frameworkPropName, fwkVal)
            mergeProps(new File(framework.getBaseDir(), "etc/framework.properties"), fwkProps)
        } else {
            removeProps(new File(framework.getBaseDir(), "etc/framework.properties"), [frameworkPropName])
        }
        //reload fwk props
        framework = AbstractBaseTest.createTestFramework()

        new NodeSSHConnectionInfo(
                node,
                framework,
                Mock(ExecutionContext) {
                    getFrameworkProject() >> 'NodeSSHConnectionInfoTest'
                    getPrivateDataContext() >> DataContextUtils.context(privateDataContext)
                    getDataContext() >> DataContextUtils.context(dataContext)
                }
        )
    }
}
