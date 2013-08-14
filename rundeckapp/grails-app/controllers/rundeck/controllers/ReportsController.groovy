package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.common.Framework
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.app.support.ReportQuery
import rundeck.User
import rundeck.ReportFilter
import rundeck.services.FrameworkService
import rundeck.filters.ApiRequestFilters

class ReportsController {
    def reportService
    def userService
    def FrameworkService frameworkService
    def scheduledExecutionService

    private unauthorized(String action, boolean fragment = false) {
        if (!fragment) {
            response.setStatus(403)
        }
        flash.title = "Unauthorized"
        flash.error = "${request.remoteUser} is not authorized to: ${action}"
        response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, flash.error)
        render(template: fragment ? '/common/errorFragment' : '/common/error', model: [:])
    }
    def index = { ExecQuery query->
       //find previous executions
        def usedFilter
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}")
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
        if(query && !query.projFilter && session.project){
            query.projFilter = session.project
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}")
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
        if(query && !query.projFilter && session.project){
            query.projFilter = session.project
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
                flash.error="Invalid date: ${errmsg}"
            }
        }

        def curdate=new Date()


        def count= reportService.countExecutionReports(query)
//        System.err.println("checking query for reports from: "+(curdate.time-time) + " objFilter: "+query.objFilter+", result: "+count);

        withFormat{
            html{
                if(errmsg){
                    return render(template:"/common/error")
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
                            deelegate.count=count
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
    def clearFragment={ ReportQuery query ->
        params['Clear']='clear'
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}", true)
        }
        def results = index(query)
        results.params=params
        render(view:'eventsFragment',model:results)
    }
    def eventsFragment={ ExecQuery query ->
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}",true)
        }
        def results = index(query)
        results.params=params
        return results
    }
    def jobsFragment={ ExecQuery query ->
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}", true)
        }
        def results = jobs(query)
        results.params=params
        render(view:'eventsFragment',model:results)
    }
    def timelineFragment={ ReportQuery query ->
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}", true)
        }
        def results = index(query)
        render(view:'eventsFragment',model:results)
    }

    def storeFilter={ReportQuery query->
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
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
//                u.errors.allErrors.each { log.error(g.message(error:it)) }
//                flash.error="Unable to save filter for user"
                flash.error=u.errors.allErrors.collect { g.message(error:it) }.join("\n")
                return render(template:"/common/error")
            }
        }
        redirect(controller:'reports',action:params.fragment?'eventsFragment':'index',params:[filterName:filter.name])
    }

    def deleteFilter={
         def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def filtername=params.delFilterName
        final def ffilter = ReportFilter.findByNameAndUser(filtername, u)
        if(ffilter){
            ffilter.delete(flush:true)
            flash.message="Filter deleted: ${filtername}"
        }
        redirect(controller:'reports',action:params.fragment?'eventsFragment':'index')
    }
   
    def commands = {ExecQuery query ->
        //find previous executions
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}")
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
            query=null
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
        }

        if(null!=query){
            query.configureFilter()
        }
        def model= reportService.getExecutionReports(query,false)
        model = reportService.finishquery(query,params,model)
        return model
    }

    def jobs = {ExecQuery query ->
        //find previous executions
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)

        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            session.project)) {
            return unauthorized("Read Events for project ${session.project}")
        }
        def options = [:]
        if (params['jobRptCustomView']) {
            params.each {String k, v ->
                def m = k =~ /^(.*)Show$/
                if (m.matches() && (v == 'true' || v == 'on')) {
                    log.info("saw view option: ${m.group(1)}")
                    options[m.group(1)] = true
                }
            }
            session['job_reports_options'] = options
        }

        if(params['Clear']){
            query=null
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            //no default filter
        }
        if(null!=query){
            query.configureFilter()
        }
        def model= reportService.getExecutionReports(query,true)
        model = reportService.finishquery(query,params,model)
        return model
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
        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        if(params.jobListFilter || params.excludeJobListFilter){
            if (!new ApiController().requireVersion(ApiRequestFilters.V5)) {
                return
            }
        }
        //test valid project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)

        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
        }
        if (!frameworkService.authorizeProjectResourceAll(framework, [type: 'resource', kind: 'event'], ['read'],
            params.project)) {
            flash.error = g.message(code: 'api.error.item.unauthorized', args: ['Read Events', 'Project',params.project])
            return chain(controller: 'api', action: 'error')
        }
        params.projFilter=params.project
        query.projFilter = params.project


        //attempt to parse/bind "end" and "begin" parameters
        if(params.begin){
            try{
                query.endafterFilter=parseDate(params.begin)
                query.doendafterFilter=true
            }catch(ParseException e){
                flash.error=g.message(code:'api.error.history.date-format',args:['begin',params.begin])
                return chain(controller:'api',action:'error')
            }
        }
        if(params.end){
            try{
                query.endbeforeFilter=parseDate(params.end)
                query.doendbeforeFilter=true
            }catch(ParseException e){
                flash.error=g.message(code:'api.error.history.date-format',args:['end',params.end])
                return chain(controller:'api',action:'error')
            }
        }

        if(null!=query){
            query.configureFilter()
        }
        def model=reportService.getExecutionReports(query,true)
        model = reportService.finishquery(query,params,model)

        def statusMap=[succeed:ExecutionController.EXECUTION_SUCCEEDED,
            cancel:ExecutionController.EXECUTION_ABORTED,
            fail:ExecutionController.EXECUTION_FAILED]
        return new ApiController().success{ delegate->
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
