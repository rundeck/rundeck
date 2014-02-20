import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.services.ExecutionFileStoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ResourceConverterPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ResourceStoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogReaderPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogWriterPluginProviderService
import com.dtolabs.rundeck.server.resourcetree.ResourceTreeFactory
import groovy.io.FileType
import org.springframework.core.task.SimpleAsyncTaskExecutor
import us.vario.greg.lct.data.file.DirectFilepathMapper
import us.vario.greg.lct.data.file.JsonMetadataMapper

beans={
    log4jConfigurer(org.springframework.beans.factory.config.MethodInvokingFactoryBean) {
        targetClass = "org.springframework.util.Log4jConfigurer"
        targetMethod = "initLogging"
        arguments = ["classpath:log4j.properties"]
    }
    defaultGrailsServiceInjectorJobListener(GrailsServiceInjectorJobListener){
        name= 'defaultGrailsServiceInjectorJobListener'
        services=[grailsApplication: ref('grailsApplication'),
                executionService: ref('executionService'),
                frameworkService: ref('frameworkService'),
                metricRegistry:ref('metricRegistry'),
                executionUtilService:ref('executionUtilService')]
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
     * Define the single Framework instance to use
     */
    rundeckFramework(Framework, rdeckBase){bean->
        bean.factoryMethod='getInstanceWithoutProjectsDir'
    }
    def configDir = new File(Constants.getFrameworkConfigDir(rdeckBase))
    rundeckPolicyAuthorization(SAREAuthorization, configDir){

    }
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
     * the LogFileStorage plugin provider service (rundeck v2.0+)
     */
    executionFileStoragePluginProviderService(ExecutionFileStoragePluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }
    logFileTaskExecutor(SimpleAsyncTaskExecutor,"LogFileStorage"){
        concurrencyLimit= 2 + (application.config.rundeck?.execution?.logs?.fileStorage?.concurrencyLimit ?: 5)
    }

    resourceStoragePluginProviderService(ResourceStoragePluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    resourceConverterPluginProviderService(ResourceConverterPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

//    File resDir = new File(rdeckBase,"var/resources")
//    filetreeMapper(DirectFilepathMapper,resDir)
//    jsonmetadataMapper(JsonMetadataMapper)
//    rundeckResourceTree(FileTree,filetreeMapper,jsonmetadataMapper)

    rundeckResourceTree(ResourceTreeFactory){
        rundeckFramework=ref('rundeckFramework')
        //XXX: use pluginRegistry?
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
        pluginRegistry=ref("rundeckPluginRegistry")
        serverConfig=application.config
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
