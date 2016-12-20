package rundeck.services

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.MultiAuthorization
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.PluginCustomizer
import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import com.dtolabs.rundeck.server.plugins.loader.PluginFileManifest
import com.dtolabs.rundeck.server.plugins.loader.PluginFileSource
import com.dtolabs.utils.Streams
import grails.spring.BeanBuilder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scripting.config.LangNamespaceUtils
import org.springframework.scripting.groovy.GroovyScriptFactory
import org.springframework.scripting.support.ScriptFactoryPostProcessor
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution

import javax.security.auth.Subject

/**
 * Interfaces with the core Framework object
 */
class FrameworkService implements ApplicationContextAware {

    static transactional = false

    boolean initialized = false
    private String serverUUID
    private boolean clusterModeEnabled
    def authorizationService

    def ApplicationContext applicationContext
    def ExecutionService executionService
    def metricService
    def Framework rundeckFramework
    def rundeckPluginRegistry

    def getRundeckBase(){
        return rundeckFramework.baseDir.absolutePath;
    }

    /**
     * Install all the embedded plugins, will not overwrite existing plugin files with the same name
     * @param grailsApplication
     * @return
     */
    def extractEmbeddedPlugins(GrailsApplication grailsApplication){
        def loader = new ApplicationContextPluginFileSource(grailsApplication.mainContext, '/WEB-INF/rundeck/plugins/')
        def result=[success:true,logs:[]]
        def pluginsDir = getRundeckFramework().getLibextDir()
        def pluginList
        try {
            pluginList = loader.listManifests()
        } catch (IOException e) {
            log.error("Could not load plugins: ${e}",e)
            result.message = "Could not load plugins: ${e}"
            result.success = false
            return result
        }

        pluginList.each { PluginFileManifest pluginmf ->
            try{
                if(installPlugin(pluginsDir, loader, pluginmf, false)){
                    result.logs << "Extracted bundled plugin ${pluginmf.fileName}"
                }else{
                    result.logs << "Skipped existing plugin: ${pluginmf.fileName}"
                }
            } catch (Exception e) {
                log.error("Failed extracting bundled plugin ${pluginmf}", e)
                result.logs << "Failed extracting bundled plugin ${pluginmf}: ${e}"
            }
            if(pluginmf.fileName.endsWith(".groovy")){
                //initialize groovy plugin as spring bean if it does not exist
                def bean=loadGroovyScriptPluginBean(grailsApplication,new File(pluginsDir, pluginmf.fileName))
                result.logs << "Loaded groovy plugin ${pluginmf.fileName} as ${bean.class} ${bean}"

            }
        }
        return result
    }
    def loadGroovyScriptPluginBean(GrailsApplication grailsApplication,File file){
        String beanName = file.name.replace('.groovy', '')
        def testBean
        try {
            testBean = grailsApplication.mainContext.getBean(beanName)
            log.debug("Groovy plugin bean already exists in main context: $beanName: $testBean")
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("Bean not found: $beanName")
        }
        if (testBean) {
            return testBean
        }

        def builder=new BeanBuilder(grailsApplication.mainContext)
        builder.beans {
            xmlns lang: 'http://www.springframework.org/schema/lang'
            lang.groovy(id: beanName, 'script-source': file.toURI().toString(), 'customizer-ref': 'pluginCustomizer')
        }

        def context = builder.createApplicationContext()
        def result= context.getBean(beanName)

        log.debug("Loaded groovy plugin bean; type: ${result.class} ${result}")
        rundeckPluginRegistry.registerDynamicPluginBean(beanName,context)

        return result
    }

    /**
     * Install a plugin from a source
     * @param pluginsDir destination directory
     * @param loader source
     * @param pluginmf plugin to install
     * @param overwrite true to overwrite existing file
     * @return true if the plugin file was written, false otherwise
     * @throws IOException
     */
    public boolean installPlugin(File pluginsDir, PluginFileSource loader, PluginFileManifest pluginmf,
                                 boolean overwrite) throws IOException {
        File destFile = new File(pluginsDir, pluginmf.fileName)
        if (!overwrite && destFile.exists()) {
            return false
        }
        def pload = loader.getContentsForPlugin(pluginmf)
        if (!pload) {
            throw new Exception("Failed to load plugin: ${pluginmf}")
        }
        destFile.withOutputStream { os ->
            Streams.copyStream(pload.contents,os)
        }

        return true
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
        def authed = authorizeApplicationResourceSet(authContext, resources, 'read')
        return new ArrayList(authed.collect{projMap[it.name]})
    }
    def projectNames (AuthContext authContext) {
        //authorize the list of projects
        def resources=[] as Set
        for (projName in rundeckFramework.frameworkProjectMgr.listFrameworkProjectNames()) {
            resources << authResourceForProject(projName)
        }
        def authed = authorizeApplicationResourceSet(authContext, resources, 'read')
        return new ArrayList(authed.collect{it.name}).sort()
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
    def getFrameworkProjectReadmeContents(IRundeckProject project1){
        def result = [:]
        if(project1.info?.readme){
            result.readme = project1.info?.readme
            result.readmeHTML = project1.info?.readmeHTML?:result.readme?.decodeMarkdown()
        }
        if(project1.info?.motd){
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
        return authResourceForJob(se.jobName,se.groupPath)
    }

    /**
     * Return the resource definition for a job for use by authorization checks, using parameters as input
     * @param se
     * @return
     */
    def Map authResourceForJob(String name, String groupPath){
        return AuthorizationUtil.resource(AuthConstants.TYPE_JOB,[name:name,group:groupPath?:''])
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
     * Return true if the user is authorized for all actions for the execution in the project context
     * @param framework
     * @param exec
     * @param actions
     * @param project
     * @return true/false
     */
    def authorizeProjectExecutionAll( AuthContext authContext, Execution exec, Collection actions){
        def ScheduledExecution se = exec.scheduledExecution
        return se ?
               authorizeProjectJobAll(authContext, se, actions, se.project)  :
               authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_ADHOC, actions, exec.project)

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
     * @param action
     * @return set of authorized resources
     */
    def Set authorizeApplicationResourceSet(AuthContext authContext, Set<Map> resources, String action) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions = metricService.withTimer(this.class.name,'authorizeApplicationResourceSet') {
            authContext.evaluate(
                    resources,
                    [action] as Set,
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
    def Framework getRundeckFramework(){
        if (!initialized) {
            initialize()
        }
        return rundeckFramework;
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
    def Description getNodeStepPluginDescription(String type){
        rundeckFramework.getNodeStepExecutorService().providerOfType(type).description
    }
    /**
     * Return step plugin description of a certain type
     * @param framework
     * @param type
     * @return
     */
    def Description getStepPluginDescription(String type){
        rundeckFramework.getStepExecutionService().providerOfType(type).description
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
        result.props = parseResourceModelConfigInput(result.desc, prefix, params)

        if (result.desc) {
            def report = Validator.validate(result.props as Properties, result.desc)
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
        result.props = parseResourceModelConfigInput(result.desc, prefix, params)
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
    public Map parseResourceModelConfigInput(Description desc, String prefix, final Map params) {
        Map props = [:]
        if (desc) {
            desc.properties.each {prop ->
                def v = params[prefix  + prop.name]
                if (prop.type == Property.Type.Boolean) {
                    props.put(prop.name, (v == 'true' || v == 'on') ? 'true' : 'false')
                } else if (v) {
                    props.put(prop.name, v)
                }
            }
        } else {
            final cfgprefix = prefix
            //just parse all properties with the given prefix
            params.keySet().each {String k ->
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

    public def listResourceModelConfigurations(String project) {
        def fproject = getFrameworkProject(project)
        fproject.projectNodes.listResourceModelConfigurations()
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
}
