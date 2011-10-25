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
    def authorizeProjectResource(Framework framework, Map resource, String action, String project){

        def Decision decision=framework.getAuthorizationMgr().evaluate(
            resource,
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        return decision.isAuthorized()
    }
    def authorizeProjectResourceAll(Framework framework, Map resource, Collection actions, String project){

        def decisions=framework.getAuthorizationMgr().evaluate(
            [resource] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        return !(decisions.find {!it.authorized})
    }
    def authorizeProjectJobAll(Framework framework, ScheduledExecution job, Collection actions, String project){

        def decisions=framework.getAuthorizationMgr().evaluate(
            [[type:'job',job:job.jobName,group:job.groupPath?:'']] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)))
        return !(decisions.find {!it.authorized})
    }

    def authorizeApplicationResource(Framework framework, Map resource, String action) {

        def Decision decision = framework.getAuthorizationMgr().evaluate(
            resource,
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        return decision.isAuthorized()
    }

    def authorizeApplicationResourceAll(Framework framework, Map resource, Collection actions) {


        def Set decisions = framework.getAuthorizationMgr().evaluate(
            [resource] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))

        return !(decisions.find {!it.authorized})
    }
    def authorizeApplicationResourceType(Framework framework, String resourceType, String action) {

        def Decision decision =framework.getAuthorizationMgr().evaluate(
            [type: 'resource', kind: resourceType],
            framework.getAuthenticationMgr().subject,
            action,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))
        return decision.isAuthorized()
    }
    def authorizeApplicationResourceTypeAll(Framework framework, String resourceType, Collection actions) {


        def Set decisions =framework.getAuthorizationMgr().evaluate(
            [[type: 'resource', kind: resourceType]] as Set,
            framework.getAuthenticationMgr().subject,
            actions as Set,
            Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "application"), 'rundeck')))

        return !(decisions.find {!it.authorized})
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
    def getFramework(){
        return getFrameworkForUserAndRoles(null,null)
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
