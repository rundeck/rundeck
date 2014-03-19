package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.plugins.configuration.Describable

import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.resources.FileResourceModelSource
import com.dtolabs.rundeck.core.resources.FileResourceModelSourceFactory
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.shared.resources.ResourceXMLGenerator

import grails.converters.JSON
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ApiService

import javax.servlet.http.HttpServletResponse
import java.util.regex.PatternSyntaxException
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.Framework

import com.dtolabs.rundeck.core.common.NodesFileGenerator
import com.dtolabs.rundeck.core.common.NodesYamlGenerator
import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.execution.service.FileCopierService

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.dtolabs.rundeck.core.common.ProviderService
import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.app.support.BaseNodeFilters
import com.dtolabs.rundeck.app.support.ExtNodeFilters
import rundeck.User
import rundeck.NodeFilter
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.UserService
import rundeck.filters.ApiRequestFilters

class FrameworkController extends ControllerBase {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    def metricService
    def ApiService apiService
    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
        apiProjectResources: [ 'POST'],
        apiProjectResourcesRefresh: ['POST'],
    ]

    def index = {
        redirect(action:"nodes")        
    }

    def noProjectAccess = {
        response.setStatus(403)
        def roles = request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.collect { it.name }?.join(", ")
        request.title = "Unauthorized"
        request.error = "No authorized access to projects. Contact your administrator. (User roles: " + roles + ")"
        response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, request.error)

        log.error("'${request.remoteUser}' has no authorized access. Roles: "+ roles)
        return renderErrorView([:])
    }

    def nodeFilterPresets = { ExtNodeFilters query->
        query.filter = 'name: .*'
        def model = nodesdata(query)
        def filterset=User.findByLogin(session.user)?.nodefilters
        render(template: 'nodeFilterPresets', model:model + [filterset:filterset])
    }
    def nodes ={ ExtNodeFilters query ->
        if(params.fromExecId|| params.retryFailedExecId) {
            return redirect(action: 'adhoc',params: params)
        }else if(params.exec){
            return redirect(action: 'adhoc', params: params)
        }
        def User u = userService.findOrCreateUser(session.user)
        def usedFilter = null
        if (params.filterName) {
            //load a named filter and create a query from it
            if (u) {
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                if (filter) {
                    def query2 = filter.createExtNodeFilters()
                    query = query2
                    params.filter=query.asFilter()
                    usedFilter = params.filterName
                }
            }
        }

        def summaryOnly = false
        if (params['Clear']) {
            query = new ExtNodeFilters()
            usedFilter = null
        }
        if(params.showall=='true'){
            query.filter = '.*'

        }else if (query.nodeFilterIsEmpty() && 'true'!=params.formInput) {
            query.filter = '.*'
            params.showall='true'
//            summaryOnly=true
            //filter all and summarize
        }
        if (query && !query.project && params.project) {
            query.project = params.project
        }
        def sortkeys = filterSummaryKeys(query)
        def model = [query: query, params: params, showFilter:true,filter:query.filter,colkeys:sortkeys]

        if (usedFilter) {
            model['filterName'] = usedFilter
        }

        return model + [summaryOnly: summaryOnly]
    }

    /**
     * Return the list of filter keys used in the query, excluding the 'name' key
     * @param query
     * @return
     */
    private List filterSummaryKeys(ExtNodeFilters query) {
        def filter = NodeSet.parseFilter(query.filter)
        def incset = filter.include.keySet()
        incset.removeAll(['name'])
        def excset = filter.exclude.keySet()
        excset.removeAll(['name'])
        return new ArrayList(incset + excset)
    }

    def adhoc = { ExtNodeFilters query ->
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResource(authContext, [type: 'adhoc'], AuthConstants.ACTION_RUN,
                params.project),
                AuthConstants.ACTION_RUN, 'adhoc', 'commands')) {
            return
        }
        String runCommand;
        if (params.fromExecId || params.retryFailedExecId) {
            Execution e = Execution.get(params.fromExecId ?: params.retryFailedExecId)
            if (e && unauthorizedResponse(
                    frameworkService.authorizeProjectExecutionAll(authContext, e, [AuthConstants.ACTION_READ]),
                    AuthConstants.ACTION_READ, 'Execution', params.fromExecId ?: params.retryFailedExecId)) {
                return
            }

            if (e && !e.scheduledExecution && e.workflow.commands.size() == 1) {
                def cmd = e.workflow.commands[0]
                if (cmd.adhocRemoteString) {
                    runCommand = cmd.adhocRemoteString
                    //configure node filters
                    if (params.retryFailedExecId) {
                        query = new ExtNodeFilters(filter: OptsUtil.join("name:", e.failedNodeList), project: e.project)
                    } else {
                        if(e.doNodedispatch){
                            query = ExtNodeFilters.from(e, e.project)
                        }else{
                            query=new ExtNodeFilters(filter: OptsUtil.join("name:", frameworkService.getFrameworkNodeName()),
                                    project: e.project)
                        }
                    }
                }
            }
        } else if (params.exec) {
            runCommand = params.exec
        }
        def usedFilter = null
        if (params.filterName) {
            def User u = userService.findOrCreateUser(session.user)
            //load a named filter and create a query from it
            if (u) {
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                if (filter) {
                    def query2 = filter.createExtNodeFilters()
                    //XXX: node query doesn't use pagination, as it is not an actual DB query
                    query = query2
                    def props = query.properties
                    params.putAll(props)
                    usedFilter = params.filterName
                }
            }
        }

        if (params['Clear']) {
            query = new ExtNodeFilters()
            usedFilter = null
        }

        if (query && !query.project && params.project) {
            query.project = params.project
        }
        def result
        if(!query.nodeFilterIsEmpty()){
            params.requireRunAuth='true'
            result = [query: query, params: params, allnodes: [:]]
        }else{
            result= [query: query, params: params, allnodes:[:]]
        }
        def model = result//[query: query, params: params]

        if (usedFilter) {
            model['filterName'] = usedFilter
        }
        return model + [runCommand: runCommand, emptyQuery: query.nodeFilterIsEmpty()]
    }

    /**
     * Nodes action lists nodes in resources view, also called by nodesFragment to
     * render a set of nodes via ajax
     */
    def nodesdata (ExtNodeFilters query){

        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if(query.nodeFilterIsEmpty()){
            if(params.formInput=='true' && 'true'!=params.defaultLocalNode){
                query.filter = 'name: .*'
            }else{
                query.nodeIncludeName = framework.getFrameworkNodeName()
            }
        }
        if(query && !query.project && params.project){
            query.project= params.project
        }
        if(!query.project){
            request.error="No project selected"
            return [allnodes: [:],
                params:params,
                total:0,
                query:query]
        }
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(authContext, [type: 'resource', kind: 'node'], [AuthConstants.ACTION_READ],
                        query.project),
                AuthConstants.ACTION_READ, 'Project', 'nodes')) {
            return
        }
        def allnodes = [:]
        def totalexecs = [:]
        def total=0
        def allcount=null
        NodeSet nset = ExecutionService.filtersAsNodeSet(query)
        def projects=[]
        def filterErrors=[:]
        def project = framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        def INodeSet nodeset

        INodeSet nodes1 = project.getNodeSet()
//        allcount=nodes1.nodes.size()
        if(params.localNodeOnly){
            nodeset=new NodeSetImpl()
            nodeset.putNode(nodes1.getNode(framework.getFrameworkNodeName()))
        }
        else if (nset && !(nset.include.blank && nset.exclude.blank)){
            //match using nodeset unless all filters are blank
            try {
                nodeset = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nset, nodes1)
            } catch (PatternSyntaxException e) {
                filterErrors['filter']=e.getMessage()
                nodeset=new NodeSetImpl()
            }
        }else if("true"==params.defaultAllNodes){
            //match all nodes if filters are all blank
            nodeset=nodes1
        }else{
            //match only local node if filters are blank
            nodeset = new NodeSetImpl()
//            nodeset.putNode(nodes1.getNode(framework.getFrameworkNodeName()))
        }
//            nodes = nodes.sort { INodeEntry a, INodeEntry b -> return a.nodename.compareTo(b.nodename) }
        //filter nodes by read authorization

        def readnodes = frameworkService.filterAuthorizedNodes(query.project, ['read'] as Set, nodeset, authContext)
        def runnodes = frameworkService.filterAuthorizedNodes(query.project, ['run'] as Set, readnodes, authContext)
        def noderunauthmap = [:]


        def nodes=params.requireRunAuth=='true'? runnodes.nodes:readnodes.nodes
        total= nodes.size()
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
                allnodes[nd.nodename]=[node:nd,projects:[project],project:project,executions:[],resources:[],islocal:nd.nodename==framework.getFrameworkNodeName()]
                if(params.requireRunAuth == 'true'  || runnodes.getNode(nd.nodename)){
                    noderunauthmap[nd.nodename]=true
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

        def parseExceptions= project.getResourceModelSourceExceptions()

        if(!query.filter){
            query.filter=NodeSet.generateFilter(nset)
        }
        def model=[
            allnodes: allnodes,
            nodesvalid: !parseExceptions,
            nodeserror: parseExceptions,
            nodeauthrun:noderunauthmap,
//            nodesfile:nodes1.file,
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


        return model
    }
    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesFragment = {ExtNodeFilters query->

        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(authContext, [type: 'resource', kind: 'node'],
                        [AuthConstants.ACTION_READ],
                        query.project),
                AuthConstants.ACTION_READ, 'Project', 'nodes')) {
            return
        }
        def User u = userService.findOrCreateUser(session.user)
        def usedFilter = null
        if (!params.filterName && u && query.nodeFilterIsEmpty() && params.formInput != 'true') {
            Map filterpref = userService.parseKeyValuePref(u.filterPref)
            if (filterpref['nodes']) {
                params.filterName = filterpref['nodes']
            }
        }
        if (params.filterName) {
            //load a named filter and create a query from it
            if (u) {
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                if (filter) {
                    def query2 = filter.createExtNodeFilters()
                    //XXX: node query doesn't use pagination, as it is not an actual DB query
                    query = query2
                    usedFilter = params.filterName
                }
            }
        }

        if (params['Clear']) {
            query = new ExtNodeFilters()
            usedFilter = null
        }
        if (query.nodeFilterIsEmpty()) {
            if ('true' != params.defaultLocalNode) {
                query.filter = '.*'
            } else {
                query.nodeIncludeName = framework.getFrameworkNodeName()
            }
        }
        if (query && !query.project && params.project) {
            query.project = params.project
        }
        def result = nodesdata(query)
        result.colkeys= filterSummaryKeys(query)
        if (usedFilter) {
            result['filterName'] = usedFilter
        }
        if(!result.nodesvalid){
            request.error="Error parsing file \"${result.nodesfile}\": "+result.nodeserror? result.nodeserror*.message?.join("\n"):'no message'
        }
        render(template:"allnodes",model: result)
    }

    /**
     * calls performNodeReload, then redirects to 'nodes' action (for normal request), or returns JSON 
     * results (for ajax request). JSON format: {success:true/false, message:string}
     */
    def reloadNodes = {
        def result=performNodeReload()
        def didsucceed=result.success
        withFormat {
            json{
                def data=[success:didsucceed,message:didsucceed?"Remote resources loaded for project: ${params.project}":"Failed to load remote resources for project: ${params.project}"]
                render data as JSON
            }
            html{
                if(!didsucceed){
                    flash.error=result.message?:'Failed reloading nodes: unknown reason'
                }
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
        if(!params.project){
            return [success: false, message: "project parameter is required", invalid: true]
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!frameworkService.authorizeProjectResource(authContext,[type:'resource',kind:'node'], AuthConstants.ACTION_REFRESH,params.project)){
            def msg = "user: ${session.user} UNAUTHORIZED for performNodeReload"
            log.error(msg)
            return [success:false,message:msg,unauthorized:true]
        }
        def project=framework.getFrameworkProjectMgr().getFrameworkProject(params.project)
       //if reload parameter is specified, and user is admin, reload from source URL
        try {
            if(url){
                if(!(url==~ /(?i)^(https?|file):\/\/.*$/)){
                    log.error("Error updating node resources file for project ${project.name}: invalid URL: " + url)
                    return [success: false, message: "Error updating node resources file for project ${project.name}: invalid URL: " + url, invalid: true]
                }
                project.updateNodesResourceFileFromUrl(url, null, null)
                return [success:true]
            }else{
                return [success:project.updateNodesResourceFile(),url:url,
                        message:g.message(code:'api.project.updateResources.noproviderUrl.failed',args: [params.project])]
            }
        } catch (Exception e) {
            log.error("Error updating node resources file for project ${project.name}: "+e.message)
            return [success: false, message: "Error updating node resources file for project ${project.name}: " + e.message, error: true]
        }
    }

    def storeNodeFilter={ExtNodeFilters query->
        def User u = userService.findOrCreateUser(session.user)
        def NodeFilter filter
        def boolean saveuser=false
        if(params.newFilterName){
            def ofilter = NodeFilter.findByNameAndUser(params.newFilterName,u)
            if(ofilter){
                ofilter.properties = query.properties
                filter=ofilter
            }else{
                filter= new NodeFilter(query.properties)
                filter.name=params.newFilterName
                u.addToNodefilters(filter)
                saveuser=true
            }
        }else if(!params.newFilterName){
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
                return renderErrorView(u.errors.allErrors.collect { g.message(error: it) }.join("\n"))
            }
        }
        redirect(controller:'framework',action:params.fragment?'nodesFragment':'nodes',params:[filterName:filter.name,project:params.project])
    }
    def deleteNodeFilter={
        def User u = userService.findOrCreateUser(session.user)
        def filtername=params.delFilterName
        final def ffilter = NodeFilter.findByNameAndUser(filtername, u)
        if(ffilter){
            ffilter.delete(flush:true)
        }
        redirect(controller:'framework',action:params.fragment?'nodesFragment':'nodes',params:[project: params.project])
    }

    /**
     * Handles POST when creating a new project
     * @return
     */

    def createProjectPost() {
        metricService.markMeter(this.class.name,actionName)
        //only attempt project create if form POST is used
        def prefixKey = 'plugin'
        def project = params.newproject
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceTypeAll(authContext, 'project', [AuthConstants
                        .ACTION_CREATE]),
                AuthConstants.ACTION_CREATE, 'New Project')) {
            return
        }
        def nodeexec, nodeexecreport
        def fcopy, fcopyreport
        def resourcesUrl
        def projectNameError
        def Properties projProps = new Properties()
        def errors = []
        def configs
        final defaultNodeExec = NodeExecutorService.DEFAULT_REMOTE_PROVIDER
        final defaultFileCopy = FileCopierService.DEFAULT_REMOTE_PROVIDER

        if (params.defaultNodeExec) {
            def ndx = params.defaultNodeExec
            (defaultNodeExec, nodeexec) = parseServiceConfigInput(params, "nodeexec", ndx)
            final validation = frameworkService.validateServiceConfig(defaultNodeExec, "nodeexec.${ndx}.config.", params, framework.getNodeExecutorService())
            if (!validation.valid) {
                nodeexecreport = validation.report
                errors << (validation.error ?: "Default Node Executor configuration was invalid")
            } else {
                try {
                    addProjectServiceProperties(params, projProps, ndx, "nodeexec", NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY, framework.getNodeExecutorService())
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                    errors << e.getMessage()
                }
            }
        }
        if (params.defaultFileCopy) {
            def ndx = params.defaultFileCopy
            (defaultFileCopy, fcopy) = parseServiceConfigInput(params, "fcopy", ndx)
            final validation = frameworkService.validateServiceConfig(defaultFileCopy, "fcopy.${ndx}.config.", params, framework.getFileCopierService())
            if (!validation.valid) {
                fcopyreport = validation.report
                errors << (validation.error ?: "Default File copier configuration was invalid")
            } else {
                try {
                    addProjectServiceProperties(params, projProps, ndx, "fcopy", FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY, framework.getFileCopierService())
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                    errors << e.getMessage()
                }
            }
        }

        //parse plugin config properties, and convert to project.properties
        def sourceConfigPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX
        def ndxes = [params.list('index')].flatten()

        def count = 1
        configs = []
        ndxes.each { ndx ->
            def String type = params[prefixKey + '.' + ndx + '.type']
            if (!type) {
                log.warn("missing type def for prefix: " + prefixKey + '.' + ndx);
                return
            }
            final service = framework.getResourceModelSourceService()
            final provider = service.providerOfType(type)
            if (!(provider instanceof Describable)) {
                errors << "Invalid provider type: ${params.type}, not available for configuration"
            } else {
                projProps[sourceConfigPrefix + '.' + count + '.type'] = type
                def mapprops = frameworkService.parseResourceModelConfigInput(provider.description, prefixKey + '.' + ndx + '.' + 'config.', params)
                def props = new Properties()
                props.putAll(mapprops)
                props.keySet().each { k ->
                    if (props[k]) {
                        projProps[sourceConfigPrefix + '.' + count + '.config.' + k] = props[k]
                    }
                }
                count++
                configs << [type: type, props: props]
            }
        }
        if (!project) {
            projectNameError = "Project name is required"
            errors << projectNameError
        } else if (!(project =~ FrameworkResource.VALID_RESOURCE_NAME_REGEX)) {
            projectNameError = message(code: "project.name.can.only.contain.these.characters")
            errors << projectNameError
        } else if (framework.getFrameworkProjectMgr().existsFrameworkProject(project)) {
            projectNameError = "Project already exists: ${project}"
            log.error(projectNameError)
            errors << projectNameError
        } else if (!errors) {
            log.debug("create project, properties: ${projProps}");
            def proj
            (proj, errors)=frameworkService.createFrameworkProject(project,projProps)
            if (!errors && proj) {
                session.frameworkProjects = frameworkService.projects(authContext)
                def result = userService.storeFilterPref(session.user, [project: proj.name])
                return redirect(controller: 'menu', action: 'index',params: [project:proj.name])
            }
        }
        if (errors) {
//            request.error=errors.join("\n")
//            request.errors = errors
        }
        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()
        return render(view:'createProject',
                model:         [
                newproject: params.newproject,
                projectNameError: projectNameError,
                resourcesUrl: resourcesUrl,
                resourceModelConfigDescriptions: descriptions,
                defaultNodeExec: defaultNodeExec,
                defaultFileCopy: defaultFileCopy,
                nodeExecDescriptions: nodeexecdescriptions,
                fileCopyDescriptions: filecopydescs,

                nodeexecconfig: nodeexec,
                fcopyconfig: fcopy,
                nodeexecreport: nodeexecreport,
                fcopyreport: fcopyreport,

                prefixKey: prefixKey, configs: configs])
    }

    /**
     * Shows form to create a new project
     * @return
     */
    def createProject(){
        def prefixKey= 'plugin'
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceTypeAll(authContext, 'project', [AuthConstants
                        .ACTION_CREATE]),
                AuthConstants.ACTION_CREATE, 'New Project')) {
            return
        }
        final defaultNodeExec = NodeExecutorService.DEFAULT_REMOTE_PROVIDER
        final defaultFileCopy = FileCopierService.DEFAULT_REMOTE_PROVIDER
        final sshkeypath = new File(System.getProperty("user.home"), ".ssh/id_rsa").getAbsolutePath()
        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()
        def defaultResourceFile= new File(framework.getFrameworkProjectsBaseDir(),'${project.name}/etc/resources.xml').getAbsolutePath()
        def configs=[
            [
                type: FileResourceModelSourceFactory.SERVICE_PROVIDER_TYPE,
                props:[
                        (FileResourceModelSource.Configuration.FILE): defaultResourceFile,
                        (FileResourceModelSource.Configuration.FORMAT): 'resourcexml',
                        (FileResourceModelSource.Configuration.GENERATE_FILE_AUTOMATICALLY): 'true',
                        (FileResourceModelSource.Configuration.INCLUDE_SERVER_NODE): 'true',
                ]
            ]
        ]
        return [
                newproject:params.newproject,
            resourceModelConfigDescriptions: descriptions,
            defaultNodeExec:defaultNodeExec,
            nodeexecconfig: ['keypath': sshkeypath],
            fcopyconfig: ['keypath': sshkeypath],
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            prefixKey:prefixKey,
            configs: configs
        ]
    }

    private def addProjectServiceProperties(GrailsParameterMap params, Properties projProps, final def ndx, final String param, final String default_provider_prop, final ProviderService service, Set removePrefixes=null) {
        def type, config
        (type, config) = parseServiceConfigInput(params, param, ndx)
        projProps[default_provider_prop] = type

        final executor = service.providerOfType(type)
        final Description desc = executor.description
        addPropertiesForDescription(config, projProps, desc)
        if(null!=removePrefixes && desc.propertiesMapping) {
            removePrefixes.addAll(desc.propertiesMapping.values())
        }
    }

    private List parseServiceConfigInput(GrailsParameterMap params, String param, ndx) {
        final nparams = params."${param}"?."${ndx}"
        def type = nparams?.type
        def config = nparams?.config
        config = config?.subMap(config?.keySet().findAll{config[it]})
        return [type, config]
    }


    private def addPropertiesForDescription(Map config, Properties projProps, desc) {
        if (config && config instanceof Map) {
            projProps.putAll(Validator.mapProperties(config, desc))
        }
    }

    def saveProject={
        def prefixKey= 'plugin'
        def project=params.project?:params.newproject
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def error=null
        def configs = []
        if (!project) {
            return renderErrorView("Project parameter is required")
        }
        if (params.cancel == 'Cancel') {
            //cancel modification
            return redirect(controller: 'menu', action: 'admin')
        }
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project', name: project], [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project',project)) {
            return
        }
        def fproject = frameworkService.getFrameworkProject(project)

        def defaultNodeExec
        def defaultFileCopy
        def nodeexec,fcopy
        def nodeexecreport,fcopyreport
        if(request.method=='POST'){
            //only attempt project create if form POST is used
            def Properties projProps = new Properties()
            def Set<String> removePrefixes=[]
            removePrefixes<< FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY

            if (params.defaultNodeExec) {
                def ndx=params.defaultNodeExec
                (defaultNodeExec, nodeexec)=parseServiceConfigInput(params,"nodeexec",ndx)
                final validation = frameworkService.validateServiceConfig(defaultNodeExec, "nodeexec.${ndx}.config.", params, framework.getNodeExecutorService())
                if(!validation.valid){
                    nodeexecreport=validation.report
                    error = validation.error ?: "Node Executor configuration was invalid"
                }else{
                    try {
                        addProjectServiceProperties(params, projProps, ndx, "nodeexec", NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY, framework.getNodeExecutorService(), removePrefixes)
                    } catch (ExecutionServiceException e) {
                        log.error(e.message)
                        error = e.getMessage()
                    }
                }
            }
            if (params.defaultFileCopy) {
                def ndx=params.defaultFileCopy
                (defaultFileCopy, fcopy) = parseServiceConfigInput(params, "fcopy", ndx)
                final validation = frameworkService.validateServiceConfig(defaultFileCopy, "fcopy.${ndx}.config.", params, framework.getFileCopierService())
                if(!validation.valid){
                    fcopyreport = validation.report
                    error=validation.error?:"File copier configuration was invalid"
                }else{
                    try {
                        addProjectServiceProperties(params, projProps, ndx, "fcopy", FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY,framework.getFileCopierService(), removePrefixes)
                    } catch (ExecutionServiceException e) {
                        log.error(e.message)
                        error = e.getMessage()
                    }
                }
            }
            //removePrefixes << JschNodeExecutor.PROJ_PROP_SSH_KEYPATH

            //parse plugin config properties, and convert to project.properties
            def sourceConfigPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX
            def ndxes = [params.list('index')].flatten()

            def count = 1
            ndxes.each {ndx ->
                def type = params[prefixKey + '.' + ndx + '.type']
                if(!type) {
                    log.warn("missing type def for prefix: " + prefixKey + '.' + ndx);
                    return
                }
                final service = framework.getResourceModelSourceService()
                final provider
                try {
                    provider= service.providerOfType(type)
                } catch (com.dtolabs.rundeck.core.execution.service.ExecutionServiceException e) {
                }
                def description
                if (provider && provider instanceof Describable) {
                    description=provider.description
                }
                projProps[sourceConfigPrefix + '.' + count + '.type'] = type
                def mapprops = frameworkService.parseResourceModelConfigInput(description, prefixKey + '.' + ndx + '.' + 'config.', params)
                def props = new Properties()
                props.putAll(mapprops)
                props.keySet().each {k ->
                    if(props[k]){
                        projProps[sourceConfigPrefix + '.' + count + '.config.' + k] = props[k]
                    }
                }
                count++
                configs << [type: type, props: props]
            }
            removePrefixes<< FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX

            if(!error){
                def result = frameworkService.updateFrameworkProjectConfig(project,projProps,removePrefixes)
                if(!result.success){
                    error=result.error
                }
            }
            if(!error){
                def result=userService.storeFilterPref(session.user, [project: fproject.name])
                flash.message="Project ${project} saved"
                return redirect(controller:'menu',action:'admin',params:[project:fproject.name])
            }
        }
        if(error){
            request.error=error
        }
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()

        return render(view:'editProject',model:
        [
            project: params.project,
            newproject: params.newproject,
            defaultNodeExec: defaultNodeExec,
            nodeexecconfig: nodeexec,
            fcopyconfig: fcopy,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            resourceModelConfigDescriptions: descriptions,
            nodeexecreport: nodeexecreport,
            fcopyreport: fcopyreport,
            prefixKey: prefixKey,
            configs: configs])
    }

    def editProject = {
        def prefixKey = 'plugin'
        def project = params.project
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!project){
            return renderErrorView("Project parameter is required")
        }

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project', name: project], [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project', project)) {
            return
        }
        def fproject = frameworkService.getFrameworkProject(project)
        def configs = fproject.listResourceModelConfigurations()
        final service = framework.getResourceModelSourceService()
        final descriptions = service.listDescriptions()

        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()

        final defaultNodeExec = fproject.hasProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null
        final defaultFileCopy = fproject.hasProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null

        //load config for node exec
        def nodeexec=[:]
        if(defaultNodeExec){
            try {
                final executor = framework.getNodeExecutorService().providerOfType(defaultNodeExec)
                final desc = executor.description
                nodeexec = Validator.demapProperties(fproject.getProperties(),desc)
            } catch (ExecutionServiceException e) {
                log.error(e.message)
            }
        }
        //load config for file copy
        def fcopy=[:]
        if(defaultFileCopy){
            try {
                final executor = framework.getFileCopierService().providerOfType(defaultFileCopy)
                final desc = executor.description
                fcopy = Validator.demapProperties(fproject.getProperties(),desc)
            } catch (ExecutionServiceException e) {
                log.error(e.message)
            }
        }

        [
            project: params.project, resourceModelConfigDescriptions: descriptions,
            defaultNodeExec: defaultNodeExec,
            nodeexecconfig:nodeexec,
            fcopyconfig:fcopy,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            prefixKey: prefixKey, configs: configs]
    }
    def createResourceModelConfig={
        Framework framework = frameworkService.getRundeckFramework()
        def error
        if(!params.type){
            error = "Plugin provider type must be specified"
        }
        final service = framework.getResourceModelSourceService()
        final provider = service.providerOfType(params.type)
        if(provider instanceof Describable){
            def desc = provider.description
            return [description:desc,prefix:params.prefix,type:params.type,isCreate:true]
        }else{
            error="Invalid provider type: ${params.type}, not available for configuration"
        }

        flash.error=error
    }
    def saveResourceModelConfig = {
        def project = params.project
        Framework framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type=params[prefix+'type']
        Properties props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        }else{
            def validate = frameworkService.validateServiceConfig(type, prefix + 'config.', params, framework.getResourceModelSourceService())
            error = validate.error
            desc = validate.desc
            props = validate.props
            report = validate.report
            if(report.valid){
                return render(template: 'viewResourceModelConfig',model:[project:project,prefix:prefix,includeFormFields:true,values:props,description:desc])
            }
        }
        render(view:'createResourceModelConfig',model:[project:project,prefix:prefix,values:props,description:desc,report:report,error:error])
    }
    def checkResourceModelConfig = {
        def project = params.project
        Framework framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type=params[prefix+'type']
        if('true'==params.revert){
            prefix='orig.'+prefix
        }
        Properties props
        def report
        def desc
        def result=[valid:false]
        if (!type) {
            result.error = "Plugin provider type must be specified"
        }else{
            def validate = frameworkService.validateServiceConfig(type, prefix + 'config.', params, framework.getResourceModelSourceService())
            result.valid=validate.valid
            result.error=validate.error
        }
        render result as JSON
    }


    def editResourceModelConfig = {
        Framework framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type = params[prefix + 'type']
        Properties props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        } else {
            def validate = frameworkService.validateServiceConfig(type, prefix + 'config.', params, framework.getResourceModelSourceService())
            error = validate.error
            desc=validate.desc
            props=validate.props
            report=validate.report
        }

        render(view: 'createResourceModelConfig', model: [ prefix: prefix, values: props, description: desc, report: report, error: error, isEdit: "true"!=params.iscreate,type:type, isCreate:params.isCreate])
    }
    def viewResourceModelConfig = {
        Framework framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type = params[prefix + 'type']
        def useprefix=prefix
        if ('true' == params.revert) {
            useprefix = 'orig.' + prefix
        }
        Properties props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        } else {
            def validate = frameworkService.validateServiceConfig(type, prefix + 'config.', params, framework.getResourceModelSourceService())
            error = validate.error
            desc = validate.desc
            props = validate.props
            report = validate.report
        }

        return render(template: 'viewResourceModelConfig',model:[ prefix: prefix, values: props, includeFormFields: true, description: desc, report: report, error: error, saved:true, type: type])
    }
    def projectDescFragment(){
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def project = params.project
        if (!project) {
            return renderErrorFragment("Project parameter is required")
        }
        if(notFoundResponse(frameworkService.existsFrameworkProject(project),'Project',project,true)){
            return
        }
        if(unauthorizedResponse(frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project',
                name: project], [AuthConstants.ACTION_READ]), AuthConstants.ACTION_READ,'Project',project,true)){
            return
        }
        //look for readme.md in project directory
        def project1 = frameworkService.getFrameworkProject(project)
        def readme = new File(project1.baseDir, "readme.md")
        def motd = new File(project1.baseDir, "motd.md")
        def html=''//empty
        if (motd.exists() && motd.isFile()) {
            //load file and apply markdown
            html = motd.text?.decodeMarkdown()
            html+= '<hr>\n'
        }
        if(readme.exists() && readme.isFile()){
            //load file and apply markdown
            html += readme.text?.decodeMarkdown()
        }
        return render(contentType: 'text/html',text: html)
    }
    /**
     * JSON output
     * @return
     */
    def apiProjectSummary(){
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def project=params.project
        if (!project) {
            return renderErrorFragment("Project parameter is required")
        }
        if (notFoundResponse(frameworkService.existsFrameworkProject(project), 'Project', project, true)) {
            return
        }
        if (unauthorizedResponse(frameworkService.authorizeApplicationResourceAll(authContext, [type: 'project',
                name: project], [AuthConstants.ACTION_READ]), AuthConstants.ACTION_READ, 'Project', project, true)) {
            return
        }

        Calendar n = GregorianCalendar.getInstance()
        n.add(Calendar.DAY_OF_YEAR, -1)
        Date today = n.getTime()
        def c = Execution.createCriteria()

        def users = c.list {
            eq('project',project)
            gt('dateStarted',today)
            projections {
                distinct('user')
            }
        }
        //authorization
        def auth=[
                jobCreate: frameworkService.authorizeProjectResource(authContext,[type:'resource',kind:'job'], 'create',project)
        ]
        def project1 = frameworkService.getFrameworkProject(project)
        //summary data
        def data= [
            project:project,
            jobCount: ScheduledExecution.countByProject(project),
            execCount: Execution.countByProjectAndDateStartedGreaterThan(project, today),
            userCount: users.size(),
            nodeCount:project1.nodeSet.nodeNames.size(),
            users: users,
            auth:auth
        ]
        withFormat {
            json{
                render data as JSON
            }
        }
    }

    /**
     * Render selection list for projects
     */
    def projectSelect={
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def projects
        if(session.frameworkProjects){
            projects=session.frameworkProjects
        }else{
            projects = frameworkService.projects(authContext)
            session.frameworkProjects=projects
        }
        [projects:projects,project:params.project]
    }
    /**
     * Select project via parameter, and redirect to default page for the project
     */
    def selectProject= {
        //also set User project filter pref
        return redirect(controller: 'menu',action: 'index',params: [page:params.page,project:params.project])
    }

    /*******
     * API actions
     */

    /**
     * API: /api/2/project/NAME/resources/refresh
     * calls performNodeReload, then returns API response
     * */
    def apiProjectResourcesRefresh = {
        if (!apiService.requireVersion(request,response,ApiRequestFilters.V2)) {
            return
        }
        Framework framework = frameworkService.getRundeckFramework()
        if (!params.project) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required',
                    args: ['project']])
        }
        def exists = frameworkService.existsFrameworkProject(params.project)
        if (!exists) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args: ['project', params.project]])
        }

        //check content
        def result = performNodeReload(params.providerURL)
        def didsucceed=result.success
        if(didsucceed){
            return apiService.renderSuccessXml(response, 'api.project.updateResources.succeeded', [params.project])
        }else{
            def error=[:]
            if(result.invalid){
                error.code='api.error.invalid.request'
                error.args=[result.message]
            }else if(result.unauthorized){
                error.code='api.error.item.unauthorized'
                error.args=['Refresh Resources','Project: '+params.project,result.message]
            }
            if(!error.code && !result.url){
                error.code= 'api.project.updateResources.failed'
                error.args=[params.project]
                error.message=result.message
            }else if(!error.code){
                error.code = 'api.project.updateResources.failed'
                error.args = [error.message?:'Unknown reason']
            }

            return apiService.renderErrorXml(response, error)
        }
    }
    /**
     * API: /api/2/project/NAME/resources
     * POST: update resources data with either: text/xml content, text/yaml content, form-data param providerURL=<url>
     *     GET: see {@link #apiResourcesv2}
     * */
    def apiProjectResourcesPost = {
        if (!apiService.requireVersion(request, response,ApiRequestFilters.V2)) {
            return
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!params.project) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])

        }
        def exists = frameworkService.existsFrameworkProject(params.project)
        if (!exists) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])
        }
        if (!frameworkService.authorizeProjectResourceAll(authContext, [type: 'resource', kind: 'node'], ['create','update'], params.project)) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Update Nodes', 'Project', params.project]])
        }
        final FrameworkProject project = frameworkService.getFrameworkProject(params.project)

        def didsucceed=false
        def errormsg=null
        //determine data
        //assume post request
        if(!request.post){
            //bad method
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    code: 'api.error.invalid.request', args: ['Method not allowed']])
        }
        final contentType = request.contentType
        //try to parse loaded data
        if(!(contentType?.endsWith("/xml")||contentType?.endsWith('/yaml')|| contentType?.endsWith('/x-yaml'))){
            if (!apiService.requireVersion(request, response,ApiRequestFilters.V3)) {
                //require api V3 for any other content type
                return
            }
        }

        final parser
        try {
            parser = framework.getResourceFormatParserService().getParserForMIMEType(contentType)
        } catch (UnsupportedFormatException e) {
            //invalid data
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.resources-import.unsupported-format', args: [contentType]])
        }

        //write content to temp file
        File tempfile=File.createTempFile("post-input","data")
        tempfile.deleteOnExit()
        final stream = new FileOutputStream(tempfile)
        try {
            com.dtolabs.utils.Streams.copyStream(request.getInputStream(), stream)
        } finally {
            stream.close()
        }

        def INodeSet nodeset
        try {
            nodeset=parser.parseDocument(tempfile)
        }catch (ResourceFormatParserException e){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: [e.message]])
        }catch (Exception e){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    code: 'api.project.updateResources.failed', args: [e.message]])
        }
        tempfile.delete()

        //finally update resources file with the new nodes data
        try {
            project.updateNodesResourceFile nodeset
            didsucceed=true
        } catch (Exception e) {
            log.error("Failed updating nodes file: "+e.getMessage())
            e.printStackTrace(System.err)
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    code: 'api.project.updateResources.failed', args: [e.message]])
        }
        return apiService.renderSuccessXml(response, 'api.project.updateResources.succeeded', [params.project])
    }

    /**
     * Convert input node filter parameters into specific property names used by
     * domain objects
     */
    public static Map extractApiNodeFilterParams(Map params){
        def result=[:]
        def value=false
        //convert api parameters to node filter parameters
        BaseNodeFilters.filterKeys.each{k,v->
            if(params[k]){
                result["nodeInclude${v}"]=params[k]
                value=true
            }
            if(params["exclude-"+k]){
                result["nodeExclude${v}"]=params["exclude-"+k]
                value = true
            }
        }
        if(params.filter){
            result.filter=params.filter
            value=true
        }
        if (value && null!=params.'exclude-precedence') {
            result.nodeExcludePrecedence = params['exclude-precedence'] == 'true'
        }
        return result
    }


    /**
     * API: /api/resource/$name, version 1
     */
    def apiResource={
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        if(!params.name){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['name']])
        }
        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project',params.project]])
        }
        if (!frameworkService.authorizeProjectResourceAll(authContext, [type: 'resource', kind: 'node'], ['read'], params.project)) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project]])
        }

        NodeSet nset = new NodeSet()
        nset.setSingleNodeName(params.name)
        def pject=frameworkService.getFrameworkProject(params.project)
        final INodeSet nodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nset,pject.getNodeSet())
        if(!nodes || nodes.nodes.size()<1 ){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['Node Name', params.name]])

        }
        return apiRenderNodeResult(nodes, framework, params.project)
    }
    /**
     * API: /api/2/project/NAME/resources, version 2
     */
    def apiResourcesv2={ExtNodeFilters query->
        if (!apiService.requireVersion(request, response,ApiRequestFilters.V2)) {
            return
        }
        return apiResources(query)
    }
    /**
     * API: /api/1/resources, version 1
     */
    def apiResources={ExtNodeFilters query->
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])

        }
        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project',params.project]])


        }
        if (!frameworkService.authorizeProjectResourceAll(authContext, [type: 'resource', kind: 'node'], ['read'], params.project)) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project]])

        }
        if (params.format && !(params.format in ['xml','yaml']) || request.format && !(request.format in ['html','xml','yaml'])) {
            //expected another content type
            def reqformat = params.format ?: request.format
            if (!apiService.requireVersion(request, response,ApiRequestFilters.V3)) {
                return
            }
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
            query.filter = 'name: .*'
        }
        def pject=frameworkService.getFrameworkProject(params.project)
//        final Collection nodes = pject.getNodes().filterNodes(ExecutionService.filtersAsNodeSet(query))
        final INodeSet nodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(ExecutionService.filtersAsNodeSet(query), pject.getNodeSet())
        return apiRenderNodeResult(nodes,framework,params.project)
    }
    protected String apiRenderNodeResult(INodeSet nodes, Framework framework, String project){
        if (params.format && !(params.format in ['xml', 'yaml']) || request.format && !(request.format in ['all','html', 'xml', 'yaml'])) {
            //expected another content type
            if (!apiService.requireVersion(request, response,ApiRequestFilters.V3)) {
                return
            }
            def reqformat=params.format?:request.format
            //render specified format
            final FrameworkProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(project)
            final service = framework.getResourceFormatGeneratorService()
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            final generator
            try {
                generator = service.getGeneratorForFormat(reqformat?:'resourcexml')
                generator.generateDocument(nodes,baos)
            } catch (UnsupportedFormatException e) {
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        code: 'api.error.resource.format.unsupported', args: [reqformat]])
            }catch (ResourceFormatGeneratorException e){
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.resource.format.generator', args: [e.message]])
            }catch (IOException e){
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.resource.format.generator', args: [e.message]])
            }
            final types = generator.getMIMETypes() as List
            return render(contentType: types[0],encoding:"UTF-8",text:baos.toString())
        }
        withFormat{
            xml{
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final NodesFileGenerator generator = new ResourceXMLGenerator(baos)
                nodes.nodes.each {INodeEntry node->
                    generator.addNode(node)
                }
                generator.generate()
                return render(contentType:"text/xml",encoding:"UTF-8",text:baos.toString())
            }
            yaml{
                if(nodes.nodes.size()>0){
                    StringWriter sw = new StringWriter()
                    final NodesFileGenerator generator = new NodesYamlGenerator(sw)
                    nodes.nodes.each {INodeEntry node->
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

