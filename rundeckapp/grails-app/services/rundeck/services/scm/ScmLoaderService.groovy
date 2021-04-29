package rundeck.services.scm

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import rundeck.ScheduledExecution
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.JobRevReferenceImpl
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import rundeck.services.StoredJobChangeEvent

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@CompileStatic
class ScmLoaderService implements EventBusAware {

    FrameworkService frameworkService
    ScmService scmService
    ScheduledExecutionService scheduledExecutionService
    ConfigurationService configurationService
    public static final long DEFAULT_LOADER_DELAY = 0
    public static final long DEFAULT_LOADER_INTERVAL_SEC = 20

    /**
     * scheduledExecutor to load job SCM cache
     */
    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2)
    final Map<String, ScheduledFuture> scmProjectLoaderProcess = Collections.synchronizedMap([:])
    final Map<String, Boolean> scmProjectInitLoaded = Collections.synchronizedMap([:])
    final Map<String, ScmPluginConfigData> scmPluginMeta = Collections.synchronizedMap([:])

    @Subscriber("rundeck.bootstrap")
    @CompileDynamic
    void beginScmLoader(){
        if(frameworkService) {

            //check if each project has set the SCM Loader process (if needed)
            scheduledExecutor.scheduleAtFixedRate(
                    {
                        for (String project : frameworkService.projectNames()) {
                            for (String integration : scmService.INTEGRATIONS) {
                                String projectIntegration = project + "-" + integration
                                ScmPluginConfigData pluginConfigData = scmService.loadScmConfig(project, integration)
                                if(!scmProjectLoaderProcess.get(projectIntegration)){
                                    if(pluginConfigData && pluginConfigData.enabled) {
                                        scmProjectLoaderProcess.put(projectIntegration, startScmLoader(project, integration))
                                    }
                                }else{
                                    //cleanup: if scm was disabled or the project was deleted
                                    if(pluginConfigData && !pluginConfigData.enabled || !pluginConfigData) {
                                        ScheduledFuture scheduler = scmProjectLoaderProcess.get(projectIntegration)
                                        scheduler.cancel(true)
                                        scmProjectLoaderProcess.remove(projectIntegration)
                                        cleanUpScmPlugin(project, integration)
                                    }
                                }
                            }
                        }
                    },
                    scmLoaderInitialDelaySeconds,
                    scmLoaderIntervalSeconds,
                    TimeUnit.SECONDS
            )
        }
    }

    def startScmLoader(String project, String integration){

        def state = new ScmExportLoaderStateImpl()
        //enable project integration cache loader
        def scheduler = scheduledExecutor.scheduleAtFixedRate(
                {
                    String projectIntegration = project + "-" + integration
                    ScmPluginConfigData pluginConfigData = scmService.loadScmConfig(project, integration)
                    if(!scmPluginMeta.get(projectIntegration)){
                        scmPluginMeta.put(projectIntegration, pluginConfigData)
                    }
                    if(pluginConfigData && pluginConfigData.enabled){
                        try {
                            if (integration == scmService.EXPORT) {
                                processScmExportLoader(project, pluginConfigData, state)
                            }else{
                                processScmImportLoader(project, pluginConfigData)
                            }

                        } catch (Throwable t) {
                            log.error("processMessages error: $project/$integration: ${t.message}")
                        }
                    }else{
                        //removing task
                        log.debug("removing thread ${projectIntegration}")
                        scmProjectLoaderProcess.remove(projectIntegration)
                        cleanUpScmPlugin(project, integration)
                        throw new RuntimeException("SCM disabled or project removed");
                    }
                },
                scmLoaderInitialDelaySeconds,
                scmLoaderIntervalSeconds,
                TimeUnit.SECONDS
        )
        scheduler
    }

    long getScmLoaderInitialDelaySeconds() {
        configurationService?.getLong('scmLoader.delay', DEFAULT_LOADER_DELAY) ?: DEFAULT_LOADER_DELAY
    }

    long getScmLoaderIntervalSeconds() {
        configurationService?.getLong(
                'scmLoader.interval',
                DEFAULT_LOADER_INTERVAL_SEC
        ) ?:
                DEFAULT_LOADER_INTERVAL_SEC
    }

    @CompileDynamic
    List<ScheduledExecution> getJobs(String project){
        def query=new ScheduledExecutionQuery()
        query.projFilter = project
        def listWorkflows = scheduledExecutionService.listWorkflows(query)
        List<ScheduledExecution> jobs = listWorkflows["schedlist"]
        return jobs
    }

    /**
     * state container interface for export loader
     */
    @CompileStatic
    static interface ScmExportLoaderState {
        boolean getInited()

        void setInited(boolean val)

        void addJobs(List<ScheduledExecution> jobs)

        Map<String, JobRevReference> getScannedJobs()
    }

    /**
     * state implementation for export loader
     */
    @CompileStatic
    static class ScmExportLoaderStateImpl implements ScmExportLoaderState {
        boolean inited
        Map<String, JobRevReference> scannedJobs = new HashMap<>()

        void addJobs(List<ScheduledExecution> jobs) {
            jobs.each {
                scannedJobs.
                    put(
                        it.extid,
                        new JobRevReferenceImpl(
                            project: it.project,
                            id: it.extid,
                            jobName: it.jobName,
                            groupPath: it.groupPath,
                            version: it.version
                        )
                    )
            }
        }
    }

    /**
     *
     * @param project project
     * @param pluginConfig plugin config
     * @param state state holder object
     * @return
     */
    @Transactional
    def processScmExportLoader(String project, ScmPluginConfigData pluginConfig, ScmExportLoaderState state) {

        if (!scmService.projectHasConfiguredExportPlugin(project)) {
            return
        }

        //if plugin property changed, the plugin must be re-enabled
        if(reloadPlugin(project, ScmService.EXPORT, pluginConfig)){
            log.info("SCM Export has change for project ${project}")
        }

        log.debug("processing SCM export Loader ${project} / ${pluginConfig.type}")

        def plugin = scmService.getLoadedExportPluginFor(project)

        if (plugin) {
            log.debug("export plugin found")

            def username = pluginConfig.getSetting("username")
            def roles = pluginConfig.getSettingList("roles")
            ScmOperationContext context = scmService.scmOperationContext(username, roles, project)
            //check global status
            //needed to perform fetch
            plugin.getStatus(context)

            List<ScheduledExecution> jobs = getJobs(project)
            List<String> deleted = []
            log.debug("processing ${jobs.size()} jobs")
            if (!state.inited) {
                state.inited = true
                state.addJobs(jobs)
            } else {
                //detect deleted jobs
                List<String> jobids = jobs*.extid
                if (!jobids.containsAll(state.scannedJobs.keySet())) {
                    deleted.addAll((Collection<String>) state.scannedJobs.keySet())
                    deleted.removeAll(jobids)
                    //remove known deleted jobs
                    def deletedfiles = scmService.deletedExportFilesForProject(project)
                    deleted.removeAll(deletedfiles.values()*.id)
                    if (deleted) {
                        log.debug("detected deleted jobs: ${deleted}...")
                        //emit fake job change event
                        eventBus.notify(
                            'multiJobChanged',
                            deleted.collect { jobid ->
                                def cacheItem = state.scannedJobs.remove(jobid)
                                new StoredJobChangeEvent(
                                    eventType: JobChangeEvent.JobChangeEventType.DELETE,
                                    jobReference: cacheItem,
                                    originalJobReference: cacheItem
                                )
                            }
                        )
                    }
                }
                state.scannedJobs.clear()
                state.addJobs(jobids.collect { id -> jobs.find { it.extid == id } })
            }

            Map<String, Map> jobPluginMeta = scmService.getJobsPluginMeta(project)
            List<JobExportReference> joblist = scmService.exportjobRefsForJobs(jobs, jobPluginMeta)

            def key = project+"-export"
            if(!scmProjectInitLoaded.containsKey(key)){
                plugin.initJobsStatus(joblist)
                //loading first time job status
                log.debug("refresh jobs status")
                plugin.refreshJobsStatus(joblist)
                scmProjectInitLoaded.put(key, true)
            }

            //refresh job plugin meta
            scmService.refreshExportPluginMetadata(project, plugin,jobs,jobPluginMeta)

            log.debug("processing cluster fix")
            if(frameworkService.isClusterModeEnabled()){

                Map<String,String> originalPaths = joblist.collectEntries{[it.id,scmService.getRenamedPathForJobId(it.project, it.id)]}

                //run cluster fix
                plugin.clusterFixJobs(context, joblist, originalPaths)
            }
        }

    }

    @Transactional
    def processScmImportLoader(String project, ScmPluginConfigData pluginConfig){
        log.debug("processing SCM import Loader ${project} / ${pluginConfig.type}")

        if (!scmService.projectHasConfiguredImportPlugin(project)) {
            return
        }

        if(reloadPlugin(project, ScmService.IMPORT, pluginConfig)){
            log.info("SCM Import has change for project ${project}")
        }

        def plugin = scmService.getLoadedImportPluginFor(project)

        if(plugin){
            log.debug("import plugin found")

            def username = pluginConfig.getSetting("username")
            def roles = pluginConfig.getSettingList("roles")
            ScmOperationContext context = scmService.scmOperationContext(username, roles, project)
            //check global status
            //needed to perform fetch
            plugin.getStatus(context)

            List<ScheduledExecution> jobs = getJobs(project)
            log.debug("processing ${jobs.size()} jobs")

            Map<String, Map> jobPluginMeta = scmService.getJobsPluginMeta(project)
            List<JobScmReference> joblist = scmService.scmJobRefsForJobs(jobs, jobPluginMeta)

            def key = project+"-import"
            if(!scmProjectInitLoaded.containsKey(key)){
                plugin.initJobsStatus(joblist)
                //loading first time job status
                log.debug("refresh jobs status")
                //loading first time job status
                plugin.refreshJobsStatus(joblist)

                scmProjectInitLoaded.put(key, true)
            }

            if(frameworkService.isClusterModeEnabled()){
                Map<String,String> originalPaths = joblist.collectEntries { [it.id, scmService.getRenamedPathForJobId(it.project, it.id)] }

                //run cluster fix
                plugin.clusterFixJobs(context, joblist, originalPaths)
            }


        }
    }

    boolean reloadPlugin(String project, String integration, ScmPluginConfigData pluginConfig){
        String key = project + "-" + integration
        def pluginConfigCache = scmPluginMeta.get(key)
        if(pluginConfigCache && pluginConfig.getProperties() != pluginConfigCache.getProperties()){
            scmService.unregisterPlugin(integration, project)
            scmService.initProject(project, integration)
            scmPluginMeta.remove(key)
            return true
        }

        return false
    }


    def cleanUpScmPlugin(String project, String integration){
        def key = project + "-" + integration
        if(scmProjectInitLoaded.containsKey(key)){
            scmProjectInitLoaded.remove(key)
        }

        scmService.unregisterPlugin(integration, project)
    }

}