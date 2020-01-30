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

package rundeck.services

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.internal.logging.LogFlusher
import com.dtolabs.rundeck.app.internal.workflow.MultiWorkflowExecutionListener
import com.dtolabs.rundeck.app.support.*
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.workflow.*
import com.dtolabs.rundeck.core.execution.workflow.steps.*
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.logging.*
import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.execution.JobExecutionItem
import com.dtolabs.rundeck.execution.JobRefCommand
import com.dtolabs.rundeck.execution.JobReferenceFailureReason
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.ToString
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.apache.log4j.MDC
import org.grails.web.json.JSONObject
import org.hibernate.StaleObjectStateException
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.type.StandardBasicTypes
import org.rundeck.app.components.jobs.JobQuery
import org.rundeck.core.auth.AuthConstants
import org.rundeck.storage.api.StorageException
import org.rundeck.util.Sizes
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.dao.DuplicateKeyException
import org.springframework.transaction.annotation.Propagation
import org.springframework.validation.ObjectError
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils as RCU
import rundeck.*
import rundeck.services.events.ExecutionCompleteEvent
import rundeck.services.events.ExecutionPrepareEvent
import rundeck.services.logging.ExecutionLogWriter
import rundeck.services.logging.LoggingThreshold

import javax.annotation.PreDestroy
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.nio.charset.Charset
import java.sql.Time
import java.text.DateFormat
import java.text.MessageFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Coordinates Command executions via Ant Project objects
 */
@Transactional
class ExecutionService implements ApplicationContextAware, StepExecutor, NodeStepExecutor, EventPublisher {
    static Logger executionStatusLogger = Logger.getLogger("org.rundeck.execution.status")

    def FrameworkService frameworkService
    def notificationService
    def ScheduledExecutionService scheduledExecutionService
    def ReportService reportService
    def LoggingService loggingService
    def WorkflowService workflowService
    def StorageService storageService

    def ThreadBoundOutputStream sysThreadBoundOut
    def ThreadBoundOutputStream sysThreadBoundErr
    def String defaultLogLevel

    def ApplicationContext applicationContext
    def metricService
    def apiService
    def grailsLinkGenerator
    def logFileStorageService
    MessageSource messageSource
    def jobStateService
    def rundeckNodeService
    def grailsApplication
    def configurationService
    def executionUtilService
    def fileUploadService
    def pluginService
    def executorService
    JobLifecyclePluginService jobLifecyclePluginService
    def executionLifecyclePluginService

    static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMAT_WITH_MS_XXX =
        new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
			}
		}
    static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMAT_XXX =
        new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
			}
		}
    static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMAT_WITH_MS =
        new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
			}
		}
    static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMAT =
        new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
			}
		}

    boolean getExecutionsAreActive(){
        configurationService.executionModeActive
    }

    void setExecutionsAreActive(boolean active){
        configurationService.executionModeActive=active

        if(!active){
            log.info("Rundeck changed to PASSIVE mode: No executions can be run.")
        }else{
            log.info("Rundeck changed to ACTIVE: executions can be run.")
        }
        if(active){
            scheduledExecutionService.rescheduleJobs(frameworkService.isClusterModeEnabled()?frameworkService.getServerUUID():null)
        }else{
            scheduledExecutionService.unscheduleJobs(frameworkService.isClusterModeEnabled()?frameworkService.getServerUUID():null)
        }
    }

    private boolean applicationIsShutdown

    public boolean isApplicationShutdown() {
        return applicationIsShutdown
    }

    @PreDestroy
    void cleanUp() {
        applicationIsShutdown = true;
    }


    void initialize(){
        //TODO: use plugin registry instead
        //ExecutionService handles Job reference steps
        final cis = frameworkService.rundeckFramework.stepExecutionService
        cis.providerRegistryService.registerInstance(JobExecutionItem.STEP_EXECUTION_TYPE, this)
        //ExecutionService handles Job reference node steps
        final nis = frameworkService.rundeckFramework.nodeStepExecutorService
        nis.providerRegistryService.registerInstance(JobExecutionItem.STEP_EXECUTION_TYPE, this)
    }
    /**
     * Render execution document for api response
     */

    public def respondExecutionsXml(HttpServletRequest request,HttpServletResponse response, List<Execution> executions, paging = [:]) {
        def apiv14=request.api_version>=ApiVersions.V14
        return apiService.respondExecutionsXml(request,response,executions.collect { Execution e ->
                def data=[
                        execution: e,
                        href: apiv14?apiService.apiHrefForExecution(e):apiService.guiHrefForExecution(e),
                        status: getExecutionState(e),
                        summary: summarizeJob(e.scheduledExecution, e)
                ]
                if(apiv14){
                    data.permalink=apiService.guiHrefForExecution(e)
                }
            if(e.customStatusString){
                data.customStatus=e.customStatusString
            }
                if(e.retryExecution) {
                    data.retryExecution = [
                            id    : e.retryExecution.id,
                            href  : apiv14 ? apiService.apiHrefForExecution(e.retryExecution) :
                                    apiService.guiHrefForExecution(e.retryExecution),
                            status: getExecutionState(e.retryExecution),
                    ]
                    if (apiv14) {
                        data.retryExecution.permalink = apiService.guiHrefForExecution(e.retryExecution)
                    }
                }
                data
            }, paging)
    }
    public def respondExecutionsJson(HttpServletRequest request,HttpServletResponse response, List<Execution> executions, paging = [:]) {
        return apiService.respondExecutionsJson(request,response,executions.collect { Execution e ->
                def data=[
                        execution: e,
                        permalink: apiService.guiHrefForExecution(e),
                        href: apiService.apiHrefForExecution(e),
                        status: getExecutionState(e),
                        summary: summarizeJob(e.scheduledExecution, e)
                ]
            if(e.customStatusString){
                data.customStatus=e.customStatusString
            }
                if(e.retryExecution){
                    data.retryExecution=[
                            id:e.retryExecution.id,
                            permalink: apiService.guiHrefForExecution(e.retryExecution),
                            href: apiService.apiHrefForExecution(e.retryExecution),
                            status: getExecutionState(e.retryExecution),
                    ]
                }
                data
            }, paging)
    }

    /**
     * Render xml or json response data for the result of a call to {@link #deleteBulkExecutionIds(java.util.Collection, com.dtolabs.rundeck.core.authorization.AuthContext, java.lang.String)}
     *
     * @param request
     * @param response
     * @param result
     * @return
     */
    def renderBulkExecutionDeleteResult(request, response, result) {
        def respFormat = apiService.extractResponseFormat(request, response, ['xml', 'json'])
        def total = result.successTotal + result.failures.size()
        switch (respFormat) {
            case 'json':
                return apiService.renderSuccessJson(response) {
                    requestCount = total
                    allsuccessful = result.successTotal == total
                    successCount = result.successTotal
                    failedCount = result.failures ? result.failures.size() : 0
                    if (result.failures) {
                        failures = result.failures.collect { [message: it.message, id: it.id] }
                    }
                }
                break
            case 'xml':
            default:
                return apiService.renderSuccessXml(request, response) {
                    deleteExecutions(requestCount: total, allsuccessful: result.successTotal == total) {
                        successful(count: result.successTotal)
                        if (!result.success) {
                            failed(count: result.failures.size()) {
                                result.failures.each { failure ->
                                    delegate.'execution'(id: failure.id, message: failure.message)
                                }
                            }
                        }
                    }
                }
                break;
        }
    }


    def listLastExecutionsPerProject(AuthContext authContext, int max=5){
        def projects = frameworkService.projectNames(authContext)

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
     * Return the last execution id in the project
     * @param query
     * @return
     */
    def lastExecutionId(String project) {
        def execs = Execution.createCriteria().get {
            eq('project', project)
            order('dateCompleted', 'desc')
            maxResults(1)
            projections {
                property('id')
            }
        }
        return execs ?: null
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
                proj: 'project',
        ]
        def txtfilters = [
            obj:'name',
            type:'type',
            cmd:'command',
            user:'user',
        ]
        def schedTxtFilters= [
            job:'jobName',
        ]
        def schedPathFilters=[
            groupPath: 'groupPath'
        ]
        def schedExactFilters= [
            groupPathExact:'groupPath',
            jobId:'uuid'
        ]
        def schedFilterKeys= (schedExactFilters.keySet() + schedTxtFilters.keySet() + schedPathFilters.keySet())

        def filters = [ :]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)

        def Date endAfterDate = new Date(System.currentTimeMillis()-1000*60*60)
        if(query && query.doendafterFilter){
            endAfterDate = query.endafterFilter
        }
        def Date nowDate = new Date()

        def allProjectsQuery=query.projFilter=='*';
        def multiProjectsQuery = query.projFilter?.indexOf(',') > 0

        def crit = Execution.createCriteria()
        def runlist = crit.list{
            if(query?.max){
                maxResults(query.max.toInteger())
            }else{
                maxResults(grailsApplication.config.rundeck?.pagination?.default?.max ?
                                   grailsApplication.config.rundeck.pagination.default.max.toInteger() :
                                   20 )
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
                if(!allProjectsQuery){

                    eqfilters.each { filtPrefix, fieldName ->
                        if (query["${filtPrefix}Filter"]) {
                            if (!multiProjectsQuery) {
                                eq(fieldName, query["${filtPrefix}Filter"])
                            } else {
                                or {
                                    query["${filtPrefix}Filter"].split(/,/).each { xval ->
                                        eq(fieldName, xval.trim())
                                    }
                                }
                            }
                        }
                    }
                }

                //running status filter.
                if (query.runningFilter) {
                    Date now = new Date()
                    if (EXECUTION_SCHEDULED == query.runningFilter ) {
                        eq('status', EXECUTION_SCHEDULED)
                    } else if ('running' == query.runningFilter) {
                        and{
                            isNull('dateCompleted')
                            if(!query.considerPostponedRunsAsRunningFilter){
                                le('dateStarted', now)
                                ne('status', EXECUTION_SCHEDULED)
                            }
                        }
                    } else {
                        and {
                            eq('status', 'completed' == query.runningFilter ? 'true' : 'false')
                            eq('cancelled', 'killed' == query.runningFilter ? 'true' : 'false')
                            isNotNull('dateCompleted')
                        }
                    }
                 }
                 def schedfilts=[:]
                 schedFilterKeys.each {
                     if( query["${it}Filter"]){
                         schedfilts[it]= query["${it}Filter"]
                     }
                 }
                 if(schedfilts.groupPath=='*'){
                     schedfilts.remove('groupPath')
                 }
                 if(schedfilts){
                     if(schedfilts['jobId'] == 'null'){
                         isNull('scheduledExecution')
                     }else{
                         scheduledExecution {
                             schedExactFilters.each{key,v->
                                 if (query["${key}Filter"] == 'null') {
                                     isNull(v)
                                 } else if (query["${key}Filter"] == '!null') {
                                     isNotNull(v)
                                 } else if (query["${key}Filter"]) {
                                     eq(v, query["${key}Filter"] )
                                 }
                             }
                             schedTxtFilters.each{key,v->
                                 if (query["${key}Filter"]) {
                                     ilike(v, '%'+query["${key}Filter"] + '%')
                                 }
                             }
                             schedPathFilters.each{key,v->
                                 if (query["${key}Filter"]) {
                                     ilike(v, query["${key}Filter"] + '%')
                                 }
                             }
                         }
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

                 if(!allProjectsQuery){
                     eqfilters.each { filtPrefix, fieldName ->
                         if (query["${filtPrefix}Filter"]) {
                             if (!multiProjectsQuery) {
                                 eq(fieldName, query["${filtPrefix}Filter"])
                             } else {
                                 or {
                                     query["${filtPrefix}Filter"].split(/,/).each { xval ->
                                         eq(fieldName, xval.trim())
                                     }
                                 }
                             }
                         }
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

                 def schedfilts = [:]
                 schedFilterKeys.each {
                     if (query["${it}Filter"]) {
                         schedfilts[it] = query["${it}Filter"]
                     }
                 }
                 if (schedfilts.groupPath == '*') {
                     schedfilts.remove('groupPath')
                 }
                 if (schedfilts) {
                     if (schedfilts['jobId'] == 'null') {
                         isNull('scheduledExecution')
                     } else {
                         scheduledExecution {
                             schedExactFilters.each { key, v ->
                                 if (query["${key}Filter"] == 'null') {
                                     isNull(v)
                                 } else if (query["${key}Filter"] == '!null') {
                                     isNotNull(v)
                                 } else if (query["${key}Filter"]) {
                                     eq(v, query["${key}Filter"])
                                 }
                             }
                             schedTxtFilters.each { key, v ->
                                 if (query["${key}Filter"]) {
                                     ilike(v, '%' + query["${key}Filter"] + '%')
                                 }
                             }
                             schedPathFilters.each { key, v ->
                                 if (query["${key}Filter"]) {
                                     ilike(v, query["${key}Filter"] + '%')
                                 }
                             }
                         }
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

    def public finishQueueQuery(query,params,model){

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
     * Set the result status to FAIL for any Executions that are not complete,
     * excludes executions which are still scheduled and haven't started.
     *
     * @param serverUUID if not null, only match executions assigned to the given serverUUID
     */
    def cleanupRunningJobsAsync(String serverUUID = null, String status = null, Date before = new Date()) {
        def executionIds = Execution.withCriteria {
            isNotNull('dateStarted')
            isNull('dateCompleted')
            if (serverUUID == null) {
                isNull('serverNodeUUID')
            } else {
                eq('serverNodeUUID', serverUUID)
            }
            lt('dateStarted', before)
            or{
                isNull('status')
                ne('status', EXECUTION_SCHEDULED)
            }

            projections {
                property('id')
            }
        }
        executorService.submit {
            def found = executionIds.collect { Execution.get(it) }
            cleanupRunningJobs(found, status)
        }
    }

    /**
     * Find currently running executions. Excludes executions which are scheduled
     * to run but have not started yet.
     *
     * @param   serverUUID  if not null, only match executions assigned to the given server UUID
     */
    private List<Execution> findRunningExecutions(String serverUUID = null, Date before=new Date()) {
        return Execution.withCriteria{
            isNotNull('dateStarted')
            isNull('dateCompleted')
            if (serverUUID == null) {
                isNull('serverNodeUUID')
            } else {
                eq('serverNodeUUID', serverUUID)
            }
            lt('dateStarted', before)
            or{
                isNull('status')
                ne('status', EXECUTION_SCHEDULED)
            }
        }
    }
    /**
     * Set the result status to FAIL for any Executions that are not complete
     * @param serverUUID if not null, only match executions assigned to the given serverUUID
     */
    def cleanupRunningJobs(String serverUUID = null, String status = null, Date before = new Date()) {
        cleanupRunningJobs(findRunningExecutions(serverUUID, before), status)
    }

    /**
     * Set the result status to FAIL for any Executions that are not complete
     * @param serverUUID if not null, only match executions assigned to the given serverUUID
     */
    def cleanupRunningJobs(List<Execution> found, String status = null) {
        found.each { Execution e ->
            cleanupExecution(e, status)
            log.error("Stale Execution cleaned up: [${e.id}] in ${e.project}")
            metricService.markMeter(this.class.name, 'executionCleanupMeter')
        }
    }

    private void cleanupExecution(Execution e, String status = null) {
        saveExecutionState(
                e.scheduledExecution?.id,
                e.id,
                [
                        status       : status ?: String.valueOf(false),
                        dateCompleted: new Date(),
                        cancelled    : !status
                ],
                null,
                null
        )

    }

    /**
     * Log details about execution start and finish to a log4j listener named org.rundeck.execution.status
     * @param e
     * @param event @return @param user
     */
    def logExecutionLog4j(Execution e, String event, String user) {
        def (Map mdcprops, String message) = createExecutionLogMDCdata(e)
        mdcprops.event= event
        mdcprops.eventUser= user
        logExecutionLog4jState(mdcprops, event+" ("+ user+"): "+ message)
    }

    protected void logExecutionLog4jState(Map mdcprops, String message) {
        mdcprops.each MDC.&put
        executionStatusLogger.info(message)
        mdcprops.keySet().each(MDC.&remove)
    }

    protected List createExecutionLogMDCdata(Execution e) {
        def state = getExecutionState(e)
        def execprops = ['user', 'id', 'abortedby', 'dateStarted', 'dateCompleted', 'project', 'argString']
        def jobProps = ['uuid', 'jobName', 'groupPath']
        Map mdcprops=[:]
        execprops.each { k ->
            def v = e[k]
            if (v instanceof Date) {
                //TODO: reformat date
                mdcprops.put(k, v.toString())
                mdcprops.put("${k}Time", v.time.toString())
            } else if (v instanceof String) {
                mdcprops.put(k, v ? v : "-")
            } else {
                mdcprops.put(k, v ? v.toString() : "-")
            }
        }
        mdcprops.put('state', state)
        final jobstring = ''
        if (e.scheduledExecution) {
            jobProps.each { k ->
                final var = e.scheduledExecution[k]
                mdcprops.put(k, var ? var : '-')
            }
            jobstring = " job: " + e.scheduledExecution.extid + " " + (e.scheduledExecution.groupPath ?: '') + "/" +
                    e.scheduledExecution.jobName
        } else {
            def adhocCommand = e.workflow.commands.size()==1 && (e.workflow.commands[0] instanceof CommandExec) && e.workflow.commands[0].adhocRemoteString
            if (adhocCommand) {
                mdcprops.put("command", adhocCommand)
                jobstring += " command: " + adhocCommand
            }
            jobProps.each{k->
                mdcprops[k]='-'
            }
        }
        [mdcprops, "id: " + e.id +" state: " + state +  " project: " + e.project + " user: " + e.user + jobstring]
    }

    public logExecution(uri,project,user,issuccess,statusString,execId,Date startDate=null, jobExecId=null, jobName=null,
                        jobSummary=null,iscancelled=false,istimedout=false,willretry=false, nodesummary=null,
                        abortedby=null, succeededNodeList=null, failedNodeList=null, filter=null){

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
            reportMap.adhocExecution = false
        }else{
            reportMap.reportId='adhoc'
            reportMap.adhocExecution = true
        }

        if(succeededNodeList){
            reportMap.succeededNodeList = succeededNodeList
        }
        if(failedNodeList){
            reportMap.failedNodeList = failedNodeList
        }
        if(filter){
            reportMap.filterApplied = filter
        }
        reportMap.ctxProject=project

        if(iscancelled && abortedby){
            reportMap.abortedByUser=abortedby
        }else if(iscancelled){
            reportMap.abortedByUser=user
        }
        reportMap.author=user
        reportMap.title= jobSummary?jobSummary:"Rundeck Job Execution"

        def statusMap =[
                true:'succeed',
                (EXECUTION_SUCCEEDED):'succeed',
                (EXECUTION_ABORTED):'cancel',
                (EXECUTION_FAILED_WITH_RETRY):'retry',
                (EXECUTION_TIMEDOUT):'timeout',
                (EXECUTION_FAILED):'fail',
                false:'fail',
        ]
        reportMap.status= statusMap[statusString]?:'other'
        reportMap.node= null!=nodesummary?nodesummary: frameworkService.getFrameworkNodeName()

        reportMap.message=(statusString? "Job status ${statusString}" : issuccess?'Job completed successfully':iscancelled?('Job killed by: '+(abortedby?:user)):
            willretry?'Job Failed (will retry)':istimedout?'Job timed out':'Job failed')
        reportMap.dateCompleted=new Date()
        def result=reportService.reportExecutionResult(reportMap)
        if(result.error){
            log.error("Failed to create report: "+result.report.errors.allErrors.collect{it.toString()}).join("; ")
        }
    }

    def static HashMap<String, String> exportContextForExecution(Execution execution, LinkGenerator grailsLinkGenerator) {
        def jobcontext = new HashMap<String, String>()
        if (execution.scheduledExecution) {
            jobcontext.name = execution.scheduledExecution.jobName
            jobcontext.group = execution.scheduledExecution.groupPath
            jobcontext.id = execution.scheduledExecution.extid
            jobcontext.successOnEmptyNodeFilter=execution.scheduledExecution.successOnEmptyNodeFilter?"true":"false"
        }
        if (execution.filter) {
            jobcontext.filter = execution.filter
        }
        jobcontext.execid = execution.id.toString()
        jobcontext.executionType = execution.executionType
        jobcontext.serverUrl = generateServerURL(grailsLinkGenerator)
        jobcontext.url = generateExecutionURL(execution,grailsLinkGenerator)
        jobcontext.serverUUID = execution.serverNodeUUID
        jobcontext.username = execution.user
        jobcontext['user.name'] = execution.user
        jobcontext.project = execution.project
        jobcontext.loglevel = textLogLevels[execution.loglevel] ?: execution.loglevel
        jobcontext.retryAttempt=Integer.toString(execution.retryAttempt?:0)
        if(execution.retryAttempt>0 && execution.retryOriginalId!=null){
            jobcontext.retryInitialExecId=Long.toString(execution.retryOriginalId)
        }else{
            jobcontext.retryInitialExecId="0"
        }
        if(execution.retryAttempt>0 && execution.retryPrevId!=null){
            jobcontext.retryPrevExecId=Long.toString(execution.retryPrevId)
        }else{
            jobcontext.retryPrevExecId="0"
        }
        jobcontext.wasRetry=Boolean.toString(execution.retryAttempt?true:false)
        jobcontext.threadcount=Integer.toString(execution.nodeThreadcount?:1)
        jobcontext
    }

    static String generateExecutionURL(Execution execution,LinkGenerator grailsLinkGenerator) {
        grailsLinkGenerator.link(controller: 'execution', action: 'follow', id: execution.id, absolute: true,
                params: [project: execution.project])
    }
    static String generateServerURL(LinkGenerator grailsLinkGenerator) {
        grailsLinkGenerator.link(controller: 'menu', action: 'index', absolute: true)
    }

    /**
     * starts an execution in a separate thread, returning a map of [thread:Thread, loghandler:LogHandler, threshold:Threshold]
     */
    def Map executeAsyncBegin(
            Framework framework,
            UserAndRolesAuthContext authContext,
            Execution execution,
            ScheduledExecution scheduledExecution = null,
            Map extraParams = null,
            Map extraParamsExposed = null,
            int retryAttempt = 0
    ) {
        //TODO: method can be transactional readonly
        metricService.markMeter(this.class.name,'executionStartMeter')
        execution.refresh()
        //set up log output threshold
        def thresholdMap = ScheduledExecution.parseLogOutputThreshold(scheduledExecution?.logOutputThreshold)
        def threshold = LoggingThreshold.fromMap(thresholdMap,scheduledExecution?.logOutputThresholdAction)

        def ExecutionLogWriter loghandler= loggingService.openLogWriter(
                execution,
                logLevelForString(execution.loglevel),
                [user:execution.user, node: framework.getFrameworkNodeName()],
                threshold
        )
        execution.outputfilepath = loghandler.filepath?.getAbsolutePath()
        execution.save(flush:true)
        if(execution.scheduledExecution){
            metricService.markMeter(this.class.name,'executionJobStartMeter')
        }else{
            metricService.markMeter(this.class.name,'executionAdhocStartMeter')
        }
        try{
            def jobcontext=exportContextForExecution(execution,grailsLinkGenerator)
            loghandler.openStream()

            //manages workflow step+node context data
            ContextManager contextmanager = new ContextManager()
            ContextLogWriter directLogWriter = new ContextLogWriter(loghandler)
            LoggerWithContext directLogger = new LoggerWithContext(directLogWriter, contextmanager)


            //can create contexts for using log filter plugins
            def logFilterPluginLoader = pluginService.createSimplePluginLoader(
                    execution.project,
                    framework,
                    pluginService.getRundeckPluginRegistry().createPluggableService(LogFilterPlugin)
            )

            //Root level override allows plugins to filter all output, even outside a workflow step (e.g.
            // debug output)
            def rootoverride = new OverridableStreamingLogWriter(loghandler)

            def globalConfig = getGlobalPluginConfigurations(execution.project)

            def rootLogManager = new LoggingManagerImpl(
                    rootoverride,
                    directLogger,
                    logFilterPluginLoader,
                    globalConfig
            )



            //workflow level override selectively overrides logging with per-step plugin configurations
            def workflowoverride = new OverridableStreamingLogWriter(rootoverride)

            LoggerWithContext workflowlogger = new LoggerWithContext(
                    new ContextLogWriter(workflowoverride),
                    contextmanager
            )
            def workflowLogManager = new LoggingManagerImpl(
                    workflowoverride,
                    directLogger,
                    logFilterPluginLoader,
                    scheduledExecution ?
                            ExecutionUtilService.createLogFilterConfigs(
                                    execution.workflow.getPluginConfigDataList(ServiceNameConstants.LogFilter)
                            ) + globalConfig :
                            globalConfig
            )


            NodeRecorder recorder = new NodeRecorder()

            //create listener to handle log messages
            WorkflowExecutionListenerImpl executionListener = new WorkflowExecutionListenerImpl(
                    recorder,
                    workflowlogger
            );

            WorkflowExecutionListener execStateListener = workflowService.createWorkflowStateListenerForExecution(
                    execution,
                    framework,
                    authContext,
                    jobcontext,
                    extraParamsExposed
            )
            def checkpoint = logFileStorageService.createPeriodicCheckpoint(execution)

            def wfEventListener = new WorkflowEventLoggerListener(executionListener)
            def logOutFlusher = new LogFlusher()
            def logErrFlusher = new LogFlusher()
            def multiListener = MultiWorkflowExecutionListener.create(
                    executionListener, //delegate for ExecutionListener
                    [
                            contextmanager,
                            executionListener, //manages context for logging
                            wfEventListener, //emits state change events to log
                            execStateListener, //updates WF execution state model
                            logOutFlusher, //flushes stdout output after node steps
                            logErrFlusher, //flush stderr output after node steps
                            /*new EchoExecListener() */
                    ]
            )

            def secureOptionNodeDeferred = [:]
            if(scheduledExecution) {
                if(!extraParamsExposed){
                    extraParamsExposed=[:]
                }
                if(!extraParams){
                    extraParams=[:]
                }
                Map<String, String> args = FrameworkService.parseOptsFromString(execution.argString)
                loadSecureOptionStorageDefaults(scheduledExecution, extraParamsExposed, extraParams, authContext, true,
                        args, jobcontext, secureOptionNodeDeferred)
            }
            String inputCharset=frameworkService.getDefaultInputCharsetForProject(execution.project)

            StepExecutionContext executioncontext = createContext(
                    execution,
                    null,
                    framework,
                    authContext,
                    execution.user,
                    jobcontext,
                    multiListener,
                    multiListener,
                    null,
                    extraParams,
                    extraParamsExposed,
                    inputCharset,
                    workflowLogManager,
                    secureOptionNodeDeferred
            )

            fileUploadService.executionBeforeStart(
                    new ExecutionPrepareEvent(
                            execution: execution,
                            job: scheduledExecution,
                            context: executioncontext
                    )
            )


            logExecutionLog4j(execution, "start", execution.user)
            if (scheduledExecution) {
                //send onstart notification
                def result = notificationService.triggerJobNotification('start', scheduledExecution.id,
                        [execution: execution, context:executioncontext])

            }
            //install custom outputstreams for System.out and System.err for this thread and any child threads
            //output will be sent to loghandler instead.
            sysThreadBoundOut.installThreadStream(
                    loggingService.createLogOutputStream(
                            workflowoverride,
                            LogLevel.NORMAL,
                            contextmanager,
                            logOutFlusher,
                            inputCharset ? Charset.forName(inputCharset) : null
                    )
            );
            sysThreadBoundErr.installThreadStream(
                    loggingService.createLogOutputStream(
                            workflowoverride,
                            LogLevel.ERROR,
                            contextmanager,
                            logErrFlusher,
                            inputCharset ? Charset.forName(inputCharset) : null
                    )
            );
            WorkflowExecutionItem item = executionUtilService.createExecutionItemForWorkflow(execution.workflow)


            def executionLifecyclePluginConfigs = scheduledExecution ?
                                   executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(scheduledExecution) :
                                   null
            def executionLifecyclePluginExecHandler = executionLifecyclePluginService.getExecutionHandler(executionLifecyclePluginConfigs, execution.asReference())
            //create service object for the framework and listener
            Thread thread = new WorkflowExecutionServiceThread(
                    framework.getWorkflowExecutionService(),
                    item,
                    executioncontext,
                    workflowLogManager,
                    executionLifecyclePluginExecHandler
            )

            thread.start()
            log.debug("started thread")
            return [
                    thread            : thread,
                    loghandler        : loghandler,
                    noderecorder      : recorder,
                    execution         : execution,
                    scheduledExecution: scheduledExecution,
                    threshold         : threshold,
                    periodicCheck     : checkpoint
            ]
        }catch(Exception e) {
            log.error("Failed while starting execution: ${execution.id}", e)
            loghandler.logError('Failed to start execution: ' + e.getClass().getName() + ": " + e.message)
            sysThreadBoundOut.close()
            sysThreadBoundOut.removeThreadStream()
            sysThreadBoundErr.close()
            sysThreadBoundErr.removeThreadStream()
            loghandler.close()
            return null
        }
    }

    /**
     * Return true if password can be read
     * @param authContext
     * @param storagePath
     * @param failIfMissing
     * @return
     */
    boolean canReadStoragePassword(AuthContext authContext, String storagePath, boolean failIfMissing){
        if(storagePath?.contains('${')){
            return true;//bypass validation if uses an execution variable
        }
        def keystore = storageService.storageTreeWithContext(authContext)
        try {
            return keystore.hasPassword(storagePath)
        } catch (StorageException e) {
            if (failIfMissing) {
                throw new ExecutionServiceException(
                        "Could not read password, storage path ${storagePath}: ${e.message}",
                        e
                )
            }
            return false;
        }
    }

    /**
     * Load stored password default values for secure options with defaultStoragePath, and no value set.
     *
     * @param scheduledExecution job
     * @param secureOptsExposed exposed secure option values
     * @param secureOpts private secure option values
     * @param authContext auth context
     */
    void loadSecureOptionStorageDefaults(
            ScheduledExecution scheduledExecution,
            Map secureOptsExposed,
            Map secureOpts,
            AuthContext authContext,
            boolean failIfMissingRequired=false,
            Map<String, String> args = null,
            Map<String, String> job = null,
            Map secureOptionNodeDeferred = null
    )
    {
        def found = scheduledExecution.options?.findAll {
            it.secureInput && it.defaultStoragePath
        }?.findAll {
            it.secureExposed ?
                    !(secureOptsExposed?.containsKey(it.name)) :
                    !(secureOpts?.containsKey(it.name))
        }
        if (found) {
            //load secure option defaults from key storage
            def keystore = storageService.storageTreeWithContext(authContext)
            found?.each {
                def defStoragePath = it.defaultStoragePath
                def failMessage
                def exists=false
                try {
                    //search and replace ${option.
                    if (args && defStoragePath?.contains('${option.')) {
                        defStoragePath = DataContextUtils.replaceDataReferencesInString(defStoragePath, DataContextUtils.addContext("option", args, null)).trim()
                    }
                    if (job && defStoragePath?.contains('${job.')) {
                        defStoragePath = DataContextUtils.replaceDataReferencesInString(defStoragePath, DataContextUtils.addContext("job", job, null)).trim()
                    }
                    def password
                    def nodeDeferred = false
                    if (defStoragePath?.contains('${node.')) {
                        nodeDeferred = secureOptionNodeDeferred != null ? true : false
                        password = defStoragePath //to be resolved later
                        exists=true
                    }else {
                        if(keystore.hasPassword(defStoragePath)){
                            password = keystore.readPassword(defStoragePath)
                            exists=true
                        }else{
                            failMessage = "path not found"
                        }
                    }
                    if(exists){
                        if (it.secureExposed) {
                            secureOptsExposed[it.name] = new String(password)
                            if(nodeDeferred) {
                                secureOptionNodeDeferred[it.name] = password
                            }
                        } else {
                            secureOpts[it.name] = new String(password)
                            if(nodeDeferred) {
                                secureOptionNodeDeferred[it.name] = password
                            }
                        }
                    }
                } catch (StorageException e) {
                    failMessage = e.message
                }

                if(!exists){
                    if (it.required && failIfMissingRequired) {
                        throw new ExecutionServiceException(
                                "Required option '${it.name}' default value could not be loaded from key storage " +
                                        "path: ${defStoragePath}: ${failMessage}"
                        )
                    } else {
                        log.warn(
                                "Option '${it.name}' default value could not be loaded from key storage " +
                                        "path: ${defStoragePath}: ${failMessage}"
                        )
                    }
                }
            }
        }
    }

    private LogLevel logLevelForString(String level){
        def deflevel = applicationContext?.getServletContext()?.getAttribute("LOGLEVEL_DEFAULT")
        return LogLevel.looseValueOf(level?:deflevel,LogLevel.NORMAL)
    }

    def static textLogLevels = ['ERR': 'ERROR']
    def static mappedLogLevels = ['ERROR', 'WARN', 'INFO', 'VERBOSE', 'DEBUG']
    /**
     * Map the log level to an integer used internally
     * @param level
     * @return
     */
    private int logLevelIntValue(String level){
        LogLevel loglevel = logLevelForString(level)
        List levels= LogLevel.values() as List
        return levels.indexOf(loglevel)
    }



    public static String EXECUTION_RUNNING = "running"
    public static String EXECUTION_SUCCEEDED = "succeeded"
    public static String EXECUTION_FAILED = "failed"
    public static String EXECUTION_ABORTED = "aborted"
    public static String EXECUTION_TIMEDOUT = "timedout"
    public static String EXECUTION_FAILED_WITH_RETRY = "failed-with-retry"
    public static String EXECUTION_STATE_OTHER = "other"
    public static String EXECUTION_SCHEDULED = "scheduled"

    public static String ABORT_PENDING = "pending"
    public static String ABORT_ABORTED = "aborted"
    public static String ABORT_FAILED = "failed"

    public String getExecutionState(Execution e) {
        e.executionState
    }

    /**
     * Return an StepExecutionItem instance for the given workflow Execution, suitable for the ExecutionService layer
     */
    public StepExecutionContext createContext(ExecutionContext execMap, StepExecutionContext origContext,
                                              Map<String, String> jobcontext, String[] inputargs = null,
                                              Map extraParams = null, Map extraParamsExposed = null,
                                              String charsetEncoding = null,
                                              LoggingManager manager = null,
                                              Map secureOptionNodeDeferred = null
    )
    {
        createContext(execMap,
                      origContext,
                      origContext.framework,
                      origContext.authContext,
                      origContext.user,
                      jobcontext,
                      origContext.executionListener,
                      origContext.workflowExecutionListener,
                      inputargs,
                      extraParams,
                      extraParamsExposed,
                      charsetEncoding,
                      manager,
                      secureOptionNodeDeferred
        )
    }
    /**
     * Return an StepExecutionItem instance for the given workflow Execution, suitable for the ExecutionService layer
     */
    public StepExecutionContext createContext(
            ExecutionContext execMap,
            StepExecutionContext origContext,
            Framework framework,
            UserAndRolesAuthContext authContext,
            String userName = null,
            Map<String, String> jobcontext,
            ExecutionListener listener,
            WorkflowExecutionListener wlistener,
            String[] inputargs = null,
            Map extraParams = null,
            Map extraParamsExposed = null,
            String charsetEncoding = null,
            LoggingManager manager = null,
            Map secureOptionNodeDeferred = null
    )
    {
        if (!userName) {
            userName=execMap.user
        }

        def userLogin = User.findByLogin(userName)
        if(userLogin && userLogin.email){
            jobcontext['user.email'] = userLogin.email
        }
        //convert argString into Map<String,String>
        def String[] args = execMap.argString? OptsUtil.burst(execMap.argString):inputargs
        def Map<String, String> optsmap = execMap.argString ? FrameworkService.parseOptsFromString(execMap.argString)
                : null != args ? frameworkService.parseOptsFromArray(args) : [:]
        if(extraParamsExposed){
            optsmap.putAll(extraParamsExposed)
        }

        def Map<String,Map<String,String>> datacontext = new HashMap<String,Map<String,String>>()

        // Put globals in context.
        Map<String, String> globals = frameworkService.getProjectGlobals(origContext?.frameworkProject?:execMap.project);
        datacontext.put("globals", globals ? globals : new HashMap<>());

        //add delimiter to option variables
        if(null !=optsmap){

            //replaces options values by global ones.
            optsmap.putAll(DataContextUtils.replaceDataReferences(optsmap, datacontext))

            def se=null
            if(execMap  instanceof Execution && null!=execMap.scheduledExecution){
                se=execMap.scheduledExecution
            }else if(execMap instanceof ScheduledExecution){
                se=execMap
            }

            if( null!=se){
                se.options?.sort().each{option->
                    if(option.multivalued){
                        optsmap["${option.name}.delimiter"]=option.delimiter
                    }
                }
            }
        }

        datacontext.put("option",optsmap)
        if(extraParamsExposed){
            datacontext.put("secureOption",extraParamsExposed.clone())
        }
        if(secureOptionNodeDeferred) {
            datacontext.put("nodeDeferred", secureOptionNodeDeferred.clone())
        }
        datacontext.put("job",jobcontext?jobcontext:new HashMap<String,String>())


        NodesSelector nodeselector
        int threadCount=1
        boolean keepgoing=false

        if (execMap.doNodedispatch) {
            //set nodeset for the context if doNodedispatch parameter is true
            def filter = DataContextUtils.replaceDataReferencesInString(execMap.asFilter(), datacontext)
            def filterExclude = DataContextUtils.replaceDataReferencesInString(execMap.asExcludeFilter(),datacontext)
            NodeSet nodeset = filtersAsNodeSet([filter:filter,
                    filterExclude: filterExclude,
                    nodeExcludePrecedence:execMap.nodeExcludePrecedence,
                    nodeThreadcount: execMap.nodeThreadcount,
                    nodeKeepgoing: execMap.nodeKeepgoing
            ])
            nodeselector=nodeset
            // enhnacement to allow ${option.xyz} in tags and names
            if (nodeset != null) {
                threadCount=nodeset.threadCount
                keepgoing=nodeset.keepgoing
            }
        } else if(framework){
            //blank?
            nodeselector = SelectorUtils.singleNode(framework.frameworkNodeName)
        }else{
            nodeselector = null
        }

        def INodeSet nodeSet = frameworkService.filterAuthorizedNodes(
                origContext?.frameworkProject?:execMap.project,
                new HashSet<String>(Arrays.asList("read", "run")),
                frameworkService.filterNodeSet(nodeselector, origContext?.frameworkProject?:execMap.project),
                authContext)

        def Map<String, Map<String, String>> privatecontext = new HashMap<String, Map<String, String>>()
        if (null != extraParams) {
            privatecontext.put("option", extraParams)
        }

        def OrchestratorConfig orchestrator
        if(execMap.orchestrator){
            orchestrator = new OrchestratorConfig(execMap.orchestrator.type, execMap.orchestrator.configuration);
        }else{
            orchestrator = null;
        }

        //create execution context
        def builder = ExecutionContextImpl.builder((StepExecutionContext)origContext)
        builder.with {
            pluginControlService(PluginControlServiceImpl.forProject(framework, origContext?.frameworkProject?:execMap.project))
            frameworkProject(origContext?.frameworkProject?:execMap.project)
            storageTree(storageService.storageTreeWithContext(authContext))
            jobService(jobStateService.jobServiceWithAuthContext(authContext))
            nodeService(rundeckNodeService)
            user(userName)
            nodeSelector(nodeselector)
            nodes(nodeSet)
            loglevel(logLevelIntValue(execMap.loglevel))
            sharedDataContextClear()
            dataContext(datacontext)
            privateDataContext(privatecontext)
            executionListener(listener)
            workflowExecutionListener(wlistener)
        }
        builder.charsetEncoding(charsetEncoding)
        builder.framework(framework)
        builder.authContext(authContext)
        builder.threadCount(threadCount)
        builder.keepgoing(keepgoing)
        builder.orchestrator(orchestrator)
        builder.with {
            nodeRankAttribute(execMap.nodeRankAttribute)
            nodeRankOrderAscending(null == execMap.nodeRankOrderAscending || execMap.nodeRankOrderAscending)
            loggingManager(manager)
        }
        if(origContext){
            //start a sub context
            builder.pushContextStep(1)
        }
        return builder.build()
    }

    @ToString(includeNames = true)
    class AbortResult {
        String abortstate
        String jobstate
        String status
        String reason
    }

    /**
     * Abort execution if authorized
     * @param se job
     * @param e execution
     * @param user username
     * @param authContext auth context
     * @param killAsUser as username
     * @return AbortResult
     */
    def abortExecution(
            ScheduledExecution se,
            Execution e,
            String user,
            AuthContext authContext,
            String killAsUser = null,
            boolean forceIncomplete = false
    )
    {
        if (!frameworkService.authorizeProjectExecutionAll(authContext, e, [AuthConstants.ACTION_KILL])) {
            return new AbortResult(
                    abortstate: ABORT_FAILED,
                    jobstate: getExecutionState(e),
                    status: getExecutionState(e),
                    reason: "unauthorized"
            )
        } else if (killAsUser && !frameworkService.authorizeProjectExecutionAll(
                authContext,
                e,
                [AuthConstants.ACTION_KILLAS]
        )) {
            return new AbortResult(
                    abortstate: ABORT_FAILED,
                    jobstate: getExecutionState(e),
                    status: getExecutionState(e),
                    reason: "unauthorized"
            )
        }
        abortExecutionDirect(se, e, user, killAsUser, forceIncomplete)
    }

    /**
     * Abort execution without testing authorization
     * @param se job
     * @param e execution
     * @param user username
     * @param killAsUser as username
     * @return AbortResult
     */
    def abortExecutionDirect(
            ScheduledExecution se,
            Execution e,
            String user,
            String killAsUser = null,
            boolean forceIncomplete = false
    )
    {
        metricService.markMeter(this.class.name, 'executionAbortMeter')
        def eid = e.id
        def dateCompleted = e.dateCompleted
        e.discard()
        def ident = scheduledExecutionService.getJobIdent(se, e)
        def isadhocschedule = se && e.status == "scheduled"
        def userIdent = killAsUser?:user
        if (frameworkService.isClusterModeEnabled() && !forceIncomplete) {
            def serverUUID = frameworkService.serverUUID
            if (e.serverNodeUUID != serverUUID) {
                CompletableFuture<AbortResult> fresult = new CompletableFuture<>()
                sendAndReceive(
                        'cluster.abortExecution',
                        [
                                jobId      : se?.extid,
                                executionId: e.id,
                                project    : e.project,
                                user       : user,
                                killAsUser : killAsUser,
                                uuidSource : serverUUID,
                                uuidTarget : e.serverNodeUUID
                        ]
                ) { resp ->
                    //recieve reply from event
                    Map abortresult = [
                            abortstate: ABORT_FAILED,
                            jobstate  : getExecutionState(e),
                            status    : getExecutionState(e),
                            reason    : "Execution is running on a different cluster server: " + e.serverNodeUUID
                    ]
                    if (resp && resp instanceof Map) {
                        fresult.complete(new AbortResult(abortresult + resp))
                    } else {
                        fresult.complete(new AbortResult(abortresult))
                    }
                }
                AbortResult result = fresult.get(30, TimeUnit.SECONDS)
                if(result) return result
            }
        }
        def result = new AbortResult()
        def cleanupStatus = configurationService.getString(
                'executionService.startup.cleanupStatus',
                'incomplete'
        )
        def quartzJobInstanceId = scheduledExecutionService.findExecutingQuartzJob(se, e)
        if (quartzJobInstanceId) {
            boolean success = false
            int repeat = 3;
            //set abortedBy on the execution
            while (!success && repeat > 0) {
                try {
                    Execution.withTransaction {
                        Execution e2 = Execution.get(eid)
                        if (!e2.abortedby) {
                            e2.abortedby = userIdent
                            e2.save(flush: true)
                            success = true
                        }
                    }
                } catch (org.springframework.dao.ConcurrencyFailureException ex) {
                    log.error("Could not abort ${eid}, the execution was modified")
                } catch (StaleObjectStateException ex) {
                    log.error("Could not abort ${eid}, the execution was modified")
                }
                if (!success){
                    Thread.sleep(200)
                    repeat--
                }
            }
            result.abortstate = success ? ABORT_PENDING : ABORT_FAILED
            result.reason = success ? '' : 'Unable to modify the execution'
            if (success) {
                def didCancel = scheduledExecutionService.interruptJob(
                        quartzJobInstanceId,
                        ident.jobname,
                        ident.groupname,
                        isadhocschedule
                )
                result.abortstate = didCancel ? ABORT_PENDING : ABORT_FAILED
                result.reason = didCancel ? '' : 'Unable to interrupt the running job'
            }
            result.jobstate = EXECUTION_RUNNING
        } else if (null == dateCompleted) {
            scheduledExecutionService.interruptJob(null, ident.jobname, ident.groupname, isadhocschedule)
            saveExecutionState(
                se ? se.id : null,
                eid,
                    [
                        status       : forceIncomplete ? cleanupStatus : String.valueOf(false),
                        dateCompleted: new Date(),
                        cancelled    : !forceIncomplete,
                        abortedby    : userIdent
                    ],
                    null,
                    null
                )
            result.abortstate = ABORT_ABORTED
            result.jobstate = EXECUTION_ABORTED
        } else {
            result.jobstate = getExecutionState(e)
            result.status = 'previously ' + result.jobstate
            result.abortstate = ABORT_FAILED
            result.reason = 'Job is not running'
        }
        if (result.abortstate == ABORT_FAILED && forceIncomplete) {
            cleanupExecution(e, cleanupStatus)
            result.abortstate = ABORT_ABORTED
            result.jobstate = EXECUTION_ABORTED
            result.reason = 'Marked as ' + cleanupStatus
        }
        result
    }

    /**
     * Delete an execution and associated log files, return a map of results
     * @param e execution
     * @param user
     * @param authContext
     * @return [success:true/false, failures:[ [success:false, message: String, id: id],... ], successTotal:Integer]
     */
    Map deleteBulkExecutionIds(Collection ids, AuthContext authContext, String username) {
        def failures=[]
        def failed=false
        def count=0;
        for (Object id : ids) {
            def exec = Execution.get(id)
            def result
            if (!exec) {
                result = [success: false, message: 'Execution Not found: ' + id, id: id]
            } else {
                result = deleteExecution(exec, authContext, username)
                result.id = id
            }
            if(!result.success){
                failed=true
                failures<<result
            }else{
                count++;
            }
        }
        return [success:!failed, failures:failures, successTotal:count]
    }
    /**
     * Delete an execution and associated log files
     * @param e execution
     * @param user
     * @param authContext
     * @return
     */
    Map deleteExecution(Execution e, AuthContext authContext, String username){
        if (!frameworkService.authorizeApplicationResourceAny(authContext,
                frameworkService.authResourceForProject(e.project),
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN])) {
            return [success: false, error: 'unauthorized', message: "Unauthorized: Delete execution in project ${e.project}"]
        }

        Map result
        try {
            if (e.dateCompleted == null && e.dateStarted != null) {
                return [error: 'running', message: "Failed to delete execution {{Execution ${e.id}}}: The execution is currently running", success: false]
            }
            ReferencedExecution.findAllByExecution(e).each{ re ->
                re.delete()
            }
                //delete all reports
            ExecReport.findAllByJcExecId(e.id.toString()).each { rpt ->
                rpt.delete()
            }

            List<File> files = []
            def execs = []

            def executionFiles = logFileStorageService.getExecutionFiles(e, [], false)

            //aggregate all files to delete
            execs << e
            executionFiles.each { ftype, executionFile ->

                def localFile = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, false)
                if (null != localFile && localFile.exists()) {
                    files << localFile
                }

                def partialFile = logFileStorageService.getFileForExecutionFiletype(e, ftype, false, true)
                if (null != partialFile && partialFile.exists()) {
                    files << partialFile
                }

                def resultDeleteRemote = logFileStorageService.removeRemoteLogFile(e, ftype)
                if(!resultDeleteRemote.started){
                    log.debug(resultDeleteRemote.error)
                }
            }
            //delete all job file records
            fileUploadService.deleteRecordsForExecution(e)

            log.debug("${files.size()} files from execution will be deleted")
            logExecutionLog4j(e, "delete", username)
            //delete execution
            //find an execution that this is a retry for
            Execution.findAllByRetryExecution(e).each{e2->
                e2.retryExecution=null
            }
            e.delete()
            //delete all files
            def deletedfiles = 0
            files.each { file ->
                if (!FileUtils.deleteQuietly(file)) {
                    log.warn("Failed to delete file while deleting execution ${e.id}: ${file.absolutePath}")
                } else {
                    deletedfiles++
                }
            }
            log.debug("${deletedfiles} files removed")
            result = [success: true]
        } catch (Exception ex) {
            log.error("Failed to delete execution ${e.id}", ex)
            result = [error:'failure',message: "Failed to delete execution {{Execution ${e.id}}}: ${ex.message}", success: false]
        }
        return result
    }

    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(BaseNodeFilters econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(BaseNodeFilters.asExcludeMap(econtext)).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(BaseNodeFilters.asIncludeMap(econtext)).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        return nodeset
    }

    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersExcludeAsNodeSet(BaseNodeFilters econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(BaseNodeFilters.asExcludeUnselectedMap(econtext)).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(BaseNodeFilters.asIncludeUnselectedMap(econtext)).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        return nodeset
    }
    /**
     * Return a NodeSet using the filters in the execution context and expanding variables by using the supplied
     * datacontext
     * @param econtext execution context
     * @param datacontext data values
     * @return nodeset
     */
    public static NodeSet filtersAsNodeSet(ExecutionContext econtext, Map<String,Map<String,String>> datacontext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(
                DataContextUtils.replaceDataReferences(BaseNodeFilters.asExcludeMap(econtext),datacontext)
        ).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(
                DataContextUtils.replaceDataReferences(BaseNodeFilters.asIncludeMap(econtext),datacontext)
        ).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing ? true : false)
        nodeset.setThreadCount(econtext.nodeThreadcount ? econtext.nodeThreadcount : 1)
        return nodeset
    }
    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(ExecutionContext econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(BaseNodeFilters.asExcludeMap(econtext)).setDominant(econtext.nodeExcludePrecedence ? true : false);
        nodeset.createInclude(BaseNodeFilters.asIncludeMap(econtext)).setDominant(!econtext.nodeExcludePrecedence ? true : false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing?true:false)
        nodeset.setThreadCount(econtext.nodeThreadcount?econtext.nodeThreadcount:1)
        return nodeset
    }

    /**
     * Return a NodeSet using the filters in the execution context
     */
    public static NodeSet filtersAsNodeSet(Map econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(BaseNodeFilters.asExcludeMap(econtext)).setDominant(econtext.nodeExcludePrecedence?true:false);
        nodeset.createInclude(BaseNodeFilters.asIncludeMap(econtext)).setDominant(!econtext.nodeExcludePrecedence?true:false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing?true:false)
        nodeset.setThreadCount(econtext.nodeThreadcount?econtext.nodeThreadcount:1)
        return nodeset
    }


   def Execution createExecution(Map params) {
        def Execution execution
        if (params.project && params.workflow) {
            execution = new Execution(project:params.project,
                                      user:params.user, loglevel:params.loglevel,
                                      doNodedispatch:params.doNodedispatch?"true" == params.doNodedispatch.toString():false,
                                      filter: params.filter,
                                      filterExclude: params.filterExclude,
                                      nodeExcludePrecedence:params.nodeExcludePrecedence,
                                      nodeThreadcount:params.nodeThreadcount,
                                      nodeKeepgoing:params.nodeKeepgoing,
                                      orchestrator:params.orchestrator,
                                      nodeRankOrderAscending:params.nodeRankOrderAscending,
                                      nodeRankAttribute:params.nodeRankAttribute,
                                      workflow:params.workflow,
                                      argString:params.argString,
                                      executionType: params.executionType ?: 'scheduled',
                                      timeout:params.timeout?:null,
                                      retryAttempt:params.retryAttempt?:0,
                                      retryOriginalId:params.retryOriginalId?:null,
                                      retryPrevId:params.retryPrevId?:null,
                                      retry:params.retry?:null,
                                      retryDelay: params.retryDelay?:null,
                                      serverNodeUUID: frameworkService.getServerUUID(),
                                      excludeFilterUncheck: params.excludeFilterUncheck?"true" == params.excludeFilterUncheck.toString():false,
                                      extraMetadataMap: params.extraMetadataMap?:null
            )

            execution.userRoles = params.userRoles


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
     * Expand date format strings in the string, in the form
     * ${DATE:FORMAT} or ${DATE[+-]#:FORMAT}
     * @param input
     * @return string with dates expanded, or original
     */
    def expandDateStrings(String input, Date dateStarted){
        if(input =~ /\$\{DATE((?:[-+]\d+)?:.*?)\}/) {
            def newstr = input

            try {
                newstr = input.replaceAll(/\$\{DATE((?:[-+]\d+)?:.*?)\}/) { all, tstamp ->
                    if (tstamp.lastIndexOf(":") == -1) {
                        return all
                    }
                    final operator = tstamp.substring(0, tstamp.lastIndexOf(":"))
                    final fdate = tstamp.substring(tstamp.lastIndexOf(":") + 1)
                    def formatter = new SimpleDateFormat(fdate)
                    if (operator == '') {
                        formatter.format(dateStarted)
                    } else {
                        final number = operator as int
                        final newDate = dateStarted + number
                        formatter.format(newDate)
                    }
                }

            } catch (IllegalArgumentException e) {
                log.warn(e)
            } catch (NumberFormatException e) {
                log.warn(e)
            }


            return newstr
        }
        return input
    }
    /**
     * creates an execution with the parameters, and evaluates dynamic buildstamp
     */
    def Execution createExecutionAndPrep(Map params, String user) throws ExecutionServiceException{
        def props =[:]
        props.putAll(params)
        if(!props.user){
            props.user=user
        }
        def Execution execution = createExecution(props)
        execution.dateStarted = new Date()

        def newstr = expandDateStrings(execution.argString, execution.dateStarted)
        if(newstr!=execution.argString){
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

    /**
     * Run a job,
     * @param scheduledExecution
     * @param framework
     * @param authContext
     * @param subject
     * @param user
     * @param input, map of input overrides, allowed keys: nodeIncludeName: Collection/String, loglevel: String, argString: String, optparams: Map,   option.*: String, option: Map, _replaceNodeFilters:true/false, filter: String
     * @return
     */
    public Map executeJob(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext, String user, Map input) {
        def secureOpts = selectSecureOptionInput(scheduledExecution, input)
        def secureOptsExposed = selectSecureOptionInput(scheduledExecution, input, true)
        return retryExecuteJob(scheduledExecution, authContext, user, input, secureOpts, secureOptsExposed, 0,-1, -1)
    }
    /**
     * retry a job execution
     * @param scheduledExecution
     * @param authContext
     * @param subject
     * @param user
     * @param input , map of input overrides, allowed keys: loglevel: String, argString: String,  option: Map, node
     * (Include|Exclude).*: String, _replaceNodeFilters:true/false, filter: String
     * @param secureOpts
     * @param secureOptsExposed
     * @param attempt
     * @return
     */
    public Map retryExecuteJob(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext,
                               String user, Map input, Map secureOpts=[:], Map secureOptsExposed = [:], int attempt,
                               long prevId, long originalId) {
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
                scheduledExecution.project)) {
            return [success: false, error: 'unauthorized', message: "Unauthorized: Execute Job ${scheduledExecution.extid}"]
        }

        if(!getExecutionsAreActive()){
            return [success:false,failed:true,error:'disabled',message:lookupMessage('disabled.execution.run',null)]
        }

        if(!scheduledExecutionService.isProjectExecutionEnabled(scheduledExecution.project)){
            return [success:false,failed:true,error:'disabled',message:lookupMessage('project.execution.disabled',null)]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            return [success:false,failed:true,error:'disabled',message:lookupMessage('scheduleExecution.execution.disabled',null)]
        }

        input.retryAttempt = attempt
        if(originalId>0){
            input.retryOriginalId = originalId
        }
        if(prevId>0){
            input.retryPrevId = prevId
        }
        def Execution e = null
        boolean success = false
        try {

            Map allowedOptions = input.subMap(
                    ['loglevel', 'argString', 'option', '_replaceNodeFilters', 'filter', 'executionType',
                     'retryAttempt', 'nodeoverride', 'nodefilter','retryOriginalId','retryPrevId','meta']
            ).findAll { it.value != null }
            allowedOptions.putAll(input.findAll { it.key.startsWith('option.') || it.key.startsWith('nodeInclude') || it.key.startsWith('nodeExclude') }.findAll { it.value != null })
            e = createExecution(scheduledExecution, authContext, user, allowedOptions, attempt > 0, prevId, secureOpts, secureOptsExposed)

            def timeout = 0
            def eid = scheduledExecutionService.scheduleTempJob(
                    scheduledExecution,
                    user,
                    authContext,
                    e,
                    secureOpts,
                    secureOptsExposed,
                    e.retryAttempt
            )
            success = true
            return [success: true, executionId: eid, name: scheduledExecution.jobName, execution: e]
        } catch (ExecutionServiceValidationException exc) {
            return [success: false, error: 'invalid', message: exc.getMessage(), options: exc.getOptions(), errors: exc.getErrors()]
        } catch (ExecutionServiceException exc) {
            def msg = exc.getMessage()
            log.error("Unable to create execution",exc)
            return [success: false, error: exc.code ?: 'failed', message: msg, options: input.option]
        } finally {
            if (!success && e) {
                e.delete()
            }
        }
    }

    /**
     * Run a job at a later time.
     *
     * The runAtTime parameter must be specified as an ISO 8601 compliant timestamp.
     *
     * @param scheduledExecution
     * @param framework
     * @param authContext
     * @param subject
     * @param user
     * @param input, map of input overrides, allowed keys: nodeIncludeName: Collection/String, loglevel: String, argString: String, optparams: Map,   option.*: String, option: Map, _replaceNodeFilters:true/false, filter: String, runAtTime: String
     * @return
     */
    public Map scheduleAdHocJob(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext, String user, Map input) {
        def secureOpts = selectSecureOptionInput(scheduledExecution, input)
        def secureOptsExposed = selectSecureOptionInput(scheduledExecution, input, true)

        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_RUN],
                scheduledExecution.project)) {
            return [success: false, error: 'unauthorized', message: "Unauthorized: Execute Job ${scheduledExecution.extid}"]
        }

        if (!getExecutionsAreActive()) {
            return [success: false, failed: true, error: 'disabled',
                    message: lookupMessage('disabled.execution.run', null)]
        }

        if (!scheduledExecution.hasExecutionEnabled()) {
            return [success: false, failed: true, error: 'disabled',
                    message: lookupMessage('scheduleExecution.execution.disabled', null)]
        }

        try {

            Map allowedOptions = input.subMap(['loglevel', 'argString', 'option', '_replaceNodeFilters', 'filter',
                                               'nodeoverride', 'nodefilter',
                                               'executionType',
                                               'meta',
                                               'retryAttempt']).findAll { it.value != null }
            allowedOptions.putAll(input.findAll {
                        it.key.startsWith('option.') || it.key.startsWith('nodeInclude') ||
                        it.key.startsWith('nodeExclude')
                }.findAll { it.value != null })

            if (!input.runAtTime) {
                return [success: false, failed: true, error: 'failed',
                        message: 'A date and time is required to schedule a job',
                        options: input.option]
            }

            // Try and parse schedule time

            Date startTime = parseRunAtTime(input.runAtTime)
            if (null == startTime) {
                return [success: false, failed: true, error: 'failed',
                        message: 'Invalid date/time format, only ISO 8601 is supported',
                        options: input.option]
            }


            if (startTime.before(new Date())) {
                return [success: false, failed: true, error: 'failed',
                        message: 'A job cannot be scheduled for a time in the past',
                        options: input.option]
            }
            if(!allowedOptions['executionType']){
                allowedOptions['executionType'] = 'user-scheduled'
            }
            def Execution e = createExecution(scheduledExecution, authContext, user, allowedOptions, false, -1, secureOpts, secureOptsExposed)

            // Update execution
            e.dateStarted       = startTime
            e.status            = "scheduled"
            e.save()

            def nextRun = scheduledExecutionService.scheduleAdHocJob(
                    scheduledExecution,
                    user,
                    authContext,
                    e,
                    secureOpts,
                    secureOptsExposed,
                    startTime
            )
            if (nextRun != null) {
                return [success: true, id: e.id, executionId: e.id,
                        name: scheduledExecution.jobName, execution: e,
                        nextRun: nextRun, message: 'scheduled successfully']
            } else {
                return [success: false, error: 'failed', message: 'unable to schedule job', options: input.option]
            }
        } catch (IllegalArgumentException iae) {
            return [success: false, error: 'invalid', message: 'cannot schedule job in the past', options: input.option]
        } catch (ExecutionServiceValidationException exc) {
            return [success: false, error: 'invalid', message: exc.getMessage(), options: exc.getOptions(), errors: exc.getErrors()]
        } catch (ExecutionServiceException exc) {
            def msg = exc.getMessage()
            log.error("Unable to create execution",exc)
            return [success: false, error: exc.code ?: 'failed', message: msg, options: input.option]
        }
    }

    /**
     * Parse the Date from the time input
     * @param runAtTime
     * @return valid Date, or null if it cannot be parsed
     */
    def Date parseRunAtTime(String runAtTime) {
        try {
            return ISO_8601_DATE_FORMAT_XXX.get().parse(runAtTime)
        } catch (ParseException e1) {

        }
        try {
            return ISO_8601_DATE_FORMAT_WITH_MS_XXX.get().parse(runAtTime)
        } catch (ParseException e1) {

        }
        try {
            return ISO_8601_DATE_FORMAT.get().parse(runAtTime)
        } catch (ParseException e1) {

        }
        try {
            return ISO_8601_DATE_FORMAT_WITH_MS.get().parse(runAtTime)
        } catch (ParseException e1) {

        }
        null
    }
    /**
     * Create execution
     * @param se job
     * @param authContext auth context
     * @param runAsUser owner of execution
     * @param input , map of input overrides, allowed keys: loglevel: String, option.*:String, argString: String, node(Include|Exclude).*: String, _replaceNodeFilters:true/false, filter: String, retryAttempt: Integer
     * @param securedOpts
     * @param secureExposedOpts
     * @return execution
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Execution int_createExecution(
            ScheduledExecution se,
            UserAndRolesAuthContext authContext,
            String runAsUser,
            Map input,
            Map securedOpts,
            Map secureExposedOpts
    ) {
        def props = [:]

        se = ScheduledExecution.get(se.id)
        def propset=[
                'project',
                'user',
                'loglevel',
                'doNodedispatch',
                'filter',
                'filterExclude',
                'nodeExcludePrecedence',
                'nodeThreadcount',
                'nodeThreadcountDynamic',
                'nodeKeepgoing',
                'nodeRankOrderAscending',
                'nodeRankAttribute',
                'workflow',
                'argString',
                'timeout',
                'orchestrator',
                'retry',
                'retryDelay',
                'excludeFilterUncheck'
        ]
        propset.each{k->
            props.put(k,se[k])
        }
        props.user = authContext.username
        def roles = authContext.roles
        if (roles) {
            props.userRoles = (roles as List)
        }
        if (runAsUser) {
            props.user = runAsUser
        }
        if (input && 'true' == input['_replaceNodeFilters']) {
            if('filter' == input.nodeoverride){
                input.filter = input.nodefilter
                input.doNodedispatch = true
            }else{
                //remove all existing node filters to replace with input filters
                props = props.findAll {!(it.key =~ /^(filter|node(Include|Exclude).*)$/)}

                def filterprops = input.findAll { it.key =~ /^(filter|node(Include|Exclude).*)$/ }
                def nset = filtersAsNodeSet(filterprops)
                input.filter = NodeSet.generateFilter(nset)
                input.filterExclude=""
                input.doNodedispatch=true
            }
        }
        if (input) {
            props.putAll(input.subMap(['argString','filter','filterExclude','loglevel','retryAttempt','doNodedispatch','retryPrevId','retryOriginalId']).findAll{it.value!=null})
            props.putAll(input.findAll{it.key.startsWith('option.') && it.value!=null})
        }

        if (input && input['executionType']) {
            props.executionType = input['executionType']
        } else {
            throw new ExecutionServiceException("executionType is required")
        }
        if(input['meta'] instanceof Map){
            props['extraMetadataMap'] = input['meta']
        }
        //evaluate embedded Job options for validation
        HashMap optparams = validateJobInputOptions(props, se, authContext, securedOpts, secureExposedOpts)

        optparams = removeSecureOptionEntries(se, optparams)

        props.argString = generateJobArgline(se, optparams)
        if (props.retry?.contains('${')) {
            //replace data references
            if (optparams) {
                props.retry = DataContextUtils.replaceDataReferencesInString(props.retry, DataContextUtils.addContext("option", optparams, null)).trim()
            }
        }
        if(props.retry){
            //validate retry is a valid integer
            try{
                Integer.parseInt(props.retry)
            }catch(NumberFormatException e){
                throw new ExecutionServiceException("Unable to create execution: the value for 'retry' was not a valid integer: "+e.message,e)
            }
        }
        if (props.timeout?.contains('${')) {
            //replace data references
            if (optparams) {
                props.timeout = DataContextUtils.replaceDataReferencesInString(props.timeout, DataContextUtils.addContext("option", optparams, null))
            }
        }
        if (props.retryDelay?.contains('${')) {
            //replace data references
            if (optparams) {
                props.retryDelay = DataContextUtils.replaceDataReferences(props.retryDelay, DataContextUtils.addContext("option", optparams, null))
            }
        }
        if (props.nodeThreadcountDynamic?.contains('${')) {
            //replace data references
            if (optparams) {
                props.nodeThreadcount = DataContextUtils.replaceDataReferencesInString(props.nodeThreadcountDynamic, DataContextUtils.addContext("option", optparams, null))

                if(!props.nodeThreadcount.isInteger()){
                    props.nodeThreadcount = 1
                }
            }
        }

        Workflow workflow = new Workflow(se.workflow)
        //create duplicate workflow
        props.workflow = workflow

        Execution execution = createExecution(props)
        execution.dateStarted = new Date()

        def newstr = expandDateStrings(execution.argString, execution.dateStarted)

        if(newstr!=execution.argString){
            execution.argString=newstr
        }

        execution.scheduledExecution=se
        if (workflow && !workflow.save(flush:true)) {
            execution.workflow.errors.allErrors.each { log.error(it.toString()) }
            log.error("unable to save execution workflow")
            throw new ExecutionServiceException("unable to create execution workflow")
        }
        if (!execution.save(flush:true)) {
            execution.errors.allErrors.each { log.warn(it.toString()) }
            def msg=execution.errors.allErrors.collect { ObjectError err-> lookupMessage(err.codes,err.arguments,err.defaultMessage) }.join(", ")
            log.error("unable to create execution: " + msg)
            throw new ExecutionServiceException("unable to create execution: "+msg)
        }
        return execution
    }
    /**
     * sync objects for preventing multiple executions of a job within this server
     */
    ConcurrentMap<String, Object> multijobflag = new ConcurrentHashMap<String, Object>()

    /**
     * Return an object for synchronization of the job id
     * @param id
     * @return object
     */
    private Object syncForJob(String id){
        def object = new Object()
        //return existing object if present, or the new object
        multijobflag.putIfAbsent(id, object) ?: object
    }

    @Subscriber
    def jobChanged(StoredJobChangeEvent e) {
        if (e.eventType == JobChangeEvent.JobChangeEventType.DELETE) {
            //clear multijob sync object
            multijobflag?.remove(e.originalJobReference.id)
        }
    }
    /**
     * Create an execution
     * @param se
     * @param user
     * @param input, map of input overrides, allowed keys: loglevel: String, argString: String, node(Include|Exclude)
     * .*: String, _replaceNodeFilters:true/false, filter: String, retryAttempt: Integer
     * @param securedOpts
     * @param secureExposedOpts
     * @return
     * @throws ExecutionServiceException
     */
    def Execution createExecution(
            ScheduledExecution se,
            UserAndRolesAuthContext authContext,
            String runAsUser,
            Map input = [:],
            boolean retry=false,
            long prevId=-1,
            Map securedOpts = [:],
            Map secureExposedOpts = [:]
    )
            throws ExecutionServiceException
    {
        def maxExecutions = 1
        if(se.multipleExecutions){
            maxExecutions = 0
            if(se.maxMultipleExecutions){
                maxExecutions = se.maxMultipleExecutions?.toInteger()
            }
        }
        if (maxExecutions > 0 ) {
            synchronized (syncForJob(se.extid)) {
                //find any currently running executions for this job, and if so, throw exception
                def found = Execution.withCriteria {
                    isNull('dateCompleted')
                    eq('scheduledExecution', se)
                    isNotNull('dateStarted')
                    if (retry) {
                        ne('id', prevId)
                    }
                }

                if (found && found.size() >= maxExecutions) {
                    throw new ExecutionServiceException('Job "' + se.jobName + '" {{Job ' + se.extid + '}} is currently being executed {{Execution ' + found[0].id + '}}', 'conflict')
                }

                return int_createExecution(se, authContext, runAsUser, input, securedOpts, secureExposedOpts)
            }
        }else{
            return int_createExecution(se,authContext,runAsUser,input, securedOpts, secureExposedOpts)
        }
    }

    /**
     * Parse input "option.NAME" values, or a single "argString" value. Add default missing defaults for required
     * options. Validate the values for the Job options and throw exception if validation fails. Return a map of name
     * to value for the parsed options.
     * @param props
     * @param scheduledExec
     * @param authContext auth for reading storage defaults
     * @param optionValues values modified by a job plugin
     * @param securedOpts
     * @param securedExposedOpts
     * @return
     */
    private HashMap validateJobInputOptions(Map props, ScheduledExecution scheduledExec, UserAndRolesAuthContext authContext, Map securedOpts, Map securedExposedOpts) {
        HashMap optparams
        optparams = parseJobOptionInput(props, scheduledExec)
        def result = checkBeforeJobExecution(scheduledExec, optparams, props, authContext)
        if(result?.isUseNewValues()){
            optparams = result.optionsValues
            checkSecuredOptions(result.optionsValues, securedOpts, securedExposedOpts)
        }
        validateOptionValues(scheduledExec, optparams,authContext)
        return optparams
    }

    /**
     * It replaces values coming from optionsValues into secured options and secured exposed options
     * it doesn't add values to secured/exposed options
     * @param optionsValues
     * @param securedOpts
     * @param securedExposedOpts
     */
    def checkSecuredOptions(optionsValues, Map securedOpts = [:], Map securedExposedOpts = [:]){

        optionsValues.each { k, v ->
            if(securedOpts[k]){
                securedOpts.put(k, v)
            }else if(securedExposedOpts[k]){
                securedExposedOpts.put(k, v)
            }
        }
    }

    /**
     * Parse input "option.NAME" values, or a single "argString" value. Add default missing defaults for required
     * options. return a key value map for option name and value.
     * @param props
     * @param scheduledExec
     * @return a Map of String to String, does not produce multiple values for multivalued options
     */
    protected HashMap parseJobOptionInput(Map props, ScheduledExecution scheduledExec, UserAndRolesAuthContext authContext = null) {
        def optparams = filterOptParams(props)
        if (!optparams && props.argString) {
            optparams = FrameworkService.parseOptsFromString(props.argString)
        }
        optparams = addOptionDefaults(scheduledExec, optparams)
        optparams = addRemoteOptionSelected(scheduledExec, optparams, authContext)
        optparams
    }

    /**
     * evaluate the options and return a map of the values of any secure options, using defaults for required options if
     * they are not present, and selecting between exposed/hidden secure values
     */
    def Map selectSecureOptionInput(ScheduledExecution scheduledExecution, Map params, Boolean exposed=false) throws ExecutionServiceException {
        def results=[:]
        def optparams
        if (params?.argString) {
            optparams = FrameworkService.parseOptsFromString(params.argString)
        }else if(params?.optparams){
            optparams=params.optparams
        }else{
            optparams = filterOptParams(params)
        }
        final options = scheduledExecution.options
        if (options) {
            options.each {Option opt ->
                if (opt.secureInput && optparams[opt.name] && (exposed && opt.secureExposed || !exposed && !opt.secureExposed)) {
                    results[opt.name]= optparams[opt.name]
                }else if (opt.secureInput && opt.defaultValue && opt.required && (exposed && opt.secureExposed || !exposed && !opt.secureExposed)) {
                    results[opt.name] = opt.defaultValue
                }
            }
        }
        return results
    }
    /**
     * Return a map containing all params that are not secure option parameters
     */
    def Map removeSecureOptionEntries(ScheduledExecution scheduledExecution, Map params) throws ExecutionServiceException {
        def results=new HashMap(params)
        final options = scheduledExecution.options
        if (options) {
            options.each {Option opt ->
                if (opt.secureInput) {
                    results.remove(opt.name)
                }
            }
        }
        return results
    }

    /**
     * evaluate the options in the input argString, and if any Options defined for the Job have enforced=true, the values
     * is taken from remote URL, have a selected value as default, and have null value in the input properties,
     * then append the selected by default option value to the argString
     */
    def Map addRemoteOptionSelected(ScheduledExecution scheduledExecution, Map optparams, UserAndRolesAuthContext authContext = null) throws ExecutionServiceException {
        def newmap = new HashMap(optparams)

        final options = scheduledExecution.options
        if (options) {
            def defaultoptions=[:]
            options.each {Option opt ->
                if(null==optparams[opt.name] && opt.enforced && !opt.optionValues){
                    Map remoteOptions = scheduledExecutionService.loadOptionsRemoteValues(scheduledExecution, [option: opt.name, extra: [option: optparams]], authContext?.username)
                    if(!remoteOptions.err && remoteOptions.values){
                        Map selectedOption = remoteOptions.values.find {it instanceof Map && [true, 'true'].contains(it.selected)}
                        if(selectedOption){
                            defaultoptions[opt.name]=selectedOption.value
                        }
                    }
                }
            }
            if(defaultoptions){
                newmap.putAll(defaultoptions)
            }
        }
        return newmap
    }

    /**
     * evaluate the options in the input argString, and if any Options defined for the Job have required=true, have a
     * defaultValue, and have null value in the input properties, then append the default option value to the argString
     */
    def Map addOptionDefaults(ScheduledExecution scheduledExecution, Map optparams) throws ExecutionServiceException {
        def newmap = new HashMap(optparams)

        final options = scheduledExecution.options
        if (options) {
            def defaultoptions=[:]
            options.each {Option opt ->
                if (null==optparams[opt.name] && opt.defaultValue) {
                    defaultoptions[opt.name]=opt.defaultValue
                }
            }
            if(defaultoptions){
                newmap.putAll(defaultoptions)
            }
        }
        return newmap
    }

    /**
     * Add only the options that exists on the child job
     */
    def Map<String,String> addImportedOptions(ScheduledExecution scheduledExecution, Map optparams, StepExecutionContext executionContext) throws ExecutionServiceException {
        def newMap = new HashMap()
        executionContext.dataContext.option.each {it ->
            if(scheduledExecution.findOption(it.key)){
                newMap<<it
            }
        }
        return newMap+optparams
    }


    /**
     * evaluate the options in the input properties, and if any Options defined for the Job have regex constraints,
     * require the values in the properties to match the regular expressions.  Throw ExecutionServiceException if
     * any options don't match.
     * @deprecated unused? cull
     */
    def boolean validateInputOptionValues(ScheduledExecution scheduledExecution, Map props) throws ExecutionServiceException{
        def optparams = filterOptParams(props)
        if(!optparams && props.argString){
            optparams = parseJobOptsFromString(scheduledExecution,props.argString)
        }
        return validateOptionValues(scheduledExecution,optparams)
    }
    /**
     * evaluate the options value map, and if any Options defined for the Job have regex constraints,
     * require the values in the properties to match the regular expressions.  Throw ExecutionServiceException if
     * any options don't match.
     * @param scheduledExecution the job
     * @param optparams Map of String to String
     * @param authContext auth for reading storage defaults
     */
    def boolean validateOptionValues(
            ScheduledExecution scheduledExecution,
            Map optparams,
            AuthContext authContext = null,
            isJobRef = false
    ) throws ExecutionServiceValidationException
    {

        def fail = false
        def sb = []

        def failedkeys = [:]
        def invalidOpt={Option opt, String msg->
            fail = true
            if (!failedkeys[opt.name]) {
                failedkeys[opt.name] = ''
            }
            sb << msg
            failedkeys[opt.name] += msg
        }
        if (scheduledExecution.options) {
            scheduledExecution.options.each { Option opt ->
                if (!opt.multivalued && optparams[opt.name] && !(optparams[opt.name] instanceof String)) {
                    invalidOpt opt,lookupMessage("domain.Option.validation.multivalue.notallowed",[opt.name,opt.secureInput ? '***' : optparams[opt.name]])
                    return
                }

                if (opt.typeFile && optparams[opt.name]) {
                    def validate = fileUploadService.validateFileRefForJobOption(
                            optparams[opt.name],
                            scheduledExecution.extid,
                            opt.name,
                            isJobRef
                    )
                    if (!validate.valid) {
                        invalidOpt opt, lookupMessage('domain.Option.validation.file.' + validate.error, validate.args)
                    }
                }
                if (opt.required && !optparams[opt.name]) {

                    if (!opt.defaultStoragePath) {
                        invalidOpt opt,lookupMessage("domain.Option.validation.required",[opt.name])
                        return
                    }
                    try {
                        def canread = canReadStoragePassword(
                                authContext,
                                opt.defaultStoragePath,
                                true
                        )

                        if (!canread) {
                            invalidOpt opt, lookupMessage(
                                    "domain.Option.validation.required.storageDefault",
                                    [opt.name, opt.defaultStoragePath]
                            )
                            return
                        }
                    } catch (ExecutionServiceException e1) {

                        invalidOpt opt, lookupMessage(
                                "domain.Option.validation.required.storageDefault.reason",
                                [opt.name, opt.defaultStoragePath, e1.message]

                        )
                        return
                    }
                }
                if(opt.enforced && !(opt.optionValues || opt.optionValuesPluginType)){
                    Map remoteOptions = scheduledExecutionService.loadOptionsRemoteValues(scheduledExecution,
                            [option: opt.name, extra: [option: optparams]], authContext?.username)
                    if(!remoteOptions.err && remoteOptions.values){
                        opt.optionValues = remoteOptions.values.collect { optValue ->
                            if (optValue instanceof Map) {
                                return optValue.value
                            } else {
                                return optValue
                            }
                        }
                    }
                }
                if (opt.multivalued) {
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        def val
                        if (optparams[opt.name] instanceof Collection) {
                            val = [optparams[opt.name]].flatten();
                        } else {
                            val = optparams[opt.name].toString().split(Pattern.quote(opt.delimiter))
                        }
                        val.grep { it }.each { value ->
                            if (!(value ==~ opt.regex)) {
                                fail = true
                            }
                        }
                        if (fail) {
                            invalidOpt opt,lookupMessage("domain.Option.validation.regex.values",[opt.name,optparams[opt.name],opt.regex])
                            return
                        }
                    }
                    if (opt.enforced && opt.optionValues && optparams[opt.name]) {
                        def val
                        if (optparams[opt.name] instanceof Collection) {
                            val = [optparams[opt.name]].flatten();
                        } else {
                            val = optparams[opt.name].toString().split(Pattern.quote(opt.delimiter))
                        }
                        if (!opt.optionValues.containsAll(val.grep { it })) {
                            invalidOpt opt,lookupMessage("domain.Option.validation.allowed.values",[opt.name,optparams[opt.name],opt.values])
                            return
                        }
                    }
                } else {
                    if (opt.regex && !opt.enforced && optparams[opt.name]) {
                        if (!(optparams[opt.name] ==~ opt.regex)) {
                            invalidOpt opt, opt.secureInput ?
                                    lookupMessage("domain.Option.validation.secure.invalid",[opt.name])
                                    : lookupMessage("domain.Option.validation.regex.invalid",[opt.name,optparams[opt.name],opt.regex])

                            return
                        }
                    }
                    if (opt.enforced && opt.optionValues &&
                            optparams[opt.name] &&
                            optparams[opt.name] instanceof String &&
                            !opt.optionValues.contains(optparams[opt.name])) {
                        invalidOpt opt,  opt.secureInput ?
                                lookupMessage("domain.Option.validation.secure.invalid",[opt.name])
                                : lookupMessage("domain.Option.validation.allowed.invalid",[opt.name,optparams[opt.name],opt.values])
                        return
                    }
                }
            }
        }
        if (fail) {
            def msg = sb.join('\n')
            throw new ExecutionServiceValidationException(msg, optparams, failedkeys)
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
        def optparams = FrameworkService.parseOptsFromString(argString)
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

    def saveExecutionState( schedId, exId, Map props, Map execmap, Map retryContext){
        def ScheduledExecution scheduledExecution
        def boolean execSaved = false
        def Execution execution
        Execution.withNewTransaction {
            execution = Execution.get(exId)
            execution.properties = props
            if (props.failedNodes) {
                execution.failedNodeList = props.failedNodes.join(",")
            }
            if (props.succeededNodes) {
                execution.succeededNodeList = props.succeededNodes.join(",")
            }

            if (schedId) {
                scheduledExecution = ScheduledExecution.get(schedId)
            }

            if (!execution.cancelled && !(execution.statusSucceeded()) && scheduledExecution && retryContext) {
                //determine retry necessity
                int count = retryContext?.retryAttempt ?: 0
                def retryStr = execution.retry
                int maxRetries = 0
                if (retryStr) {
                    try {
                        maxRetries = Integer.parseInt(retryStr)
                    } catch (NumberFormatException e) {
                        log.error("Retry string for job was not resolvable: ${retryStr}")
                    }
                }
                if (maxRetries > count) {

                    //geting the original exec id
                    long originalId=-1
                    if(execution.retryAttempt==0){
                        originalId = execution.id
                    }else{
                        originalId = execution.retryOriginalId
                    }

                    execution.willRetry = true
                    def input = [
                            argString    : execution.argString,
                            executionType: execution.executionType,
                            loglevel     : execution.loglevel,
                            filter       : execution.filter //TODO: failed nodes?
                    ]
                    def result = retryExecuteJob(scheduledExecution, retryContext.authContext,
                            retryContext.user, input, retryContext.secureOpts,
                            retryContext.secureOptsExposed, count + 1,execution.id,originalId)
                    if (result.success) {
                        execution.retryExecution = result.execution
                    }
                }
            }

            if (execution.save(flush: true)) {
                log.debug("saved execution status. id: ${execution.id}")
                execSaved = true
            } else {

                execution.errors.allErrors.each { log.warn(it.defaultMessage) }
                log.error("failed to save execution status")
            }
            def jobname="adhoc"
            def jobid=null
            def summary= summarizeJob(scheduledExecution, execution)
            if (scheduledExecution) {
                jobname = scheduledExecution.groupPath ? scheduledExecution.generateFullName() : scheduledExecution.jobName
                jobid = scheduledExecution.id
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
                logExecution(
                        null,
                        execution.project,
                        execution.user,
                        execution.statusSucceeded(),
                        execution.status,
                        exId,
                        execution.dateStarted,
                        jobid,
                        jobname,
                        summary,
                        props.cancelled,
                        props.timedOut,
                        execution.willRetry,
                        node,
                        execution.abortedby,
                        execution.succeededNodeList,
                        execution.failedNodeList,
                        execution.filter
                )
                logExecutionLog4j(execution, "finish", execution.user)

                def context = execmap?.thread?.context
                notificationService.triggerJobNotification(
                        execution.statusSucceeded() ? 'success' : execution.willRetry ? 'retryablefailure' : 'failure',
                        schedId,
                        [
                                execution: execution,
                                nodestatus: [succeeded: sucCount,failed:failedCount,total:totalCount],
                                context: context
                        ]
                )
                notify('executionComplete',
                        new ExecutionCompleteEvent(
                                state: execution.executionState,
                                execution:execution,
                                job:scheduledExecution,
                                nodeStatus: [succeeded: sucCount,failed:failedCount,total:totalCount],
                                context: context?.dataContext

                        )
                )
            }
        }
    }

    public String summarizeJob(ScheduledExecution job=null,Execution exec){
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
    /**
     * Update a scheduledExecution statistics with a successful execution duration
     * @param schedId
     * @param execution
     * @return
     */
    def updateScheduledExecStatistics(Long schedId, eId, long time) {
        def success = false
        try {
            ScheduledExecutionStats.withTransaction {
                def scheduledExecution = ScheduledExecution.get(schedId)
                def seStats = scheduledExecution.getStats()


                if (scheduledExecution.scheduled) {
                    scheduledExecution.nextExecution = scheduledExecutionService.nextExecutionTime(scheduledExecution)
                    if (scheduledExecution.save(flush: true)) {
                        log.info("updated scheduled Execution nextExecution")
                    } else {
                        scheduledExecution.errors.allErrors.each { log.warn(it.defaultMessage) }
                        log.warn("failed saving scheduled Execution nextExecution")
                    }
                }
                def statsMap = seStats.getContentMap()
                if (null == statsMap.execCount || 0 == statsMap.execCount || null == statsMap.totalTime || 0 == statsMap.totalTime) {
                    statsMap.execCount = 1
                    statsMap.totalTime = time
                } else if (statsMap.execCount > 0 && statsMap.execCount < 10) {
                    statsMap.execCount++
                    statsMap.totalTime += time
                } else if (statsMap.execCount >= 10) {
                    def popTime = statsMap.totalTime.intdiv(statsMap.execCount)
                    statsMap.totalTime -= popTime
                    statsMap.totalTime += time
                }
                seStats.setContentMap(statsMap)

                if (seStats.validate()) {
                    if (seStats.save(flush: true)) {
                        log.info("updated scheduled Execution Stats")
                    } else {
                        seStats.errors.allErrors.each { log.warn(it.defaultMessage) }
                        log.warn("failed saving execution to history")
                    }
                    success = true
                }


            }
        } catch (org.springframework.dao.ConcurrencyFailureException e) {
            log.warn("Caught ConcurrencyFailureException, will retry updateScheduledExecStatistics for ${eId}")
        } catch (StaleObjectStateException e) {
            log.warn("Caught StaleObjectState, will retry updateScheduledExecStatistics for ${eId}")
        } catch (DuplicateKeyException ve) {
            log.warn("Caught DuplicateKeyException for migrated stats, will retry updateScheduledExecStatistics for ${eId}")
        }
        return success
    }

    /**
     * Update jobref stats
     * @param schedId
     * @param time
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def updateJobRefScheduledExecStatistics(Long schedId, long time) {
        def success = false
        try {
            def scheduledExecution = ScheduledExecution.get(schedId)
            def seStats = scheduledExecution.getStats()
            def statsMap = seStats.getContentMap()

            if (null == statsMap.execCount || 0 == statsMap.execCount || null == statsMap.totalTime || 0 == statsMap.totalTime) {
                statsMap.execCount = 1
                statsMap.totalTime = time
            } else if (statsMap.execCount > 0 && statsMap.execCount < 10) {
                statsMap.execCount++
                statsMap.totalTime += time
            } else if (statsMap.execCount >= 10) {
                def popTime = statsMap.totalTime.intdiv(statsMap.execCount)
                statsMap.totalTime -= popTime
                statsMap.totalTime += time
            }


            if (!statsMap.refExecCount) {
                statsMap.refExecCount = 1
            } else {
                statsMap.refExecCount++
            }
            seStats.setContentMap(statsMap)

            if (seStats.validate()) {
                if (seStats.save(flush: true)) {
                    log.info("updated referenced Job Stats")
                } else {
                    seStats.errors.allErrors.each { log.warn(it.defaultMessage) }
                    log.warn("failed saving referenced Job Stats")
                }
                success = true
            }
        } catch (org.springframework.dao.ConcurrencyFailureException e) {
            log.warn("Caught ConcurrencyFailureException, dismissed statistic for referenced Job")
        } catch (StaleObjectStateException e) {
            log.warn("Caught StaleObjectState, dismissed statistic for for referenced Job")
        } catch (DuplicateKeyException ve) {
            // Do something ...
            log.warn("Caught DuplicateKeyException for migrated stats, dismissed statistic for referenced Job")
        }
        return success
    }




    /**
    * Generate an argString from a map of options and values
     */
    public static String generateArgline(Map<String,String> opts){
        def argsList = []
        for (Map.Entry<String, String> entry : opts.entrySet()) {
            String val = opts.get(entry.key)
            argsList<<'-'+entry.key
            argsList<<val
        }
        return OptsUtil.join(argsList)
    }

    /**
    * Generate an argString from a map of options and values
     */
    public static String generateJobArgline(ScheduledExecution sched,Map<String,Object> opts){
        def newopts = [:]
        def addOptVal={key,obj,Option opt=null->
            String val
            if (obj instanceof String[] || obj instanceof Collection) {
                //join with delimiter
                if (opt && opt.delimiter) {
                    val = obj.grep { it }.join(opt.delimiter)
                } else {
                    val = obj.grep { it }.join(",")
                }
            } else {
                val = (String) obj
            }
            newopts[key] = val
        }
        for (Option opt : sched.options.findAll {opts.containsKey(it.name)}) {
            addOptVal(opt.name, opts.get(opt.name),opt)
        }
        //add any input options that don't match job options, to preserve information
        opts.keySet().findAll {!newopts[it]}.sort().each {
            addOptVal(it, opts[it])
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
        def optpatt = '^option\\.(.+)$'
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

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        if (!(item instanceof JobExecutionItem)) {
            throw new IllegalArgumentException("Unsupported item type: " + item.getClass().getName());
        }
        JobExecutionItem jitem = (JobExecutionItem) item;
        return jitem.isNodeStep()
    }

    /**
     * Execute the workflow step, the executionItem is expected to be a {@link JobExecutionItem} to execute a Job reference workflow step.
     * @param executionContext
     * @param executionItem
     * @return
     * @throws StepException
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    StepExecutionResult executeWorkflowStep(StepExecutionContext executionContext, StepExecutionItem executionItem) throws StepException{
        if (!(executionItem instanceof JobExecutionItem)) {
            throw new IllegalArgumentException("Unsupported item type: " + executionItem.getClass().getName());
        }
        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        JobExecutionItem jitem = (JobExecutionItem) executionItem
        return runJobRefExecutionItem(executionContext, jitem, createFailure, createSuccess)
    }

    ///////////////
      //for loading i18n messages
      //////////////

      /**
       * @parameter key
       * @returns corresponding value from messages.properties
       */
      def lookupMessage(String theKey, List<Object> data, String defaultMessage=null) {
          def locale = getLocale()
          def theValue = null
          MessageSource messageSource = messageSource?:applicationContext.getBean("messageSource")
          try {
              theValue =  messageSource.getMessage(theKey,data as Object[],locale )
          } catch (org.springframework.context.NoSuchMessageException e){
          } catch (java.lang.NullPointerException e) {
              log.error "Expression does not exist."
          }
          if(null==theValue && defaultMessage){
              MessageFormat format = new MessageFormat(defaultMessage);
              theValue=format.format(data as Object[])
          }
          return theValue
      }
      /**
       * @parameter key
       * @returns corresponding value from messages.properties
       */
      def lookupMessage(String[] theKeys, List<Object> data, String defaultMessage=null) {
          def locale = getLocale()
          def theValue = null
          theKeys.any{key->
              try {
                  theValue =  messageSource.getMessage(key,data as Object[],locale )
                  return true
              } catch (org.springframework.context.NoSuchMessageException e){
              } catch (java.lang.NullPointerException e) {
              }
              return false
          }
          if(null==theValue && defaultMessage){
              MessageFormat format = new MessageFormat(defaultMessage);
              theValue=format.format(data as Object[])
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

    /**
     * Override the node set in the context given the new filter
     * @param origContext
     * @param nodeFilter
     * @param nodeThreadcount
     * @param nodeKeepgoing
     * @return
     */
    StepExecutionContext overrideJobReferenceNodeFilter(
            INodeEntry node,
            StepExecutionContext origContext,
            StepExecutionContext newContext,
            String nodeFilter,
            Integer nodeThreadcount,
            Boolean nodeKeepgoing,
            String nodeRankAttribute,
            Boolean nodeRankOrderAscending,
            Boolean nodeIntersect
    )
    {
        def builder = ExecutionContextImpl.builder(newContext);

        if (nodeFilter) {
            //set nodeset for the context if doNodedispatch parameter is true
            def filter = SharedDataContextUtils.replaceDataReferences(
                    nodeFilter,
                    origContext.sharedDataContext,
                    node ? ContextView.node(node.nodename) : ContextView.global(),
                    ContextView.&nodeStep,
                    null,
                    false,
                    false
            )
            def nodeselector = filtersAsNodeSet([
                    filter               : filter,
                    nodeExcludePrecedence: true, //XXX: fix
                    nodeThreadcount      : nodeThreadcount?:1,
                    nodeKeepgoing        : nodeKeepgoing
            ])


            def INodeSet trialNodes
            if (nodeIntersect) {
                // Create intersection of overridden node filter and upstream job nodes
                trialNodes = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(nodeselector, origContext.nodes)
                nodeselector = SelectorUtils.nodeList(trialNodes.nodeNames)
            } else {
                trialNodes = frameworkService.filterNodeSet(nodeselector, newContext.frameworkProject)
            }

            INodeSet nodeSet = frameworkService.filterAuthorizedNodes(
                    newContext.frameworkProject,
                    new HashSet<String>(["read", "run"]),
                    trialNodes,
                    newContext.authContext);

            builder.nodeSelector(nodeselector).nodes(nodeSet)

            if (null != nodeThreadcount) {
                builder.threadCount(nodeThreadcount)
            }
            if (null != nodeKeepgoing) {
                builder.keepgoing(nodeKeepgoing)
            }
            if (null != nodeRankAttribute) {
                builder.nodeRankAttribute(nodeRankAttribute)
            }
            if (null != nodeRankOrderAscending) {
                builder.nodeRankOrderAscending(nodeRankOrderAscending)
            }
        } else if (nodeIntersect) {
            // Create intersection of referenced job node filter and upstream job nodes
            INodeSet nodeSet = com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(
                    newContext.nodeSelector,
                    origContext.nodes
            )
            def nodeselector = SelectorUtils.nodeList(nodeSet.nodeNames)
            nodeSet = frameworkService.filterAuthorizedNodes(
                    newContext.frameworkProject,
                    new HashSet<String>(["read", "run"]),
                    nodeSet,
                    newContext.authContext);

            builder.nodeSelector(nodeselector).nodes(nodeSet)
        }

        return builder.build()
    }

    /**
     * Create a step execution context for a Job Reference step
     * @param se the job
     * @param exec the execution, or null if not known
     * @param executionContext the original step context
     * @param newargs argument strings for the job, which will have data context references expanded
     * @param nodeFilter overriding node filter
     * @param nodeKeepgoing overriding keepgoing
     * @param nodeThreadcount overriding threadcount
     * @param dovalidate if true, validate the input arguments to the job
     * @return
     * @throws ExecutionServiceValidationException if input argument validation fails
     */
    StepExecutionContext createJobReferenceContext(
            ScheduledExecution se,
            Execution exec,
            StepExecutionContext executionContext,
            String[] newargs,
            String nodeFilter,
            Boolean nodeKeepgoing,
            Integer nodeThreadcount,
            String nodeRankAttribute,
            Boolean nodeRankOrderAscending,
            INodeEntry node,
            Boolean nodeIntersect,
            Boolean importOptions,
            dovalidate
    )
    throws ExecutionServiceValidationException
    {

        //substitute any data context references in the arguments
        if (null != newargs && executionContext.sharedDataContext) {
            def curDate=exec?exec.dateStarted: new Date()
            newargs = newargs.collect { expandDateStrings(it, curDate) }.toArray()

            newargs = SharedDataContextUtils.replaceDataReferences(
                    newargs,
                    executionContext.sharedDataContext,
                    node ? ContextView.node(node.nodename) : ContextView.global(),
                    ContextView.&nodeStep,
                    null,
                    false,
                    false
            )
        }

        def jobOptsMap = frameworkService.parseOptsFromArray(newargs)
        if(importOptions && executionContext.dataContext?.option) {
            jobOptsMap = addImportedOptions(se,jobOptsMap, executionContext)
        }
        jobOptsMap = addOptionDefaults(se, jobOptsMap)

        //select secureAuth and secure options from the args to pass
        Map<String,String> secAuthOpts = selectSecureOptionInput(se, [optparams: jobOptsMap], false)
        Map<String,String> secOpts = selectSecureOptionInput(se, [optparams: jobOptsMap], true)

        //for secAuthOpts, evaluate each in context of original private data context
        def evalSecAuthOpts = [:]
        secAuthOpts.each { k, v ->
            def newv = DataContextUtils.replaceDataReferencesInString(
                    v,
                    executionContext.privateDataContext,
                    DataContextUtils.replaceMissingOptionsWithBlank,
                    false
            )
            if (newv != v || !v.startsWith('${option.')) {
                evalSecAuthOpts[k] = newv
            }
        }

        //for secOpts, evaluate each in context of original secure option data context
        def evalSecOpts = [:]
        secOpts.each { k, v ->
            def newv = DataContextUtils.replaceDataReferencesInString(
                    v,
                    [option: executionContext.dataContext['secureOption']],
                    DataContextUtils.replaceMissingOptionsWithBlank,
                    false
            )
            if (newv != v || !v.startsWith('${option.')) {
                evalSecOpts[k] = newv
            }
        }

        //for plain opts, evaluate in context of non secure data context
        final Map<String,String> plainOpts = removeSecureOptionEntries(se, jobOptsMap)

        //define nonsecure opts entries
        def plainOptsContext = executionContext.dataContext['option']?.findAll { !executionContext.dataContext['secureOption'] || null == executionContext.dataContext['secureOption'][it.key] }
        def evalPlainOpts = [:]
        plainOpts.each { k, v ->
            evalPlainOpts[k] = DataContextUtils.replaceDataReferencesInString(
                    v,
                    [option: plainOptsContext],
                    DataContextUtils.replaceMissingOptionsWithBlank,
                    false
            )
        }

        //construct job data context
        def jobcontext = new HashMap<String, String>(executionContext.dataContext.job?:[:])
        jobcontext.id = se.extid
        jobcontext.loglevel = mappedLogLevels[executionContext.loglevel]
        jobcontext.name = se.jobName
        jobcontext.group = se.groupPath
        jobcontext.project = se.project
        jobcontext.username = executionContext.getUser()
        jobcontext['user.name'] = jobcontext.username

        def secureOptionNodeDeferred = [:]
        loadSecureOptionStorageDefaults(se, evalSecOpts, evalSecAuthOpts, executionContext.authContext,false,plainOpts,jobcontext, secureOptionNodeDeferred)

        //validate the option values
        if(dovalidate){
            validateOptionValues(se, evalPlainOpts + evalSecOpts + evalSecAuthOpts,executionContext.authContext,true)
        }

        //arg list for new context
        def stringList = evalPlainOpts.collect { ["-" + it.key, it.value] }.flatten()
        newargs = stringList.toArray(new String[stringList.size()]);



        def loggingFilters = se  ?
                ExecutionUtilService.createLogFilterConfigs(
                        se.workflow.getPluginConfigDataList(ServiceNameConstants.LogFilter)
                ) :
                []
        def workflowLogManager = executionContext.loggingManager?.createManager(
                loggingFilters
        )

        def newContext = createContext(
                se,
                executionContext,
                jobcontext,
                newargs,
                evalSecAuthOpts,
                evalSecOpts,
                null,
                workflowLogManager,
                secureOptionNodeDeferred
        )

        if (nodeFilter || nodeIntersect) {
            newContext = overrideJobReferenceNodeFilter(
                    node,
                    executionContext,
                    newContext,
                    nodeFilter,
                    nodeThreadcount,
                    nodeKeepgoing,
                    nodeRankAttribute,
                    nodeRankOrderAscending,
                    nodeIntersect
            )
        }
        if(dovalidate){
            fileUploadService.executionBeforeStart(
                    new ExecutionPrepareEvent(
                            execution: exec,
                            job: se,
                            context: newContext
                    ),true
            )
        }
        return newContext
    }
    /**
     * Execute a job reference workflow with a particular context, optionally overriding the target node set,
     * and return the result based on the createFailure/createSuccess closures
     * @param executionContext
     * @param jitem
     * @param nodeselector
     * @param nodeSet
     * @param createFailure closure that takes {@link FailureReason} and String as arguments, and returns a {@link StepExecutionResult} or {@link NodeStepResult}
     * @param createSuccess closure that returns a {@link StepExecutionResult} or {@link NodeStepResult}
     * @param node a node entry if this is a node step
     *
     * @return
     */
    private def runJobRefExecutionItem(
            StepExecutionContext executionContext,
            JobExecutionItem jitem,
            Closure createFailure,
            Closure createSuccess,
            INodeEntry node = null
    ) {
        def timeout = 0
        def averageDuration = 0
        def id
        def String execid
        def refId
        def result
        def project = null
        def failOnDisable = false
        def uuid

        def schedlist

        def StepExecutionContext newContext
        def WorkflowExecutionItem newExecItem
        def ScheduledExecution se
        def Execution exec
        def executionReference

        def props = frameworkService.getFrameworkProperties()
        def disableRefStats = (props?.getProperty('rundeck.disable.ref.stats') == 'true')
        def useName = false
        if (jitem instanceof JobRefCommand) {
            project = jitem.project
            failOnDisable = jitem.failOnDisable
            uuid = jitem.uuid
            useName = jitem.useName
        }

        ScheduledExecution.withTransaction {
            if(!useName && uuid){
                schedlist = ScheduledExecution.findAllScheduledExecutions(uuid)
                if (!schedlist || 1 != schedlist.size()) {
                  def msg = "Job [${uuid}] not found by uuid, project: ${project}"
                  executionContext.getExecutionListener().log(0, msg)
                  throw new StepException(msg, JobReferenceFailureReason.NotFound)
                }
            } else {
                def group = null
                def name = null
                def m = jitem.jobIdentifier =~ '^/?(.+)/([^/]+)$'
                if (m.matches()) {
                    group = m.group(1)
                    name = m.group(2)
                } else {
                    name = jitem.jobIdentifier
                }
                project = project ? project : executionContext.getFrameworkProject()
                schedlist = ScheduledExecution.findAllScheduledExecutions(group, name, project)
                if (!schedlist || 1 != schedlist.size()) {
                  def msg = "Job [${jitem.jobIdentifier}] not found by name, project: ${project}"
                  executionContext.getExecutionListener().log(0, msg)
                  throw new StepException(msg, JobReferenceFailureReason.NotFound)
                }
            }

            if (!schedlist || 1 != schedlist.size()) {
                def msg = "Job [${jitem.jobIdentifier}] not found, project: ${project}"
                executionContext.getExecutionListener().log(0, msg)
                throw new StepException(msg, JobReferenceFailureReason.NotFound)
            }
            id = schedlist[0].id

            execid = executionContext.dataContext.job?.execid
            if (!execid) {
                def msg = "Execution identifier (job.execid) not found in data context"
                executionContext.getExecutionListener().log(0, msg)
                throw new StepException(msg, JobReferenceFailureReason.NotFound)
            }

            def enableSe = true
            se = ScheduledExecution.get(id)
            enableSe = se.executionEnabled
            if (!enableSe) {
                if (failOnDisable) {
                    result = createFailure(JobReferenceFailureReason.ExecutionsDisabled, "Job [${jitem.jobIdentifier}] execution disabled")
                } else {
                    result = createSuccess()
                }
            } else {
                averageDuration = se.averageDuration
                exec = Execution.get(execid as Long)
                if (!exec) {
                    def msg = "Execution not found: ${execid}"
                    executionContext.getExecutionListener().log(0, msg);
                    result = createFailure(JobReferenceFailureReason.NotFound, msg)
                    return
                }

                if (!frameworkService.authorizeProjectJobAll(executionContext.getAuthContext(), se, [AuthConstants.ACTION_RUN], se.project)) {
                    def msg = "Unauthorized to execute job [${jitem.jobIdentifier}}: ${se.extid}"
                    executionContext.getExecutionListener().log(0, msg);
                    result = createFailure(JobReferenceFailureReason.Unauthorized, msg)
                    return
                }
                newExecItem = executionUtilService.createExecutionItemForWorkflow(se.workflow, se.project)

                try {
                    newContext = createJobReferenceContext(
                            se,
                            exec,
                            executionContext,
                            jitem.args,
                            jitem.nodeFilter,
                            jitem.nodeKeepgoing,
                            jitem.nodeThreadcount,
                            jitem.nodeRankAttribute,
                            jitem.nodeRankOrderAscending,
                            node,
                            jitem.nodeIntersect,
                            jitem.importOptions,
                            true
                    )
                } catch (ExecutionServiceValidationException e) {
                    executionContext.getExecutionListener().log(0, "Option input was not valid for [${jitem.jobIdentifier}]: ${e.message}");
                    def msg = "Invalid options: ${e.errors.keySet()}"
                    result = createFailure(JobReferenceFailureReason.InvalidOptions, msg.toString())
                }

                def timeouttmp = '0'
                if(se.timeout){
                    if(se.timeout.contains('${')){
                        timeouttmp = DataContextUtils.replaceDataReferencesInString(se.timeout, newContext.dataContext)
                    }else{
                        timeouttmp = se.timeout
                    }
                }
                timeout = Sizes.parseTimeDuration(timeouttmp)
            }
        }

        if (null != result) {
            return result
        }

        ScheduledExecution.withTransaction {
            exec = Execution.get(execid as Long)
            executionReference = exec.asReference()
            refId = saveRefExecution(EXECUTION_RUNNING, null, se.id, exec.id)

            if (!(schedlist[0].successOnEmptyNodeFilter) && newContext.getNodes().getNodeNames().size() < 1) {
                String msg = "No nodes matched for the filters: " + newContext.getNodeSelector()
                executionContext.getExecutionListener().log(0, msg)
                saveRefExecution(EXECUTION_FAILED, refId)

                throw new StepException(msg, JobReferenceFailureReason.NoMatchedNodes)
            }
        }

        long startTime = System.currentTimeMillis()
        def wresult = metricService.withTimer(this.class.name, 'runJobReference') {
            WorkflowExecutionService wservice = executionContext.getFramework().getWorkflowExecutionService()

            def timeoutms = 1000 * timeout
            def shouldCheckTimeout = timeoutms > 0

            def executionLifecyclePluginConfigs = se ?
                                   executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(se) :
                                   null
            def executionLifecyclePluginExecHandler = executionLifecyclePluginService.getExecutionHandler(executionLifecyclePluginConfigs, executionReference)
            Thread thread = new WorkflowExecutionServiceThread(
                    wservice,
                    newExecItem,
                    newContext,
                    null,
                    executionLifecyclePluginExecHandler
            )


            if(!exec.abortedby){
                thread.start()
                if (!jitem.ignoreNotifications) {
                    ScheduledExecution.withTransaction {
                        // Get a new object attached to the new session
                        def scheduledExecution = ScheduledExecution.get(id)
                        notificationService.triggerJobNotification('start', scheduledExecution,
                                [execution: exec, context: newContext, jobref: jitem.jobIdentifier])
                    }

                }
            }
            return executionUtilService.runRefJobWithTimer(thread, startTime, shouldCheckTimeout, timeoutms)

        }

        if (!wresult.result || !wresult.result.success || wresult.interrupt) {
            result = createFailure(JobReferenceFailureReason.JobFailed, "Job [${jitem.jobIdentifier}] failed")

        } else {
            result = createSuccess()
        }

        ScheduledExecution.withTransaction {
            def duration = System.currentTimeMillis() - startTime

            if (wresult.result) {
                def savedJobState = false
                if (!disableRefStats) {
                    updateJobRefScheduledExecStatistics(id, duration)
                }
                saveRefExecution(wresult.result.success ? EXECUTION_SUCCEEDED : EXECUTION_FAILED, refId)
            }

            Execution execution = Execution.get(execid as Long)


            def sucCount = 0
            def failedCount = 0
            if (wresult.result instanceof WorkflowExecutionResult) {
                WorkflowExecutionResult data = ((WorkflowExecutionResult) wresult.result)
                for (StepExecutionResult temp : data.getResultSet()) {
                    if (temp.success) {
                        sucCount++
                    } else {
                        failedCount++
                    }
                }
            }

            if(!jitem.ignoreNotifications) {
                // Get a new object attached to the new session

                if (averageDuration > 0 && duration > averageDuration) {
                    avgDurationExceeded(id, [
                            execution: execution,
                            context  : newContext,
                            jobref   : jitem.jobIdentifier
                    ])
                }

                def scheduledExecution = ScheduledExecution.get(id)
                notificationService.triggerJobNotification(
                        wresult?.result.success ? 'success' : 'failure',
                        scheduledExecution,
                        [
                                execution : execution,
                                nodestatus: [succeeded: sucCount, failed: failedCount, total: newContext.getNodes().getNodeNames().size()],
                                context   : newContext,
                                jobref    : jitem.jobIdentifier
                        ]
                )
            }

            result.sourceResult = wresult.result

            Map<String, String> data = ((WorkflowExecutionResult) wresult.result)?.getSharedContext()?.getData(ContextView.global())?.get("export")
            if (data) {
                executionContext.getOutputContext().addOutput(ContextView.global(), "export", data)
            }
        }

        return result
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def saveRefExecution(String status, Long refId, Long seId = null, Long execId=null){
            if(refId){
                ReferencedExecution refExec = ReferencedExecution.findById(refId)
                refExec.status = status
                refExec.save(flush:true)
            }else{
                ScheduledExecution se = ScheduledExecution.findById(seId)
                Execution exec = Execution.findById(execId)
                ReferencedExecution refExec = new ReferencedExecution(
                        scheduledExecution: se, execution: exec, status: status).save(flush:true)
                return refExec.id
            }
    }
    /**
     * Query for executions for the specified job
     * @param scheduledExecution the job
     * @param state status string
     * @param offset paging offset
     * @param max paging max
     * @return result map from {@link #queryExecutions(com.dtolabs.rundeck.app.support.ExecutionQuery, int, int)}
     */
    def queryJobExecutions(ScheduledExecution scheduledExecution,String state,int offset=0,int max=-1){
        def query=new ExecutionQuery()
        query.jobIdListFilter=[scheduledExecution.id.toString()]
        query.statusFilter=state
        query.projFilter=scheduledExecution.project
        return queryExecutions(query,offset,max)
    }

    /**
     * Query executions
     * @param criteriaClosure criteriaClos
     * @param offset paging offset
     * @param max paging max
     * @return result map [total: int, result: List<Execution>]
     */
    def queryExecutions(Closure criteriaClos){
        def result = Execution.createCriteria().list(criteriaClos.curry(false))
        def total = Execution.createCriteria().count(criteriaClos.curry(true))
        return [result:result,total:total]
    }

  /**
   * Query executions
   * @param query query
   * @param offset paging offset
   * @param max paging max
   * @return result map [total: int, result: List<Execution>]
   */
  def queryExecutions(ExecutionQuery query, int offset = 0, int max = -1) {
    def jobQueryComponents = applicationContext.getBeansOfType(JobQuery)
    def criteriaClos = { isCount ->

      // Run main query criteria
      def queryCriteria = query.createCriteria(delegate, jobQueryComponents)
      queryCriteria()

      if (!isCount) {
        if (offset) {
          firstResult(offset)
        }
        if (max && max > 0) {
          maxResults(max)
        }
        and {
          order('dateCompleted', 'desc')
          order('dateStarted', 'desc')
        }
      }
    }
    def result = Execution.createCriteria().list(criteriaClos.curry(false))
    def total = Execution.createCriteria().count(criteriaClos.curry(true))
    return [result: result, total: total]
  }


  /**
     * Return statistics over a Query resultset.
     * @param query query
     * @return result map [total: long, duration: Map[average: double, max: long, min: long]]
     */
    def queryExecutionMetrics(ExecutionQuery query) {

        // Prepare Query Criteria
        def jobQueryComponents = applicationContext.getBeansOfType(JobQuery)
        def metricCriteria = {

            // Run main query criteria
            def baseQueryCriteria = query.createCriteria(delegate, jobQueryComponents)
            baseQueryCriteria()

            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
            projections {

                /* Group by Calculated Status */
//                groupProperty("state") // state field added as formula on Execution.groovy
//                property("state", "state")

/*
                // Exec Status sql expression. This works if added as a derived property on Execution.groovy.
                sqlProjection "CASE " +
                    "WHEN cancelled THEN '${ExecutionService.EXECUTION_ABORTED}'" +
                    "WHEN date_completed IS NULL AND status = '${ExecutionService.EXECUTION_SCHEDULED}' THEN '${ExecutionService.EXECUTION_SCHEDULED}'" +
                    "WHEN date_completed IS NULL THEN '${ExecutionService.EXECUTION_RUNNING}'" +
                    "WHEN status IN ('true', 'succeeded') THEN '${ExecutionService.EXECUTION_SUCCEEDED}'" +
                    "WHEN will_retry THEN '${ExecutionService.EXECUTION_FAILED_WITH_RETRY}'" +
                    "WHEN timed_out THEN '${ExecutionService.EXECUTION_TIMEDOUT}'" +
                    "WHEN status IN ('false', 'failed') THEN '${ExecutionService.EXECUTION_FAILED}'" +
                    "ELSE '${ExecutionService.EXECUTION_STATE_OTHER}' END as estado",
                    'estado',
                    StandardBasicTypes.STRING
*/

                rowCount("count")
                sqlProjection 'sum(date_completed - date_started) as durationSum',
                    'durationSum',
                    StandardBasicTypes.TIME
                sqlProjection 'min(date_completed - date_started) as durationMin',
                    'durationMin',
                    StandardBasicTypes.TIME
                sqlProjection 'max(date_completed - date_started) as durationMax',
                    'durationMax',
                    StandardBasicTypes.TIME

            }
        }

        // get data and calculate
        def metricsData = Execution.createCriteria().get(metricCriteria)

        def totalCount = metricsData?.count ? metricsData.count : 0

        Long maxDuration = sqlTimeToMillis(metricsData?.durationMax)
        Long minDuration = sqlTimeToMillis(metricsData?.durationMin)
        Long durationSum = sqlTimeToMillis(metricsData?.durationSum)
        double avgDuration = totalCount != 0 ? durationSum / totalCount : 0

        // Build response
        def metrics = [
            total: totalCount,

            duration: [
                average: avgDuration,
                max: maxDuration,
                min: minDuration
            ]
        ]

    }

    /**
     * Convert a java.sql.Time from its hh:mm:ss form to number of milliseconds without TZ issues.
     * @param t
     * @return
     */
    static long sqlTimeToMillis(Time t) {
        if (t == null) return 0
        def arr = t.toString().split(":")
        return ((Long.parseLong(arr[0]) * 3600) + (Long.parseLong(arr[1]) * 60) + (Long.parseLong(arr[2]))) * 1000
    }


    /**
     * Execute the node step, the executionItem is expected to be a {@link JobExecutionItem} to execute a Job reference
     * as a node step on a single node.
     * @param executionContext
     * @param executionItem
     * @return
     * @throws StepException
     */
    @Override
    @NotTransactional
    NodeStepResult executeNodeStep(StepExecutionContext executionContext, NodeStepExecutionItem executionItem,
                                   INodeEntry node) throws NodeStepException {
        if (!(executionItem instanceof JobExecutionItem)) {
            throw new IllegalArgumentException("Unsupported item type: " + executionItem.getClass().getName());
        }
        def createFailure= { FailureReason reason, String msg ->
            return NodeExecutorResultImpl.createFailure(reason, msg, node)
        }
        def createSuccess={
            return NodeExecutorResultImpl.createSuccess(node)
        }
        JobExecutionItem jitem = (JobExecutionItem) executionItem
        //don't override node filters, to allow option inputs to be used in the filters
        return runJobRefExecutionItem(executionContext, jitem, createFailure, createSuccess, node)
    }

    /**
     * Tidy up ThreadLocal instances on shutdown.
     */
    @PreDestroy
    public void onShutdownCleanUp() {
        ISO_8601_DATE_FORMAT_WITH_MS_XXX.remove()
        ISO_8601_DATE_FORMAT_XXX.remove()
        ISO_8601_DATE_FORMAT_WITH_MS.remove()
        ISO_8601_DATE_FORMAT.remove()
    }



    boolean avgDurationExceeded(schedId, Map content){
        notificationService.triggerJobNotification('avgduration',schedId, content)
    }

    List<PluginConfiguration> getGlobalPluginConfigurations(String project){
        def list = []

        def fwPlugins = ProjectNodeSupport.listPluginConfigurations(
                frameworkService.getProjectProperties(project),
                'framework.globalfilter',
                ServiceNameConstants.LogFilter,
                true
        )

        def projPlugins = ProjectNodeSupport.listPluginConfigurations(
                frameworkService.getProjectProperties(project),
                'project.globalfilter',
                ServiceNameConstants.LogFilter,
                true
        )
        list = list + projPlugins
        list = list + fwPlugins

        return list
    }

    def checkBeforeJobExecution(ScheduledExecution scheduledExecution, optparams, props, authContext) {

        INodeSet nodes = scheduledExecutionService.getNodes(scheduledExecution, props.filter, authContext)
        def nodeFilter = props.filter? props.filter : scheduledExecution.asFilter()
        JobPreExecutionEventImpl event = new JobPreExecutionEventImpl(
                scheduledExecution.jobName,
                props.project,
                props.user,
                optparams,
                nodes,
                nodeFilter,
                scheduledExecution.jobOptionsSet())
        try {
            return jobLifecyclePluginService.beforeJobExecution(scheduledExecution, event)
        } catch (JobLifecyclePluginException jpe) {
            throw new ExecutionServiceValidationException(jpe.message, optparams, null)
        }
    }
}
