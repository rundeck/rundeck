package repository

import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import com.rundeck.repository.client.RundeckRepositoryClient
import com.rundeck.repository.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.repository.client.repository.RundeckRepositoryFactory
import com.rundeck.repository.client.repository.RundeckRepositoryManager
import com.rundeck.repository.client.repository.tree.NamedTreeProvider
import grails.plugins.*

class RepositoryGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "4.0.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Repository" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://rundeck.org/plugin/repository"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->
            if(application.config.getProperty("rundeck.feature.repository.enabled", Boolean.class, false)) {
                def rdeckBase = System.getProperty('rdeck.base')

                ensureRequiredFilesExist(rdeckBase)

                //storage tree for repository installed plugins
                repoPluginStorageTreeFactory(StorageTreeFactory) {
                    frameworkPropertyLookup=ref('frameworkPropertyLookup')
                    pluginRegistry=ref("rundeckPluginRegistry")
                    storagePluginProviderService=ref('storagePluginProviderService')
                    storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
                    configuration = application.config.rundeck?.repository?.plugins?.toFlatConfig()
                    storageConfigPrefix='provider'
                    converterConfigPrefix='converter'
                    baseStorageType='file'
                    baseStorageConfig=['baseDir':rdeckBase+"/repository/installedPlugins"]
                    defaultConverters=['StorageTimestamperConverter']
                    loggerName='org.rundeck.repository.plugins.storage.events'
                }
                repoPluginStorageTree(repoPluginStorageTreeFactory:"createTree")

                //storage tree for private repository artifacts
                repositoryStorageTreeFactory(StorageTreeFactory) {
                    frameworkPropertyLookup=ref('frameworkPropertyLookup')
                    pluginRegistry=ref("rundeckPluginRegistry")
                    storagePluginProviderService=ref('storagePluginProviderService')
                    storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
                    configuration = application.config.rundeck?.repository?.artifacts?.toFlatConfig()
                    storageConfigPrefix='provider'
                    converterConfigPrefix='converter'
                    baseStorageType='file'
                    baseStorageConfig=['baseDir':rdeckBase+"/repository/artifacts"]
                    defaultConverters=['StorageTimestamperConverter']
                    loggerName='org.rundeck.repository.storage.events'
                }
                repositoryStorageTree(repositoryStorageTreeFactory:"createTree")

                def serverLibextDir = application.config.getProperty("rundeck.server.plugins.dir", String.class, "${rdeckBase}/libext")
                File pluginDir = new File(serverLibextDir)

                String installedPluginStorageTreePath = "/"
                if(!application.config.getProperty("rundeck.feature.repository.installedPlugins.storageTreePath", String.class, "").isEmpty()) {
                    installedPluginStorageTreePath = application.config.getProperty("rundeck.feature.repository.installedPlugins.storageTreePath", String.class)
                }
                //Repository
                repoArtifactInstaller(StorageTreeArtifactInstaller, ref('repoPluginStorageTree'),installedPluginStorageTreePath)
                repositoryPluginService(RepositoryPluginService) {
                    localFilesystemPluginDir = pluginDir
                    storageTreePath = installedPluginStorageTreePath
                    installedPluginTree = ref('repoPluginStorageTree')
                }
                repositoryFactory(RundeckRepositoryFactory) {
                    repositoryStorageTree = ref('repositoryStorageTree')
                }

                String defaultValue = "file:" + System.getProperty("rundeck.server.configDir") + "/artifact-repositories.yaml"
                String repoDefnUrl = grailsApplication.config.getProperty("rundeck.repository.repositoryDefinitionUrl",String.class, defaultValue)

                repositoryManager(RundeckRepositoryManager, ref('repositoryFactory')) {
                    repositoryDefinitionListDatasourceUrl = repoDefnUrl
                }
                repoClient(RundeckRepositoryClient) {
                    artifactInstaller = ref('repoArtifactInstaller')
                    repositoryManager = ref('repositoryManager')
                }
            }

        }
    }

    def ensureRequiredFilesExist(final String rundeckBase) {
        //repository requirements for default config
        File repoDir = new File(rundeckBase+"/repository")
        if(!repoDir.exists()) {
            if(!repoDir.mkdir()) throw new Exception("Unable to create directory required for Rundeck repository. Please ensure the file: ${repoDir.absolutePath} exists and that Rundeck has write access to the directory.")
            File installationLoc = new File(repoDir,"installedPlugins")
            installationLoc.mkdir()
            File repoLoc = new File(repoDir,"artifacts")
            repoLoc.mkdir()
        }
        File repoDefnFile = new File(System.getProperty("rundeck.server.configDir") +"/artifact-repositories.yaml")
        if(!repoDefnFile.exists()) {
            repoDefnFile << getClass().getClassLoader().getResourceAsStream("default-repo-defn.yaml")
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
