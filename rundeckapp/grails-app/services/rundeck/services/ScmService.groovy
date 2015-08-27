package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
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

    def initialize() {
        def projects = frameworkService.projectNames()
        projects.each { String project ->

            //load export plugins

            def pluginConfig = loadScmConfig(project, 'export')
            if (pluginConfig.enabled && pluginConfig.type) {
                System.err.println("Loading 'export' plugin ${pluginConfig.type} for ${project}...")
                initPlugin('export', project, pluginConfig.type, pluginConfig.config)
                //TODO: refresh status of all jobs in project?
            }

            //TODO: import plugins

        }
    }

    def listPlugins(String integration) {
        pluginService.listPlugins(ScmExportPluginFactory, scmExportPluginProviderService)
        //TODO: import
    }

    def getPluginDescriptor(String type) {
        pluginService.getPluginDescriptor(
                type,
                scmExportPluginProviderService
        )
    }

    def isAlreadySetup(String project) {
        loadedExportPlugins.containsKey(project)
    }

    //todo: turn off/disable plugin

    def getCommitProperties(String project, List<String> jobIds) {
        def refs = jobRefsForIds(jobIds)
        def plugin = loadedExportPlugins[project]
        plugin.getExportProperties(refs as Set)
    }

    def getSetupProperties(String project, String type) {
        DescribedPlugin<ScmExportPluginFactory> plugin = getPluginDescriptor(type)
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
        ScmPluginConfig.loadFromStream(integration, new ByteArrayInputStream(baos.toByteArray()))
    }

    private String storedConfigFile(String integration) {
        "etc/scm.properties"
    }


    def storeConfig(String integration, String type, Map config, String project) {
        def scmPluginConfig = new ScmPluginConfig(new Properties(), integration)
        scmPluginConfig.config = config
        scmPluginConfig.type = type

        //todo: store with server UUID
        def project1 = frameworkService.getFrameworkProject(project)
        def configPath = storedConfigFile(integration)
        project1.storeFileResource configPath, scmPluginConfig.asInputStream()
    }
    /**
     * Create new plugin config and load it
     * @param project
     * @param ndx
     * @param config
     * @param capabilities
     * @return
     */
    def validatePluginConfig(String integration, String project, String type, Map config) {
        //TODO: validate
//        DescribedPlugin<ScmExportPluginFactory> plugin = getPluginDescriptor(type)
    }
    /**
     * Create new plugin config and load it
     * @param project
     * @param ndx
     * @param config
     * @param capabilities
     * @return
     */
    private def initPlugin(String integration, String project, String type, Map config) {
        validatePluginConfig(integration, project, type, config)
        def loaded = loadPluginWithConfig(project, type, config)
        loadedExportPlugins[project] = loaded
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
        validatePluginConfig(integration, project, type, config)
        storeConfig(integration, type, config, project)
        return initPlugin(integration, project, type, config)
    }

    def loadPluginWithConfig(String project, String type, Map config) {


        ScmExportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmExportPluginProviderService
        )
        def created = plugin.createPlugin(config, project)

        //XXX:
        jobEventsService.addListener { JobChangeEvent event, JobSerializer serializer ->
            System.err.println("job change event: " + event)
            created.jobChanged(event, new JobSerializerReferenceImpl(event.jobReference, serializer))
        }

        return created
    }

    Map<String, String> filePathsMapForJobRefs(List<JobReference> refs) {
        def files = [:]
        refs.each { jobReference ->
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

    private JobRevReferenceImpl jobRevReference(ScheduledExecution entry) {
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
    Map<String, ScmPlugin.ScmFileStatus> exportStatusForJobs(List<ScheduledExecution> jobs) {
        def status = [:]
        exportjobRefsForJobs(jobs).each { jobReference ->
            def plugin = loadedExportPlugins[jobReference.project]
            if (plugin) {
                status[jobReference.id] = plugin.getJobStatus(jobReference)
                System.err.println("Status for job ${jobReference}: ${status[jobReference.id]}")
            }
        }
        status
    }

    def exportCommit(String project, Map config, List<ScheduledExecution> jobs) {
        //store config
        def jobrefs = exportjobRefsForJobs(jobs)
        def plugin = loadedExportPlugins[project]
        def result = plugin.export(jobrefs as Set, config)
        System.err.println("Commit id: ${result}")
        result
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
        properties[prefix + '.enabled'] == Boolean.toString(enabled)
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
        new ScmPluginConfig(props, integration)
    }
}
