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

import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.*
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.core.cluster.ClusterInfoService
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.*
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import com.dtolabs.rundeck.server.plugins.services.StoragePluginProviderService
import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils
import org.rundeck.app.spi.Services
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.services.ExecutionServiceException

import javax.security.auth.Subject
import java.util.function.Predicate

/**
 * Interfaces with the core Framework object
 */
class FrameworkService implements ApplicationContextAware, AuthContextProcessor, ClusterInfoService {
    static transactional = false
    public static final String REMOTE_CHARSET = 'remote.charset.default'
    public static final String FIRST_LOGIN_FILE = ".firstLogin"

    boolean initialized = false
    private String serverUUID
    private boolean clusterModeEnabled
    def authorizationService

    def ApplicationContext applicationContext
    def ExecutionService executionService
    def metricService
    def Framework rundeckFramework
    def rundeckPluginRegistry
    def PluginService pluginService
    def PluginControlService pluginControlService
    def scheduledExecutionService
    def logFileStorageService
    def fileUploadService
    def AuthContextEvaluator rundeckAuthContextEvaluator
    StoragePluginProviderService storagePluginProviderService
    JobSchedulerService jobSchedulerService

    def getRundeckBase(){
        return rundeckFramework.baseDir.absolutePath;
    }

    /**
     * Install all the embedded plugins, will not overwrite existing plugin files with the same name
     * @param grailsApplication
     * @return
     */
    def listEmbeddedPlugins(GrailsApplication grailsApplication) {
        def loader = new ApplicationContextPluginFileSource(grailsApplication.mainContext, '/WEB-INF/rundeck/plugins/')
        def result = [success: true, logs: []]
        def pluginsDir = getRundeckFramework().getLibextDir()
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

    // Initailize the Framework
    def initialize() {
        if(initialized){
            return
        }

        clusterModeEnabled = applicationContext?.getServletContext()?.getAttribute("CLUSTER_MODE_ENABLED")=='true'
        serverUUID = applicationContext?.getServletContext()?.getAttribute("SERVER_UUID")
        initialized = true
    }

    boolean isClusterModeEnabled() {
        clusterModeEnabled
    }

    String getServerUUID() {
        serverUUID
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

/**
     * Return a list of FrameworkProject objects
     */
    def projectNames () {
        rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()
    }
    def projects (AuthContext authContext) {
        //authorize the list of projects
        def projMap=[:]
        def resources=[] as Set
        for (proj in rundeckFramework.frameworkProjectMgr.listFrameworkProjects()) {
            projMap[proj.name] = proj;
            resources << authResourceForProject(proj.name)
        }
        def authed = authorizeApplicationResourceSet(authContext, resources, [AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN] as Set)
        return new ArrayList(new HashSet(authed.collect{it.name}).sort().collect{projMap[it]})
    }
    List<String> projectNames (AuthContext authContext) {
        //authorize the list of projects
        def resources=[] as Set
        for (projName in rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()) {
            resources << authResourceForProject(projName)
        }
        def authed = authorizeApplicationResourceSet(authContext, resources, [AuthConstants.ACTION_READ,AuthConstants.ACTION_ADMIN] as Set)
        return new ArrayList(new HashSet(authed.collect{it.name})).sort()
    }
    def projectLabels (AuthContext authContext) {
        def projectNames = projectNames(authContext)
        def projectMap = [:]
        projectNames.each { project ->
            def fwkProject = getFrameworkProject(project)
            def label = fwkProject.getProjectProperties().get("project.label")
            projectMap.put(project,label?:project)
        }
        projectMap
    }
    def projectCleanerExecutionsScheduled () {
        def projectNames = rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()
        def projectMap = [:]
        projectNames.each { project ->
            def projectConfig = [:]
            def fwkProject = getFrameworkProject(project)
            def enabled = ["true", true].contains(fwkProject.getProjectProperties().get("project.execution.history.cleanup.enabled"))
            def maxDaysToKeep = fwkProject.getProjectProperties().get("project.execution.history.cleanup.retention.days")
            def cronExpression = fwkProject.getProjectProperties().get("project.execution.history.cleanup.schedule")
            def minimumExecutionToKeep = fwkProject.getProjectProperties().get("project.execution.history.cleanup.retention.minimum")
            def maximumDeletionSize = fwkProject.getProjectProperties().get("project.execution.history.cleanup.batch")
            if(enabled){
                projectConfig.put("enabled",enabled)
                projectConfig.put("maxDaysToKeep",maxDaysToKeep)
                projectConfig.put("cronExpression",cronExpression)
                projectConfig.put("minimumExecutionToKeep",minimumExecutionToKeep)
                projectConfig.put("maximumDeletionSize",maximumDeletionSize)
                projectMap.put(project,projectConfig)
            }
        }
        projectMap
    }
    def rescheduleAllCleanerExecutionsJob(){
        def projectsConfigs = projectCleanerExecutionsScheduled()
        projectsConfigs.each { project, config ->
            scheduleCleanerExecutions(project, config.enabled,
                    config.maxDaysToKeep ? Integer.parseInt(config.maxDaysToKeep) : -1,
                    StringUtils.isNotEmpty(config.minimumExecutionToKeep) ? Integer.parseInt(config.minimumExecutionToKeep) : 0,
                    StringUtils.isNotEmpty(config.maximumDeletionSize) ? Integer.parseInt(config.maximumDeletionSize) : 500,
                    config.cronExpression)
        }
    }
    /**
     * Refresh the session.frameworkProjects and session.frameworkLabels
     * @param authContext
     * @param session @param var @return
     */
    def refreshSessionProjects(AuthContext authContext, session){
        def fprojects = projectNames(authContext)
        def flabels = projectLabels(authContext)
        session.frameworkProjects = fprojects
        session.frameworkLabels = flabels
        fprojects
    }

    def scheduleCleanerExecutions(String project,
                                  boolean enabled,
                                  Integer cleanerHistoryPeriod,
                                  Integer minimumExecutionToKeep,
                                  Integer maximumDeletionSize,
                                  String cronExression){
        log.info("removing cleaner executions job scheduled for ${project}")
        scheduledExecutionService.deleteCleanerExecutionsJob(project)

        if(enabled) {
            log.info("scheduling cleaner executions job for ${project}")
            scheduledExecutionService.scheduleCleanerExecutionsJob(project, cronExression,
                    [
                            maxDaysToKeep: cleanerHistoryPeriod,
                            minimumExecutionToKeep: minimumExecutionToKeep,
                            maximumDeletionSize: maximumDeletionSize,
                            project: project,
                            logFileStorageService: logFileStorageService,
                            fileUploadService: fileUploadService,
                            frameworkService: this,
                            jobSchedulerService: jobSchedulerService
                    ])
        }
    }

    def existsFrameworkProject(String project) {
        return rundeckFramework.getFrameworkProjectMgr().existsFrameworkProject(project)
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
    def getFrameworkPropertyResolver(String projectName=null, Map instanceConfiguration=null) {
        return PropertyResolverFactory.createResolver(
                instanceConfiguration ? PropertyResolverFactory.instanceRetriever(instanceConfiguration) : null,
                null != projectName ? PropertyResolverFactory.instanceRetriever(getFrameworkProject(projectName).getProperties()) : null,
                rundeckFramework.getPropertyRetriever()
        )
    }
    /**
     * Get a property resolver for optional project level
     * @param projectName
     * @return
     */
    def getFrameworkPropertyResolverWithProps(Map projectProperties=null, Map instanceConfiguration=null) {
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
    def INodeSet filterNodeSet( NodesSelector selector, String project) {
        metricService.withTimer(this.class.name,'filterNodeSet') {
            def unfiltered = rundeckFramework.getFrameworkProjectMgr().getFrameworkProject(project).getNodeSet();
            if(0==unfiltered.getNodeNames().size()) {
                log.warn("Empty node list");
            }
            NodeFilter.filterNodes(selector, unfiltered);
        }
    }

    public INodeSet filterAuthorizedNodes(final String project, final Set<String> actions, final INodeSet unfiltered,
                                          AuthContext authContext) {
        return rundeckFramework.filterAuthorizedNodes(project,actions,unfiltered,authContext)
    }

    public Map<String,Integer> summarizeTags(Collection<INodeEntry> nodes){
        def tagsummary=[:]
        nodes.collect{it.tags}.flatten().findAll{it}.each{
            tagsummary[it]=(tagsummary[it]?:0)+1
        }
        tagsummary
    }

    /**
     * Return the resource definition for a job for use by authorization checks
     * @param se
     * @return
     */
    def Map authResourceForJob(ScheduledExecution se){
        return authResourceForJob(se.jobName,se.groupPath,se.extid)
    }

    /**
     * Return the resource definition for a job for use by authorization checks, using parameters as input
     * @param se
     * @return
     */
    def Map authResourceForJob(String name, String groupPath, String uuid){
        return AuthorizationUtil.resource(AuthConstants.TYPE_JOB,[name:name,group:groupPath?:'',uuid: uuid])
    }

    @Override
    Map<String, String> authResourceForProject(String name) {
        rundeckAuthContextEvaluator.authResourceForProject(name)
    }

    @Override
    Map<String, String> authResourceForProjectAcl(String name) {
        rundeckAuthContextEvaluator.authResourceForProjectAcl(name)
    }


    @Override
    Set<Decision> authorizeProjectResources(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions,
            String project
    ) {
        metricService.withTimer(this.class.name,'authorizeProjectResources') {
            rundeckAuthContextEvaluator.authorizeProjectResources(authContext, resources, actions, project)
        }
    }

    @Override
    boolean authorizeProjectResource(
            AuthContext authContext,
            Map<String, String> resource,
            String action,
            String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResource') {
            rundeckAuthContextEvaluator.authorizeProjectResource(authContext, resource, action, project)
        }
    }

    @Override
    boolean authorizeProjectResourceAll(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResourceAll') {
            rundeckAuthContextEvaluator.authorizeProjectResourceAll(authContext, resource, actions, project)
        }
    }

    @Override
    boolean authorizeProjectResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResourceAny') {
            rundeckAuthContextEvaluator.authorizeProjectResourceAny(authContext, resource, actions, project)
        }
    }

    /**
     * Return true if the user is authorized for all actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    boolean authorizeProjectExecutionAll( AuthContext authContext, Execution exec, Collection<String> actions){
        def ScheduledExecution se = exec.scheduledExecution
        return se ?
               authorizeProjectJobAll(authContext, se, actions, se.project)  :
               authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_ADHOC, actions, exec.project)

    }
    /**
     * Return true if the user is authorized for any actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    boolean authorizeProjectExecutionAny( AuthContext authContext, Execution exec, Collection<String> actions){
        def ScheduledExecution se = exec.scheduledExecution
        return se ?
               authorizeProjectJobAny(authContext, se, actions, se.project)  :
               authorizeProjectResourceAny(authContext, AuthConstants.RESOURCE_ADHOC, actions, exec.project)

    }
    /**
     * Filter a list of Executions and return only the ones that the user has authorization for all actions in the project context
     * @param framework
     * @param execs list of executions
     * @param actions
     * @return List of authorized executions
     */
    List<Execution> filterAuthorizedProjectExecutionsAll( AuthContext authContext, List<Execution> execs, Collection<String> actions){
        def semap=[:]
        def adhocauth=null
        def results=[]
        metricService.withTimer(this.class.name,'filterAuthorizedProjectExecutionsAll') {
            execs.each{Execution exec->
                def ScheduledExecution se = exec.scheduledExecution
                if(se && null==semap[se.id]){
                    semap[se.id]=authorizeProjectJobAll(authContext, se, actions, se.project)
                }else if(!se && null==adhocauth){
                    adhocauth=authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_ADHOC, actions,
                            exec.project)
                }
                if(se ? semap[se.id] : adhocauth){
                    results << exec
                }
            }
        }
        return results
    }
    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    boolean authorizeProjectJobAny(AuthContext authContext, ScheduledExecution job, Collection<String> actions, String project) {
        actions.any {
            authorizeProjectJobAll(authContext, job, [it], project)
        }
    }
    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    boolean authorizeProjectJobAll( AuthContext authContext, ScheduledExecution job, Collection<String> actions, String project){
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions= metricService.withTimer(this.class.name,'authorizeProjectJobAll') {
            authContext.evaluate(
                    [authResourceForJob(job)] as Set,
                    actions as Set,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        }
        return !(decisions.find {!it.authorized})
    }


    @Override
    boolean authorizeApplicationResource(AuthContext authContext, Map<String, String> resource, String action) {

        metricService.withTimer(this.class.name, 'authorizeApplicationResource') {
            rundeckAuthContextEvaluator.authorizeApplicationResource(authContext, resource, action)
        }

    }

    @Override
    Set<Map<String, String>> authorizeApplicationResourceSet(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions
    ) {
        metricService.withTimer(this.class.name, 'authorizeApplicationResourceSet') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceSet(authContext, resources, actions)
        }
    }


    @Override
    boolean authorizeApplicationResourceAll(AuthContext authContext, Map<String, String> resource, Collection<String> actions) {
        metricService.withTimer(this.class.name, 'authorizeApplicationResourceAll') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceAll(authContext, resource, actions)
        }
    }

    @Override
    boolean authorizeApplicationResourceAny(AuthContext authContext, Map<String, String> resource, List<String> actions) {
        rundeckAuthContextEvaluator.authorizeApplicationResourceAny(authContext, resource, actions)
    }

    @Override
    boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        metricService.withTimer(this.class.name, 'authorizeApplicationResourceType') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceType(authContext, resourceType, action)
        }
    }

    @Override
    boolean authorizeApplicationResourceTypeAll(AuthContext authContext, String resourceType, Collection<String> actions) {
        return metricService.withTimer(this.class.name, 'authorizeApplicationResourceTypeAll') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceTypeAll(authContext, resourceType, actions)
        }
    }

    def getFrameworkNodeName() {
        return rundeckFramework.getFrameworkNodeName()
    }

    def getFrameworkRoles() {
        initialize()
        return new HashSet(authorizationService.getRoleList())
    }

    def AuthContext userAuthContext(session) {
        if (!session['_Framework:AuthContext']) {
            session['_Framework:AuthContext'] = getAuthContextForSubject(session.subject)
        }
        return session['_Framework:AuthContext']
    }
    def IFramework getRundeckFramework(){
        if (!initialized) {
            initialize()
        }
        return rundeckFramework;
    }

    def PluginControlService getPluginControlService(String project) {
        PluginControlServiceImpl.forProject(getRundeckFramework(), project)
    }

    public UserAndRolesAuthContext getAuthContextForSubject(Subject subject) {
        if (!subject) {
            throw new RuntimeException("getAuthContextForSubject: Cannot get AuthContext without subject")
        }
        return new SubjectAuthContext(subject, authorizationService.systemAuthorization)
    }
    /**
     * Extend a generic auth context, with project-specific authorization
     * @param orig original auth context
     * @param project project name
     * @return new AuthContext with project-specific authorization added
     */
    public UserAndRolesAuthContext getAuthContextWithProject(UserAndRolesAuthContext orig, String project) {
        if (!orig) {
            throw new RuntimeException("getAuthContextWithProject: Cannot get AuthContext without orig")
        }
        if(!project){
            throw new RuntimeException("getAuthContextWithProject: Cannot get AuthContext without project")
        }
        def project1 = getFrameworkProject(project)
        def projectAuth = project1.getProjectAuthorization()
        log.debug("getAuthContextWithProject ${project}, orig: ${orig}, project auth ${projectAuth}")
        return orig.combineWith(projectAuth)
    }
    public UserAndRolesAuthContext getAuthContextForSubjectAndProject(Subject subject, String project) {
        if (!subject) {
            throw new RuntimeException("getAuthContextForSubjectAndProject: Cannot get AuthContext without subject")
        }
        if(!project){
            throw new RuntimeException("getAuthContextForSubjectAndProject: Cannot get AuthContext without project")
        }

        def project1 = getFrameworkProject(project)

        def projectAuth = project1.getProjectAuthorization()
        def authorization = new MultiAuthorization(authorizationService.systemAuthorization, projectAuth)
        log.debug("getAuthContextForSubjectAndProject ${project}, authorization: ${authorization}, project auth ${projectAuth}")
        return new SubjectAuthContext(subject, authorization)
    }
    public UserAndRolesAuthContext getAuthContextForUserAndRolesAndProject(String user, List rolelist, String project) {
        getAuthContextWithProject(getAuthContextForUserAndRoles(user, rolelist), project)
    }
    public UserAndRolesAuthContext getAuthContextForUserAndRoles(String user, List rolelist) {
        if (!(null != user && null != rolelist)) {
            throw new RuntimeException("getAuthContextForUserAndRoles: Cannot get AuthContext without user, roles: ${user}, ${rolelist}")
        }
        //create fake subject
        Subject subject = new Subject()
        subject.getPrincipals().add(new Username(user))
        rolelist.each { String s ->
            subject.getPrincipals().add(new Group(s))
        }
        return new SubjectAuthContext(subject, authorizationService.systemAuthorization)
    }


    /**
     * Create a map of option name to value given an input argline.
     * Supports the form "-option value".  Tokens not in that form are ignored. The string
     * can have quoted values, using single or double quotes, and allows double/single to be
     * embedded. To embed single/single or double/double, the quotes should be repeated.
     */
    public static Map<String,String> parseOptsFromString(String argstring){
        if(!argstring){
            return null;
        }
        def String[] tokens=com.dtolabs.rundeck.core.utils.OptsUtil.burst(argstring)
        return parseOptsFromArray(tokens)
    }
    /**
     * Parse an array of tokens in the form ['-optionname','value',...], ignoring
     * incorrectly sequenced values and options.
     * @param tokens
     * @return
     */
    public static Map<String,String> parseOptsFromArray(String[] tokens){
        def Map<String,String> optsmap = new HashMap<String,String>()
        def String key=null
        for(int i=0;i<tokens.length;i++){
            if (key) {
                optsmap[key] = tokens[i]
                key = null
            }else if (tokens[i].startsWith("-") && tokens[i].length()>1){
                key=tokens[i].substring(1)
            }
        }
        if(key){
            //ignore
        }
        return optsmap
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
     * load the dynamic select values for properties from the plugin, or null
     * @param serviceName
     * @param type
     * @param project
     * @param services
     * @return
     */
    def Map<String, Object> getDynamicProperties(
        String serviceName,
        String type,
        String project,
        Services services
    ) {
        final PropertyResolver resolver = PropertyResolverFactory.createPluginRuntimeResolver(
            project,
            rundeckFramework,
            null,
            serviceName,
            type
        );

        def pluginServiceType

        if(serviceName == ServiceNameConstants.WorkflowNodeStep){
            pluginServiceType = rundeckFramework.getNodeStepExecutorService()
        }else if(serviceName == ServiceNameConstants.WorkflowStep){
            pluginServiceType = rundeckFramework.getStepExecutionService()
        }else{
            pluginServiceType = serviceName
        }

        def pluginDescriptor = pluginService.getPluginDescriptor(type, pluginServiceType)

        final Map<String, Object> config = PluginAdapterUtility.mapDescribedProperties(
            resolver,
            pluginDescriptor.description,
            PropertyScope.Project
        );

        def plugin = pluginDescriptor.instance
        if(plugin instanceof DynamicProperties){
            return plugin.dynamicProperties(config, services)
        }
        return null
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
        result.props = parsePluginConfigInput(result.desc, prefix, params)
        def resolver = getFrameworkPropertyResolver(project, result.props)
        if (result.desc) {
            def report = Validator.validate(resolver, description, defaultScope, ignored)
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

    /**
     * Return a map of property name to value
     * @param serviceType
     * @param project
     * @return
     */
    public Map<String, String> getNodeExecConfigurationForType(String serviceType, Properties properties) {
        getServicePropertiesMapForType(serviceType, getNodeExecutorService(), properties)
    }

    /**
     * Return a map of property name to value for the configured project plugin
     * @param serviceType
     * @param service
     * @param project
     * @return
     */
    private Map<String,String> getServicePropertiesForType(String serviceType, PluggableProviderRegistryService service, String project) {
        return getServicePropertiesMapForType(serviceType,service,getFrameworkProject(project).getProperties())
    }
    /**
     * Return a map of property name to value for the configured project plugin
     * @param serviceType
     * @param service
     * @param project
     * @return
     */
    public Map<String,String> getServicePropertiesMapForType(String serviceType, PluggableProviderRegistryService service, Map props) {
        def properties = [:]
        if (serviceType) {
            try {
                def described = pluginService.getPluginDescriptor(serviceType, service)
                if(described) {
                    final desc = described.description
                    properties = Validator.demapProperties(props, desc)
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
        def properties = [:]
        if (serviceType) {
            try {
                def described = pluginService.getPluginDescriptor(serviceType, service)
                final desc = described.description
                properties = Validator.mapProperties(report.errors, desc)
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                log.debug(e.message,e)
            }
        }
        Validator.buildReport().errors(properties).build()
    }


    public ProviderService getFileCopierService() {
        getRundeckFramework().getFileCopierService()
    }

    public ProviderService getNodeExecutorService() {
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
    Map kickJob(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext, String user, Map input){
        executionService.executeJob(scheduledExecution, authContext, user, input)
    }

    /**
     * non transactional interface to bulk delete executions
     * {@link ExecutionService#deleteBulkExecutionIds deleteBulkExecutionIds}
     * @return [success:true/false, failures:[ [success:false, message: String, id: id],... ], successTotal:Integer]
     */
    Map deleteBulkExecutionIds(Collection ids, AuthContext authContext, String username) {
        executionService.deleteBulkExecutionIds(ids,authContext,username)
    }

    /**
     * non transactional interface to query executions
     * {@link ExecutionService#queryExecutions queryExecutions}
     * @return [result:result,total:total]
     */
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
            def valid = []
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
            if (category) {
                mapping = mapping.subMap(valid)
            }
            def values = Validator.demapProperties(projectInputProps, mapping, true)
            extraConfig[k] = [
                    name        : k,
                    configurable: v,
                    values      : values,
                    prefix      : prefix + k + '.',
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
    Map validateProjectConfigurableInput(Map<String, Map> inputMap, String prefix, Predicate<String> categoryPredicate = null) {
        Map<String, ProjectConfigurable> projectConfigurableBeans = applicationContext.getBeansOfType(
                ProjectConfigurable
        )
        def errors = []
        def extraConfig = [:]
        def projProps = [:]
        def removePrefixes = []

        projectConfigurableBeans.each { k, ProjectConfigurable v ->
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
