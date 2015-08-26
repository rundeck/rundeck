package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobFileMapper
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmDiffResult
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmPlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.jobs.PatternJobFileMapper
import com.dtolabs.rundeck.server.plugins.jobs.TestJobChangeListener
import com.dtolabs.rundeck.server.plugins.services.ScmExportPluginProviderService
import rundeck.ScheduledExecution

/**
 * Created by greg on 4/30/15.
 */
class ScmService {
    def JobEventsService jobEventsService
    def grailsApplication
    def frameworkService
    ScmExportPluginProviderService scmExportPluginProviderService
    PluginService pluginService
    Map<String, ScmExportPlugin> loadedPlugins = Collections.synchronizedMap([:])

    def initialize() {
        def projects = frameworkService.projectNames()
        //TODO: init pre-configured plugins
        projects.each { String project ->
            def type = hasConfig(project)
            if (type) {
                System.err.println("Loading ScmExport plugin ${type} for ${project}...")
                def config = loadConfig(type, project)
                initPlugin(project,type,config)
            }
        }

//        [testScmPlugin].eachWithIndex { plugin, ndx ->
//            plugin.init()
//            def config=loadConfig(plugin,ndx,)
//        }
    }

    def listPlugins(String project) {
//        scmExportPluginProviderService.listDescriptions()
        pluginService.listPlugins(ScmExportPluginFactory, scmExportPluginProviderService)
    }

    def getPluginDescriptor(String type) {
//        scmExportPluginProviderService.listDescriptions()
        pluginService.getPluginDescriptor(
                type,
                scmExportPluginProviderService
        )
    }

    def isAlreadySetup(String project) {
        //listPlugins(project).get(ndx).isSetup()
        loadedPlugins.containsKey(project)
    }

    def getCommitProperties(String project, List<String> jobIds) {
        def refs = jobRefsForIds(jobIds)
        def plugin = loadedPlugins[project]
        plugin.getExportProperties(refs as Set)
    }

    def getSetupProperties(String project, String type) {
        DescribedPlugin<ScmExportPluginFactory> plugin = getPluginDescriptor(type)
        File baseDir = new File(frameworkService.rundeckFramework.frameworkProjectsBaseDir, project)

        plugin.instance.getSetupPropertiesForBasedir(baseDir)
    }

    def hasConfig(String project) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (!project1.existsFileResource("etc/scm.properties")) {
            return false
        }
        def baos = new ByteArrayOutputStream()
        project1.loadFileResource("etc/scm.properties", baos)
        def props = new Properties()
        props.load(new ByteArrayInputStream(baos.toByteArray()))
        return props['scm.export.type']
    }

    def loadConfig(String type, String project) {
        def project1 = frameworkService.getFrameworkProject(project)
        if (!project1.existsFileResource("etc/scm.properties")) {
            return null
        }
        def baos = new ByteArrayOutputStream()
        project1.loadFileResource("etc/scm.properties", baos)
        def props = new Properties()
        props.load(new ByteArrayInputStream(baos.toByteArray()))
        return props.findAll {
            it.key.startsWith('scm.' + type + '.config.')
        }.collectEntries {
            [it.key.substring(('scm.' + type + '.config.').length()), it.value]
        }
    }

    def storeConfig(plugin, String type, Map config, String project) {
        //todo
        def props = config.collectEntries { ['scm.' + type + '.config.' + it.key, it.value] } as Properties
        props['scm.export.type'] = type
        def project1 = frameworkService.getFrameworkProject(project)
        def baos = new ByteArrayOutputStream()
        props.store(baos, "scm config")
        def bais = new ByteArrayInputStream(baos.toByteArray())
        project1.storeFileResource("etc/scm.properties", bais)
    }
    /**
     * Create new plugin config and load it
     * @param project
     * @param ndx
     * @param config
     * @param capabilities
     * @return
     */
    def initPlugin(String project, String type, Map config) {
        //store config
        DescribedPlugin<ScmExportPluginFactory> plugin = getPluginDescriptor(type)
        //TODO: validate
//        def plugin = listPlugins(project).get(ndx)
        storeConfig(plugin, type, config, project)
        def loaded = loadPluginWithConfig(project, type, config)
        loadedPlugins[project] = loaded
        loaded
    }

    def loadPluginWithConfig(String project, String type, Map config) {


        ScmExportPluginFactory plugin = pluginService.getPlugin(
                type,
                scmExportPluginProviderService
        )
        def created = plugin.createPlugin(config, project)
        jobEventsService.addListener { JobChangeEvent event, JobSerializer serializer ->
            System.err.println("job change event: " + event)
            created.jobChanged(event, new JobSerializerReferenceImpl(event.jobReference, serializer))
        }

        return created
        //TODO: cache object?
        //TODO: listen for job changes?
//        def listener = new TestJobChangeListener(mapper: testMapper)
//        jobEventsService.addListener(listener)
    }

    def jobStatus(JobReference jobReference) {
        def plugin = loadedPlugins[jobReference.project]
        plugin ? plugin.getJobStatus(jobReference) : null
    }

    List<File> filesForJobRefs(List<JobReference> refs) {
        refs.collect { jobReference ->
            def plugin = loadedPlugins[jobReference.project]
            plugin?.getLocalFileForJob(jobReference)
        }.findAll { it }
    }

    Map<String, File> filesMapForJobRefs(List<JobReference> refs) {
        def files = [:]
        refs.each {
            files[it.id] = testMapper?.fileForJob(it)
        }
        files
    }

    Map<String, String> filePathsMapForJobRefs(List<JobReference> refs) {
        def files = [:]
        refs.each { jobReference ->
            def plugin = loadedPlugins[jobReference.project]
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

    Map<String, ScmPlugin.ScmFileStatus> statusForJobs(List<ScheduledExecution> jobs) {
        def status = [:]
        exportjobRefsForJobs(jobs).each { jobReference ->
            def plugin = loadedPlugins[jobReference.project]
            if (plugin) {
                status[jobReference.id] = plugin.getJobStatus(jobReference)
                System.err.println("Status for job ${jobReference}: ${status[jobReference.id]}")
            }
        }
        status
    }

    def commit(String project, Map config, List<ScheduledExecution> jobs) {
        //store config
        def jobrefs = exportjobRefsForJobs(jobs)
        def plugin = loadedPlugins[project]
        def result = plugin.export(jobrefs as Set, config)
        System.err.println("Commit id: ${result}")
        result
    }

    ScmDiffResult diff(String project, ScheduledExecution job){
        def jobref = exportJobRef(job)
        def plugin = loadedPlugins[project]
        plugin.getFileDiff(jobref)
    }
}
