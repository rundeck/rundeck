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

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRoles
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import grails.converters.JSON
import grails.gorm.transactions.NotTransactional
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import org.grails.web.json.JSONArray
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Subqueries
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.components.jobs.JobDefinitionException
import org.hibernate.sql.JoinType
import org.rundeck.app.components.jobs.JobQuery
import org.rundeck.app.components.jobs.JobQueryInput
import org.rundeck.app.components.schedule.TriggerBuilderHelper
import org.rundeck.app.components.schedule.TriggersExtender
import org.rundeck.app.components.jobs.UnsupportedFormatException
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.common.SelectorUtils
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.plugins.quartz.listeners.SessionBinderJobListener
import org.grails.web.json.JSONObject
import org.hibernate.StaleObjectStateException
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.criterion.Restrictions
import org.quartz.*
import org.rundeck.util.Sizes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.transaction.annotation.Propagation
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.*
import rundeck.controllers.EditOptsController
import rundeck.controllers.ScheduledExecutionController
import rundeck.controllers.WorkflowController
import rundeck.quartzjobs.ExecutionJob
import rundeck.quartzjobs.ExecutionsCleanUp
import rundeck.services.events.ExecutionPrepareEvent
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.utils.OptionsUtil
import org.rundeck.app.spi.AuthorizedServicesProvider

import javax.servlet.http.HttpSession
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


/**
 *  ScheduledExecutionService manages scheduling jobs with the Quartz scheduler
 */
@Transactional
class ScheduledExecutionService implements ApplicationContextAware, InitializingBean, ProjectConfigurable, EventPublisher {
    static transactional = true
    public static final String CONF_GROUP_EXPAND_LEVEL = 'project.jobs.gui.groupExpandLevel'
    public static final String CONF_PROJECT_DISABLE_EXECUTION = 'project.disable.executions'
    public static final String CONF_PROJECT_DISABLE_SCHEDULE = 'project.disable.schedule'

    def JobScheduleManager rundeckJobScheduleManager
    RundeckJobDefinitionManager rundeckJobDefinitionManager

    public final String REMOTE_OPTION_DISABLE_JSON_CHECK = 'project.jobs.disableRemoteOptionJsonCheck'

    public static final List<Property> ProjectConfigProperties = [
            PropertyBuilder.builder().with {
                integer 'groupExpandLevel'
                title 'Job Group Expansion Level'
                description 'In the Jobs page, expand Job groups to this depth by default.\n\n' +
                                    '* `0`: collapse all Groups\n' +
                                    '* `-1`: expand all Groups.'
                required(false)
                defaultValue '1'
                renderingOption('projectConfigCategory', 'gui')
            }.build(),
            PropertyBuilder.builder().with {
                booleanType 'disableExecution'
                title 'Disable Execution'
                required(false)
                defaultValue null
                renderingOptions( ['booleanTrueDisplayValueClass': 'text-warning','groupName':'Enable/Disable Execution Now'])
            }.build(),
            PropertyBuilder.builder().with {
                booleanType 'disableSchedule'
                title 'Disable Schedule'
                required(false)
                defaultValue null
                renderingOptions( ['booleanTrueDisplayValueClass': 'text-warning','groupName':'Enable/Disable Execution Now'])
            }.build(),
    ]

    public static final LinkedHashMap<String, String> ConfigPropertiesMapping = [
            groupExpandLevel: CONF_GROUP_EXPAND_LEVEL,
            disableExecution: CONF_PROJECT_DISABLE_EXECUTION,
            disableSchedule: CONF_PROJECT_DISABLE_SCHEDULE,
    ]
    public static final String CLEANER_EXECUTIONS_JOB_GROUP_NAME = "cleanerExecutionsJob"

    FrameworkService frameworkService
    AppAuthContextProcessor rundeckAuthContextProcessor
    def NotificationService notificationService
    //private field to set lazy bean dependency
    private ExecutionService executionServiceBean
    def executorService
    def Scheduler quartzScheduler
    /**
     * defined in quartz plugin
     */
    def SessionBinderJobListener sessionBinderListener
    ApplicationContext applicationContext

    def grailsApplication
    def MessageSource messageSource
    PluginService pluginService
    def executionUtilService
    FileUploadService fileUploadService
    JobSchedulerService jobSchedulerService
    JobLifecyclePluginService jobLifecyclePluginService
    ExecutionLifecyclePluginService executionLifecyclePluginService
    SchedulesManager jobSchedulesService
    private def triggerComponents
    AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    def OrchestratorPluginService orchestratorPluginService

    @Override
    void afterPropertiesSet() throws Exception {
        //add listener for every job
        quartzScheduler?.getListenerManager()?.addJobListener(sessionBinderListener)
        triggerComponents = applicationContext.getBeansOfType(TriggersExtender) ?: [:]
    }

    @Override
    Map<String, String> getCategories() {
        [groupExpandLevel: 'gui', disableExecution: 'executionMode', disableSchedule: 'executionMode',]
    }

    @Override
    List<Property> getProjectConfigProperties() { ProjectConfigProperties }

    @Override
    Map<String, String> getPropertiesMapping() { ConfigPropertiesMapping }

    /**
     * Return project config for node cache delay
     * @param project
     * @return
     */
    int getJobExpandLevel(final IRundeckProjectConfig projectConfig) {
        projectConfig.hasProperty(CONF_GROUP_EXPAND_LEVEL) ?
                tryParseInt(projectConfig).orElse(1) :
                1
    }

    private Optional<Integer> tryParseInt(IRundeckProjectConfig projectConfig) {
        try {
            Optional.of(Integer.parseInt(projectConfig.getProperty(CONF_GROUP_EXPAND_LEVEL)))
        } catch (NumberFormatException e) {
            Optional.empty()
        }
    }

    /**
     * private getter for executionService that is not auto-injected
     * @return
     */
    private ExecutionService getExecutionService(){
        if(null==executionServiceBean){
            this.executionServiceBean = applicationContext.getBean("executionService",ExecutionService)
        }
        return executionServiceBean
    }

    def getWorkflowStrategyPluginDescriptions(){
        pluginService.listPlugins(WorkflowStrategy, frameworkService.rundeckFramework.workflowStrategyService).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
    }

    boolean getPaginationEnabled() {
        grailsApplication.config.rundeck.gui.paginatejobs.enabled in ["true",true]
    }

    def getMatchedNodesMaxCount() {
        !grailsApplication.config.rundeck.gui.matchedNodesMaxCount?.isEmpty() ? grailsApplication.config.rundeck.gui.matchedNodesMaxCount?.toInteger() : null
    }

    def getConfiguredMaxPerPage(int defaultMax) {
        if(paginationEnabled) {
            return grailsApplication.config.rundeck.gui.paginatejobs.max.per.page.isEmpty() ? defaultMax : grailsApplication.config.rundeck.gui.paginatejobs.max.per.page.toInteger()
        }
        return defaultMax
    }

    def Map finishquery ( query,params,model){

        if(!params.max){
            params.max=getConfiguredMaxPerPage(10)
        }
        if(!params.offset){
            params.offset=0
        }

        def paginateParams=[:]
        if(query){
            model._filters.each{ key,val ->
                if(null!=query."${key}Filter" && !''.equals(query."${key}Filter")){
                    paginateParams["${key}Filter"]=query."${key}Filter"
                }
            }
            if(null!=query.'idlist' && !''.equals(query.'idlist')){
                paginateParams['idlist']=query.'idlist'
            }
        }
        def displayParams = [:]
        displayParams.putAll(paginateParams)

        if(query.groupPath && query.groupPath!="*"){
            paginateParams['groupPath']=query.groupPath
        }else{
            params.groupPath=null
        }
        if(model.customFilters && !model.customFilters.isEmpty()){
            def totalCustomFilters = [:]
            model.customFilters.each{
                totalCustomFilters.putAll(it)
            }
            if(totalCustomFilters && !totalCustomFilters.isEmpty()){
                paginateParams.customFilters = totalCustomFilters
            }
        }


        def tmod=[max: query?.max?query.max:getConfiguredMaxPerPage(10),
            offset:query?.offset?query.offset:0,
            paginateParams:paginateParams,
            displayParams:displayParams]
        model.putAll(tmod)
        return model
    }
    /**
     * Query for job definitions based on the input
     * @param queryInput
     * @param params parameter map, used to extend query criteria
     * @return
     */
    def listWorkflows(JobQueryInput queryInput, Map params=[:]){
        JobQueryInput query = queryInput
        def jobQueryComponents = applicationContext.getBeansOfType(JobQuery)
        def txtfilters = ScheduledExecutionQuery.TEXT_FILTERS
        def eqfilters=ScheduledExecutionQuery.EQ_FILTERS
        def boolfilters=ScheduledExecutionQuery.BOOL_FILTERS
        def filters = ScheduledExecutionQuery.ALL_FILTERS
        def xfilters = ScheduledExecutionQuery.X_FILTERS
        def totalCustomFilters = []
        Integer queryMax=query.max
        Integer queryOffset=query.offset

        if(paginationEnabled) {
            if (!queryMax) {
                queryMax = getConfiguredMaxPerPage(10)
            }
            if (!queryOffset) {
                queryOffset = 0
            }
        }

        def idlist=[]
        if(query.idlist){

            def arr = query.idlist.split(",")
            arr.each{
                try{
                    idlist<<Long.valueOf(it)
                }catch(NumberFormatException e){
                    idlist<<it
                }
            }

        }

        def crit = ScheduledExecution.createCriteria()

        def scheduled = crit.list{
            if(queryMax && queryMax>0){
                maxResults(queryMax)
            }else{
//                maxResults(10)
            }
            if(queryOffset){
                firstResult(queryOffset.toInteger())
            }

            if(idlist){
                or{
                    idlist.each{ theid->
                        if(theid instanceof Long){
                            eq("id",theid)
                        }else{
                            eq("uuid", theid)
                        }
                    }
                }
            }

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

            def restr = Restrictions.conjunction()
            boolfilters.each{ key,val ->
                if(null!=query["${key}Filter"]){
                    restr.add(Restrictions.eq(val, query["${key}Filter"]))
                }
            }

            if(query.runJobLaterFilter){
                Date now = new Date()
                DetachedCriteria subquery = DetachedCriteria.forClass(Execution.class, "e").with{
                    setProjection Projections.property('e.id')
                    add(Restrictions.gt('e.dateStarted', now))
                    add Restrictions.conjunction().
                            add(Restrictions.eqProperty('e.scheduledExecution.id', 'this.id'))
                }

                add(Restrictions.disjunction().add(Subqueries.exists(subquery)).add(restr))
            } else if(restr.conditions().size() > 0){
                add(restr)
            }

            if('-'==query["groupPath"]||"-"==query["groupPathExact"]){
                or {
                    eq("groupPath", "")
                    isNull("groupPath")
                }
            }else if(query["groupPath"] &&  '*'!=query["groupPath"]){
                or{
                    like("groupPath",query["groupPath"]+"/%")
                    eq("groupPath",query['groupPath'])
                }
            }else if(query["groupPathExact"]){
                or{
                    eq("groupPath",query['groupPathExact'])
                }
            }

            def critDelegate=delegate
            jobQueryComponents?.each { name, jobQuery ->
                def customFilters = jobQuery.extendCriteria(query, params, critDelegate)
                if(customFilters && !customFilters.isEmpty()){
                    totalCustomFilters.add(customFilters)
                }
            }

            if(query && query.sortBy && xfilters[query.sortBy]){
                order(xfilters[query.sortBy],query.sortOrder=='ascending'?'asc':'desc')
            }else{
                if(paginationEnabled) {
                    order("groupPath","asc")
                }
                order("jobName","asc")
            }
        };
        def schedlist = [];
        scheduled.each{
            schedlist << it
        }

        def total = schedlist.size()
        if(queryMax && queryMax>0) {
            //count full result set
            total = ScheduledExecution.createCriteria().count {

                if (idlist) {
                    or {
                        idlist.each { theid ->
                            if (theid instanceof Long) {
                                eq("id", theid)
                            } else {
                                eq("uuid", theid)
                            }
                        }
                    }
                }

                txtfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        ilike(val, '%' + query["${key}Filter"] + '%')
                    }
                }
                eqfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }
                boolfilters.each { key, val ->
                    if (null != query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }


                if('-'==query["groupPath"]||"-"==query["groupPathExact"]){
                    or {
                        eq("groupPath", "")
                        isNull("groupPath")
                    }
                }else if(query["groupPath"] &&  '*'!=query["groupPath"]){
                    or{
                        like("groupPath",query["groupPath"]+"/%")
                        eq("groupPath",query['groupPath'])
                    }
                }else if(query["groupPathExact"]){
                    or{
                        eq("groupPath",query['groupPathExact'])
                    }
                }

                def critDelegate=delegate
                jobQueryComponents?.each { name, jobQuery ->
                    def customFilters = jobQuery.extendCriteria(query, params, critDelegate)
                    if(customFilters && !customFilters.isEmpty()){
                        totalCustomFilters.add(customFilters)
                    }
                }
            }
        }


        return [
            query:query,
            schedlist:schedlist,
            total: total,
            _filters:filters,
            customFilters: totalCustomFilters
            ]

    }


    /**
     * return a map of defined group path to count of the number of jobs with that exact path
     */
    def Map getGroups(project, AuthContext authContext){
        def groupMap=[:]

        //collect all jobs and authorize the user for the set of available Job actions
        Set res = new HashSet()
        def schedlist= ScheduledExecution.findAllByProject(project)
        schedlist.each { ScheduledExecution sched ->
            res.add(rundeckAuthContextProcessor.authResourceForJob(sched))
        }
        // Filter the groups by what the user is authorized to see.

        def decisions = rundeckAuthContextProcessor.authorizeProjectResources(authContext,res,
            new HashSet([AuthConstants.ACTION_READ]),project)

        decisions.each{
            if(it.authorized){
                if(null!= groupMap[it.resource['group']]){
                    groupMap[it.resource['group']]= groupMap[it.resource['group']]+1
                }else{
                    groupMap[it.resource['group']]=1
                }
            }
        }
        return groupMap
    }

    /**
     * Claim schedule for a job with the passed in serverUUID
     * @param scheduledExecution
     * @param serverUUID uuid to assign to the scheduled job
     * @return
     */
    private Map claimScheduledJob(
            ScheduledExecution scheduledExecution,
            String serverUUID,
            String fromServerUUID = null
    )
    {
        def schedId=scheduledExecution.id
        def retry = true
        List<Execution> claimedExecs = []
        Date claimDate = new Date()
        int maxTries = 10
        int tryCount = 0
        while (retry && tryCount < maxTries) {
            try {
//                ScheduledExecution.withNewSession { session -> //TODO: withNewSession dont work on integration test
                    scheduledExecution = ScheduledExecution.get(schedId)
                    scheduledExecution.refresh()

                    if (jobSchedulesService.isScheduled(scheduledExecution.uuid)) {
                        scheduledExecution.serverNodeUUID = serverUUID
                        if (scheduledExecution.save()) {
                            log.info("claimScheduledJob: schedule claimed for ${schedId} on node ${serverUUID}")
                        } else {
                            log.debug("claimScheduledJob: failed for ${schedId} on node ${serverUUID}")
                        }
                    }
                    //claim scheduled adhoc executions
                    Execution.findAllByScheduledExecutionAndStatusAndDateStartedGreaterThanAndDateCompletedIsNull(
                            scheduledExecution,
                            'scheduled',
                            claimDate
                    ).each {
                        it.serverNodeUUID = serverUUID
                        it.save()
                        log.info("claimed adhoc execution ${it.id}")
                        claimedExecs << it
                    }
                    retry = false
//                }
            } catch (org.springframework.dao.ConcurrencyFailureException e) {
                log.error("claimScheduledJob: failed for ${schedId} on node ${serverUUID}: locking failure")
                retry = true
                tryCount++
            } catch (StaleObjectStateException e) {
                log.error("claimScheduledJob: failed for ${schedId} on node ${serverUUID}: stale data")
                retry = true
                tryCount++
            }
        }
        return [claimed: !retry, executions: claimedExecs]
    }

    /**
     * Claim scheduling for any jobs assigned to fromServerUUID, or not assigned if it is null
     * @param toServerUUID uuid to assign to scheduled jobs
     * @param fromServerUUID uuid to claim from, or null to claim from unassigned jobs
     *
     * @return Map of job ID to boolean, indicating whether the job was claimed
     */
    def Map claimScheduledJobs(
            String toServerUUID,
            String fromServerUUID = null,
            boolean selectAll = false,
            String projectFilter = null,
            List<String> jobids = null
    )
    {
        Map claimed = [:]
        def queryFromServerUUID = fromServerUUID
        def queryProject = projectFilter
        ScheduledExecution.withSession { session ->
            def scheduledExecutions = jobSchedulesService.getSchedulesJobToClaim(toServerUUID, queryFromServerUUID, selectAll, queryProject, jobids)
            scheduledExecutions.each { ScheduledExecution se ->
                def orig = se.serverNodeUUID
                if (!claimed[se.extid]) {
                    def claimResult = claimScheduledJob(se, toServerUUID, queryFromServerUUID)
                    claimed[se.extid] = [
                            success   : claimResult.claimed,
                            job       : se,
                            previous  : orig,
                            executions: claimResult.executions
                    ]
                }
            }
            session.flush()
        }
        claimed
    }
    /**
     * Remove all scheduling for job executions, triggered when passive mode is enabled
     * @param serverUUID
     */
    def unscheduleJobs(String serverUUID=null){
        def schedJobs = serverUUID ? jobSchedulesService.getAllScheduled(serverUUID) : jobSchedulesService.getAllScheduled()
        schedJobs.each { ScheduledExecution se ->
            def jobname = se.generateJobScheduledName()
            def groupname = se.generateJobGroupName()

            quartzScheduler.deleteJob(new JobKey(jobname,groupname))
            log.info("Unscheduled job: ${se.id}")
        }

        def results = Execution.isScheduledAdHoc()
        if (serverUUID) {
            results = results.withServerNodeUUID(serverUUID)
        }
        results.list().each { Execution e ->
            ScheduledExecution se = e.scheduledExecution
            def identity = getJobIdent(se, e)
            quartzScheduler.deleteJob(new JobKey(identity.jobname, identity.groupname))
            log.info("Unscheduled job: ${se.id}")
        }
    }

    /**
     * Remove all scheduling for job executions, triggered when passive mode is enabled
     * @param serverUUID
     */
    def unscheduleJobsForProject(String project,String serverUUID=null){
        def schedJobs = serverUUID ? jobSchedulesService.getAllScheduled(serverUUID, project) : jobSchedulesService.getAllScheduled(null, project)
        schedJobs.each { ScheduledExecution se ->
            def jobname = se.generateJobScheduledName()
            def groupname = se.generateJobGroupName()

            quartzScheduler.deleteJob(new JobKey(jobname,groupname))
            log.info("Unscheduled job: ${se.id}")
        }

        def results = Execution.isScheduledAdHoc()
        if (serverUUID) {
            results = results.withServerNodeUUID(serverUUID)
        }
        results = results.withProject(project)

        results.list().each { Execution e ->
            ScheduledExecution se = e.scheduledExecution
            def identity = getJobIdent(se, e)
            quartzScheduler.deleteJob(new JobKey(identity.jobname, identity.groupname))
            log.info("Unscheduled job: ${se.id}")
        }
    }

    def rescheduleJob(ScheduledExecution scheduledExecution) {
        rescheduleJob(scheduledExecution, false, null, null, false)
    }

    /**
     *
     * @param scheduledExecution job
     * @param wasScheduled true if job was previously scheduled
     * @param oldJobName previous Quartz job name
     * @param oldJobGroup previous Quartz group name
     * @param forceLocal true to reschedule locally always
     * @return
     */
    def rescheduleJob(ScheduledExecution scheduledExecution, wasScheduled, oldJobName, oldJobGroup, boolean forceLocal, boolean remoteAssigned = false) {
        if (jobSchedulesService.shouldScheduleExecution(scheduledExecution.uuid) && shouldScheduleInThisProject(scheduledExecution.project)) {
            try {
                return scheduleJob(scheduledExecution, oldJobName, oldJobGroup, forceLocal, remoteAssigned)
            } catch (SchedulerException e) {
                log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
            }
        } else if (wasScheduled && oldJobName && oldJobGroup) {
            return deleteJob(oldJobName, oldJobGroup)
        }

        return false
    }

    /**
     * Reschedule all scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @return
     */
    def rescheduleJobsAsync(String serverUUID=null) {
        executorService.execute{
            rescheduleJobs(serverUUID)
        }
    }

    /**
     * Reschedule all scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @return
     */
    def rescheduleJobs(String serverUUID = null, String project = null) {
        Date now = new Date()
        def succeededJobs = []
        def failedJobs = []
        // Reschedule jobs on fixed schedules
        def scheduledList = jobSchedulesService.getAllScheduled(serverUUID, project)
        scheduledList.each { ScheduledExecution se ->
            try {
                def nexttime = null
                def nextExecNode = null
                (nexttime, nextExecNode) = scheduleJob(se, null, null, true)
                succeededJobs << [job: se, nextscheduled: nexttime]
                log.info("rescheduled job in project ${se.project}: ${se.extid}")
            } catch (Exception e) {
                failedJobs << [job: se, error: e.message]
                log.error("Job not rescheduled in project ${se.project}: ${se.extid}: ${e.message}", e)
                //log.error(e)
            }
        }

        // Reschedule any executions which were scheduled ad hoc
        def results = Execution.isScheduledAdHoc()
        if (serverUUID) {
            results = results.withServerNodeUUID(serverUUID)
        }
        if(project) {
            results = results.withProject(project)
        }
        def executionList = results.list()

        def adhocRescheduleResult = rescheduleOnetimeExecutions(executionList)

        [jobs: succeededJobs, failedJobs: failedJobs, executions: adhocRescheduleResult.executions, failedExecutions: adhocRescheduleResult.failedExecutions]
    }

    /**
     * Reschedule the provided one-time executions. Invalid executions will be cleaned up.
     * @param executionList The list of executions to reschedule.
     * @return A map with: <pre>
     *   [executions: List, // succeeded executions<br>
     *   failedExecutions: List] // failed executions
     * </pre>
     */
    def rescheduleOnetimeExecutions(List<Execution> executionList) {

        Date now = new Date()
        // Reschedule any executions which were scheduled for one time execution.

        List<Execution> cleanupExecutions   = []
        def succeedExecutions = []

        executionList.each { Execution e ->
            boolean ok = true
            ScheduledExecution se = e.scheduledExecution

            if (se.options.find { it.secureInput } != null) {
                log.error("One-time execution not rescheduled: ${se.jobName} [${e.id}]: " +
                    "cannot reschedule automatically as it has secure input options")
                ok = false
            } else if (e.dateStarted == null) {
                log.error("One-time execution not rescheduled: ${se.jobName} [${e.id}]: " +
                    "no start time is set: ${e}")
                ok = false
            } else if (e.dateStarted.before(now)) {
                log.error("One-time execution not rescheduled: ${se.jobName} [${e.id}]: " +
                    "the schedule time has past")
                ok = false
            }

            if (ok) {
                log.info("Rescheduling one-time execution of: " +
                                 "${se.jobName} [${e.id}]: ${e.dateStarted}"
                )
                try {
                    AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject(
                            e.user ?: se.user,
                            e.userRoles ?: se.userRoles,
                            e.project
                    )
                    Date nexttime = scheduleAdHocJob(
                            se,
                            authContext.username,
                            authContext,
                            e,
                            null,
                            null,
                            e.dateStarted,
                            false
                    )
                    if (nexttime) {
                        succeedExecutions << [execution: e, time: nexttime]
                    }
                } catch (Exception ex) {
                    log.error("One Time job not rescheduled: ${se.jobName}: ${ex.message}", ex)
                    ok = false
                }
            }
            if (!ok) {
                // Mark this execution to be cleaned up (killed)
                cleanupExecutions.add(e)
            }
        }

        if (!cleanupExecutions.isEmpty()) {
            log.error("${cleanupExecutions.size()} one-time scheduled executions " +
                "could not be rescheduled and will be killed")
            executionService.cleanupRunningJobs(cleanupExecutions)
        }
        [executions: succeedExecutions, failedExecutions: cleanupExecutions]
    }

    /**
     * Claim scheduling of jobs from the given fromServerUUID, and return a map identifying successfully claimed jobs
     * @param fromServerUUID server UUID to claim scheduling of jobs from
     * @return map of job ID to [success:boolean, job:ScheduledExecution] indicating reclaim was successful or not.
     */
    def reclaimAndScheduleJobs(String fromServerUUID, boolean all=false, String project=null, List<String> ids=null) {
        def toServerUuid = frameworkService.getServerUUID()
        if (toServerUuid == fromServerUUID) {
            return [:]
        }
        def claimed = claimScheduledJobs(toServerUuid, fromServerUUID, all, project, ids)
        if (claimed.find { it.value.success }) {
            rescheduleJobs(toServerUuid)
        }
        claimed
    }

    /**
     *  Return a Map with a tree structure of the available grouppaths, and their job counts
     * <pre>
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
     * </pre>
     */
    def Map getGroupTree(project, AuthContext authContext){
        def groupMap = getGroups(project, authContext)
        def tree=[:]
        groupMap.keySet().each{
            tree[it]=[]
        }
        return tree
    }

    /**
     *
     * @param maxDepth
     * @param workflow
     * @param readAuth if true, includes contents of each step, if false only includes only basic step details
     * @return List of maps for each step, descend up to maxDepth following job references
     */
    def getWorkflowDescriptionTree(String project,Workflow workflow,readAuth,maxDepth=3){
        def jobids=[:]
        def cmdData={}
        cmdData={x,WorkflowStep step->
            def map=readAuth?step.toMap():step.toDescriptionMap()
            map.remove('plugins')
            if(map.type){
                map.remove('configuration')
            }
            if(step instanceof JobExec) {
                ScheduledExecution refjob = step.findJob(project)
                if(!step.useName && step.uuid){
                    if(refjob) {
                        map.jobref.name = refjob.jobName
                        map.jobref.group = refjob.groupPath
                    }
                }

                if(refjob){
                    map.jobId=refjob.extid
                    boolean doload=(null==jobids[map.jobId])
                    if(doload){
                        jobids[map.jobId]=[]
                    }
                    if(doload && x>0){
                        map.workflow=jobids[map.jobId]
                        jobids[map.jobId].addAll(refjob.workflow.commands.collect(cmdData.curry(x-1)))
                    }
                }
            }
            def eh = step.errorHandler

            if(eh instanceof JobExec) {
                ScheduledExecution refjob = eh.findJob(project)
                if(refjob){
                    map.ehJobId=refjob.extid
                    boolean doload=(null==jobids[map.ehJobId])
                    if(doload){
                        jobids[map.ehJobId]=[]
                    }
                    if(doload && x>0){
                        map.ehWorkflow=jobids[map.ehJobId]
                        jobids[map.ehJobId].addAll(refjob.workflow.commands.collect(cmdData.curry(x-1)))
                    }
                }
            }
            return map
        }
        workflow.commands.collect(cmdData.curry(maxDepth))
    }
    /**
     * Delete all executions for a job. Return a map with results, as {@link ExecutionService#deleteBulkExecutionIds(java.util.Collection, com.dtolabs.rundeck.core.authorization.AuthContext, java.lang.String)}
     * @param scheduledExecution
     * @param authContext @param var
     */
    def deleteJobExecutions(ScheduledExecution scheduledExecution, AuthContext authContext, def username){
        Execution.withTransaction {
            //unlink any Execution records
            def executions = Execution.findAllByScheduledExecution(scheduledExecution)
            def results=executionService.deleteBulkExecutionIds(executions*.id, authContext, username)
            return results
        }
    }

    /**
     * Immediately delete a ScheduledExecution
     * @param username @param scheduledExecution
     * @return
     */
    def deleteScheduledExecution(ScheduledExecution scheduledExecution, boolean deleteExecutions=false,
                                 AuthContext authContext=null, String username){
        scheduledExecution = ScheduledExecution.get(scheduledExecution.id)
        def originalRef=jobEventRevRef(scheduledExecution)
        def jobname = scheduledExecution.generateJobScheduledName()
        def groupname = scheduledExecution.generateJobGroupName()
        def errmsg=null
        def success = false
        Execution.withTransaction {
            //find any currently running executions for this job, and if so, throw exception
            def found = Execution.createCriteria().get {
                delegate.'scheduledExecution' {
                    eq('id', scheduledExecution.id)
                }
                isNotNull('dateStarted')
                isNull('dateCompleted')
            }

            if (found) {
                errmsg = 'Cannot delete {{Job ' + scheduledExecution.extid + '}} "' + scheduledExecution.jobName  +
                        '" it is currently being executed: {{Execution ' + found.id + '}}'
                return [success:false,error:errmsg]
            }
            def stats= ScheduledExecutionStats.findAllBySe(scheduledExecution)
            if(stats){
                stats.each { st ->
                    st.delete()
                }
            }
            def refExec = ReferencedExecution.findAllByScheduledExecution(scheduledExecution)
            if(refExec){
                refExec.each { re ->
                    re.delete()
                }
            }
            //unlink any Execution records
            def result = Execution.findAllByScheduledExecution(scheduledExecution)
            if(deleteExecutions){
                executionService.deleteBulkExecutionIds(result*.id,authContext, username)
            }else{

                result.each { Execution exec ->
                    exec.scheduledExecution = null
                }
            }
            fileUploadService.deleteRecordsForScheduledExecution(scheduledExecution)
            //before delete job
            rundeckJobDefinitionManager.beforeDelete(scheduledExecution, authContext)
            try {
                scheduledExecution.delete(flush: true)
                deleteJob(jobname, groupname)
                success = true
            } catch (org.springframework.dao.ConcurrencyFailureException e) {
                scheduledExecution.discard()
                errmsg = 'Cannot delete Job "' + scheduledExecution.jobName + '" [' + scheduledExecution.extid + ']: it may have been modified or executed by another user'
            } catch (StaleObjectStateException e) {
                scheduledExecution.discard()
                errmsg = 'Cannot delete Job "' + scheduledExecution.jobName + '" [' + scheduledExecution.extid + ']: it may have been modified or executed by another user'
            }
        }
        if(success){

            //after delete job
            rundeckJobDefinitionManager.afterDelete(scheduledExecution, authContext)
            def event = createJobChangeEvent(JobChangeEvent.JobChangeEventType.DELETE, scheduledExecution, originalRef)

            //issue event directly
            notify('jobChanged', event)
        }
        return [success:success,error:errmsg]
    }
    /**
     * Attempt to delete a job given an id
     * @param jobid
     * @param original auth context
     * @param deleteBulkExecutions true to delete all executions of the job
     * @param user user requesting delete action
     * @param callingAction name of action/method requesting delete for logging
     *
     * @return map [error: [message: String, errorCode: String, id: String, job: ScheduledExecution?], success:boolean]
     */
    def deleteScheduledExecutionById(jobid, AuthContext original, boolean deleteExecutions, String user,
    String callingAction){

        def ScheduledExecution scheduledExecution = getByIDorUUID(jobid)
        if (!scheduledExecution) {
            def err = [
                    message: lookupMessage( "api.error.item.doesnotexist",  ['Job ID', jobid]),
                    errorCode: 'notfound',
                    id: jobid
            ]
            return [error: err,success: false]
        }

        //extend auth context using project-specific authorization
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextWithProject(original, scheduledExecution.project)

        def authActions = [AuthConstants.ACTION_DELETE]
        if (callingAction == 'scm-import') {
            authActions << AuthConstants.ACTION_SCM_DELETE
        }
        if ((
            !rundeckAuthContextProcessor.authorizeProjectResourceAny(
                authContext,
                AuthConstants.RESOURCE_TYPE_JOB,
                authActions,
                scheduledExecution.project
            ) || !rundeckAuthContextProcessor.authorizeProjectJobAny(
                authContext,
                scheduledExecution,
                authActions,
                scheduledExecution.project
            )
        )) {
            def err = [
                    message: lookupMessage('api.error.item.unauthorized', ['Delete', 'Job ID', scheduledExecution.extid]),
                    errorCode: 'unauthorized',
                    id: scheduledExecution.extid,
                    job: scheduledExecution
            ]
            return [error: err,success: false]
        }
        def changeinfo = [user: user, method: callingAction, change: 'delete']
        def jobdata = scheduledExecution.properties
        def jobtitle = "[" + scheduledExecution.extid + "] " + scheduledExecution.generateFullName()
        def result = deleteScheduledExecution(scheduledExecution, deleteExecutions, authContext, user)
        if (!result.success) {
            return [success:false,error:  [message: result.error, job: scheduledExecution, errorCode: 'failed', id: scheduledExecution.extid]]
        } else {
            logJobChange(changeinfo, jobdata)
            return [success: [message: lookupMessage('api.success.job.delete.message', [jobtitle]), job:
                    scheduledExecution]]
        }
    }
    /**
     * Delete a quartz job by name/group
     * @param jobname
     * @param groupname
     * @return
     */
    def deleteJob(String jobname, String groupname){
        jobSchedulerService.deleteJobSchedule(jobname, groupname)
    }

    def deleteCleanerExecutionsJob(String projectName){
        jobSchedulerService.deleteJobSchedule(projectName, CLEANER_EXECUTIONS_JOB_GROUP_NAME)
    }

    def userAuthorizedForJob(ScheduledExecution se, AuthContext authContext){
        return rundeckAuthContextProcessor.authorizeProjectJobAll(authContext,se,[AuthConstants.ACTION_READ],se.project)
    }
    def userAuthorizedForAdhoc(request,ScheduledExecution se, AuthContext authContext){
        return rundeckAuthContextProcessor.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                AuthConstants.ACTION_RUN,se.project)
    }

    def scheduleJob(ScheduledExecution se, String oldJobName, String oldGroupName, boolean forceLocal=false, boolean remoteAssigned = false) {
        def jobid = "${se.generateFullName()} [${se.extid}]"
        def jobDesc = "Attempt to schedule job $jobid in project $se.project"
        if (!executionService.executionsAreActive) {
            log.warn("$jobDesc, but executions are disabled.")
            return [null, null]
        }

        if(!shouldScheduleInThisProject(se.project)){
            log.warn("$jobDesc, but project executions are disabled.")
            return [null, null]
        }

        if (!jobSchedulesService.shouldScheduleExecution(se.uuid)) {
            log.warn(
                    "$jobDesc, but job execution is disabled."
            )
            return [null, null];
        }

        def data=["project": se.project,
                  "jobId":se.uuid,
                  "oldQuartzJobName": oldJobName,
                  "oldQuartzGroupName": oldGroupName]

        if(!forceLocal && frameworkService.isClusterModeEnabled()){
            boolean remoteAssign = remoteAssigned ?: jobSchedulerService.scheduleRemoteJob(data)

            if(remoteAssign){
                return [null, null]
            }
        }

        def jobDetail = createJobDetail(se)
        jobDetail.getJobDataMap().put("bySchedule", true)
        def Date nextTime
        if(oldJobName && oldGroupName){
            log.info("$jobid was renamed, removing old job and scheduling new one")
            deleteJob(oldJobName,oldGroupName)
        }
        if ( hasJobScheduled(se) ) {
            log.info("rescheduling existing job in project ${se.project} ${se.extid}: " + se.generateJobScheduledName())
            def result = jobSchedulesService.handleScheduleDefinitions(se.uuid, true)
            nextTime = result? result.nextTime: null
        } else {
            log.info("scheduling new job in project ${se.project} ${se.extid}: " + se.generateJobScheduledName())
            def result = jobSchedulesService.handleScheduleDefinitions(se.uuid, false)
            nextTime = result? result.nextTime: null
        }

        log.info("scheduled job ${se.extid}. next run: " + nextTime.toString())
        return [nextTime, jobDetail?.getJobDataMap()?.get("serverUUID")]
    }

    /**
     * Schedule a job, ad-hoc.
     *
     * The schedule time is required and must be in the future.
     *
     * @param se the job
     * @param   user                user running this job
     * @param authContext the auth context
     * @param e the execution
     * @param secureOpts secure authentication input
     * @param secureOptsExposed secure input
     * @param   startTime           the time to start running the job
     * @param pending if the job should be scheduled in a pending/paused state
     * @return  the scheduled date/time as returned by Quartz, or null if it couldn't be scheduled
     * @throws  IllegalArgumentException    if the schedule time is not set, or if it is in the past
     */
    def Date scheduleAdHocJob(
            ScheduledExecution se,
            String user,
            AuthContext authContext,
            Execution e,
            Map secureOpts,
            Map secureOptsExposed,
            Date startTime,
            boolean pending
    )
    {
        if (!executionService.executionsAreActive) {
            log.warn("Attempt to schedule job ${se}, but executions are disabled.")
            return null
        }
        if (!isProjectExecutionEnabled(se.project)) {
            log.warn("Attempt to schedule job ${se}, but project executions are disabled.")
            return null
        }


        if (startTime == null) {
            throw new IllegalArgumentException("Scheduled date and time must be present")
        }

        java.util.Calendar now = java.util.Calendar.getInstance()
        if (startTime.before(now.getTime())) {
            throw new IllegalArgumentException("Cannot schedule a job in the past")
        }

        log.debug("ScheduledExecutionService: will schedule job at ${startTime}")
        def identity = getJobIdent(se, e)
        Map jobDetail = createJobDetailMap(se) + [
                bySchedule  : true,
                user        : user,
                authContext : authContext,
                executionId : e.id.toString(),
                retryAttempt: 0
        ]
        if (secureOpts) {
            jobDetail["secureOpts"] = secureOpts
        }
        if (secureOptsExposed) {
            jobDetail["secureOptsExposed"] = secureOptsExposed
        }


        try {
            fileUploadService.executionBeforeSchedule(new ExecutionPrepareEvent(
                    execution: e,
                    job: se,
                    options: executionService.parseJobOptsFromString(se, e.argString)
            )
            )
        } catch (FileUploadServiceException exc) {
            log.warn("Failed uploaded file preparation for scheduled job: $exc", exc)
        }

        try {
            return jobSchedulerService.scheduleJob(identity.jobname, identity.groupname, jobDetail, startTime, pending)
        } catch (JobScheduleFailure exc) {
            throw new ExecutionServiceException("Could not schedule job: " + exc.message, exc)
        }
    }

    Date scheduleCleanerExecutionsJob(String projectName, String cronExpression, Map config) {
        Date nextTime
        def trigger = localCreateTrigger(projectName, CLEANER_EXECUTIONS_JOB_GROUP_NAME, cronExpression, 1)
        JobDetail jobDetail = createCleanerExecutionJobDetail(projectName, CLEANER_EXECUTIONS_JOB_GROUP_NAME, config)

        if ( hasJobScheduled(projectName, CLEANER_EXECUTIONS_JOB_GROUP_NAME) ) {
            log.info("rescheduling existing cleaner execution job in project ${projectName}")

            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(projectName, CLEANER_EXECUTIONS_JOB_GROUP_NAME), trigger)
        } else {
            log.info("scheduling new cleaner execution job in project ${projectName}")
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        log.info("scheduled cleaner executions job next run: " + nextTime.toString())
        return nextTime
    }

    /**
     *
     * @param se job
     * @param e execution
     * @return quartz scheduler fire instance Id
     */
    def String findExecutingQuartzJob(ScheduledExecution se, Execution e) {
        String found = null
        def ident = getJobIdent(se, e)

        quartzScheduler.getCurrentlyExecutingJobs().each { def JobExecutionContext jexec ->
            if (jexec.getJobDetail().key.getName() == ident.jobname &&
                    jexec.getJobDetail().key.getGroup() == ident.groupname) {
                def job = jexec.getJobInstance()
                if (job instanceof ExecutionJob && e.id == job.executionId) {
                    found = jexec.fireInstanceId
                }
            }
        }

        return found
    }

    /**
     *
     * @param id execution id
     * @return quartz scheduler JobExecutionContext
     */
    def JobExecutionContext findExecutingQuartzJob(Long id) {
        JobExecutionContext found = null
        quartzScheduler.getCurrentlyExecutingJobs().each {def JobExecutionContext jexec ->
            def job = jexec.getJobInstance()
            if (job instanceof ExecutionJob && id == job.executionId) {
                found = jexec
            }
        }

        return found
    }

    /**
     * Interrupt a running quartz job if present or optinoally delete from scheduler if not
     * @param quartzIntanceId quartz fire instance Id
     * @param jobName
     * @param groupName
     * @param deleteFromScheduler
     * @return true if the job was interrupted or deleted
     */
    def boolean interruptJob(
            String quartzIntanceId,
            String jobName,
            String groupName,
            boolean deleteFromScheduler = false
    )
    {
        def didCancel = quartzIntanceId ? quartzScheduler.interrupt(quartzIntanceId) : false

        /** If the job has not started yet, it will not be included in currently executing jobs **/
        if (!didCancel && deleteFromScheduler) {
            JobKey jobKey = new JobKey(jobName, groupName)
            if (quartzScheduler.deleteJob(jobKey)) {
                didCancel = true
            }
        }

        return didCancel
    }

    Map<String, String> getJobIdent(ScheduledExecution se, Execution e){
        Map<String, String> ident

        if (!se) {
            ident = [jobname:"TEMP:"+e.user +":"+e.id, groupname:e.user+":run"]
        } else if (se.scheduled && e.executionType == "scheduled" && !e.retryAttempt) {
            // For jobs which have fixed schedules
            ident = [jobname:se.generateJobScheduledName(),groupname:se.generateJobGroupName()]
        } else {
            ident = [jobname:"TEMP:"+e.user +":"+se.id+":"+e.id, groupname:e.user+":run:"+se.id]
        }

        return ident
    }

    /**
     * Schedule a stored job to execute immediately, include a set of params in the data map
     * @param se the job
     * @param user the user running the job
     * @param authContext the auth context
     * @param e the execution
     * @param secureOpts the secure authentication input
     * @param secureOptsExposed the secure input
     * @param retryAttempt the retry attempt
     * @return the execution id
     */
    def long scheduleTempJob(
            ScheduledExecution se,
            String user,
            AuthContext authContext,
            Execution e,
            Map secureOpts,
            Map secureOptsExposed,
            int retryAttempt
    ) throws ExecutionServiceException
    {
        def ident = getJobIdent(se, e)
        def jobDetail = createJobDetailMap(se) + [
                user        : user,
                authContext : authContext,
                executionId : e.id.toString(),
                retryAttempt: retryAttempt ?: 0
        ]
        if (secureOpts) {
            jobDetail["secureOpts"] = secureOpts
        }
        if (secureOptsExposed) {
            jobDetail["secureOptsExposed"] = secureOptsExposed
        }
        try {
            if (retryAttempt > 0 && e.retryDelay) {
                long retryTime = Sizes.parseTimeDuration(e.retryDelay, TimeUnit.MILLISECONDS)
                Date now = new Date()
                jobSchedulerService.scheduleJob(
                        ident.jobname,
                        ident.groupname,
                        jobDetail,
                        new Date(now.getTime() + retryTime),
                        true)
            } else {
                jobSchedulerService.scheduleJobNow(ident.jobname, ident.groupname, jobDetail, true)
            }
        } catch (JobScheduleFailure exc) {
            throw new ExecutionServiceException("Could not schedule job: " + exc.message, exc)
        }

        return e.id
    }

    /**
     * Schedule a temp job to execute immediately.
     */
    def Map scheduleTempJob(AuthContext authContext, Execution e) {
        if(!executionService.getExecutionsAreActive()){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if(!isProjectExecutionEnabled(e.project)){
            def msg=g.message(code:'project.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if (!e.hasExecutionEnabled()) {
            def msg=g.message(code:'scheduleExecution.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        def ident = getJobIdent(null, e);
        def jobDetail = JobBuilder.newJob(ExecutionJob)
                .withIdentity(ident.jobname, ident.groupname)
                .withDescription("Execute command: " + e)
                .usingJobData(
                    new JobDataMap(
                        [
                            'isTempExecution': 'true',
                            'executionId': e.id.toString(),
                            'authContext': authContext
                        ]
                    )
                )
                .build()



        def Trigger trigger = TriggerBuilder.newTrigger().withIdentity(ident.jobname + "Trigger").startNow().build()
        def nextTime
        try {
            log.info("scheduling temp job: " + ident.jobname)
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        } catch (Exception exc) {
            throw new RuntimeException("caught exception while adding job: " + exc.getMessage(), exc)
        }
        return [success:true,execution:e,id:e.id]
    }

    @NotTransactional
    JobDetail createJobDetail(ScheduledExecution se) {
        return createJobDetail(se,se.generateJobScheduledName(), se.generateJobGroupName())
    }

    @NotTransactional
    Map createJobDetailMap(ScheduledExecution se) {
        Map data = [:]
        data.put("scheduledExecutionId", se.id.toString())
        data.put("rdeck.base", frameworkService.getRundeckBase())

        if(se.scheduled){
            data.put("userRoles", se.userRoleList)
            if(frameworkService.isClusterModeEnabled()){
                data.put("serverUUID", frameworkService.getServerUUID())
            }
        }

        return data
    }

    @NotTransactional
    JobDetail createJobDetail(ScheduledExecution se, String jobname, String jobgroup) {
        def jobDetailBuilder = JobBuilder.newJob(ExecutionJob)
                                         .withIdentity(jobname, jobgroup)
                                         .withDescription(se.description)
                                         .usingJobData(new JobDataMap(createJobDetailMap(se)))


        return jobDetailBuilder.build()
    }

    def JobDetail createCleanerExecutionJobDetail(String jobname, String jobgroup, Map config) {
        String description = "Cleaner executions job"
        def jobDetailBuilder = JobBuilder.newJob(ExecutionsCleanUp)
                                         .withIdentity(jobname, jobgroup)
                                         .withDescription(description)
                                         .usingJobData(new JobDataMap(config))


        return jobDetailBuilder.build()
    }

    boolean hasJobScheduled(ScheduledExecution se) {
        return quartzScheduler.checkExists(JobKey.jobKey(se.generateJobScheduledName(),se.generateJobGroupName()))
    }

    boolean hasJobScheduled(String jobName, String jobGroup) {
        return quartzScheduler.checkExists(JobKey.jobKey(jobName, jobGroup))
    }

    /**
     * Return a map of job ID to next trigger Date
     * @param scheduledExecutions
     * @return
     */
    Map nextExecutionTimes(Collection<ScheduledExecution> scheduledExecutions, boolean require=false) {
        def map = [ : ]
        scheduledExecutions.each {
            def next = jobSchedulesService.nextExecutionTime(it.uuid, require)
            if(next){
                map[it.id] = next
            }
        }
        return map
    }

    /**
     * Return a map of job ID to serverNodeUUID for any jobs which are scheduled on a different server, if cluster mode is enabled.
     * @param scheduledExecutions
     * @return
     */
    def Map clusterScheduledJobs(Collection<ScheduledExecution> scheduledExecutions) {
        def map = [ : ]
        if(frameworkService.isClusterModeEnabled()) {
            def serverUUID = frameworkService.getServerUUID()
            scheduledExecutions.findAll { it.serverNodeUUID != serverUUID }.each {
                map[it.id] = it.serverNodeUUID
            }
        }
        return map
    }

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    Date nextExecutionTime(ScheduledExecution se, boolean require=false) {
        jobSchedulesService.nextExecutionTime(se.uuid, require)
    }

    /**
     * Find a ScheduledExecution by UUID or ID.  Checks if the
     * input value is a Long, if so finds the ScheduledExecution with that ID.
     * If it is a String it attempts to parse the String as a Long and if it is
     * valid it finds the ScheduledExecution by ID. Otherwise it attempts to find the ScheduledExecution with that
     * UUID.
     * @param anid
     * @return ScheduledExecution found or null
     */
    def ScheduledExecution getByIDorUUID(anid){
        ScheduledExecution.getByIdOrUUID(anid)
    }

    /**
     * Get the locale
     * @return locale
     * */
    def getLocale() {
        def Locale locale = null
        try {
            locale = RequestContextUtils.getLocale(getSession().request)
        }
        catch (java.lang.IllegalStateException e) {
            //log.debug "Running in console?"
        }
        //log.debug "locale: ${locale}"
        return locale
    }
    /**
     * @parameter key
     * @returns corresponding value from messages.properties
     */
    def lookupMessage(String theKey, List<Object> data, String defaultMessage = null) {
        def locale = getLocale()
        def theValue = null
//        MessageSource messageSource = applicationContext.getBean("messageSource")
        try {
            theValue = messageSource.getMessage(theKey, data as Object[], locale)
        } catch (org.springframework.context.NoSuchMessageException e) {
            log.error "Missing message ${theKey}"
//        } catch (java.lang.NullPointerException e) {
//            log.error "Expression does not exist: ${theKey}: ${e}"
        }
        if (null == theValue && defaultMessage) {
            MessageFormat format = new MessageFormat(defaultMessage);
            theValue = format.format(data as Object[])
        }
        return theValue
    }
    /**
     * @parameter key
     * @returns corresponding value from messages.properties
     */
    def lookupMessageError(error, String defaultMessage = null) {
        def locale = getLocale()
        def theValue = null
//        MessageSource messageSource = applicationContext.getBean("messageSource")
        try {

            theValue = messageSource.getMessage(error, locale)
        } catch (org.springframework.context.NoSuchMessageException e) {
            log.error "Missing message ${error}"
//        } catch (java.lang.NullPointerException e) {
//            log.error "Expression does not exist: ${error}: ${e}"
        }
        if (null == theValue && defaultMessage) {
            MessageFormat format = new MessageFormat(defaultMessage);
            theValue = format.format(null)
        }
        return theValue
    }
    /**
     * Get the HTTP Session
     * @return session
     * */
    private HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }


    /**
     * Given list of imported jobs, create, update or skip them as defined by the dupeOption parameter.
     * @return map of load results, [jobs: List of ScheduledExecutions, jobsi: list of maps [scheduledExecution:
     * (job), entrynum: (index)], errjobs: List of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg],
     * skipjobs: list of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]]
     */
    @Deprecated
    def loadJobs(
            List<ScheduledExecution> jobset,
            String option,
            String uuidOption,
            Map changeinfo = [:],
            UserAndRolesAuthContext authContext,
            Boolean validateJobref = false
    ) {
        loadImportedJobs(
                jobset.collect { RundeckJobDefinitionManager.importedJob(it) },
                option,
                uuidOption,
                changeinfo,
                authContext,
                validateJobref

        )
    }
    /**
     * Given list of imported jobs, create, update or skip them as defined by the dupeOption parameter.
     * @return map of load results, [jobs: List of ScheduledExecutions, jobsi: list of maps [scheduledExecution:
     * (job), entrynum: (index)], errjobs: List of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg],
     * skipjobs: list of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]]
     */
    def loadImportedJobs(
            List<ImportedJob<ScheduledExecution>> jobset,
            String option,
            String uuidOption,
            Map changeinfo = [:],
            UserAndRolesAuthContext authContext,
            Boolean validateJobref = false
    ) {
        def jobs = []
        def jobsi = []
        def i = 1
        def errjobs = []
        def skipjobs = []
        def jobChangeEvents = []
        def remappedIds = [:]

        def updateAuthActions = [AuthConstants.ACTION_UPDATE]
        def createAuthActions = [AuthConstants.ACTION_CREATE]
        if (changeinfo?.method == 'scm-import') {
            updateAuthActions += [AuthConstants.ACTION_SCM_UPDATE]
            createAuthActions += [AuthConstants.ACTION_SCM_CREATE]
        }
        jobset.each { importedJob ->
            def jobdata = importedJob.job
            log.debug("saving job data: ${jobdata}")
            def ScheduledExecution scheduledExecution
            def jobchange = new HashMap(changeinfo)
            if(!jobdata.project){
                errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "Project was not specified"]
                i++
                return
            }
            if(!frameworkService.existsFrameworkProject(jobdata.project)){
                errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "Project does not exist: ${jobdata.project}"]
                i++
                return
            }
            String origid=jobdata.uuid?:jobdata.id
            if (uuidOption == 'remove') {
                jobdata.uuid = null
                jobdata.id = null
            }
            if (option == "update" || option == "skip") {
                //look for dupe by name and group path and project
                def schedlist
                //first look for uuid
                if (jobdata.uuid && jobdata.project) {
                    scheduledExecution = ScheduledExecution.findByUuidAndProject(jobdata.uuid,jobdata.project)
                } else if(jobdata.jobName && jobdata.project){
                    schedlist = ScheduledExecution.findAllScheduledExecutions(jobdata.groupPath, jobdata.jobName, jobdata.project)
                    if (schedlist && 1 == schedlist.size()) {
                        scheduledExecution = schedlist[0]
                    }
                }
            }

            def project = scheduledExecution ? scheduledExecution.project : jobdata.project
            def projectAuthContext = rundeckAuthContextProcessor.getAuthContextWithProject(authContext, project)

            def handleResult={result->
                def errorStrings=[]
                def errdata=[:]
                def success = result.success
                scheduledExecution = result.scheduledExecution
                if (!success) {
                    if(result.error){
                        errorStrings << result.error
                    }
                    if(scheduledExecution && scheduledExecution.hasErrors()){
                        errorStrings.addAll scheduledExecution.errors.allErrors.collect { lookupMessageError(it) }
                        errdata["validation"] = scheduledExecution.errors.allErrors.collect { lookupMessageError(it) }
                    }
                    if(result.validation){
                        errorStrings << "Validation errors for: "+result.validation.keySet().join(', ')
                        errdata.putAll result.validation
                    }
                    if(!errorStrings){
                        errorStrings << "Failed to save job: $result"
                    }
                } else {
                    logJobChange(jobchange, scheduledExecution.properties)
                    jobChangeEvents<<result.jobChangeEvent
                }
                [
                    success           : success,
                    errmsgs           : errorStrings,
                    errmsg            : errorStrings.join('\n'),
                    errdata           : errdata,
                    scheduledExecution: scheduledExecution
                ]
            }

            if (option == "skip" && scheduledExecution) {
                jobdata.id = scheduledExecution.id
                skipjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "A Job named '${jobdata.jobName}' already exists"]
            }
            else if (option == "update" && scheduledExecution) {
                def success = false
                def errmsg=''
                def errmsgs=[]
                def errdata=[:]
                jobchange.change = 'modify'
                if (!rundeckAuthContextProcessor.authorizeProjectJobAny(
                    projectAuthContext,
                    scheduledExecution,
                    updateAuthActions,
                    scheduledExecution.project
                )) {
                    errmsg = "Unauthorized: Update Job ${scheduledExecution.id}"
                } else {
                    try {
                        def result = _doupdateJob(
                                scheduledExecution.id,
                                importedJob,
                                projectAuthContext,
                                jobchange,
                                validateJobref
                        )
                        def xresult = handleResult(result)
                        success = xresult.success
                        scheduledExecution = xresult.scheduledExecution
                        errmsg = xresult.errmsg
                        errdata = xresult.errdata
                        errmsgs = xresult.errmsgs

                    } catch (Exception e) {
                        errmsg = e.getMessage()
                        System.err.println("caught exception: " + errmsg);
                        e.printStackTrace()
                    }
                }
                if (!success) {
                    errjobs << [scheduledExecution: scheduledExecution, entrynum: i, errmsg: errmsg, errdata: errdata, errmsgs: errmsgs]
                } else {
                    jobs << scheduledExecution
                    jobsi << [scheduledExecution: scheduledExecution, entrynum: i]
                }
            } else if (option == "create" || !scheduledExecution) {
                def errmsg=''
                def errdata=[:]
                def errmsgs=[]
                def success=false

                if (!rundeckAuthContextProcessor.authorizeProjectResourceAny(
                    projectAuthContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    createAuthActions,
                    jobdata.project
                )) {
                    errmsg = "Unauthorized: Create Job"
                    errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]
                } else {
                    try {
                        jobchange.change = 'create'
                        def result = _docreateJobOrParams(
                            importedJob,
                            [:],
                            projectAuthContext,
                            changeinfo,
                            validateJobref
                        )

                        def xresult=handleResult(result)
                        success = xresult.success
                        scheduledExecution = xresult.scheduledExecution
                        errmsg = xresult.errmsg
                        errdata = xresult.errdata
                        errmsgs = xresult.errmsgs

                    } catch (Exception e) {
                        System.err.println("caught exception");
                        e.printStackTrace()
                        scheduledExecution = jobdata
                        errmsg = e.getMessage()
                    }
                    if (!success) {
                        errjobs << [scheduledExecution: scheduledExecution && !scheduledExecution.id?scheduledExecution:jobdata, entrynum: i, errmsg: errmsg, errdata: errdata, errmsgs: errmsgs]
                    } else {
                        jobs << scheduledExecution
                        jobsi << [scheduledExecution: scheduledExecution, entrynum: i]
                    }
                }
            }
            if ( scheduledExecution && origid && origid != scheduledExecution.extid) {
                remappedIds[scheduledExecution.extid] = origid
            }

            i++

        }
        return [jobs: jobs, jobsi: jobsi, errjobs: errjobs, skipjobs: skipjobs,jobChangeEvents:jobChangeEvents,idMap:remappedIds]
    }
    static Logger jobChangeLogger = LoggerFactory.getLogger("com.dtolabs.rundeck.data.jobs.changes")

    def logJobChange(data, jobdata) {
        data.keySet().each {k ->
            def v = data[k]
            if (v instanceof Date) {
                //TODO: reformat date
                MDC.put(k, v.toString())
                MDC.put("${k}Time", v.time.toString())
            } else if (v instanceof String) {
                MDC.put(k, v ? v : "-")
            } else {
                final string = v.toString()
                MDC.put(k, string ? string : "-")
            }
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            final var = jobdata[k]
            MDC.put(k, var ? var.toString() : '-')
        }
        if (jobdata.uuid) {
            MDC.put('id', jobdata.uuid)
        }
        final msg = data.user + " " + data.change.toUpperCase() + " [" + (jobdata.uuid ?: jobdata.id) + "] " + jobdata.project + " \"" + (jobdata.groupPath ? jobdata.groupPath : '') + "/" + jobdata.jobName + "\" (" + data.method + ")"
        jobChangeLogger.info(msg)
        data.keySet().each {k ->
            if (data[k] instanceof Date) {
                //reformat date
                MDC.remove(k + 'Time')
            }
            MDC.remove(k)
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            MDC.remove(k)
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def void issueJobChangeEvents(Collection<JobChangeEvent> events) {
        notify('multiJobChanged',new ArrayList(events))
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def void issueJobChangeEvent(JobChangeEvent event) {
        if (event) {
            notify('jobChanged', event)
        }
    }
    static List<Map> parseParamNotifications(params){
        List nots=[]
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL]) {
            def config= [
                    recipients: params[ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS],
            ]
            if(params[ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT]){
                config.subject= params[ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT]
            }
            if (params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH]!=null) {
                config.attachLog = params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH] in ['true',true]
                config.attachLogInFile = params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH_TYPE] in ['file']
                config.attachLogInline = params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH_TYPE] in ['inline']
            }
            nots << [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    format: params[ScheduledExecutionController.NOTIFY_SUCCESS_URL_FORMAT],
                    content: params[ScheduledExecutionController.NOTIFY_SUCCESS_URL]]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT]
            }
            if (params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH]!=null) {
                config.attachLog = params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH] in ['true',true]
                config.attachLogInFile = params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH_TYPE] in ['file']
                config.attachLogInline = params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH_TYPE] in ['inline']
            }
            nots << [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    format: params[ScheduledExecutionController.NOTIFY_FAILURE_URL_FORMAT],
                    content: params[ScheduledExecutionController.NOTIFY_FAILURE_URL]]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSTART_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_START_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_START_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_START_SUBJECT]
            }
            nots << [eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSTART_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    format: params[ScheduledExecutionController.NOTIFY_START_URL_FORMAT],
                    content: params[ScheduledExecutionController.NOTIFY_START_URL]]
        }

        if ('true' == params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_SUBJECT]
            }
            nots << [eventTrigger: ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME,
                     type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                     configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONOVERAVGDURATION_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME,
                     type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                     format: params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL_FORMAT],
                     content: params[ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL]]
        }

        if ('true' == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_SUBJECT]
            }
            if (params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH]!=null) {
                config.attachLog = params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH] in ['true',true]
                config.attachLogInFile = params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH_TYPE] in ['file']
                config.attachLogInline = params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH_TYPE] in ['inline']
            }
            nots << [eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                     type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                     configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                     type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                     format: params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL_FORMAT],
                     content: params[ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL]]
        }


        //notifyOnsuccessPlugin
        if (params.notifyPlugin) {
            [ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, ScheduledExecutionController
                    .ONFAILURE_TRIGGER_NAME, ScheduledExecutionController.ONSTART_TRIGGER_NAME,
             ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME,
             ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME].each { trig ->
//                params.notifyPlugin.each { trig, plug ->
                def plugs = params.notifyPlugin[trig]
                if (plugs) {
                    def types = [plugs['type']].flatten()
                    types.each { pluginType ->
                        def config = plugs[pluginType]?.config
                        if (plugs['enabled'][pluginType] == 'true') {
                            nots << [eventTrigger: trig, type: pluginType, configuration: config]
                        }
                    }
                }
            }
        }
        nots
    }

    static Orchestrator parseParamOrchestrator(params){
        Orchestrator orchestrator = new Orchestrator(type:params.orchestratorId)
        def plugin = params.orchestratorPlugin[params.orchestratorId];
        //def config = params.orchestratorPlugin[params.orchestratorId].config
        if(plugin){
            orchestrator.configuration = plugin.config
        }
        orchestrator
    }

    private Map _updateOrchestratorData(params, ScheduledExecution scheduledExecution) {
        //plugin type
        Orchestrator orchestrator = params.orchestrator
        if(scheduledExecution.orchestrator){
            scheduledExecution.orchestrator.discard()
        }
        scheduledExecution.orchestrator = orchestrator
        //TODO:validate inputs
        return [failed:false]
    }

    def _doUpdateExecutionFlags(params, user, String roleList, Framework framework, AuthContext authContext, changeinfo = [:]) {
        log.debug("ScheduledExecutionController: update : attempting to updateExecutionFlags: " + params.id + ". params: " + params)

        def ScheduledExecution scheduledExecution = getByIDorUUID(params.id)
        if (!scheduledExecution) {
            return [success: false]
        }
        if(changeinfo){
            def extraInfo = " flags:"
            if(params.executionEnabled){
                extraInfo+= " executionEnabled: "+params.executionEnabled
            }
            if(params.scheduleEnabled){
                extraInfo+= " scheduleEnabled: "+params.scheduleEnabled
            }
            logJobChange(changeinfo+[extraInfo: extraInfo],scheduledExecution.properties)
        }


        def oldSched = jobSchedulesService.isScheduled(scheduledExecution.uuid)
        def oldJobName = scheduledExecution.generateJobScheduledName()
        def oldJobGroup = scheduledExecution.generateJobGroupName()

        if (null != params.scheduleEnabled) {
            if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_SCHEDULE], scheduledExecution.project)) {
                return [success     : false, scheduledExecution: scheduledExecution,
                        message     : lookupMessage(
                                'api.error.item.unauthorized',
                                [AuthConstants.ACTION_TOGGLE_SCHEDULE, 'Job ID', scheduledExecution.extid]
                        ),
                        errorCode   : 'api.error.item.unauthorized',
                        unauthorized: true]
            }
            if(!isScheduled(scheduledExecution)){

                return [success: false, scheduledExecution: scheduledExecution,
                         message  : lookupMessage(
                                'api.error.job.toggleSchedule.notScheduled',
                                ['Job ID', scheduledExecution.extid]
                        ),
                        status: 409,
                        errorCode: 'api.error.job.toggleSchedule.notScheduled' ]
            }
            if(frameworkService.isClusterModeEnabled()) {
                def modify = jobSchedulerService.updateScheduleOwner(scheduledExecution.asReference())

                if (modify) {
                    scheduledExecution.serverNodeUUID = frameworkService.serverUUID
                }
            }else {
                scheduledExecution.serverNodeUUID = null
            }
            scheduledExecution.properties.scheduleEnabled = params.scheduleEnabled
        }

        if (null != params.executionEnabled) {
            if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_EXECUTION], scheduledExecution.project)) {
                return [success          : false, scheduledExecution: scheduledExecution,
                        message          : lookupMessage(
                                'api.error.item.unauthorized',
                                [AuthConstants.ACTION_TOGGLE_EXECUTION, 'Job ID', scheduledExecution.extid]
                        ),
                        errorCode   : 'api.error.item.unauthorized',
                        unauthorized: true]
            }
            if(frameworkService.isClusterModeEnabled()) {
                def modify = jobSchedulerService.updateScheduleOwner(scheduledExecution.asReference())

                if (modify) {
                    scheduledExecution.serverNodeUUID = frameworkService.serverUUID
                }
            } else {
                scheduledExecution.serverNodeUUID = null
            }
            scheduledExecution.properties.executionEnabled = params.executionEnabled
        }

        if (!scheduledExecution.validate()) {
            return [success: false]
        }

        if (scheduledExecution.save(flush: true)) {
            rescheduleJob(scheduledExecution, oldSched, oldJobName, oldJobGroup, true)
            return [success: true, scheduledExecution: scheduledExecution]
        } else {
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }
    }

    def validateWorkflowStep(WorkflowStep step, List projects = [], Boolean validateJobref = false, String currentProj = null) {
        WorkflowController._validateCommandExec(step, null, projects, validateJobref, currentProj)
        if (step.errors.hasErrors()) {
            return false
        } else if (step instanceof PluginStep) {
            def validation = WorkflowController._validatePluginStep(frameworkService, step)
            if (!validation.valid) {
                step.errors.rejectValue(
                        'type',
                        'Workflow.step.plugin.configuration.invalid',
                        [step.type, validation.report.toString()].toArray(),
                        'Invalid configuration for {0}: {1}'
                )
                return false
            }
        }

        def pluginConfig = step.getPluginConfigListForType(ServiceNameConstants.LogFilter)
        if (pluginConfig && pluginConfig instanceof List) {
            def allvalid = true
            pluginConfig.eachWithIndex { Map plugindef, int index ->
                def validation = WorkflowController._validateLogFilter(
                        frameworkService, pluginService, plugindef.config, plugindef.type
                )
                if (!validation.valid) {
                    step.errors.reject('Workflow.step.logFilter.configuration.invalid',
                                       [index, plugindef.type, validation.report.toString()].toArray(),
                                       'log filter {0} type {1} not valid: {2}'
                    )

                    allvalid = false
                }
            }
            return allvalid
        }

        true
    }



    private Notification definePluginNotification(ScheduledExecution scheduledExecution, String trigger,notif){
        //plugin type
        return Notification.fromMap(trigger, notif)
    }

    private boolean validateDefinitionPluginNotification(ScheduledExecution scheduledExecution, String trigger,notif,params,validationMap, Map projectProperties){
        //plugin type
        def failed=false
        def pluginDesc = notificationService.getNotificationPluginDescriptor(notif.type)
        if (!pluginDesc) {
            scheduledExecution.errors.rejectValue(
                    'notifications',
                    'scheduledExecution.notifications.pluginTypeNotFound.message',
                    [notif.type] as Object[],
                    'Notification Plugin type "{0}" was not found or could not be loaded'
            )
            return true
        }
        def validation = notificationService.validatePluginConfig(notif.type, projectProperties, notif.configuration)
        if (!validation.valid) {
            failed = true

            if(params instanceof Map){
                if (!params['notificationValidation']) {
                    params['notificationValidation'] = [:]
                }

                if (!validationMap['notificationValidation']) {
                    validationMap['notificationValidation'] = [:]
                }
                if (!params['notificationValidation'][trigger]) {
                    params['notificationValidation'][trigger] = [:]
                }
                if (!validationMap['notificationValidation'][trigger]) {
                    validationMap['notificationValidation'][trigger] = [:]
                }
                params['notificationValidation'][trigger][notif.type] = validation
                validationMap['notificationValidation'][trigger][notif.type] = validation.report.errors
            }
            scheduledExecution.errors.rejectValue(
                    'notifications',
                    'scheduledExecution.notifications.invalidPlugin.message',
                    [notif.type] as Object[],
                    'Invalid Configuration for plugin: {0}'
            )
        }
        return failed
    }
    static final Map NOTIFICATION_FIELD_NAMES= [
            (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS,
            (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS,
            (ScheduledExecutionController.ONSTART_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_START_RECIPIENTS,
            (ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS,
            (ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS,
    ]
    static final Map NOTIFICATION_FIELD_ATTACHED_NAMES=[
            (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH,
            (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_FAILURE_ATTACH,
            (ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME):
                    ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_ATTACH,
    ]
    private boolean validateDefinitionEmailNotification(ScheduledExecution scheduledExecution, String trigger, Notification notif){
        def failed
        def fieldNames = NOTIFICATION_FIELD_NAMES
        def fieldAttachedNames = NOTIFICATION_FIELD_ATTACHED_NAMES
        Map conf = notif.mailConfiguration()
        def arr = conf?.recipients?.split(",")
        def validator = new AnyDomainEmailValidator()
        def validcount=0
        arr?.each { email ->
            if(email && email.indexOf('${')>=0){
                //don't reject embedded prop refs
                validcount++
            }else if (email && !validator.isValid(email)) {
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalidemail.message',
                        [email] as Object[],
                        'Invalid email address: {0}'
                )
            }else if(email){
                validcount++
            }
        }
        if(!failed && validcount<1){
            failed=true
            scheduledExecution.errors.rejectValue(
                    fieldNames[trigger],
                    'scheduledExecution.notifications.email.blank.message',
                    'Cannot be blank'
            )
        }
        if(conf?.attachLog){
            if(!conf.containsKey("attachLogInFile") &&  !conf.containsKey("attachLogInline")){
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldAttachedNames[trigger],
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }

            if(conf.attachLogInFile == false && conf.attachLogInline == false){
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldAttachedNames[trigger],
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }
        }
        return failed
    }
    private Notification defineEmailNotification(ScheduledExecution scheduledExecution, String trigger, notif){

        def conf = notif.configuration
        def arr = (conf?.recipients?: notif.content)?.split(",")

        def addrs = arr.findAll { it.trim() }.join(",")
        def configuration=[:]
        if(conf){
            configuration = conf + [recipients: addrs]
        }else{
            configuration.recipients = addrs
        }
        return Notification.fromMap(trigger, [email: configuration])
    }
    private Notification defineUrlNotification(ScheduledExecution scheduledExecution, String trigger, notif){
        def arr = notif.content.split(",")
        def addrs = arr.findAll { it.trim() }.join(",")
       return new Notification(eventTrigger: trigger, type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE, format:notif.format, content: addrs)
    }

    private boolean validateDefinitionUrlNotification(ScheduledExecution scheduledExecution, String trigger, Notification notif){
        def failed
        def fieldNamesUrl = NOTIFICATION_FIELD_NAMES_URL
        Map urlsConfiguration = notif.urlConfiguration()
        String urls = urlsConfiguration.urls
        def arr = urls?.split(",")
        def validCount=0
        arr?.each { String url ->
            boolean valid = false
            try {
                new URL(url)
                valid = true
            } catch (MalformedURLException e) {
                valid = false
            }
            if (url && !valid) {
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldNamesUrl[trigger],
                        'scheduledExecution.notifications.invalidurl.message',
                        [url] as Object[],
                        'Invalid URL: {0}'
                )
            }else if(url && valid){
                validCount++
            }
        }
        if(validCount<1){
            failed = true
            scheduledExecution.errors.rejectValue(
                    fieldNamesUrl[trigger],
                    'scheduledExecution.notifications.url.blank.message',
                    'Webhook URL cannot be blank'
            )
        }
        return failed
    }

    /**
     * Convert params into PluginConfigSet
     * @param executionLifecyclePluginsParams
     * @return
     */
    static PluginConfigSet parseExecutionLifecyclePluginsParams(Map executionLifecyclePluginsParams) {
        List<String> keys = [executionLifecyclePluginsParams?.keys].flatten().findAll { it }

        List<PluginProviderConfiguration> configs = []

        keys.each { key ->
            def enabled = executionLifecyclePluginsParams.enabled?.get(key)
            def pluginType = executionLifecyclePluginsParams.type[key]?.toString()
            if (enabled != 'true') {
                return
            }
            Map config = [:]
            def confprops = executionLifecyclePluginsParams[key]?.configMap ?: [:]
            confprops.each { k, v ->
                if (v != '' && v != null && !k.startsWith('_')) {
                    config[k] = v
                }
            }

            configs << SimplePluginConfiguration.builder().provider(pluginType).configuration(config).build()
        }
        PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, configs
    }

    private Map validateExecutionLifecyclePlugin(
            String type,
            Map config,
            ScheduledExecution scheduledExecution
    ) {
        def failed = false
        def validation = pluginService.validatePluginConfig(type, ExecutionLifecyclePlugin, scheduledExecution.project, config)
        if (validation == null) {
            scheduledExecution.errors.rejectValue(
                    'pluginConfig',
                    'scheduledExecution.executionLifecyclePlugins.pluginTypeNotFound.message',
                    [type] as Object[],
                    'Execution Life Cycle plugin type "{0}" was not found or could not be loaded'
            )
            return [failed: true]
        }
        if (!validation.valid) {
            failed = true

            scheduledExecution.errors.rejectValue(
                    'pluginConfig',
                    'scheduledExecution.executionLifecyclePlugins.invalidPlugin.message',
                    [type] as Object[],
                    'Invalid Configuration for Execution Life Cycle plugin: {0}'
            )
        }
        [failed: failed, validation: validation]
    }

    static final Map NOTIFICATION_FIELD_NAMES_URL=[
            (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_SUCCESS_URL,
            (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_FAILURE_URL,
            (ScheduledExecutionController.ONSTART_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_START_URL,
            (ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL,
            (ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL,
    ]


    /**
     * Update the job definition
     * @param params web parameters
     * @param authContext auth
     * @param changeinfo change info
     * @param validateJobref whether to validate job refs
     * @return
     */
    @CompileStatic
    public Map _doupdate(Map params, UserAndRolesAuthContext authContext, Map changeinfo = [:], boolean validateJobref = false) {
        _doupdateJobOrParams(params.id, null, params, authContext, changeinfo, validateJobref)
    }
    /**
     * Update the job definition
     * @param id job ID
     * @param importedJob imported job definition
     * @param authContext auth
     * @param changeinfo change info
     * @param validateJobref whether to validate job refs
     * @return
     */
    @CompileStatic
    public Map _doupdateJob(
            id,
            ImportedJob<ScheduledExecution> importedJob,
            UserAndRolesAuthContext authContext,
            Map changeinfo = [:],
            boolean validateJobref = false
    ) {
        _doupdateJobOrParams(id, importedJob, [:], authContext, changeinfo, validateJobref)
    }
    /**
     * Update the job definition
     * @param id job ID
     * @param importedJob imported job definition
     * @param params web parameters
     * @param authContext auth
     * @param changeinfo change info
     * @param validateJobref whether to validate job refs
     * @return
     */
    @CompileStatic
    public Map _doupdateJobOrParams(
            def id,
            ImportedJob<ScheduledExecution> importedJob,
            Map params,
            UserAndRolesAuthContext authContext,
            Map changeinfo = [:],
            boolean validateJobref = false
    ) {
        def ScheduledExecution scheduledExecution = getByIDorUUID(id)
        if (!scheduledExecution) {
            return [success: false, error:"No Job found with ID: ${id}"]
        }

        def oldJob = new OldJob(
            oldjobname: scheduledExecution.generateJobScheduledName(),
            oldjobgroup: scheduledExecution.generateJobGroupName(),
            isScheduled: jobSchedulesService.isScheduled(scheduledExecution.uuid),
            localScheduled: scheduledExecution.scheduled,
            originalCron: scheduledExecution.generateCrontabExression(),
            originalSchedule: scheduledExecution.scheduleEnabled,
            originalExecution: scheduledExecution.executionEnabled,
            originalTz: scheduledExecution.timeZone,
            originalRef: jobEventRevRef(scheduledExecution)
        )
        ImportedJob<ScheduledExecution> importedJob2 = updateJobDefinition(importedJob, params, authContext, scheduledExecution)

        _dosaveupdated(params, importedJob2, oldJob, authContext, changeinfo, validateJobref)
    }

    /**
     * Create a the job based on imported job or params
     * @param id job ID
     * @param importedJob imported job definition
     * @param params web parameters
     * @param authContext auth
     * @param changeinfo change info
     * @param validateJobref whether to validate job refs
     * @return
     */
    @CompileStatic
    public Map _docreateJobOrParams(
        ImportedJob<ScheduledExecution> importedJob,
        Map params,
        UserAndRolesAuthContext authContext,
        Map changeinfo = [:],
        boolean validateJobref = false
    ) {

        ImportedJob<ScheduledExecution> importedJob2 = updateJobDefinition(
            importedJob,
            params,
            authContext,
            new ScheduledExecution()
        )

        _dosave(params, importedJob2, authContext, changeinfo, validateJobref)
    }

    /**
     * Validate import definition for an Adhoc execution
     * @param importedJob
     * @param userAndRoles
     * @param params
     * @param validation
     * @param validateJobref
     * @return
     */
    @CompileStatic
    boolean validateAdhocDefinition(
        ImportedJob<ScheduledExecution> importedJob,
            UserAndRolesAuthContext userAndRoles,
            Map params,
            Map validation,
            boolean validateJobref
    ) {
        def adhocJobDefinition=importedJob.job

        boolean failed = !adhocJobDefinition.validate()

        failed |=  validateProject2(adhocJobDefinition)

        failed |= validateDefinitionWorkflow(adhocJobDefinition, userAndRoles, validateJobref)

        //validate error handler types
        failed |= !validateWorkflow(adhocJobDefinition.workflow, adhocJobDefinition)

        failed |= validateDefinitionArgStringDatestamp(adhocJobDefinition)

        !failed
    }

    @CompileStatic
    boolean validateJobDefinition(
            ImportedJob<ScheduledExecution> importedJob,
            UserAndRolesAuthContext userAndRoles,
            Map params,
            Map validation,
            boolean validateJobref
    ) {
        ScheduledExecution scheduledExecution = importedJob.job

        IRundeckProject frameworkProject = frameworkService.getFrameworkProject(scheduledExecution.project)
        Map<String,String> projectProps = frameworkProject.getProjectProperties()
        boolean failed = !scheduledExecution.validate()
        //v1
        failed |= validateDefinitionSchedule(scheduledExecution, userAndRoles)

        failed |=  validateProject2(scheduledExecution)
        //v2

        failed |= validateDefinitionWorkflow(scheduledExecution, userAndRoles, validateJobref)
        //v3

        //validate error handler types
        failed |= !validateWorkflow(scheduledExecution.workflow, scheduledExecution)

        //v4
        failed |=validateDefinitionWFStrategy(scheduledExecution, params, validation)

        //v5
        failed |= validateDefinitionLogFilterPlugins(scheduledExecution,params, validation)

        //v6
        failed |= validateDefinitionArgStringDatestamp(scheduledExecution)
        //v7

        failed |= validateDefinitionOptions(scheduledExecution,params)
        //v8

        failed |= validateDefinitionNotifications(scheduledExecution,params, validation, projectProps)
        //v9

        failed |= validateDefinitionExecLifecyclePlugins(scheduledExecution,params, validation)
        //v10
        failed |= validateDefinitionComponents(importedJob, params, validation)
        //v11

        !failed
    }

    /**
     * Validate job components
     * @param importedJob
     * @param params
     * @return true if not valid
     */
    @CompileStatic
    boolean validateDefinitionComponents(ImportedJob<ScheduledExecution> importedJob, Map params, Map<String, Map<String, String>> validation) {
        def reports = rundeckJobDefinitionManager.validateImportedJob(importedJob)
        params?.put('jobComponentValidation', reports.validations)
        validation?.putAll(reports.validations.collectEntries { [it.key, it.value.errors] } as Map<? extends String, ? extends Map<String, String>>)
        return !reports.valid
    }

    @CompileStatic
    boolean validateDefinitionExecLifecyclePlugins(ScheduledExecution scheduledExecution, Map params, Map validationMap) {
        boolean failed = false
        PluginConfigSet pluginConfigSet=executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(scheduledExecution)
        if (pluginConfigSet && pluginConfigSet.pluginProviderConfigs?.size()>0) {
            //validate execution life cycle plugins
            Map<String,Validator.Report> pluginValidations = [:]

            pluginConfigSet.pluginProviderConfigs.each { PluginProviderConfiguration providerConfig ->
                def pluginType = providerConfig.provider
                Map config = providerConfig.configuration
                Map validation = validateExecutionLifecyclePlugin(pluginType, config, scheduledExecution)
                if (validation.failed) {
                    failed = true
                    if (validation.validation) {
                        ValidatedPlugin validatedPlugin=(ValidatedPlugin) validation.validation
                        pluginValidations[pluginType] = validatedPlugin.report
                    }
                }
            }
            if (failed) {
                params['executionLifecyclePluginValidation'] = pluginValidations
                validationMap['executionLifecyclePluginValidation'] = pluginValidations.collectEntries {[it.key,it.value.errors]}
            }
        }
        return failed
    }

    @CompileStatic
    public boolean validateDefinitionNotifications(ScheduledExecution scheduledExecution, Map params, Map validationMap, Map projectProperties) {
        boolean failed=false

        def fieldNames = NOTIFICATION_FIELD_NAMES
        def fieldNamesUrl =NOTIFICATION_FIELD_NAMES_URL
        scheduledExecution.notifications?.each {Notification notif ->
            def trigger = notif.eventTrigger

            def String failureField
            if (notif && notif.type == ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE ) {
                failed|=validateDefinitionEmailNotification(scheduledExecution,trigger,notif)
                failureField= fieldNames[trigger]
            } else if (notif && notif.type == ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE ) {

                failed |= validateDefinitionUrlNotification(scheduledExecution, trigger, notif)
                failureField = fieldNamesUrl[trigger]
            } else if (notif.type) {
                def data=notif
                if(notif instanceof Notification){
                    data=[type:notif.type, configuration:notif.configuration]
                }
                failed |= validateDefinitionPluginNotification(scheduledExecution, trigger, data, params, validationMap, projectProperties)
                failureField="notifications"
            }
            if (!notif.validate()||failed) {
                failed = true
                notif.discard()
                def errmsg = trigger + " notification: " + notif.errors.allErrors.collect { lookupMessageError(it) }.join(";")
                scheduledExecution.errors.rejectValue(
                        failureField,
                        'scheduledExecution.notifications.invalid.message',
                        [errmsg] as Object[],
                        'Invalid notification definition: {0}'
                )
                scheduledExecution.discard()
            }

        }
        return failed
    }

    @CompileStatic
    public boolean validateDefinitionOptions(ScheduledExecution scheduledExecution, Map params) {
        boolean failed = false

        scheduledExecution.options?.each { Option origopt ->
            EditOptsController._validateOption(origopt, null, scheduledExecution.scheduled)
            fileUploadService.validateFileOptConfig(origopt)

            if (origopt.errors.hasErrors() || !origopt.validate(deepValidate: false)) {
                failed = true
                origopt.discard()
                def errmsg = origopt.name + ": " + origopt.errors.allErrors.collect { lookupMessageError(it) }.join(
                        ";"
                )
                scheduledExecution.errors.rejectValue(
                        'options',
                        'scheduledExecution.options.invalid.message',
                        [errmsg] as Object[],
                        'Invalid Option definition: {0}'
                )
            }
        }
        return failed
    }

    @CompileStatic
    public boolean validateDefinitionLogFilterPlugins(ScheduledExecution scheduledExecution, Map params, Map validationMap) {
        boolean failed=false
        if(scheduledExecution.workflow) {
            def configs = (List) scheduledExecution.workflow.getPluginConfigDataList(ServiceNameConstants.LogFilter)
            if (configs && configs instanceof List) {
                List<Map> lfReports = validateLogFilterPlugins(scheduledExecution, configs)
                if (null != lfReports && lfReports.any { !it.valid }) {
                    rejectLogFilterPluginsInput(scheduledExecution, params,validationMap, lfReports)
                    failed = true
                }
            }
        }
        return failed
    }

    @CompileStatic
    public boolean validateDefinitionWFStrategy(ScheduledExecution scheduledExecution, Map params, Map validationMap) {
        //workflow strategy plugin config and validation
        boolean failed=false
        if(scheduledExecution.workflow) {
            def frameworkProject = frameworkService.getFrameworkProject(scheduledExecution.project)
            def projectProps = frameworkProject.getProperties()

            def validation = validateWorkflowStrategyPlugin(
                    scheduledExecution,
                    projectProps,
                    (Map) scheduledExecution.
                            workflow.
                            getPluginConfigData(
                                    ServiceNameConstants.WorkflowStrategy,
                                    scheduledExecution.workflow.strategy
                            )
            )
            if (null != validation && !validation.valid) {
                rejectWorkflowStrategyInput(scheduledExecution, params,validationMap, validation)
                failed = true
            }
        }
        return failed
    }
    /**
     *
     * @param scheduledExecution
     * @param userAndRoles
     * @param validateJobref
     * @return true if validation failed
     */
    @CompileStatic
    public boolean validateDefinitionWorkflow(ScheduledExecution scheduledExecution, UserAndRolesAuthContext userAndRoles, boolean validateJobref) {
        boolean failed=false
        if (scheduledExecution.workflow) {
            def Workflow workflow = scheduledExecution.workflow
            def i = 1;
            def wfitemfailed = false
            def failedlist = []
            List<String> fprojects = frameworkService.projectNames(userAndRoles)
            workflow.commands?.each {WorkflowStep cexec ->
                if (!validateWorkflowStep(cexec, fprojects, validateJobref, scheduledExecution.project)) {
                    wfitemfailed = true
                    failedlist << "$i: " + cexec.errors.allErrors.collect {
                        messageSource.getMessage(it,Locale.default)
                    }
                }

                if (cexec.errorHandler) {
                    if (!validateWorkflowStep(cexec.errorHandler, fprojects, validateJobref, scheduledExecution.project)) {
                        wfitemfailed = true
                        failedlist << "$i: " + cexec.errorHandler.errors.allErrors.collect {
                            messageSource.getMessage(it,Locale.default)
                        }
                    }
                }

                i++
            }

            if (wfitemfailed) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
            }
        }
        if (!scheduledExecution.workflow || !scheduledExecution.workflow.commands ||
            scheduledExecution.workflow.commands.isEmpty()) {

            scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            failed= true
        }
        failed
    }

    @CompileStatic
    public boolean validateProject2(ScheduledExecution scheduledExecution) {
        boolean failed = false
        if (scheduledExecution.project && !frameworkService.existsFrameworkProject(scheduledExecution.project)) {
            failed = true
            scheduledExecution.
                    errors.
                    rejectValue(
                            'project',
                            'scheduledExecution.project.invalid.message',
                            [scheduledExecution.project].toArray(),
                            'Project does not exist: {0}'
                    )
        }
        failed
    }

    @CompileStatic
    public boolean validateDefinitionSchedule(ScheduledExecution scheduledExecution, UserAndRoles userAndRoles) {
        def failed = false
        if (scheduledExecution.scheduled) {
            def hasCrontab = scheduledExecution.crontabString && scheduledExecution.crontabString!=scheduledExecution.generateCrontabExression()
            def genCron = hasCrontab?scheduledExecution.crontabString:scheduledExecution.generateCrontabExression()
            if (!CronExpression.isValidExpression(genCron)) {
                failed = true;
                scheduledExecution.errors.rejectValue(
                        'crontabString',
                        'scheduledExecution.crontabString.invalid.message', [genCron] as Object[], "invalid: {0}"
                )
            } else {
                //test for valid schedule
                CronExpression c = new CronExpression(genCron)
                def next = c.getNextValidTimeAfter(new Date());
                if (!next) {
                    failed = true;
                    scheduledExecution.errors.rejectValue(
                            'crontabString',
                            'scheduledExecution.crontabString.noschedule.message', [genCron] as Object[], "invalid: {0}"
                    )
                }
            }
            if (scheduledExecution.timeZone) {
                TimeZone test=TimeZone.getTimeZone(scheduledExecution.timeZone)
                boolean found= Arrays.asList(TimeZone.getAvailableIDs()).contains(scheduledExecution.timeZone);
                if(!found && test.getID()=='GMT' && scheduledExecution.timeZone!='GMT'){
                    failed = true
                    scheduledExecution.errors.rejectValue(
                            'timeZone',
                            'scheduledExecution.timezone.error.message', [scheduledExecution.timeZone] as Object[],
                            "Invalid: {0}"
                    )
                }
            }
        }
        failed
    }

    @CompileStatic
    ImportedJob<ScheduledExecution> updateJobDefinition(ImportedJob<ScheduledExecution> inputJob, Map params, UserAndRoles userAndRoles, ScheduledExecution scheduledExecution) {
        log.debug("ScheduledExecutionController: save : params: " + params)
        jobDefinitionBasic(scheduledExecution, inputJob?.job, params, userAndRoles)
        //1
        jobDefinitionSchedule(scheduledExecution, inputJob?.job, params, userAndRoles)
        //2
        jobDefinitionCluster(scheduledExecution, inputJob?.job, params, userAndRoles)
        //3
        jobDefinitionWorkflow(scheduledExecution, inputJob?.job, params, userAndRoles)
        //4
        jobDefinitionWFStrategy(scheduledExecution, inputJob?.job, params, userAndRoles)
        //5
        jobDefinitionGlobalLogFilters(scheduledExecution, inputJob?.job, params, userAndRoles)
        //6
        jobDefinitionOptions(scheduledExecution, inputJob?.job, params, userAndRoles)
        //7
        jobDefinitionOrchestrator(scheduledExecution, inputJob?.job, params, userAndRoles)
        //8
        jobDefinitionNotifications(scheduledExecution, inputJob?.job, params, userAndRoles)
        //9
        jobDefinitionExecLifecyclePlugins(scheduledExecution, inputJob?.job, params, userAndRoles)
        //10
        return rundeckJobDefinitionManager.updateJob(scheduledExecution, inputJob, params)
    }


    @CompileStatic
    public void jobDefinitionExecLifecyclePlugins(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        PluginConfigSet configSet=null
        if(input){
            configSet=executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(input)
        }else if (params.executionLifecyclePlugins && params.executionLifecyclePlugins instanceof Map) {
            Map plugins=(Map)params.executionLifecyclePlugins
            //define execution life cycle plugins config
            configSet = parseExecutionLifecyclePluginsParams(plugins)
        }
        executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(scheduledExecution, configSet)
    }


    @CompileStatic
    public void jobDefinitionNotifications(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        Collection<Notification> notificationSet=[]
        boolean replaceAll=false
        if(input){
            if(input.notifications) {
                notificationSet.addAll(input.notifications.collect{Notification.fromMap(it.eventTrigger,it.toMap())})
            }
        }else if(params.jobNotificationsJson){
            def notificationsData = JSON.parse(params.jobNotificationsJson.toString())
            if(notificationsData instanceof JSONArray){
                replaceAll=true
                for(Object item: notificationsData){
                    if(item instanceof JSONObject){
                        notificationSet.add(Notification.fromNormalizedMap(item))
                    }
                }
            }
        }else if(params.notified != 'false'){
            def notifications = parseParamNotifications(params)
            notificationSet=notifications.collect {Map notif ->
                String trigger = notif.eventTrigger
                def Notification n
                if ( notif.type == ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE ) {
                    defineEmailNotification(scheduledExecution,trigger,notif)
                } else if ( notif.type == ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE ) {
                    defineUrlNotification(scheduledExecution, trigger, notif)
                } else if (notif.type) {
                    definePluginNotification(scheduledExecution, trigger, notif)
                }else{
                    null
                }
            }.findAll{it}
        }
        def addedNotifications=[]
        notificationSet.each{Notification n->
            //modify existing notification
            Notification oldn = replaceAll?null:scheduledExecution.findNotification(n.eventTrigger,n.type)
            if(oldn){
                oldn.content=n.content
                oldn.format=n.format
                def x=n
                n=oldn
                n.scheduledExecution = scheduledExecution
                x.discard()
            }else{
                n.scheduledExecution = scheduledExecution
            }

            if(!oldn){
                scheduledExecution.addToNotifications(n)
            }
            addedNotifications << n
        }
        //delete notifications that are not part of the modified set
        if (scheduledExecution.notifications) {
            List<Notification> todelete = []
            scheduledExecution.notifications.each { Notification note ->
                if(!(note in addedNotifications)){
                    todelete << note
                }
            }
            todelete.each {
                it.delete()
                scheduledExecution.removeFromNotifications(it)
//                todiscard << it
            }

        }
    }


    @CompileStatic
    public void jobDefinitionOrchestrator(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        Orchestrator orchestrator
        if(input){
            orchestrator = input.orchestrator
        }else if (params.orchestratorId) {
            orchestrator = parseParamOrchestrator(params)
        }
        if(scheduledExecution.id && scheduledExecution.orchestrator){
            if(!hasExecutionsLinkedToOrchestrator(scheduledExecution.orchestrator)) scheduledExecution.orchestrator.delete() //cannot deleted this orchestrator if linked to executions
            scheduledExecution.orchestrator=null
        }
        if (orchestrator) {
            scheduledExecution.orchestrator = orchestrator
            scheduledExecution.orchestrator.save()
        }
    }

    boolean hasExecutionsLinkedToOrchestrator(Orchestrator orchestrator) {
        Execution.countByOrchestrator(orchestrator) > 0
    }

    public void jobDefinitionOptions(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        if(input){
            deleteExistingOptions(scheduledExecution)
            input.options?.each {Option theopt ->
                theopt.convertValuesList()
                Option newopt = theopt.createClone()
                scheduledExecution.addToOptions(newopt)
                theopt.scheduledExecution = scheduledExecution
            }
        }else if (params['_sessionopts'] && null != params['_sessionEditOPTSObject']) {
            deleteExistingOptions(scheduledExecution)
            def optsmap = params['_sessionEditOPTSObject']
            optsmap.values().each { Option opt ->
                opt.convertValuesList()
                Option newopt = opt.createClone()
                scheduledExecution.addToOptions(newopt)
                newopt.scheduledExecution = scheduledExecution
            }
        } else if (params.options) {
            deleteExistingOptions(scheduledExecution)
            //set user options:
            def i = 0;
            if (params.options instanceof Collection) {
                params.options.each { origopt ->
                    if (origopt instanceof Map) {
                        origopt = Option.fromMap(origopt.name, origopt)
                    }
                    Option theopt = origopt.createClone()
                    scheduledExecution.addToOptions(theopt)
                    theopt.scheduledExecution = scheduledExecution

                    i++
                }
            } else if (params.options instanceof Map) {
                while (params.options["options[${i}]"]) {
                    def Map optdefparams = params.options["options[${i}]"]
                    def Option theopt = new Option(optdefparams)
                    scheduledExecution.addToOptions(theopt)
                    theopt.scheduledExecution = scheduledExecution
                    i++
                }
            }
        }
    }

    public void deleteExistingOptions(ScheduledExecution scheduledExecution) {
        if (scheduledExecution.options) {
            def todelete = []
            scheduledExecution.options.each {
                todelete << it
            }
            todelete.each {
                scheduledExecution.removeFromOptions(it)
                it.delete()
            }
            scheduledExecution.options = null
        }
    }

    @CompileStatic
    public boolean validateDefinitionArgStringDatestamp(ScheduledExecution scheduledExecution) {
        if (scheduledExecution.argString) {
            try {
                scheduledExecution.argString.replaceAll(
                        /\$\{DATE:(.*)\}/,
                        { all, String tstamp ->
                            new SimpleDateFormat(tstamp).format(new Date())
                        }
                )
            } catch (IllegalArgumentException e) {
                scheduledExecution.errors.rejectValue(
                                'argString',
                                'scheduledExecution.argString.datestamp.invalid',
                                [e.getMessage()].toArray(),
                                'datestamp format is invalid: {0}'
                        )
//                log.error(e)
                return true
            }
        }
        return false
    }

    /**
     * It handles log filters for scheduled execution persistence, it will consider @input first and then @params
     * is either one or the other, not both at a time
     *
     * @param scheduledExecution current scheduled execution to be persisted
     * @param input used when importing a job
     * @param params it can contain the log filter map to be persisted
     * @param userAndRoles is not being used at the moment
     */
    void jobDefinitionGlobalLogFilters(ScheduledExecution scheduledExecution, ScheduledExecution input, Map params, UserAndRoles userAndRoles) {
        if(input){
            scheduledExecution.workflow.setPluginConfigData(
                    ServiceNameConstants.LogFilter,
                    input.workflow.getPluginConfigDataList(ServiceNameConstants.LogFilter)
            )
        }else if (params.workflow instanceof Map && params.workflow.globalLogFilters) {
            //filter configs
            def i = 0;
            def configs = []
            while (params.workflow.globalLogFilters["$i"]?.type) {
                configs << [
                        type  : params.workflow.globalLogFilters["$i"]?.type,
                        config: params.workflow.globalLogFilters["$i"]?.config
                ]
                i++
            }
            //validate
            if (configs) {
                scheduledExecution.workflow.setPluginConfigData(ServiceNameConstants.LogFilter, configs)
            } else {
                scheduledExecution.workflow.setPluginConfigData(ServiceNameConstants.LogFilter, null)
            }
        }else{
            scheduledExecution.workflow.setPluginConfigData(ServiceNameConstants.LogFilter, null)
        }
    }

    public void jobDefinitionWFStrategy(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        //workflow strategy plugin config and validation
        if(input){
            scheduledExecution.workflow.setPluginConfigData(
                    ServiceNameConstants.WorkflowStrategy,
                    input.workflow.strategy,
                    input.workflow.getPluginConfigData(
                        ServiceNameConstants.WorkflowStrategy,
                        input.workflow.strategy
                    )
            )
        }else if (params.workflow instanceof Map) {
            Map configmap = params.workflow?.strategyPlugin?.get(scheduledExecution.workflow.strategy)?.config

            scheduledExecution.workflow.setPluginConfigData(
                    ServiceNameConstants.WorkflowStrategy,
                    scheduledExecution.workflow.strategy,
                    configmap
            )

        } else if (params.workflow instanceof Workflow) {
            scheduledExecution.workflow.pluginConfigMap = params.workflow.pluginConfigMap

        }

        if(!scheduledExecution.workflow.validatePluginConfigMap()){
            throw new RuntimeException("Invalid workflow plugin config: " + scheduledExecution.workflow.pluginConfig )
        }
    }

    public void jobDefinitionWorkflow(ScheduledExecution scheduledExecution, ScheduledExecution input,Map params, UserAndRoles userAndRoles) {
        if(input){
            final Workflow workflow = new Workflow(input.workflow)
            scheduledExecution.workflow = workflow
        } else if (params['_sessionwf'] == 'true' && params['_sessionEditWFObject']) {
            //use session-stored workflow
            def Workflow wf = params['_sessionEditWFObject']
            if(params.workflow && null!=params.workflow.keepgoing) {
                wf.keepgoing = params.workflow.keepgoing == 'true'
            }
            if(params.workflow && params.workflow.strategy){
                wf.strategy = params.workflow.strategy
            }else if(!wf.strategy){
                wf.strategy='sequential'
            }
            if (wf.commands) {
                final Workflow workflow = new Workflow(wf)
                scheduledExecution.workflow = workflow
                wf.discard()
            }
        } else if (params.workflow && params.workflow instanceof Workflow) {
            scheduledExecution.workflow = new Workflow(params.workflow)
        }else if (params.workflow && params.workflow instanceof Map){
            if (!scheduledExecution.workflow) {
                scheduledExecution.workflow = new Workflow(params.workflow)
            }
            if (params.workflow.strategy) {
                scheduledExecution.workflow.strategy = params.workflow.strategy
            } else if (!scheduledExecution.workflow.strategy) {
                scheduledExecution.workflow.strategy = 'sequential'
            }
            if (null != params.workflow.keepgoing) {
                scheduledExecution.workflow.keepgoing = params.workflow.keepgoing == 'true'
            }
        }
        if(!scheduledExecution.workflow){
            scheduledExecution.workflow = new Workflow()
        }
    }


    @CompileStatic
    public void jobDefinitionCluster(ScheduledExecution scheduledExecution, ScheduledExecution input, Map params, UserAndRoles userAndRoles) {
        if (frameworkService.isClusterModeEnabled()) {
            if(!scheduledExecution.id || !scheduledExecution.serverNodeUUID) {
                scheduledExecution.serverNodeUUID = frameworkService.getServerUUID()
            }
        } else {
            scheduledExecution.serverNodeUUID = null
        }
    }

    @CompileStatic
    public void jobDefinitionSchedule(ScheduledExecution scheduledExecution, ScheduledExecution input, Map params, UserAndRoles userAndRoles) {
        if(input){

        }else if (scheduledExecution.scheduled) {
            scheduledExecution.populateTimeDateFields(params)
        }
    }

    /**
     * update the job with basic definition from original job or from parameter map
     * @param scheduledExecution new job
     * @param input original job
     * @param params web input parameters
     * @param userAndRoles
     * @return
     */
    public void jobDefinitionBasic(ScheduledExecution scheduledExecution, ScheduledExecution input, Map params, UserAndRoles userAndRoles) {
        Map basicProps
        if(!input) {
            basicProps = params.findAll {
                !it.key.startsWith("option.") &&
                it.key != 'workflow' &&
                it.key != 'options' &&
                it.key != 'notifications' &&
                !it.key.startsWith( 'nodeInclude') &&//deprecating these
                !it.key.startsWith( 'nodeExclude')
            }
        }else{
            final Collection foundprops = input.properties.keySet().findAll {
                it != 'lastUpdated' &&
                it != 'dateCreated' &&
                !it.startsWith( 'nodeInclude') &&//deprecating these
                !it.startsWith( 'nodeExclude') &&
                (
                        input.properties[it] instanceof String ||
                        input.properties[it] instanceof Boolean ||
                        input.properties[it] instanceof Integer
                ) || !input.properties[it]
            }
            basicProps = foundprops ? input.properties.subMap(foundprops) : [:]

        }

        if (scheduledExecution.uuid) {
            basicProps.uuid = scheduledExecution.uuid//don't modify uuid if it exists
        }else if(!basicProps.uuid){
            //set UUID if not submitted
            basicProps.uuid = UUID.randomUUID().toString()
        }
        if (scheduledExecution.serverNodeUUID) {
            //don't modify serverNodeUUID, it will be set if needed after validation
            basicProps.serverNodeUUID = scheduledExecution.serverNodeUUID
        }

        //clean up values that should be cleared
        [
                'retry',
                'timeout',
                'nodeInclude',
                'nodeExclude',
                'nodeIncludeName',
                'nodeExcludeName',
                'nodeIncludeTags',
                'nodeExcludeTags',
                'nodeIncludeOsName',
                'nodeExcludeOsName',
                'nodeIncludeOsFamily',
                'nodeExcludeOsFamily',
                'nodeIncludeOsArch',
                'nodeExcludeOsArch',
                'nodeIncludeOsVersion',
                'nodeExcludeOsVersion',
        ].each {
            scheduledExecution."$it" = null
        }

        scheduledExecution.properties = basicProps
        if (scheduledExecution.groupPath) {
            def re = /^\/*(.+?)\/*$/
            def matcher = scheduledExecution.groupPath =~ re
            if (matcher.matches()) {
                scheduledExecution.groupPath = matcher.group(1);
            }
        }
        if (scheduledExecution.doNodedispatch && !scheduledExecution.filter) {
            scheduledExecution.filter = scheduledExecution.asFilter()
        }
        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }
        if (!scheduledExecution.jobName) {
            scheduledExecution.jobName = ''
        }
        if (scheduledExecution.doNodedispatch) {
            if (!scheduledExecution.nodeThreadcount) {
                scheduledExecution.nodeThreadcount = 1
            }
        }else{
            scheduledExecution.filter = null
        }
    }

    /**
     * Save an updated job, will verify authorization
     * @param params
     * @param authContext
     * @param changeinfo
     * @return
     */
    Map _dosaveupdated(
            Map params,
            ImportedJob<ScheduledExecution> importedJob,
            OldJob oldjob,
            UserAndRolesAuthContext authContext,
            Map changeinfo = [:],
            boolean validateJobref = false
    ) {

        def scheduledExecution = importedJob.job
        scheduledExecution.user = authContext.username
        scheduledExecution.userRoles = authContext.roles as List<String>
        Map validation=[:]
        boolean failed  = !validateJobDefinition(importedJob, authContext, params, validation, validateJobref)

        if(failed){
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution, error: "Validation failed", validation: validation]
        }

        def boolean renamed = oldjob.wasRenamed(scheduledExecution.jobName,scheduledExecution.groupPath)

        if(renamed){
            //reauthorize if the name/group has changed
            if (!rundeckAuthContextProcessor.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_CREATE], scheduledExecution.project)) {
                failed = true
                scheduledExecution.errors.rejectValue('jobName', 'ScheduledExecution.jobName.unauthorized', [AuthConstants.ACTION_CREATE, scheduledExecution.jobName].toArray(), 'Unauthorized action: {0} for value: {1}')
                scheduledExecution.errors.rejectValue('groupPath', 'ScheduledExecution.groupPath.unauthorized', [ AuthConstants.ACTION_CREATE, scheduledExecution.groupPath].toArray(), 'Unauthorized action: {0} for value: {1}')
            }
        }

        def actions = [AuthConstants.ACTION_UPDATE]
        if(changeinfo?.method == 'scm-import'){
            actions += [AuthConstants.ACTION_SCM_UPDATE]
        }
        if (!rundeckAuthContextProcessor.authorizeProjectJobAny(authContext, scheduledExecution, actions, scheduledExecution.project)) {
            scheduledExecution.discard()
            return [success: false, error: "Unauthorized: Update Job ${scheduledExecution.generateFullName()}",
                    unauthorized: true, scheduledExecution: scheduledExecution]
        }


        def modify = true
        boolean schedulingWasChanged = oldjob.schedulingWasChanged(scheduledExecution)
        if(frameworkService.isClusterModeEnabled()){
            if (schedulingWasChanged) {
                JobReferenceImpl jobReference = scheduledExecution.asReference()
                jobReference.setOriginalQuartzJobName(oldjob.oldjobname)
                jobReference.setOriginalQuartzGroupName(oldjob.oldjobgroup)
                modify = jobSchedulerService.updateScheduleOwner(jobReference)
                if (modify) {
                    scheduledExecution.serverNodeUUID = frameworkService.serverUUID
                }
            }
            if (!scheduledExecution.serverNodeUUID) {
                scheduledExecution.serverNodeUUID = frameworkService.serverUUID
            }
        }

        if (renamed) {
            changeinfo.rename = true
            changeinfo.origName = oldjob.oldjobname
            changeinfo.origGroup = oldjob.oldjobgroup
        }



        if (!failed && null != scheduledExecution.workflow) {
            if (!scheduledExecution.workflow.save(flush: true)) {
                log.error(scheduledExecution.workflow.errors.allErrors.collect {lookupMessageError(it)}.join("\n"))
                failed = true
            }
        }

        //set UUID if not submitted

        def resultFromPlugin = runBeforeSave(scheduledExecution, authContext)
        if (!resultFromPlugin.success && resultFromPlugin.error) {
            scheduledExecution.errors.reject(
                    'scheduledExecution.plugin.error.message',
                    ['Job Lifecycle: ' + resultFromPlugin.error].toArray(),
                    "A Plugin returned an error: " + resultFromPlugin.error
            )
        }
        def result2 = saveComponents(importedJob, authContext)
        if (!result2.success && result2.error) {
            failed = true
            scheduledExecution.errors.reject(
                    'scheduledExecution.plugin.error.message',
                    ['Component: ' + result2.error].toArray(),
                    "A component returned an error: " + result2.error
            )
        }

        if (!(resultFromPlugin.success && !failed && scheduledExecution.save(flush: true))) {
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }

        rundeckJobDefinitionManager.waspersisted(importedJob, authContext)

        def stats = ScheduledExecutionStats.findAllBySe(scheduledExecution)
        if (!stats) {
            stats = new ScheduledExecutionStats(se: scheduledExecution)
                    .save(flush: true)
        }

        def scheduleResult = rescheduleJob(
            scheduledExecution,
            oldjob.isScheduled,
            renamed ? oldjob.oldjobname : scheduledExecution.generateJobScheduledName(),
            renamed ? oldjob.oldjobgroup : scheduledExecution.generateJobGroupName(),
            false, !schedulingWasChanged || !modify
        )

        boolean remoteSchedulingChanged = scheduleResult == null || scheduleResult == [null, null]

        def eventType=JobChangeEvent.JobChangeEventType.MODIFY
        if (renamed) {
            eventType = JobChangeEvent.JobChangeEventType.MODIFY_RENAME
        }
        def event = createJobChangeEvent(eventType, scheduledExecution, oldjob.originalRef)
        return [success: true, scheduledExecution: scheduledExecution, jobChangeEvent: event, remoteSchedulingChanged: remoteSchedulingChanged]


    }
    /**
     * Save a new job, will verify authorization
     * @param params web params map
     * @param importedJob imported job definition
     * @param authContext
     * @param changeinfo
     * @return
     */
//    @CompileStatic
    Map _dosave(
            Map params,
            ImportedJob<ScheduledExecution> importedJob,
            UserAndRolesAuthContext authContext,
            Map changeinfo = [:],
            boolean validateJobref = false
    ) {

        def scheduledExecution = importedJob.job
        scheduledExecution.user = authContext.username
        scheduledExecution.userRoles = authContext.roles as List<String>

        Map validation = [:]
        boolean failed = !validateJobDefinition(importedJob, authContext, params, validation, validateJobref)
        //try to save workflow
        if(failed){
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution, error: "Validation failed", validation: validation]
        }
        def actions = [AuthConstants.ACTION_CREATE]
        if(changeinfo?.method == 'scm-import'){
            actions += [AuthConstants.ACTION_SCM_CREATE]
        }
        if (!rundeckAuthContextProcessor.authorizeProjectJobAny(authContext, scheduledExecution, actions, scheduledExecution
                .project)) {
            scheduledExecution.discard()
            return [success: false, error: "Unauthorized: Create Job ${scheduledExecution.generateFullName()}",
                    unauthorized: true, scheduledExecution: scheduledExecution]
        }
        if (!failed && null != scheduledExecution.workflow) {
            if (!scheduledExecution.workflow.save(flush: true)) {
                log.error(scheduledExecution.workflow.errors.allErrors.collect {lookupMessageError(it)}.join("\n"))
                failed = true
            }
        }

        //set UUID if not submitted
        if (!scheduledExecution.uuid) {
            scheduledExecution.uuid = UUID.randomUUID().toString()
        }
        def resultFromPlugin = runBeforeSave(scheduledExecution, authContext)
        if (!resultFromPlugin.success && resultFromPlugin.error) {
            scheduledExecution.errors.reject(
                    'scheduledExecution.plugin.error.message',
                    ['Job Lifecycle: ' + resultFromPlugin.error].toArray(),
                    "A Plugin returned an error: " + resultFromPlugin.error
            )
        }

        def result2 = saveComponents(importedJob, authContext)
        if (!result2.success && result2.error) {
            failed = true
            scheduledExecution.errors.reject(
                    'scheduledExecution.plugin.error.message',
                    ['Component: ' + result2.error].toArray(),
                    "A component returned an error: " + result2.error
            )
        }
        if (!(resultFromPlugin.success && !failed && scheduledExecution.save(flush: true))) {
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }

        rundeckJobDefinitionManager.waspersisted(importedJob, authContext)

        def stats = ScheduledExecutionStats.findAllBySe(scheduledExecution)
        if (!stats) {
            stats = new ScheduledExecutionStats(se: scheduledExecution)
                    .save(flush: true)
        }
        rescheduleJob(scheduledExecution)
        def event = createJobChangeEvent(JobChangeEvent.JobChangeEventType.CREATE, scheduledExecution)
        return [success: true, scheduledExecution: scheduledExecution, jobChangeEvent: event]
    }

    private static StoredJobChangeEvent createJobChangeEvent(
            JobChangeEvent.JobChangeEventType type,
            ScheduledExecution scheduledExecution,
            JobReference orig = null
    )
    {
        createJobChangeEvent(type, jobEventRevRef(scheduledExecution), scheduledExecution, orig)
    }
    private static StoredJobChangeEvent createJobChangeEvent(
            JobChangeEvent.JobChangeEventType type,
            JobRevReference rev,
            ScheduledExecution job,
            JobReference orig = null
    )
    {
        new StoredJobChangeEvent(
                eventType: type,
                originalJobReference: orig?:rev,
                jobReference: rev,
                job:job
        )
    }

    private static JobRevReferenceImpl jobEventRevRef(ScheduledExecution scheduledExecution) {
        new JobRevReferenceImpl(
                id: scheduledExecution.extid,
                jobName: scheduledExecution.jobName,
                groupPath: scheduledExecution.groupPath,
                project: scheduledExecution.project,
                version: scheduledExecution.version
        )
    }

    /**
     * Parse some kind of job input request using the specified format
     * @param input either an inputStream, a File, or a String
     */
    def parseUploadedFile (input, fileformat){
        def jobset
        try {
            jobset = rundeckJobDefinitionManager.decodeFormat(fileformat, input)
        } catch (UnsupportedFormatException e) {
            log.debug("Unsupported format requested for Job definition: $fileformat", e)
            return [errorCode: 'api.error.jobs.import.format.unsupported', args: [fileformat]]
        } catch (JobDefinitionException e) {
            log.error("Error parsing upload Job $fileformat: ${e}")
            log.warn("Error parsing upload Job $fileformat", e)
            return [error: "${e}"]
        } catch (Exception e) {
            log.error("Error parsing upload Job $fileformat", e)
            return [error: "${e}"]
        }
        if (null == jobset) {
            return [errorCode: 'api.error.jobs.import.empty']
        }
        return [jobset: jobset]
    }
    /**
     * Validate workflow command error handler types, return true if valid
     * @param workflow
     * @param scheduledExecution
     * @return
     */

//    @CompileStatic
    boolean validateWorkflow(Workflow workflow, ScheduledExecution scheduledExecution){
        def valid=true
        //validate error handler types
        if (workflow?.strategy == 'node-first') {
            //if a step is a Node step and has an error handler
            def cmdi = 1
            workflow.commands.each { WorkflowStep step ->
                if(step.errorHandler && step.nodeStep && !step.errorHandler.nodeStep){
                    //reject if the Error Handler is not a node step
                    step.errors.rejectValue('errorHandler', 'WorkflowStep.errorHandler.nodeStep.invalid', [cmdi] as Object[], "Step {0}: Must have a Node Step as an Error Handler")
                    scheduledExecution.errors.rejectValue('workflow', 'Workflow.stepErrorHandler.nodeStep.invalid', [cmdi] as Object[], "Step {0}: Must have a Node Step as an Error Handler")
                    valid = false
                }
                cmdi++
            }
        }
        //TODO: validate workflow plugin
        return valid
    }

    /**
     * convenience to define a job via web params and validate it without saving
     * @param params
     * @param userAndRoles
     * @param validateJobref
     * @return
     */
    def _dovalidate (Map params, UserAndRolesAuthContext userAndRoles, boolean validateJobref = false ){
        ImportedJob<ScheduledExecution> importedJob = updateJobDefinition(null, params, userAndRoles, new ScheduledExecution())
        def validation=[:]
        boolean failed  = !validateJobDefinition(importedJob, userAndRoles, params, validation, validateJobref)
        [scheduledExecution:importedJob.job,failed:failed, params: params, validation: validation]
    }

    /**
     * convenience to define a job via web params and validate it as an adhoc job without saving
     * @param params
     * @param userAndRoles
     * @param validateJobref
     * @return
     */
    def _dovalidateAdhoc (Map params, UserAndRolesAuthContext userAndRoles, boolean validateJobref = false ){
        ImportedJob<ScheduledExecution> importedJob = updateJobDefinition(null, params, userAndRoles, new ScheduledExecution())
        def validation=[:]
        boolean failed  = !validateAdhocDefinition(importedJob, userAndRoles, params, validation, validateJobref)
        [scheduledExecution:importedJob.job,failed:failed, params: params, validation: validation]
    }

    /**
     * Validate a workflow strategy plugin input
     * @param scheduledExecution job
     * @param projectProps project level properties
     * @param configmap configuration of the strategy plugin
     * @param params parameters map
     * @return true if valid, false otherwise,
     */
    private Validator.Report validateWorkflowStrategyPlugin(
            ScheduledExecution scheduledExecution,
            Map<String, String> projectProps,
            Map configmap
    )
    {

        def service = frameworkService.rundeckFramework.workflowStrategyService
        def workflow = new Workflow(scheduledExecution.workflow)
        workflow.discard()
        if (!workflow.commands || workflow.commands.size() < 1) {
            return null
        }
        def name = workflow.strategy
        PropertyResolver resolver = frameworkService.getFrameworkPropertyResolverWithProps(
                projectProps,
                configmap
        )
        //validate input values wrt to property definitions
        def validation = pluginService.validatePlugin(name,
                                                      service,
                                                      resolver,
                                                      PropertyScope.Instance
        )
        def report=validation?.report
        if (!report||report.valid) {
            //validate input values of configured plugin in context of the workflow defintion
            def workflowItem = executionUtilService.createExecutionItemForWorkflow(workflow)

            def workflowStrategy = service.getStrategyForWorkflow(workflowItem, resolver)

            report = workflowStrategy.validate(workflowItem.workflow)
        }

        report
    }

    @CompileStatic
    private def rejectWorkflowStrategyInput(
            ScheduledExecution scheduledExecution,
            Map params,
            Map validationMap,
            Validator.Report report
    ) {
        def name=scheduledExecution.workflow.strategy
        if (params !=null) {
            if (!params['strategyValidation']) {
                params['strategyValidation'] = [:]
            }
            if (!params['strategyValidation'][name]) {
                params['strategyValidation'][name] = [:]
            }
            params['strategyValidation'][name] = report
        }
        if (validationMap !=null) {
            if (!validationMap['workflowStrategy']) {
                validationMap['workflowStrategy'] = [:]
            }
            if (!validationMap['workflowStrategy'][name]) {
                validationMap['workflowStrategy'][name] = [:]
            }
            validationMap['workflowStrategy'][name] = report.errors
        }
        scheduledExecution.errors.rejectValue('workflow',
                                               'Workflow.strategy.plugin.config.invalid',
                                               [name] as Object[],
                                               "Workflow strategy {0}: Some config values were not valid"
        )

        scheduledExecution.workflow.errors.rejectValue(
                'strategy',
                'scheduledExecution.workflowStrategy.invalidPlugin.message',
                [name] as Object[],
                'Invalid Configuration for plugin: {0}'
        )
    }

    @CompileStatic
    protected List<Map> validateLogFilterPlugins(ScheduledExecution scheduledExecution, List<Map> configs) {
        return configs.collect { Map filterdef ->
            if(filterdef.config instanceof Map && filterdef.type instanceof String) {
                Map config=(Map)filterdef.config
                String type=(String)filterdef.type
                return validateLogFilterPlugin(config,type)
            }else{
                return [valid:false]
            }
        }
    }

    @CompileStatic
    protected Map validateLogFilterPlugin(Map config, String type) {
        DescribedPlugin described = pluginService.getPluginDescriptor(type, LogFilterPlugin)
        return frameworkService.validateDescription(
                described.description,
                '',
                config,
                null,
                PropertyScope.Instance,
                PropertyScope.Project
        )
    }

    @CompileStatic
    private def rejectLogFilterPluginsInput(ScheduledExecution scheduledExecution, Map params, Map validationMap, List<Map> reports) {
        def invalid = []

        if (params !=null) {
            if (!params['logFilterValidation']) {
                params['logFilterValidation'] = [:]
            }
            reports.eachWithIndex { report, index ->
                if (!report.valid) {
                    if (!params['logFilterValidation']["$index"]) {
                        params['logFilterValidation']["$index"] = [:]
                    }
                    params['logFilterValidation']["$index"] = report.report
                    invalid << index
                }
            }
        }
        if (validationMap !=null) {
            if (!validationMap['logFilter']) {
                validationMap['logFilter'] = [:]
            }
            reports.eachWithIndex { report, index ->
                if (!report.valid) {
                    if (!validationMap['logFilter']["$index"]) {
                        validationMap['logFilter']["$index"] = [:]
                    }
                    validationMap['logFilter']["$index"] = ((Validator.Report)report.report).errors
                    invalid << index
                }
            }
        }
        scheduledExecution?.errors.rejectValue(
                'workflow',
                'Workflow.logFilter.plugin.config.invalid',
                [invalid.join(",")] as Object[],
                "Workflow Log Filters: {0}: Some config values were not valid"
        )

    }

    def listWorkflows(HashMap query) {
        ScheduledExecutionQuery nquery = new ScheduledExecutionQuery()
        nquery.setIdlist(query.idlist)
        nquery.setGroupPath(query.groupPath)
        nquery.setGroupPathExact(query.groupPathExact)
        nquery.setMax(query.max)
        nquery.setOffset(query.offset)
        nquery.setSortBy(query.sortBy)
        return listWorkflows(nquery)
    }



    def getTimeZones(){
        TimeZone.getAvailableIDs()
    }
    @NotTransactional
    def isProjectExecutionEnabled(String project){
        IRundeckProject fwProject = frameworkService.getFrameworkProject(project)
        isRundeckProjectExecutionEnabled(fwProject)
    }

    @NotTransactional
    boolean isRundeckProjectExecutionEnabled(IRundeckProject fwProject) {
        def disableEx = fwProject.getProjectProperties().get(CONF_PROJECT_DISABLE_EXECUTION)
        ((!disableEx) || disableEx.toLowerCase() != 'true')
    }

    @NotTransactional
    def isProjectScheduledEnabled(String project){
        IRundeckProject fwProject = frameworkService.getFrameworkProject(project)
        isRundeckProjectScheduleEnabled(fwProject)
    }

    @NotTransactional
    boolean isRundeckProjectScheduleEnabled(IRundeckProject fwProject) {
        def disableSe = fwProject.getProjectProperties().get(CONF_PROJECT_DISABLE_SCHEDULE)
        ((!disableSe) || disableSe.toLowerCase() != 'true')
    }

    @NotTransactional
    def shouldScheduleInThisProject(String project){
        return isProjectExecutionEnabled(project) && isProjectScheduledEnabled(project)
    }

    def deleteScheduledExecutionById(jobid, String callingAction){
        def session = getSession()
        def user = session.user
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)

        deleteScheduledExecutionById(jobid, authContext, false, user, callingAction)
    }


    /**
     * Load options values from remote URL
     * @param scheduledExecution
     * @param mapConfig
     * @return option remote
     */
    def Map loadOptionsRemoteValues(ScheduledExecution scheduledExecution, Map mapConfig, def username) {
        //load expand variables in URL source
        Option opt = scheduledExecution.options.find { it.name == mapConfig.option }
        def realUrl = opt.realValuesUrl.toExternalForm()
        String srcUrl = OptionsUtil.expandUrl(opt, realUrl, scheduledExecution, mapConfig.extra?.option, realUrl.matches(/(?i)^https?:.*$/), username)
        String cleanUrl = srcUrl.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
        def remoteResult = [:]
        def result = null
        def remoteStats = [startTime: System.currentTimeMillis(), httpStatusCode: "", httpStatusText: "", contentLength: "", url: srcUrl, durationTime: "", finishTime: "", lastModifiedDateTime: ""]
        def err = [:]
        int timeout = 10
        int contimeout = 0
        int retryCount = 5
        if (grailsApplication.config.rundeck?.jobs?.options?.remoteUrlTimeout) {
            try {
                timeout = Integer.parseInt(
                        grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlTimeout?.toString()
                )
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlTimeout is not a valid integer: "
                                + e.message
                )
            }
        }
        if (grailsApplication.config.rundeck?.jobs?.options?.remoteUrlConnectionTimeout) {
            try {
                contimeout = Integer.parseInt(
                        grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlConnectionTimeout?.toString()
                )
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlConnectionTimeout is not a valid integer: "
                                + e.message
                )
            }
        }
        if (grailsApplication.config.rundeck?.jobs?.options?.remoteUrlRetry) {
            try {
                retryCount = Integer.parseInt(
                        grailsApplication?.config?.rundeck?.jobs?.options?.remoteUrlRetry?.toString()
                )
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlRetry is not a valid integer: "
                                + e.message
                )
            }
        }
        if (srcUrl.indexOf('#') >= 0 && srcUrl.indexOf('#') < srcUrl.size() - 1) {
            def urlanchor = new HashMap<String, String>()
            def anchor = srcUrl.substring(srcUrl.indexOf('#') + 1)
            def parts = anchor.split(";")
            parts.each { s ->
                def subpart = s.split("=", 2)
                if (subpart && subpart.length == 2 && subpart[0] && subpart[1]) {
                    urlanchor[subpart[0]] = subpart[1]
                }
            }
            if (urlanchor['timeout']) {
                try {
                    timeout = Integer.parseInt(urlanchor['timeout'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL timeout ${urlanchor['timeout']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
            if (urlanchor['contimeout']) {
                try {
                    contimeout = Integer.parseInt(urlanchor['contimeout'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL contimeout ${urlanchor['contimeout']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
            if (urlanchor['retry']) {
                try {
                    retryCount = Integer.parseInt(urlanchor['retry'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL retry ${urlanchor['retry']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
        }
        try {
            def framework = frameworkService.getRundeckFramework()
            def projectConfig = framework.projectManager.loadProjectConfig(scheduledExecution.project)
            boolean disableRemoteOptionJsonCheck = projectConfig.hasProperty(REMOTE_OPTION_DISABLE_JSON_CHECK)

            remoteResult = ScheduledExecutionController.getRemoteJSON(srcUrl, timeout, contimeout, retryCount, disableRemoteOptionJsonCheck)
            result = remoteResult.json
            if (remoteResult.stats) {
                remoteStats.putAll(remoteResult.stats)
            }
        } catch (Exception e) {
            err.message = "Failed loading remote option values"
            err.exception = e
            err.srcUrl = cleanUrl
            log.error("getRemoteJSON error: URL ${cleanUrl} : ${e.message}");
            e.printStackTrace()
            remoteStats.finishTime = System.currentTimeMillis()
            remoteStats.durationTime = remoteStats.finishTime - remoteStats.startTime
        }
        if (remoteResult.error) {
            err.message = "Failed loading remote option values"
            err.exception = new Exception(remoteResult.error)
            err.srcUrl = cleanUrl
            log.error("getRemoteJSON error: URL ${cleanUrl} : ${remoteResult.error}");
        }
        logRemoteOptionStats(remoteStats, [jobName: scheduledExecution.generateFullName(), id: scheduledExecution.extid, jobProject: scheduledExecution.project, optionName: mapConfig.option, user: username])
        //validate result contents
        boolean valid = true;
        def validationerrors = []
        if (result) {
            if (result instanceof Collection) {
                result.eachWithIndex { entry, i ->
                    if (entry instanceof JSONObject) {
                        if (!entry.name) {
                            validationerrors << "Item: ${i} has no 'name' entry"
                            valid = false;
                        }
                        if (!entry.value) {
                            validationerrors << "Item: ${i} has no 'value' entry"
                            valid = false;
                        }
                    } else if (!(entry instanceof String)) {
                        valid = false;
                        validationerrors << "Item: ${i} expected string or map like {name:\"..\",value:\"..\"}"
                    }
                }
            } else if (result instanceof JSONObject) {
                JSONObject jobject = result
                result = []
                jobject.keys().sort().each { k ->
                    result << [name: k, value: jobject.get(k)]
                }
            } else {
                validationerrors << "Expected top-level list with format: [{name:\"..\",value:\"..\"},..], or ['value','value2',..] or simple object with {name:\"value\",...}"
                valid = false
            }
            if (!valid) {
                result = null
                err.message = "Failed parsing remote option values: ${validationerrors.join('\n')}"
                err.code = 'invalid'
            }
        } else if (!err) {
            err.message = "Empty result"
            err.code = 'empty'
        }
        return [
                optionSelect : opt,
                values       : result,
                srcUrl       : cleanUrl,
                err          : err
        ]
    }


    static Logger optionsLogger = LoggerFactory.getLogger("com.dtolabs.rundeck.remoteservice.http.options")
    private logRemoteOptionStats(stats,jobdata){
        stats.keySet().each{k->
            def v= stats[k]
            if(v instanceof Date){
                //TODO: reformat date
                MDC.put(k,v.toString())
                MDC.put("${k}Time",v.time.toString())
            }else if(v instanceof String){
                MDC.put(k,v?v:"-")
            }else{
                final string = v.toString()
                MDC.put(k, string?string:"-")
            }
        }
        jobdata.keySet().each{k->
            final var = jobdata[k]
            MDC.put(k,var?var.toString():'-')
        }
        optionsLogger.info(stats.httpStatusCode + " " + stats.httpStatusText+" "+stats.contentLength+" "+stats.url)
        stats.keySet().each {k ->
            if (stats[k] instanceof Date) {
                //reformat date
                MDC.remove(k+'Time')
            }
            MDC.remove(k)
        }
        jobdata.keySet().each {k ->
            MDC.remove(k)
        }
    }


    /**
     * Retrun a list of dates in a time lapse between now and the to Date.
     * @param to Date in the future
     * @return list of dates
     */
    List<Date> nextExecutions(ScheduledExecution se, Date to, boolean past = false){
        return jobSchedulesService.nextExecutions(se.uuid, to, past)
    }


    def saveComponents(
            ImportedJob<ScheduledExecution> importedJob,
            UserAndRolesAuthContext authContext
    ) {
        try {
            rundeckJobDefinitionManager.persistComponents(importedJob, authContext)
        } catch (Throwable err) {
            log.debug("Job Component persist error: " + err.message, exception)
            log.warn("Job Component  persist error: " + err.message)
            return [success: false, error: err.message]
        }
        return [success: true]
    }


   /**
     * Return a map with date start (timestamp) of one time scheduled executions (Job Run Later)
     * @param se
     * @return Map<Long, Date> with timestamp scheduled of a ScheduledExecutions
     */
    Map<Long, Date> nextOneTimeScheduledExecutions(List<ScheduledExecution> se) {
        if(se.isEmpty()) return [:]
        Date now = new Date()
        Map item = [:]
        Execution.createCriteria().list() {
            createAlias("scheduledExecution", "se", JoinType.LEFT_OUTER_JOIN)
            projections {
                property('se.id')
                property('dateStarted')
            }
            and {
                'in'('se.id', se*.id)
                gt("dateStarted", now)
                isNull("dateCompleted")
            }
        }?.collect{ row ->
            if(row && row[0]){
                item[row[0]] = new Date(row[1]?.getTime())
            }
        }

        return item
    }

    def runBeforeSave(ScheduledExecution scheduledExecution, UserAndRolesAuthContext authContext){
        INodeSet nodeSet = getNodes(scheduledExecution, scheduledExecution.asFilter())
        JobPersistEventImpl jobPersistEvent = new JobPersistEventImpl(
                scheduledExecution.jobName,
                scheduledExecution.project,
                authContext.getUsername(),
                nodeSet,
                scheduledExecution?.filter,
                scheduledExecution.jobOptionsSet()
        )
        def jobEventStatus
        try {
            jobEventStatus = jobLifecyclePluginService?.beforeJobSave(scheduledExecution, jobPersistEvent)
        } catch (JobLifecyclePluginException exception) {
            log.debug("JobLifecycle error: " + exception.message, exception)
            log.warn("JobLifecycle error: " + exception.message)
            return [success: false, scheduledExecution: scheduledExecution, error: exception.message]
        }
        if(jobEventStatus?.isUseNewValues()){
            SortedSet<Option> rundeckOptions = getOptions(jobEventStatus.getOptions())
            def result = validateOptions(scheduledExecution, rundeckOptions)
            def failed = result.failed
            //try to save workflow
            if(failed){
                scheduledExecution.discard()
                return [success: false, scheduledExecution: scheduledExecution]
            }
            return [success: true, scheduledExecution: scheduledExecution]
        }else{
            return [success: true, scheduledExecution: scheduledExecution]
        }
    }

    def deleteEveryOption(scheduledExecution){
        def todelete = []
        scheduledExecution.options.each {
            todelete << it
        }
        todelete.each {
            it.delete()
            scheduledExecution.removeFromOptions(it)
        }
        scheduledExecution.options = null
    }

    def getOptions(SortedSet<JobOption> jobOptions) {
        SortedSet<Option> options = new TreeSet<>()
        jobOptions.each {
            final ObjectMapper mapper = new ObjectMapper()
            Map<String, Object> map = mapper.convertValue(it, Map.class)
            options.add(new Option(map))
        }
        options
    }

    def addOptions(ScheduledExecution scheduledExecution, SortedSet<Option> rundeckOptions){
        rundeckOptions?.each {
            it.convertValuesList()
            scheduledExecution.addToOptions(it)
        }
    }

    def validateOptions(scheduledExecution, rundeckOptions){
        def optfailed = false
        def optNames = [:]
        rundeckOptions?.each {Option opt ->
            EditOptsController._validateOption(opt, null,scheduledExecution.scheduled)
            fileUploadService.validateFileOptConfig(opt)
            if(!opt.errors.hasErrors() && optNames.containsKey(opt.name)){
                opt.errors.rejectValue('name', 'option.name.duplicate.message', [opt.name] as Object[], "Option already exists: {0}")
            }
            if (opt.errors.hasErrors()) {
                optfailed = true
                def errmsg = opt.name + ": " + opt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                scheduledExecution.errors.rejectValue(
                        'options',
                        'scheduledExecution.options.invalid.message',
                        [errmsg] as Object[],
                        'Invalid Option definition: {0}'
                )
            }
            optNames.put(opt.name, opt)
        }
        if (!optfailed) {
            if(scheduledExecution.options){
                deleteEveryOption(scheduledExecution)
            }
            addOptions(scheduledExecution, rundeckOptions)
            return [failed: false, scheduledExecution: scheduledExecution]
        } else {
            return [failed: true, scheduledExecution: scheduledExecution]
        }
    }

    /**
     * Return a NodeSet for the given filter, it will filter by authorized ones if authContext is not null
     *
     * @param scheduledExecution
     * @param filter
     * @param authContext
     * @param actions
     *
     * @return INodeSet
     */
    def getNodes(scheduledExecution, filter, authContext = null, Set<String> actions = null){

        NodesSelector nodeselector
        if (scheduledExecution.doNodedispatch) {
            //set nodeset for the context if doNodedispatch parameter is true
            filter = filter? filter : scheduledExecution.asFilter()
            def filterExclude = scheduledExecution.asExcludeFilter()
            NodeSet nodeset = ExecutionService.filtersAsNodeSet([
                    filter:filter,
                    filterExclude: filterExclude,
                    nodeExcludePrecedence:scheduledExecution.nodeExcludePrecedence,
                    nodeThreadcount: scheduledExecution.nodeThreadcount,
                    nodeKeepgoing: scheduledExecution.nodeKeepgoing
            ])
            nodeselector=nodeset
        } else if(frameworkService != null){
            //blank?
            nodeselector = SelectorUtils.singleNode(frameworkService.frameworkNodeName)
        }else{
            nodeselector = null
        }

        if(authContext){
            return rundeckAuthContextProcessor.filterAuthorizedNodes(
                    scheduledExecution.project,
                    actions,
                    frameworkService.filterNodeSet(nodeselector, scheduledExecution.project),
                    authContext)
        }else{
            return frameworkService.filterNodeSet(nodeselector, scheduledExecution.project)
        }
    }

    /**
     * Returns true if the job is set to schedule
     * @param se
     * @return boolean
     */
    boolean isScheduled(se){
        jobSchedulesService.isScheduled(se.uuid)
    }
    void applyAdhocScheduledExecutionsCriteria(HibernateCriteriaBuilder delegate, boolean selectAll, String fromServerUUID, String toServerUUID, String project){
        delegate.executions(CriteriaSpecification.LEFT_JOIN) {
            eq('status', ExecutionService.EXECUTION_SCHEDULED)
            isNull('dateCompleted')
            gt('dateStarted', new Date())
            if(project){
                eq('project',project)
            }
            if (!selectAll) {
                if (fromServerUUID) {
                    eq('serverNodeUUID', fromServerUUID)
                } else {
                    isNull('serverNodeUUID')
                }
            } else {
                or {
                    isNull('serverNodeUUID')
                    ne('serverNodeUUID', toServerUUID)
                }
            }
        }
    }
    /**
     * Return jobs  that should be claimed for rescheduling on a cluster member, which includes
     * all jobs that are have adhoc scheduled executions, where the cluster owner
     * matches the criteria
     * @param toServerUUID node ID of claimant
     * @param fromServerUUID null for unclaimed jobs, or owner server node ID
     * @param selectAll if true, match jobs not owned by claimant, if false match based on fromServerUUID setting
     * @param projectFilter project name for jobs, or null for any project
     * @return
     */
    List<ScheduledExecution> getJobsWithAdhocScheduledExecutionsToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter) {
        return ScheduledExecution.createCriteria().listDistinct {
            applyAdhocScheduledExecutionsCriteria(delegate, selectAll, fromServerUUID, toServerUUID, projectFilter)
        }
    }
        /**
     * Return jobs  that should be claimed for rescheduling on a cluster member, which includes
     * all jobs that are either *scheduled* or in the jobids list or have adhoc scheduled executions, where the cluster owner
     * matches the criteria
     * @param toServerUUID node ID of claimant
     * @param fromServerUUID null for unclaimed jobs, or owner server node ID
     * @param selectAll if true, match jobs not owned by claimant, if false match based on fromServerUUID setting
     * @param projectFilter project name for jobs, or null for any project
     * @param jobids explicit list of job IDs to select
     * @param ignoreInnerScheduled if true, do not select only scheduled jobs
     * @return
     */
    List<ScheduledExecution> getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids, ignoreInnerScheduled = false) {

        return ScheduledExecution.createCriteria().listDistinct {
            or {
                applyAdhocScheduledExecutionsCriteria(delegate, selectAll, fromServerUUID, toServerUUID, projectFilter)
                and{
                    if(!ignoreInnerScheduled){
                        eq('scheduled', true)
                    }
                    if (!selectAll) {
                        if (fromServerUUID) {
                            eq('serverNodeUUID', fromServerUUID)
                        } else {
                            isNull('serverNodeUUID')
                        }
                    } else {
                        or {
                            isNull('serverNodeUUID')
                            ne('serverNodeUUID', toServerUUID)
                        }
                    }
                    if (jobids){
                        'in'('uuid', jobids)
                    }
                }
            }
            if (projectFilter) {
                eq('project', projectFilter)
            }
        }
    }

    /**
     * It registers job to quartz, also using TriggersExtender Beans
     * @param jobDetail
     * @param triggerBuilderList
     * @param temporary indicates it should not be added to quartz
     * @param se
     * @return nextDate next execution date, or null if the trigger could not be registered
     */
    def registerOnQuartz(JobDetail jobDetail, List<TriggerBuilderHelper> triggerBuilderHelperList, temporary, se){
        triggerBuilderHelperList = applyTriggerComponents(jobDetail, triggerBuilderHelperList)
        Set triggers = []
        triggerBuilderHelperList?.each {
            def trigger = it.getTriggerBuilder().build()
            triggers.add(trigger)
        }

        if(!temporary){
            quartzScheduler.deleteJob(new JobKey(se.generateJobScheduledName(), se.generateJobGroupName()))
            try {
                quartzScheduler.scheduleJob(jobDetail, triggers, true)
            } catch (SchedulerException e) {
                log.warn("Failed to schedule job: $se.extid in project $se.project: ${e.message}")
                log.debug("Failed to schedule job: $se.extid in project $se.project: ${e.message}",e)
                return null
            }
        }
        return getNextExecutionDateFromTriggers(triggers)
    }

    /**
     * It calls every TriggersExtender bean to apply extra settings to the triggers
     * @param jobDetail
     * @param triggerBuilderHelperList
     * @return triggerBuilderHelperList
     */
    @NotTransactional
    def applyTriggerComponents(JobDetail jobDetail, List<TriggerBuilderHelper> triggerBuilderHelperList){
        triggerComponents.each { name, triggerComponent ->
            triggerComponent.extendTriggers(jobDetail, triggerBuilderHelperList)
        }
        return triggerBuilderHelperList
    }

    /**
     * It gets the next fire date based on the trigger list recieved
     * @param triggers
     * @return date
     */
    def getNextExecutionDateFromTriggers(triggers){
        def nextDate = null
        def dates = []
        triggers?.each {
            dates.addAll(TriggerUtils.computeFireTimes(it, (it.calendarName? quartzScheduler.getCalendar(it.calendarName):null), 1))
        }
        if(dates){
            Collections.sort(dates)
            nextDate = dates.get(0)
        }
        return nextDate
    }

    /**
     * It builds a trigger with the given parameters (used to build the cleanup job)
     * @param jobName
     * @param jobGroup
     * @param cronExpression
     * @param priority
     * @return Trigger
     */
    Trigger localCreateTrigger(String jobName, String jobGroup, String cronExpression, int priority = 5) {
        Trigger trigger
        try {
            trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withPriority(priority)
                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    def prepareCreateEditJob(params, def scheduledExecution, String action, UserAndRolesAuthContext authContext ){
        def pluginControlService=frameworkService.getPluginControlService(params.project)
        def nodeStepTypes = frameworkService.getNodeStepPluginDescriptions()?.findAll{
            !pluginControlService?.isDisabledPlugin(it.name,ServiceNameConstants.WorkflowNodeStep)
        }
        def stepTypes = frameworkService.getStepPluginDescriptions()?.findAll{
            !pluginControlService?.isDisabledPlugin(it.name,ServiceNameConstants.WorkflowStep)
        }
        def strategyPlugins = getWorkflowStrategyPluginDescriptions()

        def crontab = [:]
        if(scheduledExecution?.scheduled){
            crontab=scheduledExecution.timeAndDateAsBooleanMap()
        }

        def notificationPlugins = notificationService.listNotificationPlugins().findAll { k, v ->
            !pluginControlService?.isDisabledPlugin(k, ServiceNameConstants.Notification)
        }

        def notificationPluginsDynamicProperties = notificationService.listNotificationPluginsDynamicProperties(params.project,
                rundeckAuthorizedServicesProvider.getServicesWith(authContext)).findAll{k,v->
            !pluginControlService?.isDisabledPlugin(k,ServiceNameConstants.Notification)
        }

        def orchestratorPlugins = orchestratorPluginService.listDescriptions()
        def globals=frameworkService.getProjectGlobals(scheduledExecution?.project).keySet()

        def timeZones = getTimeZones()
        def logFilterPlugins = pluginService.listPlugins(LogFilterPlugin).findAll{k,v->
            !pluginControlService?.isDisabledPlugin(k,ServiceNameConstants.LogFilter)
        }

        def executionLifecyclePlugins = executionLifecyclePluginService.listEnabledExecutionLifecyclePlugins(pluginControlService)
        def jobComponents = rundeckJobDefinitionManager.getJobDefinitionComponents()

        def fprojects = frameworkService.projectNames(authContext)

        def model = [scheduledExecution          : scheduledExecution,
                     crontab                     : crontab,
                     notificationPlugins         : notificationPlugins,
                     notificationPluginsDynamicProperties : notificationPluginsDynamicProperties,
                     orchestratorPlugins         : orchestratorPlugins,
                     strategyPlugins             : strategyPlugins,
                     params                      : params,
                     matchedNodesMaxCount        : getMatchedNodesMaxCount(),
                     nodeStepDescriptions        : nodeStepTypes,
                     stepDescriptions            : stepTypes,
                     timeZones                   : timeZones,
                     logFilterPlugins            : logFilterPlugins,
                     executionLifecyclePlugins   : executionLifecyclePlugins,
                     projectNames                : fprojects,
                     globalVars                  : globals,
                     jobComponents               : jobComponents
        ]

        if(action == AuthConstants.ACTION_UPDATE){
            def jobComponentValues=rundeckJobDefinitionManager.getJobDefinitionComponentValues(scheduledExecution)
            model["nextExecutionTime"] = nextExecutionTime(scheduledExecution)
            model["authorized"]  = userAuthorizedForJob(scheduledExecution,authContext)
            model["jobComponentValues"] = jobComponentValues
        }

        return model

    }

}
@CompileStatic
class OldJob{
    String oldjobname
    String oldjobgroup
    Boolean isScheduled
    Boolean localScheduled
    String originalCron
    Boolean originalSchedule
    Boolean originalExecution
    String originalTz
    JobRevReferenceImpl originalRef

    boolean wasRenamed(String jobName, String groupPath) {
        originalRef.jobName != jobName || originalRef.groupPath != groupPath
    }

    boolean schedulingWasChanged(ScheduledExecution scheduledExecution){
        return originalCron != scheduledExecution.generateCrontabExression() ||
                originalSchedule != scheduledExecution.scheduleEnabled ||
                originalExecution != scheduledExecution.executionEnabled ||
                originalTz != scheduledExecution.timeZone ||
                localScheduled != scheduledExecution.scheduled ||
                wasRenamed(scheduledExecution.jobName,scheduledExecution.groupPath)
    }
}
