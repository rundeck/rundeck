import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.services.LogFileStoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogReaderPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogWriterPluginProviderService
import groovy.io.FileType
import org.springframework.core.task.SimpleAsyncTaskExecutor

beans={
    defaultGrailsServiceInjectorJobListener(GrailsServiceInjectorJobListener){
        name= 'defaultGrailsServiceInjectorJobListener'
        services=[executionService: ref('executionService')]
        quartzScheduler=ref('quartzScheduler')
    }
    def rdeckBase
    if (!application.config.rdeck.base) {
        //look for system property
        rdeckBase = System.getProperty('rdeck.base')
    } else {
        rdeckBase = application.config.rdeck.base
    }
    if(!rdeckBase){
        System.err.println("rdeck.base was not defined in application config or as a system property")
        return
    }
    def serverLibextDir = application.config.rundeck?.server?.plugins?.dir?:"${rdeckBase}/libext"
    File pluginDir = new File(serverLibextDir)
    def serverLibextCacheDir = application.config.rundeck?.server?.plugins?.cacheDir?:"${serverLibextDir}/cache"
    File cacheDir= new File(serverLibextCacheDir)

    /*
     * Define beans for Rundeck core-style plugin loader to load plugins from jar/zip files
     */
    rundeckServerServiceProviderLoader(PluginManagerService, pluginDir, cacheDir)

    /**
     * the Notification plugin provider service
     */
    notificationPluginProviderService(NotificationPluginProviderService){
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
    }
    /**
     * the StreamingLogReader plugin provider service
     */
    streamingLogReaderPluginProviderService(StreamingLogReaderPluginProviderService){
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
    }
    /**
     * the StreamingLogReader plugin provider service
     */
    streamingLogWriterPluginProviderService(StreamingLogWriterPluginProviderService){
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
    }
    /**
     * the LogFileStorage plugin provider service
     */
    logFileStoragePluginProviderService(LogFileStoragePluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }
    logFileTaskExecutor(SimpleAsyncTaskExecutor,"LogFileStorage"){

    }

    /**
     * Define groovy-based plugins as Spring beans, registered in a hash map
     */
    pluginCustomizer(PluginCustomizer)
    xmlns lang: 'http://www.springframework.org/schema/lang'

    def pluginRegistry=[:]
    if (pluginDir.exists()) {
        pluginDir.eachFileMatch(FileType.FILES, ~/.*\.groovy/) { File plugin ->
            String beanName = plugin.name.replace('.groovy', '')
            lang.groovy(id: beanName, 'script-source': "file:${pluginDir}/${plugin.name}",
                    'refresh-check-delay': application.config.plugin.refreshDelay ?: -1,
                    'customizer-ref':'pluginCustomizer'
            )
            pluginRegistry[beanName]=beanName
        }
    }

    /**
     * Registry bean contains both kinds of plugin
     */
    rundeckPluginRegistry(RundeckPluginRegistry){
        pluginRegistryMap=pluginRegistry
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
        pluginDirectory=pluginDir
        pluginCacheDirectory=cacheDir
    }
}
