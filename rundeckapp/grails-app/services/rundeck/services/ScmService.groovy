package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmExportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import com.dtolabs.rundeck.plugins.scm.ScmUserInfoMissing
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import rundeck.ScheduledExecution
import rundeck.User

/**
 * Manages scm integration
 */
class ScmService {
    def JobEventsService jobEventsService
    def grailsApplication
    def frameworkService
    ScmExportPluginProviderService scmExportPluginProviderService
    PluginService pluginService
    Map<String, ScmExportPlugin> loadedExportPlugins = Collections.synchronizedMap([:])
    Map<String, JobChangeListener> loadedExportListeners = Collections.synchronizedMap([:])

    def initialize() {
        def projects = frameworkService.projectNames()
        projects.each { String project ->

            //load export plugins

            def pluginConfig = loadScmConfig(project, 'export')
            if (pluginConfig && pluginConfig.enabled) {
                log.debug("Loading 'export' plugin ${pluginConfig.type} for ${project}...")
                try {
                    initExportPlugin(project, pluginConfig.type, pluginConfig.config)
                } catch (ScmPluginException e) {
                    log.error(
                            "Failed to initialize SCM Export plugin ${pluginConfig.type} for ${project}: ${e.message}",
                            e
                    )
                }
                //TODO: refresh status of all jobs in project?
            } else {
                log.debug("SCM Export not setup for project: ${project}: ${pluginConfig}")
            }

            //TODO: import plugins

        }
    }

    def listPlugins(String integration) {
        switch (integration) {
            case 'export':
                return pluginService.listPlugins(ScmExportPluginFactory, scmExportPluginProviderService)
        //TODO: import
        }
    }

    def getExportPluginDescriptor(String type) {
        pluginService.getPluginDescriptor(
                type,
                scmExportPluginProviderService
        )
    }

    def projectHasConfiguredExportPlugin(String project) {
        loadedExportPlugins.containsKey(project)
    }

    def getExportCommitProperties(String project, List<String> jobIds) {
        def refs = jobRefsForIds(jobIds)
        def plugin = loadedExportPlugins[project]
        plugin.getExportProperties(refs as Set)
    }

    def getExportSetupProperties(String project, String type) {
        DescribedPlugin<ScmExportPluginFactory> plugin = getExportPluginDescriptor(type)
        File baseDir = new File(frameworkService.rundeckFramework.frameworkProjectsBaseDir, project)

        plugin.instance.getSetupPropertiesForBasedir(baseDir)
    }

    ScmPluginConfig loadScmConfig(String project, String integration) {
        //TODO: load with server UUID
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
        //TODO: store export/import separately?
        if (frameworkService.serverUUID) {
            return "${frameworkService.serverUUID}/etc/scm.properties"
        }
        "etc/scm.properties"
    }


    def storeConfig(ScmPluginConfig scmPluginConfig, String project) {

        //todo: store with server UUID
        def project1 = frameworkService.getFrameworkProject(project)
        def configPath = storedConfigFile(scmPluginConfig.integration)
        project1.storeFileResource configPath, scmPluginConfig.asInputStream()
    }

    def ValidatedPlugin validateExportPluginSetup(String project, String name, Map config) {
        return pluginService.validatePlugin(name, scmExportPluginProviderService,
                                            frameworkService.getFrameworkPropertyResolver(project, config),
                                            PropertyScope.Instance,
                                            PropertyScope.Project
        )
    }

    def Validator.Report validateExportPluginConfigProperties(String project, List<Properties> properties, Map config) {
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
     * @param project
     * @param ndx
     * @param config
     * @param capabilities
     * @return
     */
    private def initExportPlugin(String project, String type, Map config) {
        def validation = validateExportPluginSetup(project, type, config)
        if (!validation.valid) {
            throw new ScmPluginException("Validation failed for ${type} plugin: " + validation.report)
        }
        def loaded = loadPluginWithConfig(project, type, config)

        //XXX:
        def changeListener = { JobChangeEvent event, JobSerializer serializer ->
            log.debug("job change event: " + event)
            if (event.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
                //record deleted path
                recordDeletedJob(
                        project,
                        loaded.getRelativePathForJob(event.jobReference),
                        [
                                id             : event.jobReference.id,
                                jobNameAndGroup: event.jobReference.getJobName(),
                        ]
                )
            } else if (event.eventType == JobChangeEvent.JobChangeEventType.MODIFY_RENAME) {
                //record original path for renamed job, if it is different
                def origpath = loaded.getRelativePathForJob(event.originalJobReference)
                def newpath = loaded.getRelativePathForJob(event.jobReference)
                if (origpath != newpath) {
                    recordRenamedJob(project, event.jobReference.id, origpath)
                }
            }
            loaded.jobChanged(event, new JobSerializerReferenceImpl(event.jobReference, serializer))
        } as JobChangeListener

        loadedExportPlugins[project] = loaded
        loadedExportListeners[project] = changeListener

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
    def savePlugin(String integration, String project, String type, Map config) {
        def validation = validateExportPluginSetup(project, type, config)
        if (!validation.valid) {
            return [valid: false, report: validation.report]
        }

        def scmPluginConfig = new ScmPluginConfig(new Properties(), integration)
        scmPluginConfig.config = config
        scmPluginConfig.type = type
        scmPluginConfig.enabled = true
        storeConfig(scmPluginConfig, project)
        try {
            def plugin = initExportPlugin(project, type, config)
            return [valid: true, plugin: plugin]
        } catch (ScmPluginException e) {
            return [error:true, message:e.message]
        }
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


        def loaded = loadedExportPlugins.remove(project)
        def changeListener = loadedExportListeners.remove(project)
        jobEventsService.removeListener(changeListener)
        loaded?.cleanup()

        //clear cached rename/delete info
        renamedJobsCache.remove(project)
        deletedJobsCache.remove(project)
    }

    /**
     * Enable a disabled plugin
     * @param project
     * @param integration integration name
     * @param type plugin type
     * @return a map [valid:true/false, report: Report, plugin: instance], will
     */
    def enablePlugin(String integration, String project, String type) {
        ScmPluginConfig scmPluginConfig = loadScmConfig(project, integration)
        def validation = validateExportPluginSetup(project, type, scmPluginConfig.config)
        if (!validation.valid) {
            return [valid: false, report: validation.report]
        }

        try {
            def plugin = initExportPlugin(project, type, scmPluginConfig.config)
            scmPluginConfig.enabled = true
            storeConfig(scmPluginConfig, project)
            return [valid: true, plugin: plugin]
        } catch (ScmPluginException e) {
            return [error: true, message: e.message]
        }
    }

    def loadPluginWithConfig(String project, String type, Map config) {


        ScmExportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmExportPluginProviderService
        )
        try {
            return plugin.createPlugin(config, project)
        } catch (ConfigurationException e) {
            throw new ScmPluginException(e)
        }
    }

    /**
     * @param jobs list of {@link ScheduledExecution} objects
     * @return map of job ID to file path
     */
    Map<String, String> filePathsMapForJobs(List<ScheduledExecution> jobs) {
        filePathsMapForJobRefs(jobRefsForJobs(jobs))
    }
    /**
     * @param refs list of {@link JobRevReference} objects
     * @return map of job ID to file path
     */
    Map<String, String> filePathsMapForJobRefs(List<JobRevReference> refs) {
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

    private JobRevReference jobRevReference(ScheduledExecution entry) {
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
                { String format, OutputStream os ->
                    switch (format) {
                        case 'xml':
                            os.write(job.encodeAsJobsXML().getBytes("UTF-8"))
                            break;
                        case 'yaml':
                            os.write(job.encodeAsJobsYAML().getBytes("UTF-8"))
                            break;
                        default:
                            throw new IllegalArgumentException("Format not supported: " + format)
                    }
                }
        )
    }

    /**
     * Get the synch status overall
     * @param project
     * @return
     */
    ScmExportSynchState exportPluginStatus(String project) {

        def plugin = loadedExportPlugins[project]
        if (plugin) {
            return plugin.status
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

    def exportCommit(
            String username,
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
        def properties = plugin.getExportProperties(jobrefs as Set)
        def report = validateExportPluginConfigProperties(project, properties, config)
        if (!report.valid) {
            return [valid: false, report: report]
        }
        def result=null
        try {
            result = plugin.export(jobrefs as Set, deletePaths as Set, lookupUserInfo(username), config)
        } catch (ScmPluginException e) {
            log.error(e.message)
            log.debug("export failed ${jobrefs}, ${deletePaths}, ${username}, ${config}", e)
            if(ScmUserInfoMissing.isFieldMissing(e)) {
                def fieldReport = new Validator.Report()
                fieldReport.errors[ScmUserInfoMissing.missingFieldName(e)]=e.message
                return [
                        error: true,
                        message: e.message,
                        report: fieldReport,
                        missingUserInfoField: ScmUserInfoMissing.missingFieldName(e)
                ]
            }
            return [error: e.message]
        }
        if(result.error){
            return [error: result.message]
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
}

class ScmUser implements ScmUserInfo {
    String email
    String fullName
    String firstName
    String lastName
    String userName
}
/**
 * Wraps stored plugin config data, writes and reads it
 */
class ScmPluginConfig {
    Properties properties
    String integration//export or import
    private String prefix

    ScmPluginConfig(final Properties properties, String integration) {
        this.properties = properties ?: new Properties()
        this.integration = integration
        prefix = 'scm.' + integration
    }

    String getType() {
        properties[prefix + '.type']
    }

    void setType(String type) {
        properties[prefix + '.type'] = type
    }

    void setEnabled(boolean enabled) {
        properties[prefix + '.enabled'] = Boolean.toString(enabled)
    }

    boolean getEnabled() {
        properties && Boolean.parseBoolean(properties.getProperty(prefix + '.enabled'))
    }

    Map getConfig() {
        return properties.findAll {
            it.key.startsWith(prefix + '.config.')
        }.collectEntries {
            [it.key.substring((prefix + '.config.').length()), it.value]
        }
    }

    void setConfig(Map config) {
        properties.putAll config.collectEntries { [prefix + '.config.' + it.key, it.value] }
    }


    def asInputStream() {
        def baos = new ByteArrayOutputStream()
        properties.store(baos, "scm config")
        new ByteArrayInputStream(baos.toByteArray())
    }

    static ScmPluginConfig loadFromStream(String integration, InputStream os) {

        def props = new Properties()
        props.load(os)
        return new ScmPluginConfig(props, integration)
    }

    @Override
    public String toString() {
        return "ScmPluginConfig{" +
                "integration='" + integration + '\'' +
                ", prefix='" + prefix + '\'' +
                ", type='" + getType() + '\'' +
                ", enabled='" + getEnabled() + '\'' +
                ", config='" + getConfig() + '\'' +
                '}';
    }
}
