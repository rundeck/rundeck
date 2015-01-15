package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.support.StoreFilterCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ApiService
import rundeck.services.ExecutionService

import javax.servlet.http.HttpServletResponse
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ReportQuery
import rundeck.User
import rundeck.ReportFilter
import rundeck.services.FrameworkService
import rundeck.filters.ApiRequestFilters

class ReportsController extends ControllerBase{
    def reportService
    def userService
    def FrameworkService frameworkService
    def scheduledExecutionService
    def ApiService apiService
    static allowedMethods = [
            deleteFilter:'POST',
            storeFilter:'POST'
    ]

    public def index (ExecQuery query) {
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
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if(unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext, AuthorizationUtil
                .resourceType('event'), [AuthConstants.ACTION_READ],
                params.project), AuthConstants.ACTION_READ,'Events in project',params.project)){
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

        if(null!=query){
            query.configureFilter()
        }
        def curdate=new Date()
        def model= reportService.getExecutionReports(query,true)
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

    def since = { ExecQuery query->
       //find previous executions
        def usedFilter
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext, AuthorizationUtil
                .resourceType('event'), [AuthConstants.ACTION_READ],
                params.project), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
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
                render(contentType:"text/json"){
                    if(errmsg){
                        delegate.error={
                            delegate.message= flash.error
                        }
                    }else{
                        delegate.since={
                            delegate.count=count
                            delegate.time= time
                        }
                    }
                }
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
    def eventsFragment={ ExecQuery query ->
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext, AuthorizationUtil
                .resourceType('event'), [AuthConstants.ACTION_READ],
                params.project), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }
        def results = index(query)
        results.params=params
        return results
    }
    def eventsAjax={ ExecQuery query ->
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)


        if (unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext, AuthorizationUtil
                .resourceType('event'), [AuthConstants.ACTION_READ],
                params.project), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }

        if (query.hasErrors()) {
            response.status=400
            log.error("query errors: "+(query.errors.allErrors.collect{it.toString()}.join(", ")))
            return render(contentType: 'application/json', text:  [errors: query.errors] as JSON)
        }
        def results = index(query)
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
            if(map.jcJobId){
                map.jobId= map.remove('jcJobId')
                try {
                    map.jobId=ScheduledExecution.get(Long.parseLong(map.jobId))?.extid
                }catch(Exception e){

                }
                if(map.execution.argString){
                    map.execution.jobArguments=FrameworkService.parseOptsFromString(map.execution.argString)
                }
            }
            map.user= map.remove('author')
            map.jobName= map.remove('reportId')
            map.executionString= map.remove('title')
            return map
        }
        results.params=params
        render(contentType: 'application/json', text: results as JSON)
    }
    def jobsFragment={ ExecQuery query ->
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(frameworkService.authorizeProjectResourceAll(authContext, AuthorizationUtil
                .resourceType('event'), [AuthConstants.ACTION_READ],
                params.project), AuthConstants.ACTION_READ, 'Events for project', params.project)) {
            return
        }
        def results = jobs(query)
        results.params=params
        render(view:'eventsFragment',model:results)
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
            flash.error=filter.errors.allErrors.collect { g.message(error:it) }.join("\n")
            params.saveFilter=true
            chain(controller:'reports',action:'index',params:params)
        }
        if(saveuser){
            if(!u.save(flush:true)){
                return renderErrorView(u.errors.allErrors.collect { g.message(error: it) }.join("\n"))
            }
        }
        redirect(controller:'reports',action:params.fragment?'eventsFragment':'index',params:[filterName:filter.name,project:params.project])
        }.invalidToken {
            flash.error=g.message(code:'request.error.invalidtoken.message')
            redirect(controller: 'reports', action: params.fragment ? 'eventsFragment' : 'index', params: [project: params.project])
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
            redirect(controller:'reports',action:params.fragment?'eventsFragment':'index',params:[project:params.project])
        }.invalidToken {
            flash.error= g.message(code: 'request.error.invalidtoken.message')
            redirect(controller: 'reports', action: params.fragment ? 'eventsFragment' : 'index', params: [filterName: params.delFilterName,project: params.project])
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
     * API, /api/history, version 1
     */
    def apiHistory={ExecQuery query->
        if (!apiService.requireApi(request, response)) {
            return
        }
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        if(params.jobListFilter || params.excludeJobListFilter){
            if (!apiService.requireVersion(request,response,ApiRequestFilters.V5)) {
                return
            }
        }
        //test valid project
        Framework framework = frameworkService.getRundeckFramework()

        def exists=frameworkService.existsFrameworkProject(params.project)
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if(!exists){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])

        }
        if (!frameworkService.authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_TYPE_EVENT,
                [AuthConstants.ACTION_READ], params.project)) {
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_FORBIDDEN,
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
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.history.date-format', args: ['begin', params.begin]])

            }
        }
        if(params.end){
            try{
                query.endbeforeFilter=parseDate(params.end)
                query.doendbeforeFilter=true
            }catch(ParseException e){
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                        code: 'api.error.history.date-format', args: ['end', params.end]])
            }
        }

        if(null!=query){
            query.configureFilter()
        }
        def model=reportService.getExecutionReports(query,true)
        model = reportService.finishquery(query,params,model)

        def statusMap=[succeed:ExecutionService.EXECUTION_SUCCEEDED,
            cancel: ExecutionService.EXECUTION_ABORTED,
            fail: ExecutionService.EXECUTION_FAILED]
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
                        status(statusMap[rpt.status]?:rpt.status)
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
                            job(id:foundjob?foundjob.extid:rpt.jcJobId)
                        }
                        if(rpt.jcExecId){
                            execution(id:rpt.jcExecId)
                        }
                    }
                }
            }
        }
    }
}
