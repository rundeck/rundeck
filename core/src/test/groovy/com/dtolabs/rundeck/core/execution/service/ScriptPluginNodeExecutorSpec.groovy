package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.ScriptExecHelper
import spock.lang.Specification

class ScriptPluginNodeExecutorSpec extends Specification {

    public static final String PROJECT_NAME = 'ScriptPluginNodeExecutorSpec'

    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def "test private context"(){
        when:

        TestScriptNodeExecutor nodeExecutor = new TestScriptNodeExecutor()
        nodeExecutor.name = "test-script-node-executor"
        nodeExecutor.service = "NodeExecutor"
        nodeExecutor.metadata = [:]
        File archiveFile = File.createTempFile("test-script-node-executor", "tmp");
        archiveFile.deleteOnExit();
        nodeExecutor.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        nodeExecutor.setScriptFile(scriptFile);
        final File baseDir = File.createTempFile("test-basedir", "tmp");
        baseDir.deleteOnExit();
        nodeExecutor.contentsBaseDir = baseDir
        nodeExecutor.scriptArgs = ""
        PluginMeta pluginMeta = new PluginMeta()
        pluginMeta.setRundeckPluginVersion("1.2")
        nodeExecutor.pluginMeta = pluginMeta

        def data = [:]

        def optionContext = new BaseDataContext([option: data])
        DataContext dataContext = DataContextUtils.context()

        ExecutionContext context = Mock(ExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getDataContextObject() >> dataContext
            getPrivateDataContext() >> privateDataContext
            getFrameworkProject() >> PROJECT_NAME
            getExecutionListener() >> Mock(ExecutionListener)
        }
        def node = new NodeEntryImpl('node')
        def command = ["ls","-lrt"]
        def resultCode = 2

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1* runLocalCommand(_,_,{ Map ctx ->

                if(privateDataContext){
                    ctx.containsKey("private") == true && ctx.private == privateData
                }else{
                    ctx.containsKey("private") == false
                }

            },_,_,_) >> resultCode
        }

        ScriptPluginNodeExecutor scriptNodeExecutor = new ScriptPluginNodeExecutor(nodeExecutor,framework)
        scriptNodeExecutor.scriptExecHelper = helper
        def result = scriptNodeExecutor.executeCommand(context, command.toArray(new String[0]), node)

        then:
        result.resultCode == resultCode


        where:

        privateDataContext              | privateData
        [option: [password: 12344]]     | [password: 12344]
        null                            | null

    }


    class TestScriptNodeExecutor implements ScriptPluginProvider{
        File scriptFile
        File archiveFile
        File contentsBaseDir
        String name
        PluginMeta pluginMeta;
        Map<String, String> providerMeta
        Map<String, Object> metadata
        String service
        String scriptArgs;
        String scriptInterpreter

        @Override
        String getName() {
            return name
        }

        @Override
        String getService() {
            return service
        }

        @Override
        File getArchiveFile() {
            return archiveFile
        }

        @Override
        File getContentsBasedir() {
            return contentsBaseDir
        }

        @Override
        String getScriptArgs() {
            return scriptArgs
        }

        @Override
        String[] getScriptArgsArray() {
            return new String[0]
        }

        @Override
        File getScriptFile() {
            return scriptFile
        }

        @Override
        String getScriptInterpreter() {
            return scriptInterpreter
        }

        @Override
        boolean getInterpreterArgsQuoted() {
            return false
        }

        @Override
        Map<String, Object> getMetadata() {
            return metadata
        }

        @Override
        Map<String, String> getProviderMeta() {
            return providerMeta
        }

        @Override
        PluginMeta getPluginMeta() {
            return pluginMeta
        }

        @Override
        boolean getDefaultMergeEnvVars() {
            return false
        }
    }


}
