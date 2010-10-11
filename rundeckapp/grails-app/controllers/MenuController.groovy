import java.util.Collections;

import com.dtolabs.rundeck.core.common.Framework

import grails.converters.JSON
import groovy.xml.MarkupBuilder
import com.dtolabs.client.utils.Constants

class MenuController {
    FrameworkService frameworkService
    ExecutionService executionService
    UserService userService
    ScheduledExecutionService scheduledExecutionService
    MenuService menuService
    RoleService roleService
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
        
        User u = userService.findOrCreateUser(session.user)
        Map filterpref=[:] 
        if(u){
            filterpref= UserController.parseKeyValuePref(u.filterPref)
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
        redirect(action:'jobs')
    }
    
    def workflows = {ScheduledExecutionQuery query ->
        return render(view:'jobs',model:jobs(query))
    }
    def jobs = {ScheduledExecutionQuery query ->
        
        def User u = userService.findOrCreateUser(session.user)
        if(params.size()<1 && !params.filterName && u ){
            Map filterpref = UserController.parseKeyValuePref(u.filterPref)
            if(filterpref['workflows']){
                params.filterName=filterpref['workflows']
            }
        }
        
        def results = jobsFragment(query)
        
        withFormat{
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
        
        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects = frameworkService.projects(framework)
        session.projects=projects
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
        return results
    }
    /**
     * Presents the jobs tree and can pass the jobsjscallback parameter
     * to the be a javascript callback for clicked jobs, instead of normal behavior.
     */
    def jobsPicker = {ScheduledExecutionQuery query ->

        Framework framework = frameworkService.getFrameworkFromUserSession(session,request)
        def projects = frameworkService.projects(framework)
        session.projects=projects
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
        def projects = frameworkService.projects(framework)
        if(null!=query){
            query.configureFilter()
        }
        def qres = scheduledExecutionService.listWorkflows(query)
        def schedlist=qres.schedlist
        def total=qres.total
        def filters=qres._filters
        
        def finishq=scheduledExecutionService.finishquery(query,params,qres)
        
        def nextExecutions=scheduledExecutionService.nextExecutionTimes(schedlist.findAll { it.scheduled })
        
        //find currently running executions
        def runlist = Execution.findAllByDateCompletedIsNullAndScheduledExecutionIsNotNull()
        def nowrunning=[:]
        runlist.each{
            nowrunning[it.scheduledExecution.id.toString()]=it.id.toString()
        }
        
        def authorizemap=[:]
        def pviewmap=[:]
        def newschedlist=[]
        def unauthcount=0
        def actualGroups=[]
        schedlist.each{ ScheduledExecution se->
            authorizemap[se.id.toString()]=scheduledExecutionService.userAuthorizedForJob(request,se,framework)
            if(authorizemap[se.id.toString()] || roleService.isUserInAnyRoles(request,['admin','job_view_unauthorized'])){
                newschedlist<<se
                if(se.groupPath){
                    actualGroups<<se.groupPath
                }
            }
            if(!authorizemap[se.id.toString()]){
                log.debug("Unauthorized job: ${se}")
                unauthcount++
            }
        }
        schedlist=newschedlist
        
        
        def groupsAndJobs = ScheduledExecution.executeQuery("select se.groupPath, se.jobName from ScheduledExecution se where se.groupPath is not null" )
        
        // Filter the groups by what the user is authorized to see.
        def Set res = groupsAndJobs.collect {
            ["job": it[1], "group": it[0]]
        }        
        
        def authorization = frameworkService.getFrameworkFromUserSession(request.session, request).getAuthorizationMgr()
        // construct groups how it was originally intended.
        // each groupPath, plus a count.
        def decisions = authorization.evaluate(res, request.subject, new HashSet(["workflow_read"]), Collections.emptySet())
        
        def groupMap=[:]
        decisions.findAll{ decision -> 
            decision.isAuthorized()
        }.collect { decision ->
            decision?.resource?.group
        }.groupBy{group -> group }.each{
            if(actualGroups.contains(it.key)){
                groupMap[it.key]=it.value.size
            }
        }
        
        def groupTree=[:]
        /*
         Build a tree structure of the available grouppaths, and their job counts
         [
         'a' :[
         count:6,
         subs:[
         'b': [
         count:4,
         ],
         'c': [
         count:1,
         subs:[
         'd': [
         count: 1,
         ]
         ]
         ]
         ]
         ]
         ]
         */
        
        
        /*
         'group/name' -> [ jobs...]
         */
        def jobgroups=[:]
        schedlist.each{sched->
            def key=sched.groupPath
            if(!key){
                key=''
            }
            if(jobgroups[key]){
                jobgroups[key]<<sched
            }else{
                jobgroups[key]=[sched]
            }
        }
        groupTree['jobs']=jobgroups['']
        groupTree['count']=jobgroups['']?.size()
        groupTree['total']=jobgroups['']?.size()
        groupTree['subs']=[:]
        groupMap.each{ String k,v ->
            def l = k.split("/")
            
            def cmap=groupTree['subs']
            def i=0
            l.each{p ->
                def path=l[0..i].join("/")
                def subs
                if(!cmap[p]){
                    subs=[:]
                    cmap[p]=[count:jobgroups[path]?jobgroups[path].size():0,total:v,subs:subs,jobs:jobgroups[path]]
                }else{
                    subs=cmap[p]['subs']
                    cmap[p]['total']+=v
                }
                cmap=subs
                i++
            }
        }
        
        return [
        projects:projects,
        nextScheduled:schedlist,
        nextExecutions: nextExecutions,
        authMap:authorizemap,
        nowrunning: nowrunning,
        groups:groupMap,
        groupTree:groupTree,
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
            filter= new ScheduledExecutionFilter(query.properties)
            filter.name=params.newFilterName
            u.addToJobfilters(filter)
            saveuser=true
        }else if(params.existsFilterName){
            filter = ScheduledExecutionFilter.findByNameAndUser(params.existsFilterName,u)
            if(filter){
                filter.properties=query.properties
            }
        }else if(!params.newFilterName && !params.existsFilterName){
            flash.error="Filter name not specified"
            params.saveFilter=true
            chain(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:params)
        }
        if(!filter.save(flush:true)){
            flash.error=filter.errors.allErrors.collect { g.message(error:it)
            }.join("<br>")
            params.saveFilter=true
            chain(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:params)
        }
        if(saveuser){
            if(!u.save(flush:true)){
                //                u.errors.allErrors.each { log.error(g.message(error:it)) }
                //                flash.error="Unable to save filter for user"
                flash.error=u.errors.allErrors.collect { g.message(error:it)
                }.join("\n")
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
            flash.message="Filter deleted: ${filtername}"
        }
        redirect(controller:'menu',action:params.fragment?'jobsFragment':'jobs',params:[compact:params.compact?'true':''])
    }
}

