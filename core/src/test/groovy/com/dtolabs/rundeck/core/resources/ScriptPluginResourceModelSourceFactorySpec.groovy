package com.dtolabs.rundeck.core.resources

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageAuthorizationException
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import org.rundeck.app.spi.Services
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import spock.lang.Specification

class ScriptPluginResourceModelSourceFactorySpec  extends Specification{
    public static final String PROJECT_NAME = 'ScriptPluginResourceModelSourceFactorySpec'

    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def "test access key storage"(){

        given:

        def storageTree = Mock(KeyStorageTree)
        Services services = Mock(Services)

        Properties configuration = new Properties()
        configuration.put("project",PROJECT_NAME)
        configuration.put("token","keys/token")

        TestScriptResourceModel resourceModelProvider = new TestScriptResourceModel()
        resourceModelProvider.name = "test-script-resource-model"
        resourceModelProvider.service = "ResourceModel"
        resourceModelProvider.metadata = ["resource-format":"yaml", "config": [
                [
                        "name":"token",
                        "type":"String",
                        "title":"Token",
                        "renderingOptions": [
                                "selectionAccessor" : "STORAGE_PATH",
                                "storage-path-root" : "keys",
                                "valueConversion" : "STORAGE_PATH_AUTOMATIC_READ"
                        ]
                ]
                ]
        ]

        File archiveFile = File.createTempFile("test-script-resource-model", "tmp");
        archiveFile.deleteOnExit();
        resourceModelProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        resourceModelProvider.setScriptFile(scriptFile);
        final File baseDir = File.createTempFile("test-basedir", "tmp");
        baseDir.deleteOnExit();
        resourceModelProvider.contentsBaseDir = baseDir
        resourceModelProvider.scriptArgs = ""

        List<Map<String, Object>> providers = [
                [
                        "name":"test-script-resource-model",
                        "title":"test"
                ]
        ]
        PluginMeta pluginMeta = new PluginMeta()
        pluginMeta.setRundeckPluginVersion("1.2")
        pluginMeta.setProviders(providers)
        resourceModelProvider.pluginMeta = pluginMeta

        ScriptPluginResourceModelSourceFactory pluginFactory = new ScriptPluginResourceModelSourceFactory(resourceModelProvider, framework)

        when:
        def resource = pluginFactory.createResourceModelSource(services, configuration)

        then:
        1 * services.getService(KeyStorageTree.class) >> storageTree
        storageTree.getResource(_) >> Mock(Resource) {
            1* getContents() >> Mock(ResourceMeta) {
                writeContent(_) >> { args ->
                    args[0].write('password'.bytes)
                    return 6L
                }
            }
        }

        resource !=null
        resource.configuration.getProperty("token") != null
        resource.configuration.getProperty("token") == "password"
    }


    def "test access key storage without access"(){

        given:

        def storageTree = Mock(KeyStorageTree){
            getResource(_) >> {throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.READ, new PathUtil.PathImpl("test"))}
        }

        Services services = Mock(Services){
            getService(KeyStorageTree.class) >> storageTree
        }

        Properties configuration = new Properties()
        configuration.put("project",PROJECT_NAME)
        configuration.put("token","keys/token")

        TestScriptResourceModel resourceModelProvider = new TestScriptResourceModel()
        resourceModelProvider.name = "test-script-resource-model"
        resourceModelProvider.service = "ResourceModel"
        resourceModelProvider.metadata = ["resource-format":"yaml", "config": [
                [
                        "name":"token",
                        "type":"String",
                        "title":"Token",
                        "renderingOptions": [
                                "selectionAccessor" : "STORAGE_PATH",
                                "storage-path-root" : "keys",
                                "valueConversion" : "STORAGE_PATH_AUTOMATIC_READ"
                        ]
                ]
        ]
        ]

        File archiveFile = File.createTempFile("test-script-resource-model", "tmp");
        archiveFile.deleteOnExit();
        resourceModelProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        resourceModelProvider.setScriptFile(scriptFile);
        final File baseDir = File.createTempFile("test-basedir", "tmp");
        baseDir.deleteOnExit();
        resourceModelProvider.contentsBaseDir = baseDir
        resourceModelProvider.scriptArgs = ""

        List<Map<String, Object>> providers = [
                [
                        "name":"test-script-resource-model",
                        "title":"test",
                ]
        ]
        PluginMeta pluginMeta = new PluginMeta()
        pluginMeta.setRundeckPluginVersion("1.2")
        pluginMeta.setProviders(providers)
        resourceModelProvider.pluginMeta = pluginMeta

        ScriptPluginResourceModelSourceFactory pluginFactory = new ScriptPluginResourceModelSourceFactory(resourceModelProvider, framework)

        when:
        def resource = pluginFactory.createResourceModelSource(services, configuration)

        then:
        ConfigurationException e = thrown()
        e.message == "Unable to load configuration key 'token' value from storage path:  keys/token"

    }

    def "disable key conversion"(){

        given:

        def storageTree = Mock(KeyStorageTree){
            getResource(_) >> {throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.READ, new PathUtil.PathImpl("test"))}
        }

        Services services = Mock(Services){
            getService(KeyStorageTree.class) >> storageTree
        }

        Properties configuration = new Properties()
        configuration.put("project",PROJECT_NAME)
        configuration.put("token","keys/token")
        configuration.put(ScriptPluginResourceModelSourceFactory.DISABLE_CONTENT_CONVERSION, true)

        TestScriptResourceModel resourceModelProvider = new TestScriptResourceModel()
        resourceModelProvider.name = "test-script-resource-model"
        resourceModelProvider.service = "ResourceModel"
        resourceModelProvider.metadata = ["resource-format":"yaml", "config": [
                [
                        "name":"token",
                        "type":"String",
                        "title":"Token",
                        "renderingOptions": [
                                "selectionAccessor" : "STORAGE_PATH",
                                "storage-path-root" : "keys",
                                "valueConversion" : "STORAGE_PATH_AUTOMATIC_READ"
                        ]
                ]
        ]
        ]

        File archiveFile = File.createTempFile("test-script-resource-model", "tmp");
        archiveFile.deleteOnExit();
        resourceModelProvider.setArchiveFile(archiveFile);
        final File scriptFile = File.createTempFile("test-scriptfile", "tmp");
        scriptFile.deleteOnExit();
        resourceModelProvider.setScriptFile(scriptFile);
        final File baseDir = File.createTempFile("test-basedir", "tmp");
        baseDir.deleteOnExit();
        resourceModelProvider.contentsBaseDir = baseDir
        resourceModelProvider.scriptArgs = ""

        List<Map<String, Object>> providers = [
                [
                        "name":"test-script-resource-model",
                        "title":"test",
                ]
        ]
        PluginMeta pluginMeta = new PluginMeta()
        pluginMeta.setRundeckPluginVersion("1.2")
        pluginMeta.setProviders(providers)
        resourceModelProvider.pluginMeta = pluginMeta

        ScriptPluginResourceModelSourceFactory pluginFactory = new ScriptPluginResourceModelSourceFactory(resourceModelProvider, framework)

        when:
        def resource = pluginFactory.createResourceModelSource(services, configuration)

        then:
        resource !=null
        resource.configuration.getProperty("token") != null
        resource.configuration.getProperty("token") == "keys/token"

    }

    class TestScriptResourceModel implements ScriptPluginProvider{
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
