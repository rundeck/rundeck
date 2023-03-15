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
import com.dtolabs.rundeck.app.api.execution.DeleteBulkRequest
import com.dtolabs.rundeck.app.api.execution.DeleteBulkRequestLong
import com.dtolabs.rundeck.app.api.execution.DeleteBulkRequestXml
import com.dtolabs.rundeck.app.api.execution.DeleteBulkResponse
import com.dtolabs.rundeck.app.api.execution.MetricsQueryResponse
import com.dtolabs.rundeck.app.api.jobs.upload.ExecutionFileInfoList
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileInfo
import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.app.support.ExecutionQueryException
import com.dtolabs.rundeck.app.support.ExecutionViewParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.PluginDisabledException
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
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
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import groovy.transform.PackageScope
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.quartz.JobExecutionContext
import org.rundeck.app.AppConstants
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeAdhoc
import org.rundeck.core.auth.web.RdAuthorizeExecution
import org.rundeck.core.auth.web.RdAuthorizeProject
import org.rundeck.core.auth.web.RdAuthorizeSystem
import org.rundeck.util.Sizes
import org.springframework.dao.DataAccessResourceFailureException
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.*
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.workflow.StateMapping

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat
/**
* ExecutionController
*/
@Controller()
class ExecutionController extends ControllerBase{
    FrameworkService frameworkService
    ExecutionService executionService
    LoggingService loggingService
    ScheduledExecutionService scheduledExecutionService
    OrchestratorPluginService orchestratorPluginService
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
        return redirect(action: 'show', controller: 'execution', params: params)
    }

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def followFragment() {
        return render(view:'showFragment',model:show())
    }

    /**
     * List recent adhoc executions to fill the recent commands menu on commands page.
     * @param project project name
     * @param max maximum results, defaults to 10
     * @return
     */
    @RdAuthorizeAdhoc(RundeckAccess.General.AUTH_APP_READ)
    def adhocHistoryAjax(String project, int max, String query){

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

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    public def show (ExecutionViewParams viewparams){
        if (viewparams.hasErrors()) {
            flash.errors=viewparams.errors
            render(view: '/common/error')
            return
        }
        def Execution e = authorizingExecution.resource
        def filesize=-1
        if(null!=e.outputfilepath){
            def file = new File(e.outputfilepath)
            if (file.exists()) {
                filesize = file.length()
            }
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
        def readAuth = authorizingExecution.isAuthorized(RundeckAccess.General.APP_READ)
        def workflowTree = scheduledExecutionService.getWorkflowDescriptionTree(e.project, e.workflow, readAuth,0)
        def inputFiles = fileUploadService.findRecords(e, FileUploadService.RECORD_TYPE_OPTION_INPUT)
        def inputFilesMap = inputFiles.collectEntries { [it.uuid, it] }

        String max = getGrailsApplication().config.getProperty("rundeck.logviewer.trimOutput", String)
        Long trimOutput = Sizes.parseFileSize(max)

        return loadExecutionViewPlugins() + [
                scheduledExecution    : e.scheduledExecution ?: null,
                isScheduled           : e.scheduledExecution ? scheduledExecutionService.isScheduled(e.scheduledExecution) : false,
                execution             : e,
                workflowTree          : workflowTree,
                filesize              : filesize,
                nextExecution         : e.scheduledExecution?.scheduled ? scheduledExecutionService.nextExecutionTime(
                        e.scheduledExecution
                ) : null,
                enext                 : enext,
                eprev                 : eprev,
                inputFilesMap         : inputFilesMap,
                clusterModeEnabled    : frameworkService.isClusterModeEnabled(),
                trimOutput            : trimOutput
        ]
    }

    @RdAuthorizeProject(RundeckAccess.Project.AUTH_APP_DELETE_EXECUTION)
    def delete() {
        withForm{
        def authed = authorizingExecution
        def Execution e = authorizingExecution.resource
        def jobid=e.scheduledExecution?.extid
        def result = executionService.deleteExecution(authorizingProject, authed)
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

    @RdAuthorizeProject(RundeckAccess.Project.AUTH_APP_DELETE_EXECUTION)
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

        def result = executionService.deleteBulkExecutionIds(ids, projectAuthContext, session.user)
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
        Execution e
        //NB: send custom response for unauthorized/not found
        try {
            e = authorizingExecution.access(RundeckAccess.Execution.APP_READ_OR_VIEW)
        } catch (UnauthorizedAccess ignored) {
            response.status=HttpServletResponse.SC_FORBIDDEN
            return render(contentType: 'application/json',text:[error: "Unauthorized: View Execution ${params.id}"] as JSON)
        } catch (NotFound ignored) {
            response.status=HttpServletResponse.SC_NOT_FOUND
            return render(contentType: 'application/json', text: [error: "Execution not found for id: " + params.id] as JSON)
        }


        def jobcomplete = e.dateCompleted != null
        def execState = e.executionState
        def execDuration = (execState == ExecutionService.EXECUTION_QUEUED) ? -1L :
            (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()
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
        }else if(loader.state in [ExecutionFileState.NOT_FOUND]) {
            data.state = [error: 'not found',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state,
                            default: "Not Found")]
        }else if(loader.state in [ExecutionFileState.ERROR]) {
            data.state = [error: 'error', errorMessage: g.message(code: loader.errorCode, args: loader.errorData)]
        }else if (loader.state in [ExecutionFileState.PENDING_LOCAL, ExecutionFileState.WAITING,
                                   ExecutionFileState.AVAILABLE_REMOTE, ExecutionFileState.PENDING_REMOTE]) {
            data.completed = false
            data.state = [error: 'pending',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state, default: "Pending")]
        } else if (loader.state in [ExecutionFileState.AVAILABLE_REMOTE_PARTIAL]) {
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
        if (loader.state == ExecutionFileState.AVAILABLE_PARTIAL) {
            data.completed = false
            data.state.partial = true
        }
        if(loader.retryBackoff>0){
            data.retryBackoff = loader.retryBackoff
        }
        if(execState == 'missed') {
            data.state = [error: 'missed',errorMessage: "Missed execution scheduled at ${StateMapping.encodeDate(e.dateStarted)}"]
        }
        def limit= configurationService.getInteger("ajax.executionState.compression.nodeThreshold", 500)
        if (selectedNodes || data.state?.allNodes?.size() > limit) {
            renderCompressed(request, response, 'application/json', data as JSON)
        }else{
            render data as JSON
        }
    }

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
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
        def Execution e = authorizingExecution.resource

        def data=[:]
        def selectedNode=params.node
        def loader = workflowService.requestStateSummary(e,[selectedNode],true)
        if (loader.state.isAvailableOrPartial()) {
            data = [
                    name:selectedNode,
                    summary:loader.workflowState.nodeSummaries[selectedNode],
                    steps:loader.workflowState.nodeSteps[selectedNode]
            ]
        }else if(loader.state in [ExecutionFileState.NOT_FOUND]) {
            data = [error: 'not found',
                    errorMessage: g.message(code: 'execution.state.storage.state.' + loader.state,
                            default: "Not Found")]
        }else if(loader.state in [ExecutionFileState.ERROR]) {
            data = [error: 'error', errorMessage: g.message(code: loader.errorCode, args: loader.errorData)]
        }else if (loader.state in [ExecutionFileState.PENDING_LOCAL, ExecutionFileState.WAITING,
                                   ExecutionFileState.AVAILABLE_REMOTE, ExecutionFileState.PENDING_REMOTE]) {
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

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def mail() {
        Execution e = authorizingExecution.resource
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
        withForm {
            def requestActive=params.mode == 'active'

            authorizingSystem.authorize(
                requestActive ? RundeckAccess.System.ADMIN_ENABLE_EXECUTION :
                RundeckAccess.System.ADMIN_DISABLE_EXECUTION
            )

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
            abortresult = executionService.abortExecution(
                authorizingExecution,
                null,
                params.forceIncomplete == 'true'
            )
        } catch (NotFound ignored){
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
        try {
            abortresult = executionService.abortExecution(
                authorizingExecution,
                null,
                true
            )
        } catch (NotFound ignored) {
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

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def downloadOutput() {
        Execution e = authorizingExecution.resource

        def jobcomplete = e.dateCompleted!=null
        def reader = loggingService.getLogReader(e)
        if (reader.state== ExecutionFileState.NOT_FOUND) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not found")
            return
        }else if (reader.state == ExecutionFileState.ERROR) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            def msg= g.message(code: reader.errorCode, args: reader.errorData)
            log.error("Output file reader error: ${msg}")
            appendOutput(response, msg)
            return
        }else if (reader.state != ExecutionFileState.AVAILABLE) {
            //TODO: handle other states
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not available")
            return
        }
        //default timezone is the server timezone
        def reqTimezone = TimeZone.getDefault()
        if (params.timeZone) {
            reqTimezone = TimeZone.getTimeZone(params.timeZone)
        } else if (params.gmt) {
            reqTimezone = TimeZone.getTimeZone('GMT')
        }
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US);
        dateFormater.timeZone = reqTimezone

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
        logFormater.timeZone = reqTimezone
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep=System.getProperty("line.separator")
        boolean nooutput = true
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
            nooutput = false
            appendOutput(response, (isFormatted?"${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.stepctx?:'_'}][${msgbuf.loglevel}] ${message}" : message))
            appendOutput(response, lineSep)
        }
        if(nooutput) appendOutput(response, "No output")
        iterator.close()
    }

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def renderOutput() {
        Execution e = authorizingExecution.resource

        def jobcomplete = e.dateCompleted!=null
        def reader = loggingService.getLogReader(e)
        if (reader.state== ExecutionFileState.NOT_FOUND) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            log.error("Output file not found")
            return
        }else if (reader.state == ExecutionFileState.ERROR) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            def msg= g.message(code: reader.errorCode, args: reader.errorData)
            log.error("Output file reader error: ${msg}")
            appendOutput(response, msg)
            return
        }else if(reader.state == ExecutionFileState.WAITING){
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

        } else if (reader.state != ExecutionFileState.AVAILABLE) {
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
<link rel="stylesheet" href="${g.assetPath(src:'ansi24.css')}"  />
</head>
<body>
<div class="container">
<div class="row">
<div class="col-sm-12">
<div class="ansicolor ansicolor-${(params.ansicolor in ['false','off'])?'off':'on'}" >""")

        def csslevel=!(params.loglevels in ['off','false'])
        def renderContent = shouldConvertContent(params)
        boolean allowUnsanitized = checkAllowUnsanitized(e.project)
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
                    if(allowUnsanitized && meta["no-strip"] == "true") {
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

    @PackageScope
    boolean checkAllowUnsanitized(String project) {
        if(frameworkService.getRundeckFramework().hasProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED)) {
            if ("true" != frameworkService.getRundeckFramework().
                    getProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED)) return false
            def projectConfig = frameworkService.getRundeckFramework().projectManager.loadProjectConfig(project)
            if(projectConfig.hasProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED)) {
                return "true" == projectConfig.getProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED)
            }
            return false
        }
        return false
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

    @Get(uri= "/execution/{id}/output")
    @Operation(
        method="GET",
        summary="Execution Output",
        description="""Get the output for an execution by ID. The execution can be currently running or may have already completed. Output can be filtered down to a specific node or workflow step.

The log output for each execution is stored in a file on the Rundeck server, and this API endpoint allows you to retrieve some or all of the output, in several possible formats: json, XML, and plain text. When retrieving the plain text output, some metadata about the log is included in HTTP Headers. JSON and XML output formats include metadata about each output log line, as well as metadata about the state of the execution and log file, and your current index location in the file.

Output can be selected by Node or Step Context or both as of API v10.

Several parameters can be used to retrieve only part of the output log data. You can use these parameters to more efficiently retrieve the log content over time while an execution is running.

The log file used to store the execution output is a formatted text file which also contains metadata about each line of log output emitted during an execution. Several data values in this API endpoint refer to "bytes", but these do not reflect the size of the final log data; they are only relative to the formatted log file itself. You can treat these byte values as opaque locations in the log file, but you should not try to correlate them to the actual textual log lines.

#### Tailing Output

To "tail" the output from a running execution, you will need to make a series of requests to this API endpoint, and update the `offset` value that you send to reflect the returned `dataoffset` value that you receive.  This gives you a consistent pointer into the output log file.

When starting these requests, there are two mechanisms you can use:

1. Start at the beginning, specifying either a `lastmod` or a `offset` of 0
2. Start at the end, by using `lastlines` to receive the last available set of log lines.

After your first request you will have the `dataoffset` and `lastmod` response values you can use to continue making requests for subsequent log output. You can choose several ways to do this:

1. Use the `offset` and `lastmod` parameters to indicate modification time and receive as much output as is available
2. Use the `offset` and `maxlines` parameter to specify a maximum number of log entries
3. Use only the `offset` parameter and receive as much output as is available.

After each request, you will update your `offset` value to reflect the `dataoffset` in the response.

All log output has been read when the `iscompleted` value is "true".

Below is some example pseudo-code for using this API endpoint to follow the output of a running execution "live":

* set offset to 0
* set lastmod to 0
* Repeat until `iscompleted` response value is "true":
    * perform request sending `offset` and `lastmod` parameters
    * print any log entries, update progress bar, etc.
    * Record the resulting `dataoffset` and `lastmod` response values for the next request
    * if `unmodified` is "true", sleep for 5 seconds
    * otherwise sleep for 2 seconds

**Authorization:**

This endpoint requires that the user have `read` access to the Job or to Adhoc executions to retrieve the output content.

""",
        parameters = [
            @Parameter(
                name = "id",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "nodename",
                description = "Node Name, all results will be filtered for only this node.",
                in = ParameterIn.QUERY,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "stepctx",
                description = "Step Context ID. This is a string in the form `1/2/3` indicating the step context.",
                in = ParameterIn.QUERY,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name='offset',
                in=ParameterIn.QUERY,
                description = "byte offset to read from in the file. 0 indicates the beginning.",
                schema = @Schema(type = "integer")
            ),
            @Parameter(
                name='lastlines',
                in=ParameterIn.QUERY,
                description = "number of lines to retrieve from the end of the available output. If specified it will override the `offset` value and return only the specified number of lines at the end of the log.",
                schema = @Schema(type = "integer")
            ),
            @Parameter(
                name='lastmod',
                in=ParameterIn.QUERY,
                description = "epoch datestamp in milliseconds, return results only if modification changed since the specified date OR if more data is available at the given `offset`.",
                schema = @Schema(type = "integer", format = "int64")
            ),
            @Parameter(
                name='maxlines',
                in=ParameterIn.QUERY,
                description = "maximum number of lines to retrieve forward from the specified offset.",
                schema = @Schema(type = "integer")
            ),
            @Parameter(
                name='compacted',
                in=ParameterIn.QUERY,
                description = "if true, results will be in compacted form. Since: v21",
                schema = @Schema(type = "boolean")
            ),
            @Parameter(
                name='format',
                in=ParameterIn.QUERY,
                description = "Specify output format",
                schema = @Schema(
                    allowableValues = ['xml','json','text'],
                    type = "string"
                )
            )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """Log Output Response. 

The result will contain a set of data values reflecting the execution's status, as well as the status and read location in the output file.

* In JSON, there will be an object containing these entries.
* In XML, there will be an `output` element, containing these sub-elements, each with a text value.
* In plain text format, HTTP headers will include some information about the loging state, but individual log entries will only be returned in textual form without metadata.

Entries:

* `id`: ID of the execution
* `message`: optional text message indicating why no entries were returned
* `error`: optional text message indicating an error case
* `unmodified`: true/false, (optional) "true" will be returned if the `lastmod` parameter was used and the file had not changed
* `empty`: true/false, (optional) "true" will be returned if the log file does not exist or is empty, which may occur if the log data is requested before any output has been stored.
* `offset`: Byte offset to read for the next set of data
* `completed`: true/false, "true" if the current log entries or request parameters include all of the available data
* `execCompleted`: true/false, "true" if the execution has completed.
* `hasFailedNodes`: true/false, "true" if the execution has recorded a list of failed nodes
* `execState`: execution state, one of "running","succeeded","failed","aborted"
* `lastModified`: (long integer), millisecond timestamp of the last modification of the log file
* `execDuration`: (long integer), millisecond duration of the execution
* `percentLoaded`: (float), (optional) percentage of the output which has been loaded by the parameters to this request
* `totalSize`: (integer), total bytes available in the output file
* `filter` - if a `node` or `step` filter was used
    - `nodename` - value of the node name filter
    - `stepctx` - value of the step context filter
* `compacted`: `true` if compacted form was requested and is used in the response (API v21+)
* `compactedAttr`: name of JSON log entry key used by default for fully compacted entries (API v21+)

Each log entry will be included in a section called `entries`.

* In JSON, `entries` will contain an array of Objects, each containing the following format
* In XML, the `entries` element will contain a sequence of `entry` elements

Content of each Log Entry:

* `time`: Timestamp in format: "HH:MM:SS"
* `absolute_time`: Timestamp in format: "yyyy-MM-dd'T'HH:mm:ssZ"
* `level`: Log level, one of: ERROR,WARN,NORMAL,VERBOSE,DEBUG,OTHER
* `log`: The log message
* `user`: User name
* `command`: Workflow command context string
* `node`: Node name
* `stepctx`: The step context such as `1` or `1/2/3`
* `metadata`: Map of extra metadata for the entry (API v43+)

#### Log Entries in Compacted Form (API v21+)

As of API v21, you can specify `compacted=true` in the URL parameters, which will send the Output Content in "compacted" form. This will be indicated by the `compacted`=`true` value in
the result data.

In this mode, Log Entries are compacted by only including the changed values from the
previous Log Entry in the list.  The first Log Entry in the results will always have complete information.  Subsequent entries may include only changed values.

In JSON format, if the `compactedAttr` value is `log` in the response data, and only the `log` value changed relative to a previous Log Entry, the Log Entry may consist only of the log message string. That is, the array entry will be a string, not an object.

When no values changed from the previous Log Entry, the Log Entry will be an empty object.

When an entry value is not present in the subsequent Log Entry, but was present in the previous
one, in JSON this will be represented with a `null` value, and in XML the entry name will be
included in a `removed` attribute.""",
        headers = [
            @Header(name='X-Rundeck-ExecOutput-Error', description='The `error` field (text format only)',schema = @Schema(type="string")),
            @Header(name='X-Rundeck-ExecOutput-Message', description='The `message` field (text format only)',schema = @Schema(type="string")),
            @Header(name='X-Rundeck-ExecOutput-Empty', description='The `empty` field (text format only)',schema = @Schema(type="boolean")),
            @Header(name='X-Rundeck-ExecOutput-Unmodified', description='The `unmodified` field (text format only)',schema = @Schema(type="boolean")),
            @Header(name='X-Rundeck-ExecOutput-Offset', description='The `offset` field (text format only)',schema = @Schema(type="integer")),
            @Header(name='X-Rundeck-ExecOutput-Completed', description='The `completed` field (text format only)',schema = @Schema(type="boolean")),
            @Header(name='X-Rundeck-Exec-Completed', description='The `execCompleted` field (text format only)',schema = @Schema(type="boolean")),
            @Header(name='X-Rundeck-Exec-State', description='The `execState` field (text format only)',schema = @Schema(type="string")),
            @Header(name='X-Rundeck-Exec-Duration', description='the `execDuration` field (text format only)',schema = @Schema(type="integer")),
            @Header(name='X-Rundeck-ExecOutput-LastModifed', description='The `lastModified` field (text format only)',schema = @Schema(type="string",format="iso")),
            @Header(name='X-Rundeck-ExecOutput-TotalSize', description='The `totalSize` field (text format only)',schema = @Schema(type="integer"))
        ],
        content = [
            @Content(
                mediaType = 'application/json',
                schema = @Schema(type = "object"),
                examples=@ExampleObject(
                    value="""{
  "id": 1,
  
  "compacted": "true",
  "compactedAttr": "log",
  "entries": [
    {
      "time": "17:00:00",
      "absolute_time": "1970-01-02T01:00:00Z",
      "level": "NORMAL",
      "log": "This is the first log message",
      "user": "bob",
      "node": "anode1",
      "stepctx": "1"
    },
    "This is the second log message",
    {},
    {
      "stepctx": "2",
      "level": "DEBUG",
      "log": "This is the fourth log message"
    },
    {
      "stepctx": null,
      "log": "This is the fifth log message",
      "node": null
    }
  ]
}""",
                    description="""
In this example, four log entries are included. The first includes all Log Entry fields.
The second is only a String, indicating only `log` value changed.
The third is an empty object, indicating the previous Log Entry was repeated identically.
The fourth specifies a new value for `stepctx` and `log` and `level` to use.
The fifth specifies a `node` and `stepctx` of `null`: indicating the `node` and `stepctx` values should be removed for
this Log Entry.""")
            ),
            @Content(
                mediaType = 'application/xml',
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    value="""<output>
  <id>1</id>
  <!-- ... snip ... -->
  <compacted>true</compacted>
  <entries>
    <entry time='17:00:00' absolute_time='1970-01-02T01:00:00Z' log='This is the first log message' level='NORMAL' user="bob" node="anode1" stepctx="1"/>
    <entry log='This is the second log message' />
    <entry />
    <entry log='This is the fourth log message' level='DEBUG' stepctx='2' />
    <entry log='This is the fifth log message' removed='node,stepctx' />
  </entries>
</output>""",
                    description="Compacted form xml output representing the same log entries as the JSON example.")
            ),
            @Content(
                mediaType = 'text/plain',
                schema = @Schema(type = "string", description="Textual log output"),
                examples = @ExampleObject(
                    value = """Log output text..."""
                )
            )
        ]
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/output, version 5
     */
    def apiExecutionOutput() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        params.stateOutput=false

        if (request.api_version < ApiVersions.V21) {
            params.remove('compacted')
        }
        return tailExecutionOutput()
    }

    @Get(uri="/execution/{id}/output/node/{nodename}")
    @Operation(
        method="GET",
        summary="Execution Output For Node",
        description="""Get the output for an execution filtered for a specific node.""",
        parameters = [
            @Parameter(
                name = "id",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "nodename",
                description = "Node Name, all results will be filtered for only this node.",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """Log Output Response. This endpoint response is the same as the Execution Output `/execution/{id}/output` response using the `nodename` parameter."""
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/output/node/{nodename}, version 5
     */
    def apiExecutionOutputNodeFilter() {
        return apiExecutionOutput()
    }

    @Get(uri="/execution/{id}/output/node/{nodename}/step/{stepctx}")
    @Operation(
        method="GET",
        summary="Execution Output For Node and Step",
        description="""Get the output for an execution filtered for a specific node and step.""",
        parameters = [
            @Parameter(
                name = "id",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "nodename",
                description = "Node Name, all results will be filtered for only this node.",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "stepctx",
                description = "Step Context ID. This is a string in the form `1/2/3` indicating the step context.",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """Log Output Response. This endpoint response is the same as the Execution Output `/execution/{id}/output` response using the `nodename` and `stepctx` parameters."""
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/output/node/{nodename}/step/{stepctx}, version 5
     */
    def apiExecutionOutputNodeStepFilter() {
        return apiExecutionOutput()
    }

    @Get(uri="/execution/{id}/output/step/{stepctx}")
    @Operation(
        method="GET",
        summary="Execution Output For Step",
        description="""Get the output for an execution filtered for a specific step.""",
        parameters = [
            @Parameter(
                name = "id",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "stepctx",
                description = "Step Context ID. This is a string in the form `1/2/3` indicating the step context.",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """Log Output Response. This endpoint response is the same as the Execution Output `/execution/{id}/output` response using the `stepctx` parameter."""
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/output/step/{stepctx}, version 5
     */
    def apiExecutionOutputStepFilter() {
        return apiExecutionOutput()
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
                ]+it.subMap(['level','user','command','stepctx','node','metadata']))
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

                        delegate.'entry'(datamap)

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
                ]+it.subMap(['level','user','command','stepctx','node','metadata']))
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


    @Get(uri="/execution/{id}/output/state")
    @Operation(
        summary="Execution Output with State",
        description = """Get the metadata associated with workflow step state changes along with the log output, optionally excluding log output.

JSON response requires API v14.
""",
        method='GET',
        parameters =[ @Parameter(
            name = "id",
            description = "Execution ID",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String)
        ),@Parameter(
            name = "stateOnly",
            description = "Whether to include only state information. When false, log entries will be included.",
            in = ParameterIn.QUERY,
            schema = @Schema(type="boolean")
        )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """The output format is the same as [Execution Output](#execution-output), with this change:

* in the `entries` section, each entry will have a `type` value indicating the entry type
    - `log` a normal log entry
    - `stepbegin` beginning of the step indicated by the `stepctx`
    - `stepend` finishing of the step
    - `nodebegin` beginning of execution of a node for the given step
    - `nodeend` finishing of execution of a node for the given step
* metadata about the entry may be included in the entry"""
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/output/state, version ?
     */
    def apiExecutionStateOutput() {
        if (!apiService.requireApi(request,response)) {
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
        Execution e

        //NB: handle authorization/not found response uniquely for this endpoint
        try {
            e = authorizingExecution.access(RundeckAccess.Execution.APP_READ_OR_VIEW)
        } catch (UnauthorizedAccess ignored) {
            return apiError(
                'api.error.item.unauthorized',
                [AuthConstants.ACTION_VIEW, "Execution", params.id],
                HttpServletResponse.SC_FORBIDDEN
            )
        }

        if (params.stepctx && !(params.stepctx ==~ /^(\d+e?(@.+?)?\/?)+$/)) {
            return apiError("api.error.parameter.invalid",[params.stepctx,'stepctx',"Invalid stepctx filter"],HttpServletResponse.SC_BAD_REQUEST)
        }

        def jobcomplete = e.dateCompleted != null
        def hasFailedNodes = e.failedNodeList ? true : false
        def execState = e.executionState
        def statusString = e.customStatusString
        def execDuration = (execState == ExecutionService.EXECUTION_QUEUED) ? -1L :
            (e.dateCompleted ? e.dateCompleted.getTime() : System.currentTimeMillis()) - e.dateStarted.getTime()

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
        def error = reader.state == ExecutionFileState.ERROR
        log.debug("Reader, state: ${reader.state}, reader: ${reader.reader}")
        if(error) {
            return apiError(reader.errorCode, reader.errorData, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (null == reader  || reader.state == ExecutionFileState.NOT_FOUND ) {
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
        else if (null == reader || reader.state in [ExecutionFileState.PENDING_LOCAL, ExecutionFileState.PENDING_REMOTE, ExecutionFileState.WAITING]) {
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
                        completed     : reader.state == ExecutionFileState.AVAILABLE,
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
            
            // Since V43 we add metadata field to log entries.
            if (request.api_version >= ApiVersions.V43) {
                logdata.metadata = data.metadata
            }
            
            entry<<logdata
            if (!(0 == max || entry.size() < max)){
                break
            }
        }
        storeoffset= logread.offset
        //not completed if the reader state is only AVAILABLE_PARTIAL
        completed = reader.state == ExecutionFileState.AVAILABLE &&
                    (logread.complete || (jobcomplete && storeoffset == totsize))
        log.debug("finish stream iterator, offset: ${storeoffset}, completed: ${completed}")
        if (storeoffset == offset) {
            //don't change last modified unless new data has been read
            lastmodl = reqlastmod
        }
        logread.close()

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
            boolean allowUnsanitized = checkAllowUnsanitized(e.project)
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
                        if(allowUnsanitized && meta["no-strip"] == "true") {
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
        def percent=100.0 * (totsize>0? (((float)storeoffset)/((float)totsize)) : 0)
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


    /**
     * API: /api/execution/{id} , version 1
     */
    @Get(uri="/execution/{id}")
    @Operation(
        summary="Execution Info",
        description = "Get the status for an execution by ID.",
        method='GET',
        parameters = @Parameter(
            name = "id",
            description = "Execution ID",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String)
        )
    )
    @ApiResponse(
        responseCode = '200',
        description = "XML response contains a single `<execution>` item, see Listing Running Executions. JSON response requires API v14.",
        content=[
            @Content(
                mediaType = 'application/xml',
                examples=  @ExampleObject(
                    value="""<?xml version="1.0" encoding="UTF-8"?>
<execution id="[ID]" href="[url]" permalink="[url]" status="[status]" project="[project]">
    <user>[user]</user>
    <date-started unixtime="[unixtime]">[datetime]</date-started>
    <customStatus>[string]</customStatus>

    <!-- optional job context if the execution is associated with a job -->
    <job id="jobID" averageDuration="[milliseconds]" href="[API url]" permalink="[GUI url]">
        <name>..</name>
        <group>..</group>
        <description>..</description>
        <!-- optional if arguments are passed to the job since v10 -->
        <options>
            <option name="optname" value="optvalue"/>...
        </options>
    </job>

    <!-- description of the execution -->
    <description>...</description>

    <!-- argString (arguments) of the execution -->
    <argstring>...</argstring>

    <!-- if Rundeck is in cluster mode -->
    <serverUUID>...</serverUUID>

    <!-- The following elements included only if the execution has ended -->

    <!-- the completion time of the execution -->
    <date-ended unixtime="[unixtime]">[datetime]</date-ended>

    <!-- if the execution was aborted, the username who aborted it: -->
    <abortedby>[username]</abortedby>

    <!-- if the execution was is finished, a list of node names that succeeded -->
    <successfulNodes>
        <node name="node1"/>
        <node name="node2"/>
    </successfulNodes>

    <!-- if the execution was is finished, a list of node names that failed -->
    <failedNodes>
        <node name="node3"/>
        <node name="node4"/>
    </failedNodes>

</execution>""")
            ),
            @Content(
                mediaType = 'application/json',
                examples = @ExampleObject("""{
  "id": 1,
  "href": "[url]",
  "permalink": "[url]",
  "status": "succeeded/failed/aborted/timedout/retried/other",
  "project": "[project]",
  "user": "[user]",
  "date-started": {
    "unixtime": 1431536339809,
    "date": "2015-05-13T16:58:59Z"
  },
  "date-ended": {
    "unixtime": 1431536346423,
    "date": "2015-05-13T16:59:06Z"
  },
  "job": {
    "id": "[uuid]",
    "href": "[url]",
    "permalink": "[url]",
    "averageDuration": 6094,
    "name": "[name]",
    "group": "[group]",
    "project": "[project]",
    "description": "",
    "options": {
      "opt2": "a",
      "opt1": "testvalue"
    }
  },
  "description": "echo hello there [... 5 steps]",
  "argstring": "-opt1 testvalue -opt2 a",
  "successfulNodes": [
    "nodea","nodeb"
  ],
  "failedNodes": [
    "nodec","noded"
  ]
}""")
            )
        ]
    )
    @Tag(name = 'execution')
    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def apiExecution(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        def Execution e = authorizingExecution.resource

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

    @Get(uri="/execution/{id}/state")
    @Operation(
        summary="Execution State",
        description = """Get detail about the node and step state of an execution by ID. The execution can be currently running or completed.

JSON response requires API v14.
""",
        method='GET',
        parameters = @Parameter(
            name = "id",
            description = "Execution ID",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String)
        )
    )
    @ApiResponse(
        responseCode = '200',
        description = """The content of the response contains state information for different parts of the workflow:

* overall state
* per-node overall state
* per-step node state

A workflow can have a step which consists of a sub-workflow, so each particular step has a "Step Context Identifier" 
which defines its location in the workflow(s), and looks something like "1/5/2". Each number identifies the step 
number (starting at 1) at a workflow level. If there is a "/" in the context identifier, it means there are 
sub-workflow step numbers, and each preceding number corresponds to a step which has a sub-workflow.

To identify the state of a particular node at a particular step, both a Node name, and a Step Context Identifier are 
necessary.

In the result set returned by this API call, state information is organized primarily by Step and is structured in 
the same way as the workflow.  This means that sub-workflows will have nested state structures for their steps.

The state information for a Node will not contain the full set of details for the Step and Node, since this 
information is present in the workflow structure which contains the step state.

#### State Result Content

The result set contains this top-level structure:

* general overall state information
    - `startTime` execution start time (see *Timestamp format* below)
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state
* `allNodes` contains a *Node Name List* (see below) of nodes known to be targeted in some workflow
* `nodes` contains an *Overall Node State List* of per-node step states
* `serverNode` name of the server node
* `executionId` current execution ID
* `completed` true/false whether the execution is completed
* A *Workflow Section* (see below)

**Workflow Section**

Each Workflow Section within the result set will contain these structures

* `stepCount` Number of steps in the workflow
* `targetNodes` contains a Node Name List identifying the target nodes of the current workflow
* `steps` contains a *Step State List* (see below) of information and state for each step

**Node Name List**

Consists of a sequence of node name entries, identifying each entry by a name.

In XML, a sequence of `node` elements:

      <node name="abc" />
      <node name="xyz" />
      <!-- ... more node elements -->

In JSON, an array of node names.

**Overall Node State List**

Consists of a sequence of entries for each Node. Each entry contains

* `name` node name
* `steps` list of simple state indicator for steps executed by this node

State Indicators:

* `stepctx` Step Context Identifier
* `executionState` execution state for this step and node

In XML:

``` xml
<node name="abc">
  <steps>
    <step>
      <stepctx>1</stepctx>
      <executionState>SUCCEEDED</executionState>
    </step>
    <step>
      <stepctx>2/1</stepctx>
      <executionState>SUCCEEDED</executionState>
    </step>
  </steps>
</node>
<!-- more node elements -->
```

In JSON: an object where each key is a node name, and the value is an array of State indicators.  A state indicator 
is an object with two keys, `stepctx` and `executionState`

``` json
{
    "abc": [
      {
        "executionState": "SUCCEEDED",
        "stepctx": "1"
      },
      {
        "executionState": "SUCCEEDED",
        "stepctx": "2/1"
      }
    ]
}
```

**Step State List**

A list of Step State information.  Each step is identified by its number in the workflow (starting at 1) and its step
 context

* `num` the step number (XML)
* `id` the step number (JSON)
* `stepctx` the step context identifier in the workflow
* general overall state information for the step
    - `startTime` execution start time
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state
* `nodeStep` true/false. true if this step directly targets each node from the targetNodes list.  If true, this means
 the step will contain a `nodeStates` section
* `nodeStates` a *Node Step State Detail List* (see below) for the target nodes if this is a node step.
* `hasSubworkflow` true/false. true if this step has a sub-workflow and a `workflow` entry
* `workflow` this section contains a Workflow Section

**Node Step State Detail List**

A sequence of state details for a set of Nodes for the containing step. Each entry will contain:

* `name` the node name
* state information for the Node
    - `startTime` execution start time
    - `endTime` execution end time if complete
    - `updateTime` last update time
    - `executionState` overall execution state

In XML:

``` xml
<nodeState name="abc">
  <startTime>2014-01-13T20:58:59Z</startTime>
  <updateTime>2014-01-13T20:59:04Z</updateTime>
  <endTime>2014-01-13T20:59:04Z</endTime>
  <executionState>SUCCEEDED</executionState>
</nodeState>
<!-- more nodeState elements -->
```

In JSON: an object with node names as keys.  Values are objects containing the state information entries.

``` json
{
    "abc": {
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:31Z",
      "updateTime": "2014-01-13T20:38:31Z",
      "startTime": "2014-01-13T20:38:25Z"
    }
}
```

**Timestamp format:**

The timestamp format is ISO8601: `yyyy-MM-dd'T'HH:mm:ss'Z'`

**Execution states:**

* `WAITING` - Waiting to start running
* `RUNNING` - Currently running
* `RUNNING_HANDLER` - Running error handler\\*
* `SUCCEEDED` - Finished running successfully
* `FAILED` - Finished with a failure
* `ABORTED` - Execution was aborted
* `NODE_PARTIAL_SUCCEEDED` - Partial success for some nodes\\*
* `NODE_MIXED` - Mixed states among nodes\\*
* `NOT_STARTED` - After waiting the execution did not start\\*

\\* these states only apply to steps/nodes and do not apply to the overall execution or workflow.
""",
        content=[
            @Content(
                mediaType = 'application/xml',
                schema = @Schema(type='object'),
                examples=  @ExampleObject(
                    value="""<result success="true">
  <executionState id="135">
    <startTime>2014-01-13T20:58:59Z</startTime>
    <updateTime>2014-01-13T20:59:10Z</updateTime>
    <stepCount>2</stepCount>
    <allNodes>
      <nodes>
        <node name="dignan" />
      </nodes>
    </allNodes>
    <targetNodes>
      <nodes>
        <node name="dignan" />
      </nodes>
    </targetNodes>
    <executionId>135</executionId>
    <serverNode>dignan</serverNode>
    <endTime>2014-01-13T20:59:10Z</endTime>
    <executionState>SUCCEEDED</executionState>
    <completed>true</completed>
    <steps>
      <step stepctx="1" id="1">
        <startTime>2014-01-13T20:58:59Z</startTime>
        <nodeStep>true</nodeStep>
        <updateTime>2014-01-13T20:58:59Z</updateTime>
        <endTime>2014-01-13T20:59:04Z</endTime>
        <executionState>SUCCEEDED</executionState>
        <nodeStates>
          <nodeState name="dignan">
            <startTime>2014-01-13T20:58:59Z</startTime>
            <updateTime>2014-01-13T20:59:04Z</updateTime>
            <endTime>2014-01-13T20:59:04Z</endTime>
            <executionState>SUCCEEDED</executionState>
          </nodeState>
        </nodeStates>
      </step>
      <step stepctx="2" id="2">
        <startTime>2014-01-13T20:59:04Z</startTime>
        <nodeStep>false</nodeStep>
        <updateTime>2014-01-13T20:59:10Z</updateTime>
        <hasSubworkflow>true</hasSubworkflow>
        <endTime>2014-01-13T20:59:10Z</endTime>
        <executionState>SUCCEEDED</executionState>
        <workflow>
          <startTime>2014-01-13T20:59:04Z</startTime>
          <updateTime>2014-01-13T20:59:10Z</updateTime>
          <stepCount>1</stepCount>
          <allNodes>
            <nodes>
              <node name="dignan" />
            </nodes>
          </allNodes>
          <targetNodes>
            <nodes>
              <node name="dignan" />
            </nodes>
          </targetNodes>
          <endTime>2014-01-13T20:59:10Z</endTime>
          <executionState>SUCCEEDED</executionState>
          <completed>true</completed>
          <steps>
            <step stepctx="2/1" id="1">
              <startTime>2014-01-13T20:59:04Z</startTime>
              <nodeStep>true</nodeStep>
              <updateTime>2014-01-13T20:59:04Z</updateTime>
              <endTime>2014-01-13T20:59:10Z</endTime>
              <executionState>SUCCEEDED</executionState>
              <nodeStates>
                <nodeState name="dignan">
                  <startTime>2014-01-13T20:59:04Z</startTime>
                  <updateTime>2014-01-13T20:59:10Z</updateTime>
                  <endTime>2014-01-13T20:59:10Z</endTime>
                  <executionState>SUCCEEDED</executionState>
                </nodeState>
              </nodeStates>
            </step>
          </steps>
        </workflow>
      </step>
    </steps>
    <nodes>
      <node name="dignan">
        <steps>
          <step>
            <stepctx>1</stepctx>
            <executionState>SUCCEEDED</executionState>
          </step>
          <step>
            <stepctx>2/1</stepctx>
            <executionState>SUCCEEDED</executionState>
          </step>
        </steps>
      </node>
    </nodes>
  </executionState>
</result>""")
            ),
            @Content(
                mediaType = 'application/json',
                schema = @Schema(type='object'),
                examples = @ExampleObject("""{
  "completed": true,
  "executionState": "SUCCEEDED",
  "endTime": "2014-01-13T20:38:36Z",
  "serverNode": "dignan",
  "startTime": "2014-01-13T20:38:25Z",
  "updateTime": "2014-01-13T20:38:36Z",
  "stepCount": 2,
  "allNodes": [
    "dignan"
  ],
  "targetNodes": [
    "dignan"
  ],
  "nodes": {
    "dignan": [
      {
        "executionState": "SUCCEEDED",
        "stepctx": "1"
      },
      {
        "executionState": "SUCCEEDED",
        "stepctx": "2/1"
      }
    ]
  },
  "executionId": 134,
  "steps": [
    {
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:31Z",
      "nodeStates": {
        "dignan": {
          "executionState": "SUCCEEDED",
          "endTime": "2014-01-13T20:38:31Z",
          "updateTime": "2014-01-13T20:38:31Z",
          "startTime": "2014-01-13T20:38:25Z"
        }
      },
      "updateTime": "2014-01-13T20:38:25Z",
      "nodeStep": true,
      "id": "1",
      "startTime": "2014-01-13T20:38:25Z"
    },
    {
      "workflow": {
        "completed": true,
        "startTime": "2014-01-13T20:38:31Z",
        "updateTime": "2014-01-13T20:38:36Z",
        "stepCount": 1,
        "allNodes": [
          "dignan"
        ],
        "targetNodes": [
          "dignan"
        ],
        "steps": [
          {
            "executionState": "SUCCEEDED",
            "endTime": "2014-01-13T20:38:36Z",
            "nodeStates": {
              "dignan": {
                "executionState": "SUCCEEDED",
                "endTime": "2014-01-13T20:38:36Z",
                "updateTime": "2014-01-13T20:38:36Z",
                "startTime": "2014-01-13T20:38:31Z"
              }
            },
            "updateTime": "2014-01-13T20:38:31Z",
            "nodeStep": true,
            "id": "1",
            "startTime": "2014-01-13T20:38:31Z"
          }
        ],
        "endTime": "2014-01-13T20:38:36Z",
        "executionState": "SUCCEEDED"
      },
      "executionState": "SUCCEEDED",
      "endTime": "2014-01-13T20:38:36Z",
      "hasSubworkflow": true,
      "updateTime": "2014-01-13T20:38:36Z",
      "nodeStep": false,
      "id": "2",
      "startTime": "2014-01-13T20:38:31Z"
    }
  ]
}""")
            )
        ]
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/state , version 11
     */
    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def apiExecutionState(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        Execution e = authorizingExecution.resource

        def loader = workflowService.requestState(e)
        def state= loader.workflowState
        if(!loader.workflowState){
            if(loader.state in [ExecutionFileState.WAITING, ExecutionFileState.AVAILABLE_REMOTE,
                                ExecutionFileState.PENDING_LOCAL, ExecutionFileState.PENDING_REMOTE]) {
                state = [error: 'pending']
            }else{
                def errormap=[:]
                if (loader.state in [ExecutionFileState.NOT_FOUND]) {
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

    @Post(uri="/execution/{id}/abort")
    @Operation(
        summary="Aborting Executions",
        description = "Abort a running execution by ID.",
        method='POST',
        parameters = [
            @Parameter(
                name = "id",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "asUser",
                description = "Specifies a username identifying the user who aborted the execution. Requires `runAs` actiion authorization.",
                in = ParameterIn.QUERY,
                required = false,
                schema = @Schema(implementation = String)
            ),
            @Parameter(
                name = "forceIncomplete",
                description = "if `true`, forces a running execution to be marked as \"incomplete\".",
                in = ParameterIn.QUERY,
                required = false,
                schema = @Schema(implementation = Boolean)
            )
        ]
    )
    @ApiResponse(
        responseCode = '200',
        description = """XML response will contain a `success/message` element will contain a descriptive message. The status of the abort action will be included as an element.

The `[abort-state]` will be one of: "pending", "failed", or "aborted".

If the `[abort-state]` is "failed", then `[reason]` will be a textual description of the reason.

Authorization required:
* action: `kill`, or `admin`, `app_admin`
* resource: `execution`
""",
        content=[
            @Content(
                mediaType = 'application/xml',
                examples=  @ExampleObject(
                    value="""<abort status="[abort-state]">
    <execution id="[id]" status="[status]"/>
</abort>""")
            ),
            @Content(
                mediaType = 'application/json',
                examples = @ExampleObject("""{
  "abort": {
    "status": "[abort-state]",
    "reason": "[reason]"
  },
  "execution": {
    "id": "[id]",
    "status": "[execution status]",
    "href": "[API href]",
  }
}""")
            )
        ]
    )
    @Tag(name = 'execution')
    /**
     * API: /api/execution/{id}/abort, version 1
     */
    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_KILL)
    def apiExecutionAbort(){
        if (!apiService.requireApi(request, response)) {
            return
        }

        def auth = authorizingExecution
        Execution e = auth.resource
        ExecutionService.AbortResult abortresult
        try {
            def killas=null
            if (params.asUser) {
                //authorized within service call
                killas= params.asUser
            }

            abortresult = executionService.abortExecution(
                auth,
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
                    abort(reportstate) {
                        execution(id: params.id, status: abortresult.jobstate,
                                  href:apiService.apiHrefForExecution(e),
                                  permalink: apiService.guiHrefForExecution(e))
                    }
                }
            }
            json{
                return render ([
                        abort    : reportstate,
                        execution: [
                            id       : params.id,
                            status   : abortresult.jobstate,
                            href     : apiService.apiHrefForExecution(e),
                            permalink: apiService.guiHrefForExecution(e)
                        ]
                    ] as JSON)
            }
        }
    }

    @Delete(uri="/execution/{id}")
    @Operation(
        summary="Delete an Execution",
        description = """Delete an execution by ID.

Authorization requirement: Requires the `delete_execution` action allowed for a `project` in the `application` context.

See: [Administration - Access Control Policy - Application Scope Resources and Actions](https://docs.rundeck.com/docs/administration/security/authorization.html#application-scope-resources-and-actions)

Since: V12
""",
        method='DELETE',
        parameters = @Parameter(
            name = "id",
            description = "Execution ID",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String)
        )
    )
    @ApiResponse(
        responseCode = '204',
        description = "No Content"
    )
    @Tag(name = 'execution')
    /**
     * DELETE /api/12/execution/[ID]
     * @return
     */
    @GrailsCompileStatic
    def apiExecutionDelete (){
        if (!apiService.requireApi(request, response, ApiVersions.V12)) {
            return
        }
        def eauth=authorizingExecution
        def result = executionService.deleteExecution(
            authorizingProject(eauth.resource.project),
            eauth
        )
        if(!result.success){
            log.error("Failed to delete execution: ${result.message}")
            def respFormat = apiService.extractResponseFormat(getRequest(), response, ['xml', 'json'])
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

    @Post('/executions/delete')
    @Operation(
        method="POST",
        summary="Bulk Delete Executions",
        description = """Delete a set of Executions by their IDs.

The IDs can be specified in two ways:

1. Using a URL parameter `ids`, as a comma separated list, with no body content

        POST /api/12/executions/delete?ids=1,2,17
        Content-Length: 0

2. Using a request body of either XML or JSON data.

Note: the JSON schema also supports a basic JSON array 
""",
        requestBody = @RequestBody(
            description = "Delete Bulk IDs request.",
            content=[
                @Content(
                    mediaType = 'application/json',
                    schema = @Schema(oneOf = [
                        DeleteBulkRequest, DeleteBulkRequestLong
                    ]),
                    examples = [
                        @ExampleObject(value = """{"ids": [ 1, 2, 17 ] }""", name = "object"),
                        @ExampleObject(value = """[ 1, 2, 17 ]""", name = "array")
                    ]
                ),
                @Content(
                    mediaType = 'application/xml',
                    schema = @Schema(implementation = DeleteBulkRequestXml),
                    examples = @ExampleObject("""<executions>
    <execution id="1"/>
    <execution id="2"/>
    <execution id="17"/>
</executions>""")
                )
            ]
        ),
        parameters = @Parameter(
            name = "ids",
            description = "comma separated list of IDs",
            in = ParameterIn.QUERY,
            required = false,
            schema = @Schema(type = "string", format = "comma-separated")
        )
    )
    @ApiResponse(
        responseCode='200',
        description = """""",
        content=[@Content(
            mediaType = 'application/json',
            schema = @Schema(implementation = DeleteBulkResponse),
            examples = @ExampleObject("""{
  "failures": [
    {
      "id": "82",
      "message": "Not found: 82"
    },
    {
      "id": "83",
      "message": "Not found: 83"
    },
    {
      "id": "84",
      "message": "Not found: 84"
    }
  ],
  "failedCount": 3,
  "successCount": 2,
  "allsuccessful": false,
  "requestCount": 5
}""")
        ),@Content(
            mediaType = 'application/xml',
            schema = @Schema(implementation = DeleteBulkResponse),
            examples = @ExampleObject("""<deleteExecutions requestCount='4' allsuccessful='false'>
  <successful count='0' />
  <failed count='4'>
    <execution id='131' message='Unauthorized: Delete execution 131' />
    <execution id='109' message='Not found: 109' />
    <execution id='81' message='Not found: 81' />
    <execution id='74' message='Not found: 74' />
  </failed>
</deleteExecutions>""")
        )]

    )
    @Tag(name = 'execution')
    /**
     * Delete bulk API action
     * @return
     */
    def apiExecutionDeleteBulk() {
        if (!apiService.requireApi(request, response, ApiVersions.V12)) {
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
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        def result=executionService.deleteBulkExecutionIds([ids].flatten(), authContext, session.user)
        executionService.renderBulkExecutionDeleteResult(request,response,result)
    }


    /**
     * API: /api/14/project/NAME/executions
     */
    def apiExecutionsQueryv14(ExecutionQuery query){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
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

        if (!apiService.requireExists(response, frameworkService.existsFrameworkProject(params.project), ['Project', params.project])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)

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
        def resMax = params.max ? params.int('max') : configurationService.getInteger('pagination.default.max',20)

        def results
        try {
            results = executionService.queryExecutions(query, resOffset, resMax)
        }
        catch (ExecutionQueryException e) {
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.parameter.error',
                    args  : [message(code: e.getErrorMessageCode())]
                ]
            )
        }

        def result = results.result
        def total = results.total
        //filter query results to READ authorized executions
        def filtered = rundeckAuthContextProcessor.filterAuthorizedProjectExecutionsAll(authContext, result, [AuthConstants.ACTION_READ])

        withFormat {
            xml {
                return executionService.respondExecutionsXml(request, response, filtered, [total: total, offset: resOffset, max: resMax])
            }
            json {
                return executionService.respondExecutionsJson(request, response, filtered, [total: total, offset: resOffset, max: resMax])
            }
        }


    }


    /**
     *
     * @return
     */
    @RdAuthorizeSystem(RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN)
    def apiExecutionModeStatus() {
        if (!apiService.requireApi(request, response, ApiVersions.V32)) {
            return
        }

        def executionStatus = configurationService.executionModeActive
        int respStatus = executionStatus ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE
        boolean apiVersionAfterV35 = request.api_version > ApiVersions.V35
        if(apiVersionAfterV35 && !executionStatus) {
            respStatus =  params.boolean('passiveAs503') ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_OK
        }

        withFormat {
            json {
                render(status: respStatus,contentType: "application/json") {
                    delegate.executionMode(executionStatus ? 'active' : 'passive')
                }
            }
            xml {
                render(status: respStatus,contentType: "application/xml") {
                    delegate.'executions'(executionMode: executionStatus ? 'active' : 'passive')
                }
            }
        }


    }

    /**
     *
     * @return
     */

    @RdAuthorizeSystem(
        RundeckAccess.System.AUTH_ADMIN_ENABLE_EXECUTION
    )
    def apiExecutionModeActive() {
        apiExecutionMode(true)
    }
    /**
     *
     * @return
     */
    @RdAuthorizeSystem(
        RundeckAccess.System.AUTH_ADMIN_DISABLE_EXECUTION
    )
    def apiExecutionModePassive() {
        apiExecutionMode(false)
    }
    /**
     *
     * @return
     */
    private def apiExecutionMode(boolean active) {
        if (!apiService.requireApi(request, response, ApiVersions.V14)) {
            return
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

    @Get(uri="/execution/{id}/input/files")
    @Operation(
        summary="List Input Files for an Execution",
        description = "List input files used for an execution. Since: V19",
        method='GET',
        parameters = @Parameter(
            name = "id",
            description = "Execution ID",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(implementation = String)
        )
    )
    @ApiResponse(
        responseCode = '200',
        description = "",
        content=[
            @Content(
                mediaType = 'application/xml',
                schema = @Schema(
                    implementation = ExecutionFileInfoList
                ),
                examples=  @ExampleObject(
                    value="""<?xml version="1.0" encoding="UTF-8"?>
<executionFiles>
  <files>
    <file id="382c7596-435b-4103-8781-6b32fbd629b2">
      <user>admin</user>
      <fileState>deleted</fileState>
      <sha>
      9284ed4fd7fe1346904656f329db6cc49c0e7ae5b8279bff37f96bc6eb59baad</sha>
      <jobId>7b3fff59-7a2d-4a31-a5b2-dd26177c823c</jobId>
      <dateCreated>2017-02-24 15:26:48.197 PST</dateCreated>
      <serverNodeUUID>
      3425B691-7319-4EEE-8425-F053C628B4BA</serverNodeUUID>
      <fileName />
      <size>12</size>
      <expirationDate>2017-02-24 15:27:18.65 PST</expirationDate>
      <execId>2837</execId>
    </file>
  </files>
</executionFiles>""")
            ),
            @Content(
                mediaType = 'application/json',
                schema = @Schema(
                    implementation = ExecutionFileInfoList
                ),
                examples = @ExampleObject("""{
  "files": [
    {
      "id": "382c7596-435b-4103-8781-6b32fbd629b2",
      "user": "admin",
      "fileState": "deleted",
      "sha": "9284ed4fd7fe1346904656f329db6cc49c0e7ae5b8279bff37f96bc6eb59baad",
      "jobId": "7b3fff59-7a2d-4a31-a5b2-dd26177c823c",
      "dateCreated": "2017-02-24T23:26:48Z",
      "serverNodeUUID": "3425B691-7319-4EEE-8425-F053C628B4BA",
      "fileName": null,
      "size": 12,
      "expirationDate": "2017-02-24T23:27:18Z",
      "execId": 2837
    }
  ]
}""")
            )
        ]
    )
    @Tag(name = 'execution')
    /**
     * List input files for an execution
     */
    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def apiExecutionInputFiles() {
        if (!apiService.requireApi(request, response, ApiVersions.V19)) {
            return
        }

        Execution e = authorizingExecution.resource

        def inputFiles = fileUploadService.findRecords(e, FileUploadService.RECORD_TYPE_OPTION_INPUT)

        respond(
                new ExecutionFileInfoList(inputFiles.collect { new JobFileInfo(it.exportMap()) }, [:]),
                [format: ['xml', 'json']]
        )
    }

    @Get(uri="/executions/metrics",produces = "application/json")
    @Operation(
        method = "GET",
        summary = "Execution Query Metrics",
        description = """Obtain metrics over the result set of an execution query.""",
        tags = "execution",
        parameters = [
            @Parameter(in=ParameterIn.QUERY,name="project",description="Project name",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="statusFilter",description="Execution status",schema=@Schema(type="string",allowableValues = ["running","succeeded", "failed" , "aborted"])),
            @Parameter(in=ParameterIn.QUERY,name="abortedbyFilter",description="Username who aborted an execution",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="jobIdListFilter",description="specify a Job ID to include, can be specified multiple times",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeJobIdListFilter",description="specify a Job ID to exclude, can be specified multiple times",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="jobListFilter",description="specify a full Job group/name to include, can be specified multiple times",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeJobListFilter",description="specify a full Job group/name to exclude, can be specified multiple times",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="groupPath",description="""specify a group or partial group path to include all jobs within that group path. Set to the special value "-" to match the top level jobs only.""",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="groupPathExact",description="""specify an exact group path to match.  Set to the special value "-" to match the top level jobs only.""",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeGroupPath",description="""specify a group or partial group path to exclude all jobs within that group path. Set to the special value "-" to match the top level jobs only.""",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeGroupPathExact",description="""specify an exact group path to exclude.  Set to the special value "-" to match the top level jobs only.""",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="jobFilter",description="specify a filter for the job Name. Include any job name that matches this value",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeJobFilter",description="specify a filter for the job Name. Exclude any job name that matches this value.",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="jobExactFilter",description="specify an exact job name to match.",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="excludeJobExactFilter",description="specify an exact job name to exclude.",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="startafterFilter",description="start after date",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="startbeforeFilter",description="start before date",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="endafterFilter",description="end after date",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="endbeforeFilter",description="end before date",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="begin",description="Specify exact date for earliest execution completion time. Format: a unix millisecond timestamp, or a W3C dateTime string in the format \"yyyy-MM-ddTHH:mm:ssZ\".",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="end",description="Specify exact date for latest execution completion time. Format: a unix millisecond timestamp, or a W3C dateTime string in the format \"yyyy-MM-ddTHH:mm:ssZ\".",schema=@Schema(type="string",format="iso")),
            @Parameter(in=ParameterIn.QUERY,name="adhoc",description="if true, include only Adhoc executions, if false return only Job executions. By default any matching executions are returned, however if you use any of the Job filters below, then only Job executions will be returned.",schema=@Schema(type="boolean")),


            @Parameter(in=ParameterIn.QUERY,name="recentFilter",
                description="""Use a simple text format to filter executions that completed within a period of time.
The format is \"XY\" where X is an integer, and \"Y\" is one of:
* `s`: second
* `n`: minute
* `h`: hour
* `d`: day
* `w`: week
* `m`: month
* `y`: year

So a value of `2w` would return executions that completed within the last two weeks.
""",
                schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="olderFilter",description="(same format as `recentFilter`) return executions that completed before the specified relative period of time.  E.g. a value of `30d` returns executions older than 30 days.",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="userFilter",description="Username who started the execution",schema=@Schema(type="string")),
            @Parameter(in=ParameterIn.QUERY,name="executionTypeFilter",description="""specify the execution type, one of: `scheduled` (schedule trigger), `user` (user trigger), `user-scheduled` (user scheduled trigger). Since: v20""",schema=@Schema(type="string",allowableValues = ['scheduled','user','user-scheduled'])),
            @Parameter(in=ParameterIn.QUERY,name="max",description="""maximum number of results to include in response. (default: 20)""",schema=@Schema(type="integer")),
            @Parameter(in=ParameterIn.QUERY,name="offset",description="""offset for first result to include. (default: 0)""",schema=@Schema(type="integer"))
        ]
    )
    @ApiResponse(
        responseCode = "200",
        description="Metrics response",
        content=[
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MetricsQueryResponse),
                examples = @ExampleObject("""{
    "duration": {
        "average": "1s",
        "min": "0s",
        "max": "3s"
    },
    "total": 1325
}""")
            ),
            @Content(
                mediaType = "application/xml",
                schema = @Schema(implementation = MetricsQueryResponse),
                examples = @ExampleObject("""<result>
  <duration>
    <average>1s</average>
    <min>0s</min>
    <max>3s</max>
  </duration>
  <total>1325</total>
</result>""")
            )
        ]
    )
    /**
     * Placeholder method to annotate for openapi spec generation.
     * Note: this method will never be used.
     * This is used in place of annotating the apiExecutionMetrics method because
     * the grails binding parameter type ExecutionQuery gets included
     * as a required parameter in the spec, which is incorrect
     */
    protected def apiExecutionMetrics_docs() {
        apiExecutionMetrics(null)
    }


    @Get(uri="/project/{project}/executions/metrics",produces = "application/json")
    @Operation(
        method = "GET",
        summary = "Execution Query Metrics",
        description = """Obtain metrics over the result set of an execution query over the executions of a single project.

Note: This endpoint has the same query parameters and response as the `/executions/metrics` endpoint.
""",
        tags = "execution",
        parameters = [
            @Parameter(in=ParameterIn.PATH,name="project",description="Project name",schema=@Schema(type="string"),required = true)
        ]
    )
    @ApiResponse(
        responseCode = "200",
        description="Metrics response",
        content=[
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MetricsQueryResponse)
            ),
            @Content(
                mediaType = "application/xml",
                schema = @Schema(implementation = MetricsQueryResponse)
            )
        ]
    )
    @Tag(name = 'execution')
    /**
     * Placeholder method to annotate for openapi spec generation.
     * Note: this method will never be used.
     * This is used in place of annotating the apiExecutionMetrics method because
     * the grails binding parameter type ExecutionQuery gets included
     * as a required parameter in the spec, which is incorrect
     */
    protected def apiExecutionMetricsProject_docs() {
        apiExecutionMetrics(null)
    }
    /**
     * API: /api/28/executions/metrics
     */
    def apiExecutionMetrics(ExecutionQuery query) {
        if (!apiService.requireApi(request, response, ApiVersions.V29)) {
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

        if(params.project) {
            query.projFilter = params.project
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


        def metrics
        try {
            // Get metric data
            metrics = executionService.queryExecutionMetrics(query)
        }
        catch (ExecutionQueryException e) {
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.parameter.error',
                    args  : [message(code: e.getErrorMessageCode())]
                ]
            )
        }

        // Format times to be human readable.
        metricsOutputFormatTimeNumberAsString(metrics.duration, [
            "average",
            "min",
            "max"
        ])


        withFormat {
            json {
                render metrics as JSON
            }
            xml {
                render(contentType: "application/xml") {
                    delegate.'result' {
                        metrics.each { key, value ->
                            if (value instanceof Map) {
                                Map sub = value
                                delegate."${key}" {
                                    sub.each { k1, v1 ->
                                        delegate."${k1}"(v1)
                                    }
                                }
                            } else {
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
    static void metricsOutputFormatTimeNumberAsString(Map<Object, Number> map, List<String> keys) {
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
