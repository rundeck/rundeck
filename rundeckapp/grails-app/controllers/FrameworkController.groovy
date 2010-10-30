import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.Framework
import grails.converters.*
import com.dtolabs.rundeck.core.common.Nodes
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.utils.NodeSet
import java.util.regex.PatternSyntaxException

class FrameworkController  {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    RoleService roleService

    def index = {
        redirect(action:"nodes")        
    }

    /**
     * This action returns a json object informing about whether the user is authorized
     * to run scripts in the current project context.
     * @param project the project name
     * @return object [authorized:true/false, found:true/false, project:project]
     */
    def testScriptAuth = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projectexists=true
        def auth=false
        if(!params.project || !frameworkService.existsFrameworkProject(params.project,framework)){
            projectexists=false
        } else {
            auth = frameworkService.userAuthorizedForScript(session.user,params.project,"*",framework)
        }
        render( [authorized:auth,found:projectexists,project:params.project] as JSON)
    }

    /**
     * Nodes action lists nodes in resources view, also called by nodesFragment and nodesData to
     * render a set of nodes via ajax
     */
    def nodes = { ExtNodeFilters query->

        def User u = userService.findOrCreateUser(session.user)
        def usedFilter=null
        if(!params.filterName && u && query.nodeFilterIsEmpty() && params.formInput!='true'){
            Map filterpref = UserController.parseKeyValuePref(u.filterPref)
            if(filterpref['nodes']){
                params.filterName=filterpref['nodes']
            }
        }
        if(params.filterName){
            //load a named filter and create a query from it
            if(u){
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName,u)
                if(filter){
                    def query2 = filter.createExtNodeFilters()
                    //XXX: node query doesn't use pagination, as it is not an actual DB query
                    query=query2
                    def props=query.properties
                    params.putAll(props)
                    usedFilter=params.filterName
                }
            }
        }

        if(params['Clear']){
            query=new ExtNodeFilters()
            usedFilter=null
        }
        if(query && !query.project && session.project){
            query.project=session.project
        }
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def allnodes = [:]
        def nodesbyproject = [:]
        def totalexecs = [:]
        def total=0
        NodeSet nset = ExecutionService.filtersAsNodeSet(query)
        def projects=[]
        def filterErrors=[:]
        projects=frameworkService.projects(framework)
        projects.each{FrameworkProject project->
            if(query.project ){
                try{
                    if(!NodeSet.matchRegexOrEquals(query.project,project.name)){
                        return
                    }
                }catch(PatternSyntaxException e){
                    filterErrors['project']='<pre>'+e.getMessage()+'</pre>'
                    return
                }
            }
            def nodes
            final Nodes nodes1 = project.getNodes()
            if(params.localNodeOnly){
                nodes=[nodes1.getNode(framework.getFrameworkNodeName())]
            }
            else if(nset && !(nset.include.blank && nset.exclude.blank)){
                //match using nodeset unless all filters are blank
                try {
                    nodes= nodes1.filterNodes(nset)
                } catch (PatternSyntaxException e) {
                    request.error='<pre>'+e.getMessage()+'</pre>'
                    filterErrors['filter']='<pre>'+e.getMessage()+'</pre>'
                    nodes=[]
                }
            }else if("true"==params.defaultLocalNode){
                //match only local node if filters are blank
                nodes=[nodes1.getNode(framework.getFrameworkNodeName())]
            }else{
                //match all nodes if filters are all blank
                nodes = nodes1.listNodes()
            }
//            nodes = nodes.sort { INodeEntry a, INodeEntry b -> return a.nodename.compareTo(b.nodename) }
            nodes.each{INodeEntry nd->
                if(null!=nd){
                    if(!allnodes[project.getName()+"/"+nd.nodename]){
                        total++
                        allnodes[project.getName()+"/"+nd.nodename]=[node:nd,projects:[project],project:project,executions:[],resources:[]]
                    }else{
                        allnodes[project.getName()+"/"+nd.nodename].projects<<project
                    }
                }
            }
            nodesbyproject[project.name]=[nodes:nodes,executions:[:]]
        }
        if(filterErrors){
            request.filterErrors=filterErrors
        }

        //get list of running jobs organized by node.
        /*def runningset=executionService.listNowRunning(framework,params.max?Math.max(Integer.parseInt(params.max),30):10)
        runningset.nowrunning.each{ Execution e->
            //determine project/node that the execution is running on
            def proj=e.project
            def dataset=nodesbyproject[proj]
            if(e.adhocExecution || e.doNodedispatch){
                def NodeSet nodeset = executionService.filtersAsNodeSet(e)
                dataset.nodes.each{INodeEntry node->
                    if(!nodeset.shouldExclude(node) || !e.doNodedispatch){
                        if(!dataset.executions[node.getNodename()]){
                            dataset.executions[node.getNodename()]=[e]
                        }else{
                            dataset.executions[node.getNodename()]<<e
                        }
                        allnodes[node.getNodename()].executions<<e
                        if(!totalexecs[node.getNodename()]){
                            totalexecs[node.getNodename()]=1
                        }else{
                            totalexecs[node.getNodename()]++
                        }
                    }
                }
            }else{
                //job is executing locally on the framework server node
                final String nodename = framework.getFrameworkNodeName()
                if(!dataset.executions[nodename]){
                    dataset.executions[nodename]=[e]
                }else{
                    dataset.executions[nodename]<<e
                }
                allnodes[nodename].executions<<e
                if(!totalexecs[nodename]){
                    totalexecs[nodename]=1
                }else{
                    totalexecs[nodename]++
                }
            }
            
        }
*/
        def resources=[:]
      
        def model=[
            allnodes: allnodes,
            nodesbyproject:nodesbyproject,
            params:params,
            total:total,
//            totalexecs:totalexecs,
//            jobs:runningset.jobs,
            resources:resources,
            query:query
        ]

        if(query.project && framework.getFrameworkProjectMgr().existsFrameworkProject(query.project)){
            model.selectedProject=framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        }

        if(usedFilter){
            model['filterName']=usedFilter
        }
        return model
    }
    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesFragment = {ExtNodeFilters query->
        def result = nodes(query)
        render(template:"allnodes",model: result)
    }
    /**
     * Render JSON data for queried nodes
     */
    def nodesData = {ExtNodeFilters query->
        def result = nodes(query)
        if(params.project){
            render result.allnodes[params.project].nodes as JSON
        }else{
            render result.allnodes as JSON
        }
    }

    /**
     * calls performNodeReload, then redirects to 'nodes' action (for normal request), or returns JSON 
     * results (for ajax request). JSON format: {success:true/false, message:string}
     */
    def reloadNodes = {
        def didsucceed=performNodeReload()
        withFormat {
            json{
                def data=[success:didsucceed,message:didsucceed?"Remote resources loaded for project: ${params.project}":"Failed to load remote resources for project: ${params.project}"]
                render data as JSON
            }
            html{
                redirect(action:'nodes')
            }
        }
    }

    /**
     * If user has admin rights and the project parameter is specified, attempt to re-fetch the resources.xml
     * via the project's project.resources.url (if it exists).
     * Returns true if re-fetch succeeded, false otherwise.
     */
    def performNodeReload = {
        if(params.project){
            if(roleService.isUserInAnyRoles(request,['admin','nodes_admin'])){
                Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
                def project=framework.getFrameworkProjectMgr().getFrameworkProject(params.project)
               //if reload parameter is specified, and user is admin, reload from source URL
                try {
                    project.updateNodesResourceFile()
                    return true
                } catch (Exception e) {
                    log.error("Error updating node resources file for project ${project.name}: "+e.message)
                    flash.error="Error updating node resources file for project ${project.name}: "+e.message
                }
            }
        }
        return false
    }

    def storeNodeFilter={ExtNodeFilters query->
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def NodeFilter filter
        def boolean saveuser=false
        if(params.newFilterName && !params.existsFilterName){
            def ofilter = NodeFilter.findByNameAndUser(params.newFilterName,u)
            if(ofilter){
                flash.error="Filter named ${params.newFilterName} already exists."
                params.saveFilter=true
                return chain(controller:'framework',action:'nodes',params:params)
            }
            filter= new NodeFilter(query.properties)
            filter.name=params.newFilterName
            u.addToNodefilters(filter)
            saveuser=true
        }else if(params.existsFilterName){
            filter = NodeFilter.findByNameAndUser(params.existsFilterName,u)
            if(filter){
                filter.properties=query.properties
            }
        }else if(!params.newFilterName && !params.existsFilterName){
            flash.error="Filter name not specified"
            params.saveFilter=true
            return chain(controller:'framework',action:'nodes',params:params)
        }
        if(!filter.save(flush:true)){
            flash.error=filter.errors.allErrors.collect { g.message(error:it) }.join("\n")
            params.saveFilter=true
            return chain(controller:'framework',action:'nodes',params:params)
        }
        if(saveuser){
            if(!u.save(flush:true)){
//                u.errors.allErrors.each { log.error(g.message(error:it)) }
//                flash.error="Unable to save filter for user"
                flash.error=u.errors.allErrors.collect { g.message(error:it) }.join("\n")
                return render(template:"/common/error")
            }
        }
        redirect(controller:'framework',action:params.fragment?'nodesFragment':'nodes',params:[filterName:filter.name])
    }
    def deleteNodeFilter={
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def filtername=params.delFilterName
        final def ffilter = NodeFilter.findByNameAndUser(filtername, u)
        if(ffilter){
            ffilter.delete(flush:true)
            flash.message="Filter deleted: ${filtername}"
        }
        redirect(controller:'framework',action:params.fragment?'nodesFragment':'nodes',params:[compact:params.compact?'true':''])
    }


    def createProject={
        def project=params.project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(framework.getFrameworkProjectMgr().existsFrameworkProject(project)){
            log.error("Project already exists: ${project}")
            request.error="Project already exists: ${project}"
            response.setStatus(500)
            return render(contentType:"application/json"){
//                 [error:"Project already exists: ${project}"].encodeAsJSON()
                error("Project already exists: ${project}")
            }
        }else{
            def proj=framework.getFrameworkProjectMgr().createFrameworkProject(project)
        }
        return selectProject()
    }

    def projectSelect={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects=frameworkService.projects(framework)
        session.projects=projects
        [projects:projects,project:session.project]
    }
    def selectProject={
        if(params.project){
            session.project=params.project
        }else{
            session.removeAttribute('project') 
        }
        render params.project
    }
}

