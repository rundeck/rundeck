package rundeck.controllers

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.plugins.configuration.Describable

import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.shared.resources.ResourceXMLGenerator

import grails.converters.JSON
import rundeck.Execution
import rundeck.ScheduledExecution

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
import com.dtolabs.rundeck.core.execution.impl.jsch.JschNodeExecutor
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

class FrameworkController  {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    def metricService
    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
        apiProjectResources: [ 'POST'],
        apiProjectResourcesRefresh: ['POST'],
    ]

    def index = {
        redirect(action:"nodes")        
    }

    private unauthorized(String action, boolean fragment = false) {
        if (!fragment) {
            response.setStatus(403)
        }
        request.title = "Unauthorized"
        request.error = "${request.remoteUser} is not authorized to: ${action}"
        response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, request.error)
        render(template: fragment ? '/common/errorFragment' : '/common/error', model: [:])
    }

    def noProjectAccess = {
        response.setStatus(403)
        def roles = request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.collect { it.name }?.join(", ")
        request.title = "Unauthorized"
        request.error = "No authorized access to projects. Contact your administrator. (User roles: " + roles + ")"
        response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, request.error)

        log.error("'${request.remoteUser}' has no authorized access. Roles: "+ roles)
        return render(view: '/common/error', model: [:])
    }

    def nodes ={ ExtNodeFilters query ->
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        String runCommand;
        if(params.fromExecId|| params.retryFailedExecId) {
            Execution e = Execution.get(params.fromExecId?: params.retryFailedExecId)
            if (e && !frameworkService.authorizeProjectExecutionAll(framework, e, [AuthConstants.ACTION_READ])) {
                return unauthorized("Read Execution ${params.fromExecId ?: params.retryFailedExecId}")
            }

            if (e && !e.scheduledExecution && e.workflow.commands.size() == 1) {
                def cmd = e.workflow.commands[0]
                if (cmd.adhocRemoteString) {
                    runCommand = cmd.adhocRemoteString
                    //configure node filters
                    if (params.retryFailedExecId) {
                        query = new ExtNodeFilters(nodeIncludeName: e.failedNodeList, project: e.project)
                    }else{
                        query = ExtNodeFilters.from(e, e.project)
                    }
                }
            }
        }else if(params.exec){
            runCommand=params.exec
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

        if (query.nodeFilterIsEmpty()) {
            if ('true' == params.defaultLocalNode) {
                query.nodeIncludeName = framework.getFrameworkNodeName()
            } else {
                query.nodeIncludeName = '.*'
            }
        }
        if (query && !query.project && session.project) {
            query.project = session.project
        }
        def model=[query:query,params:params]

        if (usedFilter) {
            model['filterName'] = usedFilter
        }
        return model + [runCommand: runCommand]
    }
    /**
     * Nodes action lists nodes in resources view, also called by nodesFragment to
     * render a set of nodes via ajax
     */
    def nodesdata (ExtNodeFilters query){

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(query.nodeFilterIsEmpty()){
            if(params.formInput=='true' && 'true'!=params.defaultLocalNode){
                query.nodeIncludeName = '.*'
            }else{
                query.nodeIncludeName = framework.getFrameworkNodeName()
            }
        }
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
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'node'], ['read'], query.project)) {
            return unauthorized("Read Nodes for project ${query.project}")
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
                request.error='<pre>'+e.getMessage()+'</pre>'
                filterErrors['filter']='<pre>'+e.getMessage()+'</pre>'
                nodeset=new NodeSetImpl()
            }
        }else if("true"==params.defaultAllNodes){
            //match all nodes if filters are all blank
            nodeset=nodes1
        }else{
            //match only local node if filters are blank
            nodeset = new NodeSetImpl()
            nodeset.putNode(nodes1.getNode(framework.getFrameworkNodeName()))
        }
//            nodes = nodes.sort { INodeEntry a, INodeEntry b -> return a.nodename.compareTo(b.nodename) }
        //filter nodes by read authorization

        def readnodes = framework.filterAuthorizedNodes(query.project, ['read'] as Set, nodeset)
        def runnodes = framework.filterAuthorizedNodes(query.project, ['run'] as Set, readnodes)
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
                if(params.fullresults){
                    allnodes[nd.nodename]=[node:nd,projects:[project],project:project,executions:[],resources:[],islocal:nd.nodename==framework.getFrameworkNodeName()]
                }
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

        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'node'], ['read'], query.project)) {
            return unauthorized("Read Nodes for project ${query.project}",true)
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
        if (query.nodeFilterIsEmpty()) {
            if (params.formInput == 'true' && 'true' != params.defaultLocalNode) {
                query.nodeIncludeName = '.*'
            } else {
                query.nodeIncludeName = framework.getFrameworkNodeName()
            }
        }
        if (query && !query.project && session.project) {
            query.project = session.project
        }
        def result = nodesdata(query)
        if (usedFilter) {
            result['filterName'] = usedFilter
        }
        if(!result.nodesvalid){
            request.error="Error parsing file \"${result.nodesfile}\": "+result.nodeserror? result.nodeserror*.message.join("\n"):'no message'
        }
        render(template:"allnodes",model: result)
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
            Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
            if(!frameworkService.authorizeProjectResource(framework,[type:'resource',kind:'node'], AuthConstants.ACTION_REFRESH,params.project)){
                def msg = "user: ${session.user} UNAUTHORIZED for performNodeReload"
                log.error(msg)
                flash.error = msg
                return false
            }
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

    /**
     * Handles POST when creating a new project
     * @return
     */

    def createProjectPost() {
        metricService.markMeter(actionName)
        //only attempt project create if form POST is used
        def prefixKey = 'plugin'
        def project = params.project
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeApplicationResourceTypeAll(framework, 'project', ['create'])) {
            return unauthorized("Create a Project")
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
        final sshkeypath = new File(System.getProperty("user.home"), ".ssh/id_rsa").getAbsolutePath()
        if (params.resourcesUrl) {
            resourcesUrl = params.resourcesUrl
            projProps[FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY] = params.resourcesUrl
        }
        if (params.defaultNodeExec) {
            def ndx = params.defaultNodeExec
            (defaultNodeExec, nodeexec) = parseServiceConfigInput(params, "nodeexec", ndx)
            final validation = frameworkService.validateServiceConfig(framework, defaultNodeExec, "nodeexec.${ndx}.config.", params, framework.getNodeExecutorService())
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
            final validation = frameworkService.validateServiceConfig(framework, defaultFileCopy, "fcopy.${ndx}.config.", params, framework.getFileCopierService())
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

        if (params.sshkeypath) {
            sshkeypath = params.sshkeypath
            projProps[JschNodeExecutor.PROJ_PROP_SSH_KEYPATH] = sshkeypath
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
            try {
                proj = framework.getFrameworkProjectMgr().createFrameworkProject(project, projProps)
            } catch (Error e) {
                log.error(e.message)
                errors << e.getMessage()
            }
            if (!errors && proj) {
                session.project = proj.name
                session.frameworkProjects = frameworkService.projects(framework)
                def result = userService.storeFilterPref(session.user, [project: proj.name])
                return redirect(controller: 'menu', action: 'index')
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
        return chain(action: 'createProject',
                model:         [
                project: params.project,
                projectNameError: projectNameError,
                resourcesUrl: resourcesUrl,
                resourceModelConfigDescriptions: descriptions,
                sshkeypath: sshkeypath,
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        if(!frameworkService.authorizeApplicationResourceTypeAll(framework,'project',['create'])){
            return unauthorized("Create a Project")
        }
        final defaultNodeExec = NodeExecutorService.DEFAULT_REMOTE_PROVIDER
        final defaultFileCopy = FileCopierService.DEFAULT_REMOTE_PROVIDER
        final sshkeypath = new File(System.getProperty("user.home"), ".ssh/id_rsa").getAbsolutePath()
        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()

        return [
            project:params.project,
            resourceModelConfigDescriptions: descriptions,
            sshkeypath:sshkeypath,
            defaultNodeExec:defaultNodeExec,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            prefixKey:prefixKey]
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
        def project=params.project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def error=null
        def configs = []
        if (!project) {
            flash.error = "Project parameter is required"
            return render(template: "/common/error")
        }
        if (params.cancel == 'Cancel') {
            //cancel modification
            return redirect(controller: 'menu', action: 'admin')
        }
        if (!frameworkService.authorizeApplicationResourceAll(framework, [type:'project',name:project], [AuthConstants.ACTION_ADMIN])) {
            return unauthorized("Update Project ${project}")
        }
        def fproject = frameworkService.getFrameworkProject(project, framework)

        def resourcesUrl = fproject.hasProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY) ? fproject.getProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY) : null
        def defaultNodeExec
        def defaultFileCopy
        def sshkeypath
        def nodeexec,fcopy
        def nodeexecreport,fcopyreport
        if(request.method=='POST'){
            //only attempt project create if form POST is used
            def Properties projProps = new Properties()
            def Set<String> removePrefixes=[]
            if (params.resourcesUrl) {
                resourcesUrl=params.resourcesUrl
                projProps[FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY] = params.resourcesUrl
            }else{
                removePrefixes<< FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY
            }

            if (params.defaultNodeExec) {
                def ndx=params.defaultNodeExec
                (defaultNodeExec, nodeexec)=parseServiceConfigInput(params,"nodeexec",ndx)
                final validation = frameworkService.validateServiceConfig(framework, defaultNodeExec, "nodeexec.${ndx}.config.", params, framework.getNodeExecutorService())
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
                final validation = frameworkService.validateServiceConfig(framework, defaultFileCopy, "fcopy.${ndx}.config.", params, framework.getFileCopierService())
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
            if (params.sshkeypath) {
                sshkeypath = params.sshkeypath
                projProps[JschNodeExecutor.PROJ_PROP_SSH_KEYPATH] = sshkeypath
            } else {
                removePrefixes << JschNodeExecutor.PROJ_PROP_SSH_KEYPATH
            }

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
                try {
                    fproject.mergeProjectProperties(projProps,removePrefixes)
                } catch (Error e) {
                    log.error(e.message)
                    error= e.getMessage()
                }
            }
            if(!error){
                session.project=fproject.name
                def result=userService.storeFilterPref(session.user, [project: fproject.name])
                flash.message="Project ${project} saved"
                return redirect(controller:'menu',action:'admin')
            }
        }
        if(error){
            request.error=error
        }
        final descriptions = framework.getResourceModelSourceService().listDescriptions()
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()

        return render(view:'editProject',model:
        [resourcesUrl: resourcesUrl,
            project: params.project,
            sshkeypath: sshkeypath,
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if(!project){
            flash.error="Project parameter is required"
            return render(template: "/common/error")
        }
        if (!frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], [AuthConstants.ACTION_ADMIN])) {
            return unauthorized("Update Project ${project}")
        }
        def fproject = frameworkService.getFrameworkProject(project,framework)
        def configs = fproject.listResourceModelConfigurations()
        final service = framework.getResourceModelSourceService()
        final descriptions = service.listDescriptions()
        def resourcesUrl=fproject.hasProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY)? fproject.getProperty(FrameworkProject.PROJECT_RESOURCES_URL_PROPERTY):null

        //get list of node executor, and file copier services
        final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
        final filecopydescs = framework.getFileCopierService().listDescriptions()
        final sshkeypath = fproject.hasProperty(JschNodeExecutor.PROJ_PROP_SSH_KEYPATH) ? fproject.getProperty(JschNodeExecutor.PROJ_PROP_SSH_KEYPATH) : null

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

        [resourcesUrl: resourcesUrl, project: params.project, resourceModelConfigDescriptions: descriptions,
            sshkeypath: sshkeypath,
            defaultNodeExec: defaultNodeExec,
            nodeexecconfig:nodeexec,
            fcopyconfig:fcopy,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            prefixKey: prefixKey, configs: configs]
    }
    def createResourceModelConfig={
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def error
        def prefix = params.prefix ?: ''
        def String type=params[prefix+'type']
        Properties props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        }else{
            def validate = frameworkService.validateServiceConfig(framework, type, prefix+'config.', params,framework.getResourceModelSourceService())
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
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
            def validate = frameworkService.validateServiceConfig(framework, type, prefix + 'config.', params,framework.getResourceModelSourceService())
            result.valid=validate.valid
            result.error=validate.error
        }
        render result as JSON
    }


    def editResourceModelConfig = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def error
        def prefix = params.prefix ?: ''
        def String type = params[prefix + 'type']
        Properties props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        } else {
            def validate = frameworkService.validateServiceConfig(framework, type, prefix + 'config.', params,framework.getResourceModelSourceService())
            error = validate.error
            desc=validate.desc
            props=validate.props
            report=validate.report
        }

        render(view: 'createResourceModelConfig', model: [ prefix: prefix, values: props, description: desc, report: report, error: error, isEdit: "true"!=params.iscreate,type:type, isCreate:params.isCreate])
    }
    def viewResourceModelConfig = {
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
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
            def validate = frameworkService.validateServiceConfig(framework, type, prefix + 'config.', params,framework.getResourceModelSourceService())
            error = validate.error
            desc = validate.desc
            props = validate.props
            report = validate.report
        }

        return render(template: 'viewResourceModelConfig',model:[ prefix: prefix, values: props, includeFormFields: true, description: desc, report: report, error: error, saved:true, type: type])
    }
    def projectDescFragment(){
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def project = params.project
        if (!project) {
            flash.error = "Project parameter is required"
            return render(template: "/common/errorFragment")
        }
        if (!frameworkService.existsFrameworkProject(project, framework)
                || !frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], ['read'])) {
            flash.error = "Unauthorized"
            return render(template: "/common/errorFragment")
        }
        //look for readme.md in project directory
        def project1 = frameworkService.getFrameworkProject(project, framework)
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        def project=params.project
        if (!project) {
            flash.error = "Project parameter is required"
            return render(template: "/common/errorFragment")
        }
        if(!frameworkService.existsFrameworkProject(project, framework)
                || !frameworkService.authorizeApplicationResourceAll(framework, [type: 'project', name: project], ['read'])){
            flash.error = "Unauthorized"
            return render(template: "/common/errorFragment")
        }

        Calendar n = GregorianCalendar.getInstance()
//        n.add(Calendar.DAY_OF_YEAR, -1)
        n.set(Calendar.HOUR_OF_DAY, 0)
        n.set(Calendar.MINUTE, 0)
        n.set(Calendar.SECOND, 0)
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
                jobCreate: frameworkService.authorizeProjectResource(framework,[type:'resource',kind:'job'], 'create',project)
        ]
        //summary data
        def data= [
                jobCount: ScheduledExecution.countByProject(project),
                execCount: Execution.countByProjectAndDateStartedGreaterThan(project, today),
                userCount: users.size(),
                users: users,
                auth:auth
        ]
        withFormat {
            json{
                render data as JSON
            }
        }
    }

    def projectSelect={
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects
        if(session.frameworkProjects){
            projects=session.frameworkProjects
        }else{
            projects = frameworkService.projects(framework)
            session.frameworkProjects=projects
        }
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
        return redirect(controller: 'menu',action: 'index',params: [page:params.page])
    }

    static autosetSessionProject(session, final ArrayList projects) {
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
     * API: /api/2/project/NAME/resources/refresh
     * calls performNodeReload, then returns API response
     * */
    def apiProjectResourcesRefresh = {
        if (!new ApiController().requireVersion(ApiRequestFilters.V2)) {
            return
        }
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
     * API: /api/2/project/NAME/resources
     * GET: return resources data
     * POST: update resources data with either: text/xml content, text/yaml content, form-data param providerURL=<url>
     * */
    def apiProjectResources = {
        if (!new ApiController().requireVersion(ApiRequestFilters.V2)) {
            return
        }
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
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'node'], ['create','update'], params.project)) {
            flash.error = g.message(code: 'api.error.item.unauthorized', args: ['Update Nodes', 'Project', params.project])
            return chain(controller: 'api', action: 'error')
        }
        final FrameworkProject project = frameworkService.getFrameworkProject(params.project, framework)

        def didsucceed=false
        def errormsg=null
        //determine data
        //assume post request
        if(request.post){
            final contentType = request.contentType
            //try to parse loaded data
            if(!(contentType?.endsWith("/xml")||contentType?.endsWith('/yaml')|| contentType?.endsWith('/x-yaml'))){
                if (!new ApiController().requireVersion(ApiRequestFilters.V3)) {
                    //require api V3 for any other content type
                    return
                }
            }

            final parser
            try {
                parser = framework.getResourceFormatParserService().getParserForMIMEType(contentType)
            } catch (UnsupportedFormatException e) {
                //invalid data
                flash.error = "Unsupported format: ${e.getMessage()}"
                return new ApiController().error()
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
                flash.error = "Invalid data: ${e.getMessage()}"
                return new ApiController().error()
            }catch (Exception e){
                flash.error = "Error parsing data: ${e.getMessage()}"
                return new ApiController().error()
            }
            tempfile.delete()

            //finally update resources file with the new nodes data
            try {
                project.updateNodesResourceFile nodeset
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
        if (!frameworkService.authorizeApplicationResourceAll(framework, [type:'project',name:params.project], ['read'])) {
            flash.error = g.message(code: 'api.error.item.unauthorized', args: ['Read','Project',params.project])
            return chain(controller: 'api', action: 'error')
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
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'node'], ['read'], params.project)) {
            flash.error = g.message(code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project])
            return chain(controller: 'api', action: 'error')
        }

        NodeSet nset = new NodeSet()
        nset.setSingleNodeName(params.name)
        def pject=frameworkService.getFrameworkProject(params.project,framework)
        final INodeSet nodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nset,pject.getNodeSet())
        if(!nodes || nodes.nodes.size()<1 ){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['Node Name',params.name])
            return chain(controller:'api',action:'error')
        }
        return apiRenderNodeResult(nodes, framework, params.project)
    }
    /**
     * API: /api/2/project/NAME/resources, version 2
     */
    def apiResourcesv2={ExtNodeFilters query->
        if (!new ApiController().requireVersion(ApiRequestFilters.V2)) {
            return
        }
        return apiResources(query)
    }
    /**
     * API: /api/1/resources, version 1
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
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'node'], ['read'], params.project)) {
            flash.error = g.message(code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project])
            return chain(controller: 'api', action: 'error')
        }
        if (params.format && !(params.format in ['xml','yaml']) || request.format && !(request.format in ['html','xml','yaml'])) {
            //expected another content type
            def reqformat = params.format ?: request.format
            if (!new ApiController().requireVersion(ApiRequestFilters.V3)) {
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
            query.nodeInclude=".*"
        }
        def pject=frameworkService.getFrameworkProject(params.project,framework)
//        final Collection nodes = pject.getNodes().filterNodes(ExecutionService.filtersAsNodeSet(query))
        final INodeSet nodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(ExecutionService.filtersAsNodeSet(query), pject.getNodeSet())
        return apiRenderNodeResult(nodes,framework,params.project)
    }
    protected String apiRenderNodeResult(INodeSet nodes, Framework framework, String project){
        if (params.format && !(params.format in ['xml', 'yaml']) || request.format && !(request.format in ['all','html', 'xml', 'yaml'])) {
            //expected another content type
            if (!new ApiController().requireVersion(ApiRequestFilters.V3)) {
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
                flash.error = g.message(code: 'api.error.resource.format.unsupported', args: [reqformat])
                return new ApiController().error()
            }catch (ResourceFormatGeneratorException e){
                flash.error = g.message(code: 'api.error.resource.format.generator', args: [e.message])
                return new ApiController().error()
            }catch (IOException e){
                flash.error = g.message(code: 'api.error.resource.format.generator', args: [e.message])
                return new ApiController().error()
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

