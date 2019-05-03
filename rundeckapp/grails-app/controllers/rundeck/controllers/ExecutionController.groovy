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

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.api.jobs.upload.ExecutionFileInfoList
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileInfo
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.app.support.ExecutionQueryMetrics
import com.dtolabs.rundeck.app.support.ExecutionViewParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.PluginDisabledException
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.ReverseSeekingStreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.util.MetricsStatsBuilder
import grails.converters.JSON
import groovy.transform.PackageScope
import org.quartz.JobExecutionContext
import org.springframework.dao.DataAccessResourceFailureException
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.*
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import rundeck.services.workflow.StateMapping

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
* ExecutionController
*/
class ExecutionController extends ControllerBase{

    FrameworkService frameworkService
    ExecutionService executionService
    LoggingService loggingService
    ScheduledExecutionService scheduledExecutionService
    OrchestratorPluginService orchestratorPluginService
    ApiService apiService
    WorkflowService workflowService
    FileUploadService fileUploadService
    PluginService pluginService
    ConfigurationService configurationService

    static allowedMethods = [
            delete:['POST','DELETE'],
            bulkDelete:['POST'],
            apiExecutionAbort: ['POST','GET'],
            apiExecutionDelete: ['DELETE'],
            apiExecutionDeleteBulk: ['POST'],
            apiExecutionModePassive: ['POST'],
            apiExecutionModeActive: ['POST'],
            cancelExecution:'POST',
            incompleteExecution: 'POST'
    ]

    def index() {
        redirect(controller:'menu',action:'index')
    }

    def follow() {
        def m = show()
        if(response.status != 302) {
            render(view:'show',model:m)
        }
    }

    def followFragment() {
        return render(view:'showFragment',model:show())
    }
    /**
     * List recent adhoc executions to fill the recent commands menu on commands page.
     * @param project project name
     * @param max maximum results, defaults to 10
     * @return
     */
    def adhocHistoryAjax(String project, int max, String query){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,project)

        if (unauthorizedResponse(
                frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                                                          AuthConstants.ACTION_READ, params.project),
                AuthConstants.ACTION_READ, 'adhoc', 'commands')) {
            return
        }

        def execs = []
        def uniques=new HashSet<String>()
        def offset=0
        if(!max){
            max=10
        }
        def notDispatchedFilter = OptsUtil.join("name:", frameworkService.getFrameworkNodeName())

        while(execs.size()<max){

            def res = Execution.findAllByProjectAndUserAndScheduledExecutionIsNull(
                    project,
                    session.user,
                    [sort: 'dateStarted', order: 'desc', max: max,offset:offset]
            )

            offset+=res.size()
            res.each{exec->
                if(execs.size()<max
                        && exec.workflow.commands.size()==1
                        && exec.workflow.commands[0] instanceof CommandExec
                        && exec.workflow.commands[0].adhocRemoteString){

                    def appliedFilter = exec.doNodedispatch ? exec.filter : notDispatchedFilter
                    def str=exec.workflow.commands[0].adhocRemoteString+";"+appliedFilter
                    if(query && query.size()>4 && !str.contains(query)){
                        return
                    }
                    if(!uniques.contains(str)){
                        uniques<<str
                        execs<<exec
                    }
                }
            }
            if(res.size()<max){
                break
            }
        }

        def elementList = execs.collect{Execution exec->
            if(exec.workflow.commands.size()==1 && exec.workflow.commands[0].adhocRemoteString) {
                def href=createLink(
                        controller: 'framework',
                        action: 'adhoc',
                        params: [project: project, fromExecId: exec.id]
                )

                def appliedFilter = exec.doNodedispatch ? exec.filter : notDispatchedFilter
                return [
                        status: exec.getExecutionState(),
                        succeeded: exec.statusSucceeded(),
                        href: href,
                        execid: exec.id,
                        title: exec.workflow.commands[0].adhocRemoteString,
                        filter: appliedFilter
                ]
            }
        }
        render(contentType: 'application/json'){
            executions elementList
        }
    }

    private Map loadExecutionViewPlugins() {
        def pluginDescs = [node: [:], workflow: [:]]

        frameworkService.getNodeStepPluginDescriptions().each { desc ->
            pluginDescs['node'][desc.name] = desc
        }
        frameworkService.getStepPluginDescriptions().each { desc ->
            pluginDescs['workflow'][desc.name] = desc
        }
        [

                stepPluginDescriptions: pluginDescs,
                orchestratorPlugins   : orchestratorPluginService.getOrchestratorPlugins(),
                strategyPlugins       : scheduledExecutionService.getWorkflowStrategyPluginDescriptions(),
                logFilterPlugins      : pluginService.listPlugins(LogFilterPlugin),
        ]
    }

    public def show (ExecutionViewParams viewparams){
        if (viewparams.hasErrors()) {
            flash.errors=viewparams.errors
            render(view: '/common/error')
            return
        }
        def Execution e = Execution.get(params.id)
        if(notFoundResponse(e,'Execution ID',params.id)){
            return
        }
        def filesize=-1
        if(null!=e.outputfilepath){
            def file = new File(e.outputfilepath)
            if (file.exists()) {
                filesize = file.length()
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)

        if (unauthorizedResponse(frameworkService.authorizeProjectExecutionAny(authContext, e,
                [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW]), AuthConstants.ACTION_VIEW,'Execution',params.id)) {
            return
        }
        if(!params.project || params.project!=e.project) {
            redirect(controller: 'execution', action: 'show', params: [id: params.id, project: e.project], fragment: params?.outdetails)
            return
        }
        params.project=e.project
        request.project=e.project

        def enext,eprev
        def result= Execution.withCriteria {
            gt('dateStarted', e.dateStarted)
            if (e.scheduledExecution) {
                eq('scheduledExecution',e.scheduledExecution)
            }else{
                isNull('scheduledExecution')
            }
            eq('project',e.project)
            maxResults(1)
            order('dateStarted','asc')
        }
        enext=result?result[0]:null
        result = Execution.withCriteria {
            lt('dateStarted', e.dateStarted)
            if (e.scheduledExecution) {
                eq('scheduledExecution', e.scheduledExecution)
            }else{
                isNull('scheduledExecution')
            }
            eq('project', e.project)
            maxResults(1)
            order('dateStarted', 'desc')
        }
        eprev = result ? result[0] : null
        def readAuth=frameworkService.authorizeProjectExecutionAny(authContext, e, [AuthConstants.ACTION_READ])
        def workflowTree = scheduledExecutionService.getWorkflowDescriptionTree(e.project, e.workflow, readAuth,0)
        def inputFiles = fileUploadService.findRecords(e, FileUploadService.RECORD_TYPE_OPTION_INPUT)
        def inputFilesMap = inputFiles.collectEntries { [it.uuid, it] }

        def projectNames = frameworkService.projectNames(authContext)
        def authProjectsToCreate = []
        projectNames.each{
            if(it != params.project && frameworkService.authorizeProjectResource(
                    authContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    AuthConstants.ACTION_CREATE,
                    it
            )){
                authProjectsToCreate.add(it)
            }
        }

        return loadExecutionViewPlugins() + [
                scheduledExecution    : e.scheduledExecution ?: null,
                execution             : e,
                workflowTree          : workflowTree,
                filesize              : filesize,
                nextExecution         : e.scheduledExecution?.scheduled ? scheduledExecutionService.nextExecutionTime(
                        e.scheduledExecution
                ) : null,
                enext                 : enext,
                eprev                 : eprev,
                inputFilesMap         : inputFilesMap,
                projectNames          : authProjectsToCreate,
                clusterModeEnabled    : frameworkService.isClusterModeEnabled()
        ]
    }

    def delete() {
        withForm{
        def Execution e = Execution.get(params.id)
        if (notFoundResponse(e, 'Execution ID', params.id)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)

        if (unauthorizedResponse(frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(e.project),
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]),
                AuthConstants.ACTION_DELETE_EXECUTION,'Project',e.project)) {
            return
        }
        params.project = e.project
        def jobid=e.scheduledExecution?.extid
        def result=executionService.deleteExecution(e,authContext,session.user)
        if(!result.success){
            flash.error=result.error
            return redirect(controller: 'execution', action: 'show', id: params.id, params: [project: params.project])
        }else{
            flash.message="Deleted execution ID: ${params.id}"
        }
        if(jobid){
            flash.message = "Deleted execution ID: ${params.id} of job {{Job ${jobid}}}"
        }
        return redirect(controller: 'reports', action: 'index', params: [project: params.project])
        }.invalidToken{
            request.error=g.message(code:'request.error.invalidtoken.message')
            renderErrorView([:])
        }
    }

    def bulkDelete(){
        withForm{
        def ids
        if(params.checkedIds){
            ids=params.checkedIds.toString().split(',').flatten()
        }else if(params.bulk_edit){
            ids=[params.bulk_edit].flatten()
        }else if(params.ids){
            ids = [params.ids].flatten()
        }else{
            flash.error="Some IDS are required for bulk delete"
            return redirect(action: 'index',controller: 'reports',params: [project:params.project])
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)
        def result=executionService.deleteBulkExecutionIds(ids,authContext,session.user)
        if(!result.success){
            flash.error=result.failures*.message.join(", ")
        }
        flash.message="${result.successTotal} Executions deleted"
        return redirect(action: 'index', controller: 'reports', params: [project: params.project])
        }.invalidToken{
            flash.error=g.message(code:'request.error.invalidtoken.message')
            return redirect(action: 'index', controller: 'reports', params: [project: params.project])
        }
    }
    def ajaxExecState(){
        def Execution e = Execution.get(params.id)
        if (!e) {
            log.error("Execution not found for id: " + params.id)
            response.status=HttpServletResponse.SC_NOT_FOUND
            return render(contentType: 'application/json', text: [error: "Execution not found for id: " + params.id] as JSON)
        }

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)

        if (e && !frameworkService.authorizeProjectExecutionAny(authContext, e, [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])) {
            response.status=HttpServletResponse.SC_FORBIDDEN
            return render(contentType: 'application/json',text:[error: "Unauthorized: View Execution ${params.id}"] as JSON)
        }

        def jobcomplete = e.dateCompleted != null
        def execState = e.executionState
        def execDuration = (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()
        def jobAverage=-1L
        if (e.scheduledExecution && e.scheduledExecution.getAverageDuration() > 0) {
            jobAverage = e.scheduledExecution.getAverageDuration()
        }
        def isClusterExec = frameworkService.isClusterModeEnabled() && e.serverNodeUUID !=
                frameworkService.getServerUUID()
        def data=[
                completed            : jobcomplete,
                execDuration         : execDuration,
                executionState       : execState.toUpperCase(),
                executionStatusString: e.customStatusString,
                jobAverageDuration   : jobAverage,
                startTime            : StateMapping.encodeDate(e.dateStarted),
                endTime              : StateMapping.encodeDate(e.dateCompleted),
                retryAttempt         : e.retryAttempt,
                retry                : e.retry,
                serverNodeUUID       : e.serverNodeUUID,
                clusterExec          : isClusterExec
        ]
        if(e.retryExecution){
            data['retryExecutionId']=e.retryExecution.id
            data['retryExecutionUrl']=createLink(controller: 'execution',action: 'show',id: e.retryExecution.id,
                    params:[project:e.project])
            data['retryExecutionState']=e.retryExecution.executionState.toUpperCase()
            data['retryExecutionAttempt']= e.retryExecution.retryAttempt
        }
        def selectedNodes=[]
        if(params.nodes instanceof String){
            selectedNodes=params.nodes.split(',') as List
        }else if(params.nodes){
            selectedNodes=[params.nodes].flatten()
        }
        def loader = workflowService.requestStateSummary(
                e,
                selectedNodes,
                params.selectedNodesOnly == 'true',
                true,
                params.stepStates == 'true'
        )
        if (loader.state.isAvailableOrPartial()) {
            data.state = loader.workflowState
        }else if(loader.state in [ExecutionLogState.NOT_FOUND]) {
            data.state = [error: 'not found',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state,
                            default: "Not Found")]
        }else if(loader.state in [ExecutionLogState.ERROR]) {
            data.state = [error: 'error', errorMessage: g.message(code: loader.errorCode, args: loader.errorData)]
        }else if (loader.state in [ ExecutionLogState.PENDING_LOCAL, ExecutionLogState.WAITING,
                ExecutionLogState.AVAILABLE_REMOTE, ExecutionLogState.PENDING_REMOTE]) {
            data.completed = false
            data.state = [error: 'pending',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state, default: "Pending")]
        } else if (loader.state in [ExecutionLogState.AVAILABLE_REMOTE_PARTIAL]) {
            data.completed = false
            data.state = [error       : 'pending',
                          errorMessage: g.message(
                                  code: 'execution.state.storage.state.' + loader.state,
                                  default: "Pending"
                          )]
        } else {
            data.state = [error       : 'unknown',
                          errorMessage: g.message(
                                  code: 'execution.state.storage.state.UNKNOWN',
                                  args: ["state: " + loader.state].toArray()
                          )]
        }
        if (loader.state == ExecutionLogState.AVAILABLE_PARTIAL) {
            data.completed = false
            data.state.partial = true
        }
        if(loader.retryBackoff>0){
            data.retryBackoff = loader.retryBackoff
        }
        def limit=grailsApplication.config.rundeck?.ajax?.executionState?.compression?.nodeThreshold?:500
        if (selectedNodes || data.state?.allNodes?.size() > limit) {
            renderCompressed(request, response, 'application/json', data as JSON)
        }else{
            render data as JSON
        }
    }

    def ajaxExecNodeState(ExecutionViewParams viewparams) {
        if (viewparams.hasErrors()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            render(contentType: 'application/json') {
                error = 'invalid'
                errorMessage = g.message(code: 'api.error.invalid.request', args: [error: viewparams.errors.toString()])
            }
            return
        }
        if (requireAjax(action: 'show', controller: 'execution', params: params)) {
            return
        }
        def Execution e = Execution.get(params.id)
        if (!e) {
            response.status=HttpServletResponse.SC_NOT_FOUND
            return render(contentType: 'application/json', text: [error: "Execution not found for id: " + params.id] as JSON)
        }

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)

        if (e && !frameworkService.authorizeProjectExecutionAny(authContext, e, [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])) {
            response.status=HttpServletResponse.SC_FORBIDDEN
            return render(contentType: 'application/json',text:[error: "Unauthorized: View Execution ${params.id}"] as JSON)
        }


        def data=[:]
        def selectedNode=params.node
        def loader = workflowService.requestStateSummary(e,[selectedNode],true)
        if (loader.state.isAvailableOrPartial()) {
            data = [
                    name:selectedNode,
                    summary:loader.workflowState.nodeSummaries[selectedNode],
                    steps:loader.workflowState.nodeSteps[selectedNode]
            ]
        }else if(loader.state in [ExecutionLogState.NOT_FOUND]) {
            data = [error: 'not found',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state,
                            default: "Not Found")]
        }else if(loader.state in [ExecutionLogState.ERROR]) {
            data = [error: 'error', errorMessage: g.message(code: loader.errorCode, args: loader.errorData)]
        }else if (loader.state in [ ExecutionLogState.PENDING_LOCAL, ExecutionLogState.WAITING,
                ExecutionLogState.AVAILABLE_REMOTE, ExecutionLogState.PENDING_REMOTE]) {
            data = [error: 'pending',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state, default: "Pending")]
        } else {
            data.state = [error       : 'unknown',
                          errorMessage: g.message(
                                  code: 'execution.state.storage.state.UNKNOWN',
                                  args: ["state: " + loader.state].toArray()
                          )]
        }
        return renderCompressed(request, response, 'application/json', data.encodeAsJSON())
    }

    def mail() {
        def Execution e = Execution.get(params.id)
        if (notFoundResponse(e, 'Execution ID', params.id)) {
            return
        }
        def file = loggingService.getLogFileForExecution(e)
        def filesize=-1
        if (file.exists()) {
            filesize = file.length()
        }
        final state = e.executionState
        if(e.scheduledExecution){
            def ScheduledExecution se = e.scheduledExecution //ScheduledExecution.get(e.scheduledExecutionId)
            return render(
                    view: "mailNotification/status",
                    model: loadExecutionViewPlugins() + [execstate: state, scheduledExecution: se, execution: e,
                                                         filesize: filesize]
            )
        }else{
            return render(
                    view: "mailNotification/status",
                    model: loadExecutionViewPlugins() + [execstate: state, execution: e, filesize: filesize]
            )
        }
    }
    def executionMode(){
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        withForm {
            def requestActive=params.mode == 'active'
            def authAction=requestActive?AuthConstants.ACTION_ENABLE_EXECUTIONS:AuthConstants.ACTION_DISABLE_EXECUTIONS
            if (unauthorizedResponse(frameworkService.authorizeApplicationResourceAny(authContext,
                                                                                      AuthConstants.RESOURCE_TYPE_SYSTEM,
                                                                                      [authAction, AuthConstants.ACTION_ADMIN]),

                                     authAction,'for','Rundeck')) {
                return
            }
            if(requestActive == executionService.executionsAreActive){
                flash.message=g.message(code:'action.executionMode.notchanged.'+(requestActive?'active':'passive')+'.text')
                return redirect(controller: 'menu',action:'executionMode')
            }
            executionService.setExecutionsAreActive(requestActive)
            flash.message=g.message(code:'action.executionMode.changed.'+(requestActive?'active':'passive')+'.text')
            return redirect(controller: 'menu',action:'executionMode')
        }.invalidToken{

            request.error=g.message(code:'request.error.invalidtoken.message')
            renderErrorView([:])
        }
    }


    private def xmlerror() {
        render(contentType:"text/xml",encoding:"UTF-8"){
            result(error:"true"){
                delegate.'error'{
                    if(flash.error){
                        response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,flash.error)
                        delegate.'message'(flash.error)
                    }
                    if(flash.errors){
                        def p = delegate
                        flash.errors.each{ msg ->
                            p.'message'(msg)
                        }
                    }
                }
            }
        }
    }
    def cancelExecution () {
        boolean valid=false
        withForm{
            valid=true
            g.refreshFormTokensHeader()
        }.invalidToken{

        }
        if(!valid){
            response.status=HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            return withFormat {
                json {
                    render(contentType: "application/json") {
                        delegate.cancelled false
                        delegate.error request.error
                    }
                }
                xml {
                    xmlerror()
                }
            }
        }

        ExecutionService.AbortResult abortresult
        try {

            def Execution e = Execution.get(params.id)
            if (!e) {
                log.error("Execution not found for id: " + params.id)
                return withFormat {
                    json {
                        render(contentType: "application/json") {
                            delegate.cancelled false
                            delegate.error "Execution not found for id: " + params.id
                        }
                    }
                    xml {
                        xmlerror()
                    }
                }
            }
            AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, e.project)
            def ScheduledExecution se = e.scheduledExecution
            abortresult = executionService.abortExecution(
                se,
                e,
                session.user,
                authContext,
                null,
                params.forceIncomplete == 'true'
            )
        }catch (DataAccessResourceFailureException ex){
            log.error("Database acces failure, forced job interruption",ex)
            //force interrupt on database problem
            JobExecutionContext jexec = scheduledExecutionService.findExecutingQuartzJob(Long.valueOf(params.id))
            abortresult = new ExecutionService.AbortResult()
            def didCancel = false
            if(jexec){
                didCancel = scheduledExecutionService.interruptJob(
                    jexec.fireInstanceId,
                    jexec.getJobDetail().key.getName(),
                    jexec.getJobDetail().key.getGroup(),
                    true
                )
            }

            abortresult.abortstate = ExecutionService.ABORT_ABORTED
            abortresult.status = "db-error"
        }


        def didcancel=abortresult.abortstate in [ExecutionService.ABORT_ABORTED, ExecutionService.ABORT_PENDING]
        def reasonstr=abortresult.reason


        withFormat{
            json{
                render(contentType:"application/json"){
                    delegate.cancelled didcancel
                    delegate.status(abortresult.status?:(didcancel?'killed':'failed'))
                    delegate.abortstate abortresult.abortstate
                    if(reasonstr){
                        delegate.'reason' reasonstr
                    }
                }
            }
            xml {
                render(contentType:"text/xml",encoding:"UTF-8"){
                    result(error: false, success: didcancel, abortstate: abortresult.abortstate) {
                        success{
                            message("Job status: ${abortresult.status?:(didcancel?'killed': 'failed')}")
                        }
                        if (reasonstr) {
                            reason(reasonstr)
                        }
                    }
                }
            }
        }
    }

    def incompleteExecution (){
        boolean valid=false
        withForm{
            valid=true
            g.refreshFormTokensHeader()
        }.invalidToken{

        }
        if(!valid){
            response.status=HttpServletResponse.SC_BAD_REQUEST
            request.error = g.message(code: 'request.error.invalidtoken.message')
            return withFormat {
                json {
                    render(contentType: "application/json") {
                        delegate.cancelled  false
                        delegate.error request.error
                    }
                }
                xml {
                    xmlerror()
                }
            }
        }

        ExecutionService.AbortResult abortresult
        def Execution e = Execution.get(params.id)
        if (!e) {
            log.error("Execution not found for id: " + params.id)
            return withFormat {
                json {
                    render(contentType: "application/json") {
                        delegate.cancelled  false
                        delegate.error  "Execution not found for id: " + params.id
                    }
                }
                xml {
                    xmlerror()
                }
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, e.project)
        def ScheduledExecution se = e.scheduledExecution
        abortresult = executionService.abortExecution(
            se,
            e,
            session.user,
            authContext,
            null,
            true
        )
        def didcancel=abortresult.abortstate in [ExecutionService.ABORT_ABORTED, ExecutionService.ABORT_PENDING]
        withFormat{
            json{
                render(contentType: "application/json") {
                    delegate.cancelled didcancel
                    delegate.status(abortresult.status ?: (didcancel ? 'killed' : 'failed'))
                    delegate.abortstate abortresult.abortstate
                }
            }
        }

    }
    def downloadOutput() {
        Execution e = Execution.get(Long.parseLong(params.id))
        if(!e){
            log.error("Execution with id "+params.id+" not found")
            flash.error="No Execution found for id: " + params.id
            flash.message="No Execution found for id: " + params.id
            return
        }

        def jobcomplete = e.dateCompleted!=null
        def reader = loggingService.getLogReader(e)
        if (reader.state==ExecutionLogState.NOT_FOUND) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not found")
            return
        }else if (reader.state == ExecutionLogState.ERROR) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            def msg= g.message(code: reader.errorCode, args: reader.errorData)
            log.error("Output file reader error: ${msg}")
            appendOutput(response, msg)
            return
        }else if (reader.state != ExecutionLogState.AVAILABLE) {
            //TODO: handle other states
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not available")
            return
        }
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US);
        def dateStamp= dateFormater.format(e.dateStarted);
        response.setContentType("text/plain")
        if("inline"!=params.view){
            response.setHeader("Content-Disposition","attachment; filename=\"${e.scheduledExecution?e.scheduledExecution.jobName:'adhoc'}-${dateStamp}.txt\"")
        }
        def isFormatted = "true"==servletContext.getAttribute("output.download.formatted")
        if(params.formatted){
            isFormatted = "true"==params.formatted
        }

        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone= TimeZone.getTimeZone("GMT")
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep=System.getProperty("line.separator")
        iterator.each { LogEvent msgbuf ->
            if (msgbuf.eventType != LogUtil.EVENT_TYPE_LOG) {
                return
            }
            def message = msgbuf.message
            if (params.stripansi != 'false' && message.contains('\033[')) {
                try {
                    message=message.decodeAnsiColorStrip()
                } catch (Exception exc) {
                }
            }
            appendOutput(response, (isFormatted?"${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.stepctx?:'_'}][${msgbuf.loglevel}] ${message}" : message))
            appendOutput(response, lineSep)
        }
        iterator.close()
    }

    def renderOutput() {
        Execution e = Execution.get(Long.parseLong(params.id))
        if(!e){
            log.error("Execution with id "+params.id+" not found")
            flash.error="No Execution found for id: " + params.id
            flash.message="No Execution found for id: " + params.id
            return
        }

        def jobcomplete = e.dateCompleted!=null
        def reader = loggingService.getLogReader(e)
        if (reader.state==ExecutionLogState.NOT_FOUND) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not found")
            return
        }else if (reader.state == ExecutionLogState.ERROR) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            def msg= g.message(code: reader.errorCode, args: reader.errorData)
            log.error("Output file reader error: ${msg}")
            appendOutput(response, msg)
            return
        }else if(reader.state == ExecutionLogState.WAITING){
            if(params.reload=='true') {
                response.setContentType("text/html")
                appendOutput(response, '''<html>
                <head>
                <title></title>
                </head>
                <body>
                <div class="container">
                <div class="row">
                <div class="col-sm-12">
                ''')
                appendOutput(response, g.message(code: "execution.html.waiting"))
                appendOutput(response, '''
                </div>
                <script>
                setTimeout(function(){
                   window.location.reload(1);
                }, 5000);
                </script>
                </body>
                </html>
                ''')
            }else{
                response.setStatus(HttpServletResponse.SC_NOT_FOUND)
                log.error("Output file not available")
            }
            return

        } else if (reader.state != ExecutionLogState.AVAILABLE) {
            //TODO: handle other states
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not available")
            return
        }
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US);
        def dateStamp= dateFormater.format(e.dateStarted);

        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone= TimeZone.getTimeZone("GMT")
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep=System.getProperty("line.separator")
        response.setContentType("text/html")
        appendOutput(response, """<html>
<head>
<title></title>
<link rel="stylesheet" href="${g.assetPath(src:'app.less.css')}"  />
<link rel="stylesheet" href="${g.assetPath(src:'ansicolor.css')}"  />
</head>
<body>
<div class="container">
<div class="row">
<div class="col-sm-12">
<div class="ansicolor ansicolor-${(params.ansicolor in ['false','off'])?'off':'on'}" >""")

        def csslevel=!(params.loglevels in ['off','false'])
        def renderContent = shouldConvertContent(params)
        iterator.each{ LogEvent msgbuf ->
            if(msgbuf.eventType != LogUtil.EVENT_TYPE_LOG){
                return
            }
            def message = msgbuf.message
            def msghtml=message.encodeAsHTML()
            boolean converted = false
            if (renderContent && msgbuf.metadata['content-data-type']) {
                //look up content-type
                Map meta = [:]
                msgbuf.metadata.keySet().findAll { it.startsWith('content-meta:') }.each {
                    meta[it.substring('content-meta:'.length())] = msgbuf.metadata[it]
                }
                String result = convertContentDataType(message, msgbuf.metadata['content-data-type'], meta, 'text/html', e.project)
                if (result != null) {
                    if(meta["no-strip"] == "true") {
                        msghtml = result
                    } else {
                        msghtml = result.encodeAsSanitizedHTML()
                    }
                    converted = true
                }
            }
            if (!converted && message.contains('\033[')) {
                try {
                    msghtml = message.decodeAnsiColor()
                } catch (Exception exc) {
                    log.error("Ansi decode error: " + exc.getMessage(), exc)
                }
            }
            def css="log_line" + (csslevel?" level_${msgbuf.loglevel.toString().toLowerCase()}":'')

            appendOutput(response, "<div class=\"$css\" >")
            appendOutput(response, msghtml)
            appendOutput(response, '</div>')

            appendOutput(response, lineSep)
        }
        iterator.close()
        if(jobcomplete || params.reload!='true'){
            appendOutput(response, '''</div>
</div>
</div>
</div>
</body>
</html>
''')
        }else{
            appendOutput(response, '''</div>
</div>
</div>
</div>
<script>
setTimeout(function(){
   window.location.reload(1);
}, 5000);
</script>
</body>
</html>
''')
        }

    }

    /**
     * @param params
     * @return true if configuration/params enable log data content conversion plugins
     */
    private boolean shouldConvertContent(Map params) {
        configurationService.getBoolean(
                'gui.execution.logs.renderConvertedContent',
                true
        ) && !(params.convertContent in ['false', false, 'off'])
    }
    /**
     * API: /api/execution/{id}/output, version 5
     */
    def apiExecutionOutput() {
        if (!apiService.requireVersion(request, response, ApiVersions.V5)) {
            return
        }
        params.stateOutput=false

        if (request.api_version < ApiVersions.V9) {
            params.nodename = null
            params.stepctx = null
        }
        if (request.api_version < ApiVersions.V21) {
            params.remove('compacted')
        }
        return tailExecutionOutput()
    }
    static final String invalidXmlPattern = "[^" + "\\u0009\\u000A\\u000D" + "\\u0020-\\uD7FF" +
            "\\uE000-\\uFFFD" + "\\u10000-\\u10FFFF" + "]+";


    /**
     * Use a builder delegate to render tailExecutionOutput result in XML
     */
    private def renderOutputFormat(String outf, Map data, List outputData, apiVersion, delegate, stateoutput = false) {
        def keys = [
                'id', 'offset', 'completed', 'empty', 'unmodified', 'error', 'message', 'execCompleted',
                'hasFailedNodes', 'execState', 'lastModified', 'execDuration', 'percentLoaded', 'totalSize',
                'lastLinesSupported', 'retryBackoff', 'clusterExec', 'serverNodeUUID',
                'compacted',
        ]
        def setProp={k,v->
            if(outf=='json'){
                delegate."${k}"(v)
            }else{
                delegate."${k}"(v)
            }
        }
        keys.each{
            if(null!=data[it]){
                setProp(it,data[it])
            }
        }
        def filterparms = [:]
        ['nodename', 'stepctx'].each {
            if (data[it]) {
                filterparms[it] = data[it]
            }
        }
        if (filterparms) {
            delegate.filter(filterparms)
        }


        def timeFmt = new SimpleDateFormat("HH:mm:ss")
        def compacted = data.compacted
        def compactedAttr = null
        if (compacted && outf == 'json') {
            setProp('compactedAttr', 'log')
            compactedAttr = 'log'
        }
        def prev = [:]
        List jsonDatamapList = []
        def dataClos= {
            outputData.each {
                def datamap = stateoutput?(it + [
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date: it.time]),
                        log: it.mesg?.replaceAll(/\r?\n$/, ''),
                ]):([
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date: it.time]),
                        log: it.mesg?.replaceAll(/\r?\n$/, ''),
                ]+it.subMap(['level','user','command','stepctx','node']))
                datamap.remove('mesg')
                if (it.loghtml) {
                    datamap.loghtml = it.loghtml
                }
                def removed = []
                if (compacted) {
                    def origmap = new HashMap(datamap)
                    prev.each { k, v ->
                        if (datamap[k] == prev[k]) {
                            datamap.remove(k)
                        } else if (null == datamap[k] && null != prev[k]) {
                            datamap[k] = null
                            removed << k
                        }
                    }
                    prev = origmap
                    if (compactedAttr && datamap.size() == 1 && datamap[compactedAttr] != null) {
                        //compact the single attribute into just the string
                        datamap = datamap[compactedAttr]
                    }
                }
                if (outf == 'json') {
                        jsonDatamapList.add(datamap) //Changes to correct ExecutionControllerSpec."api execution output compacted json" test
                } else {
                    if (datamap instanceof Map) {
                        if (compacted && removed) {
                            datamap['removed'] = removed.join(',')
                            removed.each{datamap.remove(it)}
                        }
                        if(datamap.log!=null) {
                            datamap.log = datamap.log?.replaceAll(invalidXmlPattern, '')
                        }
                        //xml
                        if (apiVersion <= ApiVersions.V5) {
                            def text = datamap.remove('log')
                            delegate.'entry'(datamap, text)
                        } else {
                            delegate.'entry'(datamap)
                        }
                    }
                }
            }
        }
        if(outf=='json'){
            //Changes to correct ExecutionControllerSpec."api execution output compacted json" test
            dataClos()
            delegate.entries(jsonDatamapList)
        }else{
            delegate.entries(dataClos)
        }
    }
    //Create a map that will be converted to JSON by the grails converter at the render phase
    private def renderOutputFormatJson(Map data, List outputData, stateoutput = false) {
        def keys = [
                'id', 'offset', 'completed', 'empty', 'unmodified', 'error', 'message', 'pending', 'execCompleted',
                'hasFailedNodes', 'execState', 'lastModified', 'execDuration', 'percentLoaded', 'totalSize',
                'lastLinesSupported', 'retryBackoff', 'clusterExec', 'serverNodeUUID',
                'compacted',
        ]
        def jsonoutput = [:]

        keys.each{
            if(null!=data[it]){
                jsonoutput[it] = data[it]
            }
        }
        def filterparms = [:]
        ['nodename', 'stepctx'].each {
            if (data[it]) {
                filterparms[it] = data[it]
            }
        }
        if (filterparms) {
            jsonoutput.filter=filterparms
        }


        def timeFmt = new SimpleDateFormat("HH:mm:ss")
        def compacted = data.compacted
        def compactedAttr = null
        if (compacted) {
            jsonoutput.compactedAttr = 'log'
            compactedAttr = 'log'
        }
        def prev = [:]
        List jsonDatamapList = []

        outputData.each {
            def datamap = stateoutput?(it + [
                    time: timeFmt.format(it.time),
                    absolute_time: g.w3cDateValue([date: it.time]),
                    log: it.mesg?.replaceAll(/\r?\n$/, ''),
            ]):([
                        time: timeFmt.format(it.time),
                        absolute_time: g.w3cDateValue([date: it.time]),
                        log: it.mesg?.replaceAll(/\r?\n$/, ''),
                ]+it.subMap(['level','user','command','stepctx','node']))
            datamap.remove('mesg')
            if (it.loghtml) {
                datamap.loghtml = it.loghtml
            }
            def removed = []
            if (compacted) {
                def origmap = new HashMap(datamap)
                prev.each { k, v ->
                    if (datamap[k] == prev[k]) {
                        datamap.remove(k)
                    } else if (null == datamap[k] && null != prev[k]) {
                        datamap[k] = null
                        removed << k
                    }
                }
                prev = origmap
                if (compactedAttr && datamap.size() == 1 && datamap[compactedAttr] != null) {
                    //compact the single attribute into just the string
                    datamap = datamap[compactedAttr]
                }
            }

            jsonDatamapList.add(datamap)

        }

        jsonoutput.entries = jsonDatamapList
        return jsonoutput
    }
    /**
     * API: /api/execution/{id}/output/state, version ?
     */
    def apiExecutionStateOutput() {
        if (!apiService.requireVersion(request,response,ApiVersions.V10)) {
            return
        }
        params.stateOutput = true
        if (request.api_version < ApiVersions.V21) {
            params.remove('compacted')
        }
        return tailExecutionOutput()
    }
    /**
     * tailExecutionOutput action, used by execution/show.gsp view to display output inline
     * Also used by apiExecutionOutput for API response
     */
    def tailExecutionOutput () {
        log.debug("tailExecutionOutput: ${params}, format: ${request.format}")
        Execution e = Execution.get(Long.parseLong(params.id))
        if(!apiService.requireExists(response,e,['Execution',params.id])){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e?.project)
        def reqError=false

        def apiError = { String code, List args, int status = 0 ->
            def message=code?g.message(code:code,args:args):'Unknown error'
            withFormat {
                xml {
                    apiService.renderErrorXml(response,[code:code,args:args,status:status])
                }
                json {
                    if (status > 0) {
                        response.setStatus(status)
                    }
                    def err = [
                            error: message,
                            id: params.id.toString(),
                            offset: "0",
                            completed: false
                            ]
                    render err as JSON
                }
                text {
                    if (status > 0) {
                        response.setStatus(status)
                    }
                    render(contentType: "text/plain", text: message)
                }
            }
        }
        if(!e){
            return apiError('api.error.item.doesnotexist', ['execution', params.id], HttpServletResponse.SC_NOT_FOUND);
        }
        if(e && !frameworkService.authorizeProjectExecutionAny(authContext,e,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW])){
            return apiError('api.error.item.unauthorized', [AuthConstants.ACTION_VIEW, "Execution", params.id], HttpServletResponse.SC_FORBIDDEN);
        }
        if (params.stepctx && !(params.stepctx ==~ /^(\d+e?(@.+?)?\/?)+$/)) {
            return apiError("api.error.parameter.invalid",[params.stepctx,'stepctx',"Invalid stepctx filter"],HttpServletResponse.SC_BAD_REQUEST)
        }

        def jobcomplete = e.dateCompleted != null
        def hasFailedNodes = e.failedNodeList ? true : false
        def execState = e.executionState
        def statusString = e.customStatusString
        def execDuration = 0L
        execDuration = (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted
                                                                                                     .getTime()

        def isClusterExec = frameworkService.isClusterModeEnabled() && e.serverNodeUUID !=
                frameworkService.getServerUUID()
        def clusterInfo = [
                serverNodeUUID: e.serverNodeUUID,
                clusterExec   : isClusterExec
        ]
        def compacted = params.compacted == 'true' &&
                !configurationService.getBoolean('gui.execution.logs.compacted.disabled', false)

        ExecutionLogReader reader
        reader = loggingService.getLogReader(e)
        def error = reader.state == ExecutionLogState.ERROR
        log.debug("Reader, state: ${reader.state}, reader: ${reader.reader}")
        if(error) {
            return apiError(reader.errorCode, reader.errorData, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (null == reader  || reader.state == ExecutionLogState.NOT_FOUND ) {
            def errmsg = g.message(code: "execution.log.storage.state.NOT_FOUND")
            //execution has not be started yet
            def dataMap= [
                    empty         : true,
                    id            : params.id.toString(),
                    offset        : "0",
                    completed     : jobcomplete,
                    execCompleted : jobcomplete,
                    hasFailedNodes: hasFailedNodes,
                    execState     : execState,
                    statusString  : statusString,
                    execDuration  : execDuration
            ] + clusterInfo
            if (e.dateCompleted) {
                dataMap.error=errmsg
            } else {
                dataMap.message=errmsg
            }
            withFormat {
                xml {
                    apiService.renderSuccessXml(request,response) {
                        output{
                            renderOutputFormat('xml', dataMap, [], request.api_version, delegate)
                        }
                    }
                }
                json {
                    render renderOutputFormatJson(dataMap,[]) as JSON
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Offset', "0")
                    if (e.dateCompleted) {
                        response.addHeader('X-Rundeck-ExecOutput-Error', errmsg.toString())
                    } else {
                        response.addHeader('X-Rundeck-ExecOutput-Message', errmsg.toString())
                    }
                    response.addHeader('X-Rundeck-ExecOutput-Empty', dataMap.empty.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.completed.toString())
                    response.addHeader('X-Rundeck-Exec-Completed', dataMap.execCompleted.toString())
                    response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                    response.addHeader('X-Rundeck-Exec-Status-String', dataMap.statusString.toString())
                    response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
                    render(contentType: "text/plain") {
                        ''
                    }
                }
            }
            return;
        }
        else if (null == reader || reader.state in [ExecutionLogState.PENDING_LOCAL, ExecutionLogState.PENDING_REMOTE, ExecutionLogState.WAITING]) {
            //pending data
            def dataMap=[
                    message       :"Pending",
                    pending       : g.message(code: 'execution.log.storage.state.' + reader.state, default: "Pending"),
                    id            :params.id.toString(),
                    offset        : params.offset ? params.offset.toString() : "0",
                    completed     :false,
                    execCompleted :jobcomplete,
                    hasFailedNodes:hasFailedNodes,
                    execState     :execState,
                    statusString  : statusString,
                    execDuration  : execDuration,
                    retryBackoff  : reader.retryBackoff
            ] + clusterInfo
            withFormat {
                xml {
                    apiService.renderSuccessXml(request,response) {
                        output {
                            renderOutputFormat('xml', dataMap, [], request.api_version, delegate)
                        }
                    }
                }
                json {
                    render renderOutputFormatJson(dataMap,[]) as JSON
                }
                text {
                    response.addHeader('X-Rundeck-ExecOutput-Message', dataMap.message.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Pending', dataMap.pending.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Offset', dataMap.offset.toString())
                    response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.completed.toString())
                    response.addHeader('X-Rundeck-Exec-Completed', dataMap.execCompleted.toString())
                    response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                    response.addHeader('X-Rundeck-Exec-Status-String', dataMap.statusString?.toString())
                    response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
                    response.addHeader('X-Rundeck-ExecOutput-RetryBackoff', dataMap.retryBackoff.toString())
                    render(contentType: "text/plain") {
                        ''
                    }
                }
            }
            return
        }
        def StreamingLogReader logread=reader.reader

        def Long offset = 0
        if(params.offset){

            try {
                offset= Long.parseLong(params.offset)
            } catch (NumberFormatException exc) {
                reqError = true
            }
            if(offset<0){
                reqError=true
            }
            if(reqError){
                return apiError('api.error.parameter.invalid', [params.offset, 'offset', 'Not an integer offset'],
                        HttpServletResponse.SC_BAD_REQUEST)
            }
        }

        def totsize = logread.getTotalSize()
        long lastmodl = logread.lastModified?.time
        long reqlastmod=0

        if(params.lastmod && lastmodl>0){
            def ll = 0
            if (params.lastmod) {

                try {
                    ll = Long.parseLong(params.lastmod)
                } catch (NumberFormatException exc) {
                    reqError = true
                }
                if (ll < 0) {
                    reqError = true
                }
                if (reqError) {
                    return apiError('api.error.parameter.invalid',
                            [params.lastmod, 'lastmod', 'Not a millisecond modification time'],
                            HttpServletResponse.SC_BAD_REQUEST)
                }
            }
            reqlastmod=ll

            if (lastmodl <= ll && (offset==0 || totsize <= offset)) {
                def dataMap=[
                        message       : "Unmodified",
                        unmodified    : true,
                        id            : params.id.toString(),
                        offset        : params.offset ? params.offset.toString() : "0",
                        completed     : reader.state == ExecutionLogState.AVAILABLE,
                        execCompleted : jobcomplete,
                        hasFailedNodes:hasFailedNodes,
                        execState     : execState,
                        statusString  : statusString,
                        lastModified  : lastmodl.toString(),
                        execDuration  : execDuration,
                        totalSize     : totsize,
                        retryBackoff  : reader.retryBackoff
                ] + clusterInfo

                withFormat {
                    xml {
                        apiService.renderSuccessXml(request,response) {
                            output {
                                renderOutputFormat('xml', dataMap, [], request.api_version, delegate)
                            }
                        }
                    }
                    json {
                        render renderOutputFormatJson(dataMap,[]) as JSON
                    }
                    text {
                        response.addHeader('X-Rundeck-ExecOutput-Message', dataMap.message.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Unmodified', dataMap.unmodified.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Offset', dataMap.offset.toString())
                        response.addHeader('X-Rundeck-ExecOutput-Completed', dataMap.completed.toString())
                        response.addHeader('X-Rundeck-Exec-Completed', dataMap.execCompleted.toString())
                        response.addHeader('X-Rundeck-Exec-State', dataMap.execState.toString())
                        response.addHeader('X-Rundeck-Exec-Status-String', dataMap.statusString?.toString())
                        response.addHeader('X-Rundeck-Exec-Duration', dataMap.execDuration.toString())
                        response.addHeader('X-Rundeck-ExecOutput-LastModifed', dataMap.lastModified.toString())
                        response.addHeader('X-Rundeck-ExecOutput-TotalSize', dataMap.totalSize.toString())
                        response.addHeader('X-Rundeck-ExecOutput-RetryBackoff', dataMap.retryBackoff.toString())
                        render(contentType: "text/plain") {
                            ''
                        }
                    }
                }
                return
            }
        }
        def storeoffset=offset
        def entry=[]
        def completed=false
        def max= 0
        def lastlinesSupported= (ReverseSeekingStreamingLogReader.isInstance(logread))
        def lastlines = params.long('lastlines',0)
        if(lastlines && lastlinesSupported){
            def ReverseSeekingStreamingLogReader reversing= (ReverseSeekingStreamingLogReader) logread
            reversing.openStreamFromReverseOffset(lastlines)
            //load only the last X lines of the file, by going to the end and searching backwards for the
            max=lastlines+1
        }else{
            logread.openStream(offset)
            max = Math.max(0,params.int('maxlines',0))
        }

        def String bufsizestr= servletContext.getAttribute("execution.follow.buffersize");
        def Long bufsize= (bufsizestr?bufsizestr.toInteger():0);
        if(bufsize<(25*1024)){
            bufsize=25*1024
        }
        def stateoutput = params.stateOutput in [true,'true']
        def stateonly = params.stateOnly in [true,'true']
        boolean paramStepCtxIdentAllowSub = params.stepctx? params.stepctx.endsWith('/'):false
        StepIdentifier paramStepCtxIdent = params.stepctx?StateUtils.stepIdentifierFromString(params.stepctx):null
        def filter={ LogEvent data ->
            if (!stateoutput && data.eventType != LogUtil.EVENT_TYPE_LOG) {
                return false
            }
            if (stateoutput && stateonly && data.eventType == LogUtil.EVENT_TYPE_LOG) {
                return false
            }
            if(params.nodename && data.metadata.node != params.nodename){
                return false
            }
            if(paramStepCtxIdent){
                def evtIdent= StateUtils.stepIdentifierFromString(data.metadata.stepctx)
                if(evtIdent!=null && StateUtils.isMatchedIdentifier(paramStepCtxIdent,evtIdent, paramStepCtxIdentAllowSub)){
                    return data
                }else{
                    return false
                }
            }
            data
        }
        for(LogEvent data : logread){
            if(!filter(data)){
                continue
            }
            log.debug("read stream event: ${data}")
            def logdata= (data.metadata ?: [:]) + [mesg: data.message, time: data.datetime, level: data.loglevel.toString(),type:data.eventType]
            entry<<logdata
            if (!(0 == max || entry.size() < max)){
                break
            }
        }
        storeoffset= logread.offset
        //not completed if the reader state is only AVAILABLE_PARTIAL
        completed = reader.state == ExecutionLogState.AVAILABLE &&
                (logread.complete || (jobcomplete && storeoffset == totsize))
        log.debug("finish stream iterator, offset: ${storeoffset}, completed: ${completed}")
        if (storeoffset == offset) {
            //don't change last modified unless new data has been read
            lastmodl = reqlastmod
        }

//        if("true" == servletContext.getAttribute("output.ansicolor.enabled") || params.ansicolor=='true'){
            entry.each {
                if (it.mesg.contains('\033[')) {
                    try {
                        it.loghtml = it.mesg.decodeAnsiColor()
                        it.mesg = it.mesg.decodeAnsiColorStrip()
                    } catch (Exception exc) {
                        log.error("Markdown error: " + exc.getMessage(), exc)
                    }
                }
            }
//        }
        if (shouldConvertContent(params)) {
            //interpret any log content

            entry.each {logentry->
                if (logentry.mesg && logentry['content-data-type']) {
                    //look up content-type
                    Map meta = [:]
                    logentry.keySet().findAll{it.startsWith('content-meta:')}.each{
                        meta[it.substring('content-meta:'.length())]=logentry[it]
                    }
                    String result = convertContentDataType(
                            logentry.mesg,
                            logentry['content-data-type'],
                            meta,
                            'text/html',
                            e.project
                    )
                    if (result != null) {
                        if(meta["no-strip"] == "true") {
                            logentry.loghtml = result
                        } else {
                            logentry.loghtml = result.encodeAsSanitizedHTML()
                        }
                    }
                }
            }
        }
        if("true" == servletContext.getAttribute("output.markdown.enabled") && !params.disableMarkdown){
            entry.each{
                if(it.mesg){
                    try{
                        it.loghtml = it.mesg.decodeMarkdown()
                    }catch (Exception exc){
                        log.error("Markdown error: "+exc.getMessage(),exc)
                    }
                }
            }
        }else if (params.markdown=='group'){
            def ctx=[:]
            def newe=[]
            def buf=[]
            entry.each {et->
                if(et.stepctx!=ctx.stepctx || et.node!=ctx.node){
                    if (newe){
                        //push buf
                        ctx.loghtml=buf.join("\n").decodeMarkdown()
                        buf = []
                    }
                    ctx = et
                    newe<< et
                }
                buf<< et.mesg
            }
            ctx.loghtml = buf.join("\n").decodeMarkdown()
            entry=newe
        }
        long marktime=System.currentTimeMillis()
        def percent=100.0 * (((float)storeoffset)/((float)totsize))
        log.debug("percent: ${percent}, store: ${storeoffset}, total: ${totsize} lastmod : ${lastmodl}")

        def resultData= [
                id: e.id.toString(),
                offset            : storeoffset.toString(),
                completed         : completed,
                execCompleted     : jobcomplete,
                hasFailedNodes    : hasFailedNodes,
                execState         : execState,
                statusString      : statusString,
                lastModified      : lastmodl.toString(),
                execDuration      : execDuration,
                percentLoaded     : percent,
                totalSize         : totsize,
                lastlinesSupported: lastlinesSupported,
                nodename          :params.nodename,
                stepctx           : params.stepctx,
                retryBackoff      : reader.retryBackoff,
                compacted         : compacted
        ] + clusterInfo
        withFormat {
            xml {
                apiService.renderSuccessXml(request,response) {
                    output {
                        renderOutputFormat('xml', resultData, entry, request.api_version, delegate, stateoutput)
                    }
                }
            }
            json {
                render renderOutputFormatJson(resultData,entry,stateoutput) as JSON
            }
            text{
                response.addHeader('X-Rundeck-ExecOutput-Offset', storeoffset.toString())
                response.addHeader('X-Rundeck-ExecOutput-Completed', completed.toString())
                response.addHeader('X-Rundeck-Exec-Completed', jobcomplete.toString())
                response.addHeader('X-Rundeck-Exec-State', execState.toString())
                response.addHeader('X-Rundeck-Exec-Status-String', statusString?.toString()?:'')
                response.addHeader('X-Rundeck-Exec-Duration', execDuration.toString())
                response.addHeader('X-Rundeck-ExecOutput-LastModifed', lastmodl.toString())
                response.addHeader('X-Rundeck-ExecOutput-TotalSize', totsize.toString())
                response.addHeader('X-Rundeck-ExecOutput-LastLinesSupported', lastlinesSupported.toString())
                response.addHeader('X-Rundeck-ExecOutput-RetryBackoff', reader.retryBackoff.toString())
                def lineSep = System.getProperty("line.separator")
                response.setHeader("Content-Type","text/plain")

                
                entry.each{
                    appendOutput(response, it.mesg+lineSep)
                }
            }
        }
    }

    //TODO: move to a service
    @PackageScope
    String convertContentDataType(final Object input, final String inputDataType, Map<String,String> meta, final String outputType, String projectName) {
//        log.error("find converter : ${input.class}(${inputDataType}) => ?($outputType)")
        def plugins = listViewPlugins()

        def isPluginEnabled = executionService.getFrameworkService().
            getPluginControlService(projectName).
            enabledPredicateForService(ServiceNameConstants.ContentConverter)

        plugins = plugins.findAll { isPluginEnabled.test(it.key) }

        List<DescribedPlugin<ContentConverterPlugin>> foundPlugins = findOutputViewPlugins(
            plugins,
            inputDataType,
            input.class
        )
        def chain = []

        def resultPlugin = foundPlugins.find {
            it.instance.getOutputDataTypeForContentDataType(input.class, inputDataType) == outputType
        }

        //attempt to resolve the data type
        if (!resultPlugin) {
//            log.error("not found converter : ${input.class}(${inputDataType}) => $outputType, searching ${foundPlugins.size()} plugins...")
            foundPlugins.find {
                def otype = it.instance.getOutputDataTypeForContentDataType(input.class, inputDataType)
                def oclass = it.instance.getOutputClassForDataType(input.class, inputDataType)
                def results = findOutputViewPlugins(plugins, otype, oclass)

//                log.error("search subconverter :<${it.name}>  ${oclass}($otype) => ${results}")
                def plugin2 = results.find {
                    it.instance.getOutputDataTypeForContentDataType(oclass, otype) == outputType
                }
                if (plugin2) {
//                    log.error(
//                            " found converter :<${it.name}> ${input.class}(${inputDataType}) => ${oclass}($otype)"
//                    )
//                    log.error(" found converter :<${plugin2.name}> ${oclass}(${otype}) => ?($outputType)")
                    chain = [it, plugin2]
                }else{

//                    log.error("not found subconverter :<${it.name}> ${input.class}(${inputDataType}) => ${oclass}($otype) => ${outputType}")
                }
                //end loop when found
                plugin2 != null
            }
        } else {
            chain = [resultPlugin]
        }
//        log.error("chain: "+chain)
        if (chain) {
            def ovalue = input
            def otype = inputDataType
            try {
                chain.each { plugin ->
                    def nexttype = plugin.instance.getOutputDataTypeForContentDataType(ovalue.getClass(), otype)
                    ovalue = plugin.instance.convert(ovalue, otype, meta)
                    otype = nexttype
                }
            } catch (PluginDisabledException disabledException){
                log.error(
                        "Failed converting data type ${input.getClass()}($inputDataType)  with plugins: ${chain*.name}",
                        disabledException
                )
            } catch (Throwable t) {
                log.error(
                        "Failed converting data type ${input.getClass()}($inputDataType)  with plugins: ${chain*.name}",
                        t
                )
            }
            return ovalue
        }

        return null
    }

    private List<DescribedPlugin<ContentConverterPlugin>> findOutputViewPlugins(
            Map<String, DescribedPlugin<ContentConverterPlugin>> plugins,
            String inputDataType,
            Class clazz
    )
    {
        def foundPlugins = plugins.entrySet().findAll {
            it.value.instance.isSupportsDataType(clazz, inputDataType)
        }.collect { it.value }
        foundPlugins
    }
    Map<String, DescribedPlugin<ContentConverterPlugin>> viewPluginsMap = [:]

    private Map<String, DescribedPlugin<ContentConverterPlugin>> listViewPlugins() {
        if (!viewPluginsMap) {
            viewPluginsMap = pluginService.listPlugins(ContentConverterPlugin)
        }
        viewPluginsMap
    }

    /**
     * API compatible Delete bulk action requiring form token
     * @return
     */
    def deleteBulkApi() {
        withForm{
            g.refreshFormTokensHeader()
            executionDeleteBulk()
        }.invalidToken{
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'request.error.invalidtoken.message',
            ])
        }
    }


    /**
    * API actions
     */

    public String createExecutionUrl(def id,def project) {
        return g.createLink(controller: 'execution', action: 'follow', id: id, absolute: true,
                params: [project: project])
    }
    public String createServerUrl() {
        return g.createLink(controller: 'menu', action: 'index', absolute: true)
    }

    /**
     * API: /api/execution/{id} , version 1
     */
    def apiExecution(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        def Execution e = Execution.get(params.id)
        if(!apiService.requireExists(response,e,['Execution ID',params.id])){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)
        if(!apiService.requireAuthorized(
                frameworkService.authorizeProjectExecutionAny(authContext,e,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW]),
                response,
                [AuthConstants.ACTION_VIEW, "Execution", params.id] as Object[])){
            return
        }

        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response, [e])
            }
            json{
                return executionService.respondExecutionsJson(request,response, [e],[single:true])
            }
        }
    }
    /**
     * API: /api/execution/{id}/state , version 10
     */
    def apiExecutionState(){
        if (!apiService.requireVersion(request, response, ApiVersions.V10)) {
            return
        }
        def Execution e = Execution.get(params.id)

        if(!apiService.requireExists(response,e,['Execution ID',params.id])){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)
        if(!apiService.requireAuthorized(
                frameworkService.authorizeProjectExecutionAny(authContext,e,[AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW]),
                response,
                [AuthConstants.ACTION_VIEW, "Execution", params.id] as Object[])){
            return
        }


        def loader = workflowService.requestState(e)
        def state= loader.workflowState
        if(!loader.workflowState){
            if(loader.state in [ExecutionLogState.WAITING, ExecutionLogState.AVAILABLE_REMOTE,
                    ExecutionLogState.PENDING_LOCAL, ExecutionLogState.PENDING_REMOTE]) {
                state = [error: 'pending']
            }else{
                def errormap=[:]
                if (loader.state in [ExecutionLogState.NOT_FOUND]) {
                    errormap = [status: HttpServletResponse.SC_NOT_FOUND, code: "api.error.item.doesnotexist",
                            args: ['Execution State ID', params.id]]
                }else {
                    errormap = [status: HttpServletResponse.SC_NOT_FOUND, code: loader.errorCode, args: loader.errorData]
                }
                    withFormat {
                        json {
                            return apiService.renderErrorJson(response, errormap)
                        }
                        xml {
                            return apiService.renderErrorXml(response, errormap)
                        }
                    }
                return
            }
        }
        def convertNodeList={Collection tnodes->
            def tnodemap = []
            tnodes.each { anode ->
                if(anode instanceof String){
                    tnodemap << [(BuilderUtil.ATTR_PREFIX + 'name'): anode]
                }else if(anode instanceof Map.Entry){
                    tnodemap << [(BuilderUtil.ATTR_PREFIX + 'name'): anode.key] + anode.value
                }
            }
            tnodemap
        }
        def convertXml;
        convertXml={Map map->
            Map newmap=[:]+map
            //for each step
            newmap.steps=map.steps.collect{Map step->
                Map newstep=[:] + step
                if(step.workflow){
                    //convert sub workflow
                    newstep.workflow=convertXml(newstep.workflow)
                }
                newstep[BuilderUtil.asAttributeName('stepctx')]= newstep.remove('stepctx')
                BuilderUtil.makeAttribute(newstep,'id')
                if (step.nodeStates) {
                    newstep.nodeStates = step['nodeStates'].collect { String node, Map nodeState ->
                        def nmap = [name: node] + nodeState
                        BuilderUtil.makeAttribute(nmap, 'name')
                        nmap
                    }
                    BuilderUtil.makePlural(newstep, 'nodeStates')
                }
                if (step.stepTargetNodes) {
                    newstep.stepTargetNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(step['stepTargetNodes'])]
                }
                newstep.remove('parameterStates')
                newstep
            }
            if(newmap.steps){
                //make steps into a <steps><step/><step/>..</steps>
                BuilderUtil.makePlural(newmap,'steps')
            }
            if (map.targetNodes) {
                newmap.targetNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(map['targetNodes'])]
            }
            if (map.allNodes) {
                newmap.allNodes = [(BuilderUtil.pluralize('nodes')):convertNodeList(map['allNodes'])]
            }
            if (map.nodes) {
                def nodesteps = [:]
                newmap.remove('nodes').each{
                    nodesteps[(it.key)]= [(BuilderUtil.pluralize('steps')): it.value]
                }
                newmap[(BuilderUtil.pluralize('nodes'))] = convertNodeList(nodesteps.entrySet())
            }
            newmap
        }
        withFormat {
            json{
                return render(contentType: "application/json", encoding: "UTF-8",text:state.encodeAsJSON())
            }
            xml{
                return render(contentType: "text/xml", encoding: "UTF-8") {
                    result(success: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
                        executionState(id:params.id){
                            new BuilderUtil().mapToDom(convertXml(state), delegate)
                        }
                    }
                }
            }
        }
    }

    /**
     * API: /api/execution/{id}/abort, version 1
     */
    def apiExecutionAbort(){
        if (!apiService.requireApi(request, response)) {
            return
        }
//        ExecutionService.AbortResult abortresult
        def Execution e
        ExecutionService.AbortResult abortresult
        try {
            e = Execution.get(params.id)
            if (!apiService.requireExists(response, e, ['Execution ID', params.id])) {
                return
            }
            AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, e.project)
            if (!apiService.requireAuthorized(
                frameworkService.authorizeProjectExecutionAll(authContext, e, [AuthConstants.ACTION_KILL]),
                response,
                [AuthConstants.ACTION_KILL, "Execution", params.id] as Object[]
            )) {
                return
            }

        def ScheduledExecution se = e.scheduledExecution
        def user=session.user
        def killas=null
        if (params.asUser && apiService.requireVersion(request,response,ApiVersions.V5)) {
            //authorized within service call
            killas= params.asUser
        }
        abortresult = executionService.abortExecution(
                se,
                e,
                user,
                authContext,
                killas,
                params.forceIncomplete == 'true'
            )
        }catch (DataAccessResourceFailureException ex){
            log.error("Database acces failure, forced job interruption",ex)
            //force interrupt on database problem
            JobExecutionContext jexec = scheduledExecutionService.findExecutingQuartzJob(Long.valueOf(params.id))
            abortresult = new ExecutionService.AbortResult()
            def didCancel = false
            if(jexec){
                didCancel = scheduledExecutionService.interruptJob(
                    jexec.fireInstanceId,
                    jexec.getJobDetail().key.getName(),
                    jexec.getJobDetail().key.getGroup(),
                    true
                )
            }

            abortresult.abortstate = didCancel?ExecutionService.ABORT_ABORTED:ExecutionService.ABORT_PENDING
            throw ex
        }

        def reportstate=[status: abortresult.abortstate]
        if(abortresult.reason){
            reportstate.reason= abortresult.reason
        }

        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorXml(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        withFormat{
            xml{
                apiService.renderSuccessXml(request,response) {
                    if (apiService.doWrapXmlResponse(request)) {
                        success {
                            delegate.'message'("Execution status: ${abortresult.status ?: abortresult.jobstate}")
                        }
                    }
                    abort(reportstate) {
                        execution(id: params.id, status: abortresult.jobstate,
                                  href:apiService.apiHrefForExecution(e),
                                  permalink: apiService.guiHrefForExecution(e))
                    }
                }
            }
            json{
                apiService.renderSuccessJson(response) {
                    abort=reportstate
                    execution=[
                            id: params.id,
                            status: abortresult.jobstate,
                            href:apiService.apiHrefForExecution(e),
                            permalink: apiService.guiHrefForExecution(e)
                    ]
                }
            }
        }
    }
    /**
     * DELETE /api/12/execution/[ID]
     * @return
     */
    def apiExecutionDelete (){
        if (!apiService.requireVersion(request, response, ApiVersions.V12)) {
            return
        }
        def Execution e = Execution.get(params.id)
        if(!apiService.requireExists(response,e,['Execution ID',params.id])){
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,e.project)
        if(!apiService.requireAuthorized(
                frameworkService.authorizeApplicationResourceAny(
                        authContext,
                        frameworkService.authResourceForProject(e.project),
                        [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
                ),
                response,
                [AuthConstants.ACTION_DELETE_EXECUTION, "Project", e.project] as Object[])){
            return
        }
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'])

        def result = executionService.deleteExecution(e, authContext,session.user)
        if(!result.success){
            log.error("Failed to delete execution: ${result.message}")
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            code: "api.error.exec.delete.failed",
                            args: [params.id, result.message],
                            format: respFormat
                    ])
        }
        return render(status: HttpServletResponse.SC_NO_CONTENT)
    }

    /**
     * Delete bulk API action
     * @return
     */
    def apiExecutionDeleteBulk() {
        if (!apiService.requireVersion(request, response, ApiVersions.V12)) {
            return
        }
        return executionDeleteBulk()
    }
    /**
     * Delete bulk
     * @return
     */
    private def executionDeleteBulk() {
        log.debug("executionController: apiExecutionDeleteBulk : params: " + params)
        def ids=[]
        if(request.format in ['json','xml']){
            def errormsg = "Format was not valid."
            def parsed =apiService.parseJsonXmlWith(request,response,[
                    json: { data ->
                        if(data instanceof List) {
                            ids = data
                        }else{
                            ids = data.ids
                        }
                        if (!ids) {
                            errormsg += " json: expected list of strings, or object with 'ids' property"
                        }
                    },
                    xml: { xml ->
                        def executions= xml.execution
                        ids = executions?executions.collect{it.'@id'.text()}:null
                        if (!ids) {
                            errormsg += " xml: expected 'executions/execution/@id' attributes"
                        }
                    }
            ])
            if(!parsed){
                return
            }
            if(!ids){
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.invalid.request',
                        args: [errormsg]
                ])
            }
        }else if (!apiService.requireParameters(params, response, ['ids'])) {
            return
        }else{
            //params
            ids = params.ids
            if (ids instanceof String) {
                ids = params.ids.split(',') as List
            }
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        //XXX:TODO use project specific auth context
        def result=executionService.deleteBulkExecutionIds([ids].flatten(), authContext, session.user)
        executionService.renderBulkExecutionDeleteResult(request,response,result)
    }


    /**
     * API: /api/14/project/NAME/executions
     */
    def apiExecutionsQueryv14(ExecutionQuery query){
        if(!apiService.requireVersion(request,response,ApiVersions.V14)){
            return
        }
        return apiExecutionsQuery(query)
    }

    /**
     * API: /api/5/executions query interface, deprecated since v14
     */
    def apiExecutionsQuery(ExecutionQuery query){
        if (!apiService.requireVersion(request, response, ApiVersions.V5)) {
            return
        }
        if(query?.hasErrors()){
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.error",
                            args: [query.errors.allErrors.collect { message(error: it) }.join("; ")]
                    ])
        }
        if (!params.project) {
            return apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.parameter.required",
                            args: ['project']
                    ])
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        query.projFilter=params.project
        if (null != query) {
            query.configureFilter()
        }

        //attempt to parse/bind "end" and "begin" parameters
        if (params.begin) {
            try {
                query.endafterFilter = ReportsController.parseDate(params.begin)
                query.doendafterFilter = true
            } catch (ParseException e) {
                return apiService.renderErrorFormat(
                        response,
                        [
                                status: HttpServletResponse.SC_BAD_REQUEST,
                                code: 'api.error.history.date-format',
                                args: ['begin', params.begin]
                        ]
                )
            }
        }
        if (params.olderFilter) {
            Date endDate=ExecutionQuery.parseRelativeDate(params.olderFilter)
            if(null!=endDate){
                query.endbeforeFilter = endDate
                query.doendbeforeFilter = true
            } else {
                return apiService.renderErrorFormat(
                        response,
                        [
                                status: HttpServletResponse.SC_BAD_REQUEST,
                                code: 'api.error.history.date-relative-format',
                                args: ['olderFilter', params.olderFilter]
                        ]
                )
            }
        }else if (params.end) {
            try {
                query.endbeforeFilter = ReportsController.parseDate(params.end)
                query.doendbeforeFilter = true
            } catch (ParseException e) {
                return apiService.renderErrorFormat(
                        response,
                        [
                                status: HttpServletResponse.SC_BAD_REQUEST,
                                code: 'api.error.history.date-format',
                                args: ['end', params.end]
                        ]
                )
            }
        }

        if (request.api_version < ApiVersions.V20 && query.executionTypeFilter) {
            //ignore
            query.executionTypeFilter = null
        }
        def resOffset = params.offset ? params.int('offset') : 0
        def resMax = params.max ?
                params.int('max') :
                grailsApplication.config.rundeck?.pagination?.default?.max ?
                        grailsApplication.config.rundeck.pagination.default.max.toInteger() :
                        20
        def results = executionService.queryExecutions(query, resOffset, resMax)
        def result=results.result
        def total=results.total
        //filter query results to READ authorized executions
        def filtered = frameworkService.filterAuthorizedProjectExecutionsAll(authContext,result,[AuthConstants.ACTION_READ])

        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response,filtered,[total:total,offset:resOffset,max:resMax])
            }
            json{
                return executionService.respondExecutionsJson(request,response,filtered,[total:total,offset:resOffset,max:resMax])
            }
        }
    }


    /**
     *
     * @return
     */
    def apiExecutionModeActive() {
        apiExecutionMode(true)
    }
    /**
     *
     * @return
     */
    def apiExecutionModePassive() {
        apiExecutionMode(false)
    }
    /**
     *
     * @return
     */
    private def apiExecutionMode(boolean active) {
        if (!apiService.requireVersion(request, response, ApiVersions.V14)) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'])

        def authAction = active?AuthConstants.ACTION_ENABLE_EXECUTIONS:AuthConstants.ACTION_DISABLE_EXECUTIONS
        if (!frameworkService.authorizeApplicationResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_SYSTEM,
                [authAction, AuthConstants.ACTION_ADMIN]
            )
        ) {
            return apiService.renderErrorFormat(response,
                                                [
                                                        status: HttpServletResponse.SC_FORBIDDEN,
                                                        code: "api.error.item.unauthorized",
                                                        args: [authAction, "Rundeck", ''],
                                                        format: respFormat
                                                ])
        }
        executionService.setExecutionsAreActive(active)
        withFormat{
            json {
                render(contentType: "application/json") {
                    delegate.executionMode (active?'active':'passive')
                }
            }
            xml {
                render(contentType: "application/xml") {
                    delegate.'executions'(executionMode:active?'active':'passive')
                }
            }
        }
    }

    /**
     * List input files for an execution
     */
    def apiExecutionInputFiles() {
        if (!apiService.requireVersion(request, response, ApiVersions.V19)) {
            return
        }
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def Execution e = Execution.get(params.id)
        if (!apiService.requireExists(response, e, ['Execution ID', params.id])) {
            return
        }
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, e.project)
        if (!apiService.requireAuthorized(
                frameworkService.authorizeProjectExecutionAny(authContext, e, [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW]),
                response,
                [AuthConstants.ACTION_VIEW, "Execution", params.id] as Object[]
        )) {
            return
        }

        def inputFiles = fileUploadService.findRecords(e, FileUploadService.RECORD_TYPE_OPTION_INPUT)

        respond(
                new ExecutionFileInfoList(inputFiles.collect { new JobFileInfo(it.exportMap()) }, [:]),
                [format: ['xml', 'json']]
        )
    }

    /**
     * API: /api/28/executions/metrics
     */
    def apiExecutionMetrics(ExecutionQuery query) {
        if (!apiService.requireVersion(request, response, ApiVersions.V29)) {
            return
        }

        if (query?.hasErrors()) {
            return apiService.renderErrorFormat(response,
                [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : "api.error.parameter.error",
                    args  : [query.errors.allErrors.collect { message(error: it) }.join("; ")]
                ])
        }

        AuthContext authContext;

        if(params.project) {
            authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
            query.projFilter = params.project
        }
        else {
            authContext = frameworkService.getAuthContextForSubject(session.subject)
        }

        if (null != query) {
            query.configureFilter()

            if (params.recentFilter && !query.recentFilter) {
                //invald recentFilter input
                return apiService.renderErrorFormat(
                    response,
                    [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.history.date-relative-format',
                        args  : ['recentFilter', params.recentFilter]
                    ]
                )
            }
        }

        //attempt to parse/bind "end" and "begin" parameters
        if (params.begin) {
            try {
                query.endafterFilter = ReportsController.parseDate(params.begin)
                query.doendafterFilter = true
            } catch (ParseException e) {
                return apiService.renderErrorFormat(
                    response,
                    [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.history.date-format',
                        args  : ['begin', params.begin]
                    ]
                )
            }
        }

        if (params.olderFilter) {
            Date endDate = ExecutionQuery.parseRelativeDate(params.olderFilter)
            if (null != endDate) {
                query.endbeforeFilter = endDate
                query.doendbeforeFilter = true
            } else {
                return apiService.renderErrorFormat(
                    response,
                    [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.history.date-relative-format',
                        args  : ['olderFilter', params.olderFilter]
                    ]
                )
            }
        } else if (params.end) {
            try {
                query.endbeforeFilter = ReportsController.parseDate(params.end)
                query.doendbeforeFilter = true
            } catch (ParseException e) {
                return apiService.renderErrorFormat(
                    response,
                    [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.history.date-format',
                        args  : ['end', params.end]
                    ]
                )
            }
        }

        def resOffset = params.int('offset', 0)
        def resMax = params.int('max', -1)

        def results = executionService.queryExecutions(query, resOffset, resMax)
        def result = results.result
        def total = results.total
        //filter query results to READ authorized executions
        def filtered = frameworkService.filterAuthorizedProjectExecutionsAll(authContext, result, [AuthConstants.ACTION_READ])

        // Calculate stats
        def metricsBuilder = new MetricsStatsBuilder()
        def metricsBuilderStatus = new MetricsStatsBuilder()
        def metricsBuilderDuration = new MetricsStatsBuilder()
        filtered.each { Execution exec ->

            // count total
            metricsBuilder.count(ExecutionQueryMetrics.TOTAL_COUNT)

            // count state
            metricsBuilderStatus.count(exec.getExecutionState())

            // duration stats.
            def dur = exec.durationAsLong()
            if (dur) {
                // register duration avg
                metricsBuilderDuration.average(ExecutionQueryMetrics.DURATION_AVERAGE, dur)
                // register duration max
                metricsBuilderDuration.max(ExecutionQueryMetrics.DURATION_MAX, dur)
                // register duration min
                metricsBuilderDuration.min(ExecutionQueryMetrics.DURATION_MIN, dur)
            }
        }

        // Build and format response
        def metrics = new HashMap(metricsBuilder.buildStatsMap())
        def metricsDur = new HashMap(metricsBuilderDuration.buildStatsMap())
        metricsOutputFormatTimeNumberAsString(metricsDur, [
                ExecutionQueryMetrics.DURATION_AVERAGE,
                ExecutionQueryMetrics.DURATION_MIN,
                ExecutionQueryMetrics.DURATION_MAX
        ])
        metrics['status']=metricsBuilderStatus.buildStatsMap()
        metrics['duration']=metricsDur



        withFormat {
            json {
                render metrics as JSON
            }
            xml {
                render(contentType: "application/xml") {
                    delegate.'result' {
                        metrics.each {key, value ->
                            if(value instanceof Map){
                                Map sub=value
                                delegate."${key}"{
                                    sub.each{k1,v1->
                                        delegate."${k1}"(v1)
                                    }
                                }
                            }
                            else {
                                delegate."${key}"(value)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * On the supplied map, formats the specified keys as a time String.
     * Map values must subclass java.lang.Number
     */
    static void metricsOutputFormatTimeNumberAsString(Map<String, Object> map, List<String> keys) {
        keys.each { k ->
            if(map.containsKey(k)) {
                map[k] = formatTimeNumberAsString(map[k])
            }
        }
    }

    /**
     * Converts an interval of milliseconds into a readable string.
     *
     * @param time An interval of time in number of milliseconds.
     * @return A readable string for the time interval specified. Eg: "5m"
     */
    static String formatTimeNumberAsString(Number time) {

        def duration
        if (time < 1000) {
            duration = "0s"
        } else if (time >= 1000 && time < 60000) {
            duration = String.valueOf((time / 1000) as Integer) + "s"
        } else {
            duration = String.valueOf((time / 60000) as Integer) + "m"
        }
        return String.valueOf(duration)
    }


}
