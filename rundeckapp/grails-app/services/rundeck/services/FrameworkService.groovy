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
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService
import com.dtolabs.rundeck.core.plugins.configuration.*
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import grails.core.GrailsApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.services.framework.RundeckProjectConfigurable

import javax.security.auth.Subject
import java.util.function.Predicate

/**
 * Interfaces with the core Framework object
 */
class FrameworkService implements ApplicationContextAware {

    static transactional = false
    public static final String REMOTE_CHARSET = 'remote.charset.default'

    boolean initialized = false
    private String serverUUID
    private boolean clusterModeEnabled
    def authorizationService

    def ApplicationContext applicationContext
    def ExecutionService executionService
    def metricService
    def Framework rundeckFramework
    def rundeckPluginRegistry
    def PluginControlService pluginControlService

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
    def isClusterModeEnabled(){
        return clusterModeEnabled
    }
    def getServerUUID(){
        return serverUUID
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
    def projectNames (AuthContext authContext) {
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

    def existsFrameworkProject(String project) {
        return rundeckFramework.getFrameworkProjectMgr().existsFrameworkProject(project)
    }
    
    def getFrameworkProject(String project) {
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
    /**
     * Return the resource definition for a project for use by authorization checks
     * @param name the project name
     * @return resource map for authorization check
     */
    def Map authResourceForProject(String name){
        return AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT, [name: name])
    }
    /**
     * Return the resource definition for a project ACL for use by authorization checks
     * @param name the project name
     * @return resource map for authorization check
     */
    def Map authResourceForProjectAcl(String name){
        return AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT_ACL, [name: name])
    }

    /**
     * return the decision set for all actions on all resources in the project context
     * @param framework
     * @param resources
     * @param actions
     * @param project
     * @return
     */
    def Set authorizeProjectResources( AuthContext authContext, Set resources, Set actions, String project) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def Set decisions
        metricService.withTimer(this.class.name,'authorizeProjectResources') {
            decisions= authContext.evaluate(
                    resources,
                    actions,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        }
        return decisions
    }
    /**
     * return true if the action is authorized for the resource in the project context
     * @param framework
     * @param resource
     * @param action
     * @param project
     * @return
     */
    def boolean authorizeProjectResource(AuthContext authContext, Map resource, String action, String project){
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decision= metricService.withTimer(this.class.name,'authorizeProjectResource') {
            authContext.evaluate(
                    resource,
                    action,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        }
        return decision.authorized
    }
    /**
     * Return true if all actions are authorized for the resource in the project context
     * @param framework
     * @param resource
     * @param actions
     * @param project
     * @return
     */
    def boolean authorizeProjectResourceAll(AuthContext authContext, Map resource, Collection actions, String project){
        if(null==project){
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions= metricService.withTimer(this.class.name,'authorizeProjectResourceAll') {
            authContext.evaluate(
                    [resource] as Set,
                    actions as Set,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        }
        return !(decisions.find {!it.authorized})
    }
    /**
     * Return true if any actions are authorized for the resource in the project context
     * @param framework
     * @param resource
     * @param actions
     * @param project
     * @return
     */
    def boolean authorizeProjectResourceAny(AuthContext authContext, Map resource, Collection actions, String project){
        if(null==project){
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions= metricService.withTimer(this.class.name,'authorizeProjectResourceAll') {
            authContext.evaluate(
                    [resource] as Set,
                    actions as Set,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        }
        return (decisions.find {it.authorized})
    }

    /**
     * Return true if the user is authorized for all actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    boolean authorizeProjectExecutionAll( AuthContext authContext, Execution exec, Collection actions){
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
    boolean authorizeProjectExecutionAny( AuthContext authContext, Execution exec, Collection actions){
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
    def List filterAuthorizedProjectExecutionsAll( AuthContext authContext, List<Execution> execs, Collection actions){
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
    def authorizeProjectJobAny(AuthContext authContext, ScheduledExecution job, Collection actions, String project) {
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
    def authorizeProjectJobAll( AuthContext authContext, ScheduledExecution job, Collection actions, String project){
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

    /**
     * return true if the action is authorized for the resource in the application context
     * @param framework
     * @param resource
     * @param action
     * @return
     */
    def boolean authorizeApplicationResource(AuthContext authContext, Map resource, String action) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }

        def decision = metricService.withTimer(this.class.name,'authorizeApplicationResource') {
            authContext.evaluate(
                resource,
                action,
                Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }
        return decision.authorized
    }
    /**
     * return all authorized resources for the action evaluated in the application context
     * @param framework
     * @param resources requested resources to authorize
     * @param actions set of any actions to authorize
     * @return set of authorized resources
     */
    def Set authorizeApplicationResourceSet(AuthContext authContext, Set<Map> resources, Set<String> actions) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions = metricService.withTimer(this.class.name,'authorizeApplicationResourceSet') {
            authContext.evaluate(
                    resources,
                    actions,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }
        return decisions.findAll {it.authorized}.collect {it.resource}
    }

    /**
     * return true if all of the actions are authorized for the resource in the application context
     * @param framework
     * @param resource
     * @param actions
     * @return
     */
    def boolean authorizeApplicationResourceAll(AuthContext authContext, Map resource, Collection actions) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def Set decisions = metricService.withTimer(this.class.name,'authorizeApplicationResourceAll') {
            authContext.evaluate(
                [resource] as Set,
                actions as Set,
                Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }

        return !(decisions.find {!it.authorized})
    }
    /**
     * return true if any of the actions are authorized for the resource in the application context
     * @param framework
     * @param resource
     * @param actions
     * @return
     */
    def boolean authorizeApplicationResourceAny(AuthContext authContext, Map resource, List actions) {
        return actions.any {
            authorizeApplicationResourceAll(authContext,resource,[it])
        }
    }
    /**
     * return true if the action is authorized for the resource type in the application context
     * @param framework
     * @param resourceType
     * @param action
     * @return
     */
    def boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decision = metricService.withTimer(this.class.name,'authorizeApplicationResourceType') {
            authContext.evaluate(
                    AuthorizationUtil.resourceType(resourceType),
                    action,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }
        return decision.authorized
    }
    /**
     * return true if all of the actions are authorized for the resource type in the application context
     * @param framework
     * @param resourceType
     * @param actions
     * @return
     */
    def boolean authorizeApplicationResourceTypeAll(AuthContext authContext, String resourceType, Collection actions) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def Set decisions= metricService.withTimer(this.class.name,'authorizeApplicationResourceType') {
            authContext.evaluate(
                    [AuthorizationUtil.resourceType(resourceType)] as Set,
                    actions as Set,
                    Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }
        return !(decisions.find {!it.authorized})
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
        if(!pluginControlService){
            pluginControlService = PluginControlServiceImpl.forProject(getRundeckFramework(), project)
        }
        return pluginControlService
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
        rundeckFramework.getNodeStepExecutorService().providerOfType(type).description
    }
    /**
     * Return step plugin description of a certain type
     * @param framework
     * @param type
     * @return
     */
    def Description getStepPluginDescription(String type) throws MissingProviderException{
        rundeckFramework.getStepExecutionService().providerOfType(type).description
    }

    /**
     * Return step plugin of a certain type
     * @param type
     * @return
     */
    def getStepPlugin(String type) throws MissingProviderException{
        rundeckFramework.getStepExecutionService().providerOfType(type)
    }

    /**
     * Return node step plugin of a certain type
     * @param type
     * @return
     */
    def getNodeStepPlugin(String type){
        rundeckFramework.getNodeStepExecutorService().providerOfType(type)
    }


    /**
     * Return dynamic properties values from step plugin
     * @param type, projectAndFrameworkValues
     * @return
     */
    def Map<String, Object> getDynamicPropertiesStepPlugin(
            String type, Map<String, Object> projectAndFrameworkValues) throws MissingProviderException{

        def plugin = getStepPlugin(type)
        getDynamicProperties(plugin, projectAndFrameworkValues)
    }

    /**
     * Return dynamic properties values from node step plugin
     * @param type, projectAndFrameworkValues
     * @return
     */
    def Map<String, Object> getDynamicPropertiesNodeStepPlugin(
            String type, Map<String, Object> projectAndFrameworkValues) throws MissingProviderException{

        def plugin = getNodeStepPlugin(type)
        getDynamicProperties(plugin, projectAndFrameworkValues)
    }

    def Map<String, Object> getDynamicProperties(plugin, Map<String, Object> projectAndFrameworkValues){
        if(plugin instanceof DynamicProperties){
            return plugin.dynamicProperties(projectAndFrameworkValues)
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
    public Map validateServiceConfig(String type, String prefix, Map params, ProviderService<?> service) {
        Map result = [:]
        result.valid=false
        def provider=null
        try {
            provider = service.providerOfType(type)
        } catch (ExecutionServiceException e) {
            result.error = e.message
        }
        if (!provider) {
            result.error = "Invalid provider type: ${type}, not found"
        } else if (!(provider instanceof Describable)) {
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
     * Return all the descriptions for the Rundeck Framework
     * @return tuple(resourceConfigs, nodeExec
     */
    public def listDescriptions() {
        final fmk = getRundeckFramework()
        final descriptions = fmk.getResourceModelSourceService().listDescriptions()
        final nodeexecdescriptions = getNodeExecutorService().listDescriptions()
        final filecopydescs = getFileCopierService().listDescriptions()
        return [descriptions, nodeexecdescriptions, filecopydescs]
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
                final desc = service.providerOfType(serviceType).description
                properties = Validator.demapProperties(props, desc)
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
                final desc = service.providerOfType(serviceType).description
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

    private void demapPropertiesForType(String type, ProviderService service, Properties projProps, String defaultProviderProp, config, Set removePrefixes) {
        final executor = service.providerOfType(type)
        final Description desc = executor.description

        projProps[defaultProviderProp] = type
        mapProperties(config, desc, projProps)
        accumulatePrefixesToRemoveFrom(desc, removePrefixes)
    }
    private void addProjectServicePropertiesForType(String type, ProviderService service, Properties projProps, String defaultProviderProp, config, Set removePrefixes) {
        final executor = service.providerOfType(type)
        final Description desc = executor.description

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
        Map<String, RundeckProjectConfigurable> projectConfigurableBeans = applicationContext.getBeansOfType(
                RundeckProjectConfigurable
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
        Map<String, RundeckProjectConfigurable> projectConfigurableBeans = applicationContext.getBeansOfType(
                RundeckProjectConfigurable
        )
        def errors = []
        def extraConfig = [:]
        def projProps = [:]
        def removePrefixes = []

        projectConfigurableBeans.each { k, RundeckProjectConfigurable v ->
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
}
