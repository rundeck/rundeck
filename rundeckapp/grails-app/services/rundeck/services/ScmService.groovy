package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobImportReference
import com.dtolabs.rundeck.plugins.scm.JobImportState
import com.dtolabs.rundeck.plugins.scm.JobImporter
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
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
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.ScmImportPluginProviderService
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.services.scm.ContextJobImporter
import rundeck.services.scm.ResolvedJobImporter
import rundeck.services.scm.ScmJobImporter
import rundeck.services.scm.ScmPluginConfig
import rundeck.services.scm.ScmUser

/**
 * Manages scm integration
 */
class ScmService {
    def JobEventsService jobEventsService
    def ContextJobImporter scmJobImporter
    def grailsApplication
    def frameworkService
    ScmExportPluginProviderService scmExportPluginProviderService
    ScmImportPluginProviderService scmImportPluginProviderService
    PluginService pluginService
    ScheduledExecutionService scheduledExecutionService
    JobMetadataService jobMetadataService
    def StorageService storageService
    Map<String, ScmExportPlugin> loadedExportPlugins = Collections.synchronizedMap([:])
    Map<String, ScmImportPlugin> loadedImportPlugins = Collections.synchronizedMap([:])
    Map<String, JobChangeListener> loadedExportListeners = Collections.synchronizedMap([:])
    Map<String, JobChangeListener> loadedImportListeners = Collections.synchronizedMap([:])

    def initialize() {
        def projects = frameworkService.projectNames()
        projects.each { String project ->

            //load export plugins

            def pluginConfig = loadScmConfig(project, 'export')
            if (pluginConfig && pluginConfig.enabled) {
                log.debug("Loading 'export' plugin ${pluginConfig.type} for ${project}...")
                def username = pluginConfig.getSetting("username")
                def roles = pluginConfig.getSettingList("roles")
                if (!username || !roles) {
                    log.error(
                            "SCM Export not valid (missing username or roles) for project: ${project}: ${pluginConfig}"
                    )
                } else {
                    try {
                        def context = scmOperationContext(username, roles, project)
                        initPlugin('export', context, pluginConfig.type, pluginConfig.config)
                    } catch (ScmPluginException e) {
                        log.error(
                                "Failed to initialize SCM Export plugin ${pluginConfig.type} for ${project}: ${e.message}",
                                e
                        )
                    }
                }
                //TODO: refresh status of all jobs in project?
            } else {
                log.debug("SCM Export not setup for project: ${project}: ${pluginConfig}")
            }


            def importPluginConfig = loadScmConfig(project, 'import')
            if (importPluginConfig && importPluginConfig.enabled) {
                log.debug("Loading 'import' plugin ${importPluginConfig.type} for ${project}...")
                def username = importPluginConfig.getSetting("username")
                def roles = importPluginConfig.getSettingList("roles")
                if (!username || !roles) {
                    log.error(
                            "SCM Import not valid (missing username or roles) for project: ${project}: ${pluginConfig}"
                    )
                } else {
                    try {
                        def context = scmOperationContext(username, roles, project)
                        initPlugin('import', context, importPluginConfig.type, importPluginConfig.config)
                    } catch (ScmPluginException e) {
                        log.error(
                                "Failed to initialize SCM Import plugin ${importPluginConfig.type} for ${project}: ${e.message}",
                                e
                        )
                    }
                }
                //TODO: refresh status of all jobs in project?
            } else {
                log.debug("SCM Import not setup for project: ${project}: ${importPluginConfig}")
            }
        }
    }

    def listPlugins(String integration) {
        switch (integration) {
            case 'export':
                return pluginService.listPlugins(ScmExportPluginFactory, scmExportPluginProviderService)
            case 'import':
                return pluginService.listPlugins(ScmImportPluginFactory, scmImportPluginProviderService)
        }
    }

    def getPluginDescriptor(String integration, String type) {
        switch (integration) {
            case 'export':
                return getExportPluginDescriptor(type)
            case 'import':
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
            case 'import':
                return projectHasConfiguredImportPlugin(project)
            case 'export':
                return projectHasConfiguredExportPlugin(project)
        }
    }

    def projectHasConfiguredExportPlugin(String project) {
        loadedExportPlugins.containsKey(project)
    }

    def projectHasConfiguredImportPlugin(String project) {
        loadedImportPlugins.containsKey(project)
    }

    BasicInputView getInputView(UserAndRolesAuthContext auth, String integration, String project, String actionId) {
        switch (integration) {
            case 'export':
                return getExportInputView(auth, project, actionId)
            case 'import':
                return getImportInputView(auth, project, actionId)
        }

    }

    BasicInputView getExportInputView(UserAndRolesAuthContext auth, String project, String actionId) {
        def plugin = loadedExportPlugins[project]
        if (plugin) {
            def context = scmOperationContext(auth, project)
            plugin.getInputViewForAction(context, actionId)
        }
    }

    BasicInputView getImportInputView(UserAndRolesAuthContext auth, String project, String actionId) {
        def plugin = loadedImportPlugins[project]
        if (plugin) {
            def context = scmOperationContext(auth, project)
            plugin.getInputViewForAction(context, actionId)
        }
    }

    def getSetupProperties(String integration, String project, String type) {
        switch (integration) {
            case 'import':
                return getImportSetupProperties(project, type)
            case 'export':
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

    ScmPluginConfig loadScmConfig(String project, String integration) {
        def project1 = frameworkService.getFrameworkProject(project)
        def configPath = storedConfigFile(integration)
        if (!project1.existsFileResource(configPath)) {
            return null
        }
        def baos = new ByteArrayOutputStream()
        project1.loadFileResource(configPath, baos)
        return ScmPluginConfig.loadFromStream(integration, new ByteArrayInputStream(baos.toByteArray()))
    }

    private String storedConfigFile(String integration) {
        if (frameworkService.serverUUID) {
            return "${frameworkService.serverUUID}/etc/scm-${integration}.properties"
        }
        "etc/scm-${integration}.properties"
    }


    def storeConfig(ScmPluginConfig scmPluginConfig, String project) {
        def project1 = frameworkService.getFrameworkProject(project)
        def configPath = storedConfigFile(scmPluginConfig.integration)
        project1.storeFileResource configPath, scmPluginConfig.asInputStream()
    }

    def ValidatedPlugin validatePluginSetup(String integration, String project, String name, Map config) {
        switch (integration) {
            case 'export':
                return validateExportPluginSetup(project, name, config)
            case 'import':
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
     * Return a map of status for jobs
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
    private def initPlugin(String integration, ScmOperationContext context, String type, Map config) {
        def validation = validatePluginSetup(integration, context.frameworkProject, type, config)
        if (!validation.valid) {
            throw new ScmPluginException("Validation failed for ${type} plugin: " + validation.report)
        }
        def loaded = loadPluginWithConfig(integration, context, type, config)

        JobChangeListener changeListener
        if (integration == 'export') {
            ScmExportPlugin plugin = loaded
            changeListener = { JobChangeEvent event, JobSerializer serializer ->
                log.debug("job change event: " + event)
                if (event.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
                    //record deleted path
                    recordDeletedJob(
                            context.frameworkProject,
                            plugin.getRelativePathForJob(event.jobReference),
                            [
                                    id             : event.jobReference.id,
                                    jobNameAndGroup: event.jobReference.getJobName(),
                            ]
                    )
                } else if (event.eventType == JobChangeEvent.JobChangeEventType.MODIFY_RENAME) {
                    //record original path for renamed job, if it is different
                    def origpath = plugin.getRelativePathForJob(event.originalJobReference)
                    def newpath = plugin.getRelativePathForJob(event.jobReference)
                    if (origpath != newpath) {
                        recordRenamedJob(context.frameworkProject, event.jobReference.id, origpath)
                    }
                }
                plugin.jobChanged(event, scmJobRef(event.jobReference, serializer))
            } as JobChangeListener
        } else {
            ScmImportPlugin plugin = loaded
            changeListener = { JobChangeEvent event, JobSerializer serializer ->
                log.debug("job change event: " + event)
                plugin.jobChanged(event, scmJobRef(event.jobReference, serializer))
                if(event.eventType==JobChangeEvent.JobChangeEventType.DELETE){
                    jobMetadataService.removeJobPluginMetaAll(event.jobReference.project,event.jobReference.id)
                }
            } as JobChangeListener
        }
        if (integration == 'export') {
            loadedExportPlugins[context.frameworkProject] = loaded
            loadedExportListeners[context.frameworkProject] = changeListener
        } else {
            loadedImportPlugins[context.frameworkProject] = loaded
            loadedImportListeners[context.frameworkProject] = changeListener
        }
        jobEventsService.addListener changeListener
        loaded
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

        def scmPluginConfig = new ScmPluginConfig(new Properties(), integration)
        scmPluginConfig.config = config
        scmPluginConfig.type = type
        scmPluginConfig.enabled = true
        scmPluginConfig.setSetting("username", auth.username)
        scmPluginConfig.setSetting("roles", auth.roles as List)
        storeConfig(scmPluginConfig, project)
        try {
            def context = scmOperationContext(auth, project)
            def plugin = initPlugin(integration, context, type, config)
            if (integration == 'import') {
                def nextAction = plugin.getSetupAction(context)
                if (nextAction) {
                    return [valid: true, plugin: plugin, nextAction: nextAction]
                }
            }
            return [valid: true, plugin: plugin]
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
    ScmOperationContext scmOperationContext(UserAndRolesAuthContext auth, String project, String job=null) {
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
        scmOperationContext(frameworkService.getAuthContextForUserAndRoles(username, roles), project)
    }

    /**
     * Disable a previously enabled and configured plugin
     * @param integration integration name
     * @param project project name
     * @param type plugin type
     * @return
     */
    def disablePlugin(String integration, String project, String type) {
        ScmPluginConfig scmPluginConfig = loadScmConfig(project, integration)

        scmPluginConfig.enabled = false
        storeConfig(scmPluginConfig, project)


        def loaded
        if (integration == 'export') {
            loaded = loadedExportPlugins.remove(project)
            def changeListener = loadedExportListeners.remove(project)
            jobEventsService.removeListener(changeListener)
            //clear cached rename/delete info
            renamedJobsCache.remove(project)
            deletedJobsCache.remove(project)
        } else {
            loaded = loadedImportPlugins.remove(project)
            def changeListener = loadedImportListeners.remove(project)
            jobEventsService.removeListener(changeListener)
        }
        loaded?.cleanup()

    }

    /**
     * Enable a disabled plugin
     * @param project
     * @param integration integration name
     * @param type plugin type
     * @return a map [valid:true/false, report: Report, plugin: instance], will
     */
    def enablePlugin(UserAndRolesAuthContext auth, String integration, String project, String type) {
        ScmPluginConfig scmPluginConfig = loadScmConfig(project, integration)
        def validation = validatePluginSetup(integration, project, type, scmPluginConfig.config)
        if (!validation.valid) {
            return [valid: false, report: validation.report]
        }

        try {
            def context = scmOperationContext(auth, project)
            def plugin = initPlugin(integration, context, type, scmPluginConfig.config)
            scmPluginConfig.enabled = true
            storeConfig(scmPluginConfig, project)
            if (integration == 'import') {
                def nextAction = plugin.getSetupAction(context)
                if (nextAction) {
                    return [valid: true, plugin: plugin, nextAction: nextAction]
                }
            }
            return [valid: true, plugin: plugin]
        } catch (ScmPluginException e) {
            return [error: true, message: e.message]
        }
    }

    def loadPluginWithConfig(String integration, ScmOperationContext context, String type, Map config) {
        switch (integration) {
            case 'export':
                return loadExportPluginWithConfig(context, type, config)
            case 'import':
                return loadImportPluginWithConfig(context, type, config)
        }

    }

    ScmExportPlugin loadExportPluginWithConfig(ScmOperationContext context, String type, Map config) {
        ScmExportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmExportPluginProviderService
        )
        try {
            return plugin.createPlugin(context, config)
        } catch (ConfigurationException e) {
            throw new ScmPluginException(e)
        }
    }

    ScmImportPlugin loadImportPluginWithConfig(ScmOperationContext context, String type, Map config) {
        ScmImportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmImportPluginProviderService
        )
        def list = loadInputTrackingItems(context.frameworkProject)
        try {
            return plugin.createPlugin(context, config, list)
        } catch (ConfigurationException e) {
            throw new ScmPluginException(e)
        }
    }

    /**
     * @param jobs list of {@link ScheduledExecution} objects
     * @return map of job ID to file path
     */
    Map<String, String> exportFilePathsMapForJobs(List<ScheduledExecution> jobs) {
        exportFilePathsMapForJobRefs(jobRefsForJobs(jobs))
    }
    /**
     * @param refs list of {@link JobRevReference} objects
     * @return map of job ID to file path
     */
    Map<String, String> exportFilePathsMapForJobRefs(List<JobRevReference> refs) {
        def files = [:]
        refs.each { JobRevReference jobReference ->
            def plugin = loadedExportPlugins[jobReference.project]
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

    List<JobExportReference> exportjobRefsForJobs(List<ScheduledExecution> jobs) {
        jobs.collect { ScheduledExecution job ->
            exportJobRef(job)
        }
    }

    private JobExportReference exportJobRef(ScheduledExecution job) {
        new JobSerializerReferenceImpl(
                jobRevReference(job),
                lazySerializerForJob(job)
        )
    }

    private JobSerializer lazySerializerForJob(ScheduledExecution job) {
        { String format, OutputStream os ->
            switch (format) {
                case 'xml':
                    def str = job.encodeAsJobsXML() + '\n'
                    os.write(str.getBytes("UTF-8"))
                    break;
                case 'yaml':
                    def str = job.encodeAsJobsYAML() + '\n'
                    os.write(str.getBytes("UTF-8"))
                    break;
                default:
                    throw new IllegalArgumentException("Format not supported: " + format)
            }
        } as JobSerializer
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
                metadata?.pluginMeta
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
                metadata?.pluginMeta
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
    JobScmReference scmJobRef(JobRevReference reference, JobSerializer serializer) {
        def metadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, 'scm-import')
        def impl = new JobImportReferenceImpl(
                reference,
                metadata?.version != null ? metadata.version : -1L,
                metadata?.pluginMeta
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
    static JobScmReference scmJobRef(JobRevReference reference, Map metadata, JobSerializer serializer=null
    ) {
//        def metadata = jobMetadataService.getJobPluginMeta(reference.project, reference.id, 'scm-import')
        def impl = new JobImportReferenceImpl(
                reference,
                metadata?.version != null ? metadata.version : -1L,
                metadata?.pluginMeta
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
        def plugin = loadedImportPlugins[project]
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
            case 'export':
                return exportPluginStatus(auth, project)
            case 'import':
                return importPluginStatus(auth, project)
        }
    }
    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    ScmExportSynchState exportPluginStatus(UserAndRolesAuthContext auth, String project) throws ScmPluginException {

        def plugin = loadedExportPlugins[project]
        if (plugin) {
            return plugin.getStatus(scmOperationContext(auth, project))
        }
        null
    }
    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    ScmImportSynchState importPluginStatus(UserAndRolesAuthContext auth, String project) throws ScmPluginException {
        def plugin = loadedImportPlugins[project]
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

        def plugin = loadedExportPlugins[project]
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

        def plugin = loadedExportPlugins[job.project]
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

        def plugin = loadedImportPlugins[project]
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
    Map<String, JobState> exportStatusForJobs(List<ScheduledExecution> jobs) {
        def status = [:]
        exportjobRefsForJobs(jobs).each { jobReference ->
            def plugin = loadedExportPlugins[jobReference.project]
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
    Map<String, JobImportState> importStatusForJobs(List<ScheduledExecution> jobs) {
        def status = [:]
        scmJobRefsForJobs(jobs).each { JobScmReference jobReference ->
            def plugin = loadedImportPlugins[jobReference.project]
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
    Map<String, Map> deletedExportFilesForProject(String project) {
        def deleted = []
        def plugin = loadedExportPlugins[project]
        if (plugin) {
            deleted = plugin.deletedFiles
        }
        //create map of path -> job info
        def map = deleted.collectEntries {
            [it, deletedJobForPath(project, it) ?: [:]]
        }
        log.debug "Deleted job map for ${project}: ${map}"
        return map
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
        def plugin = loadedExportPlugins[project]
        def jobrefs = exportjobRefsForJobs(jobs)
        def context = scmOperationContext(auth, project)
        def view = plugin.getInputViewForAction(context, actionId)
        def report = validatePluginConfigProperties(project, view.properties, config)
        if (!report.valid) {
            return [valid: false, report: report]
        }
        def result = null
        try {
            result = plugin.export(context, actionId, jobrefs as Set, deletePaths as Set, config)
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
            return [error: true, message: result.message]
        }
        forgetDeletedPaths(project, deletePaths)
        forgetRenamedJobs(project, jobrefs*.id)
        log.debug("result: ${result}")
        [valid: true, commitId: result.id, message: result.message]
    }

    ScmUserInfo lookupUserInfo(final String username) {
        def user = User.findByLogin(username)
        if (user) {
            return new ScmUser(
                    userName: username,
                    email: user.email,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    fullName: (user.firstName ?: '') + (user.lastName ? ' ' + user.lastName : ''),
                    )
        }
        throw new IllegalArgumentException("Could not find a user profile for ${username}")
    }

    ScmDiffResult exportDiff(String project, ScheduledExecution job) {
        def jobref = exportJobRef(job)
        def plugin = loadedExportPlugins[project]
        plugin.getFileDiff(jobref, getRenamedPathForJobId(project, job.extid))
    }

    ScmDiffResult importDiff(String project, ScheduledExecution job) {
        def jobref = scmJobRef(job)
        def plugin = loadedImportPlugins[project]
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
            List<String> chosenTrackedItems
    )
    {
        log.debug("performImportAction project: ${project}, items: ${chosenTrackedItems}")
        //store config
        def plugin = loadedImportPlugins[project]
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
            result = plugin.scmImport(context, actionId, jobImporter, chosenTrackedItems, config)
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
            return [error: true, message: result.message]
        }

        if (isSetupAction) {
            //merge config with stored config
            saveInputTrackingSetupConfig(project, config, chosenTrackedItems)
        }


        log.debug("performInputAction: ${result}")
        [valid: true, commitId: result.id, message: result.message]
    }

    private void saveInputTrackingSetupConfig(String project, Map config, List<String> chosenTrackedItems) {
        def pluginConfig = loadScmConfig(project, 'import')
        pluginConfig.setConfig(config)
        pluginConfig.setSetting('trackedItems', chosenTrackedItems)
        storeConfig(pluginConfig, project)
    }

    private List<String> loadInputTrackingItems(String project) {
        def pluginConfig = loadScmConfig(project, 'import')

        return pluginConfig.getSettingList('trackedItems')
    }


}




