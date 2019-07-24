package rundeck.services

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.audit.AuditEventListener
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin
import com.dtolabs.rundeck.server.plugins.services.StorageConverterPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StoragePluginProviderService
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService
import grails.core.GrailsApplication
import org.grails.web.util.WebUtils
import org.springframework.context.NoSuchMessageException
import rundeck.services.feature.FeatureService

import javax.servlet.ServletContext
import java.text.SimpleDateFormat

class PluginApiService {

    ServletContext servletContext
    GrailsApplication grailsApplication
    FrameworkService frameworkService
    def messageSource
    UiPluginService uiPluginService
    UIPluginProviderService uiPluginProviderService
    NotificationService notificationService
    LoggingService loggingService
    PluginService pluginService
    ScmService scmService
    LogFileStorageService logFileStorageService
    StoragePluginProviderService storagePluginProviderService
    StorageConverterPluginProviderService storageConverterPluginProviderService
    FeatureService featureService
    JobPluginService jobPluginService

    def listPluginsDetailed() {
        //list plugins and config settings for project/framework props
        IFramework framework = frameworkService.getRundeckFramework()
        Locale locale = getLocale()

        //framework level plugin descriptions
        //TODO: use pluginService.listPlugins for these services/plugintypes
        Map<String,List<Description>> pluginDescs= [
                framework.getNodeExecutorService(),
                framework.getFileCopierService(),
                framework.getNodeStepExecutorService(),
                framework.getStepExecutionService()
        ].collectEntries{
            [it.name, it.listDescriptions().sort {a,b->a.name<=>b.name}]
        }

        //load via pluginService to include spring-based app plugins
        pluginDescs['ResourceModelSource'] = pluginService.listPlugins(
                ResourceModelSourceFactory,
                framework.getResourceModelSourceService()
        ).findAll { it.value.description }.collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        pluginDescs[ServiceNameConstants.NodeEnhancer]=pluginService.listPlugins(NodeEnhancerPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        //TODO: use pluginService.listPlugins for these services/plugintypes
        [
                framework.getResourceFormatParserService(),
                framework.getResourceFormatGeneratorService(),
                framework.getOrchestratorService()
        ].each {

            pluginDescs[it.name] = it.listDescriptions().sort { a, b -> a.name <=> b.name }
        }

        if(featureService.featurePresent("option-values-plugin")) {
            pluginDescs['OptionValues'] = pluginService.listPlugins(OptionValuesPlugin).collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }

        pluginDescs['PasswordUtilityEncrypter'] = pluginService.listPlugins(PasswordUtilityEncrypterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        //web-app level plugin descriptions
        if(featureService.featurePresent("job-plugin")) {
            pluginDescs[jobPluginService.jobPluginProviderService.name]=jobPluginService.listJobPlugins().collect {
                it.value.description
            }.sort { a, b -> a.name <=> b.name }
        }
        pluginDescs[notificationService.notificationPluginProviderService.name]=notificationService.listNotificationPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogReaderPluginProviderService.name]=loggingService.listStreamingReaderPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogWriterPluginProviderService.name]=loggingService.listStreamingWriterPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[logFileStorageService.executionFileStoragePluginProviderService.name]= logFileStorageService.listLogFileStoragePlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storagePluginProviderService.name]= pluginService.listPlugins(StoragePlugin.class, storagePluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storageConverterPluginProviderService.name] = pluginService.listPlugins(StorageConverterPlugin.class, storageConverterPluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[scmService.scmExportPluginProviderService.name]=scmService.listPlugins('export').collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[scmService.scmImportPluginProviderService.name]=scmService.listPlugins('import').collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        pluginDescs['FileUpload']=pluginService.listPlugins(FileUploadPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['LogFilter'] = pluginService.listPlugins(LogFilterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['ContentConverter']=pluginService.listPlugins(ContentConverterPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['TourLoader']=pluginService.listPlugins(TourLoaderPlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['UserGroupSource']=pluginService.listPlugins(UserGroupSourcePlugin).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs['UI']= pluginService.listPlugins(UIPlugin, uiPluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[ServiceNameConstants.AuditEventListener] = pluginService.listPlugins(AuditEventListener).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }


        Map<String,Map> uiPluginProfiles = [:]
        def loadedFileNameMap=[:]
        pluginDescs.each { svc, list ->
            if(svc == "UI") return
            list.each { desc ->
                def provIdent = svc + ":" + desc.name
                uiPluginProfiles[provIdent] = uiPluginService.getProfileFor(svc, desc.name)
                def filename = uiPluginProfiles[provIdent].get('fileMetadata')?.filename
                if(filename){
                    if(!loadedFileNameMap[filename]){
                        loadedFileNameMap[filename]=[]
                    }
                    loadedFileNameMap[filename]<< provIdent
                }
            }
        }

        def defaultScopes=[
                (framework.getNodeStepExecutorService().name) : PropertyScope.InstanceOnly,
                (framework.getStepExecutionService().name) : PropertyScope.InstanceOnly,
        ]
        def bundledPlugins=[
                (framework.getNodeExecutorService().name): framework.getNodeExecutorService().getBundledProviderNames(),
                (framework.getFileCopierService().name): framework.getFileCopierService().getBundledProviderNames(),
                (framework.getResourceFormatParserService().name): framework.getResourceFormatParserService().getBundledProviderNames(),
                (framework.getResourceFormatGeneratorService().name): framework.getResourceFormatGeneratorService().getBundledProviderNames(),
                (framework.getResourceModelSourceService().name): framework.getResourceModelSourceService().getBundledProviderNames(),
                (storagePluginProviderService.name): storagePluginProviderService.getBundledProviderNames()+['db'],
                FileUpload: ['filesystem-temp'],
        ]
        //list included plugins
        def embeddedList = frameworkService.listEmbeddedPlugins(grailsApplication)
        def embeddedFilenames=[]
        if(embeddedList.success && embeddedList.pluginList){
            embeddedFilenames=embeddedList.pluginList*.fileName
        }
        def specialConfiguration=[
                (storagePluginProviderService.name):[
                        description: message("plugin.storage.provider.special.description",locale),
                        prefix:"rundeck.storage.provider.[index].config."
                ],
                (storageConverterPluginProviderService.name):[
                        description: message("plugin.storage.converter.special.description",locale),
                        prefix:"rundeck.storage.converter.[index].config."
                ],
                (framework.getResourceModelSourceService().name):[
                        description: message("plugin.resourceModelSource.special.description",locale),
                        prefix:"resources.source.[index].config."
                ],
                (logFileStorageService.executionFileStoragePluginProviderService.name):[
                        description: message("plugin.executionFileStorage.special.description",locale),
                ],
                (scmService.scmExportPluginProviderService.name):[
                        description: message("plugin.scmExport.special.description",locale),
                ],
                (scmService.scmImportPluginProviderService.name):[
                        description: message("plugin.scmImport.special.description",locale),
                ],
                FileUpload:[
                        description: message("plugin.FileUpload.special.description",locale),
                ]
        ]
        def specialScoping=[
                (scmService.scmExportPluginProviderService.name):true,
                (scmService.scmImportPluginProviderService.name):true
        ]

        [
                descriptions        : pluginDescs,
                serviceDefaultScopes: defaultScopes,
                bundledPlugins      : bundledPlugins,
                embeddedFilenames   : embeddedFilenames,
                specialConfiguration: specialConfiguration,
                specialScoping      : specialScoping,
                uiPluginProfiles    : uiPluginProfiles
        ]
    }

    def listPlugins() {
        Locale locale = getLocale()
        String appDate = servletContext.getAttribute('version.date')
        String appVer = servletContext.getAttribute('version.number')
        def pluginList = listPluginsDetailed()
        def tersePluginList = pluginList.descriptions.collect {
            String service = it.key
            def providers = it.value.collect { provider ->
                def meta = frameworkService.getRundeckFramework().
                        getPluginManager().
                        getPluginMetadata(service, provider.name)
                boolean builtin = meta == null
                String ver = meta?.pluginFileVersion ?: appVer
                String dte = meta?.pluginDate ?: appDate
                String artifactName = meta?.pluginArtifactName ?: provider.name
                String tgtHost = meta?.targetHostCompatibility ?: 'all'
                String rdVer = meta?.rundeckCompatibilityVersion ?: 'unspecified'
                String author = meta?.pluginAuthor ?: ''
                String id = meta?.pluginId ?: PluginUtils.generateShaIdFromName(artifactName)
                [pluginId   : id,
                 pluginName : artifactName,
                 name         : provider.name,
                 title        : provider.title,
                 description  : provider.description,
                 builtin      : builtin,
                 pluginVersion: ver,
                 pluginAuthor : author,
                 rundeckCompatibilityVersion: rdVer,
                 targetHostCompatibility: tgtHost,
                 pluginDate   : toEpoch(dte),
                 enabled      : true]
            }
            [service  : it.key,
             desc     : message("framework.service.${service}.description".toString(),locale),
             providers: providers
            ]
        }
        tersePluginList
    }

    List<Map> pluginPropertiesAsMap(String service, String pluginName, List<Property> properties) {
        properties.collect { Property prop ->
            pluginPropertyMap(service, pluginName, prop)
        }
    }

    /**
     * Return map representation of plugin property definition
     * @param service service
     * @param pluginName provider
     * @param prop property
     * @param locale locale
     * @return
     */
    Map pluginPropertyMap(String service, String pluginName, Property prop) {
        [
            name                  : prop.name,
            desc                  : prop.description,
            title                 : uiPluginService.getPluginMessage(
                service,
                pluginName,
                "property.${prop.name}.title",
                prop.title ?: prop.name,
                locale
            ),
            defaultValue          : prop.defaultValue,
            staticTextDefaultValue: uiPluginService.getPluginMessage(
                service,
                pluginName,
                "property.${prop.name}.defaultValue",
                prop.defaultValue ?: '',
                locale
            ),
            required              : prop.required,
            type                  : prop.type.toString(),
            allowed               : prop.selectValues,
            selectLabels          : prop.selectLabels,
            scope                 : prop.scope?.toString(),
            options               : asStringMap(prop)
        ]
    }

    public Map<String, String> asStringMap(Property prop) {
        if (!prop.renderingOptions) {
            return null
        }
        Map<String, String> opts = [:]
        prop.renderingOptions.each { String k, Object v ->
            opts[k]=v.toString()
        }
        opts
    }

    def listInstalledPluginIds() {
        def idList = [:]
        def plugins = pluginService.listPlugins(UIPlugin, uiPluginProviderService)
        plugins.each{ entry ->
            def meta = frameworkService.getRundeckFramework().
                    getPluginManager().
                    getPluginMetadata("UI", entry.key)
            String id = meta?.pluginId ?: PluginUtils.generateShaIdFromName(entry.key)
            idList[id] = meta?.pluginFileVersion ?: "1.0.0"
        }

        listPlugins().each { p ->
            p.providers.each { idList[it.pluginId] = it.pluginVersion }
        }

        return idList
    }

    Locale getLocale() {
        WebUtils.retrieveGrailsWebRequest().getLocale()
    }

    private def message(String code, Locale locale) {
        try {
            messageSource.getMessage(code,[].toArray(),locale)
        } catch(NoSuchMessageException nsme) {
            return code
        }

    }

    private long toEpoch(String dateString) {
        try {
            return PLUGIN_DATE_FMT.parse(dateString).time
        } catch(Exception ex) {
            log.error("unable to parse date: ${dateString}")
        }
        return System.currentTimeMillis()
    }

    private static final SimpleDateFormat PLUGIN_DATE_FMT = new SimpleDateFormat("EEE MMM dd hh:mm:ss Z yyyy")
}
