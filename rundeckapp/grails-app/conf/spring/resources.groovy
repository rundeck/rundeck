/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.app.gui.GroupedJobListLinkHandler
import com.dtolabs.rundeck.app.gui.JobListLinkHandlerRegistry
import com.dtolabs.rundeck.app.gui.SystemConfigMenuItem
import com.dtolabs.rundeck.app.gui.SystemReportMenuItem
import com.dtolabs.rundeck.app.gui.UserSummaryMenuItem
import com.dtolabs.rundeck.app.internal.framework.ConfigFrameworkPropertyLookupFactory
import com.dtolabs.rundeck.app.config.RundeckConfig
import com.dtolabs.rundeck.app.internal.framework.FrameworkPropertyLookupFactory
import com.dtolabs.rundeck.app.internal.framework.RundeckFilesystemProjectImporter
import com.dtolabs.rundeck.app.internal.framework.RundeckFrameworkFactory
import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.authorization.AclsUtil
import com.dtolabs.rundeck.core.authorization.Log4jAuthorizationLogger
import com.dtolabs.rundeck.core.authorization.providers.BaseValidatorImpl
import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory
import com.dtolabs.rundeck.core.authorization.providers.YamlValidator
import com.dtolabs.rundeck.core.cluster.ClusterInfoService
import com.dtolabs.rundeck.core.common.FrameworkFactory
import com.dtolabs.rundeck.core.common.NodeSupport
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileManagerService
import com.dtolabs.rundeck.core.plugins.FilePluginCache
import com.dtolabs.rundeck.core.plugins.JarPluginScanner
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.plugins.ScriptPluginScanner
import com.dtolabs.rundeck.core.plugins.WatchingPluginDirProvider
import com.dtolabs.rundeck.core.resources.format.ResourceFormats
import com.dtolabs.rundeck.core.storage.AuthRundeckStorageTree
import com.dtolabs.rundeck.core.storage.KeyStorageContextProvider
import com.dtolabs.rundeck.core.storage.ProjectKeyStorageContextProvider
import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import com.dtolabs.rundeck.core.storage.TreeStorageManager
import com.dtolabs.rundeck.core.utils.GrailsServiceInjectorJobListener
import com.dtolabs.rundeck.core.utils.RequestAwareLinkGenerator
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.RundeckEmbeddedPluginExtractor
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.fileupload.FSFileUploadPlugin
import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import com.dtolabs.rundeck.server.plugins.logging.*
import com.dtolabs.rundeck.server.plugins.logs.*
import com.dtolabs.rundeck.server.plugins.logstorage.TreeExecutionFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.logstorage.TreeExecutionFileStoragePluginFactory
import com.dtolabs.rundeck.server.plugins.notification.DummyEmailNotificationPlugin
import com.dtolabs.rundeck.server.plugins.notification.DummyWebhookNotificationPlugin
import com.dtolabs.rundeck.server.plugins.services.*
import com.dtolabs.rundeck.server.plugins.storage.DbStoragePlugin
import com.dtolabs.rundeck.server.plugins.storage.DbStoragePluginFactory
import com.dtolabs.rundeck.server.AuthContextEvaluatorCacheManager
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import groovy.io.FileType
import org.rundeck.app.AppRestarter
import org.rundeck.app.api.ApiInfo
import org.rundeck.app.authorization.BaseAuthContextEvaluator
import org.rundeck.app.authorization.BaseAuthContextProcessor
import org.rundeck.app.authorization.BaseAuthContextProvider
import org.rundeck.app.authorization.ContextACLStorageFileManagerFactory
import org.rundeck.app.authorization.RundeckAuthorizedServicesProvider
import org.rundeck.app.authorization.TimedAuthContextEvaluator
import org.rundeck.app.authorization.WebAuthContextProcessor
import org.rundeck.app.cluster.ClusterInfo
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.JobXMLFormat
import org.rundeck.app.components.JobYAMLFormat
import org.rundeck.app.services.EnhancedNodeService
import org.rundeck.app.spi.RundeckSpiBaseServicesProvider
import org.rundeck.security.*
import org.rundeck.web.infosec.ContainerPrincipalRoleSource
import org.rundeck.web.infosec.ContainerRoleSource
import org.rundeck.web.infosec.HMacSynchronizerTokensManager
import org.rundeck.web.infosec.PreauthenticatedAttributeRoleSource
import org.springframework.beans.factory.config.MapFactoryBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy
import org.springframework.security.web.jaasapi.JaasApiIntegrationFilter
import org.springframework.security.web.session.ConcurrentSessionFilter
import rundeck.interceptors.DefaultInterceptorHelper
import rundeck.services.DirectNodeExecutionService
import rundeck.services.ExecutionValidatorService
import rundeck.services.LocalJobSchedulesManager
import rundeck.services.PasswordFieldsService
import rundeck.services.QuartzJobScheduleManagerService
import rundeck.services.audit.AuditEventsService
import rundeck.services.jobs.JobQueryService
import rundeck.services.jobs.LocalJobQueryService
import rundeck.services.scm.ScmJobImporter
import rundeckapp.init.ExternalStaticResourceConfigurer
import rundeckapp.init.PluginCachePreloader
import rundeckapp.init.RundeckConfigReloader
import rundeckapp.init.RundeckExtendedMessageBundle
import rundeckapp.init.servlet.JettyServletContainerCustomizer

import javax.security.auth.login.Configuration

beans={
    xmlns context: "http://www.springframework.org/schema/context"
//    if (Environment.PRODUCTION == Environment.current) {
//        log4jConfigurer(org.springframework.beans.factory.config.MethodInvokingFactoryBean) {
//            targetClass = "org.springframework.util.Log4jConfigurer"
//            targetMethod = "initLogging"
//            arguments = ["classpath:log4j.properties"]
//        }
//    }
    if (application.config.getProperty("rundeck.multiURL.enabled", Boolean.class, false)) {
        Class requestAwareLinkGeneratorClass = RequestAwareLinkGenerator
        String serverURL = application.config.getProperty("grails.serverURL",String.class)
        String contextPath = application.config.server.servlet["context-path"]
        if (serverURL && (contextPath && "/" != contextPath)) {
            log.info("RequestAwareLinkGenerator using url ${serverURL} and context-path ${contextPath}")
            grailsLinkGenerator(requestAwareLinkGeneratorClass, serverURL, contextPath) {}
        } else if (serverURL) {
            log.info("context-path not set, RequestAwareLinkGenerator using url ${serverURL}")
            grailsLinkGenerator(requestAwareLinkGeneratorClass, serverURL) {}
        } else {
            log.warn("rundeck.multiURL enabled but no grails.serverURL found. This feature will be disabled.")
        }
    }
    defaultGrailsServiceInjectorJobListener(GrailsServiceInjectorJobListener){
        name= 'defaultGrailsServiceInjectorJobListener'
        services=[grailsApplication: ref('grailsApplication'),
                executionService: ref('executionService'),
                frameworkService: ref('frameworkService'),
                metricRegistry:ref('metricRegistry'),
                executionUtilService:ref('executionUtilService'),
                jobSchedulerService:ref('jobSchedulerService'),
                  authContextProvider:ref('rundeckAuthContextProvider'),
                jobSchedulesService:ref('jobSchedulesService')]
        quartzScheduler=ref('quartzScheduler')
    }
    def rdeckBase
    if (!application.config.getProperty("rdeck.base", String.class)) {
        //look for system property
        rdeckBase = System.getProperty('rdeck.base')
    } else {
        rdeckBase = application.config.getProperty("rdeck.base", String.class)
    }
    if(!rdeckBase){
        System.err.println("rdeck.base was not defined in application config or as a system property")
        return
    }

    rundeckI18nEnhancer(RundeckExtendedMessageBundle, ref("messageSource"),"file:${rdeckBase}/i18n/messages".toString())

    if(application.config.getProperty("rundeck.gui.staticUserResources.enabled",Boolean.class,false)) {
        externalStaticResourceConfigurer(ExternalStaticResourceConfigurer) {
            String filesystemLocation = application.config.getProperty("rundeck.gui.staticUserResources.filesystemLocation", String.class)
            resourceUriLocation = filesystemLocation.isEmpty() ? "file:${rdeckBase}/user-assets/" : filesystemLocation
        }
    }

    def serverLibextDir = application.config.getProperty("rundeck.server.plugins.dir",String.class,"${rdeckBase}/libext")
    File pluginDir = new File(serverLibextDir)
    def serverLibextCacheDir = application.config.getProperty("rundeck.server.plugins.cacheDir", String.class,"${serverLibextDir}/cache")
    File cacheDir= new File(serverLibextCacheDir)
    File varDir= new File(Constants.getBaseVar(rdeckBase))

    rundeckNodeService(EnhancedNodeService)

    if(application.config.getProperty("rundeck.loadFrameworkPropertiesFromRundeckConfig", Boolean.class, false)) {
        frameworkPropertyLookupFactory(ConfigFrameworkPropertyLookupFactory) { }
    } else {
        frameworkPropertyLookupFactory(FrameworkPropertyLookupFactory){
            baseDir=rdeckBase
        }
    }

    frameworkPropertyLookup(frameworkPropertyLookupFactory:'create'){

    }

    rundeckNodeSupport(NodeSupport){
        lookup = ref('frameworkPropertyLookup')
    }

    frameworkFilesystem(FrameworkFactory,rdeckBase){ bean->
        bean.factoryMethod='createFilesystemFramework'
    }
    //NB: retained for compatibilty for upgrading from <3.4, should be removed after 3.4
    filesystemProjectManager(FrameworkFactory,frameworkFilesystem,ref('rundeckNodeService')){ bean->
        bean.factoryMethod='createProjectManager'
    }
    rundeckFilesystemProjectImporter(RundeckFilesystemProjectImporter){
        importFilesOption = application.config.getProperty("rundeck.projectsStorageImportFilesOption", String.class, 'known')
        importStartupMode = application.config.getProperty("rundeck.projectsStorageImportStartupMode", String.class,'bootstrap')
    }

    frameworkFactory(RundeckFrameworkFactory){
        frameworkFilesystem=frameworkFilesystem
        propertyLookup=ref('frameworkPropertyLookup')
        type=application.config.getProperty("rundeck.projectsStorageType", String.class,'db')
        dbProjectManager=ref('projectManagerService')
        pluginManagerService=ref('rundeckServerServiceProviderLoader')
    }

    rundeckFramework(frameworkFactory:'createFramework'){
    }

    clusterInfoService(ClusterInfo) {
        clusterInfoServiceDelegate = ref('frameworkService')
    }
    rundeckApiInfoService(ApiInfo)

    rundeckSpiBaseServicesProvider(RundeckSpiBaseServicesProvider) {
        services = [
            (ClusterInfoService)         : ref('clusterInfoService'),
            (ApiInfo)                    : ref('rundeckApiInfoService'),
            (ExecutionFileManagerService): ref('logFileStorageService'),
            (ResourceFormats)            : ref('pluginService')
        ]
    }

    directNodeExecutionService(DirectNodeExecutionService)

    rundeckAuthorizedServicesProvider(RundeckAuthorizedServicesProvider) {
        baseServices = ref('rundeckSpiBaseServicesProvider')
    }

    authContextEvaluatorCacheManager(AuthContextEvaluatorCacheManager){
        enabled = grailsApplication.config.getProperty("rundeck.auth.evaluation.cache.enabled", Boolean.class, false)
        expirationTime = grailsApplication.config.getProperty("rundeck.auth.evaluation.cache.expire", Long.class, 120)
        metricService = ref('metricService')
    }

    baseAuthContextEvaluator(BaseAuthContextEvaluator){
        authContextEvaluatorCacheManager = ref('authContextEvaluatorCacheManager')
        nodeSupport = ref('rundeckNodeSupport')
    }
    rundeckAuthContextEvaluator(TimedAuthContextEvaluator){
        rundeckAuthContextEvaluator = ref('baseAuthContextEvaluator')
    }

    rundeckYamlAclValidatorFactory(BaseValidatorImpl){bean->
        bean.factoryMethod = 'factory'
    }
    rundeckYamlAclValidator(YamlValidator)

    rundeckAuthContextProvider(BaseAuthContextProvider)
    rundeckAuthContextProcessor(BaseAuthContextProcessor){
        rundeckAuthContextProvider=ref('rundeckAuthContextProvider')
        rundeckAuthContextEvaluator=ref('rundeckAuthContextEvaluator')
    }
    rundeckWebAuthContextProcessor(WebAuthContextProcessor){
        authContextProcessor = ref('rundeckAuthContextProcessor')
    }

    aclStorageFileManager(ContextACLStorageFileManagerFactory){
        systemPrefix = ContextACLStorageFileManagerFactory.ACL_STORAGE_PATH_BASE
        projectPattern = ContextACLStorageFileManagerFactory.ACL_PROJECT_STORAGE_PATH_PATTERN
        projectsStorageType=application.config.getProperty("rundeck.projectsStorageType", String.class, 'db')
        validatorFactory=ref('rundeckYamlAclValidatorFactory')
    }

    def configDir = new File(Constants.getFrameworkConfigDir(rdeckBase))

    log4jAuthorizationLogger(Log4jAuthorizationLogger)

    rundeckFilesystemPolicyAuthorization(AclsUtil, configDir, ref('log4jAuthorizationLogger')){ bean->
        bean.factoryMethod='createFromDirectory'
    }

    rundeckJobScheduleManager(QuartzJobScheduleManagerService){
        quartzScheduler=ref('quartzScheduler')
    }

    rundeckJobSchedulesManager(LocalJobSchedulesManager){
        scheduledExecutionService = ref('scheduledExecutionService')
        frameworkService = ref('frameworkService')
        quartzScheduler = ref('quartzScheduler')
    }

    executionValidatorService(ExecutionValidatorService)

    localJobQueryService(LocalJobQueryService)

    jobQueryService(JobQueryService){
        localJobQueryService = ref('localJobQueryService')
    }

    //cache for provider loaders bound to a file
    providerFileCache(PluginManagerService) { bean ->
        bean.factoryMethod = 'createProviderLoaderFileCache'
    }

    pluginDirProvider(WatchingPluginDirProvider, pluginDir)

    //scan for jar plugins
    jarPluginScanner(JarPluginScanner, ref('pluginDirProvider'), cacheDir, ref('providerFileCache'))

    //scan for script-based plugins
    scriptPluginScanner(ScriptPluginScanner, ref('pluginDirProvider'), cacheDir, ref('providerFileCache'))

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
        serviceAliases = [WorkflowNodeStep: 'RemoteScriptNodeStep']
    }

    /**
     * the Job life cycle plugin provider service
     */
    jobLifecyclePluginProviderService(JobLifecyclePluginProviderService){
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
    }

    /**
     * the Execution life cycle plugin provider service
     */
    executionLifecyclePluginProviderService(ExecutionLifecyclePluginProviderService){
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
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
//        pluginRegistry=ref("rundeckPluginRegistry")
    }
    logFileTaskExecutor(SimpleAsyncTaskExecutor, "LogFileTask") {
        concurrencyLimit = 1 + application.config.getProperty("rundeck.execution.logs.fileStorage.retrievalTasks.concurrencyLimit", Integer.class, 5)
    }
    logFileStorageTaskExecutor(SimpleAsyncTaskExecutor, "LogFileStorageTask") {
        concurrencyLimit = 1 + application.config.getProperty("rundeck.execution.logs.fileStorage.storageTasks.concurrencyLimit", Integer.class, 10)
    }
    logFileStorageTaskScheduler(ThreadPoolTaskScheduler) {
        threadNamePrefix="LogFileStorageScheduledTask"
        poolSize= application.config.getProperty("rundeck.execution.logs.fileStorage.scheduledTasks.poolSize", Integer.class, 5)

    }
    logFileStorageDeleteRemoteTask(ThreadPoolTaskExecutor) {
        threadNamePrefix="LogFileStorageDeleteRemoteTask"
        maxPoolSize= application.config.getProperty("rundeck.execution.logs.fileStorage.removeTasks.poolSize", Integer.class, 5)

    }
    nodeTaskExecutor(SimpleAsyncTaskExecutor,"NodeService-SourceLoader") {
        concurrencyLimit = application.config.getProperty("rundeck?.nodeService?.concurrencyLimit", Integer.class, 25) //-1 for unbounded
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
    storagePluginProviderService(StoragePluginProviderService) {
        pluggableStoragePluginProviderService = ref('pluggableStoragePluginProviderService')
    }

    storageConverterPluginProviderService(StorageConverterPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    rundeckJobDefinitionManager(RundeckJobDefinitionManager)
    rundeckJobXmlFormat(JobXMLFormat)
    rundeckJobYamlFormat(JobYAMLFormat) {
        trimSpacesFromLines = application.config.getProperty('rundeck.job.export.yaml.trimSpaces', Boolean)
    }

    scmExportPluginProviderService(ScmExportPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    scmImportPluginProviderService(ScmImportPluginProviderService) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    uiPluginProviderService(UIPluginProviderService,rundeckFramework) {
        rundeckServerServiceProviderLoader = ref('rundeckServerServiceProviderLoader')
    }

    auditEventsService(AuditEventsService){
        frameworkService = ref('frameworkService')
    }

    scmJobImporter(ScmJobImporter)

    containerPrincipalRoleSource(ContainerPrincipalRoleSource){
        enabled=grailsApplication.config.getProperty("rundeck.security.authorization.containerPrincipal.enabled", Boolean.class, false)
    }
    containerRoleSource(ContainerRoleSource){
        enabled=grailsApplication.config.getProperty("rundeck.security.authorization.container.enabled", Boolean.class, false)
    }
    preauthenticatedAttributeRoleSource(PreauthenticatedAttributeRoleSource){
        enabled=grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled", Boolean.class,false)
        attributeName=grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.attributeName", String.class)
        delimiter=grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.delimiter", String.class)
    }

    def storageDir= new File(varDir, 'storage')
    rundeckStorageTreeFactory(StorageTreeFactory){
        frameworkPropertyLookup=ref('frameworkPropertyLookup')
        pluginRegistry=ref("rundeckPluginRegistry")
        storagePluginProviderService=ref('storagePluginProviderService')
        storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
        configuration = application.config.rundeck?.storage?.toFlatConfig()
        storageConfigPrefix='provider'
        converterConfigPrefix='converter'
        baseStorageType='file'
        baseStorageConfig=['baseDir':storageDir.getAbsolutePath()]
        defaultConverters=['StorageTimestamperConverter','KeyStorageLayer']
        loggerName='org.rundeck.storage.events'
    }
    rundeckStorageTree(rundeckStorageTreeFactory:"createTree")
    if(grailsApplication.config.getProperty("rundeck.feature.projectKeyStorage.enabled", Boolean.class, false)) {
        rundeckKeyStorageContextProvider(ProjectKeyStorageContextProvider)
    }else{
        rundeckKeyStorageContextProvider(KeyStorageContextProvider)
    }

    authRundeckStorageTree(AuthRundeckStorageTree, rundeckStorageTree, rundeckKeyStorageContextProvider)

    rundeckConfigStorageTreeFactory(StorageTreeFactory){
        frameworkPropertyLookup=ref('frameworkPropertyLookup')
        pluginRegistry=ref("rundeckPluginRegistry")
        storagePluginProviderService=ref('storagePluginProviderService')
        storageConverterPluginProviderService=ref('storageConverterPluginProviderService')
        configuration = application.config.rundeck?.config?.storage?.toFlatConfig()
        storageConfigPrefix='provider'
        converterConfigPrefix='converter'
        baseStorageType='db'
        baseStorageConfig=[namespace:'config']
        defaultConverters=['StorageTimestamperConverter']
        loggerName='org.rundeck.config.storage.events'
    }
    rundeckConfigStorageTree(rundeckConfigStorageTreeFactory:"createTree")

    rundeckConfigStorageManager(TreeStorageManager, ref('rundeckConfigStorageTree')){ bean->
        bean.factoryMethod='createFromStorageTree'
    }

    /**
     * Define groovy-based plugins as Spring beans, registered in a hash map
     */
    pluginCustomizer(PluginCustomizer){
        pluginRegistry = ref("rundeckPluginRegistryMap")
    }
    xmlns lang: 'http://www.springframework.org/schema/lang'

    appContextEmbeddedPluginFileSource(ApplicationContextPluginFileSource, '/WEB-INF/rundeck/plugins/')

    rundeckEmbeddedPluginExtractor(RundeckEmbeddedPluginExtractor) {
        pluginTargetDir = pluginDir
        rundeckPluginBlocklist = ref("rundeckPluginBlocklist")
    }

    def pluginRegistry=[:]
    if (pluginDir.exists()) {
        pluginDir.eachFileMatch(FileType.FILES, ~/.*\.groovy/) { File plugin ->
            String beanName = plugin.name.replace('.groovy', '')
            lang.groovy(id: beanName, 'script-source': "file:${pluginDir}/${plugin.name}",
                    'refresh-check-delay': application.config.getProperty("plugin.refreshDelay",Integer.class, null) ?: -1,
                    'customizer-ref':'pluginCustomizer'
            )
        }
    }
    dbStoragePluginFactory(DbStoragePluginFactory)
    pluginRegistry[ServiceNameConstants.Storage + ':' + DbStoragePlugin.PROVIDER_NAME]='dbStoragePluginFactory'
    storageTreeExecutionFileStoragePluginFactory(TreeExecutionFileStoragePluginFactory)
    pluginRegistry[ServiceNameConstants.ExecutionFileStorage + ":" + TreeExecutionFileStoragePlugin.PROVIDER_NAME] = 'storageTreeExecutionFileStoragePluginFactory'

    def uploadsDir = new File(varDir, 'upload')
    fsFileUploadPlugin(FSFileUploadPlugin) {
        basePath = uploadsDir.absolutePath
    }
    pluginRegistry[ServiceNameConstants.FileUpload + ":" +FSFileUploadPlugin.PROVIDER_NAME] = 'fsFileUploadPlugin'

    //list of plugin classes to generate factory beans for
    [
            //log converters
            JsonConverterPlugin,
            PropertiesConverterPlugin,
            HTMLTableViewConverterPlugin,
            MarkdownConverterPlugin,
            TabularDataConverterPlugin,
            HTMLViewConverterPlugin,
            //log filters
            MaskPasswordsFilterPlugin,
            MaskLogOutputByRegexPlugin,
            SimpleDataFilterPlugin,
            RenderDatatypeFilterPlugin,
            QuietFilterPlugin,
            HighlightFilterPlugin
    ].each {
        "rundeckAppPlugin_${it.simpleName}"(PluginFactoryBean, it)
    }

    //enable dummy notification plugins for new Notifications UI
    [
        DummyEmailNotificationPlugin,
        DummyWebhookNotificationPlugin,].each {
        "rundeckAppPlugin_${it.simpleName}"(PluginFactoryBean, it)
    }

    //TODO: scan defined plugins:
    //    context.'component-scan'('base-package': "com.dtolabs.rundeck.server.plugins.logging")
    rundeckPluginRegistryMap(MapFactoryBean) {
        sourceMap = pluginRegistry
    }
    rundeckPluginBlocklist(RundeckPluginBlocklist){
        blockListFileName= application.config.getProperty("rundeck.plugins.providerBlockListFile", String.class)
    }
    /**
     * Registry bean contains both kinds of plugin
     */
    rundeckPluginRegistry(RundeckPluginRegistry){
        rundeckEmbeddedPluginExtractor = ref('rundeckEmbeddedPluginExtractor')
        pluginRegistryMap = ref('rundeckPluginRegistryMap')
        rundeckServerServiceProviderLoader=ref('rundeckServerServiceProviderLoader')
        pluginDirectory=pluginDir
        pluginCacheDirectory=cacheDir
        rundeckPluginBlocklist=ref("rundeckPluginBlocklist")
    }
    hMacSynchronizerTokensManager(HMacSynchronizerTokensManager){

    }

    /**
     * Track passwords on these plugins
     */
    obscurePasswordFieldsService(PasswordFieldsService)
    resourcesPasswordFieldsService(PasswordFieldsService)
    execPasswordFieldsService(PasswordFieldsService)
    pluginsPasswordFieldsService(PasswordFieldsService)
    fcopyPasswordFieldsService(PasswordFieldsService)


    /// XML/JSON custom marshaller support

    apiMarshallerRegistrar(ApiMarshallerRegistrar)

    //Job List Link Handler
    defaultJobListLinkHandler(GroupedJobListLinkHandler)
    jobListLinkHandlerRegistry(JobListLinkHandlerRegistry) {
        defaultHandlerName = application.config.getProperty("rundeck.gui.defaultJobList", String.class, GroupedJobListLinkHandler.NAME)
    }

    userSummaryMenuItem(UserSummaryMenuItem)
    systemReportMenuItem(SystemReportMenuItem)
    systemConfigMenuItem(SystemConfigMenuItem)

    rundeckUserDetailsService(RundeckUserDetailsService)
    rundeckJaasAuthorityGranter(RundeckJaasAuthorityGranter){
        rolePrefix=grailsApplication.config.getProperty("rundeck.security.jaasRolePrefix", String.class, '')
    }

    if(!grailsApplication.config.getProperty("rundeck.logout.expire.cookies", String.class,'').isEmpty()) {
        cookieClearingLogoutHandler(CookieClearingLogoutHandler,grailsApplication.config.getProperty("rundeck.logout.expire.cookies", String.class,'').split(","))
        SpringSecurityUtils.registerLogoutHandler("cookieClearingLogoutHandler")

    }

    if(grailsApplication.config.getProperty("rundeck.security.enforceMaxSessions", Boolean.class,false)) {
        sessionRegistry(SessionRegistryImpl)
        concurrentSessionFilter(ConcurrentSessionFilter, sessionRegistry)
        registerSessionAuthenticationStrategy(RegisterSessionAuthenticationStrategy, ref('sessionRegistry')) {}
        concurrentSessionControlAuthenticationStrategy(
                ConcurrentSessionControlAuthenticationStrategy,
                ref('sessionRegistry')
        ) {
            exceptionIfMaximumExceeded = false
            maximumSessions = grailsApplication.config.getProperty("rundeck.security.maxSessions",Integer.class, 1)
        }
        sessionFixationProtectionStrategy(SessionFixationProtectionStrategy) {
            migrateSessionAttributes = grailsApplication.config.getProperty("grails.plugin.springsecurity.sessionFixationPrevention.migrate", String.class,'')
            // true
            alwaysCreateSession = grailsApplication.config.getProperty("grails.plugin.springsecurity.sessionFixationPrevention.alwaysCreateSession", String.class, '')
            // false
        }
        sessionAuthenticationStrategy(
                CompositeSessionAuthenticationStrategy,
                [concurrentSessionControlAuthenticationStrategy, sessionFixationProtectionStrategy, registerSessionAuthenticationStrategy]
        )
    }

    //spring security preauth filter configuration
    if(grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled", Boolean.class,false)) {
        rundeckPreauthSuccessEventHandler(RundeckPreauthSuccessEventHandler) {
            configurationService = ref('configurationService')
        }
        rundeckPreauthFilter(RundeckPreauthenticationRequestHeaderFilter) {
            enabled = grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled", Boolean.class,false)
            userNameHeader = grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.userNameHeader", String.class)
            rolesHeader = grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.userRolesHeader", String.class)
            rolesAttribute = grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.attributeName", String.class)
            authenticationManager = ref('authenticationManager')
            authenticationSuccessHandler = ref("rundeckPreauthSuccessEventHandler")
        }
        rundeckPreauthFilterDeReg(FilterRegistrationBean) {
            filter = ref("rundeckPreauthFilter")
            enabled = false
        }
    }

    if(grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled", Boolean.class, false)
            || grailsApplication.config.getProperty("grails.plugin.springsecurity.useX509", Boolean.class, false)) {
        preAuthenticatedAuthProvider(PreAuthenticatedAuthenticationProvider) {
            preAuthenticatedUserDetailsService = ref('rundeckUserDetailsService')
        }
    }

    if(grailsApplication.config.getProperty("rundeck.useJaas", Boolean.class,false)) {
        //spring security jaas configuration
        jaasApiIntegrationFilter(JaasApiIntegrationFilter)

        jaasAuthProvider(RundeckJaasAuthenticationProvider) {
            configuration = Configuration.getConfiguration()
            loginContextName = grailsApplication.config.getProperty("rundeck.security.jaasLoginModuleName")
            authorityGranters = [
                    ref('rundeckJaasAuthorityGranter')
            ]
        }
    } else {
        jettyCompatiblePasswordEncoder(JettyCompatibleSpringSecurityPasswordEncoder)
        //if not using jaas for security provide a simple default
        Properties realmProperties = new Properties()
        realmProperties.load(new File(grailsApplication.config.getProperty("rundeck.security.fileUserDataSource",String.class)).newInputStream())
        realmPropertyFileDataSource(InMemoryUserDetailsManager, realmProperties)
        realmAuthProvider(DaoAuthenticationProvider) {
            passwordEncoder = ref("jettyCompatiblePasswordEncoder")
            userDetailsService = ref('realmPropertyFileDataSource')
        }
    }

    jettyServletCustomizer(JettyServletContainerCustomizer) {
        def configParams = grailsApplication.config.getProperty("rundeck.web.jetty.servlet.initParams", String.class)

        initParams = configParams?.toProperties()?.collectEntries {
            [it.key.toString(), it.value.toString()]
        }
    }

    rundeckAuthSuccessEventListener(RundeckAuthSuccessEventListener) {
        frameworkService = ref('frameworkService')
    }

    if(grailsApplication.config.getProperty("rundeck.security.syncLdapUser",Boolean.class,false)) {
        rundeckJaasAuthenticationSuccessEventListener(RundeckJaasAuthenticationSuccessEventListener) {
            configurationService = ref('configurationService')
        }
    }
    rundeckConfig(RundeckConfig)
    if(!Environment.isWarDeployed()) {
        appRestarter(AppRestarter)
    }
    rundeckConfigReloader(RundeckConfigReloader)
    pluginCachePreloader(PluginCachePreloader)
    interceptorHelper(DefaultInterceptorHelper)
}
