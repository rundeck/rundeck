package rundeck.services
import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution

import javax.security.auth.Subject
/**
 * Interfaces with the core Framework object
 */
class FrameworkService implements ApplicationContextAware, Authorization {

    static transactional = false

    boolean initialized = false
    private String serverUUID
    private boolean clusterModeEnabled
    SAREAuthorization aclpolicies

    def ApplicationContext applicationContext
    def ExecutionService executionService
    def metricService
    def projects
    def Framework rundeckFramework

    def getRundeckBase(){
        return rundeckFramework.baseDir.absolutePath;
    }


    // Initailize the Framework
    def initialize() {
        if(initialized){
            return
        }

        //todo: configure auth via spring
        aclpolicies= new SAREAuthorization(new File(Constants.getFrameworkConfigDir(getRundeckBase())))

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
   

    @Override
    Decision evaluate(Map<String, String> resource, Subject subject, String action, Set<Attribute> environment) {
        return aclpolicies.evaluate(resource,subject,action,environment)
    }

    @Override
    Set<Decision> evaluate(Set<Map<String, String>> resources, Subject subject, Set<String> actions, Set<Attribute> environment) {
        return aclpolicies.evaluate(resources,subject,actions,environment)
    }
/**
     * Return a list of FrameworkProject objects
     */
    def projects (AuthContext authContext) {
        //authorize the list of projects
        def projMap=[:]
        def resources=[] as Set
        for (proj in rundeckFramework.frameworkProjectMgr.listFrameworkProjects()) {
            projMap[proj.name] = proj;
            resources << [type: 'project', name: proj.name]
        }
        def authed = authorizeApplicationResourceSet(authContext, resources, 'read')
        return new ArrayList(authed.collect{projMap[it.name]})
    }

    def existsFrameworkProject(String project, Framework framework) {
        return framework.getFrameworkProjectMgr().existsFrameworkProject(project)
    }
    
    def getFrameworkProject(String project, Framework framework) {
        return framework.getFrameworkProjectMgr().getFrameworkProject(project)
    }
    /**
     * Return a map of the project's readme and motd content
     * @param project
     * @param framework
     * @return [readme: "readme content", readmeHTML: "rendered content", motd: "motd content", motdHTML: "readnered content"]
     */
    def getFrameworkProjectReadmeContents(String project, Framework framework){
        def project1 = getFrameworkProject(project,framework)
        def readme = new File(project1.baseDir, "readme.md")
        def motd = new File(project1.baseDir, "motd.md")
        def result=[:]
        if (motd.exists() && motd.isFile()) {
            //load file and apply markdown
            result.motd= motd.text
            result.motdHTML= result.motd?.decodeMarkdown()
        }
        if (readme.exists() && readme.isFile()) {
            //load file and apply markdown
            result.readme= readme.text
            result.readmeHTML= result.readme?.decodeMarkdown()
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
                null != projectName ? getFrameworkProject(projectName, rundeckFramework).getPropertyRetriever() : null,
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
            rundeckFramework.filterNodeSet(selector, project, null)
        }
    }

    public INodeSet filterAuthorizedNodes(final String project, final Set<String> actions, final INodeSet unfiltered,
                                          AuthContext authContext) {
        return rundeckFramework.filterAuthorizedNodes(project,actions,unfiltered,authContext)
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
        return [type:'job',name:name,group:groupPath?:'']
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
               authorizeProjectResourceAll(authContext, [type: 'adhoc'], actions, exec.project)

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
                    adhocauth=authorizeProjectResourceAll(authContext, [type: 'adhoc'], actions, exec.project)
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
        def Set decisions = metricService.withTimer(this.class.name,'authorizeApplicationResourceAll') {
            authContext.evaluate(
                [resource] as Set,
                actions as Set,
                Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        }

        return !(decisions.find {!it.authorized})
    }
    /**
     * return true if the action is authorized for the resource type in the application context
     * @param framework
     * @param resourceType
     * @param action
     * @return
     */
    def boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        def decision = metricService.withTimer(this.class.name,'authorizeApplicationResourceType') {
            authContext.evaluate(
                    [type: 'resource', kind: resourceType],
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
        def Set decisions= metricService.withTimer(this.class.name,'authorizeApplicationResourceType') {
            authContext.evaluate(
                    [[type: 'resource', kind: resourceType]] as Set,
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
        return new HashSet(aclpolicies.hackMeSomeRoles())
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

    public AuthContext getAuthContextForSubject(Subject subject) {
        if (!subject) {
            throw new RuntimeException("getAuthContextForSubject: Cannot get AuthContext without subject")
        }
        return new SubjectAuthContext(subject, aclpolicies)
    }
    public AuthContext getAuthContextForUserAndRoles(String user, List rolelist) {
        if (!(null != user && null != rolelist)) {
            throw new RuntimeException("getAuthContextForUserAndRoles: Cannot get AuthContext without user, roles: ${user}, ${rolelist}")
        }
        //create fake subject
        Subject subject = new Subject()
        subject.getPrincipals().add(new Username(user))
        rolelist.each { String s ->
            subject.getPrincipals().add(new Group(s))
        }
        return new SubjectAuthContext(subject, aclpolicies)
    }


    /**
     * Create a map of option name to value given an input argline.
     * Supports the form "-option value".  Tokens not in that form are ignored. The string
     * can have quoted values, using single or double quotes, and allows double/single to be
     * embedded. To embed single/single or double/double, the quotes should be repeated.
     */
    def Map<String,String> parseOptsFromString(String argstring){
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
    def Map<String,String> parseOptsFromArray(String[] tokens){
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
    def getPluginDescriptionForItem(Framework framework, PluginStep step) {
        try {
            return step.nodeStep ? getNodeStepPluginDescription(framework, step.type) : getStepPluginDescription(framework, step.type)
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
    def Description getNodeStepPluginDescription(Framework framework, String type){
        framework.getNodeStepExecutorService().providerOfType(type).description
    }
    /**
     * Return step plugin description of a certain type
     * @param framework
     * @param type
     * @return
     */
    def Description getStepPluginDescription(Framework framework, String type){
        framework.getStepExecutionService().providerOfType(type).description
    }
    /**
     * Return the list of NodeStepPlugin descriptions
     * @param framework
     * @return
     */
    def List getNodeStepPluginDescriptions(Framework framework){
        framework.getNodeStepExecutorService().listDescriptions()
    }

    /**
     * Return the list of StepPlugin descriptions
     * @param framework
     * @return
     */
    def List getStepPluginDescriptions(Framework framework){
        framework.getStepExecutionService().listDescriptions()
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
        final provider
        try {
            provider = service.providerOfType(type)
        } catch (ExecutionServiceException e) {
            result.error = e.message
        }
        if (provider && !(provider instanceof Describable)) {
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
}
