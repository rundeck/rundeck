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
import com.dtolabs.rundeck.app.support.ExecQueryFilterCommand
import com.dtolabs.rundeck.app.support.StoreFilterCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.common.Framework
import grails.converters.JSON
import org.grails.plugins.metricsweb.MetricService
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.ReportService

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ReportQuery
import rundeck.User
import rundeck.ReportFilter
import rundeck.services.FrameworkService
import com.dtolabs.rundeck.app.api.ApiVersions

class ReportsController extends ControllerBase{
    def reportService
    def userService
    def FrameworkService frameworkService
    AppAuthContextProcessor rundeckAuthContextProcessor
    def scheduledExecutionService
    def MetricService metricService
    static allowedMethods = [
            deleteFilter    : 'POST',
            storeFilter     : 'POST',
            saveFilterAjax  : 'POST',
            deleteFilterAjax: 'POST',
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
        def usedFilter
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
        def User u = userService.findOrCreateUser(session.user)
        def filterPref= userService.parseKeyValuePref(u.filterPref)
        if(params.size()<1 && !params.filterName && u && params.formInput!='true' && actionName=='index'){
            if(filterPref['events']){
                params.filterName=filterPref['events']
            }
        }
        if(params.filterName){
            //load a named filter and create a query from it
            if(u){
                ReportFilter filter = ReportFilter.findByNameAndUser(params.filterName,u)
                if(filter){
                    def query2 = filter.createQuery()
                    query2.setPagination(query)
                    query=query2
                    def props=query.properties
                    params.putAll(props)
                    usedFilter=params.filterName
                }
            }
        }
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
            usedFilter=null
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
                def list = ReferencedExecution.executionIdList(sched)
                def include = []
                list.each {refex ->
                    boolean add = true
                    if(refex.project != params.project){
                        if(unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_EVENT, AuthConstants.ACTION_READ,
                                params.project), AuthConstants.ACTION_READ,'Events in project',refex.project)){
                            log.debug('Cant read executions on project '+refex.project)
                        }else{
                            include << String.valueOf(refex.executionId)
                        }
                    }else{
                        include << String.valueOf(refex.executionId)
                    }
                }
                if(include){
                    query.execIdFilter = include
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
//        System.err.println("("+actionName+"): usedFilter: "+usedFilter+", p: "+params.filterName);
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
        if(usedFilter){
            model.filterName=usedFilter
            model.paginateParams['filterName']=usedFilter
        }
        model.filterPref=filterPref
        return model
    }

    def since(ExecQuery query){
       //find previous executions
        def usedFilter
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
        def User u = userService.findOrCreateUser(session.user)

        if(params.filterName){
            //load a named filter and create a query from it
            if(u){
                ReportFilter filter = ReportFilter.findByNameAndUser(params.filterName,u)
                if(filter){
                    def query2 = filter.createQuery()
                    query2.setPagination(query)
                    query=query2
                    def props=query.properties
                    params.putAll(props)
                    usedFilter=params.filterName
                }
            }
        }
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
            xml {
                render(contentType:"text/xml"){
                    if(errmsg){
                        delegate.'error'{
                            response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,flash.error)
                            delegate.'message'(flash.error)
                        }
                    }else{
                        result(success:true){
                            delegate.'since'{
                                delegate.'count'(count)
                                delegate.'time'(time)
                            }
                        }
                    }
                }
            }
        }
    }
    def eventsFragment(ExecQuery query) {
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(session.subject,params.project)

        if (unauthorizedResponse(rundeckAuthContextProcessor.authorizeProjectResource(
            authContext,
            AuthConstants.RESOURCE_TYPE_EVENT,
            AuthConstants.ACTION_READ,
            params.project
        ), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }
        def results = index_old(query)
        results.params=params
        return results
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
            if(map.jcExecId){
                map.executionId= map.remove('jcExecId')
                try {
                    map.execution = Execution.get(Long.parseLong(map.executionId)).toMap()
                    map.executionHref = createLink(controller: 'execution', action: 'show', absolute: false, id: map.executionId, params: [project: params.project])
                } catch (Exception e) {

                }
            }
            map.jobName= map.remove('reportId')
            if(map.jcJobId){
                map.jobId= map.remove('jcJobId')
                try {
                    def job = ScheduledExecution.get(Long.parseLong(map.jobId))
                    map.jobId=job?.extid
                    map.jobDeleted = job==null
                    map['jobPermalink']= createLink(
                            controller: 'scheduledExecution',
                            action: 'show',
                            absolute: true,
                            id: job?.extid,
                            params:[project:job?.project]
                    )
                    map.jobName=job?.jobName
                    map.jobGroup=job?.groupPath
                }catch(Exception e){
                }
                if(map.execution?.argString){
                    map.execution.jobArguments=FrameworkService.parseOptsFromString(map.execution.argString)
                }
            }
            map.user= map.remove('author')
            map.executionString= map.remove('title')
            return map.execution?map:null
        }.findAll{it}
//        results.params=params
        results.query=null

        render(contentType: 'application/json', text: results as JSON)
    }


    public def storeFilter(ReportQuery query, StoreFilterCommand storeFilterCommand) {
        withForm{
        if(storeFilterCommand.hasErrors()){
            request.errors=storeFilterCommand.errors
            return renderErrorView([:])
        }
        def User u = userService.findOrCreateUser(session.user)
        def ReportFilter filter
        def boolean saveuser=false
        if(params.newFilterName && !params.existsFilterName){
            filter= new ReportFilter(query.properties)
            filter.name=params.newFilterName
            u.addToReportfilters(filter)
            saveuser=true
        }else if(params.existsFilterName){
            filter = ReportFilter.findByNameAndUser(params.existsFilterName,u)
            if(filter){
                filter.properties=query.properties
            }
        }else if(!params.newFilterName && !params.existsFilterName){
            flash.error="Filter name not specified"
            params.saveFilter=true
            chain(controller:'reports',action:'index',params:params)
        }
        filter.fillProperties()
        if(!filter.save(flush:true)){
            flash.errors = filter.errors
            params.saveFilter=true
            chain(controller:'reports',action:'index',params:params)
        }
        if(saveuser){
            if(!u.save(flush:true)){
                return renderErrorView([beanErrors: u.errors])
            }
        }
            redirect(controller: 'reports', action: 'index', params: [filterName: filter.name, project: params.project])
        }.invalidToken {
            flash.error=g.message(code:'request.error.invalidtoken.message')
            redirect(controller: 'reports', action: 'index', params: [project: params.project])
        }
    }

    public def saveFilterAjax(ExecQueryFilterCommand query) {
        withForm {

            g.refreshFormTokensHeader()
            if (query.hasErrors()) {
                return apiService.renderErrorFormat(
                        response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.invalid.request',
                        args  : [query.errors.allErrors.collect { it.toString() }.join("; ")]
                ]
                )
            }

            def User u = userService.findOrCreateUser(session.user)
            def ReportFilter filter
            def boolean saveuser = false
            if (query.newFilterName && !query.existsFilterName) {
                if (ReportFilter.findByNameAndUser(query.newFilterName, u)) {
                    return apiService.renderErrorFormat(
                            response, [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code  : 'request.error.conflict.already-exists.message',
                            args  : ["Job Filter", query.newFilterName]
                    ]
                    )
                }
                filter = ReportFilter.fromQuery(query)
                filter.name = query.newFilterName
                filter.user = u
                if (!filter.validate()) {
                    return apiService.renderErrorFormat(
                            response, [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code  : 'api.error.invalid.request',
                            args  : [filter.errors.allErrors.collect { it.toString() }.join("; ")]
                    ]
                    )
                }
                u.addToReportfilters(filter)
                saveuser = true
            } else if (query.existsFilterName) {
                filter = ReportFilter.findByNameAndUser(query.existsFilterName, u)
                if (filter) {
                    filter.properties = query.properties
//                    filter.fix()
                }
            }

            if (!filter.save(flush: true)) {
                flash.errors = filter.errors
//                params.saveFilter = true
                return apiService.renderErrorFormat(
                        response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.invalid.request',
                        args  : [filter.errors.allErrors.collect { it.toString() }.join("; ")]
                ]
                )
            }
            if (saveuser) {
                if (!u.save(flush: true)) {
                    return renderErrorView([beanErrors: filter.errors])
                }
            }

            render(contentType: 'application/json') {
                success true
                filterName query.newFilterName
            }
        }.invalidToken {
            return apiService.renderErrorFormat(
                    response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'request.error.invalidtoken.message',
            ]
            )
        }
    }

    def listFiltersAjax(String project) {
        if(!project){

            return apiService.renderErrorFormat(
                    response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.parameter.required',
                    args  : ['project']
            ]
            )
        }
        def User u = userService.findOrCreateUser(session.user)
        def filterset = u.reportfilters?.findAll { it.projFilter == project } ?: []

        render(contentType: 'application/json') {
            success true
            filters filterset*.toMap()
        }
    }


    def deleteFilter(){
        withForm{
            def User u = userService.findOrCreateUser(session.user)
            def filtername=params.delFilterName
            final def ffilter = ReportFilter.findByNameAndUser(filtername, u)
            if(ffilter){
                ffilter.delete(flush:true)
                flash.message="Filter deleted: ${filtername}"
            }
            redirect(controller: 'reports', action: 'index', params: [project: params.project])
        }.invalidToken {
            flash.error= g.message(code: 'request.error.invalidtoken.message')
            redirect(
                    controller: 'reports',
                    action: 'index',
                    params: [filterName: params.delFilterName, project: params.project]
            )
        }
    }

    def deleteFilterAjax(String project) {
        withForm {
            g.refreshFormTokensHeader()

            if(!params.delFilterName){

                return apiService.renderErrorFormat(
                        response, [
                        status: HttpServletResponse.SC_BAD_REQUEST,
                        code  : 'api.error.parameter.required',
                        args  : ['delFilterName']
                ]
                )
            }
            def User u = userService.findOrCreateUser(session.user)
            def filtername = params.delFilterName
            final def ffilter = ReportFilter.findByNameAndUserAndProjFilter(filtername, u, project)
            if (ffilter) {
                ffilter.delete(flush: true)
            }

            render(contentType: 'application/json') {
                success true
            }
        }.invalidToken {
            return apiService.renderErrorFormat(
                    response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'request.error.invalidtoken.message',
            ]
            )
        }
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


    /**
     * API, /api/14/project/PROJECT/history
     */
    def apiHistoryv14(ExecQuery query){
        if(!apiService.requireApi(request,response,ApiVersions.V14)){
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
        if (request.api_version < ApiVersions.V14 && !(response.format in ['all','xml'])) {
            return apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: [response.format]
            ])
        }
        withFormat{
            xml{
                return apiService.renderSuccessXml(request,response){
                    delegate.'events'(count:model.reports.size(),total:model.total, max: model.max, offset: model.offset){
                        model.reports.each{  rpt->
                            def nodes=rpt.node
                            final Matcher matcher = nodes =~ /^(\d+)\/(\d+)\/(\d+)$/
                            def nodesum=[rpt.status =='succeed'?1:0,rpt.status =='succeed'?0:1,1]
                            if(matcher.matches()){
                                nodesum[0]=matcher.group(1)
                                nodesum[1]=matcher.group(2)
                                nodesum[2]=matcher.group(3)
                            }
                            event(starttime:rpt.dateStarted.time,endtime:rpt.dateCompleted.time){
                                title(rpt.reportId?:'adhoc')
                                status(statusMap[rpt.status]?:ExecutionService.EXECUTION_STATE_OTHER)
                                statusString(rpt.status)
                                summary(rpt.adhocScript?:rpt.title)
                                delegate.'node-summary'(succeeded:nodesum[0],failed:nodesum[1],total:nodesum[2])
                                user(rpt.author)
                                project(rpt.ctxProject)
                                if(rpt.status=='cancel' && rpt.abortedByUser){
                                    abortedby(rpt.abortedByUser)
                                }
                                delegate.'date-started'(g.w3cDateValue(date:rpt.dateStarted))
                                delegate.'date-ended'(g.w3cDateValue(date:rpt.dateCompleted))
                                if(rpt.jcJobId){
                                    def foundjob=scheduledExecutionService.getByIDorUUID(rpt.jcJobId)
                                    def jparms=[id:foundjob?foundjob.extid:rpt.jcJobId]
                                    if(foundjob){
                                        jparms.href=apiService.apiHrefForJob(foundjob)
                                        jparms.permalink=apiService.guiHrefForJob(foundjob)
                                    }
                                    job(jparms)
                                }
                                if(rpt.jcExecId){
                                    def foundExec=Execution.get(rpt.jcExecId)
                                    def execparms=[id:rpt.jcExecId]
                                    if(foundExec){
                                        execparms.href=apiService.apiHrefForExecution(foundExec)
                                        execparms.permalink=apiService.guiHrefForExecution(foundExec)
                                    }
                                    execution(execparms)
                                }
                            }
                        }
                    }
                }

            }
            json{
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
                                project=(rpt.ctxProject)
                                if(rpt.status=='cancel' && rpt.abortedByUser){
                                    abortedby=(rpt.abortedByUser)
                                }
                                delegate.'date-started'=(g.w3cDateValue(date:rpt.dateStarted))
                                delegate.'date-ended'=(g.w3cDateValue(date:rpt.dateCompleted))
                                if(rpt.jcJobId){
                                    def foundjob=scheduledExecutionService.getByIDorUUID(rpt.jcJobId)
                                    if(foundjob){
                                        job = [
                                                id       : foundjob.extid,
                                                href     : apiService.apiHrefForJob(foundjob),
                                                permalink: apiService.guiHrefForJob(foundjob)
                                        ]
                                    }
                                }
                                if(rpt.jcExecId){
                                    def foundExec=Execution.get(rpt.jcExecId)
                                    if(foundExec) {
                                        execution = [
                                                id       : rpt.jcExecId,
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
        }
    }
}
