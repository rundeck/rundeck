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

package rundeck.controllers


import com.dtolabs.rundeck.app.api.project.sources.Resources
import com.dtolabs.rundeck.app.api.project.sources.Source
import com.dtolabs.rundeck.app.api.project.sources.Sources
import com.dtolabs.rundeck.app.support.ExecutionCleanerConfigImpl
import com.dtolabs.rundeck.app.support.PluginConfigParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.NodeFileParserException
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.rundeck.app.api.model.ApiErrorResponse
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.common.ProviderService
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.plugins.ExtPluginConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import grails.converters.JSON
import grails.converters.XML
import grails.web.servlet.mvc.GrailsParameterMap
import org.rundeck.core.projects.ProjectPluginListConfigurable
import org.rundeck.storage.api.StorageException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.InvalidMimeTypeException
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.PasswordFieldsService
import rundeck.services.PluginService
import rundeck.services.ProjectService
import rundeck.services.ScheduledExecutionService

import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.Framework

import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException
import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.FrameworkResource
import rundeck.support.filters.BaseNodeFilters
import rundeck.support.filters.ExtNodeFilters
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.UserService
import com.dtolabs.rundeck.app.api.ApiVersions

@Controller
class FrameworkController extends ControllerBase implements ApplicationContextAware {
    public static final Integer MAX_DAYS_TO_KEEP = 60
    public static final Integer MINIMUM_EXECUTION_TO_KEEP = 50
    public static final Integer MAXIMUM_DELETION_SIZE = 500
    public static final String SCHEDULE_DEFAULT = "0 0 0 1/1 * ? *"
    static final String PROJECT_PLUGINS_REMOVED_EVENT = 'project.plugins.removed'
    public static final Map CRON_MODELS_SELECT_VALUES = [
            "0 0 0 1/1 * ? *"    : "Daily at 00:00",
            "0 0 23 ? * FRI *"   : "Weekly (Every Fridays 11PM)",
            "0 0 0 ? * WED,SUN *": "Weekly (Two days a week)",
            "0 30 1 1,15 * ? *"  : "Every 2 weeks",
            "0 0 12 1 1/1 ? *"   : "Monthly (All first day of month)",
            "0 0 0 1 1/2 ? *"    : "Every 2 months (Day 1)",
            "0 0 12 1 1/3 ? *"   : "Every 3 months (Day 1)"
    ]
    FrameworkService frameworkService
    ExecutionService executionService
    ScheduledExecutionService scheduledExecutionService
    UserService userService
    ProjectService projectService
    ConfigurationService configurationService

    PasswordFieldsService obscurePasswordFieldsService
    PasswordFieldsService resourcesPasswordFieldsService
    PasswordFieldsService execPasswordFieldsService
    PasswordFieldsService pluginsPasswordFieldsService
    PasswordFieldsService fcopyPasswordFieldsService
    PasswordFieldsService pluginGroupPasswordFieldsService

    def metricService
    def ContextACLManager<AppACLContext> aclFileManagerService
    def ApplicationContext applicationContext
    def MenuService menuService
    def PluginService pluginService

    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
        apiSourceWriteContent    : 'POST',
        apiSystemAcls            : ['GET', 'PUT', 'POST', 'DELETE'],
        createProjectPost        : 'POST',
        saveProject              : 'POST',
        saveProjectNodeSources   : 'POST',
        saveProjectNodeSourceFile: 'POST',
        saveProjectPluginsAjax   : 'POST',
        getProjectConfigurable   : 'GET',
        saveProjectConfigurable  : 'POST',
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
        RdUser u = userService.findOrCreateUser(session.user)
        def usedFilter = null
        def prefs=userService.getFilterPref(u.login)
        if(params.filterName=='.*'){
            query.filter='.*'
            usedFilter='.*'
        } else if (!query.filter && prefs['nodes']) {
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                        AuthConstants.ACTION_RUN, params.project),
                AuthConstants.ACTION_RUN, 'adhoc', 'commands')) {
            return
        }
        String runCommand;
        if (params.fromExecId || params.retryFailedExecId) {
            Execution e = Execution.get(params.fromExecId ?: params.retryFailedExecId)
            if (e && unauthorizedResponse(
                    rundeckAuthContextProcessor.authorizeProjectExecutionAny(authContext, e, [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW]),
                    AuthConstants.ACTION_VIEW, 'Execution', params.fromExecId ?: params.retryFailedExecId)) {
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

        query.project = params.project
        def result
        if(!query.nodeFilterIsEmpty()){
            params.requireRunAuth='true'
            result = [query: query, params: params, allnodes: [:]]
        }else{
            result= [query: query, params: params, allnodes:[:]]
        }
        def model = result//[query: query, params: params]

        return model + [runCommand: runCommand, emptyQuery: query.nodeFilterIsEmpty(), matchedNodesMaxCount: scheduledExecutionService.getMatchedNodesMaxCount()]
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
        AuthContext authContext=rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,query.project)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                        AuthConstants.ACTION_READ, query.project),
                AuthConstants.ACTION_READ, 'Project', 'nodes')) {
            return
        }
        def allnodes = [:]
        def totalexecs = [:]
        def total=0
        def allcount=null
        NodeSet nset = ExecutionService.filtersAsNodeSet(query)
        NodeSet nsetUnselected = ExecutionService.filtersExcludeAsNodeSet(query)
        def projects=[]
        def filterErrors=[:]
        def project = framework.getFrameworkProjectMgr().getFrameworkProject(query.project)
        def INodeSet nodeset
        def INodeSet unselectedNodeSet

        long mark=System.currentTimeMillis()
        INodeSet nodes1 = project.getNodeSet()

//        allcount=nodes1.nodes.size()
        if(params.localNodeOnly){
            nodeset=new NodeSetImpl()
            def localnode = nodes1.getNode(framework.getFrameworkNodeName())
            if(localnode) nodeset.putNode(localnode)
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
        if (nsetUnselected && !(nsetUnselected.include?.blank && nsetUnselected.exclude?.blank)){
            try {
                unselectedNodeSet = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nsetUnselected, nodes1)
            } catch (PatternSyntaxException e) {
                filterErrors['filter']=e.getMessage()
                unselectedNodeSet=new NodeSetImpl()
            }
        }
//            nodes = nodes.sort { INodeEntry a, INodeEntry b -> return a.nodename.compareTo(b.nodename) }
        //filter nodes by read authorization

        mark=System.currentTimeMillis()
        def readnodes = rundeckAuthContextProcessor.filterAuthorizedNodes(query.project, ['read'] as Set, nodeset, authContext)
        log.debug("nodesData filterAuthorizedNodes[read]: ${System.currentTimeMillis()-mark}ms")
        mark=System.currentTimeMillis()
        def runnodes = rundeckAuthContextProcessor.filterAuthorizedNodes(query.project, ['run'] as Set, readnodes, authContext)
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
                def maxByProps = configurationService.getInteger("gui.matchedNodesMaxCount",null)
                if( maxByProps ) {
                    max = maxByProps
                }else{
                    max=Integer.parseInt(params.max)
                }
            }else{
                max=20
            }
            if(page<0){
                //if page is negative, load all remaining values starting at page -1*page
                remaining=true;
                page=page*-1;
            }
        }
        def truncateMax=params.untruncate?-1: params.maxShown?params.int('maxShown'):100
        if(truncateMax && max){
            truncateMax=Math.max(truncateMax,max)
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
            def unSelected = unselectedNodeSet?.getNodes().findAll{it.nodename == nd.nodename}

            allnodes[nd.nodename]=[node:nd,project:project.name,islocal:nd.nodename==framework.getFrameworkNodeName(), unselected: unSelected?true:false]
            if(params.requireRunAuth == 'true'  || runnodes.getNode(nd.nodename)){
                noderunauthmap[nd.nodename]=true
            }
        }
        if(filterErrors){
            request.filterErrors=filterErrors
        }

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

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,project)
        if (!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                                                          AuthConstants.ACTION_READ,
                                                          project
        )) {

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'request.error.unauthorized.message',
                    args:['read','Nodes for Project',project],
                    format:'json'
            ])
        }
        RdUser u = userService.findOrCreateUser(session.user)
        def defaultFilter = null
        Map filterpref = userService.parseKeyValuePref(u.filterPref)
        if (filterpref['nodes']) {
            defaultFilter = filterpref['nodes']
        }

        def fwkproject = frameworkService.getFrameworkProject(project)
        INodeSet nodes1 = fwkproject.getNodeSet()
        def size=nodes1.nodes.size()
        def tagsummary = frameworkService.summarizeTags(nodes1.nodes)
        tagsummary = tagsummary.keySet().sort().collect{
            [tag:it,count:tagsummary[it]]
        }
        render(contentType: 'application/json',text: [tags:tagsummary,totalCount:size,defaultFilter:defaultFilter] as JSON)
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                                                             AuthConstants.ACTION_READ,
                                                             query.project
                ),
                AuthConstants.ACTION_READ, 'Project', 'nodes', true
        )) {
            return
        }
        RdUser u = userService.findOrCreateUser(session.user)
        if (!params.filterName && u && query.nodeFilterIsEmpty() && params.formInput != 'true') {
            Map filterpref = userService.parseKeyValuePref(u.filterPref)
            if (filterpref['nodes']) {
                params.filterName = filterpref['nodes']
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
            return renderErrorFragment([beanErrors:query.errors])
        }
        def result= nodesFragmentData(query)
        render(template:"allnodes",model: result)
    }
    /**
     * nodesFragment renders a set of nodes in HTML snippet, for ajax
     */
    def nodesQueryAjax(ExtNodeFilters query) {
        if (requireAjax(action: 'nodes', params: params)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                                                             AuthConstants.ACTION_READ,
                                                             query.project
                )) {

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'request.error.unauthorized.message',
                    args:['read','Nodes for Project',query.project],
                    format:'json'
            ])
        }
        if (query.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorFragment([beanErrors:query.errors])
        }
        Map result= nodesFragmentData(query)
        result.remove('selectedProject')
        result.remove('query')
        result.remove('params')
        def nodes=result.remove('allnodes')
        def controller = this
        withFormat {
            '*' {
                return render ((result + [
                        allnodes: nodes.collect{entry->
                            [
                                    nodename:entry.key,
                                    islocal:entry.value.islocal,
                                    tags:entry.value.node.tags,
                                    attributes:entry.value.node.attributes,
                                    authrun:result.nodeauthrun[entry.key]?true:false,
                                    unselected:entry.value.unselected
                            ]
                        }
                ]) as JSON)
            }
            if (controller.isAllowXml()) {
                xml {
                    return render(result as XML)
                }
            }
        }
    }


    /**
     * Handles POST when creating a new project
     * @return
     */

    def createProjectPost() {
        metricService.markMeter(this.class.name,actionName)
        if (!requestHasValidToken()) {
            return
        }
        //cancel modification
        if (params.cancel) {
            return redirect(controller: 'menu', action: 'home', )
        }

        //only attempt project create if form POST is used
        def prefixKey = 'plugin'
        def project = params.newproject
        Framework framework = frameworkService.getRundeckFramework()
        ProjectManager projectManager = framework.getFrameworkProjectMgr()
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeApplicationResourceTypeAll(authContext, 'project', [AuthConstants
                        .ACTION_CREATE]),
                AuthConstants.ACTION_CREATE, 'New Project')) {
            return
        }
        def nodeexec, nodeexecreport
        def fcopy, fcopyreport
        def resourcesUrl
        def projectNameError
        def projectDescriptionError
        def cleanerHistoryPeriodError
        def Properties projProps = new Properties()
        if(params.description) {
            projProps['project.description'] = params.description
        }
        if(params.label) {
            projProps['project.label'] = params.label
        }

        boolean cleanerHistoryEnabled = params.cleanerHistory == 'on'
        projProps['project.execution.history.cleanup.enabled'] = cleanerHistoryEnabled.toString()

        if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true) && cleanerHistoryEnabled) {
            projProps['project.execution.history.cleanup.retention.days'] = params.cleanperiod ?: MAX_DAYS_TO_KEEP.toString()
            projProps['project.execution.history.cleanup.retention.minimum'] = params.minimumtokeep ?: MINIMUM_EXECUTION_TO_KEEP.toString()
            projProps['project.execution.history.cleanup.batch'] = params.maximumdeletionsize ?: MAXIMUM_DELETION_SIZE.toString()
            projProps['project.execution.history.cleanup.schedule'] = params.crontabString ?: SCHEDULE_DEFAULT
        }else{
            projProps['project.execution.history.cleanup.retention.days'] = MAX_DAYS_TO_KEEP.toString()
            projProps['project.execution.history.cleanup.retention.minimum'] = MINIMUM_EXECUTION_TO_KEEP.toString()
            projProps['project.execution.history.cleanup.batch'] = MAXIMUM_DELETION_SIZE.toString()
            projProps['project.execution.history.cleanup.schedule'] = SCHEDULE_DEFAULT
        }
        def errors = []
        def configs
        String defaultNodeExec = configurationService.getString("project.defaults.nodeExecutor", "sshj-ssh")
        String defaultFileCopy = configurationService.getString("project.defaults.filecopier", "sshj-scp")

        if(params.pluginValues?.PluginGroup?.json && params.pluginValues?.PluginGroup?.json != "[]" ){
            def groupData = JSON.parse(params.pluginValues.PluginGroup.json.toString())
            if(groupData instanceof Collection){
                for(Object data: groupData){
                    if(data instanceof Map
                            && data.type instanceof String
                            && data.config instanceof Map) {
                        String type = data.get('type')
                        Map config = data.get('config')
                        projProps.put(
                                "project.PluginGroup.${type}.enabled".toString(),
                                'true'
                        )
                        for (String confKey : config.keySet()) {
                            if(config.get(confKey) != null) {
                                projProps.put(
                                        "project.plugin.PluginGroup.${type}.${confKey}".toString(),
                                        config.get(confKey).toString()
                                )
                            }
                        }
                    }
                }
            }
        }

        if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true) && cleanerHistoryEnabled
                && (params.cleanperiod && Integer.parseInt(params.cleanperiod) <= 0)) {
            cleanerHistoryPeriodError = "Days to keep executions should be greater than zero"
            errors << cleanerHistoryPeriodError
        }

        if (params.default_NodeExecutor) {
            def ndx = 'default'
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
        if (params.default_FileCopier) {
            def ndx = 'default'
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

        //load extra configuration for grails services

        def pconfigurable = frameworkService.validateProjectConfigurableInput(
                params.extraConfig,
                'extraConfig.',
                null
        )

        if (pconfigurable.errors) {
            errors.addAll(pconfigurable.errors)
        }
        Map<String, Map> extraConfig = pconfigurable.config
        projProps.putAll(pconfigurable.props)
        

        if (!project) {
            projectNameError = "Project name is required"
            errors << projectNameError
        } else if (!(project =~ FrameworkResource.VALID_RESOURCE_NAME_REGEX)) {
            projectNameError = message(code: "project.name.can.only.contain.these.characters")
            errors << projectNameError
        } else if (params.description && !(params.description =~ FrameworkResource.VALID_RESOURCE_DESCRIPTION_REGEX)) {
            projectDescriptionError = message(code: "project.description.can.only.contain.these.characters")
            errors << projectDescriptionError
        } else if (projectManager.isFrameworkProjectDisabled(project)) {
            projectNameError = message(code: "project.disabled", args: [project])
            log.error(projectNameError)
            errors << projectNameError
        } else if (projectManager.existsFrameworkProject(project)) {
            projectNameError = message(code: "project.exists", args: [project])
            log.error(projectNameError)
            errors << projectNameError
        } else if (!errors) {
            log.debug("create project, properties: ${projProps}");
            def proj
            (proj, errors)=frameworkService.createFrameworkProject(project,projProps)
            if (!errors && proj) {
                if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true)){
                    frameworkService.scheduleCleanerExecutions(
                        project,
                        ExecutionCleanerConfigImpl.build {
                            enabled(cleanerHistoryEnabled)
                            maxDaysToKeep(
                                FrameworkService.tryParseInt(params.cleanperiod).orElse(-1)
                            )
                            minimumExecutionToKeep(
                                FrameworkService.tryParseInt(params.minimumtokeep).orElse(0)
                            )
                            maximumDeletionSize(
                                FrameworkService.tryParseInt(params.maximumdeletionsize).orElse(500)
                            )
                            cronExpression(params.crontabString?:SCHEDULE_DEFAULT)
                        }
                    )
                }
                frameworkService.refreshSessionProjects(authContext, session)
                flash.message = message(code: "project.0.was.created.flash.message", args: [proj.name])
                return redirect(controller: 'framework', action: 'projectNodeSources', params: [project: proj.name])
            }
        }
        if (errors) {
            request.errors = errors
        }
        //get list of node executor, and file copier services
        def (descriptions, nodeexecdescriptions, filecopydescs) = frameworkService.listDescriptions()

        return render(view:'createProject',
                model: [
                newproject: params.newproject,
                projectDescription: params.description,
                projectLabel: params.label,
                projectNameError: projectNameError,
                projectDescriptionError: projectDescriptionError,
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeApplicationResourceTypeAll(authContext, 'project', [AuthConstants
                        .ACTION_CREATE]),
                AuthConstants.ACTION_CREATE, 'New Project')) {
            return
        }
        final defaultNodeExec = configurationService.getString("project.defaults.nodeExecutor", "sshj-ssh")
        final defaultFileCopy = configurationService.getString("project.defaults.fileCopier", "sshj-scp")
        boolean includeSshKeypath = configurationService.getBoolean("project.defaults.sshKeypath.enabled", false)
        final sshkeypath = new File(System.getProperty("user.home"), ".ssh/id_rsa").getAbsolutePath()
        //get list of node executor, and file copier services

        def (descriptions, nodeexecdescriptions, filecopydescs) = frameworkService.listDescriptions()

        List<Map<String, Object>> pluginGroupConfig = []
        //get grails services that declare project configurations
        Map<String, Map> extraConfig = frameworkService.loadProjectConfigurableInput('extraConfig.', [:])

        return [
            newproject:params.newproject,
            resourceModelConfigDescriptions: descriptions,
            defaultNodeExec:defaultNodeExec,
            nodeexecconfig: includeSshKeypath ? ['keypath': sshkeypath] : [:],
            fcopyconfig: includeSshKeypath ? ['keypath': sshkeypath] : [:],
            defaultFileCopy: defaultFileCopy,
            pluginGroupConfig: pluginGroupConfig,
            nodeExecDescriptions: nodeexecdescriptions,
            fileCopyDescriptions: filecopydescs,
            prefixKey:prefixKey,
            extraConfig:extraConfig,
            cronModelValues: CRON_MODELS_SELECT_VALUES,
            cronValues: [:]
        ]
    }

    private List parseServiceConfigInput(GrailsParameterMap params, String param, ndx, boolean isOriginalValues = false) {
        final nParams = isOriginalValues ? params.orig?."${param}"?."${ndx}" : params."${param}"?."${ndx}"
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
        if (params.cancel) {
            return redirect(controller: 'menu', action: 'index', params: [project: project])
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
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

            def isExecutionDisabledNow = !scheduledExecutionService.isProjectExecutionEnabled(project)
            def isScheduleDisabledNow = !scheduledExecutionService.isProjectScheduledEnabled(project)


            def newExecutionDisabledStatus =
                    (projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION]
                            && projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION] == 'true')
            def newScheduleDisabledStatus =
                    (projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE]
                            && projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true')

            def reschedule = ((isExecutionDisabledNow != newExecutionDisabledStatus)
                    || (isScheduleDisabledNow != newScheduleDisabledStatus))

            final nodeExecType = frameworkService.getDefaultNodeExecutorService(projProps)
            final nodeConfig = frameworkService.getNodeExecConfigurationForType(nodeExecType, projProps)

            //load node exec properties
            def nodeExecService=frameworkService.getNodeExecutorService()

            //restore tracked password values
            try {
                execPasswordFieldsService.untrack(
                        [[config: [type: nodeExecType, props: nodeConfig], index: 0]],
                        nodeexecdescriptions
                )
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                errors << e.getMessage()
            }

            //validate input values
            final nevalidation = frameworkService.validateServiceConfig(nodeExecType, "", nodeConfig, nodeExecService)
            if (!nevalidation.valid) {
                nodeexecreport = nevalidation.report ? frameworkService.remapReportProperties(
                        nevalidation.report,
                        nodeExecType,
                        nodeExecService

                ) : null
                errors << (nevalidation.error ? ("Node Executor configuration was invalid: " + nevalidation.error) :
                        "Node Executor configuration was invalid: " + nodeexecreport?.toString())
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
                fcopyPasswordFieldsService.untrack(
                        [[config: [type: fileCopyType, props: filecopyConfig], index: 0]],
                        filecopydescs
                )
            } catch (ExecutionServiceException e) {
                log.error(e.message)
                errors << e.getMessage()
            }

            //untrack any project level defaults for plugins
            def projectScopedConfigs = frameworkService.discoverScopedConfiguration(projProps, "project.plugin")
            projectScopedConfigs.each { String svcName, Map<String, Map<String, String>> providers ->
                final pluginDescriptions = pluginService.listPluginDescriptions(svcName)
                def pconfigs = []
                providers.each { String provider, Map<String, String> config ->
                    def desc = pluginDescriptions.find { it.name == provider }
                    if (!desc) {
                        return null
                    }
                    def validation = frameworkService.validateDescription(desc, "", config)
                    if (!validation.valid) {
                        Validator.Report report = validation.report
                        errors << (
                                report.errors ?
                                        "${provider} configuration was invalid: " + report.errors :
                                        "${provider} configuration was invalid"
                        )
                    }
                    pconfigs << [type: provider, props: config]
                }

                pconfigs.each { conf ->
                    pluginsPasswordFieldsService.untrack("${project}/${svcName}/defaults/${conf.type}", [[config:conf,index:0,type:conf.type]],  pluginDescriptions)
                    def provprefix = "project.plugin.${svcName}.${conf.type}."
                    conf.props.each { k, v ->
                        projProps["${provprefix}${k}"] = v
                    }
                }
            }


            //validate input values
            final fcvalidation = frameworkService.validateServiceConfig(fileCopyType, "", filecopyConfig, fileCopierService)
            if (!fcvalidation.valid) {
                nodeexecreport = nevalidation.report ? frameworkService.remapReportProperties(
                        nevalidation.report,
                        fileCopyType,
                        fileCopierService

                ) : null
                errors << (fcvalidation.error ? ("File Copier configuration was invalid: " + fcvalidation.error) : "File Copier configuration was invalid: "+nodeexecreport?.toString())
            }else{
                frameworkService.addProjectFileCopierPropertiesForType(fileCopyType, projProps, filecopyConfig)
            }


            Map<String, ProjectPluginListConfigurable> projectConfigListTypes = applicationContext.getBeansOfType(
                    ProjectPluginListConfigurable
            )

            //for each Plugin List configuration type defined, track each configuration entry's password fields
            projectConfigListTypes.each { k, v ->
                if (k.endsWith('Profiled')) {
                    //skip profiled versions of beans
                    return
                }
                def pluginListConfigs = ProjectNodeSupport.listPluginConfigurations(
                        inputMap,
                        v.propertyPrefix,
                        v.serviceName
                )
                def converted = []
                pluginListConfigs.eachWithIndex { pluginconfig, index ->
                    converted << [index: index, config: pluginconfig, type: pluginconfig.provider]
                }
                final pluginDescriptions = pluginService.listPluginDescriptions(v.serviceName)
                obscurePasswordFieldsService.untrack(
                        "${project}/${v.serviceName}/${v.propertyPrefix}",
                        converted,
                        pluginDescriptions
                )
                pluginListConfigs.eachWithIndex { pluginconfig, index ->
                    final String configPrefix = v.propertyPrefix + '.' + (index + 1) + '.config.'
                    pluginconfig.configuration.each { String confk, confv ->
                        projProps[configPrefix + confk] = confv
                    }
                }
            }

            if (!errors) {
                // Password Field Substitution

                def result = frameworkService.setFrameworkProjectConfig(project, projProps)
                if (!result.success) {
                    errors << result.error
                }
                if(reschedule){
                    frameworkService.handleProjectSchedulingEnabledChange(
                            project,
                            isExecutionDisabledNow,
                            isScheduleDisabledNow,
                            newExecutionDisabledStatus,
                            newScheduleDisabledStatus
                    )
                }
            }

            if (!errors) {
                flash.message = "Project ${project} configuration file saved"
                resourcesPasswordFieldsService.reset()
                fcopyPasswordFieldsService.reset()
                pluginsPasswordFieldsService.reset()
                execPasswordFieldsService.reset()
                frameworkService.loadSessionProjectLabel(session, project, projProps['project.label'])
                return redirect(controller: 'framework', action: 'editProjectConfig', params: [project: project])
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
        def cleanerHistoryPeriodError
        def project=params.project
        if (!project) {
            return renderErrorView("Project parameter is required")
        }

        //cancel modification
        if (params.cancel) {
            return redirect(controller: 'menu', action: 'index', params: [project: project])
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
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
            if(params.label){
                projProps['project.label']=params.label
            }else{
                projProps['project.label']=''
            }

            boolean cleanerHistoryEnabled = params.cleanerHistory == 'on'
            projProps['project.execution.history.cleanup.enabled'] = cleanerHistoryEnabled.toString()

            if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true)
                    && cleanerHistoryEnabled && params.cleanperiod && Integer.parseInt(params.cleanperiod) < 0){
                cleanerHistoryPeriodError = "Days to keep executions should be greater or equal to zero"
                errors << cleanerHistoryPeriodError
            }

            if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true) && cleanerHistoryEnabled) {
                projProps['project.execution.history.cleanup.retention.days'] = params.cleanperiod ?: MAX_DAYS_TO_KEEP.toString()
                projProps['project.execution.history.cleanup.retention.minimum'] = params.minimumtokeep ?: MINIMUM_EXECUTION_TO_KEEP.toString()
                projProps['project.execution.history.cleanup.batch'] = params.maximumdeletionsize ?: MAXIMUM_DELETION_SIZE.toString()
                projProps['project.execution.history.cleanup.schedule'] = params.crontabString ?: SCHEDULE_DEFAULT
            }else{
                projProps['project.execution.history.cleanup.retention.days'] = MAX_DAYS_TO_KEEP.toString()
                projProps['project.execution.history.cleanup.retention.minimum'] = MINIMUM_EXECUTION_TO_KEEP.toString()
                projProps['project.execution.history.cleanup.batch'] = MAXIMUM_DELETION_SIZE.toString()
                projProps['project.execution.history.cleanup.schedule'] = SCHEDULE_DEFAULT
            }

            def Set<String> removePrefixes=[]
            Properties defaultServiceProps = new Properties()
            Properties propsChanged = new Properties()
            Properties propsRemoved = new Properties()
            if (params.default_FileCopier) {
                Properties fileCopierProperties = new Properties()
                (defaultFileCopy, fcopy, fcopyreport) = parseDefaultPluginConfig(errors, 'default', "fcopy", frameworkService.getFileCopierService(), "File Copier")
                addProjectFileCopierProperties(defaultFileCopy, fileCopierProperties, fcopy, filecopydescs, removePrefixes)
                defaultServiceProps.putAll(fileCopierProperties)
                (propsChanged, propsRemoved) = extractFileCopierPropertiesChanges(defaultFileCopy, fileCopierProperties, "fcopy", filecopydescs)

            }
            if (params.default_NodeExecutor) {
                Properties nodeExecutorProps = new Properties()
                Properties changed, removed
                (defaultNodeExec, nodeexec, nodeexecreport) = parseDefaultPluginConfig(errors, 'default', "nodeexec", frameworkService.getNodeExecutorService(), "Node Executor")
                addProjectNodeExecutorProperties(defaultNodeExec, nodeExecutorProps, nodeexec, nodeexecdescriptions, removePrefixes)
                defaultServiceProps.putAll(nodeExecutorProps)
                (changed, removed) = extractNodeExecutorPropertiesChanges(defaultNodeExec, nodeExecutorProps, "nodeexec", nodeexecdescriptions)
                propsChanged.putAll(changed)
                propsRemoved.putAll(removed)
            }

            defaultServiceProps.putAll(propsChanged)
            defaultServiceProps.removeAll {propsRemoved.containsKey(it.key)}

            projProps.putAll(defaultServiceProps)


            //load extra configuration for grails services
            def pconfigurable = frameworkService.validateProjectConfigurableInput(
                    params.extraConfig,
                    'extraConfig.',
                    { String category -> category != 'resourceModelSource' }
            )
            if (pconfigurable.errors) {
                errors.addAll(pconfigurable.errors)
            }
            extraConfig = pconfigurable.config
            if (pconfigurable.props) {
                projProps.putAll(pconfigurable.props)
            }
            if (pconfigurable.remove) {
                removePrefixes.addAll(pconfigurable.remove)
            }

            def isExecutionDisabledNow = !scheduledExecutionService.isProjectExecutionEnabled(project)
            def isScheduleDisabledNow = !scheduledExecutionService.isProjectScheduledEnabled(project)

            def newExecutionDisabledStatus =
                    projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_EXECUTION] == 'true'
            def newScheduleDisabledStatus =
                    projProps[ScheduledExecutionService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            def reschedule = ((isExecutionDisabledNow != newExecutionDisabledStatus)
                    || (isScheduleDisabledNow != newScheduleDisabledStatus))

            if(featureService.featurePresent(Features.PLUGIN_GROUPS)) {
                List<Description> pluginGroupDescs = frameworkService.listPluginGroupDescriptions()
                //specific props for typed pluginValues
                removePrefixes.add("project.plugin.PluginGroup.".toString())
                removePrefixes.add("project.PluginGroup.".toString())
                if (params.pluginValues?.PluginGroup?.json) {
                    def groupData = JSON.parse(params.pluginValues.PluginGroup.json.toString())
                    if (groupData instanceof Collection) {
                        for (Object data : groupData) {
                            if (data instanceof Map
                                && data.type instanceof String
                                && data.config instanceof Map) {
                                String type = data.get('type')
                                Map config = data.get('config')
                                pluginGroupPasswordFieldsService.untrack(
                                    [[config: [type: type, props: config], type: type, index: 0]],
                                    pluginGroupDescs
                                )
                                projProps.put(
                                        "project.PluginGroup.${type}.enabled".toString(),
                                        'true'
                                )
                                for (String confKey : config.keySet()) {
                                    if(config.get(confKey) != null) {
                                        projProps.put(
                                                "project.plugin.PluginGroup.${type}.${confKey}".toString(),
                                                config.get(confKey).toString()
                                        )
                                    }

                                }
                            }
                        }
                    }
                }
            }

            //validate props for other plugins
            def projectScopedConfigs = frameworkService.discoverScopedConfiguration(projProps, "project.plugin")
            projectScopedConfigs.each { String svcName, Map<String, Map<String, String>> providers ->
                final pluginDescriptions = pluginService.listPluginDescriptions(svcName)
                providers.each { String provider, Map<String, String> config ->
                    def desc = pluginDescriptions.find { it.name == provider }
                    if (desc) {
                        def validation = frameworkService.validateDescription(desc, "", config)
                        if (!validation.valid) {
                            Validator.Report report = validation.report
                            errors << (
                                    report.errors ?
                                            "${provider} configuration was invalid: " + report.errors :
                                            "${provider} configuration was invalid"
                            )
                        }
                    }
                }
            }
            if (!errors) {

                def result = frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)
                if(reschedule){
                    frameworkService.handleProjectSchedulingEnabledChange(
                            project,
                            isExecutionDisabledNow,
                            isScheduleDisabledNow,
                            newExecutionDisabledStatus,
                            newScheduleDisabledStatus
                    )
                }
                if (!result.success) {
                    errors << result.error
                }
            }

            if (!errors) {
                flash.message = "Project ${project} saved"

                fcopyPasswordFieldsService.reset()
                execPasswordFieldsService.reset()
                pluginGroupPasswordFieldsService.reset()
                if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY, true)){
                    frameworkService.scheduleCleanerExecutions(
                        project,
                        ExecutionCleanerConfigImpl.build {
                            enabled(cleanerHistoryEnabled)
                            maxDaysToKeep(
                                FrameworkService.tryParseInt(params.cleanperiod).orElse(MAX_DAYS_TO_KEEP)
                            )
                            minimumExecutionToKeep(
                                FrameworkService.tryParseInt(params.minimumtokeep).orElse(MINIMUM_EXECUTION_TO_KEEP)
                            )
                            maximumDeletionSize(
                                FrameworkService.tryParseInt(params.maximumdeletionsize).orElse(MAXIMUM_DELETION_SIZE)
                            )
                            cronExpression(params.crontabString ?: SCHEDULE_DEFAULT)
                        }
                    )
                }
                frameworkService.refreshSessionProjects(authContext, session)
                frameworkService.loadSessionProjectLabel(session, project, projProps['project.label'])
                return redirect(controller: 'menu', action: 'index', params: [project: project])
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

    private List extractFileCopierPropertiesChanges(String serviceType, Properties projProps, String identifier, descs){
        Properties projPropsOrig = new Properties()
        def (type, configOrig) = parseServiceConfigInput(params, identifier, "default", true)
        addProjectFileCopierProperties(serviceType, projPropsOrig, configOrig, descs)

        def changed = projProps.findAll {projPropsOrig.get(it.key) != it.value}
        def removed = projPropsOrig.findAll {!projProps.keySet().contains(it.key)}
        [changed, removed]
    }

    private List extractNodeExecutorPropertiesChanges(String serviceType, Properties projProps, String identifier, descs){
        Properties projPropsOrig = new Properties()
        def (type, configOrig) = parseServiceConfigInput(params, identifier, "default", true)
        addProjectNodeExecutorProperties(serviceType, projPropsOrig, configOrig, descs)

        def changed = projProps.findAll {projPropsOrig.get(it.key) != it.value}
        def removed = projPropsOrig.findAll {!projProps.keySet().contains(it.key)}
        [changed, removed]
    }

    private addProjectFileCopierProperties(String type, Properties projectProps, config, descs, Set removePrefixes = null){
        try {
            execPasswordFieldsService.untrack(
                    [[config: [type: type, props: config], index: 0]],
                    descs
            )
            frameworkService.addProjectFileCopierPropertiesForType(type, projectProps, config, removePrefixes)
        } catch (ExecutionServiceException e) {
            log.error(e.message)
            errors << e.getMessage()
        }
    }
    private addProjectNodeExecutorProperties(String type, Properties projectProps, config, descs, Set removePrefixes = null){
        try {
            execPasswordFieldsService.untrack(
                    [[config: [type: type, props: config], index: 0]],
                    descs
            )
            frameworkService.addProjectNodeExecutorPropertiesForType(type, projectProps, config, removePrefixes)
        } catch (ExecutionServiceException e) {
            log.error(e.message)
            errors << e.getMessage()
        }
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
                errors << (
                        validation.error ?
                        "${title} configuration was invalid: " + validation.error :
                        "${title} configuration was invalid"
                )
            }
        }
        [type, config, report]
    }

    def deleteProjectNodesource() {
        if (!requestHasValidToken()) {
            return
        }
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }
        if (!params.index) {
            return renderErrorView("Index parameter is required")
        }

        int index = params.index.toInteger()
        def project = params.project


        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }


        final def fwkProject = frameworkService.getFrameworkProject(project)
        //get list of model source configes
        final resourceConfig = frameworkService.listResourceModelConfigurations(project)
        if (index < 1 || index > resourceConfig.size()) {
            //invalid
            flash.errors = ['Invalid index: ' + index]
            log.error(flash.errors)
            return redirect(action: 'projectNodeSources', params: [project: project])
        }
        resourceConfig.remove(index - 1)
        Properties projProps = ProjectNodeSupport.serializeResourceModelConfigurations(resourceConfig)

        log.error("Setting project props:  " + projProps)


        def result = frameworkService.updateFrameworkProjectConfig(
                project,
                projProps,
                [FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX] as Set
        )
        if (!result.success) {
            log.error(result.error)
            flash.errors = [result.error]
        } else {
            flash.message = 'Removed Node Source'
        }
        return redirect(action: 'projectNodeSources', params: [project: project])
    }

    def projectPluginsAjax(String project, String serviceName, String configPrefix) {
        if (!project) {
            return renderErrorView("Project parameter is required")
        }
        if (!serviceName) {
            return renderErrorView("serviceName parameter is required")
        }
        if (!configPrefix) {
            return renderErrorView("configPrefix parameter is required")
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        def framework = frameworkService.getRundeckFramework()
        def rdprojectconfig = framework.getFrameworkProjectMgr().loadProjectConfig(project)
        def plugins = ProjectNodeSupport.listPluginConfigurations(
                rdprojectconfig.projectProperties,
                configPrefix,
                serviceName,
                true
        )
        final pluginDescriptions = pluginService.listPluginDescriptions(serviceName)

        obscurePasswordFieldsService.resetTrack("${project}/${serviceName}/${configPrefix}", plugins, pluginDescriptions)

        respond(
                formats: ['json'],
                [
                        project: project,
                        plugins: plugins.collect { ExtPluginConfiguration conf ->
                            [type: conf.provider, config: conf.configuration, service: conf.service, extra: conf.extra]
                        },
                ]
        )
    }

    def saveProjectPluginsAjax(String project, String serviceName, String configPrefix) {
        boolean valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            return apiService.renderErrorFormat(
                    response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'request.error.invalidtoken.message',
            ]
            )
        }
        if (!valid) {
            return
        }

        if (request.format != 'json') {
            return apiService.renderErrorFormat(
                    response, [
                    status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code  : 'api.error.item.unsupported-format',
                    args  : [request.format]
            ]
            )
        }

        if (!project) {
            return renderErrorView("Project parameter is required")
        }
        if (!serviceName) {
            return renderErrorView("serviceName parameter is required")
        }
        if (!configPrefix) {
            return renderErrorView("configPrefix parameter is required")
        }
        def plugins = request.JSON.plugins
        List removedPlugins = request.JSON.removedPlugins as List

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        if(removedPlugins)
            notifyRemovedPlugins(project, removedPlugins)

        def errors = []
        def reports = [:]
        List<ExtPluginConfiguration> configs = []

        def mappedConfigs = []
        plugins.eachWithIndex { pluginDef, int ndx ->
            def confData = new HashMap<>(pluginDef.config ?: [:])
            mappedConfigs << [
                    type    : pluginDef.type,
                    props   : confData,
                    config  : [props: confData, type: pluginDef.type],
                    extra   : pluginDef.extra,
                    index   : pluginDef.origIndex,
                    newIndex: ndx
            ]
        }
        final pluginDescriptions = pluginService.listPluginDescriptions(serviceName)

        //replace obscured values with original values
        obscurePasswordFieldsService.untrack(
                "${project}/${serviceName}/${configPrefix}",
                mappedConfigs,
                pluginDescriptions
        )

        mappedConfigs.eachWithIndex { pluginDef, int ndx ->

            String type = pluginDef.type
            if (!type) {
                errors << "[$ndx]: missing type"
                return
            }

            if (!(type =~ /^[-_a-zA-Z0-9+][-\._a-zA-Z0-9+]*\u0024/)) {
                errors << "[$ndx]: Invalid provider type name"
                return
            }

            def described = pluginService.getPluginDescriptor(type, serviceName)
            if (!described) {
                errors << "[$ndx]: $serviceName provider was not found: ${type}"
                return
            }

            //validate
            Map<String, Object> configMap = pluginDef.props
            ValidatedPlugin validated = pluginService.validatePluginConfig(serviceName, type, configMap)
            if (!validated.valid) {
                errors << "[$ndx]: configuration was invalid: $validated.report"
                reports["$ndx"] = validated.report.errors
                return
            }
            Map<String, Object> extraMap = pluginDef.extra ?: [:]


            configs <<  SimplePluginConfiguration.builder()
                    .service(serviceName)
                    .provider(type)
                    .configuration(configMap)
                    .extra(extraMap)
                    .build()
        }

        if (!errors) {

            Properties projProps = ProjectNodeSupport.serializePluginConfigurations(configPrefix, configs, true)
            def result = frameworkService.updateFrameworkProjectConfig(project, projProps, [configPrefix+'.'].toSet())
            if (!result.success) {
                errors << result.error
            }
        }

        if (errors) {
            return respond(
                    formats: ['json'],
                    status: HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    [
                            errors : errors,
                            reports: reports
                    ]
            )
        }


        obscurePasswordFieldsService.resetTrack(
                "${project}/${serviceName}/${configPrefix}",
                configs,
                pluginDescriptions
        )

        respond(
                formats: ['json'],
                [
                        project: project,
                        plugins: configs.collect { ExtPluginConfiguration conf ->
                            [type: conf.provider, config: conf.configuration, service: conf.service, extra: conf.extra]
                        },
                ]
        )
    }

    def projectNodeSources() {
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }

        def project = params.project
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)

        Map<String, Map> extraConfig = frameworkService.loadProjectConfigurableInput(
                'extraConfig.',
                fwkProject.projectProperties,
                'resourceModelSource'
        )
        final writeableModelSources = frameworkService.listWriteableResourceModelSources(project)

        def parseExceptions = fwkProject.projectNodes.getResourceModelSourceExceptionsMap()

        [
                project           : project,
                projectDescription: fwkProject.getProjectProperties().get("project.description"),
                prefixKey         : 'plugin',
                extraConfig       : extraConfig,
                parseExceptions   : parseExceptions,
                writeableSources  : writeableModelSources,
        ]
    }

    @Hidden
    @Get(uri='/project/{project}/configurable')
    @Operation(
            method = 'GET',
            summary = 'Get Project Configurations Using Mapping defined in ProjectConfigurable beans',
            description = 'Get Project Configurable configs and properties.',
            tags = ['project', 'configuration'],
            responses = [
                    @ApiResponse(
                            responseCode = '200',
                            description = '''All configs were successfully saved or updated. A payload reflecting save or creation status is returned. `restart` will indicate if the server must be restarted for some changes to take effect.''',
                            content = @Content(
                                    mediaType= io.micronaut.http.MediaType.APPLICATION_JSON,
                                    examples = @ExampleObject('''
{
                        "project": "projectName",
                        "projectConfigurable": [
                           "name": "beanName",
                           "properties": {
                                [
                                  "name": "property name",
                                  "type": "property type",
                                  "description": "property description",
                                  "required": "true/false",
                                  "default": "default value",
                                  "values": "list of values"
                               ]
                           },
                           propertiesMapping: {
                                "enabled": "project.healthcheck.enabled",
                                "onstartup": "project.healthcheck.onstartup",
                                "delay": "project.healthcheck.delay"
                           },
                           values: {
                                "enabled": "true",
                                "onstartup": "true",
                                "delay": "0"
                           },
                        ]
}''')
                            )
                    ),
                    @ApiResponse(responseCode = '400', description = 'Bad request'),
                    @ApiResponse(responseCode = '403', description = 'Unauthorized response')
            ],
            operationId = 'GetProjectConfigurable'
    )
    def getProjectConfigurable() {
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }

        def project = params.project
        String category = params.category as String
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
                AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)

        Map<String, Map> extraConfig = frameworkService.loadProjectConfigurableInput(
                'extraConfig.',
                fwkProject.projectProperties,
                category
        )
        def propertyConfig =[]
        for (entry in extraConfig) {
            propertyConfig.add([
                    name: entry.key,
                    properties: entry.value["propertyList"],
                    propertiesMapping: entry.value["mapping"],
                    values: entry.value["values"],
            ])
        }

        respond(
                formats: ['json'],
                [
                    project                  : project,
                    projectConfigurable      : propertyConfig
                ]
        )
    }

    @Hidden
    @Post(uri='/project/{project}/configurable')
    @Operation(
            method = 'POST',
            summary = 'Create or Update Configurations Using Mapping defined in ProjectConfigurable beans',
            description = 'Create or update configs and properties.',
            tags = ['project', 'configuration'],
            requestBody = @RequestBody(
                    required = true,
                    description = '''Update Config Request.
List of config values, each value contains:

* `extraConfig` Required
  * Represents either a new config to be created, or an existing config to be updated.
  * Accepts: An object of projectConfigurable bean names and their respective properties.
''',
                    content = @Content(
                            mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(type = 'object')),
                            examples = @ExampleObject('''[
  {
  "extraConfig": {
      "nodeService": {
            "enabled": "true",
            "onstartup": "true",
            "delay": "0"
      },
      "rundeckproHealthChecker": {
          "enabled": "true",
          "onstartup": "true",
          "delay": "0"
      }
  }
  }
]''')
                    )
            ),
            responses = [
                    @ApiResponse(
                            responseCode = '200',
                            description = '''All configs were successfully saved or updated. A payload reflecting save or creation status is returned. `restart` will indicate if the server must be restarted for some changes to take effect.''',
                            content = @Content(
                                    mediaType= io.micronaut.http.MediaType.APPLICATION_JSON,
                                    examples = @ExampleObject('''
{
                        "result": {
                            "success": true,
                            "restart": false
                        },
                        "errors": [
                          "error message",
                          "error message"
                        ]
}''')
                            )
                    ),
                    @ApiResponse(responseCode = '400', description = 'Bad request'),
                    @ApiResponse(responseCode = '403', description = 'Unauthorized response')
            ],
            operationId = 'SaveProjectConfigurable'
    )
    def saveProjectConfigurable(){
        def project = params.project
        def category = params.category
        def cfgPayload = request.JSON

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
                AuthConstants.ACTION_CONFIGURE, 'Project', project)) {
            return
        }

        def errors = []
        //only attempt project create if form POST is used

        def Set<String> removePrefixes = []


        def pconfigurable = frameworkService.validateProjectConfigurableInput(
                cfgPayload.extraConfig,
                'extraConfig.',
                { String it -> it == category }
        )
        if (pconfigurable.errors) {
            errors.addAll(pconfigurable.errors)
        }

        def projProps = new Properties()
        projProps.putAll(pconfigurable.props)
        removePrefixes.addAll(pconfigurable.remove)
        def result = [success: false]
        if (!errors) {
            result = frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)
            if (!result.success) {
                errors << result.error
            }
        }

        respond(
                formats: ['json'],
                [
                        result                   : result,
                        errors                   : errors
                ]
        )
    }

    def saveProjectNodeSources() {

        if (!requestHasValidToken()) {
            return
        }

        def project = params.project
        if (!project) {
            return renderErrorView("Project parameter is required")
        }

        //cancel modification
        if (params.cancel) {
            return redirect(controller: 'framework', action: 'projectNodeSources', params: [project: project])
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        def framework = frameworkService.getRundeckFramework()

        def prefixKey = 'plugin'
        def errors = []
        //only attempt project create if form POST is used

        def Set<String> removePrefixes = []


        def pconfigurable = frameworkService.validateProjectConfigurableInput(
                params.extraConfig,
                'extraConfig.',
                { String category -> category == 'resourceModelSource' }
        )
        if (pconfigurable.errors) {
            errors.addAll(pconfigurable.errors)
        }
        Map<String, Map> extraConfig = pconfigurable.config
        def projProps = new Properties()
        projProps.putAll(pconfigurable.props)
        removePrefixes.addAll(pconfigurable.remove)

        if (!errors) {

            def result = frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)
            if (!result.success) {
                errors << result.error
            }
        }

        if (!errors) {
            flash.message = "Project ${project} Node Sources saved"

            return redirect(controller: 'framework', action: 'projectNodeSources', params: [project: project])
        }
        if (errors) {
            request.errors = errors
        }


        return render(
                view: 'projectNodeSources', model:
                [
                        project    : params.project,
                        newproject : params.newproject,
                        prefixKey  : prefixKey,
                        extraConfig: extraConfig
                ]
        )
    }

    def editProjectNodeSources() {
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }

        def project = params.project
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)

        Map<String, Map> extraConfig = frameworkService.loadProjectConfigurableInput(
                'extraConfig.',
                fwkProject.projectProperties,
                'resourceModelSource'
        )
        [
                project           : project,
                projectDescription: fwkProject.getProjectProperties().get("project.description"),
                prefixKey         : 'plugin',
                extraConfig       : extraConfig
        ]
    }

    static final Map<String, String> Formats = [
            'text/xml'        : 'xml',
            'application/xml' : 'xml',
            'application/yaml': 'yaml',
            'text/yaml'       : 'yaml',
            'application/json': 'json',
    ]

    def editProjectNodeSourceFile() {
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }
        if (!params.index) {
            return renderErrorView("Index parameter is required")
        }

        int index = params.index.toInteger()
        def project = params.project


        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }
        final def fwkProject = frameworkService.getFrameworkProject(project)
        //get list of model source configes
        final writableSources = fwkProject.projectNodes.writeableResourceModelSources
        final source = writableSources.find { it.index == index }

        if (!source) {
            //invalid
            flash.errors = ['Invalid index: ' + index]
            log.error(flash.errors)
            return redirect(action: 'projectNodeSources', params: [project: project])
        }


        def baos = new ByteArrayOutputStream()
        def emptydata = false
        if (source.writeableSource.hasData()) {
            source.writeableSource.readData(baos)
        } else {
            emptydata = true
        }
        def fileText = baos.toString('UTF-8')
        def modelFormat = source.writeableSource.syntaxMimeType
        def sourceDesc = source.writeableSource.sourceDescription
        def providerType = source.type;
        def desc = pluginService.getPluginDescriptor(
            providerType,
            frameworkService.rundeckFramework.getResourceModelSourceService()
        )?.description

        [
                project     : project,
                index       : index,
                fileText    : fileText,
                fileEmpty   : emptydata,
                fileFormat  : modelFormat ? (Formats[modelFormat] ?: modelFormat) : '',
                sourceDesc  : sourceDesc,
                providerType: providerType,
                providerDesc: desc
        ]
    }

    def saveProjectNodeSourceFile() {
        if (!requestHasValidToken()) {
            return
        }
        if (!params.project) {
            return renderErrorView("Project parameter is required")
        }
        if (!params.index) {
            return renderErrorView("Index parameter is required")
        }
        if (null == params.fileText) {
            return renderErrorView("fileText parameter is required")
        }

        def project = params.project
        def index = params.index.toInteger()

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        //cancel modification
        if (params.cancel) {
            return redirect(controller: 'framework', action: 'projectNodeSources', params: [project: project])
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        final writableSources = fwkProject.projectNodes.writeableResourceModelSources
        final source = writableSources.find { it.index == index }

        if (!source) {
            //invalid
            flash.errors = ['Invalid index: ' + index]
            log.error(flash.errors)
            return redirect(action: 'projectNodeSources', params: [project: project])
        }
        def format = source.writeableSource.syntaxMimeType
        //validate


        def bais = new ByteArrayInputStream(params.fileText.toString().getBytes("UTF-8"))
        long size = -1
        def error = null
        try {
            size = source.writeableSource.writeData(bais)
        } catch (Exception exc) {

            log.error('Error Saving nodes file content', exc)
            error = exc
        }
        if (!error) {
            flash.message = "Saved nodes content: $size bytes"
            return redirect(
                    controller: 'framework',
                    action: 'projectNodeSources',
                    params: [project: project]
            )
        }else{
            flash.error = message(code: "archive.import.importNodesSource.failed.message")
            return redirect(
                    controller: 'framework',
                    action: 'projectNodeSources',
                    params: [project: project]
            )
        }
        def modelFormat = source.writeableSource.syntaxMimeType
        def sourceDesc = source.writeableSource.sourceDescription
        def providerType = source.type;
        def desc = pluginService.getPluginDescriptor(
            providerType,
            frameworkService.rundeckFramework.getResourceModelSourceService()
        )?.description
        return render(
                view: 'editProjectNodeSourceFile',
                model: [
                        project     : project,
                        index       : index,
                        fileText    : params.fileText,
                        fileFormat  : modelFormat ? (Formats[modelFormat] ?: modelFormat) : '',
                        sourceDesc  : sourceDesc,
                        providerType: providerType,
                        providerDesc: desc,
                        saveError   : error.message
                ]
        )


    }
    def editProject (){
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }

        def project = params.project

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        final def projectDescription = fwkProject.info?.description

        final def (resourceDescs, execDesc, filecopyDesc) = frameworkService.listDescriptions()


        //get list of node executor, and file copier services

        final defaultNodeExec = frameworkService.getDefaultNodeExecutorService(project)
        final defaultFileCopy = frameworkService.getDefaultFileCopyService(project)

        final nodeConfig = frameworkService.getNodeExecConfigurationForType(defaultNodeExec, project)
        final filecopyConfig = frameworkService.getFileCopyConfigurationForType(defaultFileCopy, project)

        def errors = []

        if(defaultNodeExec!=null) {
            def nodeExecDescription = pluginService.getPluginDescriptor(defaultNodeExec, ServiceNameConstants.NodeExecutor)
            if(!nodeExecDescription){
                errors << message(code: "domain.project.edit.plugin.missing.message", args: ['Node Executor', defaultNodeExec])
            }
        }

        if(defaultFileCopy != null) {
            def fcopyDescription = pluginService.getPluginDescriptor(defaultFileCopy, ServiceNameConstants.FileCopier)
            if(!fcopyDescription){
                errors << message(code: "domain.project.edit.plugin.missing.message", args: ['File Copier', defaultFileCopy])
            }
        }

        if(errors?.size() > 0) {
            request.errors = errors
        }

        // Reset Password Fields in Session
        execPasswordFieldsService.reset()
        fcopyPasswordFieldsService.reset()
        pluginGroupPasswordFieldsService.reset()
        // Store Password Fields values in Session
        // Replace the Password Fields in configs with hashes
        execPasswordFieldsService.track([[type: defaultNodeExec, props: nodeConfig]], execDesc)
        fcopyPasswordFieldsService.track([[type: defaultFileCopy, props: filecopyConfig]], filecopyDesc)
        List<Map<String, Object>> pluginGroupConfig = []
        if(featureService.featurePresent(Features.PLUGIN_GROUPS)) {
          final fproject = frameworkService.getFrameworkProject(project)
          def projectProps = fproject.getProjectProperties()
            List<Description> pluginGroupDescs = frameworkService.listPluginGroupDescriptions()
            pluginGroupDescs.each {
                if (frameworkService.hasPluginGroupConfigurationForType(it.name, projectProps)) {
                    Map<String, String> providerConfig = frameworkService.getPluginGroupConfigurationForType(it.name, project)
                    pluginGroupPasswordFieldsService
                        .track([[type: it.name, props: providerConfig]], true, pluginGroupDescs)
                    pluginGroupConfig.add([type: it.name, config: providerConfig])
                }
            }
        }
        // resourceConfig CRUD rely on this session mapping
        // saveProject will replace the password fields on change

        //get grails services that declare project configurations
        Map<String, Map> extraConfig = frameworkService.loadProjectConfigurableInput(
                'extraConfig.',
                fwkProject.projectProperties
        )
        //sort the beans in order to control the way they are shown on the form
        extraConfig = extraConfig.sort { it.key.toLowerCase() }
        [
            project: project,
            projectDescription:projectDescription?:fwkProject.getProjectProperties().get("project.description"),
            projectLabel:fwkProject.getProjectProperties().get("project.label"),
            cleanerHistoryPeriod:fwkProject.getProjectProperties().get("project.execution.history.cleanup.retention.days") ?: MAX_DAYS_TO_KEEP,
            minimumExecutionToKeep:fwkProject.getProjectProperties().get("project.execution.history.cleanup.retention.minimum") ?: MINIMUM_EXECUTION_TO_KEEP,
            maximumDeletionSize:fwkProject.getProjectProperties().get("project.execution.history.cleanup.batch") ?: MAXIMUM_DELETION_SIZE,
            enableCleanHistory:["true", true].contains(fwkProject.getProjectProperties().get("project.execution.history.cleanup.enabled")),
            cronExression:fwkProject.getProjectProperties().get("project.execution.history.cleanup.schedule") ?: SCHEDULE_DEFAULT,
            nodeexecconfig:nodeConfig,
            fcopyconfig:filecopyConfig,
            pluginGroupConfig: pluginGroupConfig,
            defaultNodeExec: defaultNodeExec,
            defaultFileCopy: defaultFileCopy,
            nodeExecDescriptions: execDesc,
            fileCopyDescriptions: filecopyDesc,
            prefixKey: 'plugin',
            extraConfig:extraConfig,
            cronModelValues: CRON_MODELS_SELECT_VALUES,
            cronValues: [:]
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

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def fwkProject = frameworkService.getFrameworkProject(project)
        def fileText=''
        if(fwkProject.existsFileResource(params.filename)){
            def baos=new ByteArrayOutputStream()
            fwkProject.loadFileResource(params.filename,baos)
            fileText=baos.toString('UTF-8')
        }

        def displayConfig
        if (params.filename == 'readme.md') {
            displayConfig = menuService.getReadmeDisplay(fwkProject)
        } else if (params.filename == 'motd.md') {
            displayConfig = menuService.getMotdDisplay(fwkProject)
        }

        [
                displayConfig: displayConfig,
                filename     : params.filename,
                fileText     : fileText
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

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        //cancel modification
        if (params.cancel) {
            return redirect(controller: 'menu', action: 'index', params: [project: project])
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

        return redirect(
                controller: 'framework',
                action: 'editProjectFile',
                params: [project: project, filename: params.filename]
        )
    }
    def editProjectConfig (){
        if(!params.project){
            return renderErrorView("Project parameter is required")
        }

        def project = params.project

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }

        final def framework = frameworkService.getRundeckFramework()
        final def fwkProject = frameworkService.getFrameworkProject(project)
        final def (resourceDescs, execDesc, filecopyDesc) = frameworkService.listDescriptions()

        def projectPropsMap = fwkProject.getProjectProperties()
        def projectProps = projectPropsMap as Properties

        //get list of node executor, and file copier services

        final defaultNodeExec = frameworkService.getDefaultNodeExecutorService(projectProps)
        final defaultFileCopy = frameworkService.getDefaultFileCopyService(projectProps)

        final nodeConfig = frameworkService.getNodeExecConfigurationForType(defaultNodeExec, projectProps)
        final filecopyConfig = frameworkService.getFileCopyConfigurationForType(defaultFileCopy, projectProps)


        // Reset Password Fields in Session
        resourcesPasswordFieldsService.reset()
        execPasswordFieldsService.reset()
        fcopyPasswordFieldsService.reset()
        obscurePasswordFieldsService.reset('_')
        // Store Password Fields values in Session
        // Replace the Password Fields in configs with hashes


        if(defaultNodeExec) {
            execPasswordFieldsService.track([[type: defaultNodeExec, props: nodeConfig]], true, execDesc)
        }
        if(defaultFileCopy) {
            fcopyPasswordFieldsService.track([[type: defaultFileCopy, props: filecopyConfig]], true, filecopyDesc)
        }
        // resourceConfig CRUD rely on this session mapping
        // saveProject will replace the password fields on change
        //replace password values with hashed values
        if(defaultFileCopy) {
            frameworkService.addProjectFileCopierPropertiesForType(defaultFileCopy, projectProps, filecopyConfig)
        }
        if(defaultNodeExec) {
            frameworkService.addProjectNodeExecutorPropertiesForType(defaultNodeExec, projectProps, nodeConfig)
        }


        Map<String, ProjectPluginListConfigurable> projectConfigListTypes = applicationContext.getBeansOfType(
                ProjectPluginListConfigurable
        )

        //for each Plugin List configuration type defined, track each configuration entry's password fields
        projectConfigListTypes.each { k, v ->
            if (k.endsWith('Profiled')) {
                //skip profiled versions of beans
                return
            }

            def configs = ProjectNodeSupport.listPluginConfigurations(projectPropsMap, v.propertyPrefix, v.serviceName)
            final pluginDescriptions = pluginService.listPluginDescriptions(v.serviceName)
            obscurePasswordFieldsService.resetTrack(
                    "${project}/${v.serviceName}/${v.propertyPrefix}",
                    configs,
                    true,
                    pluginDescriptions
            )
            configs.eachWithIndex { pluginconfig, index ->
                final String configPrefix = v.propertyPrefix + '.' + (index + 1) + '.config.'
                final String typeProp = v.propertyPrefix + '.' + (index + 1) + '.type'

                projectProps[typeProp] = pluginconfig.provider
                pluginconfig.configuration.each { String confk, confv ->
                    projectProps[configPrefix + confk] = confv
                }
            }
        }


        // track project plugin default attributes for any discovered plugin types configured at project level
        def projectScopedConfigs = frameworkService.discoverScopedConfiguration(projectProps, "project.plugin")
        projectScopedConfigs.each { String svcName, Map<String, Map<String, String>> providers ->
            final pluginDescriptions = pluginService.listPluginDescriptions(svcName)
            def configs = []
            providers.each { String provider, Map<String, String> config ->
                def desc = pluginDescriptions.find { it.name == provider }
                if (!desc) {
                    log.warn("Not found provider: ${svcName}/${provider}")
                    return null
                }
                configs << [type: provider, props: config]
            }
            configs.each { conf ->
                pluginsPasswordFieldsService.reset("${project}/${svcName}/defaults/${conf.type}")
                pluginsPasswordFieldsService.track("${project}/${svcName}/defaults/${conf.type}", [conf], true, pluginDescriptions)
                def provprefix = "project.plugin.${svcName}.${conf.type}."
                conf.props.each { k, v ->
                    projectProps["${provprefix}${k}"] = v
                }
            }
        }

        def sw=new StringWriter()
        projectProps.store(sw,"edit below")
        def projectPropertiesText = sw.toString().
                split(Pattern.quote(System.getProperty("line.separator"))).
                sort().
                join(System.getProperty("line.separator"))
        [
                project              : project,
                projectDescription   : projectPropsMap.get("project.description"),
                projectPropertiesText:projectPropertiesText,
                prefixKey            : 'plugin'
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
        def described = pluginService.getPluginDescriptor(
            params.type,
            framework.getResourceModelSourceService()
        )

        if (described && described.description) {
            def desc = described.description
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
            def validated = pluginService.validatePluginConfig(
                type,
                framework.getResourceModelSourceService(),
                selectParamsPrefix(prefix + 'config.', params)
            )
            result.valid = validated?.valid
            result.error = !validated?.valid ? validated?.report?.toString() : null
        }
        render result as JSON
    }

    private Map selectParamsPrefix(final String prefix, final Map params) {
        params.findAll { it.key.startsWith(prefix) }.collectEntries { [it.key.substring(prefix.length()), it.value] }
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
        def props
        def report
        def desc
        if (!type) {
            error = "Plugin provider type must be specified"
        } else {
            props = selectParamsPrefix(prefix + 'config.', params)
            def descriptor = pluginService.getPluginDescriptor(type, framework.getResourceModelSourceService())
            def validated = pluginService.validatePluginConfig(
                type,
                framework.getResourceModelSourceService(),
                props
            )
            error = !validated ? 'No such plugin: ' + type : null
            desc = descriptor.description
            props = props
            report = validated?.report
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
            props = selectParamsPrefix(useprefix + 'config.', params)
            def descriptor = pluginService.getPluginDescriptor(type, framework.getResourceModelSourceService())
            def validated = pluginService.validatePluginConfig(
                type,
                framework.getResourceModelSourceService(),
                props
            )
            error = !validated ? 'No such plugin: ' + type : null
            desc = descriptor.description
            props = props
            report = validated?.report
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def project = params.project
        if (!project) {
            return renderErrorFragment("Project parameter is required")
        }
        if(notFoundResponse(frameworkService.existsFrameworkProject(project),'Project',project,true)){
            return
        }
        if(unauthorizedResponse(rundeckAuthContextProcessor.authorizeApplicationResource(authContext,
                rundeckAuthContextProcessor.authResourceForProject(project), AuthConstants.ACTION_READ),
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def project=params.project
        if (!project) {
            return renderErrorFragment("Project parameter is required")
        }
        if (notFoundResponse(frameworkService.existsFrameworkProject(project), 'Project', project, true)) {
            return
        }
        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeApplicationResource(authContext,
                rundeckAuthContextProcessor.authResourceForProject(project), AuthConstants.ACTION_READ),
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
                jobCreate: rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_JOB,
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def projects
        if(session.frameworkProjects){
            projects=session.frameworkProjects
        }else{
            projects = frameworkService.projectNames(authContext)
            session.frameworkProjects=projects
        }
        if(!session.frameworkLabels){
            def flabels = frameworkService.projectLabels(authContext)
            session.frameworkLabels = flabels
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

    @Get(uri='/project/{project}/sources')
    @Operation(
        method='GET',
        summary='List Resource Model Sources for a Project',
        description='''The response contains a set of `source` objects, each describes the `index`, the `type`, and 
details about the `resources`. If the
source had any error, that is included as `errors`.

Resources data includes any `description` provided by the source, whether it is `empty`, and
whether it is `writeable`.  The `href` indicates the URL for `/project/{project}/source/{index}/resources`.

Authorization required: `configure` for project resource

Since: v23''',
        tags=['project','nodes'],
        parameters = @Parameter(
            name = 'project',
            description = 'Project Name',
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = 'string')
        ),
        responses = @ApiResponse(
            responseCode='200',
            description='''Sources List.''',
            content=@Content(
                mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(implementation = Source)),
                examples=@ExampleObject('''[
    {
        "index": 1,
        "resources": {
            "description": "/Users/greg/rundeck2.11/projects/atest/etc/resources.xml",
            "empty": false,
            "href": "http://ecto1.local:4440/api/23/project/atest/source/1/resources",
            "writeable": true
        },
        "type": "file"
    },
    {
        "errors": "File does not exist: /Users/greg/rundeck2.11/projects/atest/etc/resources2.xml",
        "index": 2,
        "resources": {
            "href": "http://ecto1.local:4440/api/23/project/atest/source/2/resources",
            "writeable": false
        },
        "type": "stub"
    }
]''')
            )
        )
    )
    def apiSourcesList() {
        if (!apiService.requireApi(request, response, ApiVersions.V23)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['project'])) {
            return
        }
        def project = params.project
        if (!apiService.requireExists(
            response,
            frameworkService.existsFrameworkProject(project),
            ['project', project]
        )) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }
        final IRundeckProject fwkProject = frameworkService.getFrameworkProject(project)
        final IProjectNodes projectNodes = fwkProject.projectNodes
        final fmk = frameworkService.getRundeckFramework()

        //get list of model source configes
        final resourceConfig = projectNodes.listResourceModelConfigurations()
        final writeableModelSourcesMap = projectNodes.writeableResourceModelSources.collectEntries { [it.index, it] }

        def parseExceptions = fwkProject.projectNodes.getResourceModelSourceExceptionsMap()

        int index = 0
        respond(
            new Sources(
                project,
                resourceConfig.collect { Map config ->
                    index++
                    def ident = index + '.source'
                    def writeableSource = writeableModelSourcesMap[index]
                    new Source(
                        index: index,
                        type: config.type,
                        errors: parseExceptions[ident]?.message ?: null,
                        resources: new Resources(
                            writeable: writeableSource ? true : false,
                            description: writeableSource?.writeableSource?.sourceDescription,
                            empty: writeableSource ? !writeableSource.writeableSource.hasData() : null,
                            syntaxMimeType: writeableSource?.writeableSource?.syntaxMimeType,
                            href: createLink(
                                absolute: true,
                                mapping: 'apiProjectSourceResources',
                                params: [
                                    api_version: ApiVersions.API_CURRENT_VERSION,
                                    project    : project,
                                    index      : index
                                ]
                            ),
                            editPermalink: writeableSource ? createLink(
                                absolute: true,
                                controller: 'framework',
                                action: 'editProjectNodeSourceFile',
                                params: [
                                        project: project,
                                        index  : index
                                ]
                            ) : null
                        )
                    )
                }
            ),

            [formats: responseFormats]
        )
    }

    @Post(uri = '/project/{project}/source/{index}/resources')
    @Operation(
        method = 'POST',
        summary = 'Update Resources of a Resource Model Source',
        description = '''
Authorization required: `configure` for project resource

Since: v23''',
        tags = ['project', 'nodes'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'index',
                description = 'Source Index',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'integer')
            )
        ],
        requestBody = @RequestBody(
            required = true,
            description = 'Resource model data in the supported format',
            content = [
                @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema = @Schema(type = 'object', externalDocs = @ExternalDocumentation(
                        url = 'https://docs.rundeck.com/docs/manual/document-format-reference/resource-json-v10.html',
                        description = "Resources JSON Format"
                    )),
                    examples = @ExampleObject('''{
  "node1": {
    "nodename": "node1",
    "hostname": "node1",
    "osVersion": "5.15.49-linuxkit",
    "osFamily": "unix",
    "osArch": "amd64",
    "description": "Rundeck server node",
    "osName": "Linux"
  }
}''')
                ),
                @Content(
                    mediaType = 'text/yaml',
                    schema = @Schema(type = 'string', externalDocs = @ExternalDocumentation(
                        url = 'https://docs.rundeck.com/docs/manual/document-format-reference/resource-yaml-v13.html',
                        description = "Resources YAML Format"
                    )),
                    examples = @ExampleObject('''node1:
  nodename: node1
  hostname: node1
  osVersion: 5.15.49-linuxkit
  osFamily: unix
  osArch: amd64
  description: Rundeck server node
  osName: Linux
  tags: \'\'''')
                )
            ]
        ),
        responses = [
            @ApiResponse(
                ref = '#/paths/~1project~1%7Bproject%7D~1source~1%7Bindex%7D~1resources/get/responses/200'
            ),
            @ApiResponse(
                responseCode="400",
                description="Invalid format",
                content=@Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema = @Schema(type='object', implementation = ApiErrorResponse)
                )
            )
        ]
    )
    def apiSourceWriteContent() {
        if (!apiService.requireApi(request, response, ApiVersions.V23)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['project','index'])) {
            return
        }
        def project = params.project
        if (!apiService.requireExists(response, frameworkService.existsFrameworkProject(project), ['project', project])) {
            return
        }
        final IRundeckProject fwkProject = frameworkService.getFrameworkProject(project)
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (!apiService.requireAuthorized(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            response,
            [AuthConstants.ACTION_CONFIGURE, 'Project', project]
        )) {
            return
        }

        final contentType = ResourceFormatParserService.baseMimeType(request.contentType)

        def index = params.int('index')
        if (!apiService.requireExists(response, index, ['source index', params.index])) {
            return
        }
        def projectNodes = fwkProject.projectNodes
        final writableSources = projectNodes.writeableResourceModelSources
        final source = writableSources.find { it.index == index }

        if (!source) {
            return apiService.renderErrorFormat(
                response,
                [status: HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                 code  : 'api.error.invalid.request',
                 args  : ["POST to readonly project source index $index"]]
            )
        }
        def format = source.writeableSource.syntaxMimeType
        def inputStream = request.getInputStream()
        //validate
        def framework = frameworkService.rundeckFramework
        if (format != contentType) {
            //attempt to convert to expected format
            ResourceFormatParser parser
            INodeSet nodes

            try {
                parser = framework.resourceFormatParserService.getParserForMIMEType(contentType)
                nodes = parser.parseDocument(request.getInputStream())
            } catch (Exception e) {
                log.error("Cannot parse input data for format: $contentType", e)
                apiService.renderErrorFormat(
                    response,
                    [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                     code  : 'api.error.resource.format.unsupported',
                     args  : [contentType]]
                )
                return
            }
            try {
                def generator = framework.resourceFormatGeneratorService.getGeneratorForMIMEType(format)
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
                generator.generateDocument(nodes, baos)
                inputStream = new ByteArrayInputStream(baos.toByteArray())
            } catch (ResourceFormatGeneratorException | IOException e) {
                log.error("Cannot generate resource model data for format: $format", e)
                apiService.renderErrorFormat(
                    response,
                    [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     code  : 'api.error.resource.format.unsupported',
                     args  : [contentType]]
                )
                return
            } catch (Exception e) {
                log.error("Cannot generate resource model data for format: $format", e)
                apiService.renderErrorFormat(
                    response,
                    [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     code  : 'api.error.unknown',
                    ]
                )
                return
            }
        }

        long size = -1
        def error = null
        def errStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        def errCode = 'api.error.resource.write.failure'
        try {
            size = source.writeableSource.writeData(inputStream)
        } catch (ResourceModelSourceException exc) {
            error = exc
            if(exc instanceof ResourceModelSourceException){
                if(exc.cause instanceof ResourceFormatParserException){
                    if(exc.cause.cause instanceof NodeFileParserException){
                        errStatus = HttpServletResponse.SC_BAD_REQUEST
                        errCode = 'api.error.resource.format.failure'
                        error = exc.cause.cause
                    }
                }
            }
        } catch (IOException | StorageException exc){
            log.error("Failed to store Resource model data for node source[${source.index}] (type:${source.type}) in project ${project}",exc)
            exc.printStackTrace()
            error = exc
        }
        if (error) {
            apiService.renderErrorFormat(
                response,
                [status: errStatus,
                 code  : errCode,
                 args  : [error.message]]
            )
            return
        }
        def readsource = projectNodes.resourceModelSources.find { it.index == index }
        if (!apiService.requireExists(response, readsource, ['source index', params.index])) {
            return
        }

        return apiRenderNodeResult(readsource.source.nodes, framework, params.project)
    }


    @Get(uri='/project/{project}/source/{index}')
    @Operation(
        method='GET',
        summary='Get a Resource Model Source for a Project',
        description='''The response contains the `index`, the `type`, and 
details about the `resources`. If the
source had any error, that is included as `errors`.

Resources data includes any `description` provided by the source, whether it is `empty`, and
whether it is `writeable`.  The `href` indicates the URL for `/project/{project}/source/{index}/resources`.

Authorization required: `configure` for project resource

Since: v23''',
        tags=['project','nodes'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'index',
                description = 'Source Index',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'integer')
            )
        ],
        responses = @ApiResponse(
            responseCode='200',
            description='''Source definition.''',
            content=@Content(
                mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                schema=@Schema(implementation = Source),
                examples=@ExampleObject('''
    {
        "index": 1,
        "resources": {
            "description": "/Users/greg/rundeck2.11/projects/atest/etc/resources.xml",
            "empty": false,
            "href": "http://ecto1.local:4440/api/23/project/atest/source/1/resources",
            "writeable": true
        },
        "type": "file"
    }''')
            )
        )
    )
    def apiSourceGet() {
        if (!apiService.requireApi(request, response, ApiVersions.V23)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['project', 'index'])) {
            return
        }
        def project = params.project
        if (!apiService.requireExists(
            response,
            frameworkService.existsFrameworkProject(project),
            ['project', project]
        )) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }
        def index = params.int('index')
        if (!apiService.requireExists(response, index, ['source index', params.index])) {
            return
        }
        final IRundeckProject fwkProject = frameworkService.getFrameworkProject(project)
        final IProjectNodes projectNodes = fwkProject.projectNodes
        final fmk = frameworkService.getRundeckFramework()

        //get list of model source configes
        final resourceConfig = projectNodes.listResourceModelConfigurations()
        if (resourceConfig.size() < index || index < 1) {
            apiService.renderErrorFormat(
                response,
                [status: HttpServletResponse.SC_NOT_FOUND, code: 'api.error.item.doesnotexist', args: ['source index', params.index]]
            )
            return
        }
        def parseExceptions = fwkProject.projectNodes.getResourceModelSourceExceptionsMap()
        def config = resourceConfig[index - 1]
        def writeableSource = projectNodes.writeableResourceModelSources.find { it.index == index }
        def errors = parseExceptions[index + '.source']

        respondProjectSource(config.type, writeableSource, project, index, errors?.message)
    }

    protected void respondProjectSource(
        String type,
        IProjectNodes.WriteableProjectNodes writeableSource,
        project,
        index,
        String errors
    ) {
        Resources sourceContent = new Resources(
            writeable: writeableSource ? true : false,
            href: createLink(
                absolute: true,
                mapping: 'apiProjectSourceResources',
                params: [
                    api_version: ApiVersions.API_CURRENT_VERSION,
                    project    : project,
                    index      : index
                ]
            ),
            editPermalink: writeableSource ? createLink(
                    absolute: true,
                    controller: 'framework',
                    action: 'editProjectNodeSourceFile',
                    params: [
                            project: project,
                            index  : index
                    ]
            ) : null
        )
        if (writeableSource) {
            sourceContent.description = writeableSource.writeableSource.sourceDescription
            sourceContent.syntaxMimeType = writeableSource.writeableSource.syntaxMimeType
            sourceContent.empty = !writeableSource.writeableSource.hasData()
        }

        respond(
            new Source(
                project: project,
                index: index,
                type: type,
                errors: errors,
                resources: sourceContent
            ),
            [formats: responseFormats]
        )
    }

    @Get(uri='/project/{project}/source/{index}/resources')
    @Operation(
        method='GET',
        summary='List Resources of a Resource Model Source',
        description='''
Authorization required: `configure` for project resource

Since: v23''',
        tags=['project','nodes'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'index',
                description = 'Source Index',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'integer')
            )
        ],
        responses = [
            @ApiResponse(
                ref = '#/paths/~1project~1%7Bproject%7D~1resources/get/responses/200'
            )
        ]
    )
    def apiSourceGetContent() {
        if (!apiService.requireApi(request, response, ApiVersions.V23)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['project', 'index'])) {
            return
        }
        def project = params.project
        if (!apiService.requireExists(
            response,
            frameworkService.existsFrameworkProject(project),
            ['project', project]
        )) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectConfigure(authContext, project),
            AuthConstants.ACTION_CONFIGURE, 'Project',project)) {
            return
        }
        def index = params.int('index')
        if (!apiService.requireExists(response, index, ['source index', params.index])) {
            return
        }
        final IRundeckProject fwkProject = frameworkService.getFrameworkProject(project)
        final IProjectNodes projectNodes = fwkProject.projectNodes
        final fmk = frameworkService.getRundeckFramework()

        //get list of model source configes
        final resourceConfig = projectNodes.listResourceModelConfigurations()
        if (resourceConfig.size() < index || index < 1) {
            apiService.renderErrorFormat(
                response,
                [status: HttpServletResponse.SC_NOT_FOUND, code: 'api.error.item.doesnotexist', args: ['source index', params.index]]
            )
            return
        }
        def source = projectNodes.resourceModelSources.find { it.index == index }
        if (!apiService.requireExists(response, source, ['source index', params.index])) {
            return
        }
        return apiRenderNodeResult(source.source.nodes, fmk, params.project)
    }

    @Get(uri='/project/{project}/resource/{name}')
    @Operation(
        method='GET',
        summary='Get Node Info',
        description='''Get a specific resource within a project.

Authorization required: `read` for project resource type `node`, as well as `read` for the Node 

Since: v14''',
        tags=['project','nodes'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'name',
                description = 'Node Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            )
        ],
        responses = @ApiResponse(
            ref = '#/paths/~1project~1%7Bproject%7D~1resources/get/responses/200'
        )
    )
    /**
     * API: /api/14/project/PROJECT/resource/NAME, version 14
     */
    def apiResourcev14 () {
        if(!apiService.requireApi(request,response)){
            return
        }
        IFramework framework = frameworkService.getRundeckFramework()
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        if(!params.name){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['name']])
        }
        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project',params.project]])
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                AuthConstants.ACTION_READ, params.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project]])
        }

        NodeSet nset = new NodeSet()
        nset.setSingleNodeName(params.name)
        def pject=frameworkService.getFrameworkProject(params.project)
        final INodeSet nodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nset,pject.getNodeSet())

        def readnodes = rundeckAuthContextProcessor.filterAuthorizedNodes(params.project, [AuthConstants.ACTION_READ] as Set, nodes, authContext)

        if (!readnodes || readnodes.nodes.size() < 1) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code  : 'api.error.item.doesnotexist',
                    args  : ['Node Name', params.name]]
            )
        }
        return apiRenderNodeResult(readnodes, framework, params.project)
    }

    @Get(uri='/project/{project}/resources')
    @Operation(
        method='GET',
        summary='List Project Nodes',
        description='''List or query the nodes (resources) for a project.

Node Filter parameters: You can select nodes to include and exclude in the result set, see below.

**Note:** If no query parameters are included, the result set will include all Node resources for the project.

Refer to the [User Guide - Node Filters](https://docs.rundeck.com/docs/manual/11-node-filters.html) Documentation for information on
the node filter syntax and usage.

A basic node filter looks like:

    attribute: value attribute2: value2

To specify a Node Filter string as a URL parameter for an API request, use a parameter named `filter`.
Your HTTP client will have to correctly escape the value of the `filter` parameter.  For example you can
use `curl` like this;

    curl --data-urlencode "filter=attribute: value"

Common attributes:

* `name` - node name
* `tags` - tags
* `hostname`
* `username`
* `osFamily`, `osName`, `osVersion`, `osArch`

Custom attributes can also be used.

Authorization required: `read` for project resource type `node`, as well as `read` for each Node resource

Since: v14''',
        tags=['project','nodes'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                required = true,
                in = ParameterIn.PATH,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'filter',
                description = 'Node Filter String',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            )
        ],
        responses = @ApiResponse(
            responseCode='200',
            description='''The resource model data.''',
            content = [
                @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object', externalDocs = @ExternalDocumentation(
                        url = 'https://docs.rundeck.com/docs/manual/document-format-reference/resource-json-v10.html',
                        description = "Resources JSON Format"
                    )),
                    examples=@ExampleObject('''{
  "node1": {
    "nodename": "node1",
    "hostname": "node1",
    "osVersion": "5.15.49-linuxkit",
    "osFamily": "unix",
    "osArch": "amd64",
    "description": "Rundeck server node",
    "osName": "Linux"
  }
}''')
                ),
                @Content(
                    mediaType = 'text/yaml',
                    schema=@Schema(type='string', externalDocs = @ExternalDocumentation(
                        url = 'https://docs.rundeck.com/docs/manual/document-format-reference/resource-yaml-v13.html',
                        description = "Resources YAML Format"
                    )),
                    examples=@ExampleObject('''node1:
  nodename: node1
  hostname: node1
  osVersion: 5.15.49-linuxkit
  osFamily: unix
  osArch: amd64
  description: Rundeck server node
  osName: Linux
  tags: \'\'''')
                )
            ]
        )
    )
    /**
     * API: /api/2/project/NAME/resources, version 2
     */
    def apiResourcesv2(@Parameter(hidden = true) ExtNodeFilters query) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (query.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.invalid.request', args: [query.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        IFramework framework = frameworkService.getRundeckFramework()
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                                                           code: 'api.error.item.doesnotexist', args: ['project',params.project]])


        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_NODE,
                AuthConstants.ACTION_READ, params.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                                                           code: 'api.error.item.unauthorized', args: ['Read Nodes', 'Project', params.project]])

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
        def readnodes = rundeckAuthContextProcessor.filterAuthorizedNodes(
                params.project,
                Collections.singleton('read'),
                nodes,
                authContext
        )
        return apiRenderNodeResult(readnodes, framework, params.project)
    }

    def handleInvalidMimeType(InvalidMimeTypeException e) {
        return apiService.renderErrorFormat(
            response,
            [
                status: HttpServletResponse.SC_BAD_REQUEST,
                code  : 'api.error.invalid.request',
                args  : [e.message]
            ]
        )
    }
    static final Map<String, String> resourceFormatBuiltinTypes = [
        xml : 'resourcexml',
        json: 'resourcejson',
        yaml: 'resourceyaml',
    ]

    protected def apiRenderNodeResult(INodeSet nodes, IFramework framework, String project) {
        def reqformat = params.format ?: response.format
        if (reqformat in ['all', 'html']) {
            if (request.api_version < ApiVersions.V23) {
                reqformat = 'xml'
            } else {
                reqformat = 'json'
            }
        }
        reqformat = resourceFormatBuiltinTypes[reqformat] ?: reqformat
        //render specified format
        final service = framework.getResourceFormatGeneratorService()
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        def generator

        [params.format, response.format, reqformat].each {
            if (!generator && it) {
                try {
                    generator = service.getGeneratorForFormat(it)
                } catch (UnsupportedFormatException e) {
                    log.debug("could not get generator for format: ${it}: ${e.message}", e)
                }
            }
        }
        if (!generator) {
            //try accept header
            List<MediaType> mimes = []
            try {
                mimes = MediaType.parseMediaTypes(request.getHeader('accept'))
                MediaType.sortBySpecificityAndQuality(mimes)
            } catch (RuntimeException e) {
                return apiService.renderErrorFormat(
                    response,
                    [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.invalid.request',
                        args  : [e.message]
                    ]
                )
            }
            for (MediaType mime : mimes) {
                try {
                    generator = service.getGeneratorForMIMEType(mime.toString())
                    break
                } catch (UnsupportedFormatException e) {
                    log.debug(
                        "could not get generator for mime type: ${request.getHeader("accept")}: ${e.message}",
                        e
                    )
                }
            }
        }
        if (!generator) {
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code  : 'api.error.resource.format.unsupported',
                    args  : [reqformat]
                ]
            )
        }


        try {
            generator.generateDocument(nodes, baos)
        } catch (ResourceFormatGeneratorException e) {
            return apiService.renderErrorFormat(
                response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           code  : 'api.error.resource.format.generator', args: [e.message]]
            )
        } catch (IOException e) {
            return apiService.renderErrorFormat(
                response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           code  : 'api.error.resource.format.generator', args: [e.message]]
            )
        }
        final types = generator.getMIMETypes() as List
        return render(contentType: types[0], encoding: "UTF-8", text: baos.toString())

    }

    /**
     * Documentation method for DELETE system acls
     */
    @Delete('/system/acl/{path}')
    @Operation(
        method = "DELETE",
        summary = "Delete an ACL Policy.",
        description = """
Authorization required: `delete` or `admin` or `app_admin` access for `system_acl` resource type 

Since: v14""",
        tags=['acls'],
        parameters = [
            @Parameter(
                name = 'path',
                in = ParameterIn.PATH,
                description = 'Path to the Acl policy file',
                allowEmptyValue = false,
                required = true,
                schema = @Schema(implementation = String.class, pattern = '\\w+.aclpolicy')
            )
        ],
        responses = [
            @ApiResponse(
                responseCode = "204",
                description = "No Content"
            ),

            @ApiResponse(
                responseCode = "404",
                description = "Not Found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse)
                )
            )

        ]
    )
    protected def apiSystemAcls_DELETE_docs(){}

    /**
     * Documentation method for PUT system acls
     */
    @Put('/system/acl/{path}')
    @Operation(
        method = "PUT",
        summary = "Update an ACL Policy.",
        description = """
Authorization required: `update` or `admin` or `app_admin` access for `system_acl` resource type 

Since: v14""",
        tags=['acls'],
        parameters = [
            @Parameter(
                name = 'path',
                in = ParameterIn.PATH,
                description = 'Path to the Acl policy file',
                allowEmptyValue = false,
                required = true,
                schema = @Schema(implementation = String.class, pattern = '\\w+.aclpolicy')
            )
        ],
        requestBody = @RequestBody(
            ref = '#/paths/~1system~1acl~1%7Bpath%7D/post/requestBody'
        ),
        responses = [
            @ApiResponse(
                ref = '#/paths/~1system~1acl~1%7Bpath%7D/get/responses/200'
            ),

            @ApiResponse(
                responseCode = "404",
                description = "Not Found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse)
                )
            ),

            @ApiResponse(
                responseCode = "400",
                description = '''Validation failure. If Validation fails, the body will contain a list of validation errors.
Because each ACLPOLICY document can contain multiple Yaml documents, each will be listed as a separate policy.''',
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type='object'),
                    examples = @ExampleObject('''{
  "valid": false,
  "policies": [
    {
      "policy": "file1.aclpolicy[1]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    },

    {
      "policy": "file1.aclpolicy[2]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    }
  ]
}''')
                )
            )
        ]
    )
    protected def apiSystemAcls_PUT_docs(){}


    /**
     * Documentation method for POST system acls
     */
    @Post('/system/acl/{path}')
    @Operation(
        method = "POST",
        summary = "Create an ACL Policy.",
        description = """
Authorization required: `create` or `admin` or `app_admin` access for `system_acl` resource type 

Since: v14""",
        tags=['acls'],
        parameters = [
            @Parameter(
                name = 'path',
                in = ParameterIn.PATH,
                description = 'Path to the Acl policy file',
                allowEmptyValue = false,
                required = true,
                schema = @Schema(implementation = String.class, pattern = '\\w+.aclpolicy')
            )
        ],
        requestBody = @RequestBody(
            description='''If the `Content-Type` is `application/yaml` or `text/plain`, then the request body is the ACL policy contents directly.

Otherwise, you can use JSON to wrap the yaml content inside `contents`
''',
            content = [
                @Content(
                    mediaType = 'application/yaml',
                    schema=@Schema(type='string'),
                    examples = @ExampleObject('''description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build''')
                ),
                @Content(
                    mediaType = io.micronaut.http.MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object'),
                    examples = @ExampleObject('''{
  "contents": "description: \\"my policy\\"\\ncontext:\\n  application: rundeck\\nfor:\\n  project:\\n    - allow: read\\nby:\\n  group: build"
}''')
                )
            ]
        ),
        responses = [
            @ApiResponse(
                responseCode = "201",
                description = "Created",
                content = [
                    @Content(
                        mediaType = 'text/plain',
                        examples = @ExampleObject('''description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build''')
                    ),
                    @Content(
                        mediaType = "application/yaml",
                        examples = @ExampleObject('''description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build''')
                    ),
                    @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject('''{
  "contents": "description: \\"my policy\\"\\ncontext:\\n  application: rundeck\\nfor:\\n  project:\\n    - allow: read\\nby:\\n  group: build"
}''')
                    )
                ]
            ),

            @ApiResponse(
                responseCode = "409",
                description = "Conflict. Already exists",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse)
                )
            ),

            @ApiResponse(
                responseCode = "400",
                description = '''Validation failure. If Validation fails, the body will contain a list of validation errors.
Because each ACLPOLICY document can contain multiple Yaml documents, each will be listed as a separate policy.''',
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type='object'),
                    examples = @ExampleObject('''{
  "valid": false,
  "policies": [
    {
      "policy": "file1.aclpolicy[1]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    },

    {
      "policy": "file1.aclpolicy[2]",
      "errors": [
        "reason...",
        "reason2..."
      ]
    }
  ]
}''')
                )
            )
        ]
    )
    protected def apiSystemAcls_POST_docs(){}

    @Get('/system/acl/{path}')
    @Operation(
        method = "GET",
        summary = "Get an ACL Policy.",
        description = """Retrieve the YAML text of the ACL Policy file.  If YAML or text content is requested, the contents will be returned directly.
Otherwise if XML or JSON is requested, the YAML text will be wrapped within that format.

Authorization required: `read` or `admin` or `app_admin` access for `system_acl` resource type 

Since: v14""",
        tags=['acls'],
        parameters = [
            @Parameter(
                name = 'path',
                in = ParameterIn.PATH,
                description = 'Path to the Acl policy file',
                allowEmptyValue = false,
                required = true,
                schema = @Schema(implementation = String.class, pattern = '\\w+.aclpolicy')
            )
        ]
    )
    @ApiResponse(
        responseCode = "200",
        description = "ACL Policy Document",
        content = [
            @Content(
                mediaType = 'text/plain',
                examples = @ExampleObject('''description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build''')
            ),
            @Content(
                mediaType = "application/yaml",
                examples = @ExampleObject('''description: "my policy"
context:
  application: rundeck
for:
  project:
    - allow: read
by:
  group: build''')
            ),
            @Content(
                mediaType = "application/json",
                examples = @ExampleObject('''{
  "contents": "description: \\"my policy\\"\\ncontext:\\n  application: rundeck\\nfor:\\n  project:\\n    - allow: read\\nby:\\n  group: build"
}''')
            )
        ]
    )
    /**
     * /api/14/system/acl/* endpoint
     */
    def apiSystemAcls(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        def authAction = ApiService.HTTP_METHOD_ACTIONS[request.method]
        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_SYSTEM_ACL,
                [
                        authAction,
                        AuthConstants.ACTION_ADMIN,
                        AuthConstants.ACTION_APP_ADMIN
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
        def filename = (params.path ?: '')
        log.debug("apiSystemAcls, file: ${filename}")
        switch (request.method) {
            case 'POST':
            case 'PUT':
                apiSystemAclsPutResource(filename, request.method=='POST')
                break
            case 'GET':
                if(filename){
                    apiSystemAclsGetResource(filename)
                }else{
                    apiSystemAclsGetList()
                }
                break
            case 'DELETE':
                apiSystemAclsDeleteResource(filename)
                break
        }
    }

    private def renderAclHref(String path) {
        createLink(absolute: true, uri: "/api/${ApiVersions.API_CURRENT_VERSION}/system/acl/$path")
    }
    private def apiSystemAclsPutResource(String filename, boolean create) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','yaml','text'],request.format)

        def exists = aclFileManagerService.existsPolicyFile(AppACLContext.system(),filename)
        if(create && exists){
            //conflict
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_CONFLICT,
                    code: 'api.error.item.alreadyexists',
                    args: ['System ACL Policy File', filename],
                    format:respFormat
            ])
        }else if(!create && !exists){
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args: ['System ACL Policy File', filename],
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
        Validation validation = aclFileManagerService.validateYamlPolicy(AppACLContext.system(), filename, text)
        if(!validation.valid){
            response.status = HttpServletResponse.SC_BAD_REQUEST
            def controller = this
            return withFormat{
                '*' {
                    render apiService.renderJsonAclpolicyValidation(validation) as JSON
                }

                if (controller.isAllowXml()) {
                    xml {
                        render(contentType: 'application/xml') {
                            apiService.renderXmlAclpolicyValidation(validation, delegate)
                        }
                    }
                }
            }
        }

        aclFileManagerService.storePolicyFileContents(AppACLContext.system(), filename, text)
        response.status=create ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK
        //TODO: just call apiSystemAclsGetResource to respond
        if(respFormat in ['yaml','text']){
            //write directly
            response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
            aclFileManagerService.loadPolicyFileContents(AppACLContext.system(), filename, response.outputStream)
            flush(response)
        }else{
            def baos=new ByteArrayOutputStream()
            aclFileManagerService.loadPolicyFileContents(AppACLContext.system(), filename, baos)
            def controller = this
            withFormat{
                '*' {
                    def content = [contents: baos.toString()]
                    render content as JSON
                }

                if (controller.isAllowXml()) {
                    xml {
                        render(contentType: 'application/xml') {
                            apiService.renderWrappedFileContentsXml(baos.toString(), respFormat, delegate)
                        }

                    }
                }
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
    private def apiSystemAclsGetResource(String projectFilePath) {
        def respFormat = apiService.extractResponseFormat(request, response, ['yaml','xml','json','text'],request.format)
        if(aclFileManagerService.existsPolicyFile(AppACLContext.system(), projectFilePath)){
            if(respFormat in ['yaml','text']){
                //write directly
                response.setContentType(respFormat=='yaml'?"application/yaml":'text/plain')
                aclFileManagerService.loadPolicyFileContents(AppACLContext.system(), projectFilePath, response.outputStream)
                flush(response)
            }else{
                //render as json/xml with contents as string
                def baos=new ByteArrayOutputStream()
                aclFileManagerService.loadPolicyFileContents(AppACLContext.system(), projectFilePath, baos)
                def controller = this
                withFormat{
                    '*' {
                        def content = [contents:baos.toString()]
                        render content as JSON
                    }

                    if(controller.isAllowXml()) {
                        xml {
                            render(contentType: 'application/xml') {
                                apiService.renderWrappedFileContentsXml(baos.toString(), respFormat, delegate)
                            }

                        }
                    }
                }
            }
        }else{

            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['resource',params.path],
                    format: respFormat
            ])
        }

    } /**
     * Get resource or dir listing for the specified project path
     * @param project project
     * @param projectFilePath path for the project file or dir
     * @param rmprefix prefix string for the path, to be removed from paths in dir listings
     * @return
     */
    private def apiSystemAclsGetList() {
        def respFormat = apiService.extractResponseFormat(request, response, ['yaml','xml','json','text'],request.format)

        //list aclpolicy files in the dir
        def list = aclFileManagerService.listStoredPolicyFiles(AppACLContext.system())
        def controller = this
        withFormat{
            '*' {
                render apiService.jsonRenderDirlist(
                            '',
                            { String p -> p },
                            { String p -> renderAclHref(p) },
                            list
                    ) as JSON
            }

            if (controller.isAllowXml()) {
                xml {
                    render(contentType: 'application/xml') {
                        apiService.xmlRenderDirList(
                                '',
                                { String p -> p },
                                { String p -> renderAclHref(p) },
                                list,
                                delegate
                        )
                    }

                }
            }
        }
    }


    private def apiSystemAclsDeleteResource(projectFilePath) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml','json','text'],request.format)
        boolean exists=aclFileManagerService.existsPolicyFile(AppACLContext.system(), projectFilePath)
        if(!exists){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args:['System ACL Policy File',params.path],
                    format: respFormat
            ])
        }
        boolean done=aclFileManagerService.deletePolicyFile(AppACLContext.system(), projectFilePath)
        if(!done){
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_CONFLICT,
                    message: "error",
                    format: respFormat
            ])
        }
        render(status: HttpServletResponse.SC_NO_CONTENT)
    }

    private void notifyRemovedPlugins(String project, List removedPlugins){
        removedPlugins.each { plugin ->
            frameworkService.grailsEventBus.notify(PROJECT_PLUGINS_REMOVED_EVENT + '.' + plugin['type'], [ plugin: plugin, project: project ])
        }
    }
}
