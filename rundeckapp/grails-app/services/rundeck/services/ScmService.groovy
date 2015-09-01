package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import rundeck.ScheduledExecution

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
                initExportPlugin(project, pluginConfig.type, pluginConfig.config)
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

    //todo: turn off/disable plugin

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
        if(frameworkService.serverUUID) {
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
        def plugin = initExportPlugin(project, type, config)
        return [valid: true, plugin: plugin]
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
        loaded.cleanup()
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

        scmPluginConfig.enabled = true
        storeConfig(scmPluginConfig, project)
        def plugin = initExportPlugin(project, type, scmPluginConfig.config)
        return [valid: true, plugin: plugin]
    }

    def loadPluginWithConfig(String project, String type, Map config) {


        ScmExportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmExportPluginProviderService
        )
        def created = plugin.createPlugin(config, project)


        return created
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
     * Return a map of status for jobs
     * @param jobs
     * @return
     */
    Map<String, JobState> exportStatusForJobs(List<ScheduledExecution> jobs) {
        def status = [:]
        exportjobRefsForJobs(jobs).each { jobReference ->
            def plugin = loadedExportPlugins[jobReference.project]
            if (plugin) {
                status[jobReference.id] = plugin.getJobStatus(jobReference)
                log.debug("Status for job ${jobReference}: ${status[jobReference.id]}")
            }
        }
        status
    }

    def exportCommit(String project, Map config, List<ScheduledExecution> jobs) {
        //store config
        def plugin = loadedExportPlugins[project]
        def jobrefs = exportjobRefsForJobs(jobs)
        def properties = plugin.getExportProperties(jobrefs as Set)
        def report = validateExportPluginConfigProperties(project, properties, config)
        if (!report.valid) {
            return [valid: false, report: report]
        }
        def result = plugin.export(jobrefs as Set, config)
        log.debug("Commit id: ${result}")
        [valid: true, commitId: result]
    }

    ScmDiffResult exportDiff(String project, ScheduledExecution job) {
        def jobref = exportJobRef(job)
        def plugin = loadedExportPlugins[project]
        plugin.getFileDiff(jobref)
    }
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
