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
import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.api.ApiRunAdhocRequest
import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.api.execution.DeleteBulkResponse
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileInfo
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileInfoList
import com.dtolabs.rundeck.app.api.jobs.upload.JobFileUpload
import com.dtolabs.rundeck.app.support.ExtraCommand
import com.dtolabs.rundeck.app.support.RunJobCommand
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.http.HttpClient
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import groovy.transform.TypeCheckingMode
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.collections.list.TreeList
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.utils.DateUtils
import org.apache.http.client.utils.URIBuilder
import org.grails.web.json.JSONElement
import org.hibernate.criterion.Example
import org.quartz.CronExpression
import org.rundeck.app.api.model.ApiErrorResponse
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeJob
import org.rundeck.util.HttpClientCreator
import org.rundeck.util.Toposort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionDefinition
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartRequest
import org.springframework.web.multipart.commons.CommonsMultipartFile
import rundeck.*
import org.rundeck.app.jobs.options.ApiTokenReporter
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import org.rundeck.app.jobs.options.RemoteUrlAuthenticationType
import rundeck.services.*
import rundeck.services.feature.FeatureService
import rundeck.services.optionvalues.OptionValuesService

import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import java.util.function.Predicate
import java.util.regex.Pattern

@Controller()
class ScheduledExecutionController  extends ControllerBase{
    static Logger logger = LoggerFactory.getLogger(ScheduledExecutionController)

    def ExecutionService executionService
    def FrameworkService frameworkService
    def ScheduledExecutionService scheduledExecutionService
    def OrchestratorPluginService orchestratorPluginService
	def NotificationService notificationService
    def UserService userService
    def ScmService scmService
    def PluginService pluginService
    def FileUploadService fileUploadService
    def StorageService storageService
    OptionValuesService optionValuesService
    FeatureService featureService
    RundeckJobDefinitionManager rundeckJobDefinitionManager
    AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    ConfigurationService configurationService
    JobDataProvider jobDataProvider
    ReferencedExecutionDataProvider referencedExecutionDataProvider


    def index = { redirect(controller:'menu',action:'jobs',params:params) }

    // the delete, save and update actions only
    // accept POST requests
    def static allowedMethods = [
            delete                       : ['POST', 'GET'],
            deleteBulk                   : 'POST',
            flipExecutionDisabledBulk    : 'POST',
            flipExecutionEnabledBulk     : 'POST',
            flipScheduleDisabledBulk     : 'POST',
            flipScheduleEnabledBulk      : 'POST',
            flipScheduleEnabled          : 'POST',
            flipExecutionEnabled         : 'POST',
            scheduleJobInline            : 'POST',
            runJobInline                 : 'POST',
            runJobNow                    : 'POST',
            runJobLater                  : 'POST',
            runAdhocInline               : 'POST',
            save                         : 'POST',
            saveAndExec                  : 'POST',
            update                       : 'POST',
            upload                       : 'GET',
            uploadPost                   : ['POST'],
            apiFlipExecutionEnabled      : 'POST',
            apiFlipExecutionEnabledBulk  : 'POST',
            apiFlipScheduleEnabled       : 'POST',
            apiFlipScheduleEnabledBulk   : 'POST',
            apiJobCreateSingle           : 'POST',
            apiJobRun                    : ['POST', 'GET'],
            apiJobFileUpload             : 'POST',
            apiJobsImportv14             : 'POST',
            apiJobDelete                 : 'DELETE',
            apiRunScriptv14              : 'POST',
            apiRunScriptUrlv14           : ['POST', 'GET'],
            apiRunCommand                : ['POST', 'GET'],
            apiRunCommandv14             : ['POST', 'GET'],
            apiJobDeleteBulk             : ['DELETE', 'POST'],
            apiJobClusterTakeoverSchedule: 'PUT',
            apiJobUpdateSingle           : 'PUT',
            apiJobRetry                  : 'POST',
            apiJobWorkflow               : 'GET',
    ]

    def cancel (){
        //clear session workflow data
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        if(session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        if(params.id && params.id!=''){
            redirect(action:'show',params:[id:params.id])
        }else{
            redirect(action:'index',params: [project:params.project])
        }
    }

    def groupTreeFragment = {
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        def tree = scheduledExecutionService.getGroupTree(params.project,authContext)
        render(template:"/menu/groupTree",model:[jobgroups:tree,jscallback:params.jscallback])
    }

    def xmlerror={
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
    def xmlsuccess={
        render(contentType:"text/xml",encoding:"UTF-8"){
            delegate.'result'(error:"false"){
                delegate.'success'{
                    if(flash.message){
                        response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,flash.message)
                        delegate.'message'(flash.message)
                    }
                    if(flash.messages){
                        def p = delegate
                        flash.messages.each{ msg ->
                            p.'message'(msg)
                        }
                    }
                }
            }
        }
    }

    /**
     * used by jobs page, displays actions for the job as li's
     */

    @RdAuthorizeJob(RundeckAccess.Job.AUTH_APP_READ_OR_VIEW)
    @GrailsCompileStatic
    def actionMenuFragment(){
        ScheduledExecution scheduledExecution = getAuthorizingJob().resource
        String project = scheduledExecution.project
        AuthorizingProject authorizingProject = authorizingProject(project)

        def model=[
                scheduledExecution  : scheduledExecution,
                hideJobDelete       : params.hideJobDelete,
                jobDeleteSingle     : params.jobDeleteSingle,
                isScheduled         : scheduledExecutionService.isScheduled(scheduledExecution)
        ]

        if (authorizingProject.isAuthorized(RundeckAccess.Project.APP_SCM_EXPORT)) {
            def scmExportOptions = scheduledExecutionService.scmActionMenuOptions(project, authorizingProject.authContext, scheduledExecution) as LinkedHashMap<String, Object>
            model << scmExportOptions
        }
        if (authorizingProject.isAuthorized(RundeckAccess.Project.APP_SCM_IMPORT)) {
            def scmImportOptions = scheduledExecutionService.scmActionMenuOptions(project, authorizingProject.authContext, scheduledExecution) as LinkedHashMap<String, Object>
            model << scmImportOptions
        }

        render(template: '/scheduledExecution/jobActionButtonMenuContent', model: model)
    }

    private def jobDetailData(keys = []) {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        def crontab = scheduledExecution.timeAndDateAsBooleanMap()

        def total = -1
        if (keys.contains('total') || !keys) {
            def minLevel = configurationService.getString("min.isolation.level")
            def isolationLevel = (minLevel && minLevel=='UNCOMMITTED')?TransactionDefinition.ISOLATION_READ_UNCOMMITTED:TransactionDefinition.ISOLATION_DEFAULT
            total = Execution.withTransaction([isolationLevel: isolationLevel]) {
                Execution.countByScheduledExecution(scheduledExecution)
            }
        }

        def remoteClusterNodeUUID = null
        if (scheduledExecution.scheduled && frameworkService.isClusterModeEnabled()
                && scheduledExecution.serverNodeUUID != frameworkService.getServerUUID()) {
            remoteClusterNodeUUID = scheduledExecution.serverNodeUUID
        }

        def notificationPlugins = null
        if (keys.contains('notificationPlugins') || !keys) {
            notificationPlugins = notificationService.listNotificationPlugins()
        }
        def orchestratorPlugins = null
        if (keys.contains('orchestratorPlugins') || !keys) {
            orchestratorPlugins = orchestratorPluginService.getOrchestratorPlugins()
        }
        def nextExecution = null
        def isScheduled = scheduledExecutionService.isScheduled(scheduledExecution)
        if (keys.contains('nextExecution') || !keys) {
            nextExecution = isScheduled ? scheduledExecutionService.nextExecutionTime(
                    scheduledExecution
            ) : null
        }
        [scheduledExecution   : scheduledExecution,
         isScheduled          : isScheduled,
         crontab              : crontab,
         params               : params,
         total                : total,
         nextExecution        : nextExecution,
         remoteClusterNodeUUID: remoteClusterNodeUUID,
         max                  : params.max ? params.max : 10,
         notificationPlugins  : notificationPlugins,
         orchestratorPlugins  : orchestratorPlugins,
         offset               : params.offset ? params.offset : 0
        ]
    }
    def detailFragment () {
        log.debug("ScheduledExecutionController: detailFragment : params: " + params)
        Framework framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(notFoundResponse(scheduledExecution,'Job',params.id)){
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext, scheduledExecution,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
                scheduledExecution.project
            ),
            AuthConstants.ACTION_VIEW,
            'Job', params.id
        )) {
            return
        }
        def model=jobDetailData()

        return render(view:'jobDetailFragment',model: model)
    }
    def detailFragmentAjax () {
        if (requireAjax(action: 'show', controller: 'scheduledExecution', params: params)) {
            return
        }
        log.debug("ScheduledExecutionController: detailFragmentAjax : params: " + params)
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(notFoundResponse(scheduledExecution,'Job',params.id)){
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext, scheduledExecution,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
                scheduledExecution.project
            ),
            AuthConstants.ACTION_VIEW, 'Job', params.id
        )) {
            return
        }
        def model = jobDetailData(['total', 'nextExecution', 'max', 'scheduledExecution'])
        def se = model.scheduledExecution

        if (model.nextExecution) {

            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            model.nextExecutionW3CTime = format.format(model.nextExecution)
        }

        render(contentType: 'application/json') {
            total model.total
            nextExecution model.nextExecution
            nextExecutionW3CTime model.nextExecutionW3CTime
            max model.max
            job(
                    id: se.extid,
                    name: (se.jobName),
                    group: (se.groupPath),
                    project: (se.project),
                    description: (se.description),
                    href: apiService.apiHrefForJob(se),
                    permalink: apiService.guiHrefForJob(se),
                    filter: se.filter?:'',
                    doNodeDispatch: se.doNodedispatch
            )
        }
    }
    def show () {
        log.debug("ScheduledExecutionController: show : params: " + params)
        def infoMessage = flash.info
        def crontab = [:]
        def framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        def actions = [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]
        if (response.format in ['xml', 'yaml', 'json']) {
            actions = [AuthConstants.ACTION_READ]
        }
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext, scheduledExecution,
                actions,
                scheduledExecution.project
            ), AuthConstants.ACTION_READ, 'Job', params.id
        )) {
            return
        }

        if (!params.project || params.project != scheduledExecution.project) {
            flash.info = infoMessage
            return redirect(controller: 'scheduledExecution', action: 'show',
                    params: [id: params.id, project: scheduledExecution.project])
        }
        request.project=scheduledExecution.project
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        //list executions using query params and pagination params

        def minLevel = configurationService.getString("min.isolation.level")
        def isolationLevel = (minLevel && minLevel=='UNCOMMITTED')?TransactionDefinition.ISOLATION_READ_UNCOMMITTED:TransactionDefinition.ISOLATION_DEFAULT
        def total = Execution.withTransaction([isolationLevel: isolationLevel]) {
            Execution.countByScheduledExecution(scheduledExecution)
        }
        def reftotal = 0
        if(scheduledExecutionService.getRefExecCountStats(scheduledExecution.uuid)) {
            reftotal = scheduledExecutionService.getRefExecCountStats(scheduledExecution.uuid)
        }

        def remoteClusterNodeUUID=null
        def isScheduled = scheduledExecutionService.isScheduled(scheduledExecution)
        if (isScheduled && frameworkService.isClusterModeEnabled()) {
            remoteClusterNodeUUID = scheduledExecution.serverNodeUUID
        }


        def parentList = referencedExecutionDataProvider.parentJobSummaries(scheduledExecution.uuid,10)
        def isReferenced = parentList?.size()>0

        def pluginDescriptions=[:]
        if(featureService.featurePresent('executionLifecyclePlugin')) {
            pluginDescriptions[ServiceNameConstants.ExecutionLifecycle] = pluginService.
                    listPluginDescriptions(ServiceNameConstants.ExecutionLifecycle)
        }

        def model = ScheduledExecution.withNewSession {
            _prepareExecute(scheduledExecution, framework,authContext)
        }

        def jobComponents = rundeckJobDefinitionManager.getJobDefinitionComponents()
        def jobComponentValues=rundeckJobDefinitionManager.getJobDefinitionComponentValues(scheduledExecution)
        def dataMap= [
                isScheduled: isScheduled,
                scheduledExecution: scheduledExecution,
                isReferenced: isReferenced,
                parentList: parentList,
                crontab: crontab,
                params: params,
                total: total,
                reftotal: reftotal,
                nextExecution: scheduledExecutionService.nextExecutionTime(scheduledExecution),
                remoteClusterNodeUUID: remoteClusterNodeUUID,
                serverNodeUUID: frameworkService.isClusterModeEnabled()?frameworkService.serverUUID:null,
                notificationPlugins: notificationService.listNotificationPlugins(),
				orchestratorPlugins: orchestratorPluginService.getOrchestratorPlugins(),
                strategyPlugins: scheduledExecutionService.getWorkflowStrategyPluginDescriptions(),
                logFilterPlugins: pluginService.listPlugins(LogFilterPlugin),
                pluginDescriptions: pluginDescriptions,
                jobComponents: jobComponents,
                jobComponentValues: jobComponentValues,
                max: params.int('max') ?: 10,
                offset: params.int('offset') ?: 0] + model
        if (params.opt && (params.opt instanceof Map)) {
            dataMap.selectedoptsmap = params.opt
        }
        //add scm export status
        def projectResource = rundeckAuthContextProcessor.authResourceForProject(params.project)
        if (rundeckAuthContextProcessor.authorizeApplicationResourceAny(authContext,
                                                             projectResource,
                                                             [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_EXPORT,
                                                              AuthConstants.ACTION_SCM_EXPORT])) {
            if(scmService.projectHasConfiguredExportPlugin(params.project)){
                dataMap.scmExportEnabled = true
                dataMap.scmExportStatus = scmService.exportStatusForJob(scheduledExecution)
                dataMap.scmExportRenamedPath=scmService.getRenamedJobPathsForProject(params.project)?.get(scheduledExecution.extid)
            }
        }
        if (rundeckAuthContextProcessor.authorizeApplicationResourceAny(authContext,
                                                             projectResource,
                                                             [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_IMPORT,
                                                              AuthConstants.ACTION_SCM_IMPORT])) {
            if(scmService.projectHasConfiguredPlugin('import',params.project)) {
                dataMap.scmImportEnabled = true
                dataMap.scmImportStatus = scmService.importStatusForJobs(params.project, authContext, [scheduledExecution])
            }
        }

        withFormat{
            html{
                dataMap
            }
            yaml{
                response.setHeader("Content-Disposition","attachment; filename=\"${getFname(scheduledExecution.jobName)}.yaml\"")
                response.contentType='text/yaml; charset=UTF-8'

                response.outputStream.withWriter('UTF-8') { writer ->
                    rundeckJobDefinitionManager.exportAs('yaml', [scheduledExecution], writer)
                }
                flush(response)
            }
            json{
                response.setHeader("Content-Disposition","attachment; filename=\"${getFname(scheduledExecution.jobName)}.json\"")
                response.contentType='application/json; charset=UTF-8'

                response.outputStream.withWriter('UTF-8') { writer ->
                    rundeckJobDefinitionManager.exportAs('json', [scheduledExecution], writer)
                }
                flush(response)
            }
            xml{
                response.setHeader("Content-Disposition","attachment; filename=\"${getFname(scheduledExecution.jobName)}.xml\"")
                response.contentType='text/xml; charset=UTF-8'

                response.outputStream.withWriter('UTF-8') { writer ->
                    rundeckJobDefinitionManager.exportAs('xml', [scheduledExecution], writer)
                }
                flush(response)
            }
        }
    }
    private static String getFname(name){
        final Pattern s = Pattern.compile("[\\r\\n \"\\\\]")
        def fname=name.replaceAll(s,'_')
        if(fname.size()>74){
            fname = fname.substring(0,74)
        }
        fname
    }
    def runbook () {
        log.debug("ScheduledExecutionController: show : params: " + params)
        def crontab = [:]
        def framework = frameworkService.getRundeckFramework()
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext, scheduledExecution,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
                scheduledExecution.project
            ), AuthConstants.ACTION_VIEW, 'Job', params.id
        )) {
            return
        }

        if (!params.project || params.project != scheduledExecution.project) {
            return redirect(controller: 'scheduledExecution', action: 'runbook',
                    params: [id: params.id, project: scheduledExecution.project])
        }
        request.project=scheduledExecution.project
        crontab = scheduledExecution.timeAndDateAsBooleanMap()
        //list executions using query params and pagination params

        def minLevel = configurationService.getString("min.isolation.level")
        def isolationLevel = (minLevel && minLevel=='UNCOMMITTED')?TransactionDefinition.ISOLATION_READ_UNCOMMITTED:TransactionDefinition.ISOLATION_DEFAULT
        def total = Execution.withTransaction([isolationLevel: isolationLevel]) {
            Execution.countByScheduledExecution(scheduledExecution)
        }

        def remoteClusterNodeUUID=null
        if (scheduledExecution.scheduled && frameworkService.isClusterModeEnabled()) {
            remoteClusterNodeUUID = scheduledExecution.serverNodeUUID
        }


        def dataMap= [
                scheduledExecution: scheduledExecution,
                isScheduled: scheduledExecutionService.isScheduled(scheduledExecution),
                crontab: crontab,
                params: params,
                total: total,
                nextExecution: scheduledExecutionService.nextExecutionTime(scheduledExecution),
                remoteClusterNodeUUID: remoteClusterNodeUUID,
                serverNodeUUID: frameworkService.isClusterModeEnabled()?frameworkService.serverUUID:null,
                notificationPlugins: notificationService.listNotificationPlugins(),
				orchestratorPlugins: orchestratorPluginService.getOrchestratorPlugins(),
                max: params.int('max') ?: 10,
                offset: params.int('offset') ?: 0] + _prepareExecute(scheduledExecution, framework,authContext)


        dataMap
    }

    @Get(uri="/job/{id}/workflow")
    @Operation(
        method = 'GET',
        summary = 'Get Job Workflow',
        description = '''Get the workflow tree for a job. It will traverse referenced jobs to a depth of 3.

Authorization required: `read` or `view` for the Job.

The authorization level affects the response data.

* `read` - full workflow details are included for each step
* `view` - basic information and description is included for each step

Since: v34''',
        tags = ['jobs'],
        parameters = [
            @Parameter(
                name = 'id',
                in = ParameterIn.PATH,
                description = 'Job ID',
                required = true,
                schema = @Schema(type = 'string')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = '''Workflow response.

**Workflow Step Fields**
* `description`: Present and set to workflow step description if configured
* `exec`: If command step field is present and set to command string; otherwise
* `script`: Present and set to `"true"` if script step
* `scriptfile`: Present and set to file path if `scriptfile` step
* `scripturl`: If `scripturl` step field is present and set to URL if step
* `jobRef`: Present if step is a job reference
* `jobId`: If step is a job reference field is present and contains the referenced
jobs ID
* `type`: For plugin steps present and set to step plugin type
* `nodeStep`: Present if `type` is present and set to `"true"` or `"false"` to indicate
if the step is a node step. Implicitly `"true"` if not present and not a job step.
* `workflow`: If step is a job reference contains the sub-workflow''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{
    "workflow": [
        {
            "description": "[description]",
            "exec": "[exec]",
            "script": "[script]",
            "scriptfile": "[scriptfile]",
            "scripturl": "[scripturl]",
            "jobRef": {
                "name": "[name]",
                "group": "[group]",
                "uuid": "[uuid]",
                "nodeStep": "[nodeStep]",
                "importOptions": "[importOptions]"
            },
            "jobId": "[jobId]",
            "type": "[type]",
            "nodeStep": true,
            "workflow": []
        }
    ]
}''')
            )
        )
    )
    public def apiJobWorkflow (){
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V34)) {
            return
        }

        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                                                           code  : 'api.error.item.doesnotexist',
                                                           args  : ['Job ID', params.id]])
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(
                session.subject,
                scheduledExecution.project
        )
        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectJobAny(
                        authContext,
                        scheduledExecution,
                        [AuthConstants.ACTION_READ,AuthConstants.ACTION_VIEW],
                        scheduledExecution.project
                ),
                AuthConstants.ACTION_VIEW, 'Job', params.id
        )) {
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code  : 'api.error.item.unauthorized',
                    args: ['View', 'Job ' + 'ID', params.id]
                ]
            )
        }
        def maxDepth=3

        def readAuth = rundeckAuthContextProcessor.authorizeProjectJobAny(
            authContext,
            scheduledExecution,
            [AuthConstants.ACTION_READ],
            scheduledExecution.project
        )
        def wfdata=scheduledExecutionService.getWorkflowDescriptionTree(scheduledExecution.project,scheduledExecution.workflow,readAuth,maxDepth)
        withFormat {
            json {
                render(contentType: 'application/json') {
                    workflow wfdata
                }
            }
            xml {
                render(contentType: 'application/xml') {
                    workflow wfdata
                }
            }
        }
    }
    /**
     * Sanitize the html text submitted
     * @return
     */
    def sanitizeHtml(){
        if(request.JSON.content){
            return render(contentType: 'application/json'){
                content request.JSON.content.toString().encodeAsSanitizedHTML()
            }
        }
        apiService.renderErrorFormat(response, [
                status: HttpServletResponse.SC_BAD_REQUEST,
                code: "api.error.invalid.request",
                args: ["content expected"],
                format: 'json'
        ])

    }
    /**
     * check crontabString parameter if it is a valid crontab, and render any syntax warnings
     */
    def checkCrontab={
        if(!params.crontabString){
            request.error="crontabString parameter is required"
        }else{
            if(!CronExpression.isValidExpression(params.crontabString)){
                def x = params.crontabString.split(" ")
                if(x && x.size()>6 && x [3] != '?' && x [5]!='?'){
                    request.warn="day of week or day of month must be '?'"
                }else{
                    request.warn="Format invalid"
                }
            }
        }
        render(template:'/common/messages')
    }

    /**
     * This action loads the JSON data from the URL specified in
     * an option's "valueSrc" property, and renders the optionValuesSelect template
     * using the data.
     */
    def loadRemoteOptionValues(){
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext, scheduledExecution,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
                scheduledExecution.project
            ), AuthConstants.ACTION_VIEW, 'Job', params.id
        )) {
            return
        }
        if(!params.option){
            log.error("option missing")
            return renderErrorFragment("option missing")
        }

        //see if option specified, and has url
        if (scheduledExecution.options && scheduledExecution.options.find {it.name == params.option}) {
            Option opt = scheduledExecution.options.find {it.name == params.option}
            if (opt.realValuesUrl) {
                Map optionRemoteValues = scheduledExecutionService.loadOptionsRemoteValues(scheduledExecution, params, session.user, authContext)
                def model = [optionSelect : optionRemoteValues.optionSelect,
                             values       : optionRemoteValues.values,
                             srcUrl       : optionRemoteValues.srcUrl,
                             err          : optionRemoteValues.err,
                             fieldPrefix  : params.fieldPrefix,
                             selectedvalue: params.selectedvalue]
                if (params.extra?.option?.get(opt.name)) {
                    model.selectedoptsmap = [(opt.name): params.extra.option.get(opt.name)]
                }
                withFormat {
                    html {
                        return render(template: "/framework/optionValuesSelect", model: model);
                    }
                    json {
                        model.remove('optionSelect')
                        model.name = opt.name
                        if (model.err?.exception) {
                            model.err.exception = model.err.exception.toString()
                        }
                        render(contentType: 'application/json', text: model as JSON)
                    }
                }
            } else {

                withFormat {
                    html {
                        return renderErrorFragment("not a url option: " + params.option)
                    }
                    json {
                        render(contentType: 'application/json', text: [err: [message: "not a url option: " + params.option]] as JSON)
                    }
                }
            }
        }else{

            withFormat{
                html{
                    return renderErrorFragment("option not found: "+params.option)
                }
                json{
                    render(contentType: 'application/json', text: [err:[message:"option not found: "+params.option]] as JSON)
                }
            }
        }
    }
    /**
     * Map of descriptive property name to ScheduledExecution domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static jobprops=[
        name:'jobName',
        group:'groupPath',
        description:'description',
        project:'project',
    ]
    /**
     * Map of descriptive property name to Option domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static optprops=[
        name:'name',

    ]

    /**
     * Make a remote URL request and return the parsed JSON data and statistics for http requests in a map.
     * if an error occurs, a map with a single 'error' entry will be returned.
     * the stats data contains:
     *
     * url: requested url
     * startTime: start time epoch ms
     * httpStatusCode: http status code (int)
     * httpStatusText: http status text
     * finishTime: finish time epoch ms
     * durationTime: duration time in ms
     * contentLength: response content length bytes (long)
     * lastModifiedDate: Last-Modified header (Date)
     * contentSHA1: SHA1 hash of the content
     *
     * @param url URL to request
     * @param timeout request timeout in seconds
     * @return Map of data, [json: parsed json or null, stats: stats data, error: error message]
     *
     */
    static Object getRemoteJSON(HttpClientCreator clientCreator, String url, JobOptionConfigRemoteUrl configRemoteUrl, int timeout, int contimeout, int retry=5,boolean disableRemoteOptionJsonCheck=false){
        logger.debug("getRemoteJSON: "+url+", timeout: "+timeout+", retry: "+retry)

        //attempt to get the URL JSON data
        def stats=[:]
        if(url.startsWith("http:") || url.startsWith("https:")){
            HttpClient<HttpResponse> client = clientCreator.createClient()
            client.setFollowRedirects(true)
            client.setTimeout(timeout*1000)


            URL urlo
            String cleanUrl = url.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
            try{
                urlo = new URL(url)
                if(urlo.userInfo){
                    client.setUri(new URL(cleanUrl).toURI())
                    UsernamePasswordCredentials cred = new UsernamePasswordCredentials(urlo.userInfo)
                    client.setBasicAuthCredentials(cred.userName, cred.password)
                } else if (configRemoteUrl && configRemoteUrl.authenticationType) {
                    logger.debug("getRemoteJSON using authentication")
                    URIBuilder uriBuilder = new URIBuilder(cleanUrl)

                    if(configRemoteUrl.getAuthenticationType()==RemoteUrlAuthenticationType.BASIC){
                        client.setUri(uriBuilder.build())
                        logger.debug("Basic authentication with user ${configRemoteUrl.getUsername()}")
                        client.setBasicAuthCredentials(configRemoteUrl.getUsername(), configRemoteUrl.getPassword())
                    }
                    if(configRemoteUrl.getAuthenticationType()==RemoteUrlAuthenticationType.API_KEY){
                        if(configRemoteUrl.getApiTokenReporter()== ApiTokenReporter.HEADER){
                            logger.debug("API key authentication with Header ${configRemoteUrl.getKeyName()}" )
                            client.addHeader(configRemoteUrl.getKeyName(), configRemoteUrl.getToken())
                        }else{
                            logger.debug("API key authentication with query parameter ${configRemoteUrl.getKeyName()}" )
                            //add as a query Param
                            uriBuilder.addParameter(configRemoteUrl.getKeyName(), configRemoteUrl.getToken())
                        }
                        client.setUri(uriBuilder.build())
                    }
                    if(configRemoteUrl.getAuthenticationType()==RemoteUrlAuthenticationType.BEARER_TOKEN){
                        logger.debug("Bearer Token Authentication" )
                        client.addHeader("Authorization", "Bearer ${configRemoteUrl.getToken()}")
                        client.setUri(uriBuilder.build())
                    }
                } else{
                    client.setUri(urlo.toURI())
                }
            }catch(MalformedURLException e){
                throw new Exception("Failed to configure base URL for authentication: "+e.getMessage(),e)
            }
            client.addHeader("Accept","application/json")
            stats.url = cleanUrl;
            stats.startTime = System.currentTimeMillis();
            def results = [:]
            client.execute { response ->
                int resultCode = response.statusLine.statusCode
                stats.httpStatusCode = response.statusLine.statusCode
                stats.httpStatusText = response.statusLine.reasonPhrase
                stats.finishTime = System.currentTimeMillis()
                stats.durationTime=stats.finishTime-stats.startTime
                stats.contentLength = response.getEntity().contentLength
                final header = response.getFirstHeader("Last-Modified")
                if(null!=header){
                    stats.lastModifiedDate= DateUtils.parseDate(header.getValue())
                }else{
                    stats.lastModifiedDate=""
                    stats.lastModifiedDateTime=""
                }

                def reasonCode = response.statusLine.reasonPhrase
                if(resultCode>=200 && resultCode<=300){
                    def expectedContentType="application/json"
                    def resultType=''
                    if (null != response.getFirstHeader("Content-Type")) {
                        resultType = response.getFirstHeader("Content-Type").getValue();
                    }
                    String type = resultType;
                    if (type.indexOf(";") > 0) {
                        type = type.substring(0, type.indexOf(";")).trim();
                    }

                    boolean continueRendering=true

                    if(!disableRemoteOptionJsonCheck &&
                       !expectedContentType.equals(type)){
                        continueRendering=false
                    }

                    if (continueRendering) {
                        final stream = response.getEntity().content
                        final writer = new StringWriter()
                        int len=copyToWriter(new BufferedReader(new InputStreamReader(stream)),writer)
                        stream.close()
                        writer.flush()
                        final string = writer.toString()
                        def parseResult=remoteUrlParse(string, configRemoteUrl)

                        if(parseResult.error){
                            results = [error:parseResult.error]
                        }else{
                            if(parseResult.string){
                                stats.contentSHA1=parseResult.string.encodeAsSHA1()
                                if(stats.contentLength<0){
                                    stats.contentLength= len
                                }
                            }else{
                                stats.contentSHA1=""
                            }
                            results = [json:parseResult.jsonElement,stats:stats]
                        }
                    }else{
                        results = [error:"Unexpected content type received: "+resultType,stats:stats]
                    }
                }else{
                    stats.contentSHA1 = ""
                    results = [error:"Server returned an error response: ${resultCode} ${reasonCode}",stats:stats]
                }
            }
            return results
        }else if (url.startsWith("file:")) {
            stats.url=url
            def File srfile = new File(new URI(url))
            final writer = new StringWriter()
            final stream= new FileInputStream(srfile)

            stats.startTime = System.currentTimeMillis();
            int len = copyToWriter(new BufferedReader(new InputStreamReader(stream)), writer)
            stats.finishTime = System.currentTimeMillis()
            stats.durationTime = stats.finishTime - stats.startTime
            stream.close()
            writer.flush()
            final string = writer.toString()
            final JSONElement parse = grails.converters.JSON.parse(string)
            if (string) {
                stats.contentSHA1 = string.encodeAsSHA1()
            }else{
                stats.contentSHA1 = ""
            }
            stats.contentLength=srfile.length()
            stats.lastModifiedDate=new Date(srfile.lastModified())
            stats.lastModifiedDateTime=srfile.lastModified()
            return [json:parse,stats:stats]
        } else {
            throw new Exception("Unsupported protocol: " + url)
        }
    }


    static Map<String, Object> remoteUrlParse(String payload, JobOptionConfigRemoteUrl jobOptionConfigRemoteUrl){

        String jsonFilter = jobOptionConfigRemoteUrl?.getJsonFilter()
        if(!jsonFilter){
            return [jsonElement: grails.converters.JSON.parse(payload), string: payload]
        }

        def jpath = JsonPath.using(Configuration.defaultConfiguration())
        try {
            def jsonFilterResult = jpath.parse(payload).read(jobOptionConfigRemoteUrl.getJsonFilter())
            if(jsonFilterResult instanceof ArrayList){
                return [error: "the filter ${jobOptionConfigRemoteUrl.getJsonFilter()} return a list, please use another filter"]
            }
            return [jsonElement: jsonFilterResult, string: jsonFilterResult.toString()]

        }catch (Exception e){
            return [error: e.getMessage()]
        }

    }

    static int copyToWriter(Reader read, Writer writer){
        char[] chars = new char[1024];
        int len=0;
        int size=read.read(chars,0,chars.length)
        while(-1!=size){
            len+=size;
            writer.write(chars,0,size)
            size = read.read(chars, 0, chars.length)
        }
        return len;
    }

    def flipScheduleEnabled() {
        withForm{
        if (!params.id) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
        }

        def jobid = params.id

            def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
                return
            }

            Framework framework = frameworkService.getRundeckFramework()
            UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(
                    session.subject,
                    scheduledExecution.project
            )
            def changeinfo = [method: 'update', change: 'modify', user: session.user]

            //pass session-stored edit state in params map
            transferSessionEditState(session, params, params.id)

            String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

            def payload = [id: params.id, scheduleEnabled: params.scheduleEnabled]
            def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)
            if(!result.success){
                flash.error=result.message
            }
            if(params.returnToJob=='true'){
                return redirect(controller: 'scheduledExecution', action: 'show', params: [project: params.project,id:scheduledExecution.extid])
            }
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])

        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    def flipExecutionEnabled() {
        withForm{

            if (!params.id) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
            }

            def jobid = params.id
            def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
                return
            }

            Framework framework = frameworkService.getRundeckFramework()
            UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(
                    session.subject,
                    scheduledExecution.project
            )
            def changeinfo = [method: 'update', change: 'modify', user: authContext.username]

            //pass session-stored edit state in params map
            transferSessionEditState(session, params, params.id)

            String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

            def payload = [id: params.id, executionEnabled: params.executionEnabled]
            def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)
            if(!result.success){
                flash.error=result.message
            }
            if(params.returnToJob=='true'){
                return redirect(controller: 'scheduledExecution', action: 'show', params: [project: params.project,id:scheduledExecution.extid])
            }
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])

        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    @Post(uri = '/job/{id}/execution/disable')
    @Operation(
        method = 'POST',
        summary = 'Disable Executions for a Job',
        description = '''Disable executions for a job. 

Authorization required: `toggle_execution` action for a job.

Since: V14''',
        tags = ['jobs'],

        parameters = @Parameter(
            name = "id",
            description = "Job ID",
            in = ParameterIn.PATH,
            required = true,
            content = @Content(schema = @Schema(implementation = String))
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Success Response',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{"success": true}''')
            )
        )
    )
    protected def apiFlipExecutionDisabled(){}

    @Post(uri = '/job/{id}/execution/enable')
    @Operation(
        method = 'POST',
        summary = 'Enable Executions for a Job',
        description = '''Enable executions for a job. 

Authorization required: `toggle_execution` action for a job.

Since: V14''',
        tags = ['jobs'],

        parameters = @Parameter(
            name = "id",
            description = "Job ID",
            in = ParameterIn.PATH,
            required = true,
            content = @Content(schema = @Schema(implementation = String))
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Success Response',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{"success": true}''')
            )
        )
    )
    def apiFlipExecutionEnabled() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V14)) {
            return
        }


        log.debug("ScheduledExecutionController: apiFlipExecutionEnabled" + params)

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def scheduledExecution = ScheduledExecution.getByIdOrUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            //job does not exist
            return
        }

        def Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)

        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_EXECUTION],
                scheduledExecution.project)) {
            def error = [status: HttpServletResponse.SC_FORBIDDEN, code  : 'api.error.item.unauthorized', args: ['Toggle Execution', 'Job ID', params.id]]
            return apiService.renderErrorFormat(response, error)
        }

        def changeinfo = [method: 'update', change: 'modify', user: session.user]
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

        def payload = [id: params.id, executionEnabled: params.status]
        def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)

        if (result && result.success) {
            return withFormat {
                xml {
                    render(text: "<success>true</success>",contentType:"text/xml",encoding:"UTF-8")
                }

                json {
                    render ([success: true] as JSON)
                }
            }
        } else {
            return apiService.renderErrorFormat(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
        }
    }

    @Post(uri = '/job/{id}/schedule/disable')
    @Operation(
        method = 'POST',
        summary = 'Disable Schedule for a Job',
        description = '''Disable schedule for a job. 

Authorization required: `toggle_schedule` action for a job.

Since: V14''',
        tags = ['jobs'],

        parameters = @Parameter(
            name = "id",
            description = "Job ID",
            in = ParameterIn.PATH,
            required = true,
            content = @Content(schema = @Schema(implementation = String))
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Success Response',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{"success": true}''')
            )
        )
    )
    protected def apiFlipScheduleDisabled() {}

    @Post(uri = '/job/{id}/schedule/enable')
    @Operation(
        method = 'POST',
        summary = 'Enable Schedule for a Job',
        description = '''Enable schedule for a job. 

Authorization required: `toggle_schedule` action for a job.

Since: V14''',
        tags = ['jobs'],

        parameters = @Parameter(
            name = "id",
            description = "Job ID",
            in = ParameterIn.PATH,
            required = true,
            content = @Content(schema = @Schema(implementation = String))
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Success Response',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{"success": true}''')
            )
        )
    )
    def apiFlipScheduleEnabled() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V14)) {
            return
        }

        log.debug("ScheduledExecutionController: apiFlipScheduleEnabled" + params)

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def scheduledExecution = ScheduledExecution.getByIdOrUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            //job does not exist
            return
        }

        def Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)

        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_SCHEDULE],
                scheduledExecution.project)) {
            def error = [status: HttpServletResponse.SC_FORBIDDEN, code  : 'api.error.item.unauthorized', args: ['Toggle Schedule', 'Job ID', params.id]]
            return apiService.renderErrorFormat(response, error)
        }

        def changeinfo = [method: 'update', change: 'modify', user: session.user]
        String roleList = request.subject.getPrincipals(Group.class).collect { it.name }.join(",")

        def payload = [id: params.id, scheduleEnabled: params.status]
        def result = scheduledExecutionService._doUpdateExecutionFlags(payload, session.user, roleList, framework, authContext, changeinfo)

        if (result && result.success) {
            return withFormat {
                xml {
                    render(text: "<success>true</success>", contentType:"text/xml", encoding:"UTF-8")
                }

                json {
                    render ([success: true] as JSON)
                }
            }
        } else {
            return apiService.renderErrorFormat(response, [status: result.status?:400, code: result.errorCode] + result);
        }
    }

    /**
    */
    def delete(){
        if (!params.id) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            return renderErrorView(g.message(code: 'api.error.parameter.required', args: ['id']))
        }
        def jobid=params.id

        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectResource(
                        authContext,
                        AuthConstants.RESOURCE_TYPE_JOB,
                        AuthConstants.ACTION_DELETE,
                        scheduledExecution.project
                ) && rundeckAuthContextProcessor.authorizeProjectJobAll(
                        authContext,
                        scheduledExecution,
                        [AuthConstants.ACTION_DELETE],
                        scheduledExecution.project
                ),
                AuthConstants.ACTION_DELETE,
                'Job',
                params.id
        )) {
            return
        }
        if(request.method=='POST') {
            def isReferenced = referencedExecutionDataProvider.countByJobUuid(scheduledExecution.uuid)>0
            withForm {
                def result = scheduledExecutionService.deleteScheduledExecutionById(
                        jobid,
                        authContext,
                        params.deleteExecutions == 'true',
                        session.user,
                        'delete'
                )
                if (!result.success) {
                    return renderErrorView(result.error.message)
                } else {
                    def project = result.success.job ? result.success.job.project : params.project
                    flash.bulkJobResult = [success: [result.success]]
                    if(isReferenced){
                        def err = [
                                message: g.message(code: 'deleted.referenced.job'),
                                errorCode: 'jobref',
                                id: jobid
                        ]
                        flash.bulkJobResult+=[errors:[err]]
                    }
                    redirect(controller: 'menu', action: 'jobs', params: [project: project])
                }
            }.invalidToken {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                request.errorCode = 'request.error.invalidtoken.message'
                return renderErrorView([:])
            }
        }else{
            return [scheduledExecution: scheduledExecution]
        }

    }
    /**
     * Enable execution for a set of jobs.
     * Only allowed via POST http method
     */
    def flipExecutionEnabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipExecutionEnabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipExecutionEnabledBulk', [executionEnabled: true], 'api.success.job.execution.enabled')
    }

    private def handleFormFlipJobFlagBulk(ApiBulkJobDeleteRequest deleteRequest, String methodName, Map flags, String successCode) {
        if (deleteRequest.hasErrors()) {
            flash.bulkJobResult = [success: false, errors: deleteRequest.errors]
            return redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
        }
        withForm {
            if (!params.ids && !params.idlist) {
                flash.error = g.message(code: 'ScheduledExecutionController.bulkUpdate.empty')
                return redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
            }
            flash.bulkJobResult = performFlipJobFlagBulk(deleteRequest,methodName,flags, successCode)
            redirect(controller: 'menu', action: 'jobs', params: [project: params.project])
        }.invalidToken {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    private def performFlipJobFlagBulk(ApiBulkJobDeleteRequest deleteRequest,String methodName,Map flags, String successCode) {

        def ids = deleteRequest.generateIdSet()

        def successful = []
        def errs = []
        def framework = frameworkService.getRundeckFramework()
        ids.sort().each { jobid ->
            ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)
            def result = [success: false]
            if(scheduledExecution) {
                UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, scheduledExecution.project)
                def changeinfo = [method: methodName, change: 'modify', user: authContext.username]
                result = scheduledExecutionService._doUpdateExecutionFlags(
                        [id: jobid] + flags,
                        authContext.username,
                        authContext.roles.join(','),
                        framework,
                        authContext,
                        changeinfo
                )
            }
            if (!result.success) {
                if (result.unauthorized) {
                    errs << [id       : jobid,
                             errorCode: result.errorCode,
                             message  : result.message ?:
                                     g.message(code: result.errorCode, args: ['Job', "{{Job " + jobid + "}}"])
                    ]
                } else if (result.errorCode) {
                    errs << [id     : jobid, errorCode: result.errorCode,
                             message: result.message ?:
                                     g.message(code: result.errorCode, args: ['Job', "{{Job " + jobid + "}}"]
                                     )]
                } else if (result.error) {
                    errs << result.error
                } else {
                    errs << [id: jobid, message: result.message]
                }
            } else {
                def jobtitle = "{{Job " + result.scheduledExecution.extid + "}}"
                successful << [message: g.message(code: successCode, args: [jobtitle]),id:jobid]
            }
        }
        return [success: successful, errors: errs]
    }

    /**
     * Disable execution for a set of jobs.
     * Only allowed via POST http method
     */
    def flipExecutionDisabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipExecutionDisabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipExecutionDisabledBulk', [executionEnabled: false], 'api.success.job.execution.disabled')

    }
    /**
     * Enable schedule for a set of jobs.
     * Only allowed via POST http method
     */
    def flipScheduleEnabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipScheduleEnabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipScheduleEnabledBulk', [scheduleEnabled: true], 'api.success.job.schedule.enabled')
    }
    /**
     * Disable schedule for a set of jobs.
     * Only allowed via POST http method
     */
    def flipScheduleDisabledBulk (ApiBulkJobDeleteRequest deleteRequest) {
        log.debug("ScheduledExecutionController: flipScheduleDisabledBulk : params: " + params)
        return handleFormFlipJobFlagBulk(deleteRequest, 'flipScheduleDisabledBulk', [scheduleEnabled: false], 'api.success.job.schedule.disabled')

    }
    /**
     * Delete a set of jobs as specified in the idlist parameter.
     * Only allowed via POST http method
     */
    def deleteBulk (ApiBulkJobDeleteRequest deleteRequest) {
        if(deleteRequest.hasErrors()){
            flash.errors = deleteRequest.errors
            return redirect(controller: 'menu', action: 'jobs')
        }
        log.debug("ScheduledExecutionController: deleteBulk : params: " + params)
        withForm{
            if (!params.ids && !params.idlist) {
                flash.error = g.message(code: 'ScheduledExecutionController.bulkDelete.empty')
                return redirect(controller: 'menu', action: 'jobs')
            }
            AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
            def ids = deleteRequest.generateIdSet()

            def successful = []
            def deleteerrs = []
            ids.sort().each {jobid ->
                def result = scheduledExecutionService.deleteScheduledExecutionById(jobid, authContext,
                        params.deleteExecutions == 'true', session.user, 'deleteBulk')
                if (result.errorCode) {
                    deleteerrs << [id: jobid, errorCode: result.errorCode, message: g.message(code: result.errorCode, args: ['Job ID', jobid])]
                }else if (result.error) {
                    deleteerrs << result.error
                } else {
                    successful << result.success
                }
            }
            flash.bulkJobResult = [success: successful, errors: deleteerrs]
            redirect(controller: 'menu', action: 'jobs',params:[project:params.project])
        }.invalidToken{
            response.status = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
            return renderErrorView([:])
        }
    }

    @Post(uri='/jobs/execution/disable')
    @Operation(
        method = "POST",
        summary = "Bulk Toggle Job Execution Disabled",
        description = '''Toggle executions disabled for a set of jobs.

Authorization required: `toggle_execution` action for each job.

Since: v16''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name='ids',
                in=ParameterIn.QUERY,
                description="The Job IDs to delete, can be specified multiple times",
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name='idlist',
                in=ParameterIn.QUERY,
                description='The Job IDs to delete as a single comma-separated string.',
                schema=@Schema(type='string',format='comma-separated')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = '''Bulk toggle result.

Failed results will contain:

* `id` - the Job ID
* `error` - result error message for the request
* `errorCode` - a code indicating the type of failure, currently one of `failed`, `unauthorized` or 
`notfound`.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''
{
  "requestCount": 2,
  "enabled": true,
  "allsuccessful": false,
  "succeeded": [
      {
      "id": "[UUID]",
      "message": "success message"
    }
  ],
  "failed":[
      {
      "id": "[UUID]",
      "errorCode": "(error code, see above)",
      "message": "(success or failure message)"
    }]
}''')
            )
        )
    )
    protected def apiFlipExecutionDisabledBulk(
        @RequestBody(
            description = "request",
            content = @Content(
                mediaType = 'application/json',
                schema = @Schema(implementation = ApiBulkJobDeleteRequest)
            )
        )
            ApiBulkJobDeleteRequest deleteRequest
    ){}

    @Post(uri='/jobs/execution/enable')
    @Operation(
        method = "POST",
        summary = "Bulk Toggle Job Execution Enabled",
        description = '''Toggle executions enabled for a set of jobs.

Authorization required: `toggle_execution` action for each job.

Since: v16''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name='ids',
                in=ParameterIn.QUERY,
                description="The Job IDs to delete, can be specified multiple times",
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name='idlist',
                in=ParameterIn.QUERY,
                description='The Job IDs to delete as a single comma-separated string.',
                schema=@Schema(type='string',format='comma-separated')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = '''Bulk toggle result.

Failed results will contain:

* `id` - the Job ID
* `error` - result error message for the request
* `errorCode` - a code indicating the type of failure, currently one of `failed`, `unauthorized` or 
`notfound`.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''
{
  "requestCount": 2,
  "enabled": true,
  "allsuccessful": false,
  "succeeded": [
      {
      "id": "[UUID]",
      "message": "success message"
    }
  ],
  "failed":[
      {
      "id": "[UUID]",
      "errorCode": "(error code, see above)",
      "message": "(success or failure message)"
    }]
}''')
            )
        )
    )
    def apiFlipExecutionEnabledBulk(
        @RequestBody(
            description = "Bulk ID request",
            content = @Content(
                mediaType = 'application/json',
                schema = @Schema(implementation = ApiBulkJobDeleteRequest)
            )
        )
            ApiBulkJobDeleteRequest deleteRequest
    ) {
        if(!apiService.requireApi(request,response,ApiVersions.V16)){
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.invalid.request',
                                                           args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiFlipExecutionEnabledBulk : params: " + params)

        def ids = deleteRequest.generateIdSet()
        if(!ids) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist','id'])) {
                return
            }
        }
        def result = performFlipJobFlagBulk(
                deleteRequest,
                'apiFlipExecutionEnabledBulk',
                [executionEnabled: params.status],
                'api.success.job.execution.'+(params.status?'enabled':'disabled')
        )
        def successful = result.success
        def errors=result.errors

        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'toggleExecution'(
                            enabled: params.status,
                            requestCount: ids.size(),
                            allsuccessful: (successful.size() == ids.size())
                    ) {
                        if (successful) {
                            delegate.'succeeded'(count: successful.size()) {
                                successful.each { del ->
                                    delegate.'toggleExecutionResult'(id: del.id,) {
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if (errors) {
                            delegate.'failed'(count: errors.size()) {
                                errors.each { del ->
                                    delegate.'toggleExecutionResult'(id: del.id, errorCode: del.errorCode) {
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    enabled=params.status
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.id,message:del.message)
                            }
                        }
                    }
                    if(errors){
                        delegate.'failed'=array {
                            errors.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }

    @Post(uri='/jobs/schedule/disable')
    @Operation(
        method = "POST",
        summary = "Bulk Toggle Job Schedule Disabled",
        description = '''Toggle schedule disabled for a set of jobs.

Authorization required: `toggle_schedule` action for each job.

Since: v16''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name='ids',
                in=ParameterIn.QUERY,
                description="The Job IDs to delete, can be specified multiple times",
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name='idlist',
                in=ParameterIn.QUERY,
                description='The Job IDs to delete as a single comma-separated string.',
                schema=@Schema(type='string',format='comma-separated')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = '''Bulk toggle result.

Failed results will contain:

* `id` - the Job ID
* `error` - result error message for the request
* `errorCode` - a code indicating the type of failure, currently one of `failed`, `unauthorized` or 
`notfound`.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''
{
  "requestCount": 2,
  "enabled": true,
  "allsuccessful": false,
  "succeeded": [
      {
      "id": "[UUID]",
      "message": "success message"
    }
  ],
  "failed":[
      {
      "id": "[UUID]",
      "errorCode": "(error code, see above)",
      "message": "(success or failure message)"
    }]
}''')
            )
        )
    )
    protected def apiFlipScheduleDisabledBulk(
        @RequestBody(
            description = "Bulk ID request",
            content = @Content(
                mediaType = 'application/json',
                schema = @Schema(implementation = ApiBulkJobDeleteRequest)
            )
        )
            ApiBulkJobDeleteRequest deleteRequest
    ){}

    @Post(uri='/jobs/schedule/enable')
    @Operation(
        method = "POST",
        summary = "Bulk Toggle Job Schedule Enabled",
        description = '''Toggle schedule enabled for a set of jobs.

Authorization required: `toggle_schedule` action for each job.

Since: v16''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name='ids',
                in=ParameterIn.QUERY,
                description="The Job IDs to delete, can be specified multiple times",
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name='idlist',
                in=ParameterIn.QUERY,
                description='The Job IDs to delete as a single comma-separated string.',
                schema=@Schema(type='string',format='comma-separated')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = '''Bulk toggle result.

Failed results will contain:

* `id` - the Job ID
* `error` - result error message for the request
* `errorCode` - a code indicating the type of failure, currently one of `failed`, `unauthorized` or 
`notfound`.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''
{
  "requestCount": 2,
  "enabled": true,
  "allsuccessful": false,
  "succeeded": [
      {
      "id": "[UUID]",
      "message": "success message"
    }
  ],
  "failed":[
      {
      "id": "[UUID]",
      "errorCode": "(error code, see above)",
      "message": "(success or failure message)"
    }]
}''')
            )
        )
    )
    def apiFlipScheduleEnabledBulk(
        @RequestBody(
            description = "Bulk ID request",
            content = @Content(
                mediaType = 'application/json',
                schema = @Schema(implementation = ApiBulkJobDeleteRequest)
            )
        )
            ApiBulkJobDeleteRequest deleteRequest
    ) {
        if(!apiService.requireApi(request,response,ApiVersions.V16)){
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.invalid.request',
                                                           args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiFlipScheduleEnabledBulk : params: " + params)

        def ids = deleteRequest.generateIdSet()
        if(!ids) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist','id'])) {
                return
            }
        }
        def result = performFlipJobFlagBulk(
                deleteRequest,
                'apiFlipScheduleEnabledBulk',
                [scheduleEnabled: params.status],
                'api.success.job.schedule.'+(params.status?'enabled':'disabled')
        )
        def successful = result.success
        def errors=result.errors

        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'toggleSchedule'(
                            enabled: params.status,
                            requestCount: ids.size(),
                            allsuccessful: (successful.size() == ids.size())
                    ) {
                        if (successful) {
                            delegate.'succeeded'(count: successful.size()) {
                                successful.each { del ->
                                    delegate.'toggleScheduleResult'(id: del.id,) {
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if (errors) {
                            delegate.'failed'(count: errors.size()) {
                                errors.each { del ->
                                    delegate.'toggleScheduleResult'(id: del.id, errorCode: del.errorCode) {
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    enabled=params.status
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.id,message:del.message)
                            }
                        }
                    }
                    if(errors){
                        delegate.'failed'=array {
                            errors.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }

    static final String API_DOC_JOB_DELETE_TITLE ='Bulk Job Delete'
    static final String API_DOC_JOB_DELETE_DESC ="""Delete multiple job definitions at once.

Both `DELETE` and `POST` are allowed for doing a bulk delete of jobs. 
However, to send a body with the request, 
then the POST method must be used, 
since the DELETE method does not allow for request bodies.
 
Authorization required: `delete` on project resource type `job`, and `delete` on each Job resource.
"""

    @Delete('/jobs/delete')
    @Operation(
        method="DELETE",
        summary=API_DOC_JOB_DELETE_TITLE,
        description = API_DOC_JOB_DELETE_DESC,
        tags=['jobs'],
        parameters = [
            @Parameter(
                name = "ids",
                description = "The Job IDs to delete, can be specified multiple times",
                in = ParameterIn.QUERY,
                required = false,
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name = "idlist",
                description = "The Job IDs to delete as a single comma-separated string.",
                in = ParameterIn.QUERY,
                required = false,
                schema = @Schema(type = 'string', format = 'comma-separated')
            )
        ]
    )
    @ApiResponse(
        ref = '#/paths/~1jobs~1delete/post/responses/200'
    )
    protected def apiJobDeleteBulk_docs2(){}

    @Post('/jobs/delete')
    @Operation(
        method = "POST",
        summary = API_DOC_JOB_DELETE_TITLE,
        description = API_DOC_JOB_DELETE_DESC,
        tags = ['jobs']
    )
    @ApiResponse(
        responseCode='200',
        description = """Summary of bulk delete results""",
        content=[@Content(
            mediaType = MediaType.APPLICATION_JSON,
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
        )]

    )
    /**
     * Delete a set of jobs as specified in the ids, idlist params, or request body
     * API: /api/5/jobs/delete, version 5
    */
    def apiJobDeleteBulk(
        @RequestBody(
            description = "Bulk ID request",
            content = @Content(
                mediaType = 'application/json',
                schema = @Schema(implementation = ApiBulkJobDeleteRequest)
            )
        )
        ApiBulkJobDeleteRequest deleteRequest
    ) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (deleteRequest.hasErrors()) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: [deleteRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]])
        }
        log.debug("ScheduledExecutionController: apiJobDeleteBulk : params: " + params)
        if(!deleteRequest.ids && !deleteRequest.idlist) {
            if (!apiService.requireAnyParameters(params, response, ['ids', 'idlist'])) {
                return
            }
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        def ids = new HashSet<String>()
        if(params.id){
            ids.add(params.id)
        }else{
            if (deleteRequest.ids){
                ids.addAll(deleteRequest.ids)
            }
            if(deleteRequest.idlist){
                ids.addAll(deleteRequest.idlist.split(','))
            }
        }

        def successful = []
        def deleteerrs=[]
        ids.sort().each{jobid->
            def result = scheduledExecutionService.deleteScheduledExecutionById(jobid,authContext, false, session.user, 'apiJobDeleteBulk')
            if (result.errorCode) {
                deleteerrs << [id:jobid,errorCode:result.errorCode,message: g.message(code: result.errorCode, args: ['Job ID', jobid])]
            }else if (result.error) {
                deleteerrs<< result.error
            } else {
                successful<< result.success
            }
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
                return apiService.renderSuccessXml(request,response) {
                    delegate.'deleteJobs'(requestCount: ids.size(), allsuccessful:(successful.size()==ids.size())){
                        if(successful){
                            delegate.'succeeded'(count:successful.size()) {
                                successful.each{del->
                                    delegate.'deleteJobResult'(id:del.job.extid,){
                                        delegate.'message'(del.message)
                                    }
                                }
                            }
                        }
                        if(deleteerrs){
                            delegate.'failed'(count: deleteerrs.size()) {
                                deleteerrs.each{del->
                                    delegate.'deleteJobResult'(id:del.id,errorCode:del.errorCode){
                                        delegate.'error'(del.message)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            json{
                return apiService.renderSuccessJson(response) {
                    requestCount= ids.size()
                    allsuccessful=(successful.size()==ids.size())
                    if(successful){
                        delegate.'succeeded'=array {
                            successful.each{del->
                                delegate.'element'(id:del.job.extid,message:del.message)
                            }
                        }
                    }
                    if(deleteerrs){
                        delegate.'failed'=array {
                            deleteerrs.each{del->
                                delegate.'element'(id:del.id,errorCode:del.errorCode,message:del.message)
                            }
                        }
                    }
                }
            }
        }
    }

    def edit (){
        log.debug("ScheduledExecutionController: edit : params: " + params)
        def scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if(!scheduledExecution) {
            throw new NotFound('Job ID', params.id)
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_READ], scheduledExecution.project),
                AuthConstants.ACTION_UPDATE, 'Job', params.id)) {
            return
        }
        if (!params.project || params.project != scheduledExecution.project) {
            return redirect(controller: 'scheduledExecution', action: 'edit',
                    params: [id: params.id, project: scheduledExecution.project])
        }
        //clear session workflow
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        //clear session opts
        if(session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        def result = scheduledExecutionService.prepareCreateEditJob(params, scheduledExecution, AuthConstants.ACTION_UPDATE, authContext)

        ImportedJob<ScheduledExecution> importedJob = RundeckJobDefinitionManager.importedJob(scheduledExecution, [:])
        def validation=[:]
        boolean failed  = !scheduledExecutionService.validateJobDefinition(importedJob, authContext, params, validation, false)
        if (failed) {
            flash.message = g.message(code:'scheduledExecution.invalid.message', args: [scheduledExecution.jobName])
        }

        return result
    }



    def update (){
        withForm{
        def changeinfo=[method:'update',change:'modify',user:session.user]

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,params.id)
        def found = scheduledExecutionService.getByIDorUUID( params.id )
        if(!found) {
            throw new NotFound('Job ID', params.id)
        }

        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,found.project)
        def result = [:]
        try{
            result = scheduledExecutionService._doupdate(params, authContext, changeinfo)
        }catch(exception){
            flash.error = "Failed to update job: [${exception.message}]"
            log.warn("There was errors with the update process: ${exception.stackTrace}")
            return redirect(controller: 'scheduledExecution', action: 'edit',
                        params: [id: params.id, project: found.project])
        }
        def scheduledExecution=result.scheduledExecution
        def success = result.success
        if(!scheduledExecution){
            flash.message = "ScheduledExecution not found with id ${params.id}"
            log.warn("update: there was no object by id: " +params.id+". redirecting to edit.")
            redirect(controller:'menu',action:'jobs')
        }else if(result.unauthorized){
            return renderUnauthorized(result.message)
        }else if (!success){
            if(scheduledExecution.errors){
                log.debug scheduledExecution.errors.allErrors.collect {g.message(error: it)}.join(", ")
            }
            request.message="Error updating Job: "
            if(result.message){
                request.message+="{"+result.message+"} "
            }
            if(result.validation){
                request.message+= result.validation.toString() + " "
            }

//            if(!scheduledExecution.isAttached()) {
//                scheduledExecution.attach()
//            }else{
//                scheduledExecution.refresh()
//            }
            //update notification checkbox values

            def model = scheduledExecutionService.prepareCreateEditJob(params,scheduledExecution, AuthConstants.ACTION_UPDATE,  authContext)
            model["sessionOpts"] = params['_sessionEditOPTSObject']?.values()
            model["notificationValidation"] = params['notificationValidation']
            model["jobComponentValidation"] = params['jobComponentValidation']

            return render(
                    view: 'edit', model: model)
        }else{

            scheduledExecutionService.issueJobChangeEvent(result.jobChangeEvent)

            clearEditSession('_new')
            clearEditSession(scheduledExecution.id.toString())
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Saved changes to Job"

            if(result.remoteSchedulingChanged){
                flash.info = "INFO: Any scheduling changes will take effect after a few seconds."
            }
            scheduledExecutionService.logJobChange(changeinfo,scheduledExecution.properties)
            redirect(controller: 'scheduledExecution', action: 'show', params: [id: scheduledExecution.extid])
        }
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
    }

    def copy() {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )

        if (notFoundResponse(scheduledExecution, 'Job', params.id)) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        //authorize
        if(unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(authContext,
                AuthConstants.RESOURCE_TYPE_JOB, AuthConstants.ACTION_CREATE, params.project),
                AuthConstants.ACTION_CREATE,
                'New Job'
        )){
            return
        }
        log.debug("ScheduledExecutionController: create : params: " + params)

        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_READ], scheduledExecution.project), AuthConstants.ACTION_READ, 'Job', params.id)) {
            return
        }
        def importedJob = rundeckJobDefinitionManager.copy(scheduledExecution)
        def newScheduledExecution = importedJob.job
        if (newScheduledExecution.hasErrors()) {
            newScheduledExecution.errors.allErrors.each{log.warn("job copy data binding: "+it)}
        }
        newScheduledExecution.project=scheduledExecution.project
        newScheduledExecution.id=null
        newScheduledExecution.uuid=null
        //set session new workflow
        WorkflowController.getSessionWorkflow(session,null,new Workflow(scheduledExecution.workflow))
        if(scheduledExecution.options){
            def editopts = [:]

            scheduledExecution.options.each {Option opt ->
                editopts[opt.name] = opt.createClone()
            }
            EditOptsController.getSessionOptions(session,null,editopts)
        }
        def model = scheduledExecutionService.prepareCreateEditJob(params, newScheduledExecution , AuthConstants.ACTION_CREATE, authContext)
        def jobComponentValues=rundeckJobDefinitionManager.getJobDefinitionComponentValues(scheduledExecution)
        model["jobComponentValues"] = jobComponentValues
        model["iscopy"] = true

        render(
                view: 'create',
                model: model
        )

    }
    /**
     * action to populate the Create form with execution info from a previous (transient) execution
     */
    def createFromExecution(){

        log.debug("ScheduledExecutionController: create : params: " + params)
        Execution execution = Execution.get(params.executionId)

        if (notFoundResponse(execution, 'Execution', params.executionId)) {
            return
        }

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,execution.project)

        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectResource(
                authContext,
                AuthConstants.RESOURCE_TYPE_JOB,
                AuthConstants.ACTION_CREATE,
                params.project),
                AuthConstants.ACTION_CREATE,
                'New Job'
        )) {
            return
        }
        if (unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectExecutionAny(
                authContext, execution,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]
            ), AuthConstants.ACTION_VIEW, 'Execution',
            params.executionId)) {
            return
        }
        def props=[:]
        props.putAll(execution.properties)
        props.workflow=new Workflow(execution.workflow)
        if(params.failedNodes && 'true'==params.failedNodes){
            //replace the node filter with the failedNodeList from the execution
            props = props.findAll{!(it.key=~/^node(In|Ex)clude.*$/)}
            props.nodeIncludeName=execution.failedNodeList
        }
        params.putAll(props)
        //clear session workflow
        if(session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }
        if(session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        //store workflow in session
        def wf=WorkflowController.getSessionWorkflow(session,null,props.workflow)
        session.editWFPassThru=true

        def model=create()
        render(view:'create',model:model)
    }

    def create() {

        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        //authorize
        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_JOB,
            AuthConstants.ACTION_CREATE,
            params.project),
                AuthConstants.ACTION_CREATE, 'New Job')) {
            return
        }

        log.debug("ScheduledExecutionController: create : params: " + params)
        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.loglevel = servletContext.getAttribute("LOGLEVEL_DEFAULT")?servletContext.getAttribute("LOGLEVEL_DEFAULT"):"WARN"
        scheduledExecution.properties = params

        scheduledExecution.jobName = (params.command) ? params.command + " Job" : ""
        def cal = java.util.Calendar.getInstance()
        scheduledExecution.minute = String.valueOf(cal.get(java.util.Calendar.MINUTE))
        scheduledExecution.hour = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY))
        scheduledExecution.user = authContext.username
        scheduledExecution.userRoles = authContext.roles as List<String>
        if(params.project ){

            if(!frameworkService.existsFrameworkProject(params.project) ) {
                scheduledExecution.errors.rejectValue('project','scheduledExecution.project.message',[params.project].toArray(),'FrameworkProject was not found: {0}')
            }
            scheduledExecution.argString=params.argString
        }
        if(params.filterName){
            if (params.filterName) {
                def RdUser u = userService.findOrCreateUser(authContext.username)
                //load a named filter and create a query from it
                if (u) {
                    NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                    if (filter) {
                        def query2 = filter.createExtNodeFilters()
                        params.put('filter', query2.asFilter())
                    }
                }
            }
        }
        if (params.filter){
            scheduledExecution.filter=params.filter
            scheduledExecution.doNodedispatch=true
        }
        //clear session workflow
        if(session.editWFPassThru){
            //do not clear the session's editWF , as this action was called by createFromExecution
            session.removeAttribute('editWFPassThru')
        }else if (session.editWF ){
            session.removeAttribute('editWF');
            session.removeAttribute('undoWF');
            session.removeAttribute('redoWF');
        }//clear session workflow
        if(session.editOPTSPassThru){
            //do not clear the session's editWF , as this action was called by createFromExecution
            session.removeAttribute('editOPTSPassThru')
        }else if (session.editOPTS ){
            session.removeAttribute('editOPTS');
            session.removeAttribute('undoOPTS');
            session.removeAttribute('redoOPTS');
        }
        def model = scheduledExecutionService.prepareCreateEditJob(params, scheduledExecution, AuthConstants.ACTION_CREATE, authContext )
        return model
    }

    private clearEditSession(id='_new'){
        clearOPTSEditSession(id)
        clearWFEditSession(id)
    }
    private clearOPTSEditSession(id){
        session.editOPTS?.remove(id)
        session.undoOPTS?.remove(id)
        session.redoOPTS?.remove(id)
    }

    private clearWFEditSession(id) {
        session.editWF?.remove(id)
        session.undoWF?.remove(id)
        session.redoWF?.remove(id)
    }

    static void transferSessionEditState(session,params,id){
        //pass session-stored edit state in params map
        if ((params['_sessionwf'] in ['true',true]) && session.editWF && null != session.editWF[id]) {
            params['_sessionEditWFObject'] = session.editWF[id]
        }
        if ((params['_sessionopts'] in ['true',true]) && session.editOPTS && null != session.editOPTS[id]) {
            params['_sessionEditOPTSObject'] = session.editOPTS[id]
        }
    }

    /**
     * execute the job defined via input parameters, but do not store it.
     */
    def runAdhocInline(ApiRunAdhocRequest apiRunAdhocRequest){
        def results=[:]
        withForm{
            apiRunAdhocRequest.script=null
            apiRunAdhocRequest.url=null
            results=runAdhoc(apiRunAdhocRequest)
            if(results.failed){
                results.error=results.message
            } else {
                log.debug("ExecutionController: immediate execution scheduled (${results.id})")
            }
            g.refreshFormTokensHeader()
        }.invalidToken{
            results.error=g.message(code:'request.error.invalidtoken.message')
        }
        return render(contentType:'application/json'){
            if(results.error){
                'error' results.error
            }else{
                success 'true'
                id results.id
            }
        }
    }
    protected def runAdhoc(ApiRunAdhocRequest runAdhocRequest){
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,runAdhocRequest.project)
        params["user"] = authContext.username
        params.request = request
        params.jobName='Temporary_Job'
        params.groupPath='adhoc'

        if (runAdhocRequest.asUser) {
            //authorize RunAs User
            if (!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                    AuthConstants.ACTION_RUNAS, runAdhocRequest.project)) {

                def msg = g.message(code: "api.error.item.unauthorized", args: ['Run as User', 'Run', 'Adhoc'])
                return [failed:true,error: 'unauthorized', message: msg]
            }
            params['user'] = runAdhocRequest.asUser
        }
        if(runAdhocRequest.exec){
            params.workflow = new Workflow(commands: [new CommandExec(adhocRemoteString: runAdhocRequest.exec, adhocExecution: true)])
        }else if(runAdhocRequest.script){
            params.workflow = new Workflow(commands: [new CommandExec(adhocLocalString: runAdhocRequest.script,
                                                                      adhocExecution: true,
                                                                      argString: runAdhocRequest.argString,
                                                                      scriptInterpreter: runAdhocRequest.scriptInterpreter,
                                                                      interpreterArgsQuoted: runAdhocRequest.interpreterArgsQuoted,
                                                                      fileExtension:runAdhocRequest.fileExtension)])
        }else if(runAdhocRequest.url){
            params.workflow = new Workflow(commands: [new CommandExec(adhocFilepath: runAdhocRequest.url, adhocExecution: true,
                                                                      argString: runAdhocRequest.argString,
                                                                      scriptInterpreter: runAdhocRequest.scriptInterpreter,
                                                                      interpreterArgsQuoted: runAdhocRequest.interpreterArgsQuoted,
                                                                      fileExtension:runAdhocRequest.fileExtension)])
        }else{

            def msg = g.message(code: "api.error.parameter.required", args: ["url, script, or exec"])
            return [failed:true,error: 'invalid', message: msg]
        }
        params.project=runAdhocRequest.project
        params.nodeKeepgoing= runAdhocRequest.nodeKeepgoing!=null?runAdhocRequest.nodeKeepgoing:true
        params.nodeThreadcount= runAdhocRequest.nodeThreadcount?:1
        params.description = runAdhocRequest.description ?: ""
        params.excludeFilterUncheck = false
        if (params.filterName) {
            def RdUser u = userService.findOrCreateUser(authContext.username)
            //load a named filter and create a query from it
            if (u) {
                NodeFilter filter = NodeFilter.findByNameAndUser(params.filterName, u)
                if (filter) {
                    def query2 = filter.createExtNodeFilters()
                    params.put('filter',query2.asFilter())
                }
            }
        }

        params.extraMetadataMap = runAdhocRequest.meta ?: [:]

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,'_new')
        def result= scheduledExecutionService._dovalidateAdhoc(params,authContext)
        ScheduledExecution scheduledExecution=result.scheduledExecution

        // The default log level for ScheduledExecution is WARN which prevents log writing into the rdlog file later.
        // For adhoc command we have to designate the log level to NORMAL.
        scheduledExecution.loglevel = LogLevel.NORMAL

        def failed=result.failed
        if(!failed){
            return _transientExecute(scheduledExecution,params,authContext)
        }else{
            return [success:false,failed:true,invalid:true,message:'Job configuration was incorrect.',scheduledExecution:scheduledExecution,params:params]
        }
    }

    /**
    * Execute a transient ScheduledExecution and return execution data: [execution:Execution,id:Long]
     * if there is an error, return [error:'type',message:errormesg,...]
     */
    private Map _transientExecute(ScheduledExecution scheduledExecution, Map params, AuthContext authContext){
        def object
        def isauth = scheduledExecutionService.userAuthorizedForAdhoc(params.request,scheduledExecution,authContext)
        if (!isauth){
            def msg=g.message(code:'unauthorized.job.run.user',args:[params.user])
            return [success:false,error:'unauthorized',message:msg]
        }


        if(!executionService.executionsAreActive){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if(!scheduledExecutionService.isProjectExecutionEnabled(scheduledExecution.project)){
            def msg=g.message(code:'project.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            def msg=g.message(code:'scheduleExecution.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        params.workflow=new Workflow(scheduledExecution.workflow)
        params.argString=scheduledExecution.argString
        params.doNodedispatch=scheduledExecution.doNodedispatch
        params.filter=scheduledExecution.asFilter()

        def Execution e
        try {
            e = executionService.createExecutionAndPrep(scheduledExecution, authContext, params)
        } catch (ExecutionServiceException exc) {
            return [success:false,error:'failed',message:exc.getMessage()]
        }

        return scheduledExecutionService.scheduleTempJob(authContext, e);
    }



    def save () {
        withForm{
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        def changeinfo=[user:session.user,change:'create',method:'save']

        //pass session-stored edit state in params map
        transferSessionEditState(session, params,'_new')

        def result = scheduledExecutionService._docreateJobOrParams(null, params, authContext, changeinfo)
        scheduledExecutionService.issueJobChangeEvent(result.jobChangeEvent)
        def scheduledExecution = result.scheduledExecution
        if(result.success && scheduledExecution.id){
            clearEditSession()
            flash.savedJob=scheduledExecution
            flash.savedJobMessage="Created new Job"
            scheduledExecutionService.logJobChange(changeinfo,scheduledExecution.properties)
            return redirect(controller:'scheduledExecution',action:'show',params:[id:scheduledExecution.extid])
        }else{
            if(scheduledExecution){
                scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
                if(!scheduledExecution.jobName){
                    scheduledExecution.jobName=''
                }
            }
            if (result.unauthorized){
                request.message = result.error
            }else{
                request.message=g.message(code:'ScheduledExecutionController.save.failed')
            }
        }
        def model = scheduledExecutionService.prepareCreateEditJob(params, scheduledExecution, AuthConstants.ACTION_CREATE, authContext)
        model["notificationValidation"]=params['notificationValidation']
        model["jobComponentValidation"]=params['jobComponentValidation']

        render(view: 'create', model: model)
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
    }


    def upload(){

    }

    def uploadPost() {
        log.debug("ScheduledExecutionController: upload " + params)
        withForm {
            UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.
                    getAuthContextForSubjectAndProject(session.subject, params.project)

            def fileformat = params.fileformat ?: 'xml'
            def parseresult
            if (params.xmlBatch && params.xmlBatch instanceof String) {
                String fileContent = params.xmlBatch
                parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
            } else if (params.xmlBatch && params.xmlBatch instanceof CommonsMultipartFile) {
                InputStream fileContent = params.xmlBatch.inputStream
                parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
            } else if (request instanceof MultipartHttpServletRequest) {
                def file = request.getFile("xmlBatch")
                if (!file || file.empty) {
                    request.message = "No file was uploaded."
                    return render(view: 'upload')
                }
                parseresult = scheduledExecutionService.parseUploadedFile(file.getInputStream(), fileformat)
            } else {
                request.message = "No file was uploaded."
                return render(view: 'upload')
            }
            def jobset
            if (parseresult.errorCode) {
                parseresult.error = message(code: parseresult.errorCode, args: parseresult.args)
            }
            if (parseresult.error) {
                request.error = parseresult.error
                return render(view: 'upload')
            }
            jobset = parseresult.jobset
            jobset.each { it.job.project = params.project }
            def changeinfo = [user: session.user, method: 'upload']

            def loadresults = scheduledExecutionService.loadImportedJobs(
                    jobset,
                    params.dupeOption,
                    params.uuidOption,
                    changeinfo,
                    authContext,
                    (params?.validateJobref == 'true')
            )
            scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)


            def jobs = loadresults.jobs
            def msgs = loadresults.msgs
            def errjobs = loadresults.errjobs
            def skipjobs = loadresults.skipjobs
            return render(
                    view: 'upload',
                    model: [
                            jobs          : jobs,
                            errjobs       : errjobs,
                            skipjobs      : skipjobs,
                            nextExecutions: scheduledExecutionService.nextExecutionTimes(jobs.grep { scheduledExecutionService.isScheduled(it) }),
                            messages      : msgs,
                            didupload     : true
                    ]
            )
        }.invalidToken {
            request.warn = g.message(code: 'request.error.invalidtoken.message')
            render(view: 'upload', params: [project: params.project])
        }
    }

    def execute = {
        return redirect(action: 'show',params:params)
    }

    private _prepareExecute(ScheduledExecution scheduledExecution, final def framework, final AuthContext authContext){
        def model=[scheduledExecution:scheduledExecution]
        model.authorized=true
        //test nodeset to make sure there are matches
        if(scheduledExecution.doNodedispatch){
            NodeSet nset = ExecutionService.filtersAsNodeSet(scheduledExecution)
            NodeSet unselectedNset = ExecutionService.filtersExcludeAsNodeSet(scheduledExecution)
            model.nodefilter=scheduledExecution.asFilter()
            //check nodeset filters for variable expansion
            def varfound = scheduledExecution.asFilter().contains("\${")
            if (varfound) {
                model.nodesetvariables = true
            }
            def failedNodes = null
            if (params.retryFailedExecId) {
                Execution e = Execution.get(params.retryFailedExecId)
                if (e && e.scheduledExecution?.id == scheduledExecution.id) {
                    model.failedNodes = e.failedNodeList
                    // If there is no "failed nodes" from previous execution, we use the original node set to prevent NPE
                    if( e.failedNodeList ){
                        if(varfound){
                            nset = ExecutionService.filtersAsNodeSet([filter: OptsUtil.join("name:", e.failedNodeList)])
                        }
                        model.nodesetvariables = false
                        def failedSet = ExecutionService.filtersAsNodeSet([filter: OptsUtil.join("name:", e.failedNodeList)])
                        failedNodes = rundeckAuthContextProcessor.filterAuthorizedNodes(
                                scheduledExecution.project,
                                new HashSet<String>(["read", "run"]),
                                frameworkService.filterNodeSet(failedSet, scheduledExecution.project),
                                authContext).nodes;
                    }else{
                        def originalSet = ExecutionService.filtersAsNodeSet([filter: e.filter])
                        failedNodes = rundeckAuthContextProcessor.filterAuthorizedNodes(
                                scheduledExecution.project,
                                new HashSet<String>(["read", "run"]),
                                frameworkService.filterNodeSet(originalSet, scheduledExecution.project),
                                authContext).nodes;
                    }
                }
            }
            def nodes = rundeckAuthContextProcessor.filterAuthorizedNodes(
                    scheduledExecution.project,
                    new HashSet<String>(["read", "run"]),
                    frameworkService.filterNodeSet(nset, scheduledExecution.project),
                    authContext).nodes;

            def unselectedNodes

            if(unselectedNset && !(unselectedNset.include?.blank && unselectedNset.exclude?.blank)){
                def unselectedNodesFilter = rundeckAuthContextProcessor.filterAuthorizedNodes(
                                                    scheduledExecution.project,
                                                    new HashSet<String>(["read", "run"]),
                                                    frameworkService.filterNodeSet(unselectedNset, scheduledExecution.project),
                                                    authContext)

                if(unselectedNodesFilter){
                    unselectedNodes = unselectedNodesFilter.nodes
                }
            }

            if(failedNodes && failedNodes.size()>0){
                //if failed nodes are not part of original node filter, it will be added
                def failedNodeNotInNodes = failedNodes.findAll{ !nodes.contains( it ) }
                if(failedNodeNotInNodes && failedNodeNotInNodes.size()>0){
                    def nodeImp = new NodeSetImpl()
                    if(nodes){
                        nodeImp.putNodes(nodes)
                    }
                    nodeImp.putNodes(failedNodeNotInNodes)
                    nodes=nodeImp.getNodes()
                }
            }

            if(!nodes || nodes.size()<1){
                //error
                model.nodesetempty=true
            }
            else if(grailsApplication.config.getProperty("gui.execution.summarizedNodes", String.class) != 'false') {
                model.nodes=nodes
                model.nodemap=[:]
                model.tagsummary=[:]
                model.grouptags=[:]
                model.nodesSelectedByDefault=scheduledExecution.hasNodesSelectedByDefault()
                if (!model.nodesSelectedByDefault) {
                    model.selectedNodes = []
                }
                //summarize node groups
                def namegroups=[other: new TreeList()]
                nodes.each{INodeEntry node->
                    def name=node.nodename
                    model.nodemap[name]=node
                    def matcher = (name=~ /^(.*\D)(\d+)/)
                    def matcher2 = (name =~ /^(\d+)(\D.*)/)
                    def groupname
                    if(matcher.matches()){
                        def pat=matcher.group(1)
                        def num = matcher.group(2)
                        groupname= pat + '.*'

                    }else if(matcher2.matches()){
                        def pat = matcher2.group(2)
                        def num = matcher2.group(1)
                        groupname= '.*' + pat

                    }else{
                        groupname='other'
                    }
                    if (!namegroups[groupname]) {
                        namegroups[groupname] = new TreeList()
                    }
                    namegroups[groupname] << name
                    //summarize tags
                    def tags = node.getTags()
                    if (tags) {
                        tags.each { tag ->
                            if (!model.tagsummary[tag]) {
                                model.tagsummary[tag] = 1
                            } else {
                                model.tagsummary[tag]++
                            }
                            if(!model.grouptags[groupname]){
                                model.grouptags[groupname]=[(tag):1]
                            }else if (!model.grouptags[groupname][tag]) {
                                model.grouptags[groupname][tag]=1
                            }else{
                                model.grouptags[groupname][tag]++
                            }
                        }
                    }
                }
                def singles=[]
                namegroups.keySet().grep {it!='other'&&namegroups[it].size() == 1}.each{
                    namegroups['other'].addAll(namegroups[it])
                    model.grouptags[it]?.each{tag,v->
                        if (!model.grouptags['other']) {
                            model.grouptags['other'] = [(tag): v]
                        } else if (!model.grouptags['other'][tag]) {
                            model.grouptags['other'][tag] = v
                        } else {
                            model.grouptags['other'][tag] += v
                        }
                    }
                    singles<<it
                }
                singles.each{
                    namegroups.remove(it)
                    model.grouptags.remove(it)
                }
                if(!namegroups['other']){
                    namegroups.remove('other')
                }

                model.namegroups=namegroups
            }else{
                model.nodes = nodes
            }
            if(unselectedNodes){
                model.unselectedNodes = unselectedNodes
            }

        }

        if(params.retryExecId){
            Execution e = Execution.get(params.retryExecId)
            if(e && e.scheduledExecution?.id == scheduledExecution.id){
                model.selectedoptsmap=FrameworkService.parseOptsFromString(e.argString)
                if (e.filter != scheduledExecution.filter) {

                    def retryNodes = rundeckAuthContextProcessor.filterAuthorizedNodes(
                            scheduledExecution.project,
                            new HashSet<String>([AuthConstants.ACTION_READ, AuthConstants.ACTION_RUN]),
                            frameworkService.filterNodeSet(
                                    ExecutionService.filtersAsNodeSet([filter:e.filter]),
                                    scheduledExecution.project
                            ),
                            authContext).nodes;

                    model.selectedNodes = retryNodes*.nodename
                }
            }
        }else if(params.argString){
            model.selectedoptsmap = FrameworkService.parseOptsFromString(params.argString)
        }
        if(model.unselectedNodes && !params.retryExecId){
            def selectedNodes = model.nodes.findAll{ ! model.unselectedNodes.contains(it)  }
            model.selectedNodes = selectedNodes*.nodename
            model.unselectedNodes = model.unselectedNodes*.nodename
        }
        model.localNodeName=framework.getFrameworkNodeName()

        //determine option dependencies based on valuesURl embedded references
        //map of option name to list of option names which depend on it
        def depopts=[:]
        //map of option name to list of option names it depends on
        def optdeps=[:]
        boolean explicitOrdering=false
        def optionSelections=[:]
        def optionValuesPluginErrors=[:]
        scheduledExecution.options.each { Option opt->
            optionSelections[opt.name]=opt
            if(opt.sortIndex!=null){
                explicitOrdering=true
            }
            if(opt.realValuesUrl){
                (opt.realValuesUrl=~/\$\{option\.([^.}\s]+?)\.value\}/ ).each{match,oname->
                    if(oname==opt.name){
                        return
                    }
                    //add opt to list of dependents of oname
                    if(!depopts[oname]){
                        depopts[oname]=[opt.name]
                    }else{
                        depopts[oname] << opt.name
                    }
                    //add oname to list of dependencies of opt
                    if(!optdeps[opt.name]){
                        optdeps[opt.name]=[oname]
                    }else{
                        optdeps[opt.name] << oname
                    }
                }
            }
            if(opt.optionValuesPluginType) {
                try{
                    opt.valuesFromPlugin = optionValuesService.getOptions(scheduledExecution.project,opt.optionValuesPluginType, authContext)
                }catch(Exception e){
                    optionValuesPluginErrors.put(opt.name, "Error loading option plugin: ${e.message}")
                    log.warn("option value plugin failed: ${e.message}")

                }
            }
        }
        if(optionValuesPluginErrors){
            model.jobexecOptionErrors = optionValuesPluginErrors
        }
        model.dependentoptions=depopts
        model.optiondependencies=optdeps

        //Option sort order will use sortIndex if set
        model.optionordering = scheduledExecution.options*.name

        //topo sort the dependencies
        def toporesult = Toposort.toposort(model.optionordering, depopts, optdeps)
        if (scheduledExecution.options && !toporesult.result) {
            log.warn("Cyclic dependency for options for job ${scheduledExecution.extid}: (${toporesult.cycle})")
            model.optionsDependenciesCyclic = true
        }
        if (toporesult.result && !scheduledExecution.options.any { it.sortIndex != null }) {
            //auto sort only if no ordering is defined
            model.optionordering = toporesult.result
        }


        //prepare dataset used by option view
        //includes dependency information, auto reload and for remote options, selected values
        def remoteOptionData = [:]
        (model.optionordering).each{optName->
            Option opt = optionSelections[optName]
            def optData = [
                    'optionDependencies': model.optiondependencies[optName],
                    'optionDeps': model.dependentoptions[optName],
            ];
            if (opt.realValuesUrl != null) {
                optData << [
                        'hasUrl': true,
                        'scheduledExecutionId': scheduledExecution.extid,
                        'selectedOptsMap': model.selectedoptsmap ? model.selectedoptsmap[optName] : '',
                ]
            } else {
                optData['localOption'] = true;
            }
            remoteOptionData[optName] = optData
        }
        model.remoteOptionData=remoteOptionData

        return model
    }
    public def executeFragment(RunJobCommand runParams, ExtraCommand extra) {
        if ([runParams, extra].any { it.hasErrors() }) {
            request.errors = [runParams, extra].find { it.hasErrors() }.errors
        }
        Framework framework = frameworkService.getRundeckFramework()
        def scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if(unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution,
                [AuthConstants.ACTION_RUN], scheduledExecution.project), AuthConstants.ACTION_RUN,
                'Job',params.id,true
        )){
            return
        }
        def model = _prepareExecute(scheduledExecution, framework,authContext)
        model.nextExecution = scheduledExecutionService.nextExecutionTime(scheduledExecution)
        if(params.dovalidate){
            model.jobexecOptionErrors=session.jobexecOptionErrors
            model.selectedoptsmap=session.selectedoptsmap
            session.jobexecOptionErrors=null
            session.selectedoptsmap=null
            model.options=null
        }
        render(template:'execOptionsForm',model:model)
    }

    /**
     * Execute job specified by parameters, and return json results
     */
    public def runJobInline(RunJobCommand runParams, ExtraCommand extra) {
        def results=[:]
        if ([runParams, extra].any { it.hasErrors() }) {
            request.errors = [runParams, extra].find { it.hasErrors() }.errors
            return render(contentType: 'application/json') {
                delegate.error 'invalid'
                delegate.message  "Invalid parameters: " + request.errors.allErrors.collect { g.message(error: it) }.join(", ")
            }
        }
        withForm{

            results = scheduleJob(null)

            if(results.error=='invalid'){
                session.jobexecOptionErrors=results.errors
                session.selectedoptsmap=results.options
            }
        }.invalidToken{
            results.failed=true
            results.error='request.error.invalidtoken.message'
            results.message=g.message(code:'request.error.invalidtoken.message')
        }

        def link
        if(params.followdetail){
            if(params.followdetail=='html'){
                link =createLink(controller: "execution", action: "renderOutput", id: results.id,
                        params:[convertContent:'on', loglevels:'on', ansicolor:'on', project:params.project, reload:'true'])
            }else{
                link =createLink(controller: "execution",action: "show",id: results.id, fragment: params.followdetail)
            }
        } else {
            link = createLink(controller: "execution", action: "show", id: results.id)
        }
        boolean follow_ = params.follow == 'true'
        return render(contentType:'application/json'){
            if (results.failed) {
                error results.error
                message results.message
            } else {
                success true
                id results.id
                href link
                follow follow_
            }
        }
    }
    /**
     * Schedule job specified by parameters, and return json results
     */
    public def scheduleJobInline(RunJobCommand runParams, ExtraCommand extra) {
        def results = [:]
        if ([runParams, extra].any { it.hasErrors() }) {
            request.errors = [runParams, extra].find { it.hasErrors() }.errors
            return render(contentType: 'application/json') {
                delegate.error 'invalid'
                delegate.message  "Invalid parameters: " + request.errors.allErrors.collect { g.message(error: it) }.join(", ")
            }
        }
        withForm {
            results = scheduleJob(params.runAtTime)

            if (results.error == 'invalid') {
                session.jobexecOptionErrors = results.errors
                session.selectedoptsmap = results.options
            }
        }.invalidToken {
            results.failed = true
            results.error = 'request.error.invalidtoken.message'
            results.message = g.message(code: 'request.error.invalidtoken.message')
        }
        def link
        if(params.followdetail){
            if(params.followdetail=='html'){
                link =createLink(controller: "execution", action: "renderOutput", id: results.id, params:[convertContent:'on', loglevels:'on', ansicolor:'on', project:params.project, reload:'true'])
            }else{
                link =createLink(controller: "execution",action: "show",id: results.id, fragment: params.followdetail)
            }
        }else {
            link = createLink(controller: "execution", action: "show", id: results.id)
        }
        boolean follow_ = params.follow == 'true'

        return render(contentType: 'application/json') {
            if (results.failed) {
                error results.error
                message results.message
            } else {
                success true
                id results.id
                href link
                follow follow_
            }
        }
    }

    public def runJobNow(RunJobCommand runParams, ExtraCommand extra) {
        return runOrScheduleJob(runParams, extra, true)
    }

    public def runOrScheduleJob(RunJobCommand runParams, ExtraCommand extra, boolean runNow) {
        if ([runParams, extra].any{it.hasErrors()}) {
            request.errors= [runParams, extra].find { it.hasErrors() }.errors
            def model = show()
            return render(view: 'show', model: model)
        }
        def results=[:]
        withForm{
            results = scheduleJob(runNow ? null : params.runAtTime)
        }.invalidToken{
            results.error = "Invalid request token"
            results.code = HttpServletResponse.SC_BAD_REQUEST
            request.errorCode = 'request.error.invalidtoken.message'
        }
        if (results.failed) {
            log.error(results.message)
            if (results.error == 'unauthorized'){
                return render(view: "/common/execUnauthorized", model: results)
            }else {
                def model = show()
                results.error = results.remove('message')
                results.jobexecOptionErrors = results.errors
                results.selectedoptsmap = results.options
                results.putAll(model)
                results.options = null
                return render(view: 'show', model: results)
            }
        } else if (results.error){
            log.error(results.error)
            if (results.code) {
                response.setStatus (results.code)
            }
            return renderErrorView(results)
        } else if (params.follow == 'true') {
            if(params.followdetail=='html'){
                redirect(controller: "execution", action: "renderOutput", id: results.id, params:[convertContent:'on', loglevels:'on', ansicolor:'on', project:params.project, reload:'true'])
            }else{
                redirect(controller: "execution", action: "show", id: results.id, fragment: params.followdetail)
            }
        } else {
            redirect(controller: "scheduledExecution", action: "show", id: params.id)
        }
    }


    /**
     * Run a job at a later time.
     *
     * @param   runParams
     * @param   extra
     * @return  success or failure result in JSON
     */
    public def runJobLater(RunJobCommand runParams, ExtraCommand extra) {
        // Prepare and schedule
        return runOrScheduleJob(runParams, extra, false)
    }

    /**
     * Schedule a job for a later time
     *
     * @params runAtTime if scheduling in the future, the time to run the job, otherwise it will be run immediately
     * @return  the result in JSON
     */
    private Map scheduleJob(String runAtTime = null) {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!scheduledExecution) {
            return [error: "No Job found for id: " + params.id, code: 404]
        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
                                                     scheduledExecution.project)) {
            return [success: false, failed: true, error: 'unauthorized', message: "Unauthorized: Execute Job ${scheduledExecution.extid}"]
        }

        if(!executionService.executionsAreActive){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message: msg]
        }

        if(!scheduledExecutionService.isProjectExecutionEnabled(scheduledExecution.project)){
            def msg=g.message(code:'project.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            def msg = g.message(code: 'scheduleExecution.execution.disabled')
            return [success: false, failed: true, error: 'disabled', message: msg]
        }

        if (params.extra?.debug == 'true') {
            params.extra.loglevel='DEBUG'
        }
        Map inputOpts=[:]
        //add any option.* values, or nodeInclude/nodeExclude filters
        if (params.extra) {
            inputOpts.putAll(params.extra.subMap(['nodeIncludeName', 'loglevel',/*'argString',*/ 'optparams', 'option', '_replaceNodeFilters',
                                                  'filter', 'nodeoverride', 'nodefilter']).findAll { it.value })
            inputOpts.putAll(params.extra.findAll{it.key.startsWith('option.') || it.key.startsWith('nodeInclude') ||
                    it.key.startsWith('nodeExclude')}.findAll { it.value != null })
        }

        if (params.meta instanceof Map) {
            inputOpts['meta'] = new HashMap(params.meta)
        }

        Date expirationStart = runAtTime != null ? executionService.parseRunAtTime(runAtTime) : null

        //handle uploaded files
        def fcopy = handleUploadFiles(scheduledExecution, authContext, inputOpts, expirationStart)
        if (!fcopy.success) {
            return fcopy
        }

        if (runAtTime) {
            inputOpts['runAtTime'] = runAtTime

            def scheduleResult = executionService.scheduleAdHocJob(
                    scheduledExecution,
                    authContext,
                    session.user,
                    inputOpts
            )
            if (null == scheduleResult) {
                return [success: false, failed: true, error: 'error', message: "Unable to schedule job"]
            }

            log.debug("ScheduledExecutionController: deferred execution scheduled for ${scheduleResult.nextRun}")

            if (scheduleResult.error) {
                scheduleResult.failed = true
            }
            return scheduleResult
        } else {
            inputOpts['executionType'] = 'user'

            def result = executionService.executeJob(scheduledExecution, authContext, session.user, inputOpts)

            if (result.error) {
                result.failed = true
                return result
            } else {
                log.debug("ExecutionController: immediate execution scheduled")
                return [success: true, message: "immediate execution scheduled", id: result.executionId]
            }
        }

    }


    private def handleUploadFiles(
            ScheduledExecution scheduledExecution,
            UserAndRolesAuthContext authContext,
            Map inputOpts,
            Date expirationStart = null
    )
    {
        def optionParameterPrefix = "extra.option."
        def fileresults = [:]
        if (request instanceof MultipartRequest) {
            def fileOptions = scheduledExecution.listFileOptions()
            def fileOptionConfig = [:]
            fileOptions.each { Option option ->
                fileOptionConfig[option.name] = option.configMap
            }
            def fileOptionNames = fileOptionConfig.keySet()
            def invalid = []
            long maxsize = fileUploadService.optionUploadMaxSize
            if (maxsize > 0) {
                def find = ((MultipartRequest) request).fileMap.find { String name, file -> file.size > maxsize }
                if (find) {
                    def msg = g.message(
                            code: 'api.error.job-upload.filesize',
                            args: [find.value.size, find.key.substring(optionParameterPrefix.length()), maxsize]
                    )
                    return [success: false, failed: true, error: 'filesize', message: msg]
                }
            }
            ((MultipartRequest) request).fileMap.each { String name, file ->
                if (name.startsWith(optionParameterPrefix)) {
                    //process file option upload
                    String optname = name.substring(optionParameterPrefix.length())
                    if (!(optname in fileOptionNames)) {
                        invalid << optname
                    }
                } else {
                    invalid << name
                }
            }
            if (invalid) {
                def msg = g.message(code: 'api.error.job-upload.option.unexpected', args: [invalid.join(',')])
                return [success: false, failed: true, error: 'input', message: msg]
            }

            for (def entry : ((MultipartRequest) request).fileMap) {
                String name = entry.key
                MultipartFile file = entry.value
                if (name.startsWith(optionParameterPrefix)) {
                    //process file option upload
                    String optname = name.substring(optionParameterPrefix.length())
                    if (optname in fileOptionNames && (!file.empty || file.originalFilename)) {
                        try {
                            String ref = fileUploadService.receiveFile(
                                    file.inputStream,
                                    file.size,
                                    authContext.username,
                                    file.originalFilename,
                                    optname,
                                    fileOptionConfig[optname],
                                    scheduledExecution.extid,
                                    scheduledExecution.project,
                                    expirationStart ?: new Date()
                            )
                            fileresults[optname] = ref
                        } catch (FileUploadServiceException e) {
                            def msg = g.message(code: 'api.error.job-upload.error', args: [e.message])
                            return [success: false, failed: true, error: 'input', message: msg]
                        }
                    }
                }
            }
            fileOptionNames.each { optname ->
                if(fileresults[optname]) {
                    inputOpts["option.$optname".toString()] = fileresults[optname]
                    inputOpts.get('option')?.put(optname, fileresults[optname])
                }else{
                    inputOpts.remove("option.$optname".toString())
                    inputOpts.get('option')?.remove(optname)
                }
            }
        }
        [success: true, files: fileresults]
    }

    /**
    * API Actions
     */


    /**
     * Utility, render content for jobs/import response
     */
    def renderJobsImportApiXML={jobs,jobsi,errjobs,skipjobs, delegate->
        delegate.'succeeded'(count:jobs.size()){
            jobsi.each { Map job ->
                delegate.'job'(index: job.entrynum,href: apiService.apiHrefForJob(job.scheduledExecution)) {
                    id(job.scheduledExecution.extid)
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath ?: '')
                    project(job.scheduledExecution.project)
                    permalink(apiService.guiHrefForJob(job.scheduledExecution))
                }
            }
        }
        delegate.failed(count:errjobs.size()){
            errjobs.each{ Map job ->
                def jmap=[index:job.entrynum]
                if(job.scheduledExecution.id){
                    jmap.href=apiService.apiHrefForJob(job.scheduledExecution)
                }
                delegate.'job'(jmap){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        permalink(apiService.guiHrefForJob(job.scheduledExecution))
                    }
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath?:'')
                    project(job.scheduledExecution.project)
                    StringBuffer sb = new StringBuffer()
                    job.scheduledExecution?.errors?.allErrors?.each{err->
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb << g.message(error:err)
                    }
                    if(job.errmsg){
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb<<job.errmsg
                    }
                    delegate.'error'(sb.toString())
                }
            }
        }
        delegate.skipped(count:skipjobs.size()){

            skipjobs.each{ Map job ->
                def jmap = [index: job.entrynum]
                if (job.scheduledExecution.id) {
                    jmap.href = apiService.apiHrefForJob(job.scheduledExecution)
                }
                delegate.'job'(jmap){
                    if(job.scheduledExecution.id){
                        id(job.scheduledExecution.extid)
                        permalink(apiService.guiHrefForJob(job.scheduledExecution))
                    }
                    name(job.scheduledExecution.jobName)
                    group(job.scheduledExecution.groupPath?:'')
                    project(job.scheduledExecution.project)
                    StringBuffer sb = new StringBuffer()
                    if(job.errmsg){
                        if(sb.size()>0){
                            sb<<"\n"
                        }
                        sb<<job.errmsg
                    }
                    delegate.'error'(sb.toString())
                }
            }
        }
    }/**
     * Utility, render content for jobs/import response
     */
    private def renderJobsImportApiJson(jobs,jobsi,errjobs,skipjobs, delegate){
        delegate.'succeeded'=delegate.array{
            jobsi.each { Map job ->
                delegate.element(
                        index: job.entrynum,
                        href: apiService.apiHrefForJob(job.scheduledExecution),
                        id:job.scheduledExecution.extid,
                        name:job.scheduledExecution.jobName,
                        group:job.scheduledExecution.groupPath ?: '',
                        project:job.scheduledExecution.project,
                        permalink:apiService.guiHrefForJob(job.scheduledExecution)
                )
            }
        }
        delegate.failed=delegate.array{
            errjobs.each{ Map job ->
                def jmap=[index:job.entrynum]
                if(job.scheduledExecution.id){
                    jmap.href=apiService.apiHrefForJob(job.scheduledExecution)
                    jmap.id=job.scheduledExecution.extid
                    jmap.permalink=apiService.guiHrefForJob(job.scheduledExecution)
                }
                StringBuffer sb = new StringBuffer()
                job.scheduledExecution?.errors?.allErrors?.each{err->
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb << g.message(error:err)
                }
                if(job.errmsg){
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb<<job.errmsg
                }
                jmap.'error'=(sb.toString())
                delegate.element(jmap + [name:(job.scheduledExecution.jobName),
                                           group:(job.scheduledExecution.groupPath?:''),
                                           project:(job.scheduledExecution.project)])
            }
        }
        delegate.skipped=delegate.array{

            skipjobs.each{ Map job ->
                def jmap = [index: job.entrynum]
                if (job.scheduledExecution.id) {
                    jmap.href = apiService.apiHrefForJob(job.scheduledExecution)
                    jmap.id=(job.scheduledExecution.extid)
                    jmap.permalink=apiService.guiHrefForJob(job.scheduledExecution)
                }
                StringBuffer sb = new StringBuffer()
                if(job.errmsg){
                    if(sb.size()>0){
                        sb<<"\n"
                    }
                    sb<<job.errmsg
                }
                jmap.'error'=(sb.toString())
                jmap.name=(job.scheduledExecution.jobName)
                jmap.group=(job.scheduledExecution.groupPath?:'')
                jmap.project=(job.scheduledExecution.project)
                delegate.element(jmap)
            }
        }
    }

    @Post(uri = "/project/{project}/jobs/import", produces = [MediaType.APPLICATION_JSON])
    @Operation(
        method = "POST",
        summary = "Import Job definitions",
        description = '''Import a set of job definitions in a supported format.


Since: v14''',
        tags=['jobs'],
        parameters = [
            @Parameter(
                name = 'project',
                description = 'Project Name',
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'fileformat',
                description = 'Input file format, to specify the input format, if multipart of form input is sent.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string',allowableValues = ['json','yaml'])
            ),
            @Parameter(
                name = 'dupeOption',
                description = 'A value to indicate the behavior when importing jobs which already exist.  Value can be "skip", "create", or "update". Default is "create".',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string',allowableValues = ['skip','create','update'])
            ),
            @Parameter(
                name = 'uuidOption',
                description = '''Whether to preserve or remove UUIDs from the imported jobs:

*  `preserve`: Preserve the UUIDs in imported jobs.  This may cause the import to fail if the UUID is already used. (Default value).
*  `remove`: Remove the UUIDs from imported jobs. Allows update/create to succeed without conflict on UUID.
''',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string',allowableValues = ['preserve','remove'])
            )
        ],
        requestBody = @RequestBody(
            description='''Input Request supports multiple types:

* `Content-Type: x-www-form-urlencoded`, with a `xmlBatch` request parameter containing the input content
* `Content-Type: multipart/form-data` multipart MIME request part named `xmlBatch` containing the content.
* `Content-Type: application/yaml`, request body is the Jobs YAML formatted job definition
* `Content-Type: application/json`, request body is the Jobs JSON formatted job definition (API v44+)
''',
            content=[
                @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED
                ),
                @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA
                ),
                @Content(
                    schema = @Schema(
                        type = 'object',
                        externalDocs = @ExternalDocumentation(
                            url = 'https://docs.rundeck.com/docs/manual/document-format-reference/job-json-v44.html',
                            description = "Job JSON Format"
                        )
                    ),
                    mediaType = MediaType.APPLICATION_JSON
                ),
                @Content(
                    schema = @Schema(
                        type = 'string',
                        externalDocs = @ExternalDocumentation(
                            url = 'https://docs.rundeck.com/docs/manual/document-format-reference/job-yaml-v12.html',
                            description = "Job YAML Format"
                        )
                    ),
                    mediaType = 'text/yaml'
                )
            ]
        ),
        responses = [
            @ApiResponse(
                responseCode = "200",
                description = '''Job definition import result.

A set of status results.  Each imported job definition will be either "succeeded", "failed" or "skipped".
Within each one there will be 0 or more objects representing the imported job.

Each job entry contains:

* `index`: index in the input content of the job definition.
* `id`: If the job exists, or was successfully created, its UUID
* `href`: If the job exists, or was successfully created, its API href
* `permalink`: If the job exists, or was successfully created, its GUI URL.
* `project`: The project
* `name`: The job name
* `group`: The job Group
* `error`: (if failed) the error message

''',
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    examples = @ExampleObject('''{
  "succeeded": [{
  "index": 1,
  "href": "http://madmartigan.local:4440/api/14/job/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "id": "3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "name": "restart",
  "group": "app2/dev",
  "project": "test",
  "permalink": "http://madmartigan.local:4440/job/show/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a"
}],
  "failed": [{
  "index": 2,
  "href": "http://madmartigan.local:4440/api/14/job/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "id": "3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "name": "restart",
  "group": "app2/dev",
  "project": "test",
  "permalink": "http://madmartigan.local:4440/job/show/3b6c19f6-41ee-475f-8fd0-8f1a26f27a9a",
  "error": "error message"
}],
  "skipped": []
}''')
                )
            ),
            @ApiResponse(
                responseCode = "415",
                description = "Unsupported media type",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema=@Schema(implementation = ApiErrorResponse)
                )
            )
        ]
    )
    @Tag(name = "jobs")
    /**
     * API: /api/14/project/NAME/jobs/import
     */
    def apiJobsImportv14(){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
            return
        }

        //require project parameter
        if(!apiService.requireParameters(params,response, ['project'])){
            return
        }
        log.debug("ScheduledExecutionController: upload " + params)
        String fileformat = params.format ?:params.fileformat ?: 'xml'
        def parseresult
        def supportedFormats = ['xml', 'yaml']
        if (request.api_version > ApiVersions.V43) {
            supportedFormats<<'json'
        }
        if(request.format in supportedFormats){
            parseresult = scheduledExecutionService.parseUploadedFile(request.getInputStream(), request.format)
        }else if (!apiService.requireParameters(params,response,['xmlBatch'])) {
            return
        }else if(!(fileformat in supportedFormats)){
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code  : 'api.error.jobs.import.format.unsupported',
                    args  : [fileformat]
                ]
            )
        }else if (request.format=='multipartForm' && request instanceof MultipartHttpServletRequest) {
            if (!request.fileNames.toList().contains('xmlBatch')) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.jobs.import.missing-file', args: null])
            }
            def file = request.getFile("xmlBatch")
            parseresult = scheduledExecutionService.parseUploadedFile(file.getInputStream(), fileformat)
        }else if (params.xmlBatch) {
            String fileContent = params.xmlBatch
            parseresult = scheduledExecutionService.parseUploadedFile(fileContent, fileformat)
        }else{
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.missing-file', args: null])
        }
        if (parseresult.errorCode) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: parseresult.errorCode, args: parseresult.args])
        }

        if (parseresult.error) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.jobs.import.invalid', args: [fileformat,parseresult.error]])
        }
        def jobset = parseresult.jobset

        jobset.each{it.job.project=params.project}

        def changeinfo = [user: session.user,method:'apiJobsImport']
        //nb: loadJobs will get correct project auth context
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        def option = params.uuidOption
        def loadresults = scheduledExecutionService.loadImportedJobs(jobset,params.dupeOption, option, changeinfo, authContext,
                (params?.validateJobref=='true'))
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        def jobs = loadresults.jobs
        def jobsi = loadresults.jobsi
        def msgs = loadresults.msgs
        def errjobs = loadresults.errjobs
        def skipjobs = loadresults.skipjobs

        //force hibernate session flush
        ScheduledExecution.withSession { session->
            session.flush()
        }
        withFormat{
            xml{
                apiService.renderSuccessXml(request, response) {
                    delegate.'result'(success: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
                        renderJobsImportApiXML(jobs, jobsi, errjobs, skipjobs, delegate)
                    }
                }
            }
            json{
                apiService.renderSuccessJson(response){
                    renderJobsImportApiJson(jobs, jobsi, errjobs, skipjobs, delegate)
                }
            }
        }
    }

    @Get(uri = "/job/{id}", produces = [MediaType.APPLICATION_JSON, 'text/yaml'])
    @Operation(
        method = "GET",
        summary = "Getting a Job Definition",
        description = '''Export a single job definition, in one of the supported formats.

Authorization required: `read` for the Job.''',
        responses = [
            @ApiResponse(
                responseCode = "200",
                description = '''Job definition, depending on the requested format:

* YAML: [job-yaml](https://docs.rundeck.com/docs/manual/document-format-reference/job-yaml-v12.html) format
* JSON: [job-json](https://docs.rundeck.com/docs/manual/document-format-reference/job-json-v44.html) format (API v44+)''',
                content = [
                    @Content(
                        schema = @Schema(
                            type = 'object',
                            externalDocs = @ExternalDocumentation(
                                url = 'https://docs.rundeck.com/docs/manual/document-format-reference/job-json-v44.html',
                                description = "Job JSON Format"
                            )
                        ),
                        mediaType = MediaType.APPLICATION_JSON
                    ),
                    @Content(
                        schema = @Schema(
                            type = 'string',
                            externalDocs = @ExternalDocumentation(
                                url = 'https://docs.rundeck.com/docs/manual/document-format-reference/job-yaml-v12.html',
                                description = "Job YAML Format"
                            )
                        ),
                        mediaType = 'text/yaml'
                    )
                ]
            ),
            @ApiResponse(
                responseCode = '404',
                description = "Not Found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            ),
            @ApiResponse(
                responseCode = '403',
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            ),
            @ApiResponse(
                responseCode = '415',
                description = '''Unsupported Media type.''',
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            )
        ],
        parameters = [
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                content = @Content(schema = @Schema(implementation = String))
            ),
            @Parameter(
                name = "format",
                description = '''can be "yaml" or "json" (API v44+) to specify the output format''',
                in = ParameterIn.QUERY,
                content = @Content(schema = @Schema(implementation = String))
            )
        ]
    )
    @Tag(name = "jobs")

    /**
     * API: export job definition: /job/{id}, version 1
     */
    def apiJobExport(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job GET : params: " + params)
        def supportedFormats = ['all', 'xml', 'yaml']
        if(request.api_version > ApiVersions.V43){
            supportedFormats << 'json'
        }
        if (!(response.format in supportedFormats)) {
            return apiService.renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code  : 'api.error.item.unsupported-format',
                    args: [response.format]
                ]
            )
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID( params.id )
        if (!apiService.requireExists(response, scheduledExecution,['Job ID',params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_READ], scheduledExecution.project)) {
            return apiService.renderErrorFormat(response,[status:HttpServletResponse.SC_FORBIDDEN,
                    code:'api.error.item.unauthorized',args:['Read','Job ID',params.id]])
        }

        def defaultFormat = 'xml'//TODO: set default to json after 5.0
        def contentTypes = [
            json: 'application/json',
            xml : 'text/xml',
            yaml: 'text/yaml'
        ]
        def format = defaultFormat
        if (response.format != 'all') {
            format = response.format
        }

        response.contentType = contentTypes[format] + ';charset=UTF-8'
        response.outputStream.withWriter('UTF-8') { writer ->
            rundeckJobDefinitionManager.exportAs(format, [scheduledExecution], writer)
        }
        flush(response)
    }

    @Post(uris = ['/job/{id}/run','/job/{id}/executions'])
    @Operation(
        method='POST',
        summary='Running a Job',
        description = '''Run a job specified by ID.

Parameters can be specified in the request body, instead of as query parameters

Authorization required: `run` for the Job resource.
''',
        tags=['jobs'],
        requestBody = @RequestBody(
            description = '''Parameters can be specified in the request body, instead of as query parameters.

(**API v18** or later): The `options` entry can contain a map of option name -> value, in which case the `argString` is ignored.''',
            content = [
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = 'object'),
                    examples = @ExampleObject('''{
    "argString":"...",
    "loglevel":"...",
    "asUser":"...",
    "filter":"...",
    "runAtTime":"...",
    "options": {
        "myopt1":"value"
    }
}'''
                    )
                )
            ]
        ),
        parameters = [
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            ),
            @Parameter(name='argString',in = ParameterIn.QUERY,
                description='argument string to pass to the job, of the form: `-opt value -opt2 value ...`.',
                schema = @Schema(type='string')),
            @Parameter(name='loglevel', in = ParameterIn.QUERY,
                description='argument specifying the loglevel to use',schema=@Schema(type='string',
                allowableValues = ['DEBUG','VERBOSE','INFO','WARN','ERROR'])),
            @Parameter(name='asUser', in = ParameterIn.QUERY,
                description='specifies a username identifying the user who ran the job. Requires `runAs` permission.',
                schema = @Schema(type='string')),
            @Parameter(name='filter', in = ParameterIn.QUERY,
                description='can be a node filter string.',
                schema = @Schema(type='string')),
            @Parameter(name='runAtTime', in = ParameterIn.QUERY,
                description='''Specify a time to run the job (Since: v18).

This is a ISO-8601 date and time stamp with timezone, with optional milliseconds., e.g. `2016-11-23T12:20:55-0800` or `2016-11-23T12:20:55.123-0800`''',
            schema=@Schema(type='string',format='date-time')),
            @Parameter(name='option.OPTNAME',in = ParameterIn.QUERY,
                description='Option value for option named `OPTNAME`. If any `option.OPTNAME` parameters are specified, the `argString` value is ignored (Since: v18).',
                schema = @Schema(type='string')),
            @Parameter(name='meta.KEY',in = ParameterIn.QUERY,
                description='Additional metadata keyd by `KEY`. (Since: v32).',
                schema = @Schema(type='string'))
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Created Execution',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
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
        )
    )
    /**
     * API: Run a job immediately: /job/{id}/run, version 1
     */
    def apiJobRun() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        //require POST for api v14
        if (request.method == 'GET' && request.api_version >= ApiVersions.V14) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
            return
        }
        String jobid = params.id

        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)

        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', jobid])) {
            return
        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(
                session.subject,
                scheduledExecution.project
        )

        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
                                                     scheduledExecution.project
        )) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                                                           code  : 'api.error.item.unauthorized', args: ['Run', 'Job ' +
                    'ID', jobid]]
            )
        }
        def username = session.user

        def jobAsUser, jobArgString, jobLoglevel, jobFilter, jobRunAtTime, jobOptions
        if (request.format == 'json') {
            def data= request.JSON
            jobAsUser = data?.asUser
            jobArgString = data?.argString
            jobLoglevel = data?.loglevel
            jobFilter = data?.filter
            jobRunAtTime = data?.runAtTime
            jobOptions = data?.options
        } else {
            jobAsUser=params.asUser
            jobArgString=params.argString
            jobLoglevel=params.loglevel
            jobRunAtTime = params.runAtTime
            jobOptions = params.option
        }
        if (request instanceof MultipartRequest) {
            //process file uploads
            ((MultipartRequest) request).fileMap.each { String name, file ->
                if (name.startsWith('option.')) {
                    //process file option upload
                    //XXX
//                    String fuploadPlugin = 'filesystem' //TODO: from option config
//
//                    def plugin = pluginService.getPlugin(fuploadPlugin, ExecutionFileStoragePlugin)
//
//                    plugin.store(jobid + name + file.originalFilename + UUID.randomUUID().toString(),
//                                 file.inputStream,
//                                 file.size,
//                                 new Date()
//
//                    )
                } else if (name == 'run' && file.contentType == 'application/json') {
                    //process job run json
                }
            }
        }
        if(jobAsUser) {
            // authorize RunAs User
            if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUNAS],
                                                         scheduledExecution.project
            )) {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                                                               code  : 'api.error.item.unauthorized', args: ['Run as User', 'Job ID', jobid]]
                )
            }
            username = jobAsUser
        }

        def inputOpts = [:]

        if (request.api_version >= ApiVersions.V18 && jobOptions && jobOptions instanceof Map) {
            jobOptions.each { k, v ->
                inputOpts['option.' + k] = v
            }
        } else if (jobArgString) {
            inputOpts["argString"] = jobArgString
        }
        if (jobLoglevel) {
            inputOpts["loglevel"] = jobLoglevel
        }
        if (!scheduledExecution.hasNodesSelectedByDefault()){
            inputOpts['_replaceNodeFilters']='true'
        }

        // convert api parameters to node filter parameters
        def filters = jobFilter?[filter:jobFilter]:FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            inputOpts['_replaceNodeFilters']='true'
            inputOpts['doNodedispatch']=true
            filters.each {k, v ->
                inputOpts[k] = v
            }
            if(null== inputOpts['nodeExcludePrecedence']){
                inputOpts['nodeExcludePrecedence'] = true
            }
        }


        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorXml(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }

        if (request.api_version > ApiVersions.V32 && params.meta instanceof Map) {
            inputOpts.meta = new HashMap<>(params.meta)
        }

        def result
        if (request.api_version > ApiVersions.V17 && jobRunAtTime) {
            inputOpts["runAtTime"] = jobRunAtTime
            result = executionService.scheduleAdHocJob(scheduledExecution,
                        authContext, username, inputOpts)
        }

        if (request.api_version <= ApiVersions.V17 || !jobRunAtTime) {
            inputOpts['executionType'] = 'user'
            result = executionService.executeJob(scheduledExecution,
                        authContext, username, inputOpts)
        }

        if (!result.success) {
            if (result.error == 'unauthorized') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized', args: ['Execute', 'Job ID', jobid]])
            } else if (result.error=='invalid') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.job.options-invalid', args: [result.message]])
            } else if (result.error=='conflict') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_CONFLICT,
                        code: 'api.error.execution.conflict', args: [result.message]])
            } else {
                //failed
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.execution.failed', args: [result.message]])
            }
        }
        def e = result.execution
        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response,[e])
            }
            json{
                return executionService.respondExecutionsJson(request,response,[e],[single:true])
            }
        }
    }

    @Post(uri='/job/{id}/retry/{executionId}')
    @Operation(
        method='POST',
        summary='Retry a Job based on execution',
        description = '''Retry a failed execution on failed nodes only or on the same as the execution.
This is the same functionality as the `Retry Failed Nodes ...` button on the execution page.

Parameters can be specified in the request body, instead of as query parameters

Authorization required: `run` for the Job resource, and `read` or `view` for the Execution resource.

Since: v24
''',
        tags=['jobs'],
        requestBody = @RequestBody(
            description = '''Parameters can be specified in the request body, instead of as query parameters.

(**API v18** or later): The `options` entry can contain a map of option name -> value, in which case the `argString` is ignored.''',
            content = [
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = 'object'),
                    examples = @ExampleObject('''{
    "failedNodes": true,
    "argString":"...",
    "loglevel":"...",
    "asUser":"...",
    "filter":"...",
    "runAtTime":"...",
    "options": {
        "myopt1":"value"
    }
}'''
                    )
                )
            ]
        ),
        parameters = [
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            ),
            @Parameter(
                name = "executionId",
                description = "Execution ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            ),
            @Parameter(name='failedNodes',in = ParameterIn.QUERY,
                description='`false` to run on the same nodes as the original execution, `true`or empty to run only on failed nodes.',
                schema = @Schema(type='boolean')),
            @Parameter(name='argString',in = ParameterIn.QUERY,
                description='argument string to pass to the job, of the form: `-opt value -opt2 value ...`.',
                schema = @Schema(type='string')),
            @Parameter(name='loglevel', in = ParameterIn.QUERY,
                description='argument specifying the loglevel to use',schema=@Schema(type='string',
                    allowableValues = ['DEBUG','VERBOSE','INFO','WARN','ERROR'])),
            @Parameter(name='asUser', in = ParameterIn.QUERY,
                description='specifies a username identifying the user who ran the job. Requires `runAs` permission.',
                schema = @Schema(type='string')),
            @Parameter(name='filter', in = ParameterIn.QUERY,
                description='can be a node filter string.',
                schema = @Schema(type='string')),
            @Parameter(name='runAtTime', in = ParameterIn.QUERY,
                description='''Specify a time to run the job (Since: v18).

This is a ISO-8601 date and time stamp with timezone, with optional milliseconds., e.g. `2016-11-23T12:20:55-0800` or `2016-11-23T12:20:55.123-0800`''',
                schema=@Schema(type='string',format='date-time')),
            @Parameter(name='option.OPTNAME',in = ParameterIn.QUERY,
                description='Option value for option named `OPTNAME`. If any `option.OPTNAME` parameters are specified, the `argString` value is ignored (Since: v18).',
                schema = @Schema(type='string')),
            @Parameter(name='meta.KEY',in = ParameterIn.QUERY,
                description='Additional metadata keyd by `KEY`. (Since: v32).',
                schema = @Schema(type='string'))
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Created Execution',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
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
        )
    )
    def apiJobRetry() {
        if (!apiService.requireApi(request, response, ApiVersions.V24)) {
            return
        }
        String jobId = params.id
        String execId = params.executionId
        String failedOnly = 'true'

        Execution e = Execution.get(execId)
        if(e?.scheduledExecution?.extid != jobId){
            e = null
        }
        if (!apiService.requireExists(response, e, ['Execution ID', execId])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, e.project)
        if (!apiService.requireAuthorized(
            rundeckAuthContextProcessor.authorizeProjectExecutionAny(
                authContext,
                e,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]
            ),
            response,
            [AuthConstants.ACTION_VIEW, 'Execution', execId] as Object[]
        )) {
            return
        }

        if (!apiService.requireExists(response, e.failedNodeList, ['Failed node List for execution ID', execId])) {
            return
        }

        if (request.format == 'json') {
            failedOnly = request.JSON.failedNodes?:'true'
            request.JSON.asUser = request.JSON.asUser?:null
            request.JSON.loglevel = request.JSON.loglevel?:e.loglevel
            if(request.JSON.options){
                def map = FrameworkService.parseOptsFromString(e.argString)
                map.each{k,v ->
                    if(!request.JSON.options.containsKey(k)){
                        request.JSON.options.put(k,v)
                    }
                }
            }else if(!request.JSON.argString){
                request.JSON.argString = request.JSON.argString?:e.argString
            }
        }else{
            failedOnly = params.failedNodes?:'true'
            params.asUser=params.asUser?:null
            params.loglevel=params.loglevel?:e.loglevel
            if(params.option){
                def map = FrameworkService.parseOptsFromString(e.argString)
                map.each{k,v ->
                    if(!params.option.containsKey(k)){
                        params.option.put(k,v)
                    }
                }
            }else if(!params.argString){
                params.argString = params.argString?:e.argString
            }
        }
        if(failedOnly == 'true'){
            params.name=e.failedNodeList
        }

        apiJobRun()
    }


    @Post(uri = '/job/{id}/input/file')
    @Operation(
        method='POST',
        summary='Upload Multiple Files for Job Options',
        description='''Job Options of type `file` require a file input. You can upload multiple files en-masse.

Each uploaded file is assigned a unique "file key" identifier.
You can then Run the Job using the "file key" as the option value.

For multiple files, use a Multi-part request.  For each file, specify the field name as `option.NAME` where NAME
is the option name. The filename is specified normally within the multi-part request.

Since: v19''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            )
        ],
        requestBody = @RequestBody(
            description='''Upload Multiple Files.

For multiple files, use a Multi-part request.  For each file, specify the field name as `option.NAME` where NAME
is the option name. The filename is specified normally within the multi-part request.
''',
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA
            )
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Successful response, with multiple uploaded file tokens',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JobFileUpload)
            )
        )
    )
    protected def apiJobFileMultiUpload(){}

    @Post(uri = '/job/{id}/input/file/{optionName}')
    @Operation(
        method='POST',
        summary='Upload a File for a Job Option',
        description='''Job Options of type `file` require a file input. This endpoint uploads a single file individually.

Each uploaded file is assigned a unique "file key" identifier.
You can then Run the Job using the "file key" as the option value.

Since: v19''',
        tags=['jobs'],
        parameters=[
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            ),
            @Parameter(
                name = "optionName",
                description = "For a single file/option value, specify the option name either as a query parameter or as part of the URL path",
                in = ParameterIn.PATH,
                required = true,
                schema = @Schema(type='string')
            ),
            @Parameter(
                name = "fileName",
                description = "Specify the original file name (optional)",
                in = ParameterIn.QUERY,
                schema = @Schema(type='string')
            )
        ],
        requestBody = @RequestBody(
            description="Upload a single file directly",
            content=@Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM
            )
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = 'Successful response',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JobFileUpload)
            )
        )
    )
    /**
     * API v19, File upload input for job
     * @return
     */
    def apiJobFileUpload() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V19)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        String jobid = params.id

        ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(jobid)

        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', jobid])) {
            return
        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(
                session.subject,
                scheduledExecution.project
        )

        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
                                                     scheduledExecution.project
        )) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                                                           code  : 'api.error.item.unauthorized', args: ['Run', 'Job ' +
                    'ID', jobid]]
            )
        }
        String optionParameterPrefix = 'option.'
        def fileOptions = scheduledExecution.listFileOptions()

        if (!fileOptions) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.job-upload.invalid',
                    args  : [jobid]
            ]
            )
        }
        def fileOptionNames = fileOptions*.name
        def fileMap = [:]
        fileOptions.each { Option option ->
            fileMap[option.name] = option
        }
        def uploadedFileRefs = [:]
        def uploadError
        if (request instanceof MultipartRequest && request.fileMap) {
            def invalid = []
            Map<String,MultipartFile> optionRequestFiles = [:]
            ((MultipartRequest) request).fileNames.each { String name ->
                MultipartFile file = ((MultipartRequest) request).getFile(name)
                if (name.startsWith(optionParameterPrefix)) {
                    //process file option upload
                    String optname = name.substring(optionParameterPrefix.length())
                    //require file option
                    if (optname in fileOptionNames) {
                        optionRequestFiles[optname] = file
                    } else {
                        invalid << name
                    }
                } else {
                    invalid << name
                }
            }
            if (invalid) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.job-upload.option.unexpected',
                        args  : [invalid.join(', ')]
                ]
                )
            }

            long maxsize = fileUploadService.optionUploadMaxSize
            if (maxsize > 0) {
                def find = ((MultipartRequest) request).fileMap.find { String name, file -> file.size > maxsize }
                if (find) {
                    return apiService.renderErrorFormat(response, [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code  : 'api.error.job-upload.filesize',
                            args  : [find.value.size, find.key.substring(optionParameterPrefix.length()), maxsize]
                    ]
                    )
                }
            }
            for (def entry : optionRequestFiles) {
                String optname = entry.key
                def file = entry.value
                try {
                    String ref = fileUploadService.receiveFile(
                            file.inputStream,
                            file.size,
                            authContext.username,
                            file.originalFilename,
                            optname,
                            fileMap[optname].configMap,
                            scheduledExecution.extid,
                            scheduledExecution.project,
                            new Date()
                    )
                    uploadedFileRefs[optname] = ref
                } catch (FileUploadServiceException e) {
                    uploadError = e
                    break
                }
            }
        } else {
            //single option
            if (!apiService.requireParameters(params, response, ['optionName'])) {
                return
            }
            if (!(fileOptionNames.contains(params.optionName))) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.job-upload.option.unexpected',
                        args  : [params.optionName]
                ]
                )
            }
            long maxsize = fileUploadService.optionUploadMaxSize

            if (maxsize > 0 && request.contentLength > maxsize) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.job-upload.filesize',
                        args  : [request.contentLength, params.optionName, maxsize]
                ]
                )
            }
            try {
                //request body is file
                String ref = fileUploadService.receiveFile(
                        request.getInputStream(),
                        request.contentLength,
                        authContext.username,
                        params.fileName,
                        params.optionName,
                        fileMap[params.optionName].configMap,
                        scheduledExecution.extid,
                        scheduledExecution.project,
                        new Date()
                )
                uploadedFileRefs[params.optionName] = ref
            } catch (FileUploadServiceException e) {
                uploadError = e
            }
        }
        if (uploadError) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.job-upload.error',
                    args  : [uploadError.message]
            ]
            )
        }
        respond(new JobFileUpload(total: uploadedFileRefs.size(), options: uploadedFileRefs), [formats: ['xml', 'json']])
    }

    @Get(uri='/jobs/file/{id}')
    @Operation(
        method = "GET",
        summary = 'Get Info About an Uploaded File',
        description = '''Get info about an uploaded file given its ID.''',
        tags = ['jobs'],
        parameters = @Parameter(
            name = "id",
            description = "File ID",
            in = ParameterIn.PATH,
            required = true,
            content = @Content(schema = @Schema(implementation = String))
        ),
        responses = @ApiResponse(
            responseCode = '200',
            description = "File info",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = JobFileInfo),
                examples = @ExampleObject('''
{
  "dateCreated": "2017-02-24T19:10:33Z",
  "execId": 2741,
  "expirationDate": "2017-02-24T19:11:03Z",
  "fileName": null,
  "fileState": "deleted",
  "id": "f985864b-fa1b-4e09-af7a-4315e9908372",
  "jobId": "7b3fff59-7a2d-4a31-a5b2-dd26177c823c",
  "serverNodeUUID": "3425B691-7319-4EEE-8425-F053C628B4BA",
  "sha": "9284ed4fd7fe1346904656f329db6cc49c0e7ae5b8279bff37f96bc6eb59baad",
  "size": 12,
  "user": "admin"
}''')
            )
        )
    )
    /**
     * API v19, File upload input for job
     * @return
     */
    def apiJobFileInfo() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V19)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def jobFileRecord = fileUploadService.findUuid(params.id)
        if (!apiService.requireExists(response, jobFileRecord, ['Job File Record', params.id])) {
            return
        }
        def job = scheduledExecutionService.getByIDorUUID(jobFileRecord.jobId)
        if (!apiService.requireExists(response, job, ['Job File Record', params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, job.project)
        if (!rundeckAuthContextProcessor.authorizeProjectJobAny(
            authContext,
            job,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
            job.project
        )) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_VIEW, 'Job File Record', params.id])
        }

        respond(new JobFileInfo(jobFileRecord.exportMap()), [formats: ['xml', 'json']])
    }
    /**
     * API v19, File upload input for job
     * @return
     */
    def apiJobFilesList() {
        if (!apiService.requireApi(request, response)) {
            return
        }

        if (!apiService.requireApi(request, response, ApiVersions.V19)) {
            return
        }

        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }

        def job = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, job, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, job.project)
        if (!rundeckAuthContextProcessor.authorizeProjectJobAny(
            authContext,
            job,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
            job.project
        )) {
            return apiService.renderUnauthorized(response, [AuthConstants.ACTION_VIEW, 'Job ID', params.id])
        }

        def paging = [offset: 0, max: 20, sort: 'dateCreated', order: 'desc']
        if (params.max) {
            paging.max = params.int('max')
        }
        if (params.offset) {
            paging.offset = params.int('offset')
        }
        String fileState = params.fileState ?: JobFileRecord.STATE_TEMP
        def records = fileUploadService.findRecords(
                job.extid,
                FileUploadService.RECORD_TYPE_OPTION_INPUT,
                fileState,
                paging
        )
        int total = fileUploadService.countRecords(
                job.extid,
                FileUploadService.RECORD_TYPE_OPTION_INPUT,
                fileState
        )
        respond(
                new JobFileInfoList(
                        records.collect{new JobFileInfo(it.exportMap())},
                        paging + [total:total, count: records.size()]
                ),
                [formats: ['xml', 'json']]
        )
    }

    @Delete('/job/{id}')
    @Operation(
        method = 'DELETE',
        summary = 'Deleting a Job Definition',
        description = '''Delete a single job definition.

Authorization required: `delete` for the job.''',
        tags = ['jobs'],
        parameters = [
            @Parameter(
                name = "id",
                description = "Job ID",
                in = ParameterIn.PATH,
                required = true,
                content = @Content(schema = @Schema(implementation = String))
            )
        ],
        responses = [
            @ApiResponse(
                responseCode = '204',
                description = "Successful delete. No Content"
            ),
            @ApiResponse(
                responseCode = '404',
                description = "Not Found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            ),
            @ApiResponse(
                responseCode = '403',
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            ),
            @ApiResponse(
                responseCode = '409',
                description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse))
            )
        ]
    )
    /**
     * API: DELETE job definition: /job/{id}, version 1
     */
    def apiJobDelete() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job DELETE : params: " + params)
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_DELETE],
                scheduledExecution.project)) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Delete', 'Job ID', params.id]])
        }
        def result = scheduledExecutionService.deleteScheduledExecutionById(params.id, authContext,
                false, session.user, 'apiJobDelete')
        if (!result.success) {
            if (result.error?.errorCode == 'notfound') {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND, code: 'api.error.item.doesnotexist',
                        args: ['Job ID', params.id]])
            } else if (result.error?.errorCode == 'unauthorized') {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized',
                        args: ['Delete', 'Job ID', params.id]]
                )
            } else {
                apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_CONFLICT,
                        code: 'api.error.job.delete.failed',
                        args: [result.error.message]]
                )
            }
        } else {
            //return 204 no content
            return render(status: HttpServletResponse.SC_NO_CONTENT)
        }
    }

    @Delete(uri='/job/{id}/executions')
    @Operation(
        method = 'DELETE',
        summary = 'Delete all Executions for a Job',
        description = '''Delete all executions for a Job.''',
        tags = ['jobs', 'execution'],
        parameters = [
            @Parameter(name = 'id', description = 'Job ID', in = ParameterIn
                .PATH, required = true, schema = @Schema(type = 'string'))
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = """Summary of bulk delete results""",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = DeleteBulkResponse)
            )
        )
    )
    /**
     * API: DELETE /job/{id}/executions, version 12
     * delete all executions for a job
     */
    def apiJobExecutionsDelete(){
        if (!apiService.requireApi(request, response)) {
            return
        }
        log.debug("ScheduledExecutionController: /api/job DELETE : params: " + params)
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)
        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(authContext,
                rundeckAuthContextProcessor.authResourceForProject(scheduledExecution.project),
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Delete Execution', 'Project',
                    scheduledExecution.project]])
        }
        def result = scheduledExecutionService.deleteJobExecutions(scheduledExecution, authContext, session.user)
        executionService.renderBulkExecutionDeleteResult(request,response,result)
    }
    @Post(uri='/project/{project}/run/command')
    @Operation(
        method='POST',
        summary='Run Adhoc Command',
        description='''Run a command string.

Authorization required: `run` for project resource type `adhoc`, as well as `runAs` if the runAs parameter is used

Since: v14''',
        tags = ['adhoc'],
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
            ),
            @Parameter(
                name = 'exec',
                description = 'The shell command string to run, e.g. "echo hello".',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'nodeThreadcount',
                description = 'threadcount to use',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'integer')
            ),
            @Parameter(
                name = 'nodeKeepgoing',
                description = 'if "true", continue executing on other nodes even if some fail.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'boolean')
            ),
            @Parameter(
                name = 'asUser',
                description = 'specifies a username identifying the user who ran the command. Requires `runAs` permission.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            )
        ],
        requestBody = @RequestBody(
            description='Request body',
            content=@Content(
                mediaType=MediaType.APPLICATION_JSON,
                schema=@Schema(implementation = ApiRunAdhocRequest),
                examples=@ExampleObject('''
{
    "project":"[project]",
    "exec":"[exec]",
    "nodeThreadcount": 2,
    "nodeKeepgoing": true,
    "asUser": "[asUser]",
    "filter": "[node filter string]"
}''')
            )
        ),
        responses = @ApiResponse(
            responseCode='200',
            description='''item identifying the new execution by ID.''',
            content = [
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object'),
                    examples=@ExampleObject('''{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": 1,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}''')
                )
            ]
        )
    )
    /**
     * API: run simple exec: /api/14/project/PROJECT/run/command
     */
    def apiRunCommandv14(@Parameter(hidden = true) ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
            return
        }
        runAdhocRequest.validate()
        if(runAdhocRequest.hasErrors()){
            return apiService.renderErrorFormat(
                    response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: 'api.error.invalid.request',
                            args: [runAdhocRequest.errors.allErrors.collect { g.message(error: it) }.join("; ")]
                    ]
            )
        }
        if (null==runAdhocRequest.exec || null==runAdhocRequest.project){
            if(!apiService.requireParameters(params, response, ['project','exec'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project
        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}

        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch']=true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }


    @Post(uri='/project/{project}/run/script')
    @Operation(
        method='POST',
        summary='Run Adhoc Script',
        description='''Run a script.

Authorization required: `run` for project resource type `adhoc`, as well as `runAs` if the runAs parameter is used

Since: v14''',
        tags = ['adhoc'],
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
            ),
            @Parameter(
                name = 'argString',
                description = 'Arguments to pass to the script when executed',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'nodeThreadcount',
                description = 'threadcount to use',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'integer')
            ),
            @Parameter(
                name = 'nodeKeepgoing',
                description = 'if "true", continue executing on other nodes even if some fail.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'boolean')
            ),
            @Parameter(
                name = 'asUser',
                description = 'specifies a username identifying the user who ran the command. Requires `runAs` permission.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'scriptInterpreter',
                description = 'a command to use to run the script',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'fileExtension',
                description = 'extension of the script file on the remote node (since v14)',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'interpreterArgsQuoted',
                description = 'if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter`',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'boolean')
            )
        ],
        requestBody = @RequestBody(
            description='''The script file content can be submitted either as a form request
 or multipart attachment with request parameters, or can be a json document.

For Content-Type: `application/x-www-form-urlencoded`

* `scriptFile`: A `x-www-form-urlencoded` request parameter containing the script file content.

For Content-Type: `multipart/form-data`

* `scriptFile`: the script file contents (`scriptFile` being the `name` attribute of the `Content-Disposition` header)''',
            content=[
                @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA,
                    schema=@Schema(type='string')
                ),
                @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED,
                    schema=@Schema(type='string')
                ),
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiRunAdhocRequest),
                    examples = @ExampleObject('''
{
    "project":"[project]",
    "script":"[script]",
    "nodeThreadcount": 1,
    "nodeKeepgoing": true,
    "asUser": "[asUser]",
    "argString": "[argString]",
    "scriptInterpreter": "[scriptInterpreter]",
    "interpreterArgsQuoted": true,
    "fileExtension": "[fileExtension]",
    "filter": "[node filter string]"
}''')
                )
            ]
        ),
        responses = @ApiResponse(
            responseCode='200',
            description='''item identifying the new execution by ID.''',
            content = [
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object'),
                    examples=@ExampleObject('''{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": 1,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}''')
                )
            ]
        )
    )
    /**
     * API: run script: /api/14/project/PROJECT/run/script
     */
    def apiRunScriptv14(ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
            return
        }
        if(null==runAdhocRequest.project || null==runAdhocRequest.script) {
            if (!apiService.requireParameters(params, response, ['project', 'scriptFile'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project

        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        def script
        //read attached script content
        if(runAdhocRequest.script){

        }else if (request instanceof MultipartHttpServletRequest) {
            def file = request.getFile("scriptFile")
            if(!file) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.run-script.upload.missing',args:['scriptFile']])
            }else if(file.empty) {
                return apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.run-script.upload.is-empty'])
            }
            runAdhocRequest.script = new String(file.bytes)
        }else if(params.scriptFile){
            runAdhocRequest.script=params.scriptFile
        }



        //remote any input parameters that should not be used when creating the execution
        ['options','scheduled'].each{params.remove(it)}
//        def scriptInterpreter = null
//        def interpreterArgsQuoted = false
//        if (request.api_version >= ApiVersions.V8) {
//            scriptInterpreter = params.scriptInterpreter ?: null
//            interpreterArgsQuoted = Boolean.parseBoolean(params.interpreterArgsQuoted?.toString())
//        }


        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if(filters){
            filters['doNodedispatch']=true
            filters.each{k,v->
                params[k]=v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results=runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }

    private apiResponseAdhoc(results){
        if (results.failed) {
            results.error = results.message
        }
        if (!results.success) {
            def errors = [results.error]
            if (results.scheduledExecution) {
                errors = []
                results.scheduledExecution.errors.allErrors.each {
                    errors << g.message(error: it)
                }
            }
            if (results.error == 'unauthorized') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                        code: 'api.error.item.unauthorized', args: ['Execute', 'Adhoc', 'Command']])
            } else if (results.error == 'invalid') {
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.execution.invalid', args: [errors.join(", ")]])
            } else {
                //failed
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        code: 'api.error.execution.failed', args: [errors.join(", ")]])
            }
        } else {
            if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
                return apiService.renderErrorFormat(response,[
                        status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        code: 'api.error.item.unsupported-format',
                        args: [response.format]
                ])
            }
            withFormat{
                xml{

                    return apiService.renderSuccessXml(request,response) {
                        delegate.'execution'(
                                id: results.id,
                                href: apiService.apiHrefForExecution(results.execution),
                                permalink: apiService.guiHrefForExecution(results.execution)
                        )
                    }
                }
                json{
                    return apiService.renderSuccessJson(response) {
                        delegate.'message'=("Immediate execution scheduled (${results.id})")
                        delegate.'execution' = [
                                id       : results.id,
                                href     : apiService.apiHrefForExecution(results.execution),
                                permalink: apiService.guiHrefForExecution(results.execution)
                        ]
                    }
                }
            }
        }
    }

    @Post(uri='/project/{project}/run/url')
    @Operation(
        method='POST',
        summary='Run Adhoc Script URL',
        description='''Run a script downloaded from a URL.

Authorization required: `run` for project resource type `adhoc`, as well as `runAs` if the runAs parameter is used

Since: v14''',
        tags = ['adhoc'],
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
            ),
            @Parameter(
                name = 'argString',
                description = 'Arguments to pass to the script when executed',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'scriptURL',
                description = 'A URL pointing to a script file',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'nodeThreadcount',
                description = 'threadcount to use',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'integer')
            ),
            @Parameter(
                name = 'nodeKeepgoing',
                description = 'if "true", continue executing on other nodes even if some fail.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'boolean')
            ),
            @Parameter(
                name = 'asUser',
                description = 'specifies a username identifying the user who ran the command. Requires `runAs` permission.',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'scriptInterpreter',
                description = 'a command to use to run the script',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'fileExtension',
                description = 'extension of the script file on the remote node (since v14)',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'interpreterArgsQuoted',
                description = 'if true, the script file and arguments will be quoted as the last argument to the `scriptInterpreter`',
                in = ParameterIn.QUERY,
                schema = @Schema(type = 'boolean')
            )
        ],
        requestBody = @RequestBody(
            description='''Adhoc Script URL Request''',
            content=[
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiRunAdhocRequest),
                    examples = @ExampleObject('''
{
    "project":"[project]",
    "url":"[scriptURL]",
    "nodeThreadcount": 1,
    "nodeKeepgoing": true,
    "asUser": "[asUser]",
    "argString": "[argString]",
    "scriptInterpreter": "[scriptInterpreter]",
    "interpreterArgsQuoted": true,
    "fileExtension": "[fileExtension]",
    "filter": "[node filter string]"
}''')
                )
            ]
        ),
        responses = @ApiResponse(
            responseCode='200',
            description='''item identifying the new execution by ID.''',
            content = [
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object'),
                    examples=@ExampleObject('''{
  "message": "Immediate execution scheduled (X)",
  "execution": {
    "id": 1,
    "href": "[API Href]",
    "permalink": "[GUI Href]"
  }
}''')
                )
            ]
        )
    )
    /**
     * API: run script: /api/14/project/PROJECT/run/url
     */
    def apiRunScriptUrlv14 (ApiRunAdhocRequest runAdhocRequest){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
            return
        }
        if(null==runAdhocRequest.project || null==runAdhocRequest.url) {
            if (!apiService.requireParameters(params, response, ['project', 'scriptURL'])) {
                return
            }
        }
        def project=runAdhocRequest.project?:params.project
        runAdhocRequest.project=project
        //test valid project
        def exists=frameworkService.existsFrameworkProject(project)
        if (!apiService.requireExists(response, exists, ['project', project])) {
            return
        }

        //remote any input parameters that should not be used when creating the execution
        ['options', 'scheduled'].each {params.remove(it)}

        if(null==runAdhocRequest.url && params.scriptURL){
            runAdhocRequest.url=params.scriptURL
        }


        //convert api parameters to node filter parameters
        def filters = runAdhocRequest.filter ? [filter: runAdhocRequest.filter] :
                FrameworkController.extractApiNodeFilterParams(params)
        if (filters) {
            filters['doNodedispatch'] = true
            filters.each {k, v ->
                params[k] = v
            }
            if (null == params['nodeExcludePrecedence']) {
                params['nodeExcludePrecedence'] = true
            }
        }

        def results = runAdhoc(runAdhocRequest)
        return apiResponseAdhoc(results)
    }

    @Get(uris = ['/job/{id}/executions'])
    @Operation(
        method = 'GET',
        summary = 'Getting Executions for a Job',
        description = '''Get the list of executions for a Job.

Authorizations required: `read` or `view` for the Job, and `read` for the project resource type `execution`.
''',
        tags = ['jobs', 'execution'],
        parameters = [
            @Parameter(name = 'id', description = 'Job ID', in = ParameterIn
                .PATH, required = true, schema = @Schema(type = 'string')),
            @Parameter(name = 'status', description = '''the status of executions you want to be returned.  Must be 
one of "succeeded", "failed", "aborted", or "running".  If this parameter is blank or unset, include all executions.''',
                in = ParameterIn
                    .QUERY, schema = @Schema(type = 'string', allowableValues = ["succeeded", "failed", "aborted",
                    "running"])),
            @Parameter(name = 'max', description = '''indicate the maximum number of results to return. If 
unspecified, all results will be returned''',
                in = ParameterIn.QUERY, schema = @Schema(type = 'integer')),
            @Parameter(name = 'offset', description = '''indicate the 0-indexed offset for the first result to 
return.''',
                in = ParameterIn.QUERY, schema = @Schema(type = 'integer'))
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = "Executions results",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(schema = @Schema(type = 'object')),
                examples = @ExampleObject('''[{
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
}]''')
            )
        )
    )
    /**
     * API: /api/job/{id}/executions , version 1
     */
    def apiJobExecutions() {
        if (!apiService.requireApi(request, response)) {
            return
        }
        if (!apiService.requireParameters(params, response, ['id'])) {
            return
        }
        return apiJobExecutionsResult(true)
    }
    /**
     * non-api interface to job executions results
     */
    def jobExecutionsAjax() {
        if (requireAjax(action: 'jobs', controller: 'menu', params: params)) {
            return
        }
        return apiJobExecutionsResult(false)
    }

    /**
     * API: /api/job/{id}/executions , version 1
     */
    private def apiJobExecutionsResult(boolean apiRequest) {
        def ScheduledExecution scheduledExecution = scheduledExecutionService.getByIDorUUID(params.id)
        if (!apiService.requireExists(response, scheduledExecution, ['Job ID', params.id])) {
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,scheduledExecution.project)

        if (!rundeckAuthContextProcessor.authorizeProjectJobAny(
            authContext,
            scheduledExecution,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
            scheduledExecution.project
        )
        ) {

            return apiService.renderErrorFormat(
                    response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: 'api.error.item.unauthorized',
                            args: ['Read', 'Job ID', params.id]
                    ]
            )
        }
        if (!rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_EVENT,
            AuthConstants.ACTION_READ,
            scheduledExecution.project
        )
        ) {

            return apiService.renderErrorFormat(
                    response,
                    [
                            status: HttpServletResponse.SC_FORBIDDEN,
                            code: 'api.error.item.unauthorized',
                            args: ['Read', 'Events in Project', scheduledExecution.project]
                    ]
            )
        }

        if (apiRequest && request.api_version < ApiVersions.V14 && !(response.format in ['all', 'xml'])) {
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }

        def result = executionService.queryJobExecutions(
                scheduledExecution,
                params['status'],
                params.offset ? params.int('offset') : 0,
                params.max  ? params.int('max') : -1

        )
        def resOffset = params.offset ? params.int('offset') : 0
        def resMax = params.max ?
                params.int('max') :
                configurationService.getInteger("pagination.default.max", 20)
        def total=result.total
        withFormat{
            xml{
                return executionService.respondExecutionsXml(request,response,result.result,[total:total,offset:resOffset,max:resMax])
            }
            json{
                return executionService.respondExecutionsJson(request,response,result.result,[total:total,offset:resOffset,max:resMax])
            }
        }

    }

    @Put(uri='/scheduler/takeover')
    @Operation(
        method='PUT',
        summary='Takeover Schedule in Cluster Mode',
        description='''Tell a Rundeck server in cluster mode to claim all scheduled jobs from another
cluster server.

This endpoint can take over the schedule of certain jobs based on the input:

* specify a server `uuid`: take over all jobs from that server
* specify server `all` value of `true`: take over all jobs regardless of server UUID

Additionally, you can specify a `project` name to take over only jobs matching
the given project name, in combination with the server options.

Alternately, specify one or more job IDs to takeover certain Jobs' schedules.

Authorization required: `ops_admin` for resource type `job`

Since: v14''',
        tags=['cluster','scheduler'],
        requestBody = @RequestBody(
            description='''Takeover Request.

* optional `server` entry, with one of these required entries:
    * `uuid` server UUID to take over from
    * `all` value of `true` to take over from all servers
* optional `project` entry, specifying a project name
* optional `job` entry, with required entry:
    * `id` Job UUID
* optional `jobs` array, each object has:
    * `id` Job UUID
    * (Since: v32)
''',
            required=true,
            content=@Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema=@Schema(type='object'),
                examples = [
                    @ExampleObject(value='''{
  "server": {
    "all": true
  },
  "project": "[PROJECT]"
}''',name='all-servers-project',description='Takeover all jobs for a project'),
                    @ExampleObject(value='''
{
  "job": {
    "id": "[UUID]"
  }
}''',name='job',description='Takeover specific job'),
                    @ExampleObject(value='''{
    "server": {
    "all": true
  },
  "jobs":[
    {
    "id": "[UUID]"
    },
    {
    "id": "[UUID]"
    }
  ]
}''',name='multiple-jobs',description='Takeover multiple jobs. Since: v32')
                ]
            )
        ),
        responses=@ApiResponse(
            responseCode='200',
            description='Successful Response',
            content=[
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema=@Schema(type='object'),
                    examples=[
                        @ExampleObject(value='''
{
  "takeoverSchedule": {
    "jobs": {
      "failed": [],
      "successful": [
        {
          "href": "http://dignan:4440/api/14/job/a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "permalink": "http://dignan:4440/job/show/a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "id": "a1aa53ac-73a6-4ead-bbe4-34afbff8e057",
          "previous-owner": "8F3D5976-2232-4529-847B-8E45764608E3"
        },
        {
          "href": "http://dignan:4440/api/14/job/116e2025-7895-444a-88f7-d96b4f19fdb3",
          "permalink": "http://dignan:4440/job/show/116e2025-7895-444a-88f7-d96b4f19fdb3",
          "id": "116e2025-7895-444a-88f7-d96b4f19fdb3",
          "previous-owner": "8F3D5976-2232-4529-847B-8E45764608E3"
        }
      ],
      "total": 2
    },
    "server": {
      "uuid": "8F3D5976-2232-4529-847B-8E45764608E3"
    }
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}''',
                        description='response for `uuid` specified',name='uuid-specified'),
                        @ExampleObject(value='''
{
  "takeoverSchedule": {
    "jobs": {
      "failed": [],
      "successful": [{"job":"data"}],
      "total": 2
    },
    "server": {
      "all": true
    }
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}''',
                        description='response for `all` specified',name='all-specified'),
                        @ExampleObject(value='''{
  "takeoverSchedule": {
    "jobs": {
      "failed": [],
      "successful": [{"job":"data"}],
      "total": 2
    },
    "project": "My Project"
  },
  "self": {
    "server": {
      "uuid": "C677C663-F902-4B97-B8AC-4AA57B58DDD6"
    }
  },
  "message": "Schedule Takeover successful for 2/2 Jobs.",
  "apiversion": 14,
  "success": true
}''',
                        description='response for `project` specified',name='project-specified')
                    ]
                )
            ]
        )
    )
    /**
     * API: /api/14/scheduler/takeover
     */
    def apiJobClusterTakeoverSchedule (){
        if (!apiService.requireApi(request,response,ApiVersions.V14)) {
            return
        }
        def api17 = request.api_version >= ApiVersions.V17

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        //test valid project

        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(authContext, AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN])) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [
                            'Reschedule Jobs (admin)',
                            'Server',
                            frameworkService.getServerUUID()
                    ]
            ])
        }
        if (!frameworkService.isClusterModeEnabled()) {
            withFormat {
                xml {
                    return apiService.renderSuccessXml(request, response) {
                        delegate.'result'(success: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
                            delegate.'message'("No action performed, cluster mode is not enabled.")
                        }
                    }
                }
                json {

                    return apiService.renderSuccessJson(response) {
                        delegate.'message'=("No action performed, cluster mode is not enabled.")
                        success=true
                        apiversion=ApiVersions.API_CURRENT_VERSION
                        self=[server:[uuid:frameworkService.getServerUUID()]]
                    }
                }
            }
        }

        String serverUUID=null
        boolean serverAll=false
        String project=null
        def jobIds=[]
        def jobid=null
        if(request.format=='json' ){
            def data= request.JSON
            serverUUID = data?.server?.uuid?:null
            serverAll = data?.server?.all?true:false
            project = data?.project?:null
            jobid = data?.job?.id?:null
            if(jobid){
                jobIds << jobid
            }
            if(request.api_version >= ApiVersions.V32 && data?.jobs){
                data?.jobs.each{job->
                    jobIds << job.id
                }
            }
        }else if(request.format=='xml' || !request.format){
            def data= request.XML
            if(data.name()=='server'){
                serverUUID = data.'@uuid'?.text()?:null
                serverAll = data.'@all'?.text()=='true'
            }else if(data.name()=='takeoverSchedule'){
                serverUUID = data.server?.'@uuid'?.text()?:null
                serverAll = data.server?.'@all'?.text()=='true'
                project = data.project?.'@name'?.text()?:null
                if(request.api_version >= ApiVersions.V32){
                    if(data.job?.size()>0){
                        data.job?.each{ job ->
                            jobIds << job?.'@id'?.text()

                        }
                    }
                }else{
                    jobid = data.job?.'@id'?.text()?:null
                    if(jobid){
                        jobIds << jobid
                    }
                }
            }
        }else{
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.invalid.request',
                    args: ['Expected content of type text/xml or text/json, content was of type: ' + request.format]])
        }
        if (!serverUUID && !serverAll && !jobIds) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request', args: ['Expected server.uuid or server.all or job.id in request.']])
        }

        def reclaimMap=scheduledExecutionService.reclaimAndScheduleJobs(serverUUID,serverAll,project,jobIds?:null)
        def successCount=reclaimMap.findAll {it.value.success}.size()
        def failedCount = reclaimMap.size() - successCount
        //TODO: retry for failed reclaims?

        def jobData = { entry ->
            def dat=[
                    id: entry.key,
                    href: apiService.apiHrefForJob(entry.value.job),
                    permalink:apiService.guiHrefForJob(entry.value.job),

            ]
            if(api17){
                dat['previous-owner']=entry.value.previous
            }
            dat
        }
        def jobLink={ delegate, entry->
            delegate.'job'(jobData(entry))
        }
        def successMessage= "Schedule Takeover successful for ${successCount}/${reclaimMap.size()} Jobs."
        withFormat {
            xml{
                return apiService.renderSuccessXml(request,response) {
                    delegate.'takeoverSchedule'{
                        delegate.'self' {
                            delegate.'server'(uuid: frameworkService.getServerUUID())
                        }
                        if(!serverAll) {
                            delegate.'server'(uuid: serverUUID)
                        }else{
                            delegate.'server'(all: true)
                        }
                        if(project){
                            delegate.'project'(name:project)
                        }
                        if(jobid){
                            delegate.'job'(id:jobid)
                        }
                        if(jobIds){
                            jobIds.each { jid ->
                                delegate.'job'(id:jid)
                            }
                        }
                        delegate.'jobs'(total: reclaimMap.size()){
                            delegate.'successful'(count: successCount) {
                                reclaimMap.findAll { it.value.success }.each(jobLink.curry(delegate))
                            }
                            delegate.'failed'(count: failedCount) {
                                reclaimMap.findAll { !it.value.success }.each(jobLink.curry(delegate))
                            }
                        }
                    }
                }
            }
            json{
                def datamap=serverAll?[server:[all:true]]:[server:[uuid: serverUUID]]
                if(project){
                    datamap.project=project
                }
                render(contentType: "application/json",text: [
                    success:true,
                    apiversion:ApiVersions.API_CURRENT_VERSION,
                    message: successMessage,
                    self:[server:[uuid:frameworkService.getServerUUID()]],
                    takeoverSchedule:datamap+[
                        jobs:[
                            total:reclaimMap.size(),
                            successful:reclaimMap.findAll { it.value.success }.collect (jobData),
                            failed:reclaimMap.findAll { !it.value.success }.collect (jobData)
                        ]
                    ]
                ] as JSON)
            }
        }
    }

}

class JobXMLException extends Exception{

    public JobXMLException() {
        super();
    }

    public JobXMLException(String s) {
        super(s);
    }

    public JobXMLException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public JobXMLException(Throwable throwable) {
        super(throwable);
    }

}
