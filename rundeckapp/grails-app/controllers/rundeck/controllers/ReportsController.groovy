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
import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ExecQueryFilterCommand
import com.dtolabs.rundeck.app.support.ReportQuery
import com.dtolabs.rundeck.app.support.StoreFilterCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.config.Features
import grails.converters.JSON
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.data.model.v1.user.RdUser
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import org.rundeck.core.auth.AuthConstants
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.data.util.OptionsParserUtil
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.ReportService

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Matcher

@Controller
class ReportsController extends ControllerBase{
    def reportService
    def userService
    def FrameworkService frameworkService
    def scheduledExecutionService
    def MetricService metricService
    def ReferencedExecutionDataProvider referencedExecutionDataProvider
    static allowedMethods = [

    ]

    public def index(){

        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject, params.project)

        if (unauthorizedResponse(
                rundeckAuthContextProcessor.authorizeProjectResource(
                        authContext,
                        AuthConstants.RESOURCE_TYPE_EVENT,
                        AuthConstants.ACTION_READ,
                        params.project
                ),
                AuthConstants.ACTION_READ,
                'Events in project',
                params.project
        )) {
            return
        }
    }
    public def index_old (ExecQuery query) {
        //data binding allows '123' followed by any characters to bind as integer 123, prevent additional chars after the integer value
        if (params.max != null && params.max != query.max.toString()) {
            query.errors.rejectValue('max', 'typeMismatch.java.lang.Integer', ['max'] as Object[], 'invalid')
        }
        if (params.offset != null && params.offset != query.offset.toString()) {
            query.errors.rejectValue('offset', 'typeMismatch.java.lang.Integer', ['offset'] as Object[], 'invalid')
        }
        if (query.hasErrors()) {
            return render(view: '/common/error', model: [beanErrors: query.errors])
        }
        //find previous executions
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)

        if(unauthorizedResponse(
            rundeckAuthContextProcessor.authorizeProjectResource(
                authContext,
                AuthConstants.RESOURCE_TYPE_EVENT,
                AuthConstants.ACTION_READ,
                params.project
            ),
            AuthConstants.ACTION_READ,
            'Events in project',
            params.project)
        ){
            return
        }
        def RdUser u = userService.findOrCreateUser(session.user)
        def filterPref= userService.parseKeyValuePref(u.filterPref)

        def options = [:]
        if (params['execRptCustomView']) {
            params.each {String k, v ->
                def m = k =~ /^(.*)Show$/
                if (m.matches() && (v == 'true' || v == 'on')) {
                    log.info("saw view option: ${m.group(1)}")
                    options[m.group(1)] = true
                }
            }
            session['exec_reports_options'] = options
        }

        if(params['Clear']){
            query=new ExecQuery()
            //no default filter
            params.recentFilter=null
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
        }
        if(query && !query.projFilter && params.project){
            query.projFilter = params.project
        }
        if(params.sessionOnly){
            //auto date filter based on session login
            query.dostartafterFilter=true
            query.startafterFilter=new Date(session.creationTime)
        }
        if(params.includeJobRef && params.jobIdFilter){
            ScheduledExecution.withTransaction {
                ScheduledExecution sched = !params.jobIdFilter.toString().isNumber() ? ScheduledExecution.findByUuid(params.jobIdFilter) : ScheduledExecution.get(params.jobIdFilter)
                def list = []
                if(sched!= null) {
                    list = referencedExecutionDataProvider.executionProjectList(sched.uuid)
                }
                def allowedProjects = []
                list.each { project ->
                    if(project != params.project){
                        if(!rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_EVENT, AuthConstants.ACTION_READ,
                                project)){
                            log.debug('Cant read executions on project ' + project)
                        }else{
                            allowedProjects << project
                        }
                    }else{
                        allowedProjects << project
                    }
                }
                if(allowedProjects){
                    query.execProjects = allowedProjects
                }
            }

        }

        Map<Explanation.Code, List> authorizations = reportService.jobHistoryAuthorizations(authContext, params.project)
        query.excludeJobListFilter = authorizations.get(ReportService.DENIED_VIEW_HISTORY_JOBS)

        if(null!=query){
            query.configureFilter()
        }
        def curdate=new Date()
        def model =
                metricService?.withTimer(ReportsController.name, 'index.getExecutionReports') {
                    reportService.getExecutionReports(query, true)
                } ?: reportService.getExecutionReports(query, true)

//        System.err.println("("+actionName+"): lastDate: "+model.lastDate);
        if(model.lastDate<1 && query.recentFilter ){
            model.lastDate=curdate.time
        }else if (model.lastDate<1 && query.doendafterFilter  && (!query.doendbeforeFilter || curdate.time<query.endbeforeFilter.time)){
            model.lastDate=curdate.time
        }
        if(query?.offset && query?.offset >0){
            model.remove('lastDate')
        }
//        System.err.println("lastDatex: "+model.lastDate);
        model = reportService.finishquery(query,params,model)
        model.filterPref=filterPref
        return model
    }

    def since(ExecQuery query){
       //find previous executions
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_EVENT,
            AuthConstants.ACTION_READ,
            params.project
        ), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }
        if (params.max != null && params.max != query.max.toString()) {
            query.errors.rejectValue('max', 'typeMismatch.java.lang.Integer', ['max'] as Object[], 'invalid')
        }
        if (params.offset != null && params.offset != query.offset.toString()) {
            query.errors.rejectValue('offset', 'typeMismatch.java.lang.Integer', ['offset'] as Object[], 'invalid')
        }
        if (query.hasErrors()) {
            return render(view: '/common/error', model: [beanErrors: query.errors])
        }
        def RdUser u = userService.findOrCreateUser(session.user)

        def options = [:]

        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
        }
        if(query && !query.projFilter && params.project){
            query.projFilter = params.project
        }

//        if(null!=query){
//            query.configureFilter()
//        }

        //configure "endafterFilter" based on input time
        def time=0
        def errmsg
        if(params.since){
            try{
            Date d = new Date(Long.parseLong(params.since)+1)
            time=d.time
            query.endafterFilter=d
            query.doendafterFilter=true
            }catch(NumberFormatException e){
                errmsg=e.getMessage()
            }
        }

        def curdate=new Date()


        def count= reportService.countExecutionReports(query)
//        System.err.println("checking query for reports from: "+(curdate.time-time) + " objFilter: "+query.objFilter+", result: "+count);

        withFormat{
            html{
                if(errmsg){
                    return renderErrorFragment("Invalid date: ${errmsg}")
                }else{
                    render(contentType:"text/html"){
                        span('class':'eventsCountContent',count+" new")
                    }
                }
            }
            json{
                def out = [:]
                if(errmsg){
                    out.error = [message:flash.error]
                }else{
                    out.since = [
                            count: count,
                            time: time
                    ]
                }
                render out as JSON
            }
        }
    }

    def eventsAjax(ExecQuery query){
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)


        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_EVENT,
            AuthConstants.ACTION_READ,
            params.project
        ), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }

        if (query.hasErrors()) {
            response.status=400
            return render(contentType: 'application/json', text:  [errors: query.errors] as JSON)
        }
        def results = index_old(query)
        results.reports=results?.reports.collect{
            def map=it.toMap()
            map.duration= (it.dateCompleted ?: new Date()).time - it.dateStarted.time
            if(map.executionUuid){
                //nb:response data type expects string
                try {
                    map.execution = Execution.findByUuid(map.executionUuid)?.toMap()
                    map.executionId= map.execution.id.toString()
                    map.executionHref = createLink(controller: 'execution', action: 'show', absolute: false, id: map.execution.id, params: [project: (map?.project != null)? map.project : params.project])

                } catch (Exception e) {
                    log.debug("Error getting Execution: " + e.message)
                }
           }

            map.jobName= map.remove('reportId')
            map.user= map.remove('author')
            map.executionString= map.remove('title')
            return map.execution?map:null
        }.findAll{it}
//        results.params=params
        results.query=null

        render(contentType: 'application/json', text: results as JSON)
    }

    /**
     * API actions
     *
     */

    /**
     * Utility: parse string into Date, as either unix millisecond, or W3C date format.
     */
    public static Date parseDate(String input){
        long endtime=-1
        try{
            endtime=Long.parseLong(input)
        }catch(Exception e){

        }
        if(endtime>0){
            return new Date(endtime)
        }
        //attempt to parse w3c dateTime format:
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.parse(input)
    }


    @Get(uri='/project/{project}/history')
    @Operation(
        method = 'GET',
        summary = 'Listing History',
        description = '''
List the event history for a project.''',
        tags = ['history'],
        parameters = [
            @Parameter(
                name = 'project',
                in = ParameterIn.PATH,
                description = 'Project Name',
                required = true,
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'jobIdFilter',
                in = ParameterIn.QUERY,
                description = 'include events for a job ID.',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'reportIdFilter',
                in = ParameterIn.QUERY,
                description = 'include events for a event Name.',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'userFilter',
                in = ParameterIn.QUERY,
                description = 'include events created by a user.',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'statFilter',
                in = ParameterIn.QUERY,
                description = 'include events based on result status.  this can be \'succeed\',\'fail\', or \'cancel\'.',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'jobListFilter',
                in = ParameterIn.QUERY,
                description = 'include events for the job by name, format: \'group/name\'.  To use multiple values, include this parameter multiple times.',
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name = 'excludeJobListFilter',
                in = ParameterIn.QUERY,
                description = 'exclude events for the job by name, format: \'group/name\'. To use multiple values, include this parameter multiple times.',
                array = @ArraySchema(schema = @Schema(type = 'string'))
            ),
            @Parameter(
                name = 'recentFilter',
                in = ParameterIn.QUERY,
                description = '''Use a simple text format to filter events that occurred within a period of time. The format is "XY" where X is an integer, and "Y" is one of:
        * `h`: hour
        * `d`: day
        * `w`: week
        * `m`: month
        * `y`: year
        So a value of "2w" would return events within the last two weeks.''',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'begin',
                in = ParameterIn.QUERY,
                description = '''Specify exact date for earliest result. a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ"''',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'end',
                in = ParameterIn.QUERY,
                description = '''Specify exact date for latest result. a unix millisecond timestamp, or a W3C dateTime string in the format "yyyy-MM-ddTHH:mm:ssZ"''',
                schema = @Schema(type = 'string')
            ),
            @Parameter(
                name = 'max',
                in = ParameterIn.QUERY,
                description = '''indicate the maximum number of events to return. The default maximum to return is 20''',
                schema = @Schema(type = 'integer')
            ),
            @Parameter(
                name = 'offset',
                in = ParameterIn.QUERY,
                description = '''indicate the 0-indexed offset for the first event to return''',
                schema = @Schema(type = 'integer')
            )
        ],
        responses = @ApiResponse(
            responseCode = '200',
            description = 'History results',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = @ExampleObject('''{
  "paging": {
    "count": 10,
    "total": 110,
    "max": 20,
    "offset": 100
  },
  "events": [
  {
  "starttime": 123,
  "endtime": 123,
  "title": "[job title, or adhoc]",
  "status": "[status]",
  "statusString": "[string]",
  "summary": "[summary text]",
  "node-summary": {
    "succeeded": 1,
    "failed": 2,
    "total": 3
  },
  "user": "[user]",
  "project": "[project]",
  "date-started": "[yyyy-MM-ddTHH:mm:ssZ]",
  "date-ended": "[yyyy-MM-ddTHH:mm:ssZ]",
  "job": {
    "id": "[uuid]",
    "href": "[api href]"
  },
  "execution": {
    "id": "[id]",
    "href": "[api href]"
  }
}
  ]
}''')
            )
        )
    )
    /**
     * API, /api/14/project/PROJECT/history
     */
    def apiHistoryv14(@Parameter(hidden=true) ExecQuery query){
        if(!apiService.requireApi(request,response)){
            return
        }
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        //test valid project
        Framework framework = frameworkService.getRundeckFramework()

        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])

        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_EVENT,
            AuthConstants.ACTION_READ,
            params.project
        )) {
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized', args: ['Read Events', 'Project', params.project]])
        }
        params.projFilter=params.project
        query.projFilter = params.project


        //attempt to parse/bind "end" and "begin" parameters
        if(params.begin){
            try{
                query.endafterFilter=parseDate(params.begin)
                query.doendafterFilter=true
            }catch(ParseException e){
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.history.date-format', args: ['begin', params.begin]])

            }
        }
        if(params.end){
            try{
                query.endbeforeFilter=parseDate(params.end)
                query.doendbeforeFilter=true
            }catch(ParseException e){
                return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.history.date-format', args: ['end', params.end]])
            }
        }

        if(null!=query){
            query.configureFilter()
        }
        def model =
                metricService?.withTimer(ReportsController.name, 'apiHistory.getExecutionReports') {
                    reportService.getExecutionReports(query, true)
                } ?: reportService.getExecutionReports(query, true)
        model = reportService.finishquery(query,params,model)

        def statusMap = [scheduled: ExecutionService.EXECUTION_SCHEDULED,
            (ExecutionService.EXECUTION_SCHEDULED): ExecutionService.EXECUTION_SCHEDULED,
            succeed: ExecutionService.EXECUTION_SUCCEEDED,
            (ExecutionService.EXECUTION_SUCCEEDED): ExecutionService.EXECUTION_SUCCEEDED,
            cancel: ExecutionService.EXECUTION_ABORTED,
            (ExecutionService.EXECUTION_ABORTED): ExecutionService.EXECUTION_ABORTED,
            fail: ExecutionService.EXECUTION_FAILED,
            (ExecutionService.EXECUTION_FAILED): ExecutionService.EXECUTION_FAILED,
            retry: ExecutionService.EXECUTION_FAILED_WITH_RETRY,
            (ExecutionService.EXECUTION_FAILED_WITH_RETRY): ExecutionService.EXECUTION_FAILED_WITH_RETRY,
            timeout: ExecutionService.EXECUTION_TIMEDOUT,
            (ExecutionService.EXECUTION_TIMEDOUT): ExecutionService.EXECUTION_TIMEDOUT]
        def controller = this
        withFormat{
            '*' {
                return apiService.renderSuccessJson(response){
                    paging=[
                            count:model.reports.size(),
                            total:model.total,
                            max: model.max,
                            offset: model.offset
                    ]

                    delegate.'events'=array{
                        model.reports.each{  rpt->
                            def nodes=rpt.node
                            final Matcher matcher = nodes =~ /^(\d+)\/(\d+)\/(\d+)$/
                            def nodesum=[rpt.status =='succeed'?1:0,rpt.status =='succeed'?0:1,1]
                            if(matcher.matches()){
                                nodesum[0]=Integer.parseInt matcher.group(1)
                                nodesum[1]=Integer.parseInt matcher.group(2)
                                nodesum[2]=Integer.parseInt matcher.group(3)
                            }
                            delegate.'element'{
                                starttime=rpt.dateStarted.time
                                endtime=rpt.dateCompleted.time
                                title=(rpt.reportId?:'adhoc')
                                status=(statusMap[rpt.status]?:ExecutionService.EXECUTION_STATE_OTHER)
                                statusString=(rpt.status)
                                summary=(rpt.adhocScript?:rpt.title)
                                delegate.'node-summary'=[succeeded:nodesum[0],failed:nodesum[1],total:nodesum[2]]
                                user=(rpt.author)
                                project=(rpt.project)
                                if(rpt.status=='cancel' && rpt.abortedByUser){
                                    abortedby=(rpt.abortedByUser)
                                }
                                delegate.'date-started'=(g.w3cDateValue(date:rpt.dateStarted))
                                delegate.'date-ended'=(g.w3cDateValue(date:rpt.dateCompleted))
                                if(rpt.jobId){
                                    def foundjob=scheduledExecutionService.getByIDorUUID(rpt.jobId)
                                    if(foundjob){
                                        job = [
                                                id       : foundjob.extid,
                                                href     : apiService.apiHrefForJob(foundjob),
                                                permalink: apiService.guiHrefForJob(foundjob)
                                        ]
                                    }
                                }
                                if(rpt.executionId){
                                    def foundExec=Execution.get(rpt.executionId)
                                    if(foundExec) {
                                        execution = [
                                                id       : rpt.executionId,
                                                href     : apiService.apiHrefForExecution(foundExec),
                                                permalink: apiService.guiHrefForExecution(foundExec)
                                        ]
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(controller.isAllowXml()) {
                xml {
                    return apiService.renderSuccessXml(request, response) {
                        delegate.'events'(count: model.reports.size(), total: model.total, max: model.max, offset: model.offset) {
                            model.reports.each { rpt ->
                                def nodes = rpt.node
                                final Matcher matcher = nodes =~ /^(\d+)\/(\d+)\/(\d+)$/
                                def nodesum = [rpt.status == 'succeed' ? 1 : 0, rpt.status == 'succeed' ? 0 : 1, 1]
                                if (matcher.matches()) {
                                    nodesum[0] = matcher.group(1)
                                    nodesum[1] = matcher.group(2)
                                    nodesum[2] = matcher.group(3)
                                }
                                event(starttime: rpt.dateStarted.time, endtime: rpt.dateCompleted.time) {
                                    title(rpt.reportId ?: 'adhoc')
                                    status(statusMap[rpt.status] ?: ExecutionService.EXECUTION_STATE_OTHER)
                                    statusString(rpt.status)
                                    summary(rpt.adhocScript ?: rpt.title)
                                    delegate.'node-summary'(succeeded: nodesum[0], failed: nodesum[1], total: nodesum[2])
                                    user(rpt.author)
                                    project(rpt.project)
                                    if (rpt.status == 'cancel' && rpt.abortedByUser) {
                                        abortedby(rpt.abortedByUser)
                                    }
                                    delegate.'date-started'(g.w3cDateValue(date: rpt.dateStarted))
                                    delegate.'date-ended'(g.w3cDateValue(date: rpt.dateCompleted))
                                    if (rpt.jobId) {
                                        def foundjob = scheduledExecutionService.getByIDorUUID(rpt.jobId)
                                        def jparms = [id: foundjob ? foundjob.extid : rpt.jobId]
                                        if (foundjob) {
                                            jparms.href = apiService.apiHrefForJob(foundjob)
                                            jparms.permalink = apiService.guiHrefForJob(foundjob)
                                        }
                                        job(jparms)
                                    }
                                    if (rpt.executionId) {
                                        def foundExec = Execution.get(rpt.executionId)
                                        def execparms = [id: rpt.executionId]
                                        if (foundExec) {
                                            execparms.href = apiService.apiHrefForExecution(foundExec)
                                            execparms.permalink = apiService.guiHrefForExecution(foundExec)
                                        }
                                        execution(execparms)
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
