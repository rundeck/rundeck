import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.authorization.*
import com.dtolabs.rundeck.core.utils.*

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import com.dtolabs.rundeck.core.Constants
import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.Group

/**
 * Interfaces with the core Framework object
 */
class FrameworkService implements ApplicationContextAware {

    boolean transactional = false

    boolean initialized = false
    def File varDir
    String rundeckbase
    String depsdir

    def ApplicationContext applicationContext
    def ExecutionService executionService
    def projects

    // implement ApplicationContextAware interface
    def void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    def getRundeckBase(){
        if (!initialized) {
            initialize()
        }
        return rundeckbase;
    }


    // Initailize the Framework
    def initialize() {
        if (!applicationContext){
            throw new IllegalStateException("ApplicationContext instance not found!")
        }
        def props = applicationContext.getServletContext().getAttribute("FRAMEWORK_PROPERTIES")
        if (!props){
            throw new IllegalStateException("Could not obtain FRAMEWORK_PROPERTIES from servlet context")
        }
        def propkeys = ["rdeck.base", "framework.projects.dir", "framework.var.dir"]
        propkeys.each {
            if (!props.containsKey(it)) throw new RuntimeException("framework property not found: "+ it)
        }
        rundeckbase=applicationContext.getServletContext().getAttribute("RDECK_BASE")
        
        depsdir= props.getProperty("framework.projects.dir")
        log.warn("rdeck.base is: "+rundeckbase)

        initialized = true 
    }
   
    def getVarDir() {
        return varDir
    }

    /**
     * Return a list of FrameworkProject objects
     */
    def projects (Framework framework) {
        //authorize the list of projects
        def projs = framework.getFrameworkProjectMgr().listFrameworkProjects()
        def allowed=projs.findAll { FrameworkProject fp->
            authorizeApplicationResource(framework,[type:'project',name:fp.getName()],'read')
        }
        return new ArrayList(allowed)
    }

    def existsFrameworkProject(String project, Framework framework) {
        return framework.getFrameworkProjectMgr().existsFrameworkProject(project)
    }
    
    def getFrameworkProject(String project, Framework framework) {
        return framework.getFrameworkProjectMgr().getFrameworkProject(project)
    }

    /**
     *  return true if the user is authorized to execute a script in the given project
     *
     * @param user username
     * @param project project name
     * @param script string of script
     * @param framework framework
     */
    def userAuthorizedForScript(user,project,script,Framework framework){

        return framework.getAuthorizationMgr().authorizeScript(user,project,script)
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
    def Set authorizeProjectResources( framework, Set resources, Set actions, String project) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        def Set decisions = framework.getAuthorizationMgr().evaluate(
            resources,
            framework.getAuthenticationMgr().subject,
            actions,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
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
    def boolean authorizeProjectResource(framework, Map resource, String action, String project){
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        def decision=framework.getAuthorizationMgr().evaluate(
            resource,
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
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
    def boolean authorizeProjectResourceAll(framework, Map resource, Collection actions, String project){
        if(null==project){
            throw new IllegalArgumentException("null project")
        }
        def decisions=framework.getAuthorizationMgr().evaluate(
            [resource] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        return !(decisions.find {!it.authorized})
    }

    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    def authorizeProjectJobAll( framework, ScheduledExecution job, Collection actions, String project){
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        def decisions=framework.getAuthorizationMgr().evaluate(
            [authResourceForJob(job)] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        return !(decisions.find {!it.authorized})
    }

    /**
     * return true if the action is authorized for the resource in the application context
     * @param framework
     * @param resource
     * @param action
     * @return
     */
    def boolean authorizeApplicationResource(framework, Map resource, String action) {

        def decision = framework.getAuthorizationMgr().evaluate(
            resource,
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        return decision.authorized
    }

    /**
     * return true if all of the actions are authorized for the resource in the application context
     * @param framework
     * @param resource
     * @param actions
     * @return
     */
    def boolean authorizeApplicationResourceAll(framework, Map resource, Collection actions) {


        def Set decisions = framework.getAuthorizationMgr().evaluate(
            [resource] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))

        return !(decisions.find {!it.authorized})
    }
    /**
     * return true if the action is authorized for the resource type in the application context
     * @param framework
     * @param resourceType
     * @param action
     * @return
     */
    def boolean authorizeApplicationResourceType(framework, String resourceType, String action) {

        def decision =framework.getAuthorizationMgr().evaluate(
            [type: 'resource', kind: resourceType],
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        return decision.authorized
    }
    /**
     * return true if all of the actions are authorized for the resource type in the application context
     * @param framework
     * @param resourceType
     * @param actions
     * @return
     */
    def boolean authorizeApplicationResourceTypeAll(framework, String resourceType, Collection actions) {


        def Set decisions =framework.getAuthorizationMgr().evaluate(
            [[type: 'resource', kind: resourceType]] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        return !(decisions.find {!it.authorized})
    }

    def getFrameworkNodeName() {
        def rdbase= getRundeckBase()
        def Framework fw = Framework.getInstance(rdbase)
        fw.setAuthorizationMgr(new DenyAuthorization(fw, new File(Constants.getFrameworkConfigDir(rdbase))))
        return fw.getFrameworkNodeName()
    }
    def getFrameworkFromUserSession( session, request){
        if (!initialized) {
            initialize()
        }
        if(!session.Framework){
            String rundeckbase=getRundeckBase()

            session.Framework = getFrameworkForUserAndSubject(session.user, request.subject, rundeckbase)
        }
        return session.Framework;
    }
    def getFrameworkForUserAndRoles(String user, List rolelist){
        return getFrameworkForUserAndRoles(user, rolelist, getRundeckBase())
    }
    public static Framework getFrameworkForUserAndRoles(String user, List rolelist, String rundeckbase){
        def Framework fw = Framework.getInstance(rundeckbase)
        if(null!=user && null != rolelist){
            //create fake subject
            Subject subject = new Subject()
            subject.getPrincipals().add(new Username(user))
            rolelist.each{ String s->
                subject.getPrincipals().add(new Group(s))
            }
            def authen = new SingleUserAuthentication(user,subject)
            def author = new SingleUserAclsAuthorization(fw,new File(Constants.getFrameworkConfigDir(rundeckbase)), user, rolelist.toArray(new String[0]))
            fw.setAuthenticationMgr(authen)
            fw.setAuthorizationMgr(author)
        }else{
            System.err.println("getFrameworkForUserAndRoles: No user/subject authorization")
            throw new RuntimeException("Cannot get framework without user, roles: ${user}, ${rolelist}")
        }
        fw.setAllowUserInput(false)
        return fw
    }
    public static Framework getFrameworkForUserAndSubject(String user, Subject subject, String rundeckbase){
        def Framework fw = Framework.getInstance(rundeckbase)
        if(null!=user && null!=subject){
            def authen = new SingleUserAuthentication(user,subject)
            def author = new UserSubjectAuthorization(fw,new File(Constants.getFrameworkConfigDir(rundeckbase)), user, subject)
            fw.setAuthenticationMgr(authen)
            fw.setAuthorizationMgr(author)
        } else {
            System.err.println("getFrameworkForUserAndSubject: No user/subject authorization")
            throw new RuntimeException("Cannot get framework without user, subject: ${user}, ${subject}")
        }
        fw.setAllowUserInput(false)
        return fw
    }

    /**
     * Create a map of option name to value given an input argline.
     * Supports the form "-option value", and boolean "-option", which is given
     * the value of "true".  Other options are ignored. if a double-dash
     * is seen it is not interpreted, and --option is parsed as option name "-option".
     */
    def Map<String,String> parseOptsFromString(String argstring){
        if(!argstring){
            return null;
        }
        def String[] tokens=com.dtolabs.rundeck.core.utils.OptsUtil.burst(argstring)
        return parseOptsFromArray(tokens)
    }
    def Map<String,String> parseOptsFromArray(String[] tokens){
        def Map<String,String> optsmap = new HashMap<String,String>()
        def String key=null
        for(int i=0;i<tokens.length;i++){
            if(tokens[i].startsWith("-") && tokens[i].length()>1){
                if(key){
                    //previous key was boolean flag, set to true
                    optsmap[key]="true"
                    key=null
                }
                key=tokens[i].substring(1)
            }else if(key){
                optsmap[key]=tokens[i]
                key=null
            }
        }
        if(key){
            optsmap[key]="true"
            key=null
        }
        return optsmap
    }
    
}
