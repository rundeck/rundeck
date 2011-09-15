import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.core.common.Framework
import grails.converters.JSON
import groovy.xml.MarkupBuilder
import java.lang.management.ManagementFactory
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthConstants

class MenuController {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    ScheduledExecutionService scheduledExecutionService
    MenuService menuService
    RoleService roleService
    def quartzScheduler
    def list = {
        def results = index(params)
        render(view:"index",model:results)
    }

    def nowrunning={ QueueQuery query->
        //find currently running executions
        
        if(params['Clear']){
            query=null
        }
        if(null!=query && !params.find{ it.key.endsWith('Filter')
        }){
            query.recentFilter="1h"
            params.recentFilter="1h"
        }
        
        if(null!=query){
            query.configureFilter()
        }
        
        //find previous executions
        def model= executionService.queryQueue(query)
        //        System.err.println("nowrunning: "+model.nowrunning);
        model = executionService.finishQueueQuery(query,params,model)

        //include timestamp of last completed execution for the project
        Execution e=executionService.lastExecution(query.projFilter)
        model.lastExecId=e?.id
        
        User u = userService.findOrCreateUser(session.user)
        Map filterpref=[:] 
        if(u){
            filterpref= userService.parseKeyValuePref(u.filterPref)
        }
        model.boxfilters=filterpref
        return model
    }
    def queueList={QueueQuery query->
        def model= executionService.queryQueue(query)
        model = executionService.finishQueueQuery(query,params,model)
        response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs found: ${model.nowrunning.size()}")
        render(contentType:"text/xml",encoding:"UTF-8"){
            result{
                items(count:model.nowrunning.size()){
                    model.nowrunning.each{ Execution job ->
                        delegate.'item'{
                            id(job.id.toString())
                            name(job.toString())
                            url(g.createLink(controller:'execution',action:'follow',id:job.id))
                        }
                    }
                }
            }
        }
    }
    
    def nowrunningFragment = {QueueQuery query->
        def results = nowrunning(query)
        return results
    }
    def nowrunningData = {QueueQuery query->
        def results = nowrunning(query)
        //structure dataset for client-side event status processing
        render results.nowrunning as JSON
    }
    def queueFragment = {QueueQuery query->
        def results = nowrunning(query)
        return results
    }
    
    
    def index ={
        /**
        * redirect to configured start page, or default to Run page
         */
        def startpage='run'
        if(grailsApplication.config.gui.startpage){
            startpage=grailsApplication.config.gui.startpage
        }
        switch (startpage){
            case 'run':
                return redirect(controller:'framework',action:'nodes')
            case 'jobs':
                return redirect(controller:'menu',action:'jobs')
            case 'history':
                return redirect(controller:'reports',action:'index')
        }
        return redirect(controller:'framework',action:'nodes')
    }
    
    def workflows = {ScheduledExecutionQuery query ->
        return render(view:'jobs',model:jobs(query))
    }
    def jobs = {ScheduledExecutionQuery query ->
        
        def User u = userService.findOrCreateUser(session.user)
        if(params.size()<1 && !params.filterName && u ){
            Map filterpref = userService.parseKeyValuePref(u.filterPref)
            if(filterpref['workflows']){
                params.filterName=filterpref['workflows']
            }
        }
        
        def results = jobsFragment(query)
        
        withFormat{
            yaml{
                final def encoded = JobsYAMLCodec.encode(results.nextScheduled as List)
                render(text:encoded,contentType:"text/yaml",encoding:"UTF-8")
            }
            html{ results
            }
            xml{
                response.setHeader(Constants.X_RUNDECK_RESULT_HEADER,"Jobs found: ${results.nextScheduled?.size()}")
                def writer = new StringWriter()
                def xml = new MarkupBuilder(writer)
                JobsXMLCodec.encodeWithBuilder(results.nextScheduled,xml)
                writer.flush()
                render(text:writer.toString(),contentType:"text/xml",encoding:"UTF-8")
            }
        }
    }
    
    def workflowsFragment = {ScheduledExecutionQuery query ->
        return render(view:'jobsFragment',model:jobsFragment(query))
    }
    def jobsFragment = {ScheduledExecutionQuery query ->
        long start=System.currentTimeMillis()
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        FrameworkController.autosetSessionProject(session,framework)
        def usedFilter=null
        
        if(params.filterName){
            //load a named filter and create a query from it
            def User u = userService.findOrCreateUser(session.user)
            if(u){
                ScheduledExecutionFilter filter = ScheduledExecutionFilter.findByNameAndUser(params.filterName,u)
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
        if(params['Clear']){
            query=new ScheduledExecutionQuery()
            usedFilter=null
        }
        if(query && !query.projFilter && session.project){
            query.projFilter=session.project
        }
        def results=listWorkflows(query,framework,session.user)
        if(usedFilter){
            results.filterName=usedFilter
            results.paginateParams['filterName']=usedFilter
        }
        results.params=params
        log.debug("jobsFragment(tot): "+(System.currentTimeMillis()-start));
        return results
    }
    /**
     * Presents the jobs tree and can pass the jobsjscallback parameter
     * to the be a javascript callback for clicked jobs, instead of normal behavior.
     */
    def jobsPicker = {ScheduledExecutionQuery query ->

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects = frameworkService.projects(framework)
        def usedFilter=null
        if(!query){
            query = new ScheduledExecutionQuery()
        }
        if(query && !query.projFilter && session.project){
            query.projFilter=session.project
        }
        def results=listWorkflows(query,framework,session.user)
        if(usedFilter){
            results.filterName=usedFilter
            results.paginateParams['filterName']=usedFilter
        }
        results.params=params
        if(params.jobsjscallback){
            results.jobsjscallback=params.jobsjscallback
        }
        return results
    }
    def listWorkflows(ScheduledExecutionQuery query,Framework framework,String user) {
        long start=System.currentTimeMillis()
        def projects = frameworkService.projects(framework)
        if(null!=query){
            query.configureFilter()
        }
        def qres = scheduledExecutionService.listWorkflows(query)
        log.debug("service.listWorkflows: "+(System.currentTimeMillis()-start));
        long rest=System.currentTimeMillis()
        def schedlist=qres.schedlist
        def total=qres.total
        def filters=qres._filters
        
        def finishq=scheduledExecutionService.finishquery(query,params,qres)
        
        def nextExecutions=scheduledExecutionService.nextExecutionTimes(schedlist.findAll { it.scheduled })
        log.debug("listWorkflows(nextSched): "+(System.currentTimeMillis()-rest));
        long running=System.currentTimeMillis()
        
        //find currently running executions
        def runlist = Execution.findAllByDateCompletedIsNullAndScheduledExecutionIsNotNull()
        def nowrunning=[:]
        runlist.each{
            nowrunning[it.scheduledExecution.id.toString()]=it.id.toString()
        }
        log.debug("listWorkflows(running): "+(System.currentTimeMillis()-running));
        long preeval=System.currentTimeMillis()

        //collect all jobs and authorize the user for the set of available Job actions
        def jobnames=[:]
        Set res = new HashSet()
        schedlist.each{ ScheduledExecution sched->
            if(!jobnames[sched.generateFullName()]){
                jobnames[sched.generateFullName()]=[]
            }
            jobnames[sched.generateFullName()]<<sched.id.toString()
            res.add(["job": sched.jobName, "group": sched.groupPath?:''])
        }
        // Filter the groups by what the user is authorized to see.

        def authorization = frameworkService.getFrameworkFromUserSession(request.session, request).getAuthorizationMgr()
        def env = Collections.singleton(new Attribute(new URI(AuthConstants.PROJECT_URI), query.projFilter))
        def decisions = authorization.evaluate(res, request.subject, new HashSet([UserAuth.WF_CREATE,UserAuth.WF_READ,UserAuth.WF_DELETE,UserAuth.WF_RUN,UserAuth.WF_UPDATE,UserAuth.WF_KILL]), env)
//        def decisions = authorization.evaluate(res, request.subject, new HashSet([UserAuth.WF_READ]), Collections.emptySet())
        log.debug("listWorkflows(evaluate): "+(System.currentTimeMillis()-preeval));

        long viewable=System.currentTimeMillis()


        def Map jobauthorizations=[:]

        //produce map: [actionName:[id1,id2,...],actionName2:[...]] for all allowed actions for jobs
        decisions.findAll { it.authorized}.groupBy { it.action }.each{k,v->
            jobauthorizations[k] = new HashSet(v.collect {
                jobnames[ScheduledExecution.generateFullName(it.resource.group,it.resource.job)]
            }.flatten())
        }

        def authorizemap=[:]
        def pviewmap=[:]
        def newschedlist=[]
        def unauthcount=0

        /*
         'group/name' -> [ jobs...]
         */
        def jobgroups=[:]
        schedlist.each{ ScheduledExecution se->
            authorizemap[se.id.toString()]=jobauthorizations[UserAuth.WF_READ]?.contains(se.id.toString())
            if(authorizemap[se.id.toString()] || roleService.isUserInAnyRoles(request,['admin','job_view_unauthorized'])){
                newschedlist<<se
                if(!jobgroups[se.groupPath?:'']){
                    jobgroups[se.groupPath?:'']=[se]
                }else{
                    jobgroups[se.groupPath?:'']<<se
                }
            }
            if(!authorizemap[se.id.toString()]){
                log.debug("Unauthorized job: ${se}")
                unauthcount++
            }

        }

        if(org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.realJobTree != "false") {
            //Adding group entries for empty hierachies to have a "real" tree 
            def missinggroups = [:]
            jobgroups.each { k, v ->
                def splittedgroups = k.split('/')
                splittedgroups.eachWithIndex { item, idx ->
                    def thepath = splittedgroups[0..idx].join('/')
                    if(!jobgroups.containsKey(thepath)) {
                        missinggroups[thepath]=[]
                    }
                }
            }
            //sorting is done in the view
            jobgroups.putAll(missinggroups)
        }

        schedlist=newschedlist
        log.debug("listWorkflows(viewable): "+(System.currentTimeMillis()-viewable));
        long last=System.currentTimeMillis()

        log.debug("listWorkflows(last): "+(System.currentTimeMillis()-last));
        log.debug("listWorkflows(total): "+(System.currentTimeMillis()-start));

        return [
        projects:projects,
        nextScheduled:schedlist,
        nextExecutions: nextExecutions,
        jobauthorizations:jobauthorizations,
        authMap:authorizemap,
        nowrunning: nowrunning,
        jobgroups:jobgroups,
        paginateParams:finishq.paginateParams,
        displayParams:finishq.displayParams,
        total: total,
        max: finishq.max,
        offset:finishq.offset,
        unauthorizedcount:unauthcount
        ]
    }
    
    
    def storeJobfilter={ScheduledExecutionQuery query->
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def ScheduledExecutionFilter filter
        def boolean saveuser=false
        if(params.newFilterName && !params.existsFilterName){
            filter= ScheduledExecutionFilter.fromQuery(query)
            filter.name=params.newFilterName
            filter.user=u
            if(!filter.validate()){
                flash.error=filter.errors.allErrors.collect { g.message(error:it).encodeAsHTML()}.join("<br>")
                params.saveFilter=true
                return redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:params)
            }
            u.addToJobfilters(filter)
            saveuser=true
        }else if(params.existsFilterName){
            filter = ScheduledExecutionFilter.findByNameAndUser(params.existsFilterName,u)
            if(filter){
                filter.properties=query.properties
                filter.fix()
            }
        }else if(!params.newFilterName && !params.existsFilterName){
            flash.error="Filter name not specified"
            params.saveFilter=true
            return redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:params)
        }
        if(!filter.save(flush:true)){
            flash.error=filter.errors.allErrors.collect { g.message(error:it)
            }.join("<br>")
            params.saveFilter=true
            return redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:params)
        }
        if(saveuser){
            if(!u.save(flush:true)){
                //                u.errors.allErrors.each { log.error(g.message(error:it)) }
                //                flash.error="Unable to save filter for user"
                flash.error=filter.errors.allErrors.collect { g.message(error:it).encodeAsHTML()}.join("\n")
                return render(template:"/common/error")
            }
        }
        redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:[filterName:filter.name,compact:params.compact?'true':''])
    }
    
    
    def deleteJobfilter={
        def User u = userService.findOrCreateUser(session.user)
        if(!u){
            log.error("Couldn't find user: ${session.user}")
            flash.error="Couldn't find user: ${session.user}"
            return render(template:"/common/error")
        }
        def filtername=params.delFilterName
        final def ffilter = ScheduledExecutionFilter.findByNameAndUser(filtername, u)
        if(ffilter){
            ffilter.delete(flush:true)
            flash.message="Filter deleted: ${filtername.encodeAsHTML()}"
        }
        redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:[compact:params.compact?'true':''])
    }

    def admin={

        if (!roleService.isUserInAnyRoles(request, ['admin', 'user_admin'])) {
            flash.error = "User Admin role required"
            flash.title = "Unauthorized"
            response.setStatus(403)
            render(template: '/common/error', model: [:])
        }
    }

    def systemInfo = {
        if (!roleService.isUserInAnyRoles(request, ['admin', 'user_admin'])) {
            flash.error = "User Admin role required"
            flash.title = "Unauthorized"
            response.setStatus(403)
            render(template: '/common/error', model: [:])
        }
        Date nowDate = new Date();
        String nodeName = servletContext.getAttribute("FRAMEWORK_NODE")
        String appVersion = grailsApplication.metadata['app.version']
        double load = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        int processorsCount = ManagementFactory.getOperatingSystemMXBean().availableProcessors
        String osName = ManagementFactory.getOperatingSystemMXBean().name
        String osVersion = ManagementFactory.getOperatingSystemMXBean().version
        String osArch = ManagementFactory.getOperatingSystemMXBean().arch
        String vmName = ManagementFactory.getRuntimeMXBean().vmName
        String vmVendor = ManagementFactory.getRuntimeMXBean().vmVendor
        String vmVersion = ManagementFactory.getRuntimeMXBean().vmVersion
        long durationTime = ManagementFactory.getRuntimeMXBean().uptime
        Date startupDate = new Date(nowDate.getTime() - durationTime)
        int threadActiveCount = Thread.activeCount()
        def build = grailsApplication.metadata['build.ident']
        def base = servletContext.getAttribute("RDECK_BASE")

        def memmax = Runtime.getRuntime().maxMemory()
        def memfree = Runtime.getRuntime().freeMemory()
        def memtotal = Runtime.getRuntime().totalMemory()
        def schedulerRunningCount = quartzScheduler.getCurrentlyExecutingJobs().size()
        def info = [
            nowDate: nowDate,
            nodeName: nodeName,
            appVersion: appVersion,
            load: load,
            processorsCount: processorsCount,
            osName: osName,
            osVersion: osVersion,
            osArch: osArch,
            vmName: vmName,
            vmVendor: vmVendor,
            vmVersion: vmVersion,
            durationTime: durationTime,
            startupDate: startupDate,
            threadActiveCount: threadActiveCount,
            build: build,
            base: base,
            memmax: memmax,
            memfree: memfree,
            memtotal: memtotal,
            schedulerRunningCount: schedulerRunningCount
        ]
        return [systemInfo: [

            [
                "stats: uptime": [
                    duration: info.durationTime,
                    'duration.unit': 'ms',
                    datetime: g.w3cDateValue(date: info.startupDate)
                ]],

            ["stats: cpu": [

                loadAverage: info.load,
                'loadAverage.unit': '%',
                processors: info.processorsCount
            ]],
            ["stats: memory":
            [
                'max.unit': 'byte',
                'free.unit': 'byte',
                'total.unit': 'byte',
                max: info.memmax,
                free: info.memfree,
                total: info.memtotal,
                used: info.memtotal-info.memfree,
                'used.unit': 'byte',
                heapusage: (info.memtotal-info.memfree)/info.memtotal,
                'heapusage.unit': 'ratio',
                'heapusage.info': 'Ratio of Used to Free memory within the Heap (used/total)',
                allocated: (info.memtotal)/info.memmax,
                'allocated.unit': 'ratio',
                'allocated.info': 'Ratio of system memory allocated to maximum allowed (total/max)',
            ]],
            ["stats: scheduler":
            [running: info.schedulerRunningCount]
            ],
            ["stats: threads":
            [active: info.threadActiveCount]
            ],
            [timestamp: [
//                epoch: info.nowDate.getTime(), 'epoch.unit': 'ms',
                datetime: g.w3cDateValue(date: info.nowDate)
            ]],

            [rundeck:
            [version: info.appVersion,
                build: info.build,
                node: info.nodeName,
                base: info.base,
            ]],
            [os:
            [arch: info.osArch,
                name: info.osName,
                version: info.osVersion,
            ]
            ],
            [jvm:
            [
                name: info.vmName,
                vendor: info.vmVendor,
                version: info.vmVersion
            ]
            ],
        ]]
    }



    /**
    * API Actions
     */

    /**
     * API: /api/jobs, version 1
     */
    def apiJobsList = {ScheduledExecutionQuery query ->
        if(params.project){
            query.projFilter=params.project
        }else{
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
        if(query.groupPathExact || query.jobExactFilter){
            //these query inputs require API version 2
            if (!new ApiController().requireVersion(ApiRequestFilters.V2)) {
                return
            }
        }
        def results = jobsFragment(query)

        withFormat{
            xml{
                new ApiController().success{ delegate->
                    delegate.'jobs'(count:results.nextScheduled.size()){
                        results.nextScheduled.each{ ScheduledExecution se->
                            job(id:se.extid){
                                name(se.jobName)
                                group(se.groupPath)
                                project(se.project)
                                description(se.description)
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * API: /api/2/project/NAME/jobs, version 2
     */
    def apiJobsListv2 = {ScheduledExecutionQuery query ->
        if(!new ApiController().requireVersion(ApiRequestFilters.V2)){
            return
        }
        return apiJobsList(query)
    }

    /**
     * API: /jobs/export, version 1
     */
    def apiJobsExport = {ScheduledExecutionQuery query ->

        if(params.project){
            query.projFilter=params.project
        }else{
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
        def results = jobsFragment(query)

        withFormat{
            xml{
                def writer = new StringWriter()
                def xml = new MarkupBuilder(writer)
                JobsXMLCodec.encodeWithBuilder(results.nextScheduled,xml)
                writer.flush()
                render(text:writer.toString(),contentType:"text/xml",encoding:"UTF-8")
            }
            yaml{
                final def encoded = JobsYAMLCodec.encode(results.nextScheduled as List)
                render(text:encoded,contentType:"text/yaml",encoding:"UTF-8")
            }
        }
    }

    /**
     * API: /executions/running, version 1
     */
    def apiExecutionsRunning = {

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

        QueueQuery query = new QueueQuery(runningFilter:'running',projFilter:params.project)
        def results = nowrunning(query)
        return new ExecutionController().renderApiExecutionListResultXML(results.nowrunning)
    }
}

