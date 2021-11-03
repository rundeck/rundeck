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

package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.Closeables
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobImportReference
import com.dtolabs.rundeck.plugins.scm.JobImportState
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmExportResult
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmImportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportTrackedItem
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmOperationContextBuilder
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.scm.ScmUserInfoMissing
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmImportPluginProviderService
import groovy.transform.CompileStatic
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.services.scm.ContextJobImporter
import rundeck.services.scm.ResolvedJobImporter
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmPluginConfigData
import rundeck.services.scm.ScmUser

/**
 * Manages scm integration
 */
class ScmService {
    public static final String EXPORT = 'export'
    public static final String IMPORT = 'import'
    public static final String IMPORT_PREF = 'scm.import'
    public static final String EXPORT_PREF = 'scm.export'
    public static final String STORAGE_NAME_IMPORT = 'scm-import'
    public static final String STORAGE_NAME_EXPORT = 'scm-export'
    public static final Map<String, String> PREFIXES = [export: EXPORT_PREF, import: IMPORT_PREF]
    public static final Map<String, String> STORAGE_NAMES = [export: STORAGE_NAME_EXPORT, import: STORAGE_NAME_IMPORT]
    public static final ArrayList<String> INTEGRATIONS = [EXPORT, IMPORT]


    def JobEventsService jobEventsService
    def ContextJobImporter scmJobImporter
    def grailsApplication
    def frameworkService
    AuthContextProvider rundeckAuthContextProvider
    ScmExportPluginProviderService scmExportPluginProviderService
    ScmImportPluginProviderService scmImportPluginProviderService
    PluginService pluginService
    ScheduledExecutionService scheduledExecutionService
    JobMetadataService jobMetadataService
    PluginConfigService pluginConfigService
    def StorageService storageService
    RundeckJobDefinitionManager rundeckJobDefinitionManager
    final Set<String> initedProjects = Collections.synchronizedSet(new HashSet())
    Map<String, CloseableProvider<ScmExportPlugin>> loadedExportPlugins = Collections.synchronizedMap([:])
    Map<String, CloseableProvider<ScmImportPlugin>> loadedImportPlugins = Collections.synchronizedMap([:])
    Map<String, JobChangeListener> loadedExportListeners = Collections.synchronizedMap([:])
    Map<String, JobChangeListener> loadedImportListeners = Collections.synchronizedMap([:])

    def initialize() {
        if(!isScmInitDeferred()) {
            for (String project : frameworkService.projectNames()) {
                initProject(project)
            }
        }
    }

    boolean isScmInitDeferred(){
        if(grailsApplication.config.rundeck?.scm?.startup?.containsKey('initDeferred')) {
            return grailsApplication.config.rundeck?.scm?.startup?.initDeferred in [true, 'true']
        }
        return true
    }

    /**
     * Initialize integrations for the project.
     * @param project
     */
    def initProject(String project){
        for (String integration : INTEGRATIONS) {
            initProject(project, integration)
        }
    }

    def initProject(String project, String integration){
        def pluginConfig = loadScmConfig(project, integration)
        if (!pluginConfig?.enabled) {
            initedProjects.remove(integration + '/' + project)
            return false
        }
        synchronized (initedProjects) {
            if (initedProjects.contains(integration + '/' + project)) {
                return true
            }
        }

        log.debug("Loading '${integration}' plugin ${pluginConfig.type} for ${project}...")

        def username = pluginConfig.getSetting("username")
        def roles = pluginConfig.getSettingList("roles")

        if (!username || !roles) {
            log.error(
                    "SCM ${integration} config not valid (missing username or roles) for project: ${project}: ${pluginConfig}"
            )
            return false
        }

        try {
            def context = scmOperationContext(username, roles, project)
            try{
                initPlugin(integration, context, pluginConfig.type, pluginConfig.config)
            }catch(ScmPluginException validationException){
                log.error("Unable to initialize SCM for project ${project} for integration ${integration}", validationException)
                return false
            }
            initedProjects.add(integration + '/' + project)
            return true
        } catch (Throwable e) {
            log.error(
                    "Failed to initialize SCM ${integration} plugin ${pluginConfig.type} for ${project}: ${e.message}",
                    e
            )
        }
    }
    ScmExportPlugin getLoadedExportPluginFor(String project){
        initProject(project, EXPORT)
        loadedExportPlugins[project]?.provider
    }
    ScmImportPlugin getLoadedImportPluginFor(String project){
        initProject(project, IMPORT)
        loadedImportPlugins[project]?.provider
    }

    def listPlugins(String integration) {
        switch (integration) {
            case EXPORT:
                return pluginService.listPlugins(ScmExportPluginFactory, scmExportPluginProviderService)
            case IMPORT:
                return pluginService.listPlugins(ScmImportPluginFactory, scmImportPluginProviderService)
        }
    }

    def getPluginDescriptor(String integration, String type) {
        switch (integration) {
            case EXPORT:
                return getExportPluginDescriptor(type)
            case IMPORT:
                return getImportPluginDescriptor(type)
        }
    }

    def getExportPluginDescriptor(String type) {
        pluginService.getPluginDescriptor(
                type,
                scmExportPluginProviderService
        )
    }

    def getImportPluginDescriptor(String type) {
        pluginService.getPluginDescriptor(
                type,
                scmImportPluginProviderService
        )
    }

    def projectHasConfiguredPlugin(String integration, String project) {
        switch (integration) {
            case IMPORT:
                return projectHasConfiguredImportPlugin(project)
            case EXPORT:
                return projectHasConfiguredExportPlugin(project)
        }
    }

    def projectHasConfiguredExportPlugin(String project) {
        def loaded = initProject(project, EXPORT)
        if(!loaded){
            unregisterPlugin(project, EXPORT)
        }
        return loaded
    }

    def projectHasConfiguredImportPlugin(String project) {
        def loaded = initProject(project, IMPORT)
        if(!loaded){
            unregisterPlugin(project, IMPORT)
        }
        return loaded
    }

    BasicInputView getInputView(UserAndRolesAuthContext auth, String integration, String project, String actionId) {
        switch (integration) {
            case EXPORT:
                return getExportInputView(auth, project, actionId)
            case IMPORT:
                return getImportInputView(auth, project, actionId)
        }

    }

    BasicInputView getExportInputView(UserAndRolesAuthContext auth, String project, String actionId) {
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            def context = scmOperationContext(auth, project)
            plugin.getInputViewForAction(context, actionId)
        }
    }

    BasicInputView getImportInputView(UserAndRolesAuthContext auth, String project, String actionId) {
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            def context = scmOperationContext(auth, project)
            plugin.getInputViewForAction(context, actionId)
        }
    }

    def getSetupProperties(String integration, String project, String type) {
        switch (integration) {
            case IMPORT:
                return getImportSetupProperties(project, type)
            case EXPORT:
                return getExportSetupProperties(project, type)
        }
    }

    private def getExportSetupProperties(String project, String type) {
        DescribedPlugin<ScmExportPluginFactory> plugin = getExportPluginDescriptor(type)
        File baseDir = new File(frameworkService.rundeckFramework.frameworkProjectsBaseDir, project)

        plugin.instance.getSetupPropertiesForBasedir(baseDir)
    }

    private def getImportSetupProperties(String project, String type) {
        DescribedPlugin<ScmImportPluginFactory> plugin = getImportPluginDescriptor(type)
        File baseDir = new File(frameworkService.rundeckFramework.frameworkProjectsBaseDir, project)

        plugin.instance.getSetupPropertiesForBasedir(baseDir)
    }

    /**
     * Return the descriptor for the configured plugin for the project, or null
     * @param project project
     * @param integration integration
     * @return description, or null
     */
    def loadProjectPluginDescriptor(String project, String integration) {
        def config = loadScmConfig(project, integration)
        config?.enabled ? getPluginDescriptor(integration,config.type): null
    }
    ScmPluginConfigData loadScmConfig(String project, String integration) {
        pluginConfigService.loadScmConfig(project, pathForConfigFile(integration), PREFIXES[integration])
    }

    private String pathForConfigFile(String integration) {
        if(frameworkService.isClusterModeEnabled()){ //universal config for cluster
            return "etc/scm-${integration}.properties"
        }
        if (frameworkService.serverUUID) {
            return "${frameworkService.serverUUID}/etc/scm-${integration}.properties"
        }
        "etc/scm-${integration}.properties"
    }


    def storeConfig(ScmPluginConfigData scmPluginConfig, String project, String integration) {
        pluginConfigService.storeConfig(scmPluginConfig, project, pathForConfigFile(integration))
    }

    def ValidatedPlugin validatePluginSetup(String integration, String project, String name, Map config) {
        switch (integration) {
            case EXPORT:
                return validateExportPluginSetup(project, name, config)
            case IMPORT:
                return validateImportPluginSetup(project, name, config)

        }
    }

    def ValidatedPlugin validateExportPluginSetup(String project, String name, Map config) {
        return pluginService.validatePlugin(name, scmExportPluginProviderService,
                                            frameworkService.getFrameworkPropertyResolver(project, config),
                                            PropertyScope.Instance,
                                            PropertyScope.Project
        )
    }

    def ValidatedPlugin validateImportPluginSetup(String project, String name, Map config) {
        return pluginService.validatePlugin(name, scmImportPluginProviderService,
                                            frameworkService.getFrameworkPropertyResolver(project, config),
                                            PropertyScope.Instance,
                                            PropertyScope.Project
        )
    }

    def Validator.Report validatePluginConfigProperties(String project, List<Property> properties, Map config) {
        Validator.validateProperties(
                frameworkService.getFrameworkPropertyResolver(project, config),
                properties,
                PropertyScope.Instance,
                PropertyScope.Project
        )
    }

    /**
     * Track deleted jobs by repo path
     * Project->[ repoPath -> [id: jobid, jobAndGroupPath: String] ]
     */
    private Map<String, Map<String, Map<String, String>>> deletedJobsCache = Collections.synchronizedMap([:])

    /**
     * cache info about a deleted job for the repo path
     * @param project project name
     * @param path repo path
     * @param ident job info map
     * @return
     */
    private recordDeletedJob(String project, String path, Map ident) {
        if (!deletedJobsCache[project]) {
            deletedJobsCache[project] = Collections.synchronizedMap([:])
        }
        log.debug "Record deleted job ${ident} for ${project}, path: ${path}"
        deletedJobsCache[project][path] = ident
    }

    /**
     * Return a cached job info map for the deleted path, if present
     * @param project project
     * @param path repo path
     * @return job info map
     */
    public Map deletedJobForPath(String project, String path) {
        log.debug "Get deleted job for ${project}, path: ${path}"
        return deletedJobsCache[project]?.get(path)
    }

    /**
     * Remove cache info about deleted jobs
     * @param project project
     * @param paths set of repo paths to forget about
     */
    private void forgetDeletedPaths(String project, Collection<String> paths) {
        log.debug "Forget deleted jobs for ${project}, paths: ${paths}"
        if (deletedJobsCache[project]) {
            paths.each { deletedJobsCache[project].remove(it) }
        }
    }
    /**
     * Track renamed jobs by job id and old repoPath
     *
     * Project -> [ jobid: repopath]
     */
    private Map<String, Map<String, String>> renamedJobsCache = Collections.synchronizedMap([:])

    /**
     * Cache the old repo path for a renamed job
     * @param project project
     * @param jobid job id
     * @param path old path
     * @return
     */
    private recordRenamedJob(String project, String jobid, String path) {
        if (!renamedJobsCache[project]) {
            renamedJobsCache[project] = Collections.synchronizedMap([:])
        }
        log.debug "Record renamed job ${jobid} for ${project}, path: ${path}"
        renamedJobsCache[project][jobid] = path
    }
    /**
     * Return a cached repo path for a job id, if present
     * @param project project
     * @param jobid job id
     * @return repo path for original job name
     */
    public String getRenamedPathForJobId(String project, String jobid) {
        return renamedJobsCache[project]?.get(jobid)
    }
    /**
     * Remove cached repo path about renamed jobs
     * @param project project
     * @param jobids set of job ids
     */
    private void forgetRenamedJobs(String project, Collection<String> jobids) {
        log.debug "Forget renamed jobs for ${project}, paths: ${jobids}"
        if (renamedJobsCache[project]) {
            jobids.each { renamedJobsCache[project].remove(it) }
        }
    }
    /**
     * Return a map of [ jobid: originalPath]
     * @param jobs
     * @return
     */
    Map<String, String> getRenamedJobPathsForProject(String project) {
        new HashMap<>(renamedJobsCache[project] ?: [:])
    }

    /**
     * Create new plugin config and load it
     * @param context context
     * @param config
     * @param modifiedListeners used when running validation, so it doesn't add the plugin to the listeners
     * @return
     */
    def initPlugin(String integration, ScmOperationContext context, String type, Map config, modifiedListeners = true) {
        def validation = validatePluginSetup(integration, context.frameworkProject, type, config)
        if (!validation) {
            throw new ScmPluginException("Plugin could not be loaded: " + type)
        }
        if (!(validation?.valid)) {
            throw new ScmPluginInvalidInput(
                    "Validation failed for ${type} plugin: " + validation?.report,
                    validation?.report
            )
        }
        def loaded = loadPluginWithConfig(integration, context, type, config)

        JobChangeListener changeListener
        if (integration == EXPORT) {
            ScmExportPlugin plugin = loaded.provider
            changeListener = listenerForExportPlugin(plugin, context)
        } else {
            ScmImportPlugin plugin = loaded.provider
            changeListener = listenerForImportPlugin(plugin)
        }
        registerPlugin(integration, loaded, changeListener, context.frameworkProject)

        loaded.provider
    }

    private JobChangeListener listenerForImportPlugin(
            ScmImportPlugin plugin
    )
    {
        { JobChangeEvent event, JobSerializer serializer ->
            log.debug("job change event: " + event)
            def scmRef = scmJobRef(event.jobReference, false, serializer)
            def origScmRef = event.originalJobReference?scmOrigJobRef(event.originalJobReference, false, null):null
            plugin.jobChanged(new StoredJobChangeEvent(
                    eventType: event.eventType,
                    originalJobReference: origScmRef,
                    jobReference: scmRef
            ), scmRef )
            if (event.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
                jobMetadataService.removeJobPluginMetaAll(event.jobReference.project, event.jobReference.id)
            }
        } as JobChangeListener

    }

    @CompileStatic
    static class ExportChangeListener implements JobChangeListener{
        ScmService service
        ScmExportPlugin plugin
        ScmOperationContext context
        @Override
        void jobChangeEvent(final JobChangeEvent event, final JobSerializer serializer) {
            Logger logger = LoggerFactory.getLogger(ExportChangeListener)
            logger.debug("job change event: " + event)
            JobScmReference scmRef = service.scmJobRef(event.jobReference,true, serializer)
            JobScmReference origScmRef = event.originalJobReference?service.scmOrigJobRef(event.originalJobReference, true,null):null
            if (event.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
                //record deleted path
                service.recordDeletedJob(
                    context.frameworkProject,
                    plugin.getRelativePathForJob(scmRef),
                    [
                        id             : event.jobReference.id,
                        jobName        : event.jobReference.getJobName(),
                        groupPath      : event.jobReference.getGroupPath(),
                        jobNameAndGroup: event.jobReference.getJobAndGroup(),
                    ]
                )
            } else if (event.eventType == JobChangeEvent.JobChangeEventType.MODIFY_RENAME) {
                //record original path for renamed job, if it is different
                def origpath = plugin.getRelativePathForJob(origScmRef)
                def newpath = plugin.getRelativePathForJob(scmRef)
                if (origpath != newpath) {
                    service.recordRenamedJob(context.frameworkProject, event.jobReference.id, origpath)
                }
            }

            plugin.jobChanged(
                new StoredJobChangeEvent(
                    eventType: event.eventType,
                    originalJobReference: origScmRef,
                    jobReference: scmRef
                ),
                scmRef
            )
        }
    }

    private JobChangeListener listenerForExportPlugin(
            ScmExportPlugin plugin,
            ScmOperationContext context
    )
    {
        new ExportChangeListener(service: this, plugin: plugin, context: context)
    }

    /**
     * Create new plugin config and load it
     * @param project
     * @param ndx
     * @param config
     * @param capabilities
     * @return
     */
    def savePluginSetup(UserAndRolesAuthContext auth, String integration, String project, String type, Map config) {
        def validation = validatePluginSetup(integration, project, type, config)
        if (!validation || !validation.valid) {
            return [valid: false, report: validation?.report]
        }

        def scmPluginConfig = new ScmPluginConfig(new Properties(), PREFIXES[integration])
        scmPluginConfig.config = config
        scmPluginConfig.type = type
        scmPluginConfig.enabled = true
        scmPluginConfig.setSetting("username", auth.username)
        scmPluginConfig.setSetting("roles", auth.roles as List)
        storeConfig(scmPluginConfig, project, integration)
        try {
            def context = scmOperationContext(auth, project)
            def plugin = initPlugin(integration, context, type, config)
            if (integration == IMPORT) {
                def nextAction = plugin.getSetupAction(context)
                if (nextAction) {
                    return [valid: true, plugin: plugin, nextAction: nextAction]
                }
            }
            return [valid: true, plugin: plugin]
        } catch (ScmPluginInvalidInput e) {
            return [valid: false, report: e.report]
        } catch (ScmPluginException e) {
            return [error: true, message: e.message]
        }
    }

    /**
     * Construct an ScmOperationContext with an auth context
     * @param auth auth context
     * @param project project
     * @return
     */
    ScmOperationContext scmOperationContext(UserAndRolesAuthContext auth, String project, String job = null) {
        scmOperationContext {
            authContext auth
            frameworkProject project
            userInfo lookupUserInfo(auth.username)
            storageTree(storageService.storageTreeWithContext(auth))
            jobId job
        }
    }

    /**
     * Construct an ScmOperationContext with username and role list
     * @param username username
     * @param roles role list
     * @param project project
     * @return
     */
    ScmOperationContext scmOperationContext(String username, List<String> roles, String project) {
        scmOperationContext(rundeckAuthContextProvider.getAuthContextForUserAndRolesAndProject(username, roles, project), project)
    }

    /**
     * Disable a previously enabled and configured plugin
     * @param integration integration name
     * @param project project name
     * @param type plugin type
     * @return
     */
    def disablePlugin(String integration, String project, String type) {
        ScmPluginConfigData scmPluginConfig = pluginConfigService.loadScmConfig(
                project,
                pathForConfigFile(integration),
                PREFIXES[integration]
        )

        if (scmPluginConfig) {
            if (scmPluginConfig.type != type) {
                throw new IllegalArgumentException("Plugin type ${type} for ${integration} is not configured")
            }
            scmPluginConfig.enabled = false
            storeConfig(scmPluginConfig, project, integration)
        }
        if (initedProjects.contains(integration + '/' + project)) {
            initedProjects.remove(integration + '/' + project)
        }

        unregisterPlugin(integration, project)
    }


    /**
     * First unregister and plugin already registered, then register this plugin
     * @param integration
     * @param context
     * @param loaded
     * @param changeListener @param project @param project
     */
    public void registerPlugin(
            String integration,
            CloseableProvider<? extends Object> loaded,
            JobChangeListener changeListener,
            String project
    ) {
        unregisterPlugin(integration, project)
        def registeredListener = jobEventsService.addListenerForProject changeListener, project
        if (integration == EXPORT) {
            loadedExportPlugins[project] = loaded
            loadedExportListeners[project] = registeredListener
        } else {
            loadedImportPlugins[project] = loaded
            loadedImportListeners[project] = registeredListener
        }
    }

    /**
     * Unregister and close a loaded plugin, remove listeners, if present
     * @param integration
     * @param project
     */
    public void unregisterPlugin(String integration, String project) {
        def loaded
        if (integration == EXPORT) {
            loaded = loadedExportPlugins.remove(project)
            loaded?.close()
            def changeListener = loadedExportListeners.remove(project)
            jobEventsService.removeListener(changeListener)
            //clear cached rename/delete info
            renamedJobsCache.remove(project)
            deletedJobsCache.remove(project)
        } else {
            loaded = loadedImportPlugins.remove(project)
            loaded?.close()
            def changeListener = loadedImportListeners.remove(project)
            jobEventsService.removeListener(changeListener)
        }
        initedProjects.remove(integration + '/' + project)
        loaded?.provider?.cleanup()
    }

    /**
     * Clean a previously configured plugin
     * @param integration integration name
     * @param project project name
     * @param type plugin type
     * @return
     */
    def cleanPlugin(String integration, String project, String type,UserAndRolesAuthContext auth) {
        ScmPluginConfigData scmPluginConfig = pluginConfigService.loadScmConfig(
                project,
                pathForConfigFile(integration),
                PREFIXES[integration]
        )

        if (scmPluginConfig) {
            if (scmPluginConfig.type != type) {
                throw new IllegalArgumentException("Plugin type ${type} for ${integration} is not configured")
            }
            scmPluginConfig.enabled = false
            storeConfig(scmPluginConfig, project, integration)
        }

        def context = scmOperationContext(auth, project)

        try{
            def loaded = loadPluginWithConfig(integration, context, type, scmPluginConfig.config, false)
            try{
                jobMetadataService.removeProjectPluginMeta(project, integration == EXPORT ? STORAGE_NAME_EXPORT:STORAGE_NAME_IMPORT)
                loaded?.provider?.totalClean()
            }finally{
                loaded?.close()
            }
        }catch (ScmPluginInvalidInput e) {
            return [valid: false, message: e.message]
        }
    }




    /**
     * Disable and remove all plugins for a project
     * @param project project
     * @return
     */
    def removeAllPluginConfiguration(String project) {
        def importConfig = loadScmConfig(project, IMPORT)
        if(importConfig?.type) {
            removePluginConfiguration(IMPORT, project, importConfig.type)
        }
        def exportConfig = loadScmConfig(project, EXPORT)
        if(exportConfig?.type){
            removePluginConfiguration(EXPORT, project, exportConfig.type)
        }
    }

    /**
     * Disable and remove configuration for a plugin
     * @param integration integration
     * @param project project
     * @param type type
     * @return
     */
    def removePluginConfiguration(String integration, String project, String type) {
        unregisterPlugin(integration, project)
        pluginConfigService.removePluginConfiguration(project, pathForConfigFile(integration))
    }
    /**
     * Enable a disabled plugin
     * @param project
     * @param integration integration name
     * @param type plugin type
     * @return a map [valid:true/false, report: Report, plugin: instance], will
     */
    def enablePlugin(UserAndRolesAuthContext auth, String integration, String project, String type) {
        ScmPluginConfigData scmPluginConfig = pluginConfigService.loadScmConfig(
                project,
                pathForConfigFile(integration),
                PREFIXES[integration]
        )
        if (scmPluginConfig.type != type) {
            return [error:true,message:"Plugin type ${type} for ${integration} is not configured"]
        }
        def validation = validatePluginSetup(integration, project, type, scmPluginConfig.config)
        if(!validation){
            return [error:true,message:"Plugin type ${type} for ${integration} was not found"]
        }
        if (!validation.valid) {
            return [valid: false, report: validation.report]
        }

        try {
            def context = scmOperationContext(auth, project)
            def plugin = initPlugin(integration, context, type, scmPluginConfig.config)
            scmPluginConfig.enabled = true
            storeConfig(scmPluginConfig, project, integration)
            if (integration == IMPORT) {
                def nextAction = plugin.getSetupAction(context)
                if (nextAction) {
                    return [valid: true, plugin: plugin, nextAction: nextAction]
                }
            }
            return [valid: true, plugin: plugin]
        } catch (ScmPluginInvalidInput e) {
            return [valid: false, report: e.report]
        } catch (ScmPluginException e) {
            return [error: true, message: e.message]
        }
    }

    def loadPluginWithConfig(String integration, ScmOperationContext context, String type, Map config, boolean initialize = true) {
        switch (integration) {
            case EXPORT:
                return loadExportPluginWithConfig(context, type, config, initialize)
            case IMPORT:
                return loadImportPluginWithConfig(context, type, config, initialize)
        }

    }

    CloseableProvider<ScmExportPlugin> loadExportPluginWithConfig(
            ScmOperationContext context,
            String type,
            Map config,
            boolean initialize = true
    )
    {
        CloseableProvider<ScmExportPluginFactory> providerReference = pluginService.retainPlugin(
                type,
                scmExportPluginProviderService
        )
        def plugin = providerReference.provider.createPlugin(context, config, initialize)
        return Closeables.closeableProvider(plugin, providerReference)
    }

    CloseableProvider<ScmImportPlugin> loadImportPluginWithConfig(
            ScmOperationContext context,
            String type,
            Map config,
            boolean initialize = true
    )
    {
        CloseableProvider<ScmImportPluginFactory> providerReference = pluginService.retainPlugin(
                type,
                scmImportPluginProviderService
        )

        def list = loadInputTrackingItems(context.frameworkProject)

        def plugin = providerReference.provider.createPlugin(context, config, list, initialize)
        return Closeables.closeableProvider(plugin, providerReference)
    }

    /**
     * @param jobs list of {@link ScheduledExecution} objects
     * @return map of job ID to file path
     */
    Map<String, String> exportFilePathsMapForJobs(String project, List<ScheduledExecution> jobs, Map<String,Map> jobMetadata=[:]) {
        def files = [:]
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            jobs.each { ScheduledExecution job ->
                Map metadata = jobMetadata[job.extid] ?: getJobPluginMeta(job, STORAGE_NAME_EXPORT)
                files[job.extid] = plugin.getRelativePathForJob(scmJobRef(job, null, metadata))
            }
        }
        files
    }

    /**
     * @param jobs list of {@link ScheduledExecution} objects
     * @return map of job ID to file path
     */
    Map<String, String> importFilePathsMapForJobs(String project, List<ScheduledExecution> jobs, Map<String,Map> jobMetadata=[:]) {
        def files = [:]
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            jobs.each { ScheduledExecution job ->
                Map metadata = jobMetadata[job.extid] ?: getJobPluginMeta(job, STORAGE_NAME_IMPORT)
                files[job.extid] = plugin.getRelativePathForJob(scmJobRef(job, null, metadata))
            }
        }
        files
    }

    List<JobRevReference> jobRefsForIds(List<String> ids) {
        jobRefsForJobs(
                ids.collect {
                    ScheduledExecution.getByIdOrUUID(it)
                }.grep { it }
        )
    }

    List<JobRevReference> jobRefsForJobs(List<ScheduledExecution> jobs) {
        jobs.collect { ScheduledExecution entry ->
            jobRevReference(entry)
        }
    }

    static JobRevReference jobRevReference(ScheduledExecution entry) {
        new JobRevReferenceImpl(
                id: entry.extid,
                jobName: entry.jobName,
                groupPath: entry.groupPath,
                project: entry.project,
                version: entry.version
        )
    }

    static JobRevReference jobOrigRevReference(JobReference entry) {
        new JobRevReferenceImpl(
                id: entry.id,
                jobName: entry.jobName,
                groupPath: entry.groupPath,
                project: entry.project,
                version: -1
        )
    }

    List<JobExportReference> exportjobRefsForJobs(List<ScheduledExecution> jobs, Map<String, Map> jobsPluginMeta = null) {
        jobs.collect { ScheduledExecution job ->
            if(jobsPluginMeta){
                def jobPluginMeta = jobsPluginMeta.get(job.uuid)
                exportJobRef(job, jobPluginMeta)
            }else{
                scmJobRef(job, true)
            }

        }
    }

    private JobExportReference exportJobRef(ScheduledExecution job, Map jobPluginMeta=null) {
        scmJobRef(job, null, jobPluginMeta)
    }

    static class LazySerializer implements JobSerializer {
        ScheduledExecution job
        RundeckJobDefinitionManager rundeckJobDefinitionManager

        @Override
        void serialize(final String format, final OutputStream outputStream) throws IOException {
            rundeckJobDefinitionManager.createJobSerializer(job).serialize(format, outputStream)
        }

        @Override
        void serialize(final String format, final OutputStream os, final boolean preserveUuid, String sourceId)
                throws IOException
        {
            rundeckJobDefinitionManager.createJobSerializer(job).serialize(format, os, preserveUuid, sourceId)
        }
    }

    private JobSerializer lazySerializerForJob(ScheduledExecution job) {
        new LazySerializer(job: job, rundeckJobDefinitionManager: rundeckJobDefinitionManager)
    }


    List<JobScmReference> scmJobRefsForJobs(List<ScheduledExecution> jobs, Map<String, Map> jobsPluginMeta = null) {
        jobs.collect { ScheduledExecution job ->
            if(jobsPluginMeta){
                def jobPluginMeta = jobsPluginMeta.get(job.uuid)
                scmJobRef(job, null, jobPluginMeta)
            }else{
                scmJobRef(job, false)
            }

        }
    }

    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    JobScmReference scmJobRef(ScheduledExecution job, boolean exportIntegration) {
        def metadata = getJobPluginMeta(job, exportIntegration? STORAGE_NAME_EXPORT: STORAGE_NAME_IMPORT)
        scmJobRef(job, null, metadata)
    }

    /**
     * Get metadata for a single job
     * @param job
     * @return metadata map
     */
    public Map getJobPluginMeta(ScheduledExecution job, String type) {
        Map meta = jobMetadataService.getJobPluginMeta(job, type)

        if(type == STORAGE_NAME_EXPORT){
            def importPluginMeta = jobMetadataService.getJobPluginMeta(job, STORAGE_NAME_IMPORT)

            def importSourceId = null
            if(importPluginMeta && importPluginMeta["srcId"]){
                importSourceId = importPluginMeta["srcId"]
            }

            if(importSourceId){
                if (!meta){
                    meta = ["srcId":importSourceId]
                }else{
                    if(!meta["srcId"]) {
                        meta["srcId"] = importSourceId
                    }
                }
            }
        }

        meta
    }

    JobScmReference scmJobRef(ScheduledExecution job, JobSerializer serializer = null, Map metadata) {
        def impl = new JobImportReferenceImpl(
                jobRevReference(job),
                metadata?.version != null ? metadata.version : -1L,
                metadata?.pluginMeta,
                metadata?.srcId
        )
        impl.jobSerializer = serializer ?: lazySerializerForJob(job)
        impl
    }
    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    JobScmReference scmOrigJobRef(JobReference reference, boolean exportIntegration, JobSerializer serializer) {
        if (reference instanceof JobRevReference) {
            return scmJobRef((JobRevReference) reference, exportIntegration, (JobSerializer)serializer)
        } else {
            return scmJobRef(jobOrigRevReference(reference), exportIntegration, (JobSerializer) serializer, )
        }
    }
    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    JobScmReference scmJobRef(JobRevReference reference, boolean exportIntegration, JobSerializer serializer) {
        def metadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, exportIntegration?STORAGE_NAME_EXPORT:STORAGE_NAME_IMPORT)

        if(exportIntegration){
            def importMetadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, STORAGE_NAME_IMPORT)
            if(importMetadata && importMetadata["srcId"]){
                if(!metadata){
                    metadata = ["srcId": importMetadata["srcId"]]
                }else{
                    if(!metadata["srcId"]) {
                        metadata["srcId"] = importMetadata["srcId"]
                    }
                }
            }
        }


        scmJobRef(reference, metadata, serializer)
    }
    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    static JobScmReference scmJobRef(
            JobRevReference reference, Map metadata, JobSerializer serializer = null
    )
    {
        def impl = new JobImportReferenceImpl(
                reference,
                metadata?.version != null ? metadata.version : -1L,
                metadata?.pluginMeta,
                metadata?.srcId
        )
        impl.jobSerializer = serializer
        impl
    }

    /**
     * List of tracked items for the action
     * @param project
     * @param actionId
     * @return
     */
    List<ScmImportTrackedItem> getTrackingItemsForAction(String project, String actionId) {
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            return plugin.getTrackedItemsForAction(actionId)
        }
        null
    }
    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    def getPluginStatus(UserAndRolesAuthContext auth, String integration, String project) {
        switch (integration) {
            case EXPORT:
                return exportPluginStatus(auth, project)
            case IMPORT:
                return importPluginStatus(auth, project)
        }
    }
    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    ScmExportSynchState exportPluginStatus(UserAndRolesAuthContext auth, String project) throws ScmPluginException {
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            try{
                return plugin.getStatus(scmOperationContext(auth, project))
            }catch (Throwable t){
                log.error("Failed to get status for SCM export plugin in project ${project}: $t",t);
            }
        }
        null
    }
    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    ScmImportSynchState importPluginStatus(UserAndRolesAuthContext auth, String project) throws ScmPluginException {
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            return plugin.getStatus(scmOperationContext(auth, project))
        }
        null
    }
    /**
     * Get the actions for the plugin
     * @param project
     * @return
     */
    List<Action> exportPluginActions(UserAndRolesAuthContext auth, String project) {
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            def context = scmOperationContext(auth, project)
            return plugin.actionsAvailableForContext(context)
        }
        null
    }
    /**
     * Get the actions for the plugin
     * @param project
     * @return
     */
    List<Action> exportPluginActionsForJob(UserAndRolesAuthContext auth, ScheduledExecution job) {
        def plugin = getLoadedExportPluginFor job.project
        if (plugin) {
            def context = scmOperationContext(auth, job.project, job.extid)
            return plugin.actionsAvailableForContext(context)
        }
        null
    }

    /**
     * Get the actions for the plugin
     * @param project
     * @return
     */
    List<Action> importPluginActions(UserAndRolesAuthContext auth, String project, ScmImportSynchState status = null) {
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            def context = scmOperationContext(auth, project)
            return plugin.actionsAvailableForContext(context, status)
        }
        null
    }

    /**
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobState> exportStatusForJobs(String project, UserAndRolesAuthContext auth, List<ScheduledExecution> jobs, boolean runClusterFix = true, Map<String, Map> jobsPluginMeta = null) {
        def clusterMode = frameworkService.isClusterModeEnabled()

        if(jobs && jobs.size()>0 && clusterMode && runClusterFix){
            if(auth){
                fixExportStatus(auth, project, jobs, jobsPluginMeta)
            }
        }

        def status = [:]
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            jobs.each { job ->
                def jobPluginMeta = null
                if (!jobsPluginMeta) {
                    jobPluginMeta = getJobPluginMeta(job, STORAGE_NAME_EXPORT)
                } else {
                    jobPluginMeta = jobsPluginMeta.get(job.uuid)
                }

                def jobReference = exportJobRef(job, jobPluginMeta)

                def originalPath = getRenamedPathForJobId(jobReference.project, jobReference.id)
                JobState jobState = plugin.getJobStatus(jobReference, originalPath)
                status[jobReference.id] = jobState

                log.debug("Status for job ${jobReference}: ${status[jobReference.id]}, origpath: ${originalPath}")
            }
        }

        status
    }
    /**
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobState> exportStatusForJob(ScheduledExecution job) {
        def status = [:]
        def metadata = getJobPluginMeta(job, STORAGE_NAME_EXPORT)
        def jobReference = exportJobRef(job, metadata)
        def plugin = getLoadedExportPluginFor jobReference.project
        if (plugin) {
            def originalPath = getRenamedPathForJobId(jobReference.project, jobReference.id)
            status[jobReference.id] = plugin.getJobStatus(jobReference, originalPath, false)
            log.debug("Status for job ${jobReference}: ${status[jobReference.id]}, origpath: ${originalPath}")
        }

        status
    }
    /**
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobImportState> importStatusForJobs(String project, UserAndRolesAuthContext auth, List<ScheduledExecution> jobs,  boolean runClusterFix = true, Map<String, Map> jobsPluginMeta = null) {
        def status = [:]
        def clusterMode = frameworkService.isClusterModeEnabled()
        if(jobs && jobs.size()>0 && clusterMode && runClusterFix ){
            fixImportStatus(auth,project,jobs)
        }
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            jobs.collect { ScheduledExecution job ->
                def jobReference
                if(jobsPluginMeta){
                    def jobPluginMeta = jobsPluginMeta.get(job.uuid)
                    jobReference = scmJobRef(job, null, jobPluginMeta)
                }else{
                    jobReference = scmJobRef(job, false)
                }

                //TODO: deleted job paths?
//                def originalPath = getRenamedPathForJobId(jobReference.project, jobReference.id)
                status[jobReference.id] = plugin.getJobStatus(jobReference)
                log.debug("Status for job ${jobReference}: ${status[jobReference.id]},")
            }
        }
        status
    }
    /**
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobImportState> importStatusForJob(ScheduledExecution job) {
        def status = [:]
        def plugin = getLoadedImportPluginFor job.project
        if (plugin) {
            JobScmReference jobReference = scmJobRef(job, false)
            status[jobReference.id] = plugin.getJobStatus(jobReference)
            log.debug("Status for job ${jobReference}: ${status[jobReference.id]},")
        }
        status
    }
    /**
     * Return a map of [path: Map[id: jobid, jobNameAndGroup: string]]
     * @param jobs
     * @return
     */
    Map<String, Map> deletedExportFilesForProject(String project) {
        def deleted = []
        def plugin = getLoadedExportPluginFor project
        if (plugin) {
            deleted = plugin.deletedFiles
        }
        //create map of path -> job info
        def map = deleted?.collectEntries {
            [it, deletedJobForPath(project, it) ?: [:]]
        }
        log.debug "Deleted job map for ${project}: ${map}"
        return map?:[:]
    }

    def performExportAction(
            String actionId,
            UserAndRolesAuthContext auth,
            String project,
            Map config,
            List<ScheduledExecution> jobs,
            List<String> deletePaths
    )
    {
        log.debug("exportCommit project: ${project}, jobs: ${jobs}, deletePaths: ${deletePaths}")
        //store config
        def plugin = getLoadedExportPluginFor project
        def jobrefs = exportjobRefsForJobs(jobs)
        def context = scmOperationContext(auth, project)
        def view = plugin.getInputViewForAction(context, actionId)
        def report = validatePluginConfigProperties(project, view.properties, config)
        if (!report.valid) {
            return [valid: false, report: report]
        }
        ScmExportResult result = null
        try {
            result = plugin.export(context, actionId, jobrefs as Set, (deletePaths?:[]) as Set, config)

            if (result && result.success && result.commit) {
                //synch import commit info to exported commit data
                jobs.each { job ->
                    def orig = getJobPluginMeta(job, STORAGE_NAME_EXPORT) ?: [:]

                    def newmeta = [version: job.version, pluginMeta: result.commit.asMap(), name: job.jobName, groupPath: job.groupPath]

                    jobMetadataService.setJobPluginMeta(
                            job,
                            STORAGE_NAME_EXPORT,
                            orig + newmeta
                    )
                }
            }
        } catch (ScmPluginInvalidInput e) {
            return [valid: false, report: e.report]
        } catch (ScmPluginException e) {
            log.error(e.message)
            log.debug("export failed ${jobrefs}, ${deletePaths}, ${auth.username}, ${config}", e)
            if (ScmUserInfoMissing.isFieldMissing(e)) {
                def fieldReport = new Validator.Report()
                fieldReport.errors[ScmUserInfoMissing.missingFieldName(e)] = e.message
                return [
                        error               : true,
                        message             : e.message,
                        report              : fieldReport,
                        missingUserInfoField: ScmUserInfoMissing.missingFieldName(e)
                ]
            }
            return [error: true, message: e.message]
        }
        if (result.error) {
            return [error: true, message: result.message, extendedMessage: result.extendedMessage]
        }
        forgetDeletedPaths(project, deletePaths)
        forgetRenamedJobs(project, jobrefs*.id)
        log.debug("result: ${result}")
        [valid: true, commitId: result.id, message: result.message, extendedMessage: result.extendedMessage]
    }

    ScmUserInfo lookupUserInfo(final String username) {
        def user = User.findByLogin(username)
        return new ScmUser(
                userName: username,
                email: user?.email,
                firstName: user?.firstName,
                lastName: user?.lastName,
                fullName: user ? (user.firstName ?: '') + (user.lastName ? ' ' + user.lastName : '') : null,
                )
    }

    ScmDiffResult exportDiff(String project, ScheduledExecution job) {
        def metadata = getJobPluginMeta(job, STORAGE_NAME_EXPORT)
        def jobref = exportJobRef(job, metadata)
        def plugin = getLoadedExportPluginFor project
        plugin.getFileDiff(jobref, getRenamedPathForJobId(project, job.extid))
    }

    ScmImportDiffResult importDiff(String project, ScheduledExecution job) {
        def jobref = scmJobRef(job, false)
        def plugin = getLoadedImportPluginFor project
        plugin.getFileDiff(jobref, getRenamedPathForJobId(project, job.extid))
    }

    ScmOperationContext scmOperationContext(@DelegatesTo(ScmOperationContextBuilder) Closure clos) {
        def builder = ScmOperationContextBuilder.builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    def performImportAction(
            String actionId,
            UserAndRolesAuthContext auth,
            String project,
            Map config,
            List<String> chosenTrackedItems,
            List<String> jobIdsToDelete = null
    )
    {
        log.debug("performImportAction project: ${project}, items: ${chosenTrackedItems}")
        if(jobIdsToDelete){
            log.debug("Job IDs to delete: ${jobIdsToDelete}")
        }
        //store config
        def plugin = getLoadedImportPluginFor project
        def context = scmOperationContext(auth, project)
        def view = plugin.getInputViewForAction(context, actionId)
        def report = validatePluginConfigProperties(project, view.properties, config)
        if (!report.valid) {
            return [valid: false, report: report]
        }
        def isSetupAction = plugin.getSetupAction(context)?.id == actionId

        def result = null
        def jobImporter = new ResolvedJobImporter(context, scmJobImporter)

        try {
            if(!jobIdsToDelete){
                result = plugin.scmImport(context, actionId, jobImporter, chosenTrackedItems, config)
            }else{
                result = plugin.scmImport(context, actionId, jobImporter, chosenTrackedItems, jobIdsToDelete, config)
            }
        } catch (ScmPluginInvalidInput e) {
            return [valid: false, report: e.report]
        } catch (ScmPluginException e) {
            log.error(e.message)
            log.debug("import failed ${chosenTrackedItems}, ${config}", e)
            if (ScmUserInfoMissing.isFieldMissing(e)) {
                def fieldReport = new Validator.Report()
                fieldReport.errors[ScmUserInfoMissing.missingFieldName(e)] = e.message
                return [
                        error               : true,
                        message             : e.message,
                        report              : fieldReport,
                        missingUserInfoField: ScmUserInfoMissing.missingFieldName(e)
                ]
            }
            return [error: true, message: e.message]
        }
        if (result.error) {
            return [error: true, message: result.message, extendedMessage: result.extendedMessage]
        }

        if (isSetupAction) {
            //merge config with stored config
            saveInputTrackingSetupConfig(project, config, chosenTrackedItems)
        }


        log.debug("performInputAction: ${result}")
        [valid: true, commitId: result.id, message: result.message, extendedMessage: result.extendedMessage]
    }

    private void saveInputTrackingSetupConfig(String project, Map config, List<String> chosenTrackedItems) {
        def pluginConfig = pluginConfigService.loadScmConfig(project, pathForConfigFile(IMPORT), IMPORT_PREF)
        pluginConfig.setConfig(config)
        pluginConfig.setSetting('trackedItems', chosenTrackedItems)
        storeConfig(pluginConfig, project, IMPORT)
    }

    private List<String> loadInputTrackingItems(String project) {
        def pluginConfig = pluginConfigService.loadScmConfig(project, pathForConfigFile(IMPORT), IMPORT_PREF)

        return pluginConfig.getSettingList('trackedItems')
    }

    public fixExportStatus(UserAndRolesAuthContext auth, String project, List<ScheduledExecution> jobs, Map<String, Map> jobsPluginMeta = null){
        if(jobs && jobs.size()>0){
            def plugin = getLoadedExportPluginFor project
            if(plugin) {
                def context = scmOperationContext(auth, project)
                def joblist = exportjobRefsForJobs(jobs, jobsPluginMeta)
                def originalPaths = joblist.collectEntries{[it.id,getRenamedPathForJobId(it.project, it.id)]}
                plugin?.clusterFixJobs(context, joblist, originalPaths)
            }
        }
    }

    public fixImportStatus(UserAndRolesAuthContext auth, String project, List<ScheduledExecution> jobs){
        if(jobs && jobs.size()>0){
            def plugin = getLoadedImportPluginFor project
            if(plugin) {
                def context = scmOperationContext(auth, project)
                def joblist = scmJobRefsForJobs(jobs)
                def originalPaths = joblist.collectEntries{[it.id,getRenamedPathForJobId(it.project, it.id)]}
                plugin?.clusterFixJobs(context, joblist, originalPaths)
            }
        }
    }

    Map<String, Map> getJobsPluginMeta(String project, boolean isExport){
        String type = isExport ? STORAGE_NAME_EXPORT:STORAGE_NAME_IMPORT
        Map<String, Map> pluginMeta = getJobsPluginMeta(project, type)
        if(isExport){
            // check if import plugin has sourceId set
            Map<String, Map> importPluginMeta = getJobsPluginMeta(project, STORAGE_NAME_IMPORT)
            importPluginMeta?.each {id, meta->
                if(pluginMeta.get(id)){
                    def exportMeta = pluginMeta.get(id)
                    if(meta["srcId"] && !exportMeta["srcId"]){
                        exportMeta["srcId"] = meta["srcId"]
                        pluginMeta.put(id, exportMeta)
                    }
                }else{
                    if(meta["srcId"]){
                        pluginMeta.put(id, ["srcId": meta["srcId"]])
                    }
                }

            }
        }
        pluginMeta


    }

    Map<String, Map> getJobsPluginMeta(String project, String type){
        List jobsPluginMeta = jobMetadataService.getJobsPluginMeta(project, type)
        jobsPluginMeta.collectEntries{[it.key.replace("/" + type,""),it.pluginData]}
    }

    // check if jobs has the SCM metadata stored in the DB
    def checkExportJobStatus(ScmExportPlugin plugin,ScheduledExecution job, JobScmReference jobReference, def jobPluginMeta, JobState jobStatus){

        //need to save status in DB
        if(jobReference.scmImportMetadata == null){
            if(jobStatus.commit){
                def newmeta = [name: job.getJobName(),groupPath: job.getGroupPath(),version: job.version, pluginMeta: jobStatus.commit.asMap()]
                if(jobReference.sourceId){
                    newmeta.put("srcId", jobReference.sourceId)
                }
                jobMetadataService.setJobPluginMeta(
                        job,
                        STORAGE_NAME_EXPORT,
                        newmeta
                )
            }
        }else{
            //check if the name is set
            if(jobPluginMeta.name==null){
                def newmeta = [name: job.getJobName(), groupPath: job.getGroupPath()]
                jobMetadataService.setJobPluginMeta(
                        job,
                        STORAGE_NAME_EXPORT,
                        jobPluginMeta + newmeta
                )
            }
        }
    }

    //check if the job was renamed
    def checkExportJobRenamed(ScmExportPlugin plugin, String project, ScheduledExecution job, JobScmReference jobReference, def jobPluginMeta){
        def jobFullName = job.generateFullName()
        def origFullName = [jobPluginMeta.groupPath?:'',jobPluginMeta.name].join("/")

        if(jobPluginMeta && jobPluginMeta.name && jobFullName != origFullName){

            log.debug("job ${job.groupPath}/${job.jobName} was renamed, previuos name: ${jobPluginMeta.groupPath}/${jobPluginMeta.name}" )

            boolean renameProcess = true
            if (renamedJobsCache && renamedJobsCache[project] && renamedJobsCache[project][jobReference.id] ){
                renameProcess = false
            }

            if(renameProcess){
                def origScmRef = (JobScmReference)exportJobRef(job,jobPluginMeta)
                origScmRef.jobName = jobPluginMeta.name
                origScmRef.groupPath = jobPluginMeta.groupPath


                //record original path for renamed job, if it is different
                def origpath = plugin.getRelativePathForJob(origScmRef)
                recordRenamedJob(project, origScmRef.id, origpath)

                plugin.jobChanged(
                        new StoredJobChangeEvent(
                                eventType: JobChangeEvent.JobChangeEventType.MODIFY_RENAME,
                                originalJobReference: origScmRef,
                                jobReference: jobReference
                        ),
                        jobReference
                )
            }
        }

    }

    def getExportPushActionId(project){
        def plugin = getLoadedExportPluginFor project
        return plugin?.getExportPushActionId()
    }

    def refreshExportPluginMetadata(String project, ScmExportPlugin plugin, List<ScheduledExecution> jobs, Map<String, Map> jobsPluginMeta ){
        jobs.each { job ->
            Map jobPluginMeta =  jobsPluginMeta.get(job.uuid)
            JobExportReference jobReference = exportJobRef(job, jobPluginMeta)

            if(jobPluginMeta){
                //check if job was renamed
                checkExportJobRenamed(plugin, project, job, (JobScmReference)jobReference, jobPluginMeta)
            }

            String originalPath = getRenamedPathForJobId(jobReference.project, jobReference.id)
            JobState jobState = plugin.getJobStatus(jobReference, originalPath)

            // synch commit info to exported commit data
            checkExportJobStatus(plugin, job, (JobScmReference)jobReference, jobPluginMeta, jobState)
        }
    }

}




