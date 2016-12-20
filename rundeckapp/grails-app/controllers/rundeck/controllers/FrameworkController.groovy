package rundeck.controllers

import com.dtolabs.rundeck.app.support.PluginConfigParams
import com.dtolabs.rundeck.app.support.StoreFilterCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProviderService
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.resources.FileResourceModelSource
import com.dtolabs.rundeck.core.resources.FileResourceModelSourceFactory
import com.dtolabs.rundeck.core.resources.format.json.ResourceJsonFormatGenerator
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.shared.resources.ResourceXMLGenerator

import grails.converters.JSON
import grails.converters.XML
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ApiService
import rundeck.services.AuthorizationService
import rundeck.services.PasswordFieldsService
import rundeck.services.framework.RundeckProjectConfigurable

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern
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
import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.server.authorization.AuthConstants
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

class FrameworkController extends ControllerBase implements ApplicationContextAware {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService

    PasswordFieldsService resourcesPasswordFieldsService
    PasswordFieldsService execPasswordFieldsService
    PasswordFieldsService fcopyPasswordFieldsService

    def metricService
    def ApiService apiService
    def configStorageService
    def AuthorizationService authorizationService
    def ApplicationContext applicationContext
    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
            apiProjectResources: ['POST'],
            apiProjectResourcesPost: ['POST'],
            apiProjectResourcesRefresh: ['POST'],
            apiSystemAcls: ['GET','PUT','POST','DELETE'],
            createProjectPost: 'POST',
            deleteNodeFilter: 'POST',
            saveProject: 'POST',
            storeNodeFilter: 'POST',
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

    def nodes(ExtNodeFilters query) {
        if (query.hasErrors()) {
            request.errors = query.errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView([:])
        }
        if(params.fromExecId|| params.retryFailedExecId) {
            return redirect(action: 'adhoc',params: params)
        }else if(params.exec){
            return redirect(action: 'adhoc', params: params)
        }
        if(params.filterLocalNodeOnly=='true') {
            //redirect to local node filter
            return redirect(
                    action: 'nodes',
                    params: [project: params.project, filter: frameworkService.frameworkNodeName]
            )
        }
        def User u = userService.findOrCreateUser(session.user)
        def usedFilter = null
        def prefs=userService.getFilterPref(u.login)
        if(params.filterName=='.*'){
            query.filter='.*'
            usedFilter='.*'
        }else if (params.filterName) {
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
        } else if (prefs['nodes']) {
            return redirect(action: 'nodes', params: params + [filterName: prefs['nodes']])
        }

        if(params.filterName && !usedFilter){
            request.warn='Filter not found: '+params.filterName
            params.remove('filterName')
        }

        def summaryOnly = false
        if (params['Clear']) {
            query = new ExtNodeFilters()
            usedFilter = null
        }
        if(params.showall=='true'){
            query.filter = '.*'
        }
        //in case named filter stored from another project
        query.project = params.project
        def sortkeys = query.filter?filterSummaryKeys(query):[]
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

    def adhoc(ExtNodeFilters query) {
        if (query.hasErrors()) {
            request.errors = query.errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView([:])
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                        AuthConstants.ACTION_RUN, params.project),
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

        query.project = params.project
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
        if (query.hasErrors()) {
            request.errors = query.errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView([:])
        }

        Framework framework = frameworkService.getRundeckFramework()

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
        AuthContext authContext=frameworkService.getAuthContextForSubjectAndProject(session.subject,query.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                        [AuthConstants.ACTION_READ], query.project),
                AuthConstants.ACTION_READ, 'Project', 'nodes')) {
            return
        }
        def allnodes = [:]
        def totalexecs = [:]
        def total=0
        def truncateMax=params.untruncate?-1: params.maxShown?params.int('maxShown'):100
        def allcount=null
        NodeSet nset = ExecutionService.filtersAsNodeSet(query)
        def projects=[]
        def filterErrors=[:]
        def project = framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        def INodeSet nodeset

        long mark=System.currentTimeMillis()
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

        mark=System.currentTimeMillis()
        def readnodes = frameworkService.filterAuthorizedNodes(query.project, ['read'] as Set, nodeset, authContext)
        log.debug("nodesData filterAuthorizedNodes[read]: ${System.currentTimeMillis()-mark}ms")
        mark=System.currentTimeMillis()
        def runnodes = frameworkService.filterAuthorizedNodes(query.project, ['run'] as Set, readnodes, authContext)
        log.debug("nodesData filterAuthorizedNodes[run]: ${System.currentTimeMillis()-mark}ms")
        def noderunauthmap = [:]


        def nodes=params.requireRunAuth=='true'? runnodes.nodes:readnodes.nodes
        total= nodes.size()
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

        def tagsummary=frameworkService.summarizeTags(nodes)
        def count=0;
        int first=page<0? 0 : page*max
        int last=page<0 || remaining ? (nodes.size()) : ((page+1)*max)
        def truncated=false
        if(truncateMax>0 && last-first > truncateMax){
            truncated=true
            last=first+truncateMax
        }
        def nlist=new ArrayList<INodeEntry>(nodes)
        for(int i=first;i < last && i<nodes.size();i++){
            INodeEntry nd  = nlist[i]
            allnodes[nd.nodename]=[node:nd,project:project.name,islocal:nd.nodename==framework.getFrameworkNodeName()]
            if(params.requireRunAuth == 'true'  || runnodes.getNode(nd.nodename)){
                noderunauthmap[nd.nodename]=true
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

        def parseExceptions= project.projectNodes.getResourceModelSourceExceptions()

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
            allcount:total,
            tagsummary:tagsummary,
            page:page,
            max:max,
            truncated:truncated,
//            totalexecs:totalexecs,
//            jobs:runningset.jobs,
            query:query
        ]

        if(query.project && framework.getFrameworkProjectMgr().existsFrameworkProject(query.project)){
            model.selectedProject=framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        }


        return model
    }
    def nodeSummaryAjax(String project){

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                                                             [AuthConstants.ACTION_READ],
                                                             project),
                AuthConstants.ACTION_READ, 'Project', 'nodes',true)) {
            return
        }
        def User u = userService.findOrCreateUser(session.user)
        def defaultFilter = null
        Map filterpref = userService.parseKeyValuePref(u.filterPref)
        if (filterpref['nodes']) {
            defaultFilter = filterpref['nodes']
        }
        def filters=[]
        //load a named filter and create a query from it
        if (u) {
            def filterResults = NodeFilter.findAllByUserAndProject(u, project, [sort: 'name', order: 'desc'])
            filters = filterResults.collect {
                [name: it.name, filter: it.asFilter(), project: project]
            }
        }

        def fwkproject = frameworkService.getFrameworkProject(project)
        INodeSet nodes1 = fwkproject.getNodeSet()
        def size=nodes1.nodes.size()
        def tagsummary = frameworkService.summarizeTags(nodes1.nodes)
        tagsummary = tagsummary.keySet().sort().collect{
            [tag:it,count:tagsummary[it]]
        }
        render(contentType: 'application/json',text: [tags:tagsummary,totalCount:size,filters:filters,defaultFilter:defaultFilter] as JSON)
    }
    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesFragmentData(ExtNodeFilters query) {
        long start = System.currentTimeMillis()
        if (query.hasErrors()) {
            return null
        }

        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                                                             [AuthConstants.ACTION_READ],
                                                             query.project
                ),
                AuthConstants.ACTION_READ, 'Project', 'nodes', true
        )) {
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
        //in case named filter stored from another project
        query.project = params.project
        def result = nodesdata(query)
        result.colkeys = filterSummaryKeys(query)
        if (usedFilter) {
            result['filterName'] = usedFilter
        }
        if (!result.nodesvalid) {
            request.error = "Error parsing file \"${result.nodesfile}\": " + result.nodeserror ? result.nodeserror*.message?.
                    join("\n") : 'no message'
        }
        result['nodefilterLinkId'] = params.nodefilterLinkId
        return result
    }

    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesFragment(ExtNodeFilters query) {
        if (query.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorFragment(
                    g.message(error: query.errors.allErrors.collect { g.message(error: it) }.join("; "))
            )
        }
        def result= nodesFragmentData(query)
        render(template:"allnodes",model: result)
    }
    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesQueryAjax(ExtNodeFilters query) {
        if (query.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorFragment(
                    g.message(error: query.errors.allErrors.collect { g.message(error: it) }.join("; "))
            )
        }
        Map result= nodesFragmentData(query)
        result.remove('selectedProject')
        result.remove('query')
        result.remove('params')
        def nodes=result.remove('allnodes')
        withFormat {
            json{
                return render ((result + [
                        allnodes: nodes.collect{entry->
                            [
                                    nodename:entry.key,
                                    islocal:entry.value.islocal,
                                    tags:entry.value.node.tags,
                                    attributes:entry.value.node.attributes,
                                    authrun:result.nodeauthrun[entry.key]?true:false
                            ]
                        }
                ]) as JSON)
            }
            xml{
                return render (result as XML)
            }
        }
    }

    /**
     * If user has admin rights and the project parameter is specified, attempt to re-fetch the resources.xml
     * via the project's project.resources.url (if it exists).
     * Returns true if re-fetch succeeded, false otherwise.
     */
    protected def performNodeReload (String url = null){
        if(!params.project){
            return [success: false, message: "project parameter is required", invalid: true]
        }
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        if(!frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                AuthConstants.ACTION_REFRESH,params.project)){
            def msg = "user: ${session.user} UNAUTHORIZED for performNodeReload"
            log.error(msg)
            return [success:false,message:msg,unauthorized:true]
        }
        def project=framework.getFrameworkProjectMgr().getFrameworkProject(params.project)
       //if reload parameter is specified, and user is admin, reload from source URL
        try {
            if(url){
                if(!(url==~ /(?i)^(https?|file):\/\/?.*$/)){
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

    def storeNodeFilter(ExtNodeFilters query, StoreFilterCommand storeFilterCommand) {
        if (query.hasErrors()) {
            request.errors = query.errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView([:])
        }
        if (storeFilterCommand.hasErrors()) {
            request.errors = storeFilterCommand.errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView([:])
        }
        withForm{
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
        }.invalidToken{
            response.status=HttpServletResponse.SC_BAD_REQUEST
            renderErrorView(g.message('request.error.invalidtoken.message'))
        }
    }
    def deleteNodeFilter={
        withForm{
            def User u = userService.findOrCreateUser(session.user)
            def filtername=params.delFilterName
            final def ffilter = NodeFilter.findByNameAndUser(filtername, u)
            if(ffilter){
                ffilter.delete(flush:true)
            }
            redirect(controller:'framework',action:params.fragment?'nodesFragment':'nodes',params:[project: params.project])
        }.invalidToken{
            request.error=g.message(code:'request.error.invalidtoken.message')
            renderErrorView([:])
        }
    }

    def deleteNodeFilterAjax(String project, String filtername){
        withForm{
            g.refreshFormTokensHeader()
            def User u = userService.findOrCreateUser(session.user)
            final def ffilter = NodeFilter.findByNameAndUserAndProject(filtername, u, project)
            if(ffilter){
                ffilter.delete(flush:true)
            }
            render(contentType: 'application/json'){
                success=true
            }
        }.invalidToken{
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'request.error.invalidtoken.message',
            ])
        }
    }

    /**
     * Handles POST when creating a new project
     * @return
     */

    def createProjectPost() {
        metricService.markMeter(this.class.name,actionName)
        boolean valid=false
        withForm{
            valid=true
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if(!valid){
            return
        }
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
        if(params.description) {
            projProps['project.description'] = params.description
        }
        def errors = []
        def configs
        final defaultNodeExec = NodeExecutorService.DEFAULT_REMOTE_PROVIDER
        final defaultFileCopy = FileCopierService.DEFAULT_REMOTE_PROVIDER

        if (params.defaultNodeExec) {
            def ndx = params.defaultNodeExec
            (defaultNodeExec, nodeexec) = parseServiceConfigInput(params, "nodeexec", ndx)
            if (!(defaultNodeExec =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)) {
                errors << "Default Node Executor provider name is invalid"
                defaultNodeExec=null
                nodeexec=null
            }else{
                final validation = frameworkService.validateServiceConfig(defaultNodeExec, "nodeexec.${ndx}.config.", params, framework.getNodeExecutorService())
                if (!validation.valid) {
                    nodeexecreport = validation.report
                    errors << (validation.error ? "Default Node Executor configuration was invalid: "+ validation.error : "Default Node Executor configuration was invalid")
                } else {
                    try {
                        def (type, config) = parseServiceConfigInput(params, "nodeexec", ndx)
                        frameworkService.addProjectNodeExecutorPropertiesForType(type, projProps, config)
                    } catch (ExecutionServiceException e) {
                        log.error(e.message)
                        errors << e.getMessage()
                    }
                }
            }
        }
        if (params.defaultFileCopy) {
            def ndx = params.defaultFileCopy
            (defaultFileCopy, fcopy) = parseServiceConfigInput(params, "fcopy", ndx)
            if (!(defaultFileCopy =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)) {
                errors << "Default File copier provider name is invalid"
                defaultFileCopy=null
                fcopy=null
            }else{
                final validation = frameworkService.validateServiceConfig(defaultFileCopy, "fcopy.${ndx}.config.", params, framework.getFileCopierService())
                if (!validation.valid) {
                    fcopyreport = validation.report
                    errors << (validation.error ? "Default File copier configuration was invalid: "+ validation.error : "Default File copier configuration was invalid")
                } else {
                    try {
                        def (type, config) = parseServiceConfigInput(params, "fcopy", ndx)
                        frameworkService.addProjectFileCopierPropertiesForType(type, projProps, config)
                    } catch (ExecutionServiceException e) {
                        log.error(e.message)
                        errors << e.getMessage()
                    }
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
            if(!(type =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)){
                errors << "Invalid Resource Model Source definition for source #${ndx}"
                return
            }
            final service = framework.getResourceModelSourceService()
            def provider=null
            try {
                provider = service.providerOfType(type)
            } catch (MissingProviderException e) {
            }
            if (null==provider || !(provider instanceof Describable)) {
                errors << "Resource Model Source provider was not found: ${type}"
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

        //load extra configuration for grails services

        Map<String,RundeckProjectConfigurable> projectConfigurableBeans=applicationContext.getBeansOfType(RundeckProjectConfigurable)

        Map<String,Map> extraConfig=[:]
        projectConfigurableBeans.each { k, RundeckProjectConfigurable v ->
            if(k.endsWith('Profiled')){
                //skip profiled versions of beans
                return
            }
            //construct input values for the bean
            def beanData=[
                    configurable:v,
                    prefix:"extraConfig.${k}."
            ]
            def input=params.extraConfig?."${k}"?:[:]
            beanData.values=input

            v.getProjectConfigProperties().findAll{it.type==Property.Type.Boolean}.each{
                if(input[it.name]!='true'){
                    input[it.name]='false'
                }
            }
            //validate
            def report=Validator.validate(input as Properties, v.projectConfigProperties)
            beanData.report=report
            if(!report.valid){
                errors << ("Some configuration was invalid: "+ report )
            }else{
                def projvalues = Validator.performMapping(input, v.getPropertiesMapping(),true)
                projProps.putAll(projvalues)
            }
            extraConfig[k]=beanData
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
                def result = userService.storeFilterPref(session.user, [project: proj.name])
                return redirect(controller: 'menu', action: 'index',params: [project:proj.name])
            }
        }
        if (errors) {
//            request.error=errors.join("\n")
            request.errors = errors
        }
        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()
        return render(view:'createProject',
                model: [
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
                prefixKey: prefixKey,
                configs: configs,
                extraConfig:extraConfig
                ])
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
        //get grails services that declare project configurations
        Map<String,RundeckProjectConfigurable> projectConfigurableBeans=applicationContext.getBeansOfType(RundeckProjectConfigurable)

        Map<String,Map> extraConfig=[:]
        projectConfigurableBeans.each { k, v ->
            if(k.endsWith('Profiled')){
                //skip profiled versions of beans
                return
            }
            //construct existing values from project properties
            extraConfig[k]=[
                    configurable:v,
                    values:[:],
                    prefix:"extraConfig.${k}."
            ]
        }

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
            configs: configs,
            extraConfig:extraConfig
        ]
    }

    private List parseServiceConfigInput(GrailsParameterMap params, String param, ndx) {
        final nParams = params."${param}"?."${ndx}"
        [nParams?.type, filterEntriesWithCoercedFalseValues(nParams?.config)]
    }

    private filterEntriesWithCoercedFalseValues(config) {
        config?.subMap(config?.keySet().findAll{config[it]})
    }

    def saveProjectConfig(){
        boolean valid=false
        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }
        def project=params.project
        if (!project) {
            return renderErrorView("Project parameter is required")
        }

        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'menu', action: 'admin', params: [project: project])
        }

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN]
                ),
                AuthConstants.ACTION_ADMIN, 'Project',project
        )) {
            return
        }

        def configText=params.projectConfig
        if (configText==null) {
            return renderErrorView("projectConfig parameter is required")
        }

        //parse input config
        def inputProps = new Properties()
        inputProps.load(new StringReader(configText))

        def framework = frameworkService.getRundeckFramework()

        def (resourceModelSourceDescriptions, nodeexecdescriptions, filecopydescs) = frameworkService.listDescriptions()
        def errors=[]
        def configs = []
        def nodeexecreport, fcopyreport
        if(request.method=='POST'){
            //only attempt project create if form POST is used
            def Properties projProps = new Properties()
            projProps.putAll(inputProps)
            def inputMap = new HashMap(inputProps)

            final nodeExecType = frameworkService.getDefaultNodeExecutorService(projProps)
            final nodeConfig = frameworkService.getNodeExecConfigurationForType(nodeExecType, projProps)

            //load node exec properties
            def nodeExecService=frameworkService.getNodeExecutorService()

            //restore tracked password values
            try {
                execPasswordFieldsService.untrack([[config: [type: nodeExecType, props: nodeConfig], index: 0]], * nodeexecdescriptions)
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                errors << e.getMessage()
            }

            //validate input values
            final nevalidation = frameworkService.validateServiceConfig(nodeExecType, "", nodeConfig, nodeExecService)
            if (!nevalidation.valid) {
                nodeexecreport = nevalidation.report
                errors << nevalidation.error ? "Node Executor configuration was invalid: " + nevalidation.error : "Node Executor configuration was invalid"
            }else{
                //store back in props
                frameworkService.addProjectNodeExecutorPropertiesForType(nodeExecType, projProps, nodeConfig)
            }

            //load file copy properties

            final fileCopyType = frameworkService.getDefaultFileCopyService(projProps)
            final filecopyConfig = frameworkService.getFileCopyConfigurationForType(fileCopyType, projProps)
            def fileCopierService=frameworkService.getFileCopierService()

            //restore tracked password values
            try {
                fcopyPasswordFieldsService.untrack([[config: [type: fileCopyType, props: filecopyConfig], index: 0]], * filecopydescs)
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                errors << e.getMessage()
            }

            //validate input values
            final fcvalidation = frameworkService.validateServiceConfig(fileCopyType, "", filecopyConfig, fileCopierService)
            if (!fcvalidation.valid) {
                nodeexecreport = fcvalidation.report
                errors << fcvalidation.error ? "File Copier configuration was invalid: " + fcvalidation.error : "File Copier configuration was invalid"
            }else{
                frameworkService.addProjectFileCopierPropertiesForType(fileCopyType, projProps, filecopyConfig)
            }


            //parse plugin config properties, and convert to project.properties
            def newndx=0
            def resourceConfig = frameworkService.
                    listResourceModelConfigurations(projProps).
                    collect{
                        [
                                config: it,
                                index:newndx++
                        ]
                    }

            //replace any unmodified password fields with the session data
            resourcesPasswordFieldsService.untrack(resourceConfig, *resourceModelSourceDescriptions)
            //for each resources model source definition, add project properties from the input config
            resourceConfig.each{ Map mapping->
                def props=mapping.config.props
                def resourceConfigPrefix=FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + '.' + (mapping.index+1) + '.config.'
                props.keySet().each { k ->
                    if (props[k]) {
                        projProps[resourceConfigPrefix + k] = props[k]
                    }
                }
            }

            if (!errors) {
                // Password Field Substitution

                def result = frameworkService.setFrameworkProjectConfig(project, projProps)
                if (!result.success) {
                    errors << result.error
                }
            }

            if (!errors) {
                flash.message = "Project ${project} configuration file saved"
                resourcesPasswordFieldsService.reset()
                fcopyPasswordFieldsService.reset()
                execPasswordFieldsService.reset()
                return redirect(controller: 'menu', action: 'admin', params: [project: project])
            }
        }
        if(errors){
            request.errors=errors
        }


        return render(view:'editProjectConfig',model:
                [
                        project: params.project,
                        configs: configs,
                        projectPropertiesText: configText
                ])
    }
    def saveProject(){
        boolean valid=false
        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }
        def prefixKey= 'plugin'

        def project=params.project
        if (!project) {
            return renderErrorView("Project parameter is required")
        }

        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'menu', action: 'admin', params: [project: project])
        }

        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(authContext,
                        frameworkService.authResourceForProject(project), [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project',project)) {
            return
        }

        def framework = frameworkService.getRundeckFramework()

        def (resourceModelSourceDescriptions, nodeexecdescriptions, filecopydescs) = frameworkService.listDescriptions()
        def errors=[]
        def configs = []
        def resourceMappings = []
        def defaultNodeExec
        def defaultFileCopy
        def nodeexec, fcopy
        def nodeexecreport, fcopyreport
        Map<String,Map> extraConfig=[:]
        if(request.method=='POST'){
            //only attempt project create if form POST is used
            def Properties projProps = new Properties()
            if(params.description){
                projProps['project.description']=params.description
            }else{
                projProps['project.description']=''
            }
            def Set<String> removePrefixes=[]
            removePrefixes<< FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY
            if (params.defaultNodeExec) {
                (defaultNodeExec, nodeexec, nodeexecreport) = parseDefaultPluginConfig(errors, params.defaultNodeExec, "nodeexec", frameworkService.getNodeExecutorService(),'Node Executor')
                try {
                    execPasswordFieldsService.untrack([[config: [type: defaultNodeExec, props: nodeexec], index: 0]], * nodeexecdescriptions)
                    frameworkService.addProjectNodeExecutorPropertiesForType(defaultNodeExec, projProps, nodeexec, removePrefixes)
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                    errors << e.getMessage()
                }
            }
            if (params.defaultFileCopy) {
                (defaultFileCopy, fcopy, fcopyreport) = parseDefaultPluginConfig(errors, params.defaultFileCopy, "fcopy", frameworkService.getFileCopierService(),'File Copier')
                try {
                    fcopyPasswordFieldsService.untrack([[config: [type: defaultFileCopy, props: fcopy], index: 0]], * filecopydescs)
                    frameworkService.addProjectFileCopierPropertiesForType(defaultFileCopy, projProps, fcopy, removePrefixes)
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                    errors << e.getMessage()
                }
            }

            //parse plugin config properties, and convert to project.properties
            def sourceConfigPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX
            def ndxes = [params.list('index')].flatten().collect { Integer.valueOf(it) }


            resourcesPasswordFieldsService.adjust(ndxes)

            def count = 1
            ndxes.each {ndx ->
                def type = params[prefixKey + '.' + ndx + '.type']
                if(!type) {
                    log.warn("missing type def for prefix: " + prefixKey + '.' + ndx);
                    return
                }
                final service = framework.getResourceModelSourceService()
                def provider
                def description
                if (!(type =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)) {
                    errors << "Invalid Resource Model Source definition for source #${ndx}"
                } else {
                    try {
                        provider = service.providerOfType(type)
                    } catch (com.dtolabs.rundeck.core.execution.service.ExecutionServiceException e) {
                        errors << "Resource Model Source was not found: ${type}"
                    }

                    if (provider && provider instanceof Describable) {
                        description = provider.description
                    }
                }

                final String resourceConfigPrefix = sourceConfigPrefix + '.' + count + '.config.'
                final String resourceType = sourceConfigPrefix + '.' + count + '.type'
                count++

                projProps[resourceType] = type
                def mapprops = frameworkService.parseResourceModelConfigInput(description, prefixKey + '.' + ndx + '.' + 'config.', params)

                Properties props = new Properties()
                props.putAll(mapprops)

                //store the parsed config
                def config = [type: type, props: props]
                configs << config
                resourceMappings<<[config:config,prefix: resourceConfigPrefix,index:ndx-1]
            }
            //replace any unmodified password fields with the session data
            resourcesPasswordFieldsService.untrack(resourceMappings, *resourceModelSourceDescriptions)
            //for each resources model source definition, add project properties from the input config
            resourceMappings.each{ Map mapping->
                def props=mapping.config.props
                def resourceConfigPrefix=mapping.prefix
                props.keySet().each { k ->
                    if (props[k]) {
                        projProps[resourceConfigPrefix + k] = props[k]
                    }
                }
            }

            removePrefixes << FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX


            //load extra configuration for grails services

            Map<String,RundeckProjectConfigurable> projectConfigurableBeans=applicationContext.getBeansOfType(RundeckProjectConfigurable)

            projectConfigurableBeans.each { k, RundeckProjectConfigurable v ->
                if(k.endsWith('Profiled')){
                    //skip profiled versions of beans
                    return
                }
                //construct input values for the bean
                def beanData=[
                        configurable:v,
                        prefix:"extraConfig.${k}"
                ]
                def input=params.extraConfig."${k}"
                beanData.values=input

                v.getProjectConfigProperties().findAll{it.type==Property.Type.Boolean}.each{
                    if(input[it.name]!='true'){
                        input[it.name]='false'
                    }
                }
                //validate
                def report=Validator.validate(input as Properties, v.projectConfigProperties)
                beanData.report=report
                if(!report.valid){
                    errors << ("Some configuration was invalid: "+ report )
                }else{
                    def projvalues = Validator.performMapping(input, v.getPropertiesMapping(),true)
                    projProps.putAll(projvalues)
                    //remove all previous settings
                    removePrefixes.addAll(v.getPropertiesMapping().values())
                }
                extraConfig[k]=beanData
            }

            if (!errors) {

                def result = frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)
                if (!result.success) {
                    errors << result.error
                }
            }

            if (!errors) {
                flash.message = "Project ${project} saved"

                resourcesPasswordFieldsService.reset()
                fcopyPasswordFieldsService.reset()
                execPasswordFieldsService.reset()
                return redirect(controller: 'menu', action: 'admin', params: [project: project])
            }
        }
        if(errors){
            request.errors=errors
        }


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
            resourceModelConfigDescriptions: resourceModelSourceDescriptions,
            nodeexecreport: nodeexecreport,
            fcopyreport: fcopyreport,
            prefixKey: prefixKey,
            configs: configs,
            extraConfig:extraConfig
        ])
    }

    private List parseDefaultPluginConfig(ArrayList errors, ndx, String identifier, ProviderService service, String title) {
        def (type, config) = parseServiceConfigInput(params, identifier, ndx)
        def report
        if (!(type =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)) {
            errors << "${title} provider name is invalid"
        } else {
            final validation = frameworkService.validateServiceConfig(type, identifier+".${ndx}.config.", params, service)
            if (!validation.valid) {
                report = validation.report
                errors << validation.error ? "${title} configuration was invalid: " + validation.error : "${title} configuration was invalid"
            }
        }
        [type, config, report]
    }

    def editProject (){
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }

        def project = params.project

        if (unauthorizedResponse(
            frameworkService.authorizeApplicationResourceAll(
                frameworkService.getAuthContextForSubject(session.subject),
                frameworkService.authResourceForProject(project),
                [AuthConstants.ACTION_ADMIN]),
            AuthConstants.ACTION_ADMIN, 'Project', project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        final def (resourceDescs, execDesc, filecopyDesc) = frameworkService.listDescriptions()

        //get list of node executor, and file copier services

        final defaultNodeExec = frameworkService.getDefaultNodeExecutorService(project)
        final defaultFileCopy = frameworkService.getDefaultFileCopyService(project)

        final nodeConfig = frameworkService.getNodeExecConfigurationForType(defaultNodeExec, project)
        final filecopyConfig = frameworkService.getFileCopyConfigurationForType(defaultFileCopy, project)
        final resourceConfig = frameworkService.listResourceModelConfigurations(project)

        // Reset Password Fields in Session
        resourcesPasswordFieldsService.reset()
        execPasswordFieldsService.reset()
        fcopyPasswordFieldsService.reset()
        // Store Password Fields values in Session
        // Replace the Password Fields in configs with hashes
        resourcesPasswordFieldsService.track(resourceConfig, *resourceDescs)
        execPasswordFieldsService.track([[type:defaultNodeExec,props:nodeConfig]], *execDesc)
        fcopyPasswordFieldsService.track([[type:defaultFileCopy,props:filecopyConfig]], *filecopyDesc)
        // resourceConfig CRUD rely on this session mapping
        // saveProject will replace the password fields on change

        //get grails services that declare project configurations
        Map<String,RundeckProjectConfigurable> projectConfigurableBeans=applicationContext.getBeansOfType(RundeckProjectConfigurable)

        Map<String,Map> extraConfig=[:]
        projectConfigurableBeans.each { k, v ->
            if(k.endsWith('Profiled')){
                //skip profiled versions of beans
                return
            }
            //construct existing values from project properties
            def values=Validator.demapProperties(fwkProject.getProjectProperties(),v.getPropertiesMapping(), true)
            extraConfig[k]=[
                    configurable:v,
                    values:values,
                    prefix:"extraConfig.${k}."
            ]
        }

        [
            project: project,
            projectDescription:fwkProject.getProjectProperties().get("project.description"),
            resourceModelConfigDescriptions: resourceDescs,
            configs: resourceConfig,
            nodeexecconfig:nodeConfig,
            fcopyconfig:filecopyConfig,
            defaultNodeExec: defaultNodeExec,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: execDesc,
            fileCopyDescriptions: filecopyDesc,
            prefixKey: 'plugin',
            extraConfig:extraConfig
        ]
    }
    def editProjectFile (){
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }
        if(!params.filename || !(params.filename in ['readme.md','motd.md'])){
            return renderErrorView("filename parameter must be one of readme.md,motd.md")
        }

        def project = params.project

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        frameworkService.getAuthContextForSubject(session.subject),
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project', project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        def fileText=''
        if(fwkProject.existsFileResource(params.filename)){
            def baos=new ByteArrayOutputStream()
            fwkProject.loadFileResource(params.filename,baos)
            fileText=baos.toString('UTF-8')
        }
        [
                filename:params.filename,
                fileText:fileText
        ]
    }
    def saveProjectFile (){
        boolean valid=false
        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }
        if(!params.filename || !(params.filename in ['readme.md','motd.md'])){
            return renderErrorView("filename parameter must be one of readme.md,motd.md")
        }
        if(null==params.fileText){
            return renderErrorView("fileText parameter is required")
        }

        def project = params.project

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        frameworkService.getAuthContextForSubject(session.subject),
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_ADMIN, 'Project', project)) {
            return
        }

        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'menu', action: 'admin', params: [project: project])
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        if(params.fileText.trim()){
            def bais=new ByteArrayInputStream(params.fileText.toString().bytes)
            fwkProject.storeFileResource(params.filename,bais)
            flash.message='Saved project file '+params.filename
        }else{
            //delete
            fwkProject.deleteFileResource(params.filename)
            flash.message='Cleared project file '+params.filename
        }

        return redirect(controller: 'menu', action: 'admin', params: [project: project])
    }
    def editProjectConfig (){
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }

        def project = params.project

        if (unauthorizedResponse(
            frameworkService.authorizeApplicationResourceAll(
                frameworkService.getAuthContextForSubject(session.subject),
                frameworkService.authResourceForProject(project),
                [AuthConstants.ACTION_ADMIN]),
            AuthConstants.ACTION_ADMIN, 'Project', project)) {
            return
        }

        final def framework = frameworkService.getRundeckFramework()
        final def fwkProject = frameworkService.getFrameworkProject(project)
        final def (resourceDescs, execDesc, filecopyDesc) = frameworkService.listDescriptions()

        def projectProps = fwkProject.getProjectProperties() as Properties

        //get list of node executor, and file copier services

        final defaultNodeExec = frameworkService.getDefaultNodeExecutorService(projectProps)
        final defaultFileCopy = frameworkService.getDefaultFileCopyService(projectProps)

        final nodeConfig = frameworkService.getNodeExecConfigurationForType(defaultNodeExec, projectProps)
        final filecopyConfig = frameworkService.getFileCopyConfigurationForType(defaultFileCopy, projectProps)
        final resourceConfig = frameworkService.listResourceModelConfigurations(projectProps)

        // Reset Password Fields in Session
        resourcesPasswordFieldsService.reset()
        execPasswordFieldsService.reset()
        fcopyPasswordFieldsService.reset()
        // Store Password Fields values in Session
        // Replace the Password Fields in configs with hashes
        resourcesPasswordFieldsService.track(resourceConfig,true, *resourceDescs)
        execPasswordFieldsService.track([[type:defaultNodeExec,props:nodeConfig]], true, *execDesc)
        fcopyPasswordFieldsService.track([[type:defaultFileCopy,props:filecopyConfig]],true, *filecopyDesc)
        // resourceConfig CRUD rely on this session mapping
        // saveProject will replace the password fields on change
        //replace password values with hashed values
        frameworkService.addProjectFileCopierPropertiesForType(defaultFileCopy, projectProps, filecopyConfig)
        frameworkService.addProjectNodeExecutorPropertiesForType(defaultNodeExec, projectProps, nodeConfig)
        def count = 1
        final service = framework.getResourceModelSourceService()
        resourceConfig.each {resconfig ->
            def type = resconfig.type

            final String resourceConfigPrefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + '.' + count + '.config.'
            final String resourceType = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + '.' + count + '.type'
            count++

            projectProps[resourceType] = type
            resconfig.props.each{k,v->
                projectProps[resourceConfigPrefix+k]=v
            }
        }


//        def baos=new ByteArrayOutputStream()
//        projectProps.storeToXML(baos,"edit below",'UTF-8')
//        def projectPropertiesText = new String(baos.toByteArray(),"UTF-8")//.
        def sw=new StringWriter()
        projectProps.store(sw,"edit below")
        def projectPropertiesText = sw.toString().
                split(Pattern.quote(System.getProperty("line.separator"))).
                sort().
                join(System.getProperty("line.separator"))
        [
            project: project,
            projectDescription:fwkProject.getProjectProperties().get("project.description"),
            resourceModelConfigDescriptions: resourceDescs,
            configs: resourceConfig,
            nodeexecconfig:nodeConfig,
            fcopyconfig:filecopyConfig,
            defaultNodeExec: defaultNodeExec,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: execDesc,
            fileCopyDescriptions: filecopyDesc,
            projectPropertiesText:projectPropertiesText,
            prefixKey: 'plugin'
        ]
    }

    public def createResourceModelConfig(PluginConfigParams pluginConfig){
        if(pluginConfig.hasErrors()){
            request.errors=pluginConfig.errors
            return render(template: '/common/messages')
        }
        Framework framework = frameworkService.getRundeckFramework()
        def error
        if(!params.type){
            error = "Plugin provider type must be specified"
        }
        final service = framework.getResourceModelSourceService()
        def provider=null
        try {
            provider = service.providerOfType(params.type)
        } catch (ExecutionServiceException e) {
        }

        if(provider && provider instanceof Describable){
            def desc = provider.description
            return [description:desc,prefix:params.prefix,type:params.type,isCreate:true]
        }else{
            error="Invalid provider type: ${params.type}, not available for configuration"
        }

        request.error=error
        return render(template: '/common/messages')
    }

    public def checkResourceModelConfig(PluginConfigParams pluginConfig) {
        if (pluginConfig.hasErrors()) {
            def errorMsgs = pluginConfig.errors.allErrors.collect { g.message(error: it) }
            return render([valid:false,errors: errorMsgs, error: errorMsgs.join(', ')] as JSON)
        }
        def framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type=params[prefix+'type']
        def newparams = new PluginConfigParams()
        newparams.type = type
        newparams.validate()
        if (newparams.hasErrors()) {
            def errorMsgs = newparams.errors.allErrors.collect { g.message(error: it) }
            return render ([valid: false, errors: errorMsgs, error: errorMsgs.join(', ')] as JSON)
        }
        if('true'==params.revert){
            prefix='orig.'+prefix
        }
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


    def editResourceModelConfig(PluginConfigParams pluginConfig) {
        if (pluginConfig.hasErrors()) {
            request.errors = pluginConfig.errors
            return render(template: '/common/messages')
        }
        Framework framework = frameworkService.getRundeckFramework()
        def error
        def prefix = params.prefix ?: ''
        def String type = params[prefix + 'type']
        def newparams = new PluginConfigParams()
        newparams.type = type
        if (!newparams.validate()) {
            request.errors = newparams.errors
            return render(template: '/common/messages')
        }
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
    def viewResourceModelConfig (PluginConfigParams pluginConfig) {
        if (pluginConfig.hasErrors()) {
            request.errors = pluginConfig.errors
            return render(template: '/common/messages')
        }
        def framework = frameworkService.getRundeckFramework()
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
            def validate = frameworkService.validateServiceConfig(type, useprefix + 'config.', params, framework.getResourceModelSourceService())
            error = validate.error
            desc = validate.desc
            props = validate.props
            report = validate.report
        }

        return [
                prefix: prefix,
                values: props,
                includeFormFields: true,
                description: desc,
                report: report,
                error: error,
                saved:true,
                type: type
        ]
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
        if(unauthorizedResponse(frameworkService.authorizeApplicationResourceAll(authContext,
                frameworkService.authResourceForProject(project), [AuthConstants.ACTION_READ]),
                AuthConstants.ACTION_READ,'Project',project,true)){
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
        if (unauthorizedResponse(frameworkService.authorizeApplicationResourceAll(authContext,
                frameworkService.authResourceForProject(project), [AuthConstants.ACTION_READ]),
                AuthConstants.ACTION_READ, 'Project', project, true)) {
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
                jobCreate: frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_JOB,
                        AuthConstants.ACTION_CREATE, project)
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
            projects = frameworkService.projectNames(authContext)
            session.frameworkProjects=projects
        }
        [projects:projects,project:params.project] + (params.page?[selectParams:[page:params.page]]:[:])
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
     * @deprecated will be removed
     * */
    def apiProjectResourcesRefresh () {
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
    def apiProjectResourcesPost() {
        if (!apiService.requireVersion(request, response,ApiRequestFilters.V2)) {
            return
        }
        Framework framework = frameworkService.getRundeckFramework()
        if (!params.project) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])

        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        def exists = frameworkService.existsFrameworkProject(params.project)
        if (!exists) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])
        }
        if (!frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_UPDATE], params.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Update Nodes', 'Project', params.project]])
        }
        final IRundeckProject project = frameworkService.getFrameworkProject(params.project)

        def didsucceed=false
        def errormsg=null
        //determine data
        //assume post request
        if(!request.post){
            //bad method
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_METHOD_NOT_ALLOWED,
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
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
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
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: [e.message]])
        }catch (Exception e){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
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
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    code: 'api.project.updateResources.failed', args: [e.message]])
        }
        withFormat{
            xml{
                return apiService.renderSuccessXml(response, 'api.project.updateResources.succeeded', [params.project])
            }
            json{
                return apiService.renderSuccessJson(response){
                    success=true
                    message=g.message(code:'api.project.updateResources.succeeded', args:[params.project])
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
     * API: /api/14/project/PROJECT/resource/NAME, version 14
     */
    def apiResourcev14 () {
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V14)){
            return
        }
        return apiResource()
    }
    /**
     * API: /api/resource/$name, version 1
     */
    def apiResource(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        Framework framework = frameworkService.getRundeckFramework()
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
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                [AuthConstants.ACTION_READ], params.project)) {
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
    def apiResourcesv2(ExtNodeFilters query) {
        if (!apiService.requireVersion(request, response,ApiRequestFilters.V2)) {
            return
        }
        return apiResources(query)
    }
    /**
     * API: /api/1/resources, version 1
     */
    def apiResources(ExtNodeFilters query) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (query.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: [query.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        Framework framework = frameworkService.getRundeckFramework()
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])

        }
        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project',params.project]])


        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                [AuthConstants.ACTION_READ], params.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project]])

        }
        if (params.format && !(params.format in ['all','xml','yaml']) ||
                response.format && !(response.format in ['all','html','xml','yaml'])) {
            //expected another content type
            def reqformat = params.format ?: response.format
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
    protected String apiRenderNodeResult(INodeSet nodes, Framework framework, String project) {
        if (params.format && !(params.format in ['xml', 'yaml']) ||
                response.format &&
                !(response.format in ['all', 'html', 'xml', 'yaml'])) {
            //expected another content type
            if (!apiService.requireVersion(request, response, ApiRequestFilters.V3)) {
                return
            }
            def reqformat = params.format ?: response.format
            //render specified format
            final service = framework.getResourceFormatGeneratorService()
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            def generator
            [params.format,response.format].each{
                if(!generator && it){
                    try{
                        generator = service.getGeneratorForFormat(it)
                    }catch (UnsupportedFormatException e) {
                        log.debug("could not get generator for format: ${it}: ${e.message}",e)
                    }
                }
            }
            if(!generator){
                //try accept header
                try{
                    generator = service.getGeneratorForMIMEType(request.getHeader("accept"))
                }catch (UnsupportedFormatException e) {
                    log.debug("could not get generator for mime type: ${request.getHeader("accept")}: ${e.message}",e)
                }
            }
            if(!generator){
                return apiService.renderErrorFormat(
                        response,
                        [
                                status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                                code  : 'api.error.resource.format.unsupported',
                                args: [reqformat]
                        ]
                )
            }


            try {
                generator.generateDocument(nodes, baos)
            } catch (ResourceFormatGeneratorException e) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                               code  : 'api.error.resource.format.generator', args: [e.message]]
                )
            } catch (IOException e) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                               code  : 'api.error.resource.format.generator', args: [e.message]]
                )
            }
            final types = generator.getMIMETypes() as List
            return render(contentType: types[0], encoding: "UTF-8", text: baos.toString())
        }
        withFormat {
            xml {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final NodesFileGenerator generator = new ResourceXMLGenerator(baos)
                nodes.nodes.each { INodeEntry node ->
                    generator.addNode(node)
                }
                generator.generate()
                return render(contentType: "text/xml", encoding: "UTF-8", text: baos.toString())
            }
            yaml {
                if (nodes.nodes.size() > 0) {
                    StringWriter sw = new StringWriter()
                    final NodesFileGenerator generator = new NodesYamlGenerator(sw)
                    nodes.nodes.each { INodeEntry node ->
                        generator.addNode(node)
                    }
                    generator.generate()
                    return render(contentType: "text/yaml", encoding: "UTF-8", text: sw.toString())
                } else {
                    return render(contentType: "text/yaml", encoding: "UTF-8", text: "# 0 results for query\n")
                }
            }
        }
    }

    /**
     * /api/14/system/acl/* endpoint
     */
    def apiSystemAcls(){
        if (!apiService.requireVersion(request, response, ApiRequestFilters.V14)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        def authAction = ApiService.HTTP_METHOD_ACTIONS[request.method]
        if (!frameworkService.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,
                [
                        authAction,
                        AuthConstants.ACTION_ADMIN
                ]
        )) {
            apiService.renderErrorFormat(response,
                                         [
                                                 status: HttpServletResponse.SC_FORBIDDEN,
                                                 code: "api.error.item.unauthorized",
                                                 args: [authAction,'Rundeck System ACLs','']
                                         ])
            return null
        }
        if(params.path && !(params.path ==~ /^[^\/]+.aclpolicy$/ )){
            def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.invalid',
                    args:[params.path,'path','Must refer to a file ending in .aclpolicy'],
                    format:respFormat
            ])
        }
        def prefix = AuthorizationService.ACL_STORAGE_PATH_BASE
        def storagePath = prefix + (params.path ?: '')
        log.debug("apiSystemAcls, path: ${params.path}, storage path: ${storagePath}")
        switch (request.method) {
            case 'POST':
            case 'PUT':
                apiSystemAclsPutResource(storagePath, request.method=='POST')
                break
            case 'GET':
                apiSystemAclsGetResource(storagePath, prefix)
                break
            case 'DELETE':
                apiSystemAclsDeleteResource(storagePath)
                break
        }
    }

    private def renderAclHref(String path) {
        createLink(absolute: true, uri: "/api/${ApiRequestFilters.API_CURRENT_VERSION}/system/acl/$path")
    }
    private def apiSystemAclsPutResource(String storagePath, boolean create) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','yaml','text'],request.format)


        def exists = configStorageService.existsFileResource(storagePath)
        if(create && exists){
            //conflict
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_CONFLICT,
                    code: 'api.error.item.alreadyexists',
                    args: ['System ACL Policy File', params.path],
                    format:respFormat
            ])
        }else if(!create && !exists){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args: ['System ACL Policy File', params.path],
                    format:respFormat
            ])
        }
        def error = null
        String text = null
        if (request.format in ['yaml','text']) {
            try {
                text = request.inputStream.text
            } catch (Throwable e) {
                error = e.message
            }
        }else{
            def succeeded = apiService.parseJsonXmlWith(request,response,[
                    xml:{xml->
                        if(xml?.name()=='contents'){
                            text=xml?.text()
                        }else{
                            text = xml?.contents[0]?.text()
                        }
                    },
                    json:{json->
                        text = json?.contents
                    }
            ])
            if(!succeeded){
                error= "unexpected format: ${request.format}"
                return
            }
        }
        if(error){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: error,
                    format: respFormat
            ])
        }

        if(!text){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: "No content",
                    format: respFormat
            ])
        }

        //validate input
        Validation validation = authorizationService.validateYamlPolicy(params.path, text)
        if(!validation.valid){
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return withFormat{
                def j={
                    render(contentType:'application/json'){
                        apiService.renderJsonAclpolicyValidation(validation,delegate)
                    }
                }
                xml{
                    render(contentType: 'application/xml'){
                        apiService.renderXmlAclpolicyValidation(validation,delegate)
                    }
                }
                json j
                '*' j
            }
        }

        configStorageService.writeFileResource(storagePath,new ByteArrayInputStream(text.bytes),[:])
        response.status=create ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK
        if(respFormat in ['yaml','text']){
            //write directly
            response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
            configStorageService.loadFileResource(storagePath,response.outputStream)
            response.outputStream.close()
        }else{
            def baos=new ByteArrayOutputStream()
            configStorageService.loadFileResource(storagePath,baos)
            withFormat{
                xml{
                    render(contentType: 'application/xml'){
                        apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                    }

                }
                def j={
                    render(contentType:'application/json'){
                        apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                    }
                }
                json j
                '*' j
            }
        }
    }
    /**
     * Get resource or dir listing for the specified project path
     * @param project project
     * @param projectFilePath path for the project file or dir
     * @param rmprefix prefix string for the path, to be removed from paths in dir listings
     * @return
     */
    private def apiSystemAclsGetResource(String projectFilePath,String rmprefix) {
        def respFormat = apiService.extractResponseFormat(request, response, ['yaml','xml','json','text'],request.format)
        if(configStorageService.existsFileResource(projectFilePath)){
            if(respFormat in ['yaml','text']){
                //write directly
                response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
                configStorageService.loadFileResource(projectFilePath,response.outputStream)
                response.outputStream.close()
            }else{
                //render as json/xml with contents as string
                def baos=new ByteArrayOutputStream()
                configStorageService.loadFileResource(projectFilePath,baos)
                withFormat{
                    def j={
                        render(contentType:'application/json'){
                            apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                        }
                    }
                    xml{
                        render(contentType: 'application/xml'){
                            apiService.renderWrappedFileContents(baos.toString(),respFormat,delegate)
                        }

                    }
                    json j
                    '*' j
                }
            }
        }else if(configStorageService.existsDirResource(projectFilePath) || projectFilePath==rmprefix){
            //list aclpolicy files in the dir
            def list=configStorageService.listDirPaths(projectFilePath).findAll{
                it ==~ /.*\.aclpolicy$/
            }
            withFormat{
                xml{
                    render(contentType: 'application/xml'){
                        apiService.xmlRenderDirList(
                                projectFilePath,
                                { String p -> apiService.pathRmPrefix(p, rmprefix) },
                                { String p -> renderAclHref(apiService.pathRmPrefix(p, rmprefix)) },
                                list,
                                delegate
                        )
                    }

                }
                def j ={
                    render(contentType:'application/json') {
                        apiService.jsonRenderDirlist(
                                projectFilePath,
                                { String p -> apiService.pathRmPrefix(p, rmprefix) },
                                { String p -> renderAclHref(apiService.pathRmPrefix(p, rmprefix)) },
                                list,
                                delegate
                        )
                    }
                }
                json j
                '*' j
            }
        }else{

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.path],
                    format: respFormat
            ])
        }

    }


    private def apiSystemAclsDeleteResource(projectFilePath) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)
        boolean exists=configStorageService.existsFileResource(projectFilePath)
        if(!exists){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['System ACL Policy File',params.path],
                    format: respFormat
            ])
        }
        boolean done=configStorageService.deleteFileResource(projectFilePath)
        if(!done){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    message: "error",
                    format: respFormat
            ])
        }
        render(status: HttpServletResponse.SC_NO_CONTENT)
    }
}

