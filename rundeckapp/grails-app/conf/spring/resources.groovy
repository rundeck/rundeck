import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.app.internal.framework.FrameworkPropertyLookupFactory
import com.dtolabs.rundeck.app.internal.framework.RundeckFrameworkFactory
import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.authorization.AuthorizationFactory
import com.dtolabs.rundeck.core.common.FrameworkFactory
import com.dtolabs.rundeck.core.common.NodeSupport
import com.dtolabs.rundeck.core.plugins.FilePluginCache
import com.dtolabs.rundeck.core.plugins.JarPluginScanner
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.plugins.ScriptPluginScanner
import com.dtolabs.rundeck.core.storage.AuthRundeckStorageTree
import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.services.ExecutionFileStoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.PluggableStoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmImportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StorageConverterPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogReaderPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StreamingLogWriterPluginProviderService
import com.dtolabs.rundeck.server.plugins.storage.DbStoragePluginFactory
import com.dtolabs.rundeck.server.storage.StorageTreeFactory
import org.rundeck.web.infosec.ContainerPrincipalRoleSource
import org.rundeck.web.infosec.ContainerRoleSource
import org.rundeck.web.infosec.HMacSynchronizerTokensManager
import groovy.io.FileType
import org.rundeck.web.infosec.PreauthenticatedAttributeRoleSource
import org.springframework.core.task.SimpleAsyncTaskExecutor
import rundeck.services.PasswordFieldsService
import rundeck.services.scm.ScmJobImporter

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
    File varDir= new File(Constants.getBaseVar(rdeckBase))


    rundeckNodeSupport(NodeSupport){

    }

    frameworkPropertyLookupFactory(FrameworkPropertyLookupFactory){
        baseDir=rdeckBase
    }

    frameworkPropertyLookup(frameworkPropertyLookupFactory:'create'){

    }
    frameworkFilesystem(FrameworkFactory,rdeckBase){ bean->
        bean.factoryMethod='createFilesystemFramework'
    }
    filesystemProjectManager(FrameworkFactory,frameworkFilesystem,ref('nodeService')){ bean->
        bean.factoryMethod='createProjectManager'
    }

    frameworkFactory(RundeckFrameworkFactory){
        frameworkFilesystem=frameworkFilesystem
        propertyLookup=ref('frameworkPropertyLookup')
        type=application.config.rundeck?.projectsStorageType?:'filesystem'
        dbProjectManager=ref('projectManagerService')
        filesystemProjectManager=ref('filesystemProjectManager')
        pluginManagerService=ref('rundeckServerServiceProviderLoader')
    }

    rundeckFramework(frameworkFactory:'createFramework'){
    }

    def configDir = new File(Constants.getFrameworkConfigDir(rdeckBase))

    rundeckFilesystemPolicyAuthorization(AuthorizationFactory, configDir){bean->
        bean.factoryMethod='createFromDirectory'
    }

    //cache for provider loaders bound to a file
    providerFileCache(PluginManagerService) { bean ->
        bean.factoryMethod = 'createProviderLoaderFileCache'
    }

    //scan for jar plugins
    jarPluginScanner(JarPluginScanner, pluginDir, cacheDir, ref('providerFileCache'), 5000)

    //scan for script-based plugins
    scriptPluginScanner(ScriptPluginScanner, pluginDir, cacheDir, ref('providerFileCache'), 5000)

    //cache for plugins loaded via scanners
    filePluginCache(FilePluginCache, ref('providerFileCache')) {
        scanners = [
                ref('jarPluginScanner'),
                ref('scriptPluginScanner')
        ]
    }

    /*
     * Define beans for Rundeck core-style plugin loader to load plugins from jar/zip files
     */
    rundeckServerServiceProviderLoader(PluginManagerService) {
        extdir = pluginDir
        cachedir = cacheDir
        cache = filePluginCache
    }

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
    nodeTaskExecutor(SimpleAsyncTaskExecutor,"NodeService-SourceLoader") {
        concurrencyLimit = (application.config.rundeck?.nodeService?.concurrencyLimit ?: 25) //-1 for unbounded
    }
    //alternately use ThreadPoolTaskExecutor ...
//    nodeTaskExecutor(ThreadPoolTaskExecutor) {
//        threadNamePrefix="NodeService-SourceLoader"
//        corePoolSize= (application.config.rundeck?.nodeService?.corePoolSize ?: 5)
//        maxPoolSize= (application.config.rundeck?.nodeService?.maxPoolSize ?: 40)
//    }

    pluggableStoragePluginProviderService(PluggableStoragePluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }
    storagePluginProviderService(StoragePluginProviderService,rundeckFramework) {
        pluggableStoragePluginProviderService = ref('pluggableStoragePluginProviderService')
    }

    storageConverterPluginProviderService(StorageConverterPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    scmExportPluginProviderService(ScmExportPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    scmImportPluginProviderService(ScmImportPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    scmJobImporter(ScmJobImporter)

    containerPrincipalRoleSource(ContainerPrincipalRoleSource){
        enabled=grailsApplication.config.rundeck?.security?.authorization?.containerPrincipal?.enabled in [true,'true']
    }
    containerRoleSource(ContainerRoleSource){
        enabled=grailsApplication.config.rundeck?.security?.authorization?.container?.enabled in [true,'true']
    }
    preauthenticatedAttributeRoleSource(PreauthenticatedAttributeRoleSource){
        enabled=grailsApplication.config.rundeck?.security?.authorization?.preauthenticated?.enabled in [true,'true']
        attributeName=grailsApplication.config.rundeck?.security?.authorization?.preauthenticated?.attributeName
        delimiter=grailsApplication.config.rundeck?.security?.authorization?.preauthenticated?.delimiter
    }

    def storageDir= new File(varDir, 'storage')
    rundeckStorageTree(StorageTreeFactory){
        rundeckFramework=ref('rundeckFramework')
        pluginRegistry=ref("rundeckPluginRegistry")
        storagePluginProviderService=ref('storagePluginProviderService')
        storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
        configuration = application.config.rundeck?.storage?.flatten()
        storageConfigPrefix='provider'
        converterConfigPrefix='converter'
        baseStorageType='file'
        baseStorageConfig=['baseDir':storageDir.getAbsolutePath()]
        defaultConverters=['StorageTimestamperConverter','KeyStorageLayer']
        loggerName='org.rundeck.storage.events'
    }
    authRundeckStorageTree(AuthRundeckStorageTree, rundeckStorageTree)

    rundeckConfigStorageTree(StorageTreeFactory){
        frameworkPropertyLookup=ref('frameworkPropertyLookup')
        pluginRegistry=ref("rundeckPluginRegistry")
        storagePluginProviderService=ref('storagePluginProviderService')
        storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
        configuration = application.config.rundeck?.config?.storage?.flatten()
        storageConfigPrefix='provider'
        converterConfigPrefix='converter'
        baseStorageType='db'
        baseStorageConfig=[namespace:'config']
        defaultConverters=['StorageTimestamperConverter']
        loggerName='org.rundeck.config.storage.events'
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
    dbStoragePluginFactory(DbStoragePluginFactory)
    pluginRegistry['db']='dbStoragePluginFactory'
    /**
     * Registry bean contains both kinds of plugin
     */
    rundeckPluginRegistry(RundeckPluginRegistry){
        pluginRegistryMap=pluginRegistry
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
        pluginDirectory=pluginDir
        pluginCacheDirectory=cacheDir
    }
    hMacSynchronizerTokensManager(HMacSynchronizerTokensManager){

    }

    /**
     * Track passwords on these plugins
     */
    resourcesPasswordFieldsService(PasswordFieldsService)
    execPasswordFieldsService(PasswordFieldsService)
    fcopyPasswordFieldsService(PasswordFieldsService)


    /// XML/JSON custom marshaller support

    apiMarshallerRegistrar(ApiMarshallerRegistrar)
}
