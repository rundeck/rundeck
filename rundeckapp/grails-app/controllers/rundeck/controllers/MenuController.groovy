package rundeck.controllers

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.support.QueueQuery
import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.app.support.StoreFilterCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.services.StorageConverterPluginProviderService
import com.dtolabs.rundeck.server.plugins.services.StoragePluginProviderService
import grails.converters.JSON
import groovy.xml.MarkupBuilder
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionFilter
import rundeck.User
import rundeck.codecs.JobsXMLCodec
import rundeck.codecs.JobsYAMLCodec
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService
import rundeck.services.LogFileStorageService
import rundeck.services.LoggingService
import rundeck.services.NotificationService
import rundeck.services.PluginService
import rundeck.services.ScheduledExecutionService
import rundeck.services.UserService

import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

class MenuController extends ControllerBase{
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    ScheduledExecutionService scheduledExecutionService
    NotificationService notificationService
    LoggingService LoggingService
    LogFileStorageService logFileStorageService
    StoragePluginProviderService storagePluginProviderService
    StorageConverterPluginProviderService storageConverterPluginProviderService
    PluginService pluginService
    def quartzScheduler
    def ApiService apiService
    static allowedMethods = [
            deleteJobfilter:'POST',
            storeJobfilter:'POST',
    ]
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
        if(!query.projFilter && params.project){
            query.projFilter=params.project
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
    def nowrunningAjax = {QueueQuery query->
        def results = nowrunning(query)
        //structure dataset for client-side event status processing
        def running= results.nowrunning.collect {
            def map = it.toMap()
            def data = [
                    status: ExecutionService.getExecutionState(it),
                    executionHref: createLink(controller: 'execution', action: 'show', absolute: true, id: it.id),
                    executionId: it.id,
                    duration: (it.dateCompleted?:new Date()).time - it.dateStarted.time
            ]
            if(!it.scheduledExecution){
                data['executionString']=map.workflow.commands[0].exec
            }else{
                data['jobName']=it.scheduledExecution.jobName
                if (it.scheduledExecution && it.scheduledExecution.totalTime >= 0 && it.scheduledExecution.execCount > 0) {
                    data['jobAverageDuration']= Math.floor(it.scheduledExecution.totalTime / it.scheduledExecution.execCount)
                }
                if (it.argString) {
                    data.jobArguments = FrameworkService.parseOptsFromString(it.argString)
                }
            }
            map + data
        }
        render( ([nowrunning:running] + results.subMap(['total','max','offset'])) as JSON)
    }
    def queueFragment = {QueueQuery query->
        def results = nowrunning(query)
        return results
    }
    
    
    def index ={
        /**
        * redirect to configured start page, or default to Run page
         */
        def startpage='home'
        if(grailsApplication.config.rundeck.gui.startpage){
            startpage=grailsApplication.config.rundeck.gui.startpage
        }
        if(params.page){
            startpage=params.page
        }
        if(!params.project){
            startpage='home'
        }else if (params.project && startpage == 'home') {
            startpage = 'jobs'
        }
        switch (startpage){
            case 'home':
                return redirect(controller: 'menu', action: 'home')
            case 'nodes':
                return redirect(controller:'framework',action:'nodes',params:[project:params.project])
            case 'run':
            case 'adhoc':
                return redirect(controller:'framework',action:'adhoc',params:[project:params.project])
            case 'jobs':
                return redirect(controller:'menu',action:'jobs', params: [project: params.project])
            case 'createJob':
                return redirect(controller:'scheduledExecution',action: 'create', params: [project: params.project])
            case 'uploadJob':
                return redirect(controller: 'scheduledExecution', action: 'upload', params: [project: params.project])
            case 'configure':
                return redirect(controller: 'menu', action: 'admin', params: [project: params.project])
            case 'history':
            case 'activity':
            case 'events':
                return redirect(controller:'reports',action:'index', params: [project: params.project])
        }
        return redirect(controller:'framework',action:'nodes', params: [project: params.project])
    }
    
    def jobs = {ScheduledExecutionQuery query ->
        
        def User u = userService.findOrCreateUser(session.user)
        if(params.size()<1 && !params.filterName && u ){
            Map filterpref = userService.parseKeyValuePref(u.filterPref)
            if(filterpref['workflows']){
                params.filterName=filterpref['workflows']
            }
        }
        if(!params.project){
            return redirect(controller: 'menu',action: 'home')
        }
        
        def results = jobsFragment(query)
        results.execQueryParams=query.asExecQueryParams()
        results.reportQueryParams=query.asReportQueryParams()

        withFormat{
            html {
                results
            }
            yaml{
                final def encoded = JobsYAMLCodec.encode(results.nextScheduled as List)
                render(text:encoded,contentType:"text/yaml",encoding:"UTF-8")
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
    
    def jobsFragment = {ScheduledExecutionQuery query ->
        long start=System.currentTimeMillis()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
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
        if(query && !query.projFilter && params.project){
            query.projFilter= params.project
        }
        def results=listWorkflows(query,authContext,session.user)
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

        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        def usedFilter=null
        if(!query){
            query = new ScheduledExecutionQuery()
        }
        if(query && !query.projFilter && params.project){
            query.projFilter= params.project
        }
        def results=listWorkflows(query,authContext,session.user)
        if(usedFilter){
            results.filterName=usedFilter
            results.paginateParams['filterName']=usedFilter
        }
        results.params=params
        if(params.jobsjscallback){
            results.jobsjscallback=params.jobsjscallback
        }
        return results + [runAuthRequired:params.runAuthRequired]
    }
    private def listWorkflows(ScheduledExecutionQuery query,AuthContext authContext,String user) {
        long start=System.currentTimeMillis()
        def projects = frameworkService.projects(authContext)
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

        def allScheduled = schedlist.findAll { it.scheduled }
        def nextExecutions=scheduledExecutionService.nextExecutionTimes(allScheduled)
        def clusterMap=scheduledExecutionService.clusterScheduledJobs(allScheduled)
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
            res.add(frameworkService.authResourceForJob(sched))
        }
        // Filter the groups by what the user is authorized to see.

        def decisions = frameworkService.authorizeProjectResources(authContext,res, new HashSet([AuthConstants.ACTION_READ, AuthConstants.ACTION_DELETE, AuthConstants.ACTION_RUN, AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_KILL]),query.projFilter)
        log.debug("listWorkflows(evaluate): "+(System.currentTimeMillis()-preeval));

        long viewable=System.currentTimeMillis()

        def authCreate = frameworkService.authorizeProjectResource(authContext,
                AuthConstants.RESOURCE_TYPE_JOB,
                AuthConstants.ACTION_CREATE, query.projFilter)
        

        def Map jobauthorizations=[:]

        //produce map: [actionName:[id1,id2,...],actionName2:[...]] for all allowed actions for jobs
        decisions.findAll { it.authorized}.groupBy { it.action }.each{k,v->
            jobauthorizations[k] = new HashSet(v.collect {
                jobnames[ScheduledExecution.generateFullName(it.resource.group,it.resource.name)]
            }.flatten())
        }

        jobauthorizations[AuthConstants.ACTION_CREATE]=authCreate
        def authorizemap=[:]
        def pviewmap=[:]
        def newschedlist=[]
        def unauthcount=0
        def readauthcount=0

        /*
         'group/name' -> [ jobs...]
         */
        def jobgroups=[:]
        schedlist.each{ ScheduledExecution se->
            authorizemap[se.id.toString()]=jobauthorizations[AuthConstants.ACTION_READ]?.contains(se.id.toString())
            if(authorizemap[se.id.toString()]){
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
        readauthcount= newschedlist.size()

        if(grailsApplication.config.rundeck?.gui?.realJobTree != "false") {
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
                clusterMap: clusterMap,
        jobauthorizations:jobauthorizations,
        authMap:authorizemap,
        nowrunning: nowrunning,
        jobgroups:jobgroups,
        paginateParams:finishq.paginateParams,
        displayParams:finishq.displayParams,
        total: total,
        max: finishq.max,
        offset:finishq.offset,
        unauthorizedcount:unauthcount,
        totalauthorized: readauthcount,
        ]
    }
    
    
    def storeJobfilter(ScheduledExecutionQuery query, StoreFilterCommand storeFilterCommand){
        withForm{
        if (storeFilterCommand.hasErrors()) {
            flash.errors = storeFilterCommand.errors
            params.saveFilter = true
            return redirect(controller: 'menu', action: params.fragment ? 'jobsFragment' : 'jobs',
                    params: params.subMap(['newFilterName', 'existsFilterName', 'project', 'saveFilter']))
        }

        def User u = userService.findOrCreateUser(session.user)
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
                return renderErrorView(filter.errors.allErrors.collect { g.message(error: it).encodeAsHTML() }.join("\n"))
            }
        }
        redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:[filterName:filter.name,project:params.project])
        }.invalidToken {
            renderErrorView(g.message(code:'request.error.invalidtoken.message'))
        }
    }
    
    
    def deleteJobfilter={
        withForm{
        def User u = userService.findOrCreateUser(session.user)
        def filtername=params.delFilterName
        final def ffilter = ScheduledExecutionFilter.findByNameAndUser(filtername, u)
        if(ffilter){
            ffilter.delete(flush:true)
            flash.message="Filter deleted: ${filtername.encodeAsHTML()}"
        }
        redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:[project: params.project])
        }.invalidToken{
            renderErrorView(g.message(code:'request.error.invalidtoken.message'))
        }
    }

    def storage={
//        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
//        if (unauthorizedResponse(
//                frameworkService.authorizeApplicationResourceAny(authContext,
//                        frameworkService.authResourceForProject(params.project),
//                        [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT,
//                                AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_DELETE]),
//                AuthConstants.ACTION_ADMIN, 'Project', params.project)) {
//            return
//        }

    }
    def admin={
        Framework framework = frameworkService.getRundeckFramework()
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (!params.project) {
            return renderErrorView('Project parameter is required')
        }
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                        frameworkService.authResourceForProject(params.project),
                        [AuthConstants.ACTION_CONFIGURE,AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT,
                                AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_DELETE]),
                AuthConstants.ACTION_ADMIN, 'Project', params.project)) {
            return
        }

            def project= params.project
            def fproject = frameworkService.getFrameworkProject(project)
            def configs = fproject.listResourceModelConfigurations()

            final service = framework.getResourceModelSourceService()
            final descriptions = service.listDescriptions()
            final nodeexecdescriptions = framework.getNodeExecutorService().listDescriptions()
            final filecopydescs = framework.getFileCopierService().listDescriptions()


            final defaultNodeExec = fproject.hasProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(NodeExecutorService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null
            final defaultFileCopy = fproject.hasProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) ? fproject.getProperty(FileCopierService.SERVICE_DEFAULT_PROVIDER_PROPERTY) : null
            //load config for node exec
            def nodeexec = [:]
            if (defaultNodeExec) {
                nodeexec.type= defaultNodeExec
                try {
                    final executor = framework.getNodeExecutorService().providerOfType(defaultNodeExec)
                    final desc = executor.description
                    nodeexec.config = Validator.demapProperties(fproject.getProperties(), desc)
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                }
            }
            //load config for file copy
            def fcopy = [:]
            if (defaultFileCopy) {
                fcopy.type=defaultFileCopy
                try {
                    final executor = framework.getFileCopierService().providerOfType(defaultFileCopy)
                    final desc = executor.description
                    fcopy.config = Validator.demapProperties(fproject.getProperties(), desc)
                } catch (ExecutionServiceException e) {
                    log.error(e.message)
                }
            }

            return [configs:configs,
                resourceModelConfigDescriptions:descriptions,
                nodeexecconfig:nodeexec,
                fcopyconfig:fcopy,
                defaultNodeExec: defaultNodeExec,
                defaultFileCopy: defaultFileCopy,
                nodeExecDescriptions: nodeexecdescriptions,
                fileCopyDescriptions: filecopydescs,
            ]
    }
    def systemConfig(){
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                        AuthConstants.ACTION_READ),
                AuthConstants.ACTION_READ, 'System configuration')) {
            return
        }
        [rundeckFramework: frameworkService.rundeckFramework]
    }
    def securityConfig(){
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                        AuthConstants.ACTION_READ),
                AuthConstants.ACTION_READ, 'System configuration')) {
            return
        }

        [rundeckFramework: frameworkService.rundeckFramework]
    }

    def systemInfo = {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                        AuthConstants.ACTION_READ),
                AuthConstants.ACTION_READ, 'System configuration')) {
            return
        }

        Date nowDate = new Date();
        String nodeName = servletContext.getAttribute("FRAMEWORK_NODE")
        String appVersion = grailsApplication.metadata['app.version']
        double load = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        int processorsCount = ManagementFactory.getOperatingSystemMXBean().availableProcessors
        String osName = ManagementFactory.getOperatingSystemMXBean().name
        String osVersion = ManagementFactory.getOperatingSystemMXBean().version
        String osArch = ManagementFactory.getOperatingSystemMXBean().arch
        String javaVendor = System.properties['java.vendor']
        String javaVersion = System.properties['java.version']
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
            javaVendor: javaVendor,
            javaVersion: javaVersion,
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
                vendor: info.javaVendor,
                version: info.javaVersion,
                name: info.vmName,
                implementationVersion: info.vmVersion
            ],
            ],
        ]]
    }

    def metrics(){

    }
    def licenses(){

    }

    def plugins(){
        //list plugins and config settings for project/framework props

        Framework framework = frameworkService.getRundeckFramework()

        //framework level plugin descriptions
        def pluginDescs= [
            framework.getNodeExecutorService(),
            framework.getFileCopierService(),
            framework.getNodeStepExecutorService(),
            framework.getStepExecutionService(),
//                framework.getResourceModelSourceService(),
            framework.getResourceFormatParserService(),
            framework.getResourceFormatGeneratorService(),
        ].collectEntries{
            [it.name, it.listDescriptions().sort {a,b->a.name<=>b.name}]
        }

        //web-app level plugin descriptions
        pluginDescs[notificationService.notificationPluginProviderService.name]=notificationService.listNotificationPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogReaderPluginProviderService.name]=loggingService.listStreamingReaderPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[loggingService.streamingLogWriterPluginProviderService.name]=loggingService.listStreamingWriterPlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[logFileStorageService.executionFileStoragePluginProviderService.name]= logFileStorageService.listLogFileStoragePlugins().collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storagePluginProviderService.name]= pluginService.listPlugins(StoragePlugin.class,storagePluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        pluginDescs[storageConverterPluginProviderService.name] = pluginService.listPlugins(StorageConverterPlugin.class, storageConverterPluginProviderService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }

        def defaultScopes=[
                (framework.getNodeStepExecutorService().name) : PropertyScope.InstanceOnly,
                (framework.getStepExecutionService().name) : PropertyScope.InstanceOnly,
        ]
        def bundledPlugins=[
                (framework.getNodeExecutorService().name): framework.getNodeExecutorService().getBundledProviderNames(),
                (framework.getFileCopierService().name): framework.getFileCopierService().getBundledProviderNames(),
                (framework.getResourceFormatParserService().name): framework.getResourceFormatParserService().getBundledProviderNames(),
                (framework.getResourceFormatGeneratorService().name): framework.getResourceFormatGeneratorService().getBundledProviderNames(),
                (storagePluginProviderService.name): storagePluginProviderService.getBundledProviderNames()+['db'],
        ]
        def specialConfiguration=[
                (storagePluginProviderService.name):[
                        description: "Configure this plugin within the rundeck-config.properties " +
                                "file. \nDeclare the provider with 'rundeck.storage.provider.[index].type=\${pluginName}', " +
                                "and declare the path you want this plugin to apply to with " +
                                "'rundeck.storage.provider.[index].path=<storagepath>'",
                        prefix:"rundeck.storage.provider.[index]."
                ],
                (storageConverterPluginProviderService.name):[
                        description: "Configure this plugin within the rundeck-config.properties " +
                                "file. \nDeclare the provider with 'rundeck.storage.converter.[index].type=\${pluginName}', " +
                                "and declare the path you want this plugin to apply to with " +
                                "'rundeck.storage.converter.[index].path=<storagepath>'",
                        prefix:"rundeck.storage.converter.[index]."
                ]
        ]

        [descriptions:pluginDescs,serviceDefaultScopes: defaultScopes, bundledPlugins: bundledPlugins, specialConfiguration: specialConfiguration]
    }

    def home(){
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        Framework framework = frameworkService.rundeckFramework
        def fprojects = frameworkService.projects(authContext)
        session.frameworkProjects = fprojects

        Calendar n = GregorianCalendar.getInstance()
        n.add(Calendar.DAY_OF_YEAR, -1)
        Date today = n.getTime()
        def summary=[:]
        fprojects.each { FrameworkProject project->
            summary[project.name]=[
                    jobCount: ScheduledExecution.countByProject(project.name),
                    execCount: Execution.countByProjectAndDateStartedGreaterThan(project.name,today),
                    description: project.hasProperty("project.description")?project.getProperty("project.description"):''
            ]

            summary[project.name].userSummary=Execution.createCriteria().list {
                eq('project',project.name)
                gt('dateStarted', today)
                projections {
                    distinct('user')
                }
            }
            summary[project.name].userCount= summary[project.name].userSummary.size()
            summary[project.name].readme=frameworkService.getFrameworkProjectReadmeContents(project.name)
            //authorization
            summary[project.name].auth = [
                    jobCreate: frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_TYPE_JOB,
                            AuthConstants.ACTION_CREATE, project.name),
                    admin: frameworkService.authorizeApplicationResourceAny(authContext,
                            AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT, [name: project.name]),
                            [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT,
                                    AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_DELETE]),
            ]
        }
        def projects = Execution.createCriteria().list {
            gt('dateStarted', today)
            projections {
                distinct('project')
            }
        }
        def users = Execution.createCriteria().list {
            gt('dateStarted', today)
            projections {
                distinct('user')
            }
        }
        //summarize cross-project details
        def jobCount = ScheduledExecution.count()
        def execCount= Execution.countByDateStartedGreaterThan( today)
        def fwkNode = framework.getFrameworkNodeName()
        [jobCount:jobCount,execCount:execCount,projectSummary:projects,projCount: fprojects.size(),userSummary:users,
                userCount:users.size(),projectSummaries:summary,
                frameworkNodeName: fwkNode,
        ]
    }

    /**
    * API Actions
     */

    /**
     * API: /api/jobs, version 1
     */
    def apiJobsList = {ScheduledExecutionQuery query ->
        if (!apiService.requireApi(request, response)) {
            return
        }
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])

        }
        query.projFilter = params.project
        //test valid project
        Framework framework = frameworkService.getRundeckFramework()

        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project', params.project]])

        }
        if(query.groupPathExact || query.jobExactFilter){
            //these query inputs require API version 2
            if (!apiService.requireVersion(request,response,ApiRequestFilters.V2)) {
                return
            }
        }
        def results = jobsFragment(query)

        return apiService.renderSuccessXml(request,response){
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
    /**
     * API: /api/2/project/NAME/jobs, version 2
     */
    def apiJobsListv2 = {ScheduledExecutionQuery query ->
        if(!apiService.requireVersion(request,response,ApiRequestFilters.V2)){
            return
        }
        return apiJobsList(query)
    }

    /**
     * API: /jobs/export, version 1
     */
    def apiJobsExport = {ScheduledExecutionQuery query ->
        if (!apiService.requireApi(request, response)) {
            return
        }
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        query.projFilter = params.project
        //test valid project
        Framework framework = frameworkService.getRundeckFramework()

        def exists=frameworkService.existsFrameworkProject(params.project)
        if(!exists){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: ['project',params.project]])
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
        if (!apiService.requireApi(request, response)) {
            return
        }
        if(!params.project){
            return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['project']])
        }
        //test valid project
        Framework framework = frameworkService.getRundeckFramework()

        //allow project='*' to indicate all projects
        def allProjects = request.api_version >= ApiRequestFilters.V9 && params.project == '*'
        if(!allProjects){
            if(!frameworkService.existsFrameworkProject(params.project)){
                return apiService.renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                        code: 'api.error.parameter.doesnotexist', args: ['project',params.project]])
            }
        }

        QueueQuery query = new QueueQuery(runningFilter:'running',projFilter:params.project)
        def results = nowrunning(query)
        return executionService.respondExecutionsXml(request,response,results.nowrunning)
    }
}

