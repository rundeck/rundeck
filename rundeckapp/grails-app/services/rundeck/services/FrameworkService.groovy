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


import com.dtolabs.rundeck.app.support.ExecutionCleanerConfig
import com.dtolabs.rundeck.app.support.ExecutionCleanerConfigImpl
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.authorization.*
import com.dtolabs.rundeck.core.cluster.ClusterInfoService
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.options.RemoteJsonOptionRetriever
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.*
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import com.dtolabs.rundeck.core.storage.service.StoragePluginProviderService
import com.dtolabs.rundeck.server.AuthContextEvaluatorCacheManager
import grails.compiler.GrailsCompileStatic
import grails.core.GrailsApplication
import grails.events.bus.EventBus
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import org.rundeck.app.execution.workflow.WorkflowExecutionItemFactory
import org.rundeck.app.job.execlifecycle.ExecutionLifecycleJobDataAdapter
import org.rundeck.app.job.option.JobOptionUrlExpander
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.data.util.OptionsParserUtil
import rundeck.services.feature.FeatureService

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import javax.servlet.http.HttpSession
import java.util.function.Predicate

/**
 * Interfaces with the core Framework object
 */
@GrailsCompileStatic
class FrameworkService implements ApplicationContextAware, ClusterInfoService, FrameworkServiceCapabilities {
    static transactional = false
    public static final String REMOTE_CHARSET = 'remote.charset.default'
    public static final String FIRST_LOGIN_FILE = ".firstLogin"
    static final String SYS_PROP_SERVER_ID = "rundeck.server.uuid"

    ApplicationContext applicationContext
    def gormEventStoreService
    ExecutionService executionService
    MetricService metricService
    Framework rundeckFramework
    def rundeckPluginRegistry
    PluginService pluginService
    PluginControlService pluginControlService
    def scheduledExecutionService
    def logFileStorageService
    def fileUploadService
    EventBus grailsEventBus
    AuthContextEvaluator rundeckAuthContextEvaluator
    StoragePluginProviderService storagePluginProviderService
    JobSchedulerService jobSchedulerService
    AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager
    AuthContextProvider rundeckAuthContextProvider
    ConfigurationService configurationService
    FeatureService featureService
    ExecutorService executorService
    AppAuthContextProcessor rundeckAuthContextProcessor
    ReportService reportService
    JobOptionUrlExpander jobOptionUrlExpander
    RemoteJsonOptionRetriever remoteJsonOptionRetriever
    WorkflowExecutionItemFactory workflowExecutionItemFactory
    ReferencedExecutionDataProvider referencedExecutionDataProvider
    ProjectService projectService

    String getRundeckBase(){
        return rundeckFramework.baseDir.absolutePath
    }

    /**
     * Install all the embedded plugins, will not overwrite existing plugin files with the same name
     * @param grailsApplication
     * @return
     */
    def listEmbeddedPlugins(GrailsApplication grailsApplication) {
        def loader = new ApplicationContextPluginFileSource(grailsApplication.mainContext, '/WEB-INF/rundeck/plugins/')
        def result = [success: true, logs: []]
        def pluginList
        try {
            pluginList = loader.listManifests()
        } catch (IOException e) {
            log.error("Could not load plugins: ${e}", e)
            result.message = "Could not load plugins: ${e}"
            result.success = false
            return result
        }
        result.pluginList=pluginList
        return result
    }

    boolean isClusterModeEnabled() {
        configurationService.getBoolean("clusterMode.enabled", false)
    }

    String getServerUUID() {
        System.getProperty(SYS_PROP_SERVER_ID)
    }

    String getServerHostname() {
        rundeckFramework.getFrameworkNodeHostname()
    }

    /**
     *
     * @return the config dir used by the framework
     */
    File getFrameworkConfigDir() {
        rundeckFramework.getConfigDir()
    }

    /**
     *
     * @param name file name in config dir
     * @return true if a file with the name exists in the config dir
     */
    boolean existsFrameworkConfigFile(String name) {
        new File(frameworkConfigDir, name).isFile()
    }
    /**
     *
     * @param name file name in config dir
     * @return true if a file with the name exists in the config dir
     */
    String readFrameworkConfigFile(String name, String charset = 'UTF-8') {
        new File(frameworkConfigDir, name).getText(charset)
    }
    /**
     *
     * @param name file name in config dir
     * @return true if a file with the name exists in the config dir
     */
    long writeFrameworkConfigFile(String name, String text = null, Closure withOutputStream = null) throws IOException {
        def file = new File(frameworkConfigDir, name)

        if (text) {
            file.text = text
        } else if (withOutputStream) {
            file.withOutputStream(withOutputStream)
        }

        file.length()
    }

    /**
     * Deletes the framework config file
     * @param name file name
     * @return true if deleted
     */
    boolean deleteFrameworkConfigFile(String name) {
        def file = new File(frameworkConfigDir, name)
        file.delete()
    }

    @Override
    ProjectManager getFrameworkProjectManager() {
        return rundeckFramework.frameworkProjectMgr
    }
/**
     * Return a list of FrameworkProject objects
     */
    Collection<String> projectNames () {
        rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()
    }

    def projects (AuthContext authContext) {
        //authorize the list of projects
        List<String> authed = projectNames(authContext)
        def result = authed.collect {
            rundeckFramework.frameworkProjectMgr.getFrameworkProject(it)
        }
        return result
    }
    /**
     *
     * @return total number of projects
     */
    @CompileStatic
    int projectCount () {
        return rundeckFramework.frameworkProjectMgr.countFrameworkProjects()
    }

    List<String> projectNames (AuthContext authContext) {
        //authorize the list of projects
        List<String> authed=new ArrayList<String>()
        for (proj in projectNames()) {
            if(rundeckAuthContextEvaluator.authorizeApplicationResourceAny(
                authContext,
                rundeckAuthContextEvaluator.authResourceForProject(proj),
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
            )){
                authed << proj
            }
        }
        return authed.sort()
    }

    /**
     * Loads project.label property for all authorized projects
     * @param authContext
     * @return map of project name to label if it is set, or name if it is not set
     */
    @CompileStatic
    def projectLabels(AuthContext authContext) {
        projectLabels(projectNames(authContext))
    }

    /**
     * Loads project.label property for each project listed
     * @param authContext
     * @param projectNames
     * @return map of project name to label if it is set, or name if it is not set
     */
    @CompileStatic
    def projectLabels(List<String> projectNames) {
        def projectMap = [:]
        projectNames.each { project ->
            def fwkProject = getFrameworkProject(project)
            def label = fwkProject.getProperty("project.label")
            projectMap.put(project, label ?: project)
        }
        projectMap
    }

    @CompileStatic
    public static Optional<Integer> tryParseInt(String val) {
        try {
            Optional.of(Integer.parseInt(val))
        } catch (NumberFormatException ignored) {
            Optional.empty()
        }
    }
    @CompileStatic
    ExecutionCleanerConfig getProjectCleanerExecutionsScheduledConfig (String project) {
        def fwkProject = getFrameworkProject(project)
        def properties = fwkProject.getProjectProperties()
        return new ExecutionCleanerConfigImpl(
            enabled: 'true' == properties.get("project.execution.history.cleanup.enabled"),
            cronExpression: properties.get("project.execution.history.cleanup.schedule"),
            maxDaysToKeep: tryParseInt(properties.get("project.execution.history.cleanup.retention.days")).orElse(-1),
            minimumExecutionToKeep: tryParseInt(properties.get("project.execution.history.cleanup.retention.minimum")).
                orElse(0),
            maximumDeletionSize: tryParseInt(properties.get("project.execution.history.cleanup.batch")).orElse(500)
        )
    }

    @CompileStatic
    Map<String, ExecutionCleanerConfig> getProjectCleanerExecutionsScheduledConfig(Collection<String> projectNames) {
        Map<String, ExecutionCleanerConfig> projectMap = [:]
        projectNames.each { String project ->
            def projectConfig = getProjectCleanerExecutionsScheduledConfig(project)
            if (projectConfig.enabled) {
                projectMap.put(project, projectConfig)
            }
        }
        return projectMap
    }
    @CompileStatic
    void rescheduleAllCleanerExecutionsJob() {
        def projectNames = rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()
        def projectConfigs=getProjectCleanerExecutionsScheduledConfig(projectNames)
        rescheduleAllCleanerExecutionsJobForConfigs(projectConfigs)
    }
    @CompileStatic
    def rescheduleAllCleanerExecutionsJobAsync(){
        log.debug("rescheduleAllCleanerExecutionsJobAsync starting...")
        executorService.submit this.&rescheduleAllCleanerExecutionsJob
    }
    @CompileStatic
    void rescheduleAllCleanerExecutionsJobForConfigs(Map<String, ExecutionCleanerConfig> projectConfigs) {
        projectConfigs.each { String project, ExecutionCleanerConfig config ->
            scheduleCleanerExecutions(project, config)
        }
    }
    /**
     * Refresh the session.frameworkProjects and session.frameworkLabels
     * @param authContext
     * @param session @param var @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    def refreshSessionProjects(AuthContext authContext, session, force=false){
        long sessionProjectRefreshDelay = configurationService.getLong('userSessionProjectsCache.refreshDelay', 5 * 60 * 1000L)
        boolean useCache = featureService.featurePresent(Features.USER_SESSION_PROJECTS_CACHE)
        def now = System.currentTimeMillis()
        def expired=!session.frameworkProjects_expire || session.frameworkProjects_expire < now
        log.debug("refreshSessionProjects(context) cachable? ${useCache} delay: ${sessionProjectRefreshDelay}")
        int count = projectCount()
        if (session.frameworkProjects==null ||
            count != session.frameworkProjects_count ||
            !useCache ||
            expired ||
            force) {
            long start=System.currentTimeMillis()
            def projectNames = projectNames(authContext)
            session.frameworkProjects = projectNames
            if(featureService.featurePresent(Features.SIDEBAR_PROJECT_LISTING)) {
                session.frameworkLabels = projectLabels(projectNames)
            }else{
                session.frameworkLabels = [:]
            }
            session.frameworkProjects_expire = System.currentTimeMillis() + sessionProjectRefreshDelay
            session.frameworkProjects_count = count
            log.debug("refreshSessionProjects(context)... ${System.currentTimeMillis() - start}")
        }else{
            log.debug("refreshSessionProjects(context)... cached")
        }
        return session.frameworkProjects
    }

    /**
     * Load label for a project into the session frameworkLabels map, will read from project properties unless specified
     * @param session session
     * @param project project name
     * @param newLabel label to set
     * @return label or project name if label is not set
     */
    @CompileStatic
    def loadSessionProjectLabel(HttpSession session, String project, String newLabel=null){
        def labels = session.getAttribute('frameworkLabels')
        if(labels instanceof Map && labels[project] && newLabel==null){
            return labels[project]
        }
        def label = newLabel
        if (label == null) {
            def fwkProject = getFrameworkProject(project)
            label = fwkProject.getProperty("project.label")
        }

        if(labels instanceof Map){
            labels.put(project,label?:project)
        }else{
            session.setAttribute'frameworkLabels', [(project):label?:project]
        }
        return label?:project
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def scheduleCleanerExecutions(String project, ExecutionCleanerConfig config){
        if(!isClusterModeEnabled() || jobSchedulerService.tryAcquireExecCleanerJob(getServerUUID(),project)) {
            log.info("removing cleaner executions job scheduled for ${project}")
            scheduledExecutionService.deleteCleanerExecutionsJob(project)

            if (config.enabled) {
                log.info("scheduling cleaner executions job for ${project}")
                scheduledExecutionService.scheduleCleanerExecutionsJob(project, config.getCronExpression(),
                        [
                                maxDaysToKeep         : config.maxDaysToKeep,
                                minimumExecutionToKeep: config.minimumExecutionToKeep,
                                maximumDeletionSize   : config.maximumDeletionSize,
                                project               : project,
                                logFileStorageService : logFileStorageService,
                                fileUploadService     : fileUploadService,
                                frameworkService      : this,
                                jobSchedulerService   : jobSchedulerService,
                                referencedExecutionDataProvider: referencedExecutionDataProvider,
                                reportService         : reportService
                                
                        ])
            }
        }
    }

    /**
     * Create a message to notify other cluster members to reschedule or unschedule project jobs
     *
     * @param project project name
     * @param oldDisableExec disableExecutions old value
     * @param oldDisableSched disableSchedule old value
     * @param isEnabled : whether a node should reschedule or unschedule project jobs
     */
    void notifyProjectSchedulingChange(
            String project,
            boolean oldDisableExec,
            boolean oldDisableSched,
            boolean isEnabled
    ) {
        def projSchedExecProps = [:]
        projSchedExecProps.oldDisableEx = oldDisableExec
        projSchedExecProps.oldDisableSched = oldDisableSched
        projSchedExecProps.isEnabled = isEnabled
        grailsEventBus.notify('project.scheduling.changed',
                [uuid : getServerUUID(),
                 props: [project: project, projSchedExecProps: projSchedExecProps]])

    }

    /**
     * When project is updated, this determines if the project scheduling/execution was enabled/disabled compared to
     * previous setting, and applies the change by rescheduling project jobs or unscheduling them and notifies cluster
     * members via eventbus
     *
     * @param project project name
     * @param oldDisableExec disableExecutions old value
     * @param oldDisableSched disableSchedule old value
     * @param newDisableExec disableExecutions new value
     * @param newDisableSched disableSchedule new value
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    void handleProjectSchedulingEnabledChange(
            String project,
            boolean oldDisableExec,
            boolean oldDisableSched,
            boolean newDisableExec,
            boolean newDisableSched
    )
    {
        def needsChange = ((oldDisableExec != newDisableExec)
                || (oldDisableSched != newDisableSched))
        def isEnabled = (!newDisableExec && !newDisableSched)
        if (needsChange) {
            notifyProjectSchedulingChange(project, oldDisableExec, oldDisableSched, isEnabled)
            if (isEnabled) {
                log.debug("Rescheduling jobs for properties change in project: $project")
                scheduledExecutionService.rescheduleJobs(isClusterModeEnabled()?getServerUUID():null, project)
            }else{
                log.debug("Unscheduling jobs for properties change in project: $project")
                scheduledExecutionService.unscheduleJobsForProject(project,isClusterModeEnabled()?getServerUUID():null)
            }
        }
    }
    boolean existsFrameworkProject(String project) {
        return rundeckFramework.getFrameworkProjectMgr().existsFrameworkProject(project)
    }

    /**
     * @return true if the project exists and is disabled. false otherwise.
     */
    boolean isFrameworkProjectDisabled(String projectName) {
        return rundeckFramework.getFrameworkProjectMgr().isFrameworkProjectDisabled(projectName)
    }

    /**
     * Force project configuration load and returns it
     * @param projectName
     * @return new project configuration object
     */
    @CompileStatic
    IRundeckProjectConfig getProjectConfigReloaded(String projectName){
        return rundeckFramework.getFrameworkProjectMgr().loadProjectConfig(projectName)
    }

    @CompileStatic
    IRundeckProject getFrameworkProject(String project) {
        return rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project)
    }
    /**
     * Create a new project
     * @param project name
     * @param properties config properties
     * @return [project, [error list]]
     */
    def createFrameworkProject(String project, Properties properties){
        def proj=null
        def errors=[]
        try {
            proj = rundeckFramework.getFrameworkProjectMgr().createFrameworkProjectStrict(project, properties)
        } catch (Error e) {
            log.error(e.message,e)
            errors << e.getMessage()
        } catch (RuntimeException e) {
            log.error(e.message,e)
            errors << e.getMessage()
        }

        //initialize project components after  project creation
        try{
            projectService?.afterCreationProjectComponents(project)
        }catch (Exception e){
            log.error("Error initializing project components: ${e.message}",e)
            errors << "Error initializing project components: ${e.message}"
        }

        [proj,errors]
    }
    /**
     * Update project properties by merging
     * @param project name
     * @param properties new properties to merge in
     * @param removePrefixes set of string prefixes of properties to remove
     * @return [success:boolean, error: String]
     */
    def updateFrameworkProjectConfig(String project,Properties properties, Set<String> removePrefixes){
        try {
            getFrameworkProject(project).mergeProjectProperties(properties, removePrefixes)
        } catch (Error e) {
            log.error(e.message,e)
            return [success: false, error: e.message]
        }
        [success:true]
    }
    /**
     * Update project properties by merging
     * @param project name
     * @param properties new properties to merge in
     * @param removePrefixes set of string prefixes of properties to remove
     * @return [success:boolean, error: String]
     */
    def setFrameworkProjectConfig(String project,Properties properties){
        try {
            getFrameworkProject(project).setProjectProperties(properties)
        } catch (Error e) {
            log.error(e.message)
            log.debug(e.message,e)
            return [success: false, error: e.message]
        }
        [success:true]
    }
    /**
     * Update project properties by removing a set of keys
     * @param project name
     * @param toremove keys to remove
     * @return [success:boolean, error: String]
     */
    def removeFrameworkProjectConfigProperties(String project,Set<String> toremove){
        def projProps = new HashMap(loadProjectProperties(getFrameworkProject(project)))
        for (String s: toremove) {
            projProps.remove(s)
        }
        def props=new Properties()
        props.putAll(projProps)
        return setFrameworkProjectConfig(project,props)
    }
    /**
     * Return a map of the project's readme and motd content
     * @param project
     * @param framework
     * @return [readme: "readme content", readmeHTML: "rendered content", motd: "motd content", motdHTML: "readnered content"]
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    def getFrameworkProjectReadmeContents(IRundeckProject project1, boolean includeReadme=true, boolean includeMotd=true){
        def result = [:]
        if(includeReadme && project1.info?.readme){
            result.readme = project1.info?.readme
            result.readmeHTML = project1.info?.readmeHTML?:result.readme?.decodeMarkdown()
        }
        if(includeMotd && project1.info?.motd){
            result.motd = project1.info?.motd
            result.motdHTML = project1.info?.motdHTML?:result.motd?.decodeMarkdown()
        }

        return result
    }

    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    PropertyResolverFactory.Factory pluginConfigFactory(String projectName, Map instanceConfiguration) {
        pluginConfigFactory(instanceConfiguration, null!=projectName?getFrameworkProject(projectName).getProperties():null)
    }
    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    PropertyResolverFactory.Factory pluginConfigFactory(Map instanceConfiguration, Map projectConfig) {
        PropertyResolverFactory.pluginPrefixedScoped(
            PropertyResolverFactory.instanceRetriever(instanceConfiguration),
            PropertyResolverFactory.instanceRetriever(projectConfig),
            rundeckFramework.getPropertyRetriever()
        )
    }
    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    PropertyResolver getFrameworkPropertyResolver(String projectName=null, Map instanceConfiguration=null) {
        return PropertyResolverFactory.createResolver(
                instanceConfiguration ? PropertyResolverFactory.instanceRetriever(instanceConfiguration) : null,
                null != projectName ? getProjectPropertyResolver(projectName) : null,
                rundeckFramework.getPropertyRetriever()
        )
    }

    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    PropertyResolverFactory.Factory getFrameworkPropertyResolverFactory(String projectName=null, Map instanceConfiguration=null) {
        return PropertyResolverFactory.createFrameworkProjectRuntimeResolverFactory(
            rundeckFramework,
            projectName,
            instanceConfiguration
        )
    }

    public PropertyRetriever getProjectPropertyResolver(String projectName) {
        PropertyResolverFactory.instanceRetriever(getFrameworkProject(projectName).getProperties())
    }
    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    PropertyResolver getFrameworkPropertyResolverWithProps(Map projectProperties=null, Map instanceConfiguration=null) {
        return PropertyResolverFactory.createResolver(
                instanceConfiguration ? PropertyResolverFactory.instanceRetriever(instanceConfiguration) : null,
                null != projectProperties ? PropertyResolverFactory.instanceRetriever(projectProperties) : null,
                rundeckFramework.getPropertyRetriever()
        )
    }
    /**
     * Return the property retriever for framework properties from the base dir.
     * @return
     */
    def getFrameworkProperties(){
        return rundeckFramework.getPropertyRetriever()
    }
    /**
     * Filter nodes for a project given the node selector
     * @param framework
     * @param selector
     * @param project
     */
    INodeSet filterNodeSet( NodesSelector selector, String project) {
        return metricService.timer(this.class.name,'filterNodeSet').time((Callable<INodeSet>) {
            def unfiltered = rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project).getNodeSet();
            if(0==unfiltered.getNodeNames().size()) {
                log.warn("Empty node list");
            }
            return NodeFilter.filterNodes(selector, unfiltered)
        })
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    public Map<String,Integer> summarizeTags(Collection<INodeEntry> nodes){
        def tagsummary=[:]
        nodes.collect{it.tags}.flatten().findAll{it}.each{
            tagsummary[it]=(tagsummary[it]?:0)+1
        }
        tagsummary
    }



    String getFrameworkNodeName() {
        return rundeckFramework.getFrameworkNodeName()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    AuthContext userAuthContext(HttpSession session) {
        if (!session['_Framework:AuthContext']) {
            session['_Framework:AuthContext'] = rundeckAuthContextProvider.getAuthContextForSubject(session.subject)
        }
        return session['_Framework:AuthContext']
    }
    IFramework getRundeckFramework(){
        return rundeckFramework;
    }

    def PluginControlService getPluginControlService(String project) {
        PluginControlServiceImpl.forProject(getRundeckFramework(), project)
    }

    static Map<String,String> parseOptsFromString(String argstring){
        return OptionsParserUtil.parseOptsFromString(argstring)
    }

    static Map<String,String> parseOptsFromArray(String[] tokens){
        return OptionsParserUtil.parseOptsFromArray(tokens)
    }
    /**
     * Return plugin description for the step
     * @param framework
     * @param step
     * @return
     */
    def getPluginDescriptionForItem(PluginStep step) {
        try {
            return step.nodeStep ? getNodeStepPluginDescription(step.type) : getStepPluginDescription(step.type)
        } catch (MissingProviderException e) {
            log.warn("Couldn't load description for step ${step}: ${e.message}",e)
            return null
        }
    }
    /**
     * Return node step plugin description of a certain type
     * @param framework
     * @param type
     * @return
     */
    def Description getNodeStepPluginDescription(String type) throws MissingProviderException {
        final described = pluginService.getPluginDescriptor(type, rundeckFramework.getNodeStepExecutorService())
        described?.description
    }
    /**
     * Return step plugin description of a certain type
     * @param framework
     * @param type
     * @return
     */
    def Description getStepPluginDescription(String type) throws MissingProviderException{
        final described = pluginService.getPluginDescriptor(type, rundeckFramework.getStepExecutionService())
        described?.description
    }

    /**
     * Return the list of NodeStepPlugin descriptions
     * @param framework
     * @return
     */
    def List getNodeStepPluginDescriptions(){
        rundeckFramework.getNodeStepExecutorService().listDescriptions()
    }

    /**
     * Return the list of StepPlugin descriptions
     * @param framework
     * @return
     */
    def List getStepPluginDescriptions(){
        rundeckFramework.getStepExecutionService().listDescriptions()
    }

    /**
     * Validate a Service provider descriptor input
     * @param framework framework
     * @param type provider type
     * @param prefix input map prefix string
     * @param params input parameters
     * @param service service
     * @return validation results, keys: "valid" (true/false), "error" (error message), "desc" ({@link Description} object),
     *   "props" (parsed property values map), "report" (Validation report {@link Validator.Report))
     */
    public Map validateServiceConfig(String type, String prefix, Map params, PluggableProviderService<?> service) {
        Map result = [:]
        result.valid=false
        DescribedPlugin provider = null
        try {
            provider = pluginService.getPluginDescriptor(type, service)
        } catch (ExecutionServiceException e) {
            result.error = e.message
        }
        if (!provider) {
            result.error = "Invalid provider type: ${type}, not found"
        } else if (!provider.description) {
            result.error = "Invalid provider type: ${type}, not available for configuration"
        } else {
            def validated=validateDescription(provider.description, prefix, params)
            result.putAll(validated)
        }
        return result
    }
    /**
     * Perform validation of a configuration description for input parameters
     * @param description
     * @param prefix
     * @param params
     * @return result map "desc" ({@link Description} object),
     *   "props" (parsed property values map), "report" (Validation report {@link Validator.Report))
     * @deprecated use {@link #validateDescription(Description,String,Map,String,PropertyScope,PropertyScope)}
     */
    public Map validateDescription(Description description, String prefix, Map params) {
        def result=[:]
        result.valid=false
        result.desc = description
        result.props = parsePluginConfigInput(description, prefix, params)

        if (description) {
            def report = Validator.validate(result.props as Properties, description)
            if (report.valid) {
                result.valid = true
            }
            result.report = report
        }
        return result
    }

    /**
     * Perform validation of a configuration description for input parameters
     * @param description
     * @param prefix
     * @param params
     * @param project optional project name for resolving project properties
     * @param defaultScope default for unmarked properties
     * @param ignored ignore properties at or below this scope
     * @return result map "desc" ({@link Description} object),
     *   "props" (parsed property values map), "report" (Validation report {@link Validator.Report))
     */
    public Map validateDescription(
            Description description,
            String prefix,
            Map params,
            String project,
            PropertyScope defaultScope,
            PropertyScope ignored
    )
    {
        def result = [:]
        result.valid = false
        result.desc = description
        Map props = parsePluginConfigInput(description, prefix, params)
        result.props=props
        PropertyResolver resolver = getFrameworkPropertyResolver(project, props)
        if (result.desc) {
            Validator.Report report = Validator.validate(resolver, description, defaultScope, ignored)
            if (report.valid) {
                result.valid = true
            }
            result.report = report
        }
        return result
    }

    /**
     * Return a map of property names to values, for the given description and input parameters with a specified prefix
     * @param desc the properties descriptor
     * @param prefix key prefix of input map
     * @param params input parameter map
     * @return map of property name to value based on correct property types.
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    public Map parsePluginConfigInput(Description desc, String prefix, final Map params) {
        Map props = [:]
        if (desc) {
            desc.properties.each {prop ->
                def v = params ? params[prefix + prop.name] : null
                if (prop.type == Property.Type.Boolean) {
                    props.put(prop.name, (v == 'true' || v == 'on') ? 'true' : 'false')
                } else if (v) {
                    props.put(prop.name, v)
                }
            }
        } else {
            final cfgprefix = prefix
            //just parse all properties with the given prefix
            params?.keySet()?.each { String k ->
                if (k.startsWith(cfgprefix)) {
                    def key = k.substring(cfgprefix.length())
                    props.put(key, params[k])
                }
            }
        }
        return props
    }

    /**
     * Load direct project properties as a map
     * @param pject the project
     * @return loaded properties
     */
    def Map loadProjectProperties(IRundeckProject pject) {
        pject.getProjectProperties()
    }

    public List<Map<String,Object>> listResourceModelConfigurations(String project) {
        def fproject = getFrameworkProject(project)
        fproject.projectNodes.listResourceModelConfigurations()
    }

    public def listWriteableResourceModelSources(String project) {
        def fproject = getFrameworkProject(project)
        fproject.projectNodes.writeableResourceModelSources
    }

    public def listResourceModelConfigurations(Properties properties) {
        ProjectNodeSupport.listResourceModelConfigurations(properties)
    }

    /**
     * Return a map of ServiceName to list of set of Descriptions, where a property was found matching the pattern [prefix]
     * .[service].[provider].[pluginPropertyName]=value
     * @param properties
     * @param prefix
     * @return
     */
    public Map<String, Set<Description>> listScopedServiceProviders(
            Properties properties,
            String prefix
    ) {
        Map<String, Set<Description>> services = new HashMap<>()
        Map<String, List<Description>> providers = new HashMap<>()
        properties.stringPropertyNames().each { key ->
            if (key.startsWith(prefix + '.')) {
                def substring = key.substring(prefix.length() + 1)
                String[] parts = substring.split(/\./, 2)
                if (parts.length < 2) {
                    return
                }
                def svcName = parts[0]
                if (!providers.containsKey(svcName) && pluginService.hasPluginService(svcName)) {
                    providers[svcName] = pluginService.listPluginDescriptions(svcName)
                }
                if (providers.containsKey(svcName)) {
                    def provPrefix = parts[1]
                    //find applicable provider
                    def found = providers[svcName].find { Description plugin ->
                        provPrefix.startsWith(plugin.name + '.')
                    }
                    if (found) {
                        services.computeIfAbsent(svcName, { new HashSet<Description>() }).add(found)
                    }
                }
            }
        }
        return services
    }

    /**
     * Return a map of ServiceName to Provider name to config map, where a property was found matching the pattern [prefix]
     * .[service].[provider].[pluginPropertyName]=value
     * @param properties
     * @param prefix
     * @return
     */
    public Map<String, Map<String, Map<String, String>>> discoverScopedConfiguration(
            Properties properties,
            String prefix
    ) {
        Map<String, Map<String, Map<String, String>>> providers = new HashMap<>()
        def providers1 = listScopedServiceProviders(properties, prefix)
        providers1.each { svcName, providerSet ->
            providerSet.each { Description provider ->
                String provpref = "${prefix}.${svcName}.${provider.name}"
                def providerConf = providers
                        .computeIfAbsent(svcName, { new HashMap<String, Map<String, String>>() })
                        .computeIfAbsent(provider.name, { new HashMap<String, String>() })
                provider.properties.each { pprop ->
                    String key = "${provpref}.${pprop.name}"
                    String val = properties[key]
                    if (val) {
                        providerConf.put(pprop.name, val)
                    }
                }
            }
        }
        providers
    }


    /**
     * Return all the Node Exec plugin type descriptions for the Rundeck Framework, in the order:

     * @return tuple(resourceConfigs, nodeExec, filecopier)
     */
    public def listDescriptions() {
        final fmk = getRundeckFramework()
        final descriptions = pluginService.listPluginDescriptions(ResourceModelSourceFactory, fmk.getResourceModelSourceService())
        final nodeexecdescriptions = pluginService.listPluginDescriptions(NodeExecutor, fmk.getNodeExecutorService())
        final filecopydescs = pluginService.listPluginDescriptions(FileCopier, fmk.getFileCopierService())
        return [descriptions, nodeexecdescriptions, filecopydescs]
    }

    List<Description> listPluginGroupDescriptions() {
        pluginService.listPluginDescriptions(
                PluginGroup,
                pluginService.createPluggableService(PluginGroup)
        )
    }


    List<Description> listResourceModelSourceDescriptions() {
        pluginService.listPluginDescriptions(
            ResourceModelSourceFactory,
            getRundeckFramework().getResourceModelSourceService()
        )
    }

    public getDefaultNodeExecutorService(String project) {
        final fproject = getFrameworkProject(project)
        fproject.hasProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null
    }
    public getDefaultNodeExecutorService(Properties properties) {
        properties.getProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY)
    }

    public getDefaultFileCopyService(String project) {
        final fproject = getFrameworkProject(project)
        fproject.hasProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null
    }
    public getDefaultFileCopyService(Properties properties) {
        properties.getProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY)
    }

    public Map<String, String> getFileCopyConfigurationForType(String serviceType, String project) {
        getServicePropertiesForType(serviceType, getFileCopierService(), project)
    }
    public Map<String, String> getFileCopyConfigurationForType(String serviceType, Properties properties) {
        getServicePropertiesMapForType(serviceType, getFileCopierService(), properties)
    }

    /**
     * Return a map of property name to value
     * @param serviceType
     * @param project
     * @return
     */
    public Map<String, String> getNodeExecConfigurationForType(String serviceType, String project) {
        getServicePropertiesForType(serviceType, getNodeExecutorService(), project)
    }

    public boolean hasPluginGroupConfigurationForType(String serviceType, Map<String,String> projectProps) {
        return projectProps.get(
            "project.PluginGroup.${serviceType}.enabled".toString()
        )=='true'
    }

    public Map<String, String> getPluginGroupConfigurationForType(String serviceType, String project) {
        getServicePropertiesForPluginGroups(serviceType, pluginService.createPluggableService(PluginGroup), project)
    }

    /**
     * Return a map of property name to value
     * @param serviceType
     * @param project
     * @return
     */
    public Map<String, String> getNodeExecConfigurationForType(String serviceType, Properties properties) {
        getServicePropertiesMapForType(serviceType, getNodeExecutorService(), properties)
    }

    private Map<String,String> getServicePropertiesForPluginGroups(String serviceType, PluggableProviderService service, String project) {
        return getServicePropertiesMapForPluginGroups(serviceType,service,getFrameworkProject(project).getProperties())
    }
    /**
     * Return a map of property name to value for the configured project plugin
     * @param serviceType
     * @param service
     * @param project
     * @return
     */
    private Map<String,String> getServicePropertiesForType(String serviceType, PluggableProviderService service, String project) {
        return getServicePropertiesMapForType(serviceType,service,getFrameworkProject(project).getProperties())
    }
    /**
     * Return a map of property name to value for the configured project plugin
     * @param serviceType
     * @param service
     * @param project
     * @return
     */
    public Map<String,String> getServicePropertiesMapForType(String serviceType, PluggableProviderService service, Map props) {
        Map<String,String> properties = new HashMap<>()
        if (serviceType) {
            try {
                def described = pluginService.getPluginDescriptor(serviceType, service)
                if(described?.description) {
                    properties = Validator.demapProperties(props, described.description)
                }
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                log.debug(e.message,e)
            }
        }
        properties
    }

    public Map<String,String> getServicePropertiesMapForPluginGroups(String serviceType, PluggableProviderService service, Map props) {
        Map<String,String> properties = new HashMap<>()
        if (serviceType) {
            try {
                def described = pluginService.getPluginDescriptor(serviceType, service)
                if(described?.description) {
                    properties = Validator.demapPluginGroupProperties(props, described.description)
                }
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                log.debug(e.message,e)
            }
        }
        properties
    }
    /**
     * Convert property keys in a Validator.Report to the mapped property names for the provider properties
     * @param report a validator report
     * @param serviceType service type
     * @param service plugin service
     * @return Report with error keys using the property mappings
     */
    public Validator.Report remapReportProperties(Validator.Report report, String serviceType, PluggableProviderRegistryService service) {
        Map<String,String> properties = new HashMap<>()
        if (serviceType) {
            try {
                def described = pluginService.getPluginDescriptor(serviceType, service)
                if(described?.description) {
                    properties = Validator.mapProperties(report.errors, described.description)
                }
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                log.debug(e.message,e)
            }
        }
        Validator.buildReport().errors(properties).build()
    }


    public PluggableProviderService getFileCopierService() {
        getRundeckFramework().getFileCopierService()
    }

    public PluggableProviderService getNodeExecutorService() {
        getRundeckFramework().getNodeExecutorService()
    }

    public void addProjectNodeExecutorPropertiesForType(String type, Properties projectProps, config, Set removePrefixes = null) {
        addProjectServicePropertiesForType(type, getNodeExecutorService(), projectProps, NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY, config, removePrefixes)
    }

    public void addProjectFileCopierPropertiesForType(String type, Properties projectProps, config, Set removePrefixes = null) {
        addProjectServicePropertiesForType(type, getFileCopierService(), projectProps, FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY, config, removePrefixes)
    }
    public void demapFileCopierProperties(String type, Properties projectProps, config, Set removePrefixes = null) {
        demapPropertiesForType(type, getFileCopierService(), projectProps, FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY, config, removePrefixes)
    }

    private void demapPropertiesForType(
        String type,
        PluggableProviderService service,
        Properties projProps,
        String defaultProviderProp,
        config,
        Set removePrefixes
    ) {
        final described = pluginService.getPluginDescriptor(type, service)
        final Description desc = described.description
        projProps[defaultProviderProp] = type
        mapProperties(config, desc, projProps)
        accumulatePrefixesToRemoveFrom(desc, removePrefixes)
    }

    private void addProjectServicePropertiesForType(
        String type,
        PluggableProviderService service,
        Properties projProps,
        String defaultProviderProp,
        config,
        Set removePrefixes
    ) {
        final described = pluginService.getPluginDescriptor(type, service)
        if(!described){
            return
        }
        final Description desc = described.description
        projProps[defaultProviderProp] = type
        mapProperties(config, desc, projProps)
        accumulatePrefixesToRemoveFrom(desc, removePrefixes)
    }

    private void accumulatePrefixesToRemoveFrom(Description desc, Set removePrefixes) {
        if (null != removePrefixes && desc.propertiesMapping) {
            removePrefixes.addAll(desc.propertiesMapping.values())
        }
    }

    private void mapProperties(config, Description desc, Properties projectProperties) {
        if (config && config instanceof Map) {
            projectProperties.putAll(Validator.mapProperties(config, desc))
        }
    }

    Map<String, String> getProjectGlobals(final String project) {
        rundeckFramework.getProjectGlobals(project)
    }

    Map<String, String> getProjectProperties(final String project) {
        rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project).getProperties()
    }

    String getDefaultInputCharsetForProject(final String project) {
        def config = rundeckFramework.getFrameworkProjectMgr().loadProjectConfig(project)
        String charsetname
        if(config.hasProperty("project.$REMOTE_CHARSET")) {
            charsetname=config.getProperty("project.${REMOTE_CHARSET}")
        }else if (config.hasProperty("framework.$REMOTE_CHARSET")) {
            charsetname=config.getProperty("framework.$REMOTE_CHARSET")
        }
        return charsetname
    }

    /**
     * non transactional interface to run a job from plugins
     * {@link ExecutionService#executeJob executeJob}
     * @return Map of the execution result.
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map kickJob(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext, String user, Map input){
        executionService.executeJob(scheduledExecution, authContext, user, input)
    }

    /**
     * non transactional interface to bulk delete executions
     * {@link ExecutionService#deleteBulkExecutionIds deleteBulkExecutionIds}
     * @return [success:true/false, failures:[ [success:false, message: String, id: id],... ], successTotal:Integer]
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map deleteBulkExecutionIds(Collection ids, AuthContext authContext, String username) {
        executionService.deleteBulkExecutionIds(ids,authContext,username)
    }

    /**
     * non transactional interface to query executions
     * {@link ExecutionService#queryExecutions queryExecutions}
     * @return [result:result,total:total]
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map queryExecutions(ExecutionQuery query, int offset=0, int max=-1) {
        executionService.queryExecutions(query,offset,max)
    }

    /**
     * Load the input map for project configurable beans
     * @param prefix prefix for each bean for the result
     * @param projectInputProps input project properties
     * @param category optional category to limit properties
     * @return Map of [(beanName): Map [ name: String, configurable: Bean, values: demapped value Map, prefix: bean prefix] ]
     */
    Map<String, Map> loadProjectConfigurableInput(String prefix, Map projectInputProps, String category = null) {
        Map<String, ProjectConfigurable> projectConfigurableBeans = applicationContext.getBeansOfType(
                ProjectConfigurable
        )

        Map<String, Map> extraConfig = [:]
        projectConfigurableBeans.each { k, v ->
            if (k.endsWith('Profiled')) {
                //skip profiled versions of beans
                return
            }
            def categoriesMap = v.categories
            Collection<String> valid
            if (category) {
                valid = categoriesMap.keySet().findAll { k2 -> categoriesMap[k2] == category }
                if (!valid) {
                    return
                }
            } else {
                valid = categoriesMap.keySet()
            }
            //construct existing values from project properties
            Map<String, String> mapping = v.getPropertiesMapping()
            List<Property> properties = v.getProjectConfigProperties();
            if (category) {
                mapping = mapping.subMap(valid)
            }
            def values = Validator.demapProperties(projectInputProps, mapping, true)
            extraConfig[k] = [
                    name        : k,
                    configurable: v,
                    values      : values,
                    prefix      : prefix + k + '.',
                    mapping     : mapping,
                    propertyList : properties

            ]
        }
        extraConfig
    }
    /**
     * Validate the input to ProjectConfigurable beans
     * @param inputMap map of name to config map for each bean name being validated
     * @param prefix prefix string for output
     * @param category optional category to limit validation/output
     * @return map [errors:List, config: Map, props: Map, remove: List]
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map validateProjectConfigurableInput(Map<String, Map> inputMap, String prefix, Predicate<String> categoryPredicate = null) {
        Map<String, ProjectConfigurable> projectConfigurableBeans = applicationContext.getBeansOfType(
                ProjectConfigurable
        )
        def errors = []
        def extraConfig = [:]
        def projProps = [:]
        def removePrefixes = []

        projectConfigurableBeans.each { String k, ProjectConfigurable v ->
            if (k.endsWith('Profiled')) {
                //skip profiled versions of beans
                return
            }
            def categoriesMap = v.categories
            def valid = []
            if (categoryPredicate) {
                valid = categoriesMap.keySet().findAll { k2 -> categoryPredicate.test(categoriesMap[k2])}
                if (!valid) {
                    return
                }
            } else {
                valid = categoriesMap.keySet()
            }
            //construct input values for the bean

            Map input = inputMap.get(k) ?: [:]
            def beanData = [
                    name        : k,
                    configurable: v,
                    prefix      : prefix + k + '.',
                    values      : input
            ]

            def validProps = v.getProjectConfigProperties().findAll { it.name in valid }
            validProps.findAll { it.type == Property.Type.Boolean }.
                    each {

                        if(!input[it.name]){
                            input[it.name] = it.defaultValue
                        }

                        if (input[it.name] != 'true') {
                            input[it.name] = 'false'
                        }
                    }
            validProps.findAll { it.type == Property.Type.Options }.
                    each {
                        if (input[it.name] instanceof Collection) {
                            input[it.name] = input[it.name].join(',')
                        } else if (input[it.name] instanceof String[]) {
                            input[it.name] = input[it.name].join(',')
                        }
                    }
            //validate
            def report = Validator.validate(input as Properties, validProps)
            beanData.report = report
            if (!report.valid) {
                errors << ("Some configuration was invalid: " + report)
            } else {
                Map<String, String> mapping = v.getPropertiesMapping()
                if (categoryPredicate) {
                    mapping = mapping.subMap(valid)
                }
                def projvalues = Validator.performMapping(input, mapping, true)
                projProps.putAll(projvalues)
                //remove all previous settings
                removePrefixes.addAll(mapping.values())
            }
            extraConfig[k] = beanData
        }
        [
                errors: errors,
                config: extraConfig,
                props : projProps,
                remove: removePrefixes
        ]
    }

    @CompileDynamic
    ExecutionLifecycleJobDataAdapter getExecutionLifecyclePluginService() {
        return executionService.executionLifecycleComponentService
    }

    public <T> PluggableProviderService<T> getStorageProviderPluginService() {
        return (PluggableProviderService<T>)storagePluginProviderService
    }

    public File getFirstLoginFile() {
        String vardir
        if(rundeckFramework.hasProperty('framework.var.dir')) {
            vardir = rundeckFramework.getProperty('framework.var.dir')
        } else {
            vardir = getRundeckBase()+"/var"
        }
        return new File(vardir, FIRST_LOGIN_FILE)
    }
}
