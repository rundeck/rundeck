package rundeck.services.scm

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import rundeck.ScheduledExecution
import rundeck.data.job.JobRevReferenceImpl
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import rundeck.services.StoredJobChangeEvent

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@CompileStatic
class ScmLoaderService implements EventBusAware {

    FrameworkService frameworkService
    ScmService scmService
    ScheduledExecutionService scheduledExecutionService
    ConfigurationService configurationService
    public static final long DEFAULT_LOADER_DELAY = 0
    public static final long DEFAULT_LOADER_INTERVAL_SEC = 20
    public static final long INIT_RETRY_TIMES = 5
    public static final long INIT_RETRY_TIMES_DELAY = 1000

    /**
     * scheduledExecutor to load job SCM cache
     */
    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2)
    final Map<String, ScheduledFuture> scmProjectLoaderProcess = Collections.synchronizedMap([:])
    final Map<String, Boolean> scmProjectInitLoaded = Collections.synchronizedMap([:])
    final Map<String, ScmPluginConfigData> scmPluginMeta = Collections.synchronizedMap([:])
    final Map<String, ScmPluginConfigData> scmFailedProjectInit = Collections.synchronizedMap([:])

    @Subscriber("rundeck.bootstrap")
    @CompileDynamic
    void beginScmLoader(){
        if(frameworkService) {

            //check if each project has set the SCM Loader process (if needed)
            scheduledExecutor.scheduleAtFixedRate(
                    {
                        for (String project : frameworkService.projectNames()) {
                            for (String integration : scmService.INTEGRATIONS) {
                                String projectIntegration = getProjectIntegration(project, integration)
                                ScmPluginConfigData pluginConfigData = scmService.loadScmConfig(project, integration)
                                if(projectIntegrationEnabled(project, integration, pluginConfigData)){
                                    if(pluginConfigData && pluginConfigData.enabled) {
                                        scmProjectLoaderProcess.put(projectIntegration, startScmLoader(project, integration))
                                    }
                                }else{
                                    //cleanup: if scm was disabled or the project was deleted
                                    if(pluginConfigData && !pluginConfigData.enabled || !pluginConfigData) {
                                        if(scmProjectLoaderProcess.get(projectIntegration)){
                                            ScheduledFuture scheduler = scmProjectLoaderProcess.get(projectIntegration)
                                            scheduler.cancel(true)
                                            scmProjectLoaderProcess.remove(projectIntegration)
                                            cleanUpScmPlugin(project, integration)
                                        }
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

    String getProjectIntegration(String project, String integration){
        project + "-" + integration
    }

    boolean projectIntegrationEnabled(String project, String integration,  ScmPluginConfigData pluginConfigData){
        String projectIntegration = getProjectIntegration(project, integration)
        if(!scmProjectLoaderProcess.get(projectIntegration) && !scmFailedProjectInit.get(projectIntegration)){
            if(pluginConfigData && pluginConfigData.enabled) {
                return true
            }
        }else{
            if(scmFailedProjectInit.get(projectIntegration)){
                if(reloadPlugin(project, integration, pluginConfigData)){
                    scmFailedProjectInit.remove(projectIntegration)
                    return true
                }
            }
        }

        return false
    }

    def startScmLoader(String project, String integration){

        def state = new ScmLoaderStateImpl()
        //enable project integration cache loader
        def scheduler = scheduledExecutor.scheduleAtFixedRate(
                {
                    String projectIntegration = getProjectIntegration(project, integration)
                    ScmPluginConfigData pluginConfigData = scmService.loadScmConfig(project, integration)
                    if(!scmPluginMeta.get(projectIntegration)){
                        scmPluginMeta.put(projectIntegration, pluginConfigData)
                    }
                    if(pluginConfigData && pluginConfigData.enabled){
                        boolean process = false
                        int retryCount = 0
                        long retryTimes = getScmLoaderInitialRetryTimes()
                        long retryDelay = getScmLoaderInitialRetryDelay()

                        if (pluginConfigData.properties.get("flagToReturnProcess")) {
                            pluginConfigData.properties.remove("flagToReturnProcess")
                            scmService.storeConfig(pluginConfigData, project, integration)
                        }

                        while (!process){
                            try {
                                if (integration == scmService.EXPORT) {
                                    processScmExportLoader(project, pluginConfigData, state)
                                }else{
                                    processScmImportLoader(project, pluginConfigData, state)
                                }
                                process = true

                            } catch (Throwable t) {
                                if(retryCount>=retryTimes){
                                    scmFailedProjectInit.put(projectIntegration, pluginConfigData)
                                    process = true
                                    scmToFalse(pluginConfigData, project, integration)
                                    removingLoaderProcess(project, integration)
                                }else{
                                    retryCount++
                                    log.error("Error initializing SCM for: $project/$integration: ${t.message}. Retrying ${retryCount}/${retryTimes}")
                                    Thread.sleep(retryDelay)
                                }
                            }
                        }

                    }else{
                        removingLoaderProcess(project, integration)
                    }
                },
                scmLoaderInitialDelaySeconds,
                scmLoaderIntervalSeconds,
                TimeUnit.SECONDS
        )
        scheduler
    }

    def scmToFalse(ScmPluginConfigData scmPluginConfig, String project, String integration) {
        log.debug("SCM disabled")
        scmPluginConfig.enabled = false
        scmService.storeConfig(scmPluginConfig, project, integration)
    }

    def removingLoaderProcess(String project, String integration){
        String projectIntegration = getProjectIntegration(project, integration)

        //removing task
        log.debug("removing thread ${projectIntegration}")
        scmProjectLoaderProcess.remove(projectIntegration)
        cleanUpScmPlugin(project, integration)
        throw new RuntimeException("SCM disabled or project removed");
    }

    long getScmLoaderInitialDelaySeconds() {
        configurationService?.getLong('scmLoader.delay', DEFAULT_LOADER_DELAY) ?: DEFAULT_LOADER_DELAY
    }


    long getScmLoaderInitialRetryTimes() {
        configurationService?.getLong('scmLoader.init.retry', INIT_RETRY_TIMES) ?: INIT_RETRY_TIMES
    }

    long getScmLoaderInitialRetryDelay() {
        configurationService?.getLong('scmLoader.init.delay', INIT_RETRY_TIMES_DELAY) ?: INIT_RETRY_TIMES_DELAY
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
    static interface ScmLoaderState {
        boolean getInited()

        void setInited(boolean val)

        void addJobs(List<ScheduledExecution> jobs)

        Map<String, JobRevReference> getScannedJobs()

        /**
         * Detects jobs that were deleted
         * @param jobs current job list
         * @param knownDeletedIds known deleted job Ids
         * @param handleDeletedIds consume list of deleted job references
         */
        void detectDeletedJobs(
            List<ScheduledExecution> jobs,
            List<String> knownDeletedIds,
            Consumer<List<JobRevReference>> handleDeletedIds
        )
    }

    /**
     * state implementation for export loader
     */
    @CompileStatic
    static class ScmLoaderStateImpl implements ScmLoaderState {
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

        @Override
        void detectDeletedJobs(
            final List<ScheduledExecution> jobs,
            List<String> knownDeletedIds,
            Consumer<List<JobRevReference>> handleDeletedIds
        ) {
            List<String> jobids = jobs*.extid
            List<JobRevReference> deletedRefs=[]
            if (!jobids.containsAll(scannedJobs.keySet())) {
                List<String> deleted = []
                deleted.addAll((Collection<String>) scannedJobs.keySet())
                deleted.removeAll(jobids)
                deleted.removeAll(knownDeletedIds)
                //remove known deleted jobs
                deletedRefs = deleted.collect{scannedJobs.remove(it)}
            }
            scannedJobs.clear()
            addJobs(jobids.collect { id -> jobs.find { it.extid == id } })
            handleDeletedIds.accept(deletedRefs)
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
    def processScmExportLoader(String project, ScmPluginConfigData pluginConfig, ScmLoaderState state) {

        def username = pluginConfig.getSetting("username")
        def roles = pluginConfig.getSettingList("roles")
        ScmOperationContext context = scmService.scmOperationContext(username, roles, project)
        def loaded = scmService.loadPluginWithConfig(scmService.EXPORT, context, pluginConfig.type, pluginConfig.config)

        if(!loaded){
            return
        }

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

            List<ScheduledExecution> jobs = getJobs(project)
            log.debug("processing ${jobs.size()} jobs")
            if (!state.inited) {
                state.inited = true
                state.addJobs(jobs)
            } else {
                //detect deleted jobs
                def deletedfiles = scmService.deletedExportFilesForProject(project)
                List<String> knownDeletedIds = []
                knownDeletedIds.addAll(deletedfiles.values()*.id.collect{it.toString()})
                state.detectDeletedJobs(jobs, knownDeletedIds) { List<JobRevReference> newDeletedRefs ->
                    if (newDeletedRefs) {
                        //emit fake job change event
                        eventBus.notify(
                            'multiJobChanged',
                            newDeletedRefs.collect { cacheItem ->
                                new StoredJobChangeEvent(
                                    eventType: JobChangeEvent.JobChangeEventType.DELETE,
                                    jobReference: cacheItem,
                                    originalJobReference: cacheItem
                                )
                            }
                        )
                    }
                }
            }

            Map<String, Map> jobPluginMeta = scmService.getJobsPluginMeta(project, true)
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
    def processScmImportLoader(String project, ScmPluginConfigData pluginConfig, ScmLoaderState state){
        log.debug("processing SCM import Loader ${project} / ${pluginConfig.type}")
        def username = pluginConfig.getSetting("username")
        def roles = pluginConfig.getSettingList("roles")
        ScmOperationContext context = scmService.scmOperationContext(username, roles, project)

        def loaded = scmService.loadPluginWithConfig(scmService.IMPORT, context, pluginConfig.type, pluginConfig.config)

        if(!loaded){
            return
        }

        if (!scmService.projectHasConfiguredImportPlugin(project)) {
            return
        }

        if(reloadPlugin(project, ScmService.IMPORT, pluginConfig)){
            log.info("SCM Import has change for project ${project}")
        }

        def plugin = scmService.getLoadedImportPluginFor(project)

        if(plugin){
            log.debug("import plugin found")

            List<ScheduledExecution> jobs = getJobs(project)
            log.debug("processing ${jobs.size()} jobs")

            Map<String, Map> jobPluginMeta = scmService.getJobsPluginMeta(project, false)
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
            if(!state.inited){
                state.inited=true
                state.addJobs(jobs)
            }else{
                //detect deleted jobs
                state.detectDeletedJobs(jobs, []) { List<JobRevReference> newDeletedIds ->
                    if (newDeletedIds) {
                        //emit fake job change event
                        eventBus.notify(
                            'multiJobChanged',
                            newDeletedIds.collect { cacheItem ->
                                new StoredJobChangeEvent(
                                    eventType: JobChangeEvent.JobChangeEventType.DELETE,
                                    jobReference: cacheItem,
                                    originalJobReference: cacheItem
                                )
                            }
                        )
                    }
                }
            }

            if(frameworkService.isClusterModeEnabled()){
                Map<String,String> originalPaths = joblist.collectEntries { [it.id, scmService.getRenamedPathForJobId(it.project, it.id)] }

                //run cluster fix
                plugin.clusterFixJobs(context, joblist, originalPaths)
            }


        }
    }

    boolean reloadPlugin(String project, String integration, ScmPluginConfigData pluginConfig){
        String key = getProjectIntegration(project, integration)
        try{
            def pluginConfigCache = scmPluginMeta.get(key)
            if(pluginConfigCache && pluginConfig.getProperties() != pluginConfigCache.getProperties()){
                scmService.unregisterPlugin(integration, project)
                scmService.initProject(project, integration)
                scmPluginMeta.remove(key)
                return true
            }
        }catch(Exception e){
            return false
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
