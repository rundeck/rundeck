package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.ScriptExecHelper
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/11/16.
 */
class BaseScriptPluginSpec extends Specification {
    Framework framework
    FrameworkProject testProject
    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject=framework.getFrameworkProjectMgr().createFrameworkProject('BaseScriptPluginSpec')
    }
    def teardown(){
        framework.getFrameworkProjectMgr().removeFrameworkProject('BaseScriptPluginSpec')
    }

    class TestScriptPlugin extends BaseScriptPlugin {

        protected TestScriptPlugin(ScriptPluginProvider provider, Framework framework) {
            super(provider, framework);
        }

        @Override
        public boolean isAllowCustomProperties() {
            return false;
        }

        @Override
        public String[] createScriptArgs(
                Map<String, Map<String, String>> localDataContext) {
            return super.createScriptArgs(localDataContext);
        }

        @Override
        public ExecArgList createScriptArgsList(
                Map<String, Map<String, String>> dataContext) {
            return super.createScriptArgsList(dataContext);
        }
    }
    @Unroll
    def "merge env vars true"(){
        given:
        def pluginMeta = [(BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): setVal]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getDefaultMergeEnvVars() >> defVal
            getMetadata() >> pluginMeta
            getArchiveFile() >> tempFile
            getScriptFile() >> tempFile
            getContentsBasedir() >> basedir
        }
        String[] emptyArray=[]

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> 'BaseScriptPluginSpec'
            getDataContext()>>[:]
            getLogger()>>Mock(PluginLogger)
        }

        when:
        def result = plugin.runPluginScript(context, null, null, framework, [:])


        then:
        1 * plugin.scriptExecHelper.loadLocalEnvironment() >> [bloo: 'blah']
        1 * plugin.scriptExecHelper.runLocalCommand(_,{
            it.bloo=='blah'
        },_,null,null)>>0

        where:
        setVal | defVal
        true   | false
        null   | true
    }
    def "merge env vars false"(){
        given:
        def pluginMeta = [(BaseScriptPlugin.SETTING_MERGE_ENVIRONMENT): setVal]
        File tempFile = File.createTempFile("test", "zip");
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir");
        basedir.deleteOnExit()

        ScriptPluginProvider provider = Mock(ScriptPluginProvider){
            getDefaultMergeEnvVars()>>defVal
            getMetadata()>>pluginMeta
            getArchiveFile()>>tempFile
            getScriptFile()>>tempFile
            getContentsBasedir()>>basedir
        }
        String[] emptyArray=[]

        ScriptExecHelper helper = Mock(ScriptExecHelper) {
            1 * createScriptArgs(_, null, _, null, false, _) >> emptyArray
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)

        plugin.scriptExecHelper = helper
        PluginStepContext context = Mock(PluginStepContext) {
            getFrameworkProject() >> 'BaseScriptPluginSpec'
            getDataContext()>>[:]
            getLogger()>>Mock(PluginLogger)
        }

        when:
        def result = plugin.runPluginScript(context, null, null, framework, [:])


        then:
        0 * plugin.scriptExecHelper.loadLocalEnvironment()
        1 * plugin.scriptExecHelper.runLocalCommand(_,{
            it.bloo==null
        },_,null,null)>>0


        where:
        setVal | defVal
        false  | true
        false  | false
        null   | false
    }
}
