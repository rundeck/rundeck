package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import spock.lang.Specification
import spock.lang.Unroll

class AbstractDescribableScriptPluginSpec extends Specification{

    public static final String PROJECT_NAME = 'BaseScriptPluginSpec'
    Framework framework
    FrameworkProject testProject
    def instanceData
    def localDataContext
    def pluginMetaData


    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
        instanceData =  [
                'password' : 'key/path/some.pass',
                'example' : 'this is an example',
                'debug' : 'true'
        ]

        localDataContext = [
                config   : []
        ]

        pluginMetaData = [
                config                                      :
                        [
                                [
                                        type            : 'String',
                                        name            : 'password',
                                        title           : 'Password',
                                        required        : true,
                                        scope           : PropertyScope.Instance
                                ],
                                [
                                        type            : 'String',
                                        name            : 'example',
                                        scope           : PropertyScope.Instance
                                ],
                                [
                                        type            : 'String',
                                        name            : 'debug',
                                        scope           : PropertyScope.Instance
                                ],
                        ]
        ]
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    class TestScriptPlugin extends AbstractDescribableScriptPlugin {

        protected TestScriptPlugin(ScriptPluginProvider provider, Framework framework) {
            super(provider, framework);
        }

        @Override
        boolean isAllowCustomProperties() {
            return true
        }

        @Override
        boolean isUseConventionalPropertiesMapping() {
            return true
        }

        def getConfigData(ExecutionContext context, Map<String, Object> instaceData, Map<String, Map<String, String>> localDataContext,String serviceName){
            DescriptionBuilder builder = DescriptionBuilder.builder()
            createDescription(provider, isAllowCustomProperties(), isUseConventionalPropertiesMapping(), builder)
            def description = builder.build()
            return loadConfigData(context, instaceData, localDataContext, description, serviceName)
        }

        def getPluginProperties(ExecutionContext context, Map<String, Object> instanceData, Map<String, Map<String, String>> localDataContext,String serviceName){
            DescriptionBuilder builder = DescriptionBuilder.builder()
            createDescription(provider, isAllowCustomProperties(), isUseConventionalPropertiesMapping(), builder)
            return builder.build()
        }
    }

    @Unroll
    def "check envs variables for plugin 1.x"() {
        given:
        File tempFile = File.createTempFile("test", "zip")
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir")
        basedir.deleteOnExit()

        when:

        def pluginMeta = Mock(PluginMeta){
            getRundeckPluginVersion() >> "1.2"
        }
        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getMetadata() >> pluginMetaData
            getPluginMeta() >> pluginMeta
            getContentsBasedir() >> basedir
            getService() >> ServiceNameConstants.NodeExecutor
        }

        ExecutionContext context = Mock(ExecutionContext) {
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            getDataContext() >> new BaseDataContext(localDataContext)
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)
        def result = plugin.getConfigData(context, instanceData,localDataContext, ServiceNameConstants.NodeExecutor)

        then:

        result!=null
        result==[config:[debug:"true", example:"this is an example",password : "key/path/some.pass"]]

    }

    @Unroll
    def "check envs variables for plugin 2.x"() {
        given:
        File tempFile = File.createTempFile("test", "zip")
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir")
        basedir.deleteOnExit()

        when:

        def pluginMeta = Mock(PluginMeta){
            getRundeckPluginVersion() >> "2.0"
        }
        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'test1'
            getMetadata() >> pluginMetaData
            getPluginMeta() >> pluginMeta
            getContentsBasedir() >> basedir
        }

        ExecutionContext context = Mock(ExecutionContext) {
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)
        def result = plugin.getConfigData(context, instanceData,localDataContext, ServiceNameConstants.NodeExecutor)

        then:

        result!=null
        result==[config:[debug:"true", example:"this is an example",password : "key/path/some.pass"],nodeexecutor:[debug:"true", example:"this is an example",password : "key/path/some.pass"]]

    }

    @Unroll
    def "check description contains blankIfUnexpanded field"() {
        given:
        File tempFile = File.createTempFile("test", "zip")
        tempFile.deleteOnExit()
        File basedir = File.createTempFile("test", "dir")
        basedir.deleteOnExit()

        when:

        def pluginMeta = Mock(PluginMeta){
            getRundeckPluginVersion() >> "2.0"
        }
        ScriptPluginProvider provider = Mock(ScriptPluginProvider) {
            getName() >> 'testProperties'
            getMetadata() >> pluginMetaData
            getPluginMeta() >> pluginMeta
            getContentsBasedir() >> basedir
        }

        ExecutionContext context = Mock(ExecutionContext) {
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            getDataContext() >> new BaseDataContext([:])
            getLogger() >> Mock(PluginLogger)
            getExecutionContext() >> Mock(ExecutionContext) {
                getFramework() >> framework
                getFrameworkProject() >> PROJECT_NAME
            }
        }

        TestScriptPlugin plugin = new TestScriptPlugin(provider, framework)
        def result = plugin.getPluginProperties(context, instanceData,localDataContext, ServiceNameConstants.NodeExecutor)

        then:

        result!=null
        result.properties.get(0).hasProperty("blankIfUnexpandable")

    }
}
