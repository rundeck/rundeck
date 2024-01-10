/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.plugins.jsch

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import spock.lang.Specification


class JschSecretBundleUtilSpec extends Specification {
    public static final String PROJECT_NAME = 'JschSecretBundleUtilSpec'

    def "prepare secret bundle"() {
        given:
        def frameworkProject = Mock(IRundeckProject)
        def framework = Mock(Framework){
            getFrameworkProjectMgr()>> Mock(ProjectManager){
                getFrameworkProject(PROJECT_NAME) >> frameworkProject
            }

        }

        String privkey = "-----PRIV KEY-----"
        String privkeyPwd = "privKeyPwd"
        String sshPwd = "sshPwd"
        String sudoPwd = "sudoPwd"
        String sudo2Pwd = "sudo2Pwd"
        def storageTree = Mock(StorageTree) {
            1 * getResource("keys/ssh/priv") >> new MockResource(privkey)
            1 * getResource("keys/ssh/privk.pwd") >> new MockResource(privkeyPwd)
            1 * getResource("keys/ssh/pwd") >> new MockResource(sshPwd)
            1 * getResource("keys/ssh/sudo") >> new MockResource(sudoPwd)
            1 * getResource("keys/ssh/sudo2") >> new MockResource(sudo2Pwd)
        }
        def context = Mock(ExecutionContext) {
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            getExecutionListener() >> Mock(ExecutionListener)
            getStorageTree() >> storageTree
        }

        def node = new NodeEntryImpl('anode')
        node.setHostname("testhost")
        node.setAttribute(JschNodeExecutor.NODE_ATTR_SSH_KEY_RESOURCE,"keys/ssh/priv")
        node.setAttribute(JschNodeExecutor.NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH,"keys/ssh/privk.pwd")
        node.setAttribute(JschNodeExecutor.NODE_ATTR_SSH_PASSWORD_STORAGE_PATH,"keys/ssh/pwd")
        node.setAttribute(JschNodeExecutor.SUDO_OPT_PREFIX+JschNodeExecutor.NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH,"keys/ssh/sudo")
        node.setAttribute(JschNodeExecutor.SUDO2_OPT_PREFIX+JschNodeExecutor.NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH,"keys/ssh/sudo2")

        when:
        def secretBundle = JschSecretBundleUtil.createBundle(context,node)


        then:
        secretBundle.getValue("keys/ssh/priv") == privkey.bytes
        secretBundle.getValue("keys/ssh/privk.pwd") == privkeyPwd.bytes
        secretBundle.getValue("keys/ssh/pwd") == sshPwd.bytes
        secretBundle.getValue("keys/ssh/sudo") == sudoPwd.bytes
        secretBundle.getValue("keys/ssh/sudo2") == sudo2Pwd.bytes
    }

    class MockResource implements Resource<ResourceMeta> {

        private final String content

        MockResource(String content) {
            this.content = content
        }

        @Override
        Path getPath() {
            return null
        }

        @Override
        ResourceMeta getContents() {
            return new ResourceMeta() {
                @Override
                String getContentType() {
                    return null
                }

                @Override
                long getContentLength() {
                    return content.getBytes().length
                }

                @Override
                Date getModificationTime() {
                    return null
                }

                @Override
                Date getCreationTime() {
                    return null
                }

                @Override
                Map<String, String> getMeta() {
                    return null
                }

                @Override
                InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(content.getBytes())
                }

                @Override
                long writeContent(final OutputStream outputStream) throws IOException {
                    outputStream.write(content.getBytes())
                    return getContentLength()
                }
            }
        }

        @Override
        boolean isDirectory() {
            return false
        }
    }
}
