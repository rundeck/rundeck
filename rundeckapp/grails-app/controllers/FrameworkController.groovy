import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.Framework
import grails.converters.*
import com.dtolabs.rundeck.core.common.Nodes
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.utils.NodeSet
import java.util.regex.PatternSyntaxException
import com.dtolabs.shared.resources.ResourceXMLGenerator
import com.dtolabs.rundeck.core.common.NodesYamlGenerator
import com.dtolabs.rundeck.core.common.NodesFileGenerator
import com.dtolabs.rundeck.core.common.NodesGeneratorException
import com.dtolabs.utils.Streams

class FrameworkController  {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    RoleService roleService
    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
        apiProjectResources: [ 'POST'],
        apiProjectResourcesRefresh: ['POST'],
    ]

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
            Map filterpref = userService.parseKeyValuePref(u.filterPref)
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(query.nodeFilterIsEmpty()){
            if(params.formInput=='true' && 'true'!=params.defaultLocalNode){
                query.nodeIncludeName = '.*'
            }else{
                query.nodeIncludeName = framework.getFrameworkNodeName()
            }
        }
        FrameworkController.autosetSessionProject(session,framework)
        if(query && !query.project && session.project){
            query.project=session.project
        }
        if(!query.project){
            request.error="No project selected"
            return [allnodes: [],
                params:params,
                total:0,
                query:query]
        }
        def allnodes = [:]
        def totalexecs = [:]
        def total=0
        def allcount=0
        NodeSet nset = ExecutionService.filtersAsNodeSet(query)
        def projects=[]
        def filterErrors=[:]
        def project = framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        def nodes
        final Nodes nodes1 = project.getNodes()
        allcount=nodes1.countNodes()
        if(params.localNodeOnly){
            nodes=[nodes1.getNode(framework.getFrameworkNodeName())]
        }
        else if (nset && !(nset.include.blank && nset.exclude.blank)){
            //match using nodeset unless all filters are blank
            try {
                nodes= nodes1.filterNodes(nset)
            } catch (PatternSyntaxException e) {
                request.error='<pre>'+e.getMessage()+'</pre>'
                filterErrors['filter']='<pre>'+e.getMessage()+'</pre>'
                nodes=[]
            }
        }else if("true"==params.defaultAllNodes){
            //match all nodes if filters are all blank
            nodes = nodes1.listNodes()
        }else{
            //match only local node if filters are blank
            nodes=[nodes1.getNode(framework.getFrameworkNodeName())]
        }
//            nodes = nodes.sort { INodeEntry a, INodeEntry b -> return a.nodename.compareTo(b.nodename) }
        total=nodes.size()
        def tagsummary=[:]
        
        def page=-1;
        def max=-1;
        def remaining=false;
        if(params.page){
            page=Integer.parseInt(params.page)
            if(params.max){
                max=Integer.parseInt(params.max)
            }else{
                max=20
            }
            if(page<0){
                //if page is negative, load all remaining values starting at page -1*page
                remaining=true;
                page=page*-1;
            }
        }

        def count=0;
        nodes.each{INodeEntry nd->
            if(null!=nd){
                if(page>=0 && (count<(page*max) || count >=((page+1)*max) && !remaining)){
                    count++;
                    return
                }
                count++;
                if(params.fullresults){
                    allnodes[nd.nodename]=[node:nd,projects:[project],project:project,executions:[],resources:[],islocal:nd.nodename==framework.getFrameworkNodeName()]
                }
                //summarize tags
                def tags = nd.getTags()
                if(tags){
                    tags.each{ tag->
                        if(!tagsummary[tag]){
                            tagsummary[tag]=1
                        }else{
                            tagsummary[tag]++
                        }
                    }
                }

            }
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
            nodesvalid:nodes1.valid,
            nodeserror:nodes1.parserException,
            nodesfile:nodes1.file,
            params:params,
            total:total,
            allcount:allcount,
            tagsummary:tagsummary,
            page:page,
            max:max,
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
        if(!result.nodesvalid){
            request.error="Error parsing file \"${result.nodesfile}\": "+result.nodeserror? result.nodeserror.message:'no message'
        }
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
    def performNodeReload = {String url=null->
        if(params.project){
            if(roleService.isUserInAnyRoles(request,['admin','nodes_admin'])){
                Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
                def project=framework.getFrameworkProjectMgr().getFrameworkProject(params.project)
               //if reload parameter is specified, and user is admin, reload from source URL
                try {
                    if(url){
                        if(!(url==~ /(?i)^(https?|file):\/\/.*$/)){
                            log.error("Error updating node resources file for project ${project.name}: invalid URL: " + url)
                            flash.error = "Error updating node resources file for project ${project.name}: invalid URL: " + url
                            return false
                        }
                        project.updateNodesResourceFileFromUrl(url, null, null)
                        return true
                    }else{
                        return project.updateNodesResourceFile()
                    }
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
        def error=null
        if(request.method=='POST'){
            //only attempt project create if form POST is used
            if(!project){
                error="Project name must be specified"
            }else if (framework.getFrameworkProjectMgr().existsFrameworkProject(project)){
                log.error("Project already exists: ${project}")
                error="Project already exists: ${project}"
            }else{
                def proj
                try {
                    proj=framework.getFrameworkProjectMgr().createFrameworkProject(project)
                } catch (Error e) {
                    error= e.getMessage()
                }
                if(!error && proj){
                    session.project=proj.name
                    def result=userService.storeFilterPref(session.user, [project:proj.name])
                    return redirect(controller:'menu',action:'index')
                }
            }
        }
        if(error){
            request.error=error
        }
        def projects=frameworkService.projects(framework)
        return [projects:projects,project:session.project]
    }

    def projectSelect={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects=frameworkService.projects(framework)
        [projects:projects,project:session.project]
    }
    def selectProject= {
        if (null != params.project) {
            session.project = params.project
            //also set User project filter pref
            def result=userService.storeFilterPref(session.user, [project:params.project])
            if(result.error){
                log.warn("Error saving user project preference: "+result.error)
            }
        } else {
            session.removeAttribute('project')
            def result=userService.storeFilterPref(session.user, [project:'!'])
            if(result.error){
                log.warn("Error saving user project preference: "+result.error)
            }
        }
        render params.project
    }

    static autosetSessionProject( session, Framework framework) {
        def projects=new ArrayList(framework.getFrameworkProjectMgr().listFrameworkProjects())
        if(null==session.project && 1==projects.size()){
            session.project=projects[0].name
        }else if(0==projects.size()){
            session.removeAttribute('project')
        }
    }

    /*******
     * API actions
     */

    /**
     * API: /api/project/NAME/resources/refresh
     * calls performNodeReload, then returns API response
     * */
    def apiProjectResourcesRefresh = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!params.project) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['project'])
            return new ApiController().error()
        }
        def exists = frameworkService.existsFrameworkProject(params.project, framework)
        if (!exists) {
            flash.error = g.message(code: 'api.error.item.doesnotexist', args: ['project', params.project])
            return new ApiController().error()
        }

        //check content
        def didsucceed = performNodeReload(params.providerURL)
        if(didsucceed){
            return new ApiController().success { delegate ->
                delegate.'success' {
                    message(g.message(code: 'api.project.updateResources.succeeded', args: [params.project]))
                }
            }
        }else{
            if(!flash.error){
                flash.error= g.message(code: 'api.project.updateResources.failed', args: [params.project])
            }
            return new ApiController().error()
        }
    }
    /**
     * API: /api/project/NAME/resources
     * GET: return resources data
     * POST: update resources data with either: text/xml content, text/yaml content, form-data param providerURL=<url>
     * */
    def apiProjectResources = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!params.project) {
            flash.error = g.message(code: 'api.error.parameter.required', args: ['project'])
            return new ApiController().error()
        }
        def exists = frameworkService.existsFrameworkProject(params.project, framework)
        if (!exists) {
            flash.error = g.message(code: 'api.error.item.doesnotexist', args: ['project', params.project])
            return new ApiController().error()
        }
        final FrameworkProject project = frameworkService.getFrameworkProject(params.project, framework)

        def didsucceed=false
        def errormsg=null
        //determine data
        //assume post request
        if(request.post){
            final contentType = request.contentType
            //try to parse loaded data
            final Nodes.Format format
            if(contentType.endsWith("/xml")){
                format=Nodes.Format.resourcexml
            }else if(contentType.endsWith('/yaml')|| contentType.endsWith('/x-yaml')){
                format = Nodes.Format.resourceyaml
            }else {
                flash.error = "Unexpected content type: ${contentType}"
                return new ApiController().error()
            }
            //write content to temp file
            File tempfile=File.createTempFile("post-input","data")
            tempfile.deleteOnExit()
            Streams.copyStream(request.getInputStream(),new FileOutputStream(tempfile))


            final nodes = Nodes.create(project, tempfile, format)
            if(!nodes.isValid()){
                //invalid data
                flash.error = "Invalid data: ${nodes.getParserException().getMessage()}"
                return new ApiController().error()
            }

            //finally update resources file with the new nodes data
            try {
                project.updateNodesResourceFile nodes
                didsucceed=true
            } catch (Exception e) {
                log.error("Failed updating nodes file: "+e.getMessage())
                e.printStackTrace(System.err)
                flash.error=e.getMessage()
            }
        }

        if(didsucceed){
            return new ApiController().success { delegate ->
                delegate.'success' {
                    message(g.message(code: 'api.project.updateResources.succeeded', args: [params.project]))
                }
            }
        }else{
            if(!flash.error){
                flash.error= g.message(code: 'api.project.updateResources.failed', args: [params.project])
            }
            return new ApiController().error()
        }
    }
    /**
     * API: /api/projects, version 1
     */
    def apiProjects={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projlist=frameworkService.projects(framework)
        return new ApiController().success{ delegate->
                delegate.'projects'(count:projlist.size()){
                    projlist.each{ pject ->
                        renderApiProject(pject,delegate)
                    }
                }

        }
    }
    /**
     * Render project info result using a builder
     */
    def renderApiProject={ pject, delegate ->
        delegate.project{
            name(pject.name)
            description(pject.hasProperty('project.description')?pject.getProperty('project.description'):'')
            if(pject.hasProperty("project.resources.url")){
                resources{
                    providerURL(pject.getProperty("project.resources.url"))
                }
            }
        }
    }

    /**
     * Convert input node filter parameters into specific property names used by
     * domain objects
     */
    public static Map extractApiNodeFilterParams(Map params){
        def result=[:]

        //convert api parameters to node filter parameters
        BaseNodeFilters.filterKeys.each{k,v->
            if(params[k]){
                result["nodeInclude${v}"]=params[k]
            }
            if(params["exclude-"+k]){
                result["nodeExclude${v}"]=params["exclude-"+k]
            }
        }
        if(params.'exclude-precedence'){
            result.nodeExcludePrecedence=params['exclude-precedence']=='true'
        }
        return result
    }

    /**
     * API: /api/project/NAME, version 1
     */
    def apiProject={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }
        def pject=frameworkService.getFrameworkProject(params.project,framework)
        return new ApiController().success{ delegate->
            delegate.'projects'(count:1){
                renderApiProject(pject,delegate)
            }
        }
    }

    /**
     * API: /api/resource/$name, version 1
     */
    def apiResource={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        if(!params.name){
            flash.error=g.message(code:'api.error.parameter.required',args:['name'])
            return chain(controller:'api',action:'error')
        }
        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }

        NodeSet nset = new NodeSet()
        nset.setSingleNodeName(params.name)
        def pject=frameworkService.getFrameworkProject(params.project,framework)
        final Collection nodes = pject.getNodes().filterNodes(nset)
        if(!nodes || nodes.size()<1 ){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['Node Name',params.name])
            return chain(controller:'api',action:'error')
        }
        return apiRenderNodeResult(nodes)
    }
    /**
     * API: /api/resources, version 1
     */
    def apiResources={ExtNodeFilters query->
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }

        //convert api parameters to node filter parameters
        def filters=extractApiNodeFilterParams(params)
        if(filters){
            filters.each{k,v->
                query[k]=v
            }
        }

        if(query.nodeFilterIsEmpty()){
            //return all results
            query.nodeInclude=".*"
        }
        def pject=frameworkService.getFrameworkProject(params.project,framework)
        final Collection nodes = pject.getNodes().filterNodes(ExecutionService.filtersAsNodeSet(query))
        return apiRenderNodeResult(nodes)
    }
    def apiRenderNodeResult={nodes->

        withFormat{
            xml{
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final NodesFileGenerator generator = new ResourceXMLGenerator(baos)
                nodes.each {INodeEntry node->
                    generator.addNode(node)
                }
                generator.generate()
                return render(contentType:"text/xml",encoding:"UTF-8",text:baos.toString())
            }
            yaml{
                if(nodes.size()>0){
                    StringWriter sw = new StringWriter()
                    final NodesFileGenerator generator = new NodesYamlGenerator(sw)
                    nodes.each {INodeEntry node->
                        generator.addNode(node)
                    }
                    generator.generate()
                    return render(contentType:"text/yaml",encoding:"UTF-8",text:sw.toString())
                }else{
                    return render(contentType:"text/yaml",encoding:"UTF-8",text:"# 0 results for query\n")
                }
            }
        }
    }
}

