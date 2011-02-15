import org.springframework.web.servlet.ModelAndView
import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.common.Framework
import java.util.regex.Matcher
import java.text.SimpleDateFormat
import java.text.ParseException

class ReportsController {
    def reportService
    def userService
    def frameworkService

    def index = { ReportQuery query->
       //find previous executions
        def usedFilter
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        FrameworkController.autosetSessionProject(session,framework)

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
            query=new ReportQuery()
            query.recentFilter="1d"
            params.recentFilter="1d"
            usedFilter=null
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')}){
            query.recentFilter="1d"
            params.recentFilter="1d"
        }
        if(query && !query.projFilter && session.project){
            query.projFilter = session.project
        }

        if(null!=query){
            query.configureFilter()
        }
        def curdate=new Date()
        def model= reportService.getCombinedReports(query)
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

    def since = { ReportQuery query->
       //find previous executions
        def usedFilter

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
            query.recentFilter="1d"
            params.recentFilter="1d"
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


        def count= reportService.countCombinedReports(query)
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
                        delegate.'error'(message:flash.error)
                    }else{
                        delegate.'since'('count':count,'time' :time)
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
        def results = index(query)
        results.params=params
        render(view:'eventsFragment',model:results)
    }
    def eventsFragment={ ReportQuery query ->
        def results = index(query)
        results.params=params
        return results
    }
    def jobsFragment={ ExecQuery query ->
        def results = jobs(query)
        results.params=params
        render(view:'eventsFragment',model:results)
    }
    def timelineFragment={ ReportQuery query ->
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
            query.recentFilter="1d"
            params.recentFilter="1d"
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
            query.recentFilter="1d"
            params.recentFilter="1d"
        }
        if(null!=query){
            query.configureFilter()
        }
        def model= reportService.getExecutionReports(query,true)
        model = reportService.finishquery(query,params,model)
        return model
    }



    def query = {ApiQuery query ->
        if (request.getParameterMap().containsKey('action')) {
            query.actionStr = request.getParameter('action')
        }
        if (request.getParameterMap().containsKey('controller')) {
            query.controllerStr = request.getParameter('controller')
        }
        def result
        if (query.hasErrors()) {
            def StringBuffer sb = new StringBuffer()
            query.errors.allErrors.each{ it ->
//                sb << it.toString()
                sb << g.message(error:it)
            }
            result = ['error': sb.toString()]
        } else {
            result = reportService.getApiQuery(query)
        }

        if (result.error) {
            render(contentType: "text/xml", encoding: "UTF-8") {
                'reports-query-result'(error: "true") {
                    errors() {
                        error(result.error)
                    }
                }
            }
            return
        }

        def xmlclos = {
            'reports-query-result'(matchTotal: result.total) {
                reports(count: result.reports.size()) {
                    result.reports.each() {res ->
                        'report-entry'(itemType: (res instanceof ExecReport ? 'commandExec' :""),
                            actionType: (res instanceof ExecReport ? res.status :  "succeed")) {
                            date(time: res.dateCompleted.getTime(), res.dateCompleted.toString())
                            author(res.author)
                            project(res.ctxProject)
                            itemType(res instanceof ExecReport ? 'commandExec' : "")
                            resourceType(res.ctxType)
                            resourceName(res.ctxName)
                            controller(res.ctxType)
                            if (res instanceof ExecReport) {
                                commandName(res.ctxCommand)
                            }
                            maprefUri(res.maprefUri)
                            if (res instanceof ExecReport) {
                                nodename(res.node)
                            }
                            reportId(res.reportId)
                            actionType(res.status)
                            action(res.title)
                            message(res.message)
                        }

                    }
                }
            }
        }

        def jsonclos = {
            matchTotal( result.total)
            count(result.reports.size())
                reports() {
                    result.reports.each() {res ->
                        reports(time: res.dateCompleted.getTime(), date:res.dateCompleted.toString(),
                            author:res.author,
                            project:res.ctxProject,
                            itemType:(res instanceof ExecReport ? 'commandExec' : ""),
                            resourceType:res.ctxType,
                            resourceName:res.ctxName,
                            controller:res.ctxType,
                            commandName:((res instanceof ExecReport)?res.ctxCommand:''),
                            maprefUri:res.maprefUri,
                            nodename:((res instanceof ExecReport)?res.node:''),
                            reportId:res.reportId,
                            actionType:res.actionType,
                            action:res.title,
                            message:res.message
                        )

                    }

            }
        }

        //change closure resolvestrategy so that the 'message' call doesn't cause StackOverflowError
        xmlclos.resolveStrategy = Closure.DELEGATE_FIRST
        switch (query.format) {
            case 'json':
                render(contentType: "text/json",  encoding: "UTF-8",jsonclos)
                break
//            case 'yaml':
//                render(contentType: "text/yaml",  encoding: "UTF-8",jsonclos)
//                break
            case 'xml':
            default:
                render(contentType: "text/xml", encoding: "UTF-8", xmlclos)
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
    def apiHistory={ReportQuery query->
        if(!params.project){
            flash.error=g.message(code:'api.error.parameter.required',args:['project'])
            return chain(controller:'api',action:'error')
        }
        //test valid project
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)

        def exists=frameworkService.existsFrameworkProject(params.project,framework)
        if(!exists){
            flash.error=g.message(code:'api.error.item.doesnotexist',args:['project',params.project])
            return chain(controller:'api',action:'error')
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
        def model=reportService.getCombinedReports(query)
        model = reportService.finishquery(query,params,model)

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
                        summary(rpt.adhocScript?:rpt.title)
                        delegate.'node-summary'(succeeded:nodesum[0],failed:nodesum[1],total:nodesum[2])
                        user(rpt.author)
                        project(rpt.ctxProject)
                        delegate.'date-started'(g.w3cDateValue(date:rpt.dateStarted))
                        delegate.'date-ended'(g.w3cDateValue(date:rpt.dateCompleted))
                        if(rpt.jcJobId){
                            job(id:rpt.jcJobId)
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
