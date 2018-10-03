package verb

import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.verb.client.repository.RundeckRepositoryManager
import com.rundeck.verb.client.repository.VerbRepositoryFactory
import com.rundeck.verb.client.repository.tree.NamedTreeProvider
import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import grails.plugins.*

class VerbGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Verb" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/verb"

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
            if(application.config.rundeck.features.verb.enabled == "true") {
                def rdeckBase = System.getProperty('rdeck.base')

                ensureRequiredFilesExist(rdeckBase)

                //        //Verb storage tree for installed plugins
                verbPluginStorageTreeFactory(StorageTreeFactory) {
                    frameworkPropertyLookup=ref('frameworkPropertyLookup')
                    pluginRegistry=ref("rundeckPluginRegistry")
                    storagePluginProviderService=ref('storagePluginProviderService')
                    storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
                    configuration = application.config.rundeck?.verb?.plugin?.toFlatConfig()
                    storageConfigPrefix='provider'
                    converterConfigPrefix='converter'
                    baseStorageType='file'
                    baseStorageConfig=['baseDir':rdeckBase+"/verb/installedPlugins"]
                    defaultConverters=['StorageTimestamperConverter']
                    loggerName='org.rundeck.verb.plugins.storage.events'
                }
                verbPluginStorageTree(verbPluginStorageTreeFactory:"createTree")

                //Verb storage tree for private repository
                verbRepositoryStorageTreeFactory(StorageTreeFactory) {
                    frameworkPropertyLookup=ref('frameworkPropertyLookup')
                    pluginRegistry=ref("rundeckPluginRegistry")
                    storagePluginProviderService=ref('storagePluginProviderService')
                    storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
                    configuration = application.config.rundeck?.verb?.repository?.toFlatConfig()
                    storageConfigPrefix='provider'
                    converterConfigPrefix='converter'
                    baseStorageType='file'
                    baseStorageConfig=['baseDir':rdeckBase+"/verb/repo"]
                    defaultConverters=['StorageTimestamperConverter']
                    loggerName='org.rundeck.verb.repository.storage.events'
                }
                verbRepositoryStorageTree(verbRepositoryStorageTreeFactory:"createTree")

                def serverLibextDir = application.config.rundeck?.server?.plugins?.dir ?: "${rdeckBase}/libext"
                File pluginDir = new File(serverLibextDir)

                //Verb
                verbArtifactInstaller(StorageTreeArtifactInstaller, ref('verbPluginStorageTree'))
                repositoryPluginService(RepositoryPluginService) {
                    localFilesystemPluginDir = pluginDir
                    installedPluginTree = ref('verbPluginStorageTree')
                }
                verbStorageTreeRepositoryProvider(NamedTreeProvider) {
                    treeRegistry = ["private": ref('verbRepositoryStorageTree')]
                }
                repositoryFactory(VerbRepositoryFactory) {
                    treeProvider = ref("verbStorageTreeRepositoryProvider")
                }
                String repoDefnUrl = grailsApplication.config.rundeck.verb.repositoryDefinitionUrl ?:
                                     "file:" + System.getProperty("rundeck.server.configDir") + "/verb-repositories.yaml"
                repositoryManager(RundeckRepositoryManager, ref('repositoryFactory')) {
                    repositoryDefinitionListDatasourceUrl = repoDefnUrl
                }
                verbClient(RundeckVerbClient) {
                    artifactInstaller = ref('verbArtifactInstaller')
                    repositoryManager = ref('repositoryManager')
                }
            }

        }
    }

    def ensureRequiredFilesExist(final String rundeckBase) {
        //verb requirments for default config
        File verbDir = new File(rundeckBase+"/verb")
        if(!verbDir.exists()) {
            if(!verbDir.mkdir()) throw new Exception("Unable to create directory required for Rundeck Verb. Please ensure the file: ${verbDir.absolutePath} exists and that Rundeck has write access to the directory.")
            File installationLoc = new File(verbDir,"installedPlugins")
            installationLoc.mkdir()
            File repoLoc = new File(verbDir,"repo")
            repoLoc.mkdir()
        }
        File repoDefnFile = new File(System.getProperty("rundeck.server.configDir") +"/verb-repositories.yaml")
        if(!repoDefnFile.exists()) {
            repoDefnFile << getClass().getClassLoader().getResourceAsStream("default-verb-repo-defn.yaml")
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
