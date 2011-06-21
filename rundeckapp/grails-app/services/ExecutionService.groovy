import org.springframework.web.servlet.support.RequestContextUtils as RCU

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.cli.CLIToolLogger
import com.dtolabs.rundeck.core.cli.CLIUtils
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionItem
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.NodeSet.Exclude;
import com.dtolabs.rundeck.core.utils.NodeSet.Include;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import grails.util.GrailsWebUtil
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.servlet.http.HttpSession
import org.apache.tools.ant.BuildEvent
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.BuildLogger
import org.apache.tools.ant.Project
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import com.dtolabs.rundeck.core.execution.commands.*
import com.dtolabs.rundeck.core.execution.workflow.*
import com.dtolabs.rundeck.execution.*

/**
 * Coordinates Command executions via Ant Project objects
 */
class ExecutionService implements ApplicationContextAware, CommandInterpreter{

    static transactional = true
    def FrameworkService frameworkService
    def notificationService
    def ScheduledExecutionService scheduledExecutionService
    def ReportService reportService

    def ThreadBoundOutputStream sysThreadBoundOut
    def ThreadBoundOutputStream sysThreadBoundErr
    def String defaultLogLevel

    def ApplicationContext applicationContext

    // implement ApplicationContextAware interface
    def void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }


    def listLastExecutionsPerProject(Framework framework, int max=5){
        def projects = frameworkService.projects(framework).collect{ it.name }

        def c = Execution.createCriteria()
        def lastexecs=[:]
        projects.each { proj ->
            def results = c.list {
                eq("project",proj)
                maxResults(max)
                order("dateCompleted","desc")
            }
            lastexecs[proj]=results
        }
        return lastexecs
    }

    /**
     * query queue, returns map [:]:
     *
     * query: query object
     * _filters: map of used filter names-&gt; properties
     * jobs: map of ID to ScheduledExecution for matching jobs
     * nowrunning: list of Executions
     * total: total executions
     */
    def queryQueue(QueueQuery query){
        def eqfilters = [
//            maprefUri:'maprefUri',
//            running:'running',
        ]
        def txtfilters = [
            obj:'name',
            type:'type',
            proj:'project',
            cmd:'command',
            user:'user',
//            node:'node',
//            message:'message',
//            job:'jobName',
//            tags:'tags',
        ]

        def filters = [ :]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)

        def Date endAfterDate = new Date(System.currentTimeMillis()-1000*60*60)
        if(query && query.doendafterFilter){
            endAfterDate = query.endafterFilter
        }
        def Date nowDate = new Date()

        def crit = Execution.createCriteria()
        def runlist = crit.list{
            if(query?.max){
                maxResults(query?.max.toInteger())
            }else{
//                maxResults(grailsApplication.config.reportservice.pagination.default?grailsApplication.config.reportservice.pagination.default.toInteger():20)
                maxResults(20)
            }
            if(query?.offset){
                firstResult(query.offset.toInteger())
            }

             if(query ){
                txtfilters.each{ key,val ->
                    if(query["${key}Filter"]){
                        ilike(val,'%'+query["${key}Filter"]+'%')
                    }
                }

                eqfilters.each{ key,val ->
                    if(query["${key}Filter"]){
                        eq(val,query["${key}Filter"])
                    }
                }

                 //running status filter.
                 if(query.runningFilter){
                     if('running'==query.runningFilter){
                        isNull("dateCompleted")
                     }else {
                         and{
                            eq('status',"completed"==query.runningFilter?'true':'false')
                            eq('cancelled',"killed"==query.runningFilter?'true':'false')
                            isNotNull('dateCompleted')
                         }
                     }
                 }

                 //original Job name filter
                 if(query.jobFilter){
                    scheduledExecution{
                        ilike('jobName','%'+query.jobFilter+'%')
                    }
                 }

//                if(query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter){
//                    between('dateStarted',query.startafterFilter,query.startbeforeFilter)
//                }
//                else if(query.dostartbeforeFilter && query.startbeforeFilter ){
//                    le('dateStarted',query.startbeforeFilter)
//                }else if (query.dostartafterFilter && query.startafterFilter ){
//                    ge('dateStarted',query.startafterFilter)
//                }
                
//                if(query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter){
//                    between('dateCompleted',query.endafterFilter,query.endbeforeFilter)
//                }
//                else if(query.doendbeforeFilter && query.endbeforeFilter ){
//                    le('dateCompleted',query.endbeforeFilter)
//                }
//                if(query.doendafterFilter && query.endafterFilter ){

//                or{
//                    between("dateCompleted", endAfterDate,nowDate)
                    isNull("dateCompleted")
//                }
//                }
            }else{
//                and {
//                    or{
//                        between("dateCompleted", endAfterDate,nowDate)
                        isNull("dateCompleted")
//                    }
//                }
            }

            if(query && query.sortBy && filters[query.sortBy]){
                order(filters[query.sortBy],query.sortOrder=='ascending'?'asc':'desc')
            }else{
                order("dateStarted","desc")
            }

        };
        def currunning=[]
        runlist.each{
            currunning<<it
        }

        def jobs =[:]
        currunning.each{
            if(it.scheduledExecution && !jobs[it.scheduledExecution.id.toString()]){
                jobs[it.scheduledExecution.id.toString()] = ScheduledExecution.get(it.scheduledExecution.id)
            }
        }


        def total = Execution.createCriteria().count{

             if(query ){
                txtfilters.each{ key,val ->
                    if(query["${key}Filter"]){
                        ilike(val,'%'+query["${key}Filter"]+'%')
                    }
                }

                eqfilters.each{ key,val ->
                    if(query["${key}Filter"]){
                        eq(val,query["${key}Filter"])
                    }
                }

                 //running status filter.
                 if(query.runningFilter){
                     if('running'==query.runningFilter){
                        isNull("dateCompleted")
                     }else {
                         and{
                            eq('status',"completed"==query.runningFilter?'true':'false')
                            eq('cancelled',"killed"==query.runningFilter?'true':'false')
                            isNotNull('dateCompleted')
                         }
                     }
                 }

                 //original Job name filter
                 if(query.jobFilter){
                    scheduledExecution{
                        ilike('jobName','%'+query.jobFilter+'%')
                    }
                 }

                isNull("dateCompleted")
            }else{
                isNull("dateCompleted")
            }
        };

        return [query:query, _filters:filters,
            jobs: jobs, nowrunning:currunning,
            total: total]
    }

    def public  finishQueueQuery = { query,params,model->

       if(!params.max){
           params.max=20
       }
       if(!params.offset){
           params.offset=0
       }

       def paginateParams=[:]
       if(query){
           model._filters.each{ key,val ->
               if(params["${key}Filter"]){
                   paginateParams["${key}Filter"]=params["${key}Filter"]
               }
           }
       }
       def displayParams = [:]
       displayParams.putAll(paginateParams)

       params.each{key,val ->
           if(key ==~ /^(start|end)(do|set|before|after)Filter.*/){
               paginateParams[key]=val
           }
       }
       if(query){
           if(query.recentFilter && query.recentFilter!='-'){
               displayParams.put("recentFilter",query.recentFilter)
               paginateParams.put("recentFilter",query.recentFilter)
               query.dostartafterFilter=false
               query.dostartbeforeFilter=false
               query.doendafterFilter=false
               query.doendbeforeFilter=false
           }else{
               if(query.dostartafterFilter && query.startafterFilter){
                   displayParams.put("startafterFilter",query.startafterFilter)
                   paginateParams.put("dostartafterFilter","true")
               }
               if(query.dostartbeforeFilter && query.startbeforeFilter){
                   displayParams.put("startbeforeFilter",query.startbeforeFilter)
                   paginateParams.put("dostartbeforeFilter","true")
               }
               if(query.doendafterFilter && query.endafterFilter){
                   displayParams.put("endafterFilter",query.endafterFilter)
                   paginateParams.put("doendafterFilter","true")
               }
               if(query.doendbeforeFilter && query.endbeforeFilter){
                   displayParams.put("endbeforeFilter",query.endbeforeFilter)
                   paginateParams.put("doendbeforeFilter","true")
               }
           }
       }
       def remkeys=[]
       if(!query || !query.dostartafterFilter){
           paginateParams.each{key,val ->
               if(key ==~ /^startafterFilter.*/){
                   remkeys.add(key)
               }
           }
       }
       if(!query || !query.dostartbeforeFilter){
           paginateParams.each{key,val ->
               if(key ==~ /^startbeforeFilter.*/){
                   remkeys.add(key)
               }
           }
       }
       if(!query || !query.doendafterFilter){
           paginateParams.each{key,val ->
               if(key ==~ /^endafterFilter.*/){
                   remkeys.add(key)
               }
           }
       }
       if(!query || !query.doendbeforeFilter){
           paginateParams.each{key,val ->
               if(key ==~ /^endbeforeFilter.*/){
                   remkeys.add(key)
               }
           }
       }
       remkeys.each{
           paginateParams.remove(it)
       }

       def tmod=[max: query?.max?query.max:20,
           offset:query?.offset?query.offset:0,
           paginateParams:paginateParams,
           displayParams:displayParams]
       model.putAll(tmod)
       return model
   }



    /**
    * Return a dataset: [nowrunning: (list of Execution), jobs: [map of id->ScheduledExecution for scheduled jobs],
     *  total: total number of running executions, max: input max] 
     */
    def listNowRunning(Framework framework, int max=10){
        //find currently running executions

        def Date lastHour = new Date(System.currentTimeMillis()-1000*60*60)
        def Date nowDate = new Date()

        def crit = Execution.createCriteria()
        def runlist = crit.list{
            maxResults(max)
            isNull("dateCompleted")
            order("dateStarted","desc")
        };
        def currunning=[]
        runlist.each{
            currunning<<it
        }

        def jobs =[:]
        currunning.each{
            if(it.scheduledExecution && !jobs[it.scheduledExecution.id.toString()]){
                jobs[it.scheduledExecution.id.toString()] = ScheduledExecution.get(it.scheduledExecution.id)
            }
        }


        def total = Execution.createCriteria().count{
            isNull("dateCompleted")
        };

        return [jobs: jobs, nowrunning:currunning, total: total, max: max]
    }

    /**
     * Set the result status to FAIL for any Executions that are not complete
     */
    def cleanupRunningJobs(){
        Execution.findAllByDateCompleted(null).each{Execution e->
            saveExecutionState(e.scheduledExecution?.id, e.id, [status: String.valueOf(false), dateCompleted: new Date(), cancelled: true],null)
            log.error("Stale Execution cleaned up: [${e.id}]")
        }
    }


    public logExecution(uri,project,user,issuccess,framework,execId,Date startDate=null, jobExecId=null, jobName=null, jobSummary=null,iscancelled=false, nodesummary=null, abortedby=null){

        def reportMap=[:]
        def internalLog = org.apache.log4j.Logger.getLogger("ExecutionService")
        if(null==project || null==user  ){
            //invalid
            internalLog.error("could not send execution report: some required values were null: (project:${project},user:${user})")
            return
        }

        if(execId){
            reportMap.jcExecId=execId
        }
        if(startDate){
            reportMap.dateStarted=startDate
        }
        if(jobExecId){
            reportMap.jcJobId=jobExecId
        }
        if(jobName){
            reportMap.reportId=jobName
        }else{
            reportMap.reportId='adhoc'
        }
        reportMap.ctxProject=project

        if(iscancelled && abortedby){
            reportMap.abortedByUser=abortedby
        }else if(iscancelled){
            reportMap.abortedByUser=user
        }
        reportMap.author=user
        reportMap.title= jobSummary?jobSummary:"RunDeck Job Execution"
        reportMap.status= issuccess ? "succeed":iscancelled?"cancel":"fail"
        reportMap.node= null!=nodesummary?nodesummary: framework.getFrameworkNodeName()

        reportMap.message=(issuccess?'Job completed successfully':iscancelled?('Job killed by: '+(abortedby?:user)):'Job failed')
        reportMap.dateCompleted=new Date()
        def result=reportService.reportExecutionResult(reportMap)
        if(result.error){
            log.error("Failed to create report: "+result.report.errors.allErrors.collect{it.toString()}).join("; ")
        }
    }


    /**
     * starts an execution in a separate thread, returning a map of [thread:Thread, loghandler:LogHandler]
     */
    def Map executeAsyncBegin(Framework framework, Execution execution, ScheduledExecution scheduledExecution=null){
        execution.refresh()
        String lognamespace="rundeck"
        if(execution.workflow){
            lognamespace="workflow"
        }else if (execution.adhocExecution && (execution.adhocRemoteString || execution.adhocLocalString || execution.adhocFilepath)){
            lognamespace="run"
        }else{
            lognamespace=execution.command
        }

        def outfile = createOutputFilepathForExecution(execution, framework)
        execution.outputfilepath=outfile
        execution.save(flush:true)
        def LogHandler loghandler = createLogHandler(lognamespace, execution.outputfilepath,execution.loglevel,
            [user:execution.user,node:framework.getFrameworkNodeName()])

        //install custom outputstreams for System.out and System.err for this thread and any child threads
        //output will be sent to loghandler instead.
        sysThreadBoundOut.installThreadStream(loghandler.createLoggerStream(Level.WARNING, null));
        sysThreadBoundErr.installThreadStream(loghandler.createLoggerStream(Level.SEVERE, null));

        try{
            def jobcontext=new HashMap<String,String>()
            if(scheduledExecution){
                jobcontext.name=scheduledExecution.jobName
                jobcontext.group=scheduledExecution.groupPath
                jobcontext.id=scheduledExecution.id.toString()
            }
            jobcontext.username=execution.user
            jobcontext.project=execution.project

            WorkflowExecutionItem item = createExecutionItemForExecutionContext(execution, framework, execution.user)

            NodeRecorder recorder = new NodeRecorder();//TODO: use workflow-aware listener for nodes

            //create listener to handle log messages and Ant build events
            ExecutionListener executionListener = new WorkflowExecutionListenerImpl(recorder, loghandler,false,null);
            com.dtolabs.rundeck.core.execution.ExecutionContext executioncontext = createContext(execution, framework, execution.user, jobcontext, executionListener)

            final cis = CommandInterpreterService.getInstanceForFramework(framework);
            cis.registerInstance(JobExecutionItem.COMMAND_TYPE, this)

            //create service object for the framework and listener
            Thread thread = new WorkflowExecutionServiceThread(framework.getWorkflowExecutionService(),item, executioncontext)
            thread.start()
            return [thread:thread, loghandler:loghandler, noderecorder:recorder, execution: execution, scheduledExecution:scheduledExecution]
        }catch(Exception e){
            loghandler.publish(new LogRecord(Level.SEVERE, 'Failed to start execution: ' + e.getClass().getName() + ": " + e.message))
            sysThreadBoundOut.removeThreadStream()
            sysThreadBoundErr.removeThreadStream()
            loghandler.close()
            log.error('Failed to start execution',e)
            return null
        }
    }

    /**
     * create the path to the execution output file based on the Execution object.
     *
     * Uses the execution ID if present as the filename, otherwise generates a unique
     * filename based on the type of execution.
     *
     * Sets the directory based on associated ScheduledExecution,
     * and if that does not exist then based on execution type and context.
     */
    def String createOutputFilepathForExecution(Execution execution, Framework framework){
        def String name=(execution.adhocExecution?'run':(execution.workflow?'workflow':execution.command))+"-"+generateTimestamp()+"_"+generateUniqueId()+".txt"
        if(execution.id){
            name=execution.id.toString()+".txt"
        }
        if(execution.scheduledExecution){
            return new File(maybeCreateJobLogDir(execution.scheduledExecution,framework),name).getAbsolutePath()
        }else{
            return new File(maybeCreateAdhocLogDir(execution,framework),name).getAbsolutePath()
        }
    }

    /**
     * Return an appropriate ExecutionItem object for the stored Execution
     */
    public WorkflowExecutionItem createExecutionItemForExecutionContext(ExecutionContext execution, Framework framework, String user=null) {
        WorkflowExecutionItem item
        if (execution.workflow) {
            item = createExecutionItemForWorkflowContext(execution, framework,user)
        } else {
            throw new RuntimeException("unsupported job type")
        }
        return item
    }


    /**
     * Return an ExecutionItem instance for the given workflow Execution, suitable for the ExecutionService layer
     */
    public WorkflowExecutionItem createExecutionItemForWorkflowContext(ExecutionContext execMap, Framework framework, String userName=null) {
        if (!execMap.workflow.commands || execMap.workflow.commands.size() < 1) {
            throw new Exception("Workflow is empty")
        }
        def User user = User.findByLogin(userName?userName:execMap.user)
        if (!user) {
            throw new Exception(g.message(code:'unauthorized.job.run.user',args:[userName?userName:execMap.user]))
        }

        //create thread object with an execution item, and start it
        final WorkflowExecutionItemImpl item = new WorkflowExecutionItemImpl(
            new WorkflowImpl(execMap.workflow.commands.collect {itemForWFCmdItem(it)}, execMap.workflow.threadcount, execMap.workflow.keepgoing,execMap.workflow.strategy?execMap.workflow.strategy: "node-first"))
        return item
    }

    public ExecutionItem itemForWFCmdItem(final IWorkflowCmdItem cmd) throws FileNotFoundException {
        if (null != cmd.getAdhocRemoteString()) {

            final List<String> strings = CLIUtils.splitArgLine(cmd.getAdhocRemoteString());
            final String[] args = strings.toArray(new String[strings.size()]);

            return ExecutionItemFactory.createExecCommand(args);
            
        } else if (null != cmd.getAdhocLocalString()) {
            final String script = cmd.getAdhocLocalString();
            final String[] args;
            if (null != cmd.getArgString()) {
                final List<String> strings = CLIUtils.splitArgLine(cmd.getArgString());
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }
            return ExecutionItemFactory.createScriptFileItem(script, args);

        } else if (null != cmd.getAdhocFilepath()) {
            final String filepath = cmd.getAdhocFilepath();
            final String[] args;
            if (null != cmd.getArgString()) {
                final List<String> strings = CLIUtils.splitArgLine(cmd.getArgString());
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }
            return ExecutionItemFactory.createScriptFileItem(new File(filepath), args);
        } else if (cmd instanceof IWorkflowJobItem) {
            final IWorkflowJobItem jobcmditem = (IWorkflowJobItem) cmd;

            final String[] args;
            if (null != jobcmditem.getArgString()) {
                final List<String> strings = CLIUtils.splitArgLine(jobcmditem.getArgString());
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }

            return ExecutionItemFactory.createJobRef(jobcmditem.getJobIdentifier(),args)
        } else {
            throw new IllegalArgumentException("Workflow command item was not valid");
        }
    }

    /**
     * Return an ExecutionItem instance for the given workflow Execution, suitable for the ExecutionService layer
     */
    public com.dtolabs.rundeck.core.execution.ExecutionContext createContext(ExecutionContext execMap, Framework framework, String userName = null, Map<String, String> jobcontext, ExecutionListener listener, String[] inputargs=null) {
        def User user = User.findByLogin(userName ? userName : execMap.user)
        if (!user) {
            throw new Exception(g.message(code: 'unauthorized.job.run.user', args: [userName ? userName : execMap.user]))
        }
        //convert argString into Map<String,String>
        def String[] args = execMap.argString? CLIUtils.splitArgLine(execMap.argString):inputargs
        def Map<String, String> optsmap = execMap.argString ? frameworkService.parseOptsFromString(execMap.argString) : null!=args? frameworkService.parseOptsFromArray(args):null

        def Map<String,Map<String,String>> datacontext = new HashMap<String,Map<String,String>>()
				datacontext.put("option",optsmap)
				datacontext.put("job",jobcontext?jobcontext:new HashMap<String,String>())
		
				NodeSet nodeset
				
        if (execMap.doNodedispatch) {
            //set nodeset for the context if doNodedispatch parameter is true
            nodeset = filtersAsNodeSet(execMap)
        } else {
            //blank?
            nodeset = new NodeSet()
        }
				
				// enhnacement to allow ${option.xyz} in tags and names
				if (nodeset != null) {
					Include includes = nodeset.getInclude();
		
					if (includes != null) {
						if (includes.getName() != null) {
							includes.setName(DataContextUtils.replaceDataReferences(
									includes.getName(), datacontext));
						}
						if (includes.getTags() != null) {
							includes.setTags(DataContextUtils.replaceDataReferences(
									includes.getTags(), datacontext));
						}
					}
		
					Exclude excludes = nodeset.getExclude();
					if (excludes != null) {
						if (excludes.getName() != null) {
							excludes.setName(DataContextUtils.replaceDataReferences(
									excludes.getName(), datacontext));
						}
						if (excludes.getTags() != null) {
							excludes.setTags(DataContextUtils.replaceDataReferences(
									excludes.getTags(), datacontext));
						}
					}
				}
        
        //create thread object with an execution item, and start it
        final com.dtolabs.rundeck.core.execution.ExecutionContext item =  com.dtolabs.rundeck.core.execution.ExecutionContextImpl.createExecutionContextImpl(
            execMap.project,
            user.login,
            nodeset,
            args,
            loglevels[null != execMap.loglevel ? execMap.loglevel : 'WARN'],
            datacontext,
            listener,
            framework)
        return item
    }

    /**
     * cleans up executed job
     * @param framework the framework
     * @execMap map contains 'thread' and 'loghandler' keys, for Thread and LogHandler objects
     */
    def boolean executeAsyncFinish(Map execMap){
        def WorkflowExecutionServiceThread thread=execMap.thread
        def LogHandler loghandler=execMap.loghandler
        if(!thread.isSuccessful() ){
            Throwable exc = thread.getThrowable()
            def errmsgs = []

            if (exc && (exc instanceof com.dtolabs.rundeck.core.NodesetFailureException
                || exc instanceof com.dtolabs.rundeck.core.NodesetEmptyException)) {
                errmsgs << exc.getMessage()
            }else if(exc){
                errmsgs<< exc.getMessage()
                if(exc.getCause()){
                    errmsgs << "Caused by: "+exc.getCause().getMessage()
                }
            }else if (Project.MSG_VERBOSE <= loghandler.getMessageOutputLevel()) {
                loghandler.publish(new LogRecord(Level.SEVERE, thread.resultObject?.toString()))
            }
            if(errmsgs) {
                log.error("Execution failed: " + execMap.execution.id + ": " + errmsgs.join(","))
                if (exc && Project.MSG_VERBOSE <= loghandler.getMessageOutputLevel()) {
                    errmsgs << org.apache.tools.ant.util.StringUtils.getStackTrace(exc)
                }
                loghandler.publish(new LogRecord(Level.SEVERE, errmsgs.join(',')))
            }else {
                log.error("Execution failed: " + execMap.execution.id + ": " + thread.resultObject?.toString())
            }

        }else{
            log.info("Execution successful: " + execMap.execution.id )
        }
        sysThreadBoundOut.removeThreadStream()
        sysThreadBoundErr.removeThreadStream()
        loghandler.close()
        return thread.isSuccessful()
    }

    def abortExecution(ScheduledExecution se, Execution e, String user){
        def ident = scheduledExecutionService.getJobIdent(se,e)
        def statusStr
        def abortstate
        def jobstate
        if(scheduledExecutionService.existsJob(ident.jobname, ident.groupname)){
            if(!e.abortedby){
                e.abortedby=user
                e.save()
            }
            def didcancel=scheduledExecutionService.interruptJob(ident.jobname, ident.groupname)
            abortstate=didcancel?ExecutionController.ABORT_PENDING:ExecutionController.ABORT_FAILED
            jobstate=ExecutionController.EXECUTION_RUNNING
        }else if(null==e.dateCompleted){
            saveExecutionState(
                se?se.id:null,
                e.id,
                    [
                    status:String.valueOf(false),
                    dateCompleted:new Date(),
                    cancelled:true,
                    abortedby:user
                    ]
                )
            abortstate=ExecutionController.ABORT_ABORTED
            jobstate=ExecutionController.EXECUTION_ABORTED
        }else{
            jobstate=ExecutionController.getExecutionState(e)
            statusStr='previously '+jobstate
            abortstate=ExecutionController.ABORT_FAILED
        }
        return [abortstate:abortstate,jobstate:jobstate,statusStr:statusStr]
    }

    /**
     * Return a map of include filters as used by the NodeSet type
     */
    public static Map includeFiltersAsNodeSetMap( econtext) {
        def nodeIncludeMap = [:]
        if (econtext.nodeInclude || econtext.nodeExclude
                               || econtext.nodeIncludeName || econtext.nodeExcludeName
                               || econtext.nodeIncludeTags || econtext.nodeExcludeTags
                               || econtext.nodeIncludeOsName || econtext.nodeExcludeOsName
                               || econtext.nodeIncludeOsFamily || econtext.nodeExcludeOsFamily
                               || econtext.nodeIncludeOsArch || econtext.nodeExcludeOsArch
                               || econtext.nodeIncludeOsVersion || econtext.nodeExcludeOsVersion
            ) {

            nodeIncludeMap[NodeSet.HOSTNAME] = econtext.nodeInclude
            nodeIncludeMap[NodeSet.NAME] = econtext.nodeIncludeName
            nodeIncludeMap[NodeSet.TAGS] = econtext.nodeIncludeTags
            nodeIncludeMap[NodeSet.OS_NAME] = econtext.nodeIncludeOsName
            nodeIncludeMap[NodeSet.OS_FAMILY] = econtext.nodeIncludeOsFamily
            nodeIncludeMap[NodeSet.OS_ARCH] = econtext.nodeIncludeOsArch
            nodeIncludeMap[NodeSet.OS_VERSION] = econtext.nodeIncludeOsVersion
        }
        return nodeIncludeMap
    }

    /**
     * Return a map of exclude filters as used by the NodeSet type
     */
    public static Map excludeFiltersAsNodeSetMap( econtext) {

        def nodeExcludeMap = [:]
        if (econtext.nodeInclude || econtext.nodeExclude
                               || econtext.nodeIncludeName || econtext.nodeExcludeName
                               || econtext.nodeIncludeTags || econtext.nodeExcludeTags
                               || econtext.nodeIncludeOsName || econtext.nodeExcludeOsName
                               || econtext.nodeIncludeOsFamily || econtext.nodeExcludeOsFamily
                               || econtext.nodeIncludeOsArch || econtext.nodeExcludeOsArch
                               || econtext.nodeIncludeOsVersion || econtext.nodeExcludeOsVersion
            ) {
            nodeExcludeMap[NodeSet.HOSTNAME] = econtext.nodeExclude
            nodeExcludeMap[NodeSet.NAME] = econtext.nodeExcludeName
            nodeExcludeMap[NodeSet.TAGS] = econtext.nodeExcludeTags
            nodeExcludeMap[NodeSet.OS_NAME] = econtext.nodeExcludeOsName
            nodeExcludeMap[NodeSet.OS_FAMILY] = econtext.nodeExcludeOsFamily
            nodeExcludeMap[NodeSet.OS_ARCH] = econtext.nodeExcludeOsArch
            nodeExcludeMap[NodeSet.OS_VERSION] = econtext.nodeExcludeOsVersion

        }
        return nodeExcludeMap
    }

    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(BaseNodeFilters econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(excludeFiltersAsNodeSetMap(econtext)).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(includeFiltersAsNodeSetMap(econtext)).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        return nodeset
    }
    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(ExecutionContext econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(excludeFiltersAsNodeSetMap(econtext)).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(includeFiltersAsNodeSetMap(econtext)).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing?true:false)
        nodeset.setThreadCount(econtext.nodeThreadcount?econtext.nodeThreadcount:1)
        return nodeset
    }

    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(Map econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(excludeFiltersAsNodeSetMap(econtext)).setDominant(econtext.nodeExcludePrecedence?true:false);
        nodeset.createInclude(includeFiltersAsNodeSetMap(econtext)).setDominant(!econtext.nodeExcludePrecedence?true:false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing?true:false)
        nodeset.setThreadCount(econtext.nodeThreadcount?econtext.nodeThreadcount:1)
        return nodeset
    }


   def Execution createExecution(Map params, Framework framework) {
        def Execution execution
        if (params.project && params.workflow) {
            execution = new Execution(project:params.project,
                                      user:params.user,loglevel:params.loglevel,
                                    doNodedispatch:params.doNodedispatch?"true" == params.doNodedispatch.toString():false,
                                    nodeInclude:params.nodeInclude,
                                    nodeExclude:params.nodeExclude,
                                    nodeIncludeName:params.nodeIncludeName,
                                    nodeExcludeName:params.nodeExcludeName,
                                    nodeIncludeTags:params.nodeIncludeTags,
                                    nodeExcludeTags:params.nodeExcludeTags,
                                    nodeIncludeOsName:params.nodeIncludeOsName,
                                    nodeExcludeOsName:params.nodeExcludeOsName,
                                    nodeIncludeOsFamily:params.nodeIncludeOsFamily,
                                    nodeExcludeOsFamily:params.nodeExcludeOsFamily,
                                    nodeIncludeOsArch:params.nodeIncludeOsArch,
                                    nodeExcludeOsArch:params.nodeExcludeOsArch,
                                    nodeIncludeOsVersion:params.nodeIncludeOsVersion,
                                    nodeExcludeOsVersion:params.nodeExcludeOsVersion,
                                    nodeExcludePrecedence:params.nodeExcludePrecedence,
                                    nodeThreadcount:params.nodeThreadcount,
                                    nodeKeepgoing:params.nodeKeepgoing,
                                    workflow:params.workflow,
                                    argString:params.argString
            )


            
            //parse options
            if(!execution.loglevel){
                execution.loglevel=defaultLogLevel
            }
                
        } else {
            throw new IllegalArgumentException("insufficient params to create a new Execution instance: " + params)
        }
        return execution
    }

    /**
     * creates an execution with the parameters, and evaluates dynamic buildstamp
     */
    def Execution createExecutionAndPrep(Map params, Framework framework, String user) throws ExecutionServiceException{
        def props =[:]
        props.putAll(params)
        if(!props.user){
            props.user=user
        }
        def Execution execution = createExecution(props, framework)
        execution.dateStarted = new Date()

        if(execution.argString =~ /\$\{DATE:(.*)\}/){

            def newstr = execution.argString
            try{
                newstr = execution.argString.replaceAll(/\$\{DATE:(.*)\}/,{ all,tstamp ->
                    new SimpleDateFormat(tstamp).format(execution.dateStarted)
                })
            }catch(IllegalArgumentException e){
                log.warn(e)
            }


            execution.argString=newstr
        }

        if(execution.workflow){
            if(!execution.workflow.save(flush:true)){
                def err=execution.workflow.errors.allErrors.collect { it.toString() }.join(", ")
                log.error("unable to save workflow: ${err}")
                throw new ExecutionServiceException("unable to save workflow: "+err)
            }
        }

        if(!execution.save(flush:true)){
            execution.errors.allErrors.each { log.warn(it.defaultMessage) }
            log.error("unable to save execution")
            throw new ExecutionServiceException("unable to save execution")
        }
        return execution
    }



    def Execution createExecution(ScheduledExecution se, Framework framework, String user, Map extra=[:]) throws ExecutionServiceException{
        def ScheduledExecution scheduledExec = ScheduledExecution.get(se.id)
        se = scheduledExec
        se.refresh()

        //find any currently running executions for this job, and if so, throw exception
        def c = Execution.createCriteria()
        def found = c.get {
            scheduledExecution {
                eq('id', se.id)
            }
            isNotNull('dateStarted')
            isNull('dateCompleted')
        }

        log.info("createExecution for ScheduledExecution: ${se.id}")
        def props =[:]
        props.putAll(se.properties)
        if(!props.user){
            props.user=user
        }
        if(extra && 'true' == extra['_replaceNodeFilters']){
            //remove all existing node filters to replace with input filters
            props = props.findAll {!(it.key=~/^node(Include|Exclude).*$/)}
        }
        if(extra){
            props.putAll(extra)
        }

        //evaluate embedded Job options for Regex match against input values

        def optparams = ExecutionService.filterOptParams(props)
        if(!optparams){
            props.argString=addArgStringOptionDefaults(scheduledExec, props.argString)
        }
        validateInputOptionValues(scheduledExec, props)
        if (optparams) {
            props.argString = generateJobArgline(scheduledExec, optparams)
        }

        if(found){
            throw new ExecutionServiceException('Job "'+se.jobName+'" ['+se.id+'] is currently being executed (execution ['+found.id+'])')
        }

        //create duplicate workflow
        if(se.workflow){
            props.workflow=new Workflow(se.workflow)
        }

        def Execution execution = createExecution(props, framework)
        se.addToExecutions(execution)
        execution.scheduledExecution=se
        execution.dateStarted = new Date()


        if(execution.argString =~ /\$\{DATE:(.*)\}/){

            def newstr = execution.argString
            try{
                newstr = execution.argString.replaceAll(/\$\{DATE:(.*)\}/,{ all,tstamp ->
                    new SimpleDateFormat(tstamp).format(execution.dateStarted)
                })
            }catch(IllegalArgumentException e){
                log.warn(e)
            }


            execution.argString=newstr
        }


        if(execution.workflow && !execution.workflow.save(flush:true)){
            execution.workflow.errors.allErrors.each { log.warn(it.defaultMessage) }
            log.error("unable to save execution workflow")
            throw new ExecutionServiceException("unable to create execution workflow")
        }
        if(!execution.save(flush:true)){
            execution.errors.allErrors.each { log.warn(it.defaultMessage) }
            log.error("unable to save execution")
            throw new ExecutionServiceException("unable to create execution")
        }
        if(!se.save(flush:true)){
            se.errors.allErrors.each { log.warn(it.defaultMessage) }
            log.error("unable to save scheduledExecution")
            throw new ExecutionServiceException("unable to save scheduledExecution")
        }
        return execution
    }

    /**
     * evaluate the options in the input argString, and if any Options defined for the Job have required=true, have a
     * defaultValue, and have null value in the input properties, then append the default option value to the argString
     */
    def String addArgStringOptionDefaults(ScheduledExecution scheduledExecution, args) throws ExecutionServiceException {
        def StringBuffer sb = new StringBuffer()
        def optparams = [:]
        if(args && args instanceof String){
            optparams = args?frameworkService.parseOptsFromString(args):[:]
            sb.append(args?:"")
        }else if(args && args instanceof String[]){
            optparams = frameworkService.parseOptsFromArray(args)
            sb.append(args?args.join(" "):'')
        }

        final options = scheduledExecution.options
        if (options) {
            def defaultoptions=[:]
            options.each {Option opt ->
                if (opt.required && null==optparams[opt.name] && opt.defaultValue) {
                    defaultoptions[opt.name]=opt.defaultValue
                }
            }
            if(defaultoptions){
                if(sb.size()>0){
                    sb.append(" ")
                }
                sb.append( generateArgline(defaultoptions))
            }
        }
        return sb.toString()
    }

    /**
     * evaluate the options in the input properties, and if any Options defined for the Job have regex constraints,
     * require the values in the properties to match the regular expressions.  Throw ExecutionServiceException if
     * any options don't match.
     */
    def boolean validateInputOptionValues(ScheduledExecution scheduledExecution, Map props) throws ExecutionServiceException{
        def fail=false
        def StringBuffer sb = new StringBuffer()
        def optparams = ExecutionService.filterOptParams(props)
        if(!optparams && props.argString){
            optparams = parseJobOptsFromString(scheduledExecution,props.argString)
        }
        def failedkeys=[:]
        if (scheduledExecution.options) {
            scheduledExecution.options.each {Option opt ->
                if (!opt.multivalued && optparams[opt.name] && !(optparams[opt.name] instanceof String)) {
                    fail = true
                    if (!failedkeys[opt.name]) {
                        failedkeys[opt.name] = ''
                    }
                    final String msg = "Option '${opt.name}' value: ${optparams[opt.name]} does not allow multiple values.\n"
                    sb << msg
                    failedkeys[opt.name] += msg
                    return
                }
                if (opt.required && !optparams[opt.name]) {
                    fail = true
                    if (!failedkeys[opt.name]) {
                        failedkeys[opt.name] = ''
                    }
                    final String msg = "Option '${opt.name}' is required.\n"
                    sb << msg
                    failedkeys[opt.name] += msg
                    return
                }
                if(opt.multivalued){
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        def val = [optparams[opt.name]].flatten()
                        val.each{value->
                            if (!(value ==~ opt.regex)) {
                                fail = true
                                if (!failedkeys[opt.name]) {
                                    failedkeys[opt.name] = ''
                                }

                            }
                        }
                        if (fail) {
                            final String msg = "Option '${opt.name}' values: ${optparams[opt.name]} did not all match regular expression: '${opt.regex}'\n"
                            sb << msg
                            failedkeys[opt.name] += msg
                            return
                        }
                    }
                    if (opt.enforced && opt.values && optparams[opt.name]) {
                        def val = [optparams[opt.name]].flatten();
                        if (!opt.values.containsAll(val.grep{it})) {
                            fail = true
                            if (!failedkeys[opt.name]) {
                                failedkeys[opt.name] = ''
                            }
                            final String msg = "Option '${opt.name}' values: ${optparams[opt.name]} were not all in the allowed values: ${opt.values}\n"
                            sb << msg
                            failedkeys[opt.name] += msg
                            return
                        }
                    }
                }else{
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        if (!(optparams[opt.name] ==~ opt.regex)) {
                            fail = true
                            if (!failedkeys[opt.name]) {
                                failedkeys[opt.name] = ''
                            }
                            final String msg = "Option '${opt.name}' doesn't match regular expression: '${opt.regex}', value: ${optparams[opt.name]}\n"
                            sb << msg
                            failedkeys[opt.name] += msg
                            return
                        }
                    }
                    if (opt.enforced && opt.values && optparams[opt.name] && optparams[opt.name] instanceof String && !opt.values.contains(optparams[opt.name])){
                        fail=true
                        if(!failedkeys[opt.name]){
                            failedkeys[opt.name]=''
                        }
                        final String msg = "Option '${opt.name}' value: ${optparams[opt.name]} was not in the allowed values: ${opt.values}\n"
                        sb << msg
                        failedkeys[opt.name]+=msg
                        return
                    }
                }
            }
        }
        if (fail) {
            def msg = sb.toString()
            throw new ExecutionServiceValidationException(msg,optparams,failedkeys)
        }
        return !fail
    }

    /**
     *  Parse an argString for a Job, treating multi-valued options as delimiter-separated and converting to a List of values
     * @param scheduledExecution
     * @param argString
     * @return map of option name to value, where value is a String or a List of Strings
     */
    def Map parseJobOptsFromString(ScheduledExecution scheduledExecution, String argString){
        def optparams = frameworkService.parseOptsFromString(argString)
        if(optparams){
            //look for multi-valued options and try to split on delimiters
            scheduledExecution.options.each{Option opt->
                if(opt.multivalued && optparams[opt.name]){
                    def arr = optparams[opt.name].split(Pattern.quote(opt.delimiter))
                    optparams[opt.name]=arr as List
                }
            }
        }
        return optparams
    }

    def static loglevels=['ERR':Project.MSG_ERR,'ERROR':Project.MSG_ERR,'WARN':Project.MSG_WARN,'INFO':Project.MSG_INFO,'VERBOSE':Project.MSG_VERBOSE,'DEBUG':Project.MSG_DEBUG]

    def LogHandler createLogHandler(command, filepath,loglevel="WARN", Map defaultData=null){
        def namespace = "com.dtolabs.rundeck.core."+command
        if (!filepath) {
            throw new IllegalArgumentException("outputfilepath property value not set" )
        }

        if (!applicationContext){
            throw new IllegalStateException("ApplicationContext instance not found!")
        }

        def deflevel=applicationContext.getServletContext().getAttribute("LOGLEVEL_DEFAULT")
        def int level=loglevels[deflevel]?loglevels[deflevel]:Project.MSG_INFO;
        if(null!=loglevels[loglevel]){
            level=loglevels[loglevel]
        }


        return new HtTableLogger(namespace, new File(filepath), level,defaultData)
    }

    def saveExecutionState( schedId, exId, Map props, Map execmap=null){
        def ScheduledExecution scheduledExecution
        def Execution execution = Execution.get(exId)
        execution.properties=props
        if (props.failedNodes) {
            execution.failedNodeList = props.failedNodes.join(",")
        }
        def boolean execSaved=false
        if (execution.save(flush:true)) {
            log.debug("saved execution status. id: ${execution.id}")
            execSaved=true
        } else {

            execution.errors.allErrors.each { log.warn(it.defaultMessage) }
            log.error("failed to save execution status")
        }

        def jobname="adhoc"
        def jobid=null
        if (schedId) {
            ScheduledExecution.withTransaction{
            scheduledExecution = ScheduledExecution.get(schedId)
            execution = execution.merge()
            jobname=scheduledExecution.groupPath?scheduledExecution.generateFullName():scheduledExecution.jobName
            jobid=scheduledExecution.id
            if (scheduledExecution) {

                    log.debug("saveExecutionState, schedExec version: "+scheduledExecution.version)
                    try{
                        updateScheduledExecState(scheduledExecution,execution)
                    }catch(org.springframework.dao.OptimisticLockingFailureException e){
                        log.error("lock problem, refreshing to try again: "+e)
                        scheduledExecution.refresh()
                        updateScheduledExecState(scheduledExecution,execution)
                    }

            }
            }
        }
        if(execSaved) {
            //summarize node success
            String node=null
            int sucCount=-1;
            int failedCount=-1;
            int totalCount=0;
            if (execmap && execmap.noderecorder && execmap.noderecorder instanceof NodeRecorder) {
                NodeRecorder rec = (NodeRecorder) execmap.noderecorder
                final HashSet<String> success = rec.getSuccessfulNodes()
                final Map<String,Object> failedMap = rec.getFailedNodes()
                final HashSet<String> failed = new HashSet<String>(failedMap.keySet())
                final HashSet<String> matched = rec.getMatchedNodes()
                node = [success.size(),failed.size(),matched.size()].join("/")
                sucCount=success.size()
                failedCount=failed.size()
                totalCount=matched.size()
            }
            def Framework fw = frameworkService.getFramework()
            logExecution(null, execution.project, execution.user, "true" == execution.status, fw, exId,
                execution.dateStarted, jobid, jobname, summarizeJob(scheduledExecution, execution), props.cancelled,
                node, execution.abortedby)
            notificationService.triggerJobNotification(props.status == 'true' ? 'success' : 'failure', schedId, [execution: execution,nodestatus:[succeeded:sucCount,failed:failedCount,total:totalCount]])
        }
    }
    public static String summarizeJob(ScheduledExecution job=null,Execution exec){
//        if(job){
//            return job.groupPath?job.generateFullName():job.jobName
//        }else{
            //summarize execution
            StringBuffer sb = new StringBuffer()
            final def wfsize = exec.workflow.commands.size()

            if(wfsize>0){
                sb<<exec.workflow.commands[0].summarize()
            }else{
                sb<< "[Empty workflow]"
            }
            if(wfsize>1){
                sb << " [... ${wfsize} steps]"
            }
            return sb.toString()
//        }
    }
    def updateScheduledExecState(ScheduledExecution scheduledExecution, Execution execution){
        if (scheduledExecution.scheduled) {
            scheduledExecution.nextExecution = scheduledExecutionService.nextExecutionTime(scheduledExecution)
        }
        scheduledExecution.addToExecutions(execution)
        //if execution has valid timing data, update the scheduledExecution timing info
        if (!execution.cancelled && "true".equals(execution.status)) {
            if (execution.dateStarted && execution.dateCompleted) {
                def long time = execution.dateCompleted.getTime() - execution.dateStarted.getTime()
                if (null == scheduledExecution.execCount || 0 == scheduledExecution.execCount || null == scheduledExecution.totalTime || 0 == scheduledExecution.totalTime) {
                    scheduledExecution.execCount = 1
                    scheduledExecution.totalTime = time
                } else if (scheduledExecution.execCount > 0 && scheduledExecution.execCount < 10) {
                    scheduledExecution.execCount++
                    scheduledExecution.totalTime += time
                } else if (scheduledExecution.execCount >= 10) {
                    def popTime = scheduledExecution.totalTime.intdiv(scheduledExecution.execCount)
                    scheduledExecution.totalTime -= popTime
                    scheduledExecution.totalTime += time
                }
            }
        }
        if (scheduledExecution.save(flush:true)) {
            log.info("updated scheduled Execution")
        } else {
            scheduledExecution.errors.allErrors.each {log.warn(it.defaultMessage)}
            log.warn("failed saving execution to history")
        }
    }
    def saveExecutionState( scheduledExecutionId, Map execMap) {
        def ScheduledExecution scheduledExecution = ScheduledExecution.get(scheduledExecutionId)
        if(!scheduledExecution){
            log.severe("Couldn't get ScheduledExecution with id: ${scheduledExecutionId}")
        }
        def Execution execution = new Execution(execMap)
        scheduledExecution.nextExecution = scheduledExecutionService.nextExecutionTime(scheduledExecution)
        scheduledExecution.addToExecutions(execution)
        if (execution.save(flush:true)) {
            log.info("saved execution status")
        } else {
            log.warn("failed to save execution status")
        }
        if (scheduledExecution.save(flush:true)) {
            log.info("added execution to history")
        } else {
            log.warn("failed saving execution to history")
        }
    }

    def generateTimestamp() {
        return new java.text.SimpleDateFormat("yyyyMMHHmmss").format(new Date())
    }
    private static long uIdCounter=0
    /**
     * Generate a string that will be different from the last call, uses a simple serial counter.
     */
    public static synchronized String generateUniqueId() {
        uIdCounter++
        return sprintf("%x",uIdCounter)
    }

    def File maybeCreateAdhocLogDir(Execution execution, Framework framework) {
        return maybeCreateAdhocLogDir(execution.project,framework)
    }


    /**
     * create a log dir for the ScheduledExecution job based on group and jobname
     */
    def File maybeCreateJobLogDir(ScheduledExecution job, Framework framework){
        def logdir = new File(Constants.getFrameworkLogsDir(framework.getBaseDir().getAbsolutePath()),
            "rundeck/${job.project}/${job.groupPath?job.groupPath+'/':''}${job.jobName}")

        if (!logdir.exists()) {
            log.info("Creating log dir: " + logdir.getAbsolutePath())
            logdir.mkdirs()
        }
        return logdir
    }
    def File maybeCreateLogDir(String project, String type, String name, Framework framework){
        def logdir = new File(Constants.getFrameworkLogsDir(framework.getBaseDir().getAbsolutePath()),
            "rundeck/${project}/deployments/${type}/${name}/logs")

        if (!logdir.exists()) {
            log.info("Creating log dir: " + logdir.getAbsolutePath())
            logdir.mkdirs()
        }
        return logdir
    }
    def File maybeCreateAdhocLogDir(String project, Framework framework){
        def logdir = new File(Constants.getFrameworkLogsDir(framework.getBaseDir().getAbsolutePath()),
            "rundeck/${project}/run/logs")

        if (!logdir.exists()) {
            log.info("Creating log dir: " + logdir.getAbsolutePath())
            logdir.mkdirs()
        }
        return logdir
    }



    /**
    * Generate an argString from a map of options and values
     */
    public static String generateArgline(Map<String,String> opts){
        StringBuffer sb = new StringBuffer()
        for (String key: opts.keySet().sort()) {
            String val = opts.get(key)
            if(val.contains(" ")){
                if(val.contains("\\")){
                    val = val.replaceAll("\\","\\\\")
                }
                if(val.contains("'")){
                    val = val.replaceAll("'","\\'")
                }
                if(sb.size()>0){
                    sb.append(" ")
                }
                sb.append("-").append(key).append(" ")

                sb.append("'").append(val).append("'")
            }else if(val){
                if(sb.size()>0){
                    sb.append(" ")
                }
                sb.append("-").append(key).append(" ")
                sb.append(val)
            }
        }
        return sb.toString()
    }

    /**
    * Generate an argString from a map of options and values
     */
    public static String generateJobArgline(ScheduledExecution sched,Map<String,Object> opts){
        HashMap<String,String> newopts = new HashMap<String,String>();
        for (String key: opts.keySet().sort()) {
            Object obj=opts.get(key)
            String val
            if (obj instanceof String[] || obj instanceof Collection) {
                //join with delimiter
                def opt = sched.options.find {it.name == key}
                if (opt && opt.delimiter) {
                    val = obj.grep {it}.join(opt.delimiter)
                } else {
                    val = obj.grep {it}.join(",")
                }
            }else{
                val = (String) obj
            }
            newopts[key]=val
        }
        return generateArgline(newopts)
    }
    /**
     * Returns a map of option names to values, from input parameters of the form "option.NAME"
     * @param params
     * @return
     */
    public static Map filterOptParams(Map params) {
        def result = [ : ]
        def optpatt = '^option\\.(.*)$'
        params.each { key, val ->
            def matcher = key =~ optpatt
            if (matcher.matches()) {
                def optname = matcher.group(1)
                if(val instanceof Collection){
                    result[optname] = new ArrayList(val).grep{it}
                }else if (val instanceof String[]){
                    result[optname] = new ArrayList(Arrays.asList(val)).grep{it}
                }else if(val instanceof String){
                    result[optname]=val
                }else{
                    System.err.println("unable to determine parameter value type: "+val + " ("+val.getClass().getName()+")")
                }
            }
        }
        return result
    }

    def int countNowRunning() {

        def total = Execution.createCriteria().count{
            and {
                isNull("dateCompleted")
            }
        };
        return total
    }
    def public static EXEC_FORMAT_SEQUENCE=['time','level','user','module','command','node','context']


    InterpreterResult interpretCommand(com.dtolabs.rundeck.core.execution.ExecutionContext executionContext,
                                       ExecutionItem executionItem, INodeEntry iNodeEntry) throws InterpreterException {
        if (!(executionItem instanceof JobExecutionItem)) {
            throw new InterpreterException("Unsupported item type: " + executionItem.getClass().getName());
        }
        def requestAttributes = RequestContextHolder.getRequestAttributes()
        boolean unbindrequest = false
        // outside of an executing request, establish a mock version
        if (!requestAttributes) {
            def servletContext = ServletContextHolder.getServletContext()
            def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
            requestAttributes = GrailsWebUtil.bindMockWebRequest(applicationContext)
            unbindrequest = true
        }
        def id
        //lookup job, create item, submit to ExecutionService
        JobExecutionItem jitem = (JobExecutionItem) executionItem
        try{

            def group = null
            def name = null
            def m = jitem.jobIdentifier =~ '^/?(.+)/([^/]+)$'
            if (m.matches()) {
                group = m.group(1)
                name = m.group(2)
            } else {
                name = jitem.jobIdentifier
            }
            def c = ScheduledExecution.createCriteria()
            def schedlist = c.list {
                and {
                    eq('jobName', name)
                    if (!group) {
                        or {
                            eq('groupPath', '')
                            isNull('groupPath')
                        }
                    } else {
                        eq('groupPath', group)
                    }
                    eq('project', executionContext.getFrameworkProject())
                }
            }
            if (schedlist && 1 == schedlist.size()) {
                id = schedlist[0].id
            }else{
                executionContext.getExecutionListener().log(0,"Job ref [${jitem.jobIdentifier}] invalid: No Unique Job found for name: ${name}, group: ${group}, project: ${executionContext.getFrameworkProject()}")
                throw new InterpreterException("Job ref [${jitem.jobIdentifier}] invalid: No Unique Job found for name: ${name}, group: ${group}, project: ${executionContext.getFrameworkProject()}")
            }
            def com.dtolabs.rundeck.core.execution.ExecutionContext newContext
            def WorkflowExecutionItem newExecItem
            ScheduledExecution.withTransaction{status->

                ScheduledExecution se = ScheduledExecution.get(id)//.findByJobNameAndGroupPath(name, group)
//                    se.refresh()
                //replace data context within arg string
                String[] newargs = jitem.args
                //try to set defaults for any missing args
                def newargstring = addArgStringOptionDefaults(se, newargs)
                final List<String> stringList = CLIUtils.splitArgLine(newargstring);
                newargs = stringList.toArray(new String[stringList.size()]);

                if (null != executionContext.dataContext && null != jitem.args) {
                    newargs = DataContextUtils.replaceDataReferences(jitem.args, executionContext.getDataContext())
                }
                //construct job data context
                def jobcontext = new HashMap<String, String>()
                jobcontext.id = se.id.toString()
                jobcontext.name = se.jobName
                jobcontext.group = se.groupPath
                jobcontext.project = se.project
                jobcontext.username = executionContext.getUser()
                newExecItem = createExecutionItemForExecutionContext(se, executionContext.getFramework(), executionContext.getUser())
                newContext= createContext(se, executionContext.getFramework(), executionContext.getUser(), jobcontext, executionContext.getExecutionListener(),newargs)

            }
            def WorkflowExecutionService service = executionContext.getFramework().getWorkflowExecutionService()

            final WorkflowExecutionResult result = service.getExecutorForItem(newExecItem).executeWorkflow(newContext, newExecItem)
            if(!result.isSuccess()){
                System.err.println("Job ref [${jitem.jobIdentifier}] failed: "+result);
            }
            return new InterpreterResultImpl(result)
        } finally {
            if (unbindrequest) {
                RequestContextHolder.setRequestAttributes (null)
            }
        }
    }

    ///////////////
      //for loading i18n messages
      //////////////

      /**
       * @parameter key
       * @returns corresponding value from messages.properties
       */
      def lookupMessage(String theKey, Object[] data, String defaultMessage=null) {
          def locale = getLocale()
          def theValue = null
          MessageSource messageSource = applicationContext.getBean("messageSource")
          try {
              theValue =  messageSource.getMessage(theKey,data,locale )
          } catch (org.springframework.context.NoSuchMessageException e){
              log.error "Missing message ${theKey}"
          } catch (java.lang.NullPointerException e) {
              log.error "Expression does not exist."
          }
          if(null==theValue && defaultMessage){
              MessageFormat format = new MessageFormat(defaultMessage);
              theValue=format.format(data)
          }
          return theValue
      }


      /**
       * Get the locale
       * @return locale
       * */
      def getLocale() {
          def Locale locale = null
          try {
              locale = RCU.getLocale(getSession().request)
          }
          catch(java.lang.IllegalStateException e){
              //log.debug "Running in console?"
          }
          //log.debug "locale: ${locale}"
          return locale
      }
      /**
       * Get the HTTP Session
       * @return session
       **/
      private HttpSession getSession() {
          return RequestContextHolder.currentRequestAttributes().getSession()
      }
}

/**
 * Exception thrown by the ExecutionService
 */
class ExecutionServiceException extends Exception{

    def ExecutionServiceException() {
    }
    def ExecutionServiceException(s) {
        super(s);
    }
    def ExecutionServiceException(s, throwable) {
        super(s, throwable);
    }
}


class ExecutionServiceValidationException extends ExecutionServiceException{

    Map<String,String> options;
    Map<String,String> errors;
    def ExecutionServiceValidationException() {
    }
    def ExecutionServiceValidationException(s,options,errors) {
        super(s);
        this.options=options;
        this.errors=errors;
    }
    def ExecutionServiceValidationException(s,options, errors,throwable) {
        super(s, throwable);
        this.options=options;
        this.errors=errors;
    }
    public Map<String,String> getOptions(){
        return options;
    }
    public Map<String,String> getErrors(){
        return errors;
    }
}

interface LogHandler {
    public BuildLogger getBuildLogger()
    public void publish(final LogRecord lr)
    public OutputStream createLoggerStream(Level level, String prefix);
    public int getMessageOutputLevel()
}
class LogOutputStream extends OutputStream{
    HtTableLogger logger;
    Level level;
    String prefix;
    StringBuffer sb;
    def LogOutputStream(HtTableLogger logger, Level level, String prefix){
        this.logger=logger;
        this.level=level;
        this.prefix=prefix;
        sb = new StringBuffer();
    }
    def boolean crchar=false;

    public void write(final int b) {
        if(b=='\n' ){
            logger.logOOB(level,null==prefix?sb.toString():prefix+sb.toString());
            sb = new StringBuffer()
            crchar=false;
        }else if(b=='\r'){
            crchar=true;
        }else{
            if (crchar){
                logger.logOOB(level,null==prefix?sb.toString():prefix+sb.toString());
                sb = new StringBuffer()
                crchar=false;
            }
            sb.append((char)b)
        }

    }
    public void flush(){
        if(sb.size()>0){
            logger.logOOB(level,null==prefix?sb.toString():prefix+sb.toString());
        }
    }
}
/**
  * HtTableLogger
  */
class HtTableLogger extends Handler implements LogHandler, BuildLogger, CLIToolLogger, ContextLogger {
    def PrintStream printstream
    def String namespace
    def File outfile
    def boolean closed=false
    def int msgOutputLevel
    def long startTime
    def Map defaultEntries=[:]

    def HtTableLogger(final String namespace, File outfile, int msglevel) {
        this(namespace,outfile,msglevel,null)
    }
    def HtTableLogger(final String namespace, File outfile, int msglevel, Map defaultEntries) {
        this.namespace = namespace
        this.outfile = outfile
        if(null!=defaultEntries){
            this.defaultEntries=new HashMap(defaultEntries)
        }
        printstream = new PrintStream(new FileOutputStream(outfile))
        msgOutputLevel=msglevel
        def Logger logger = Logger.getLogger(namespace)
        logger.addHandler(this);
        setFormatter(new HtFormatter())
    }

    void setMessageOutputLevel(int i){
        msgOutputLevel = i;

    }
    public int getMessageOutputLevel(){
        return msgOutputLevel;
    }
    public OutputStream createLoggerStream(final Level level, String prefix){
        return new LogOutputStream(this,level, prefix) ;
    }
    void setOutputPrintStream(java.io.PrintStream stream){

    }

    void setEmacsMode(boolean b){

    }

    void setErrorPrintStream(java.io.PrintStream stream){

    }

    public void taskStarted(final BuildEvent e) {
    }

    public void taskFinished(final BuildEvent e) {
    }

    public void targetStarted(final BuildEvent e) {
        log(Level.CONFIG, e.getMessage());
    }

    public void targetFinished(final BuildEvent e) {
        log(Level.CONFIG, e.getMessage());
    }

    public void buildStarted(final BuildEvent e) {
        startTime = System.currentTimeMillis();
        log(Level.CONFIG, e.getMessage());
    }

    private String lSep = System.getProperty("line.separator");
    public void buildFinished(final BuildEvent event) {
        final Throwable error = event.getException();
        final StringBuffer message = new StringBuffer();


        if (error == null) {
//            log(Level.CONFIG, "Command successful. " + org.apache.tools.ant.util.DateUtils.formatElapsedTime(System.currentTimeMillis() - startTime));
        } else {

            message.append("Command failed.");
            message.append(lSep);

            if (Project.MSG_VERBOSE <= msgOutputLevel || !(error instanceof BuildException)) {
                message.append(org.apache.tools.ant.util.StringUtils.getStackTrace(error));
            } else {
                message.append(error.toString()).append(lSep);
            }
            log(Level.SEVERE, message.toString());
        }

        //close();
    }


    public void messageLogged(final BuildEvent event) {
        final String msg = event.getMessage();
        if (msg == null || msg.length() == 0) {
            return;
        }

        final int priority = event.getPriority();
        // Filter out messages based on priority
        if (priority <= msgOutputLevel) {
            def data = [:]
            if (msg.startsWith("[")) {
                data = parseLogDetail(event.getMessage())
                if (data) {
                    msg = data.rest ? data.rest : msg
                }
            }
            log(getLevelForPriority(priority), msg, data ? data : defaultEntries);
        }
    }
    /**
     * Converts an ant Priority (from {@link org.apache.tools.ant.Project} class) into a logging Level value.
     */
    public static Level getLevelForPriority(final int priority){
        switch(priority){
            case Project.MSG_ERR:
            return Level.SEVERE
            case Project.MSG_WARN:
            return Level.WARNING
            case Project.MSG_INFO:
            return Level.INFO
            case Project.MSG_VERBOSE:
            return Level.CONFIG
            case Project.MSG_DEBUG:
            return Level.FINEST
            default:
            return Level.WARNING
        }
    }
    /**
     * Converts an ant Priority (from {@link org.apache.tools.ant.Project} class) into a logging Level value.
     */
    public static Level getLevelForString(final String level){
        switch(level){
            case "ERROR":
            return Level.SEVERE
            case "WARN":
            return Level.WARNING
            case "INFO":
            return Level.INFO
            case "VERBOSE":
            return Level.CONFIG
            case "DEBUG":
            return Level.FINEST
            default:
            return Level.WARNING
        }
    }

    /**
     * Matches the outer most context message:
     * [context][level] *message
     */
    def static outerre1 = /(?x) ^\[   ([^\]]+)  \]  \[  ([^\]\s]+)  \] \s* (.*)    $/
    /**
    * Matches simple context:
     * user@node .*
     */
    def static userre = /(?x) ^  ([^@\]]+) @ ([^\s\]]+) (.*) $/
    def static cmdctxre = /(?x)  ^  \s*  (  [^.\]\s]+  \.  [^.\]\s]+  (\.[^\]\s]+)? )  \s+  ([^\]]+)  $/
    def static ctexecctxre = /(?x)  ^  \s* ([^\]]+)  $/
    public static Map parseLogDetail(final String output){
        def matcher1= output=~outerre1
        def map=[:]
        if(matcher1.matches()){
            def ctxInfo=matcher1.group(1)
            def level=matcher1.group(2)
            def rest=matcher1.group(3)
            def umatcher= ctxInfo=~userre
            if(umatcher.matches()){

                map['user']=umatcher.group(1)
                map['node']=umatcher.group(2)
                def restctx=umatcher.group(3)
                def matcher=restctx=~cmdctxre
                def matcher2=restctx=~ctexecctxre
                if(matcher.matches()){
                    map['context']=matcher.group(1)
                    map['command']=matcher.group(3)
                }else if(matcher2.matches()){
                    map['command']=matcher2.group(1)
                }
            }
            map['level']=level
            map['rest']=rest
        }
        return map
    }

    /**
     * Logs build output to a java.util.logging.Logger.
     *
     * @param message Message to log. <code>null</code> messages are not logged,
     *                however, zero-length strings are.
     */
    public void log(final String message) {
        logOOB(Level.WARNING, message);
    }
    /**
     * Logs build output to a java.util.logging.Logger.
     *
     * @param message Message being logged. <code>null</code> messages are not
     *                logged, however, zero-length strings are.
     * @param level   the log level
     */
    public void log(final Level level, final String message) {
        if (message == null) {
            return;
        }

        // log the message
        final LogRecord record = new LogRecord(level, message);
        publish(record);
    }
    /**
     * Logs build output to a java.util.logging.Logger.
     *
     * @param message Message being logged. <code>null</code> messages are not
     *                logged, however, zero-length strings are.
     * @param level   the log level
     */
    public void logOOB(final Level level, final String message) {
        if (message == null) {
            return;
        }

        String xmessage=message
        Level xlevel=level
        // log the message
        def Map data = parseLogDetail(message)
        if(data){
            xmessage=data.rest
            if(data.level){
                xlevel = getLevelForString(data.level)
            }
        }else{
            //output was to console from stderr/stdout
            data = defaultEntries
        }

        final LogRecord record = new LogRecord(xlevel, xmessage);


        if(data){
            publish(record,data);
        }else{
            publish(record);
        }

    }

    public static String makeContextId(final Map data){
        return "${data.level}:${data.user}:${data.node}:${data.context}:${data.command}"
    }
    /**
     * Logs build output to a java.util.logging.Logger.
     *
     * @param message Message being logged. <code>null</code> messages are not
     *                logged, however, zero-length strings are.
     * @param level   the log level
     * @param data the contextual data
     */
    public void log(final Level level, final String message, final Map data) {
        if (message == null) {
            return;
        }
        // log the message
        final LogRecord record = new LogRecord(level, message);
        if(data){
            publish(record,data);
        }else if(defaultEntries){
            publish(record,defaultEntries);
        }else{
            publish(record);
        }
    }
    public BuildLogger getBuildLogger() {
        return this
    }
    public void publish(final LogRecord lr) {
        if (lr.getMessage() == null) {
            return;
        }
        if(closed){
            return;
        }
        if(lr.getLevel().intValue()>=getLevelForPriority(msgOutputLevel).intValue()){
            printstream.println(getFormatter().format(lr))
        }
    }
    public void publish(final LogRecord lr,final Map data) {
        if (lr.getMessage() == null) {
            return;
        }
        if(closed){
            return;
        }
        if(lr.getLevel().intValue()>=getLevelForPriority(msgOutputLevel).intValue()){
            printstream.println(getHtFormatter().format(lr,data))
        }
    }
    public HtFormatter getHtFormatter(){
        return (HtFormatter)getFormatter()
    }
    public void close() {
        if(!closed ){
            closed=true;
            if(null!=getFormatter()){
                printstream.println (getFormatter().getTail(this))
            }
            flush()

            this.printstream.close()
            def Logger logger = Logger.getLogger(namespace)
            logger.removeHandler(this);
        }
    }
    public void flush() {
        this.printstream.flush()
    }

    public void error(String s) {
        logOOB(getLevelForString("ERROR"),s)

    }

    public void warn(String s) {
        logOOB(getLevelForString("WARN"),s)

    }

    public void verbose(String s) {
        logOOB(getLevelForString("VERBOSE"),s)
    }

    void log(String s, Map<String, String> data) {
        log(Level.WARNING,s,data)
    }

    void error(String s, Map<String, String> data) {
        log(getLevelForString("ERROR"), s, data)

    }

    void warn(String s, Map<String, String> data) {
        log(getLevelForString("WARN"), s, data)

    }

    void verbose(String s, Map<String, String> data) {
        log(getLevelForString("VERBOSE"), s, data)

    }

    void debug(String s, Map<String, String> data) {
        log(getLevelForString("DEBUG"), s, data)

    }

    void debug(String s) {
        logOOB(getLevelForString("DEBUG"), s)

    }
}
    
class HtFormatter extends java.util.logging.Formatter{
    public HtFormatter(){
        
    }
    def SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss");
    public String format(LogRecord record,Map data){

        def Date d = new Date(record.getMillis());
        def String dDate = fmt.format(d);
        String dMesg = record.getMessage();
        while(dMesg.endsWith('\r')){
            dMesg = dMesg.substring(0,dMesg.length()-1)
        }
        StringBuffer sb = new StringBuffer()
        sb.append('^^^')
        //date
        sb.append(dDate).append('|')
        //level
        sb.append(record.getLevel()).append("|")

        //sequence
        for(def i =2;i<ExecutionService.EXEC_FORMAT_SEQUENCE.size();i++){
            if(null==data[ExecutionService.EXEC_FORMAT_SEQUENCE[i]]){
                sb.append('|')
            }else{
                sb.append(data[ExecutionService.EXEC_FORMAT_SEQUENCE[i]]).append('|')
            }
        }
        //mesg
        sb.append(dMesg)
        //end
        sb.append('^^^')

        return sb.toString()
    }
    public String format(LogRecord record){

        def Date d = new Date(record.getMillis());
        def String dDate = fmt.format(d);
        String dMesg = record.getMessage();
        while(dMesg.endsWith('\r')){
            dMesg = dMesg.substring(0,dMesg.length()-1)
        }

        return '^^^'+dDate+"|"+record.getLevel()+"|"+dMesg+'^^^'
    }
    public String getHead(Handler h){
        return "";
    }
    public String getTail(Handler h){
        return '^^^END^^^';
    }
}
