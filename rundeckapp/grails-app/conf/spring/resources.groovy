import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.notification.TestNotificationPlugin
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import groovy.io.FileType
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

beans={
    defaultGrailsServiceInjectorJobListener(GrailsServiceInjectorJobListener){
        name= 'defaultGrailsServiceInjectorJobListener'
        services=[executionService: ref('executionService')]
        quartzScheduler=ref('quartzScheduler')
    }

    def serverLibextDir = application.config.rundeck?.server?.plugins?.dir?:"${application.config.rdeck.base}/libext/server"
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

    //dev test
    test2NotificationPlugin(TestNotificationPlugin)
    pluginRegistry['test2NotificationPlugin']='test2NotificationPlugin'

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
