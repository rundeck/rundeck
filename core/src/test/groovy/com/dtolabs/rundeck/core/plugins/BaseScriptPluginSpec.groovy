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

package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.ScriptExecHelper
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/11/16.
 */
class BaseScriptPluginSpec extends Specification {
    public static final String PROJECT_NAME = 'BaseScriptPluginSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    class TestScriptPlugin extends BaseScriptPlugin {

        protected TestScriptPlugin(ScriptPluginProvider provider, Framework framework) {
            super(provider, framework);
        }

        @Override
        public boolean isAllowCustomProperties() {
            return true;
        }

        @Override
        public String[] createScriptArgs(
                Map<String, Map<String, String>> localDataContext
        )
        {
            return super.createScriptArgs(localDataContext);
        }

        @Override
        public ExecArgList createScriptArgsList(
                Map<String, Map<String, String>> dataContext
        )
        {
            return super.createScriptArgsList(dataContext);
        }
    }

    @Unroll
    def "merge env vars true"() {
        given:
        def pluginMeta = [(BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): setVal]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> defVal
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray = []

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        when:
        def result = plugin.runPluginScript(context, null, null, framework, [:])


        then:
        1 * plugin.scriptExecHelper.loadLocalEnvironment() >> [bloo: 'blah']
        1 * plugin.scriptExecHelper.runLocalCommand(_, {
            it.bloo == 'blah'
        }, _, null, null
        ) >> 0

        where:
        setVal | defVal
        true   | false
        null   | true
    }

    def "merge env vars false"() {
        given:
        def pluginMeta = [(BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): setVal]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> defVal
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray = []

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        when:
        def result = plugin.runPluginScript(context, null, null, framework, [:])


        then:
        0 * plugin.scriptExecHelper.loadLocalEnvironment()
        1 * plugin.scriptExecHelper.runLocalCommand(_, {
            it.bloo == null
        }, _, null, null
        ) >> 0


        where:
        setVal | defVal
        false  | true
        false  | false
        null   | false
    }


    @Unroll
    def "key storage value loaded as config data for node step"() {
        given:
        def node = new NodeEntryImpl('anode')

        def pluginMeta = [
                (BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): false,
                config                                      :
                        [
                                [
                                        type            : 'String',
                                        name            : 'c',
                                        renderingOptions: [
                                                (StringRenderingConstants.VALUE_CONVERSION_KEY): 'STORAGE_PATH_AUTOMATIC_READ',
                                        ]
                                ]
                        ]
        ]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray = []

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        def storageTree = Mock(StorageTree)

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME

                getStorageTree() >> storageTree
            }
        }

        def config = [a: 'b', c: 'keys/test']

        when:
        def result = plugin.runPluginScript(context, null, null, framework, config)


        then:
        1 * plugin.scriptExecHelper.runLocalCommand(_, {
            it.RD_CONFIG_A == 'b' && it.RD_CONFIG_C == 'myvalue'
        }, _, null, null
        ) >> 0

        1 * storageTree.getResource('keys/test') >> Mock(Resource) {
            1 * getContents() >> Mock(ResourceMeta) {
                1 * writeContent(_) >> { args ->
                    args[0].write('myvalue'.bytes)
                    7L
                }
            }
        }

    }

    @Unroll
    def "key storage value not found as config data for node step"() {
        given:
        def node = new NodeEntryImpl('anode')

        def pluginMeta = [
                (BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): false,
                config                                      :
                        [
                                [
                                        type            : 'String',
                                        name            : 'c',
                                        renderingOptions: [
                                                (StringRenderingConstants.VALUE_CONVERSION_KEY): 'STORAGE_PATH_AUTOMATIC_READ',
                                        ]
                                ]
                        ]
        ]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray = []

        ScriptExecHelper helper = Mock(ScriptExecHelper)

        def storageTree = Mock(StorageTree)

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME

                getStorageTree() >> storageTree
            }
        }

        def config = [a: 'b', c: 'keys/test']

        when:
        def result = plugin.runPluginScript(context, null, null, framework, config)

        then:
        ConfigurationException e = thrown()
        0 * helper.createScriptArgs(_, null, _, null, false, _)

        1 * storageTree.getResource(_) >> {
            throw new StorageException("Not found", StorageException.Event.READ, PathUtil.asPath('keys/test'))
        }

    }

    @Unroll
    def "key storage value not found set blank as config data for node step"() {
        given:
        def node = new NodeEntryImpl('anode')

        def pluginMeta = [
                (BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): false,
                config                                      :
                        [
                                [
                                        type            : 'String',
                                        name            : 'c',
                                        renderingOptions: [
                                                (StringRenderingConstants.VALUE_CONVERSION_KEY)        : 'STORAGE_PATH_AUTOMATIC_READ',
                                                (StringRenderingConstants.VALUE_CONVERSION_FAILURE_KEY): StringRenderingConstants.VALUE_CONVERSION_FAILURE_REMOVE,
                                        ]
                                ]
                        ]
        ]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray = []

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        def storageTree = Mock(StorageTree)

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME

                getStorageTree() >> storageTree
            }
        }

        def config = [a: 'b', c: 'keys/test']

        when:
        def result = plugin.runPluginScript(context, null, null, framework, config)

        then:
        1 * plugin.scriptExecHelper.runLocalCommand(_, {
            it.RD_CONFIG_A == 'b' && it.RD_CONFIG_C == null
        }, _, null, null
        ) >> 0

        1 * storageTree.getResource(_) >> {
            throw new StorageException("Not found", StorageException.Event.READ, PathUtil.asPath('keys/test'))
        }

    }
}
