package com.dtolabs.rundeck.core.execution.workflow.steps.node

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import org.rundeck.storage.api.Resource
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 5/27/16.
 */
class ScriptBasedRemoteScriptNodeStepPluginSpec extends Specification {
    public static final String PROJECT_NAME = 'ScriptBasedRemoteScriptNodeStepPluginSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def teardown() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "Get file extension"() {
        expect:
        ext == ScriptBasedRemoteScriptNodeStepPlugin.getFileExtension(name)
        where:
        name          | ext
        'alpha.beta'  | 'beta'
        '.alpha.beta' | 'beta'
        'alpha.'      | null
        '.alpha'      | null
        '.'           | null
        'alpha.beta.' | null
        'alphabeta'   | null
        '.alphabeta'  | null
    }

    def "load key store defaults"() {
        given:
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        def pluginMeta = [
                config:
                        [
                                [
                                        name            : 'alpha',
                                        type            : 'String',
                                        renderingOptions: [
                                                (StringRenderingConstants.VALUE_CONVERSION_KEY): 'STORAGE_PATH_AUTOMATIC_READ',
                                        ]
                                ]

                        ]
        ]
        def storageTree = Mock(StorageTree)
        def provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
            getScriptArgs() >> 'test -blah ${config.alpha}'
        }
        def plugin = new ScriptBasedRemoteScriptNodeStepPlugin(provider, framework)
        def context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> [:]
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
                getStorageTree() >> storageTree
            }
        }

        def keyPath = 'keys/monkey-test/path'
        def config = [alpha: keyPath]
        def node = new NodeEntryImpl('anode')

        when:
        def result = plugin.generateScript(context, config, node)
        then:
        result != null
        Arrays.asList(result.args) == [tempFile.absolutePath, 'test', '-blah', 'myvalue']


        1 * storageTree.getResource(keyPath) >> Mock(Resource) {
            1 * getContents() >> Mock(ResourceMeta) {
                1 * writeContent(_) >> { args ->
                    args[0].write('myvalue'.bytes)
                    7L
                }
            }
        }

    }

    def "script-file-extension provider meta should supply file extension"() {
        given:
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        def pluginMeta = [
                'script-file-extension': 'myext',
        ]
        def provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
            getScriptArgs() >> 'test -blah x'
        }
        def plugin = new ScriptBasedRemoteScriptNodeStepPlugin(provider, framework)
        def context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> [:]
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        def config = [alpha: 'x']
        def node = new NodeEntryImpl('anode')

        when:
        def result = plugin.generateScript(context, config, node)
        then:
        result != null
        result instanceof FileBasedGeneratedScript
        FileBasedGeneratedScript fileBasedGeneratedScript = result
        fileBasedGeneratedScript.fileExtension == 'myext'

    }

    @Unroll
    def "use-original-file-extension value #testvalue provider meta should use original file extension"() {
        given:
        File tempFile = File.createTempFile("test", ".ribbit");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        def pluginMeta = [
                'use-original-file-extension': testvalue,
        ]
        def provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getDefaultMergeEnvVars() >> false
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
            getScriptArgs() >> 'test -blah x'
        }
        def plugin = new ScriptBasedRemoteScriptNodeStepPlugin(provider, framework)
        def context = Mock(PluginStepContext) {
            getFrameworkProject() >> PROJECT_NAME
            getDataContext() >> [:]
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        def config = [alpha: 'x']
        def node = new NodeEntryImpl('anode')

        when:
        def result = plugin.generateScript(context, config, node)
        then:
        result != null
        result instanceof FileBasedGeneratedScript
        FileBasedGeneratedScript fileBasedGeneratedScript = result
        fileBasedGeneratedScript.fileExtension == expectedExtension

        where:
        testvalue | expectedExtension
        'true'    | 'ribbit'
        true      | 'ribbit'
        'false'   | null
        false     | null
        null      | 'ribbit'
    }
}
