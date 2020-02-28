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
import org.rundeck.app.components.RundeckJobDefinitionManager
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
    public static final Map<String, String> PREFIXES = [export: EXPORT_PREF, import: IMPORT_PREF]
    public static final ArrayList<String> INTEGRATIONS = [EXPORT, IMPORT]


    def JobEventsService jobEventsService
    def ContextJobImporter scmJobImporter
    def grailsApplication
    def frameworkService
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
            //TODO: refresh status of all jobs in project?
        }
    }
    def initProject(String project, String integration){
        synchronized (initedProjects) {
            if (initedProjects.contains(integration + '/' + project)) {
                return true
            }
            initedProjects.add(integration + '/' + project)
        }
        def pluginConfig = loadScmConfig(project, integration)
        if (!pluginConfig?.enabled) {
            return false
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
            initPlugin(integration, context, pluginConfig.type, pluginConfig.config)
            return true
        } catch (Throwable e) {
            log.error(
                    "Failed to initialize SCM ${integration} plugin ${pluginConfig.type} for ${project}: ${e.message}",
                    e
            )
        }
    }
    ScmExportPlugin getLoadedExportPluginFor(String project){
        initProject(project)
        loadedExportPlugins[project]?.provider
    }
    ScmImportPlugin getLoadedImportPluginFor(String project){
        initProject(project)
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
        initProject(project)
        loadedExportPlugins.containsKey(project)
    }

    def projectHasConfiguredImportPlugin(String project) {
        initProject(project)
        loadedImportPlugins.containsKey(project)
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
        log.debug "Get renamed path for project ${project}, job: ${jobid}: " + renamedJobsCache[project]?.get(jobid)
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
     * @return
     */
    def initPlugin(String integration, ScmOperationContext context, String type, Map config) {
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
            def scmRef = scmJobRef(event.jobReference, serializer)
            def origScmRef = event.originalJobReference?scmOrigJobRef(event.originalJobReference, null):null
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

    private JobChangeListener listenerForExportPlugin(
            ScmExportPlugin plugin,
            ScmOperationContext context
    )
    {
        { JobChangeEvent event, JobSerializer serializer ->
            log.debug("job change event: " + event)
            if(event.eventType == JobChangeEvent.JobChangeEventType.CREATE){
                def metadata = jobMetadataService.getJobPluginMeta(event.jobReference.project, event.jobReference.id, 'scm-import')
                //generate "source" UUID in case the local UUID will not be exported
                jobMetadataService.setJobPluginMeta(event.jobReference.project, event.jobReference.id, 'scm-import',[
                        srcId:(metadata?.srcId)?:UUID.randomUUID().toString()
                ])
            }
            JobScmReference scmRef = scmJobRef(event.jobReference, serializer)
            JobScmReference origScmRef = event.originalJobReference?scmOrigJobRef(event.originalJobReference, null):null
            if (event.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
                //record deleted path
                recordDeletedJob(
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
                    recordRenamedJob(context.frameworkProject, event.jobReference.id, origpath)
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
        } as JobChangeListener
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
        scmOperationContext(frameworkService.getAuthContextForUserAndRolesAndProject(username, roles, project), project)
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
        def loaded = loadPluginWithConfig(integration, context, type, scmPluginConfig.config)
        loaded?.provider?.totalClean()

    }




    /**
     * Disable and remove all plugins for a project
     * @param project project
     * @param type type
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
        loaded?.provider?.cleanup()
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

    def loadPluginWithConfig(String integration, ScmOperationContext context, String type, Map config) {
        switch (integration) {
            case EXPORT:
                return loadExportPluginWithConfig(context, type, config)
            case IMPORT:
                return loadImportPluginWithConfig(context, type, config)
        }

    }

    CloseableProvider<ScmExportPlugin> loadExportPluginWithConfig(
            ScmOperationContext context,
            String type,
            Map config
    )
    {
        CloseableProvider<ScmExportPluginFactory> providerReference = pluginService.retainPlugin(
                type,
                scmExportPluginProviderService
        )
        def plugin = providerReference.provider.createPlugin(context, config)
        return Closeables.closeableProvider(plugin, providerReference)
    }

    CloseableProvider<ScmImportPlugin> loadImportPluginWithConfig(
            ScmOperationContext context,
            String type,
            Map config
    )
    {
        CloseableProvider<ScmImportPluginFactory> providerReference = pluginService.retainPlugin(
                type,
                scmImportPluginProviderService
        )

        def list = loadInputTrackingItems(context.frameworkProject)

        def plugin = providerReference.provider.createPlugin(context, config, list)
        return Closeables.closeableProvider(plugin, providerReference)
    }

    /**
     * @param jobs list of {@link ScheduledExecution} objects
     * @return map of job ID to file path
     */
    Map<String, String> exportFilePathsMapForJobs(List<ScheduledExecution> jobs) {
        def files = [:]
        jobs.each { ScheduledExecution job ->
            def plugin = getLoadedExportPluginFor job.project
            if (plugin) {
                files[job.extid] = plugin.getRelativePathForJob(scmJobRef(job))
            }
        }
        files
    }
    /**
     * @param refs list of {@link JobRevReference} objects
     * @return map of job ID to file path
     */
    Map<String, String> exportFilePathsMapForJobRefs(List<JobRevReference> refs) {
        def files = [:]
        refs.each { JobRevReference jobReference ->
            def plugin = getLoadedExportPluginFor jobReference.project
            if (plugin) {
                files[jobReference.id] = plugin.getRelativePathForJob(jobReference)
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

    List<JobExportReference> exportjobRefsForJobs(List<ScheduledExecution> jobs) {
        jobs.collect { ScheduledExecution job ->
            exportJobRef(job)
        }
    }

    private JobExportReference exportJobRef(ScheduledExecution job) {
        scmJobRef(job)
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

    List<JobImportReference> importJobRefsForJobs(List<ScheduledExecution> jobs) {
        jobs.collect { ScheduledExecution job ->
            importJobRef(job)
        }
    }

    List<JobScmReference> scmJobRefsForJobs(List<ScheduledExecution> jobs) {
        jobs.collect { ScheduledExecution job ->
            scmJobRef(job)
        }
    }

    /**
     * Create iport ref with import metadata
     * @param job
     * @return
     */
    private JobImportReference importJobRef(ScheduledExecution job) {
        def metadata = jobMetadataService.getJobPluginMeta(job, 'scm-import')
        new JobImportReferenceImpl(
                jobRevReference(job),
                metadata?.version != null ? metadata.version : -1L,
                metadata?.pluginMeta,
                metadata?.srcId
        )
    }

    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    JobScmReference scmJobRef(ScheduledExecution job, JobSerializer serializer = null) {
        def metadata = jobMetadataService.getJobPluginMeta(job, 'scm-import')
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
    JobScmReference scmOrigJobRef(JobReference reference, JobSerializer serializer) {
        if (reference instanceof JobRevReference) {
            return scmJobRef((JobRevReference) reference, (JobSerializer)serializer)
        } else {
            return scmJobRef(jobOrigRevReference(reference), (JobSerializer) serializer)
        }
    }
    /**
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    JobScmReference scmJobRef(JobRevReference reference, JobSerializer serializer) {
        def metadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, 'scm-import')
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
     * Create reference for job with scm import metadata and serializer
     * @param job job
     * @param serializer predefined serializer, or null to create lazy serializer
     * @return JobScmReference
     */
    static JobScmReference scmJobRef(
            JobRevReference reference, Map metadata, JobSerializer serializer = null
    )
    {
//        def metadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, 'scm-import')
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
    List<Action> importPluginActions(UserAndRolesAuthContext auth, String project) {
        def plugin = getLoadedImportPluginFor project
        if (plugin) {
            def context = scmOperationContext(auth, project)
            return plugin.actionsAvailableForContext(context)
        }
        null
    }

    /**
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobState> exportStatusForJobs(UserAndRolesAuthContext auth, List<ScheduledExecution> jobs) {
        def status = [:]
        def clusterMode = frameworkService.isClusterModeEnabled()
        if(jobs && jobs.size()>0 && clusterMode){
            def project = jobs.get(0).project
            if(auth){
                fixExportStatus(auth, project, jobs)
            }
        }

        exportjobRefsForJobs(jobs).each { jobReference ->
            def plugin = getLoadedExportPluginFor jobReference.project
            if (plugin) {
                def originalPath = getRenamedPathForJobId(jobReference.project, jobReference.id)
                status[jobReference.id] = plugin.getJobStatus(jobReference, originalPath)
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
        def jobReference = exportJobRef(job)
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
    Map<String, JobImportState> importStatusForJobs(UserAndRolesAuthContext auth, List<ScheduledExecution> jobs) {
        def status = [:]
        def clusterMode = frameworkService.isClusterModeEnabled()
        if(jobs && jobs.size()>0 && clusterMode){
            def project = jobs.get(0).project
            fixImportStatus(auth,project,jobs)
        }
        scmJobRefsForJobs(jobs).each { JobScmReference jobReference ->
            def plugin = getLoadedImportPluginFor jobReference.project
            if (plugin) {
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
        JobScmReference jobReference = scmJobRef(job)
        def plugin = getLoadedImportPluginFor jobReference.project
        if (plugin) {
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
                    def orig = jobMetadataService.getJobPluginMeta(job, 'scm-import') ?: [:]

                    def newmeta = [version: job.version, pluginMeta: result.commit.asMap()]

                    jobMetadataService.setJobPluginMeta(
                            job,
                            'scm-import',
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
        def jobref = exportJobRef(job)
        def plugin = getLoadedExportPluginFor project
        plugin.getFileDiff(jobref, getRenamedPathForJobId(project, job.extid))
    }

    ScmImportDiffResult importDiff(String project, ScheduledExecution job) {
        def jobref = scmJobRef(job)
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

    public fixExportStatus(UserAndRolesAuthContext auth, String project, List<ScheduledExecution> jobs){
        if(jobs && jobs.size()>0){
            def plugin = getLoadedExportPluginFor project
            if(plugin) {
                def context = scmOperationContext(auth, project)
                def joblist = exportjobRefsForJobs(jobs)
                plugin?.clusterFixJobs(context, joblist)
            }
        }
    }

    public fixImportStatus(UserAndRolesAuthContext auth, String project, List<ScheduledExecution> jobs){
        if(jobs && jobs.size()>0){
            def plugin = getLoadedImportPluginFor project
            if(plugin) {
                def context = scmOperationContext(auth, project)
                def joblist = scmJobRefsForJobs(jobs)
                plugin?.clusterFixJobs(context, joblist)
            }
        }
    }

}




