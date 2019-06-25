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

import com.dtolabs.rundeck.app.support.BaseNodeFilters
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeFilter
import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobExecutionError
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState
import com.dtolabs.rundeck.core.utils.NodeSet
import grails.gorm.transactions.Transactional
import org.rundeck.util.Sizes
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.jobs.AuthorizingJobService
import rundeck.services.jobs.ResolvedAuthJobService

import java.util.concurrent.TimeUnit

@Transactional
class JobStateService implements AuthorizingJobService {
    def frameworkService
    AppAuthContextEvaluator rundeckAuthContextEvaluator

    @Override
    JobReference jobForID(AuthContext auth, String uuid, String project) throws JobNotFound {
        def job = ScheduledExecution.getByIdOrUUID(uuid)
        if (null == job || job.project != project) {
            throw new JobNotFound("Not found", uuid, project)
        }
        checkJobView(auth, job, uuid, project)
        return job.asReference()
    }

    public void checkJobView(AuthContext auth, ScheduledExecution job, String uuid, String project) {
        if (!authCheckJob(auth, job)) {
            throw new JobNotFound("Not found", uuid, project)
        }
    }

    public void checkJobView(AuthContext auth, ScheduledExecution job, String name, String group, String project) {
        if (!authCheckJob(auth, job)) {
            throw new JobNotFound("Not found", name, group, project)
        }
    }

    public boolean authCheckJob(AuthContext auth, ScheduledExecution job) {
        rundeckAuthContextEvaluator.authorizeProjectJobAny(
            auth,
            job,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
            job.project
        )
    }

    @Override
    JobReference jobForName(AuthContext auth, String name, String project) throws JobNotFound {
        def group = null
        if (name.lastIndexOf('/') >= 0 && name.lastIndexOf('/') < name.size() - 1) {
            group = name.substring(0, name.lastIndexOf('/'))
            name = name.substring(name.lastIndexOf('/') + 1)
        }
        return jobForName(auth, group, name, project)
    }

    @Override
    JobReference jobForName(AuthContext auth, String group, String name, String project) throws JobNotFound {
        def job = ScheduledExecution.findByProjectAndJobNameAndGroupPath(project, name, group)
        if (null == job) {
            throw new JobNotFound("Not found", name, group, project)
        }
        checkJobView(auth, job, name, group, project)

        return job.asReference()
    }

    @Override
    JobState getJobState(AuthContext auth, JobReference jobReference) throws JobNotFound {
        def job = ScheduledExecution.getByIdOrUUID(jobReference.id)
        if (null == job) {
            throw new JobNotFound("Not found", jobReference.id, jobReference.project)
        }
        checkJobView(auth, job, jobReference.id, jobReference.project)

        List<Execution> running = Execution.findAllByScheduledExecutionAndDateCompletedIsNull(job)
        List<Execution> lastExec = Execution.findAllByScheduledExecutionAndDateCompletedIsNotNull(
                job,
                [order: 'desc', sort: 'dateCompleted', max: 1]
        )
        def previousState = null
        def previousCustom = null
        if (lastExec.size() == 1) {
            previousState = ExecutionState.valueOf(lastExec[0].executionState.replaceAll('[-]', '_'))
            previousCustom=lastExec[0].customStatusString
        }

        def runningIds = new HashSet<String>(running.collect { it.id.toString() })
        new JobStateImpl(
                running: running.size() > 0,
                runningExecutionIds: runningIds,
                previousExecutionState: previousState,
                previousExecutionStatusString: previousCustom
        )
    }
    /**
     * @param auth
     * @return a JobService which uses the auth context for authorization
     */
    JobService jobServiceWithAuthContext(UserAndRolesAuthContext auth) {
        return new ResolvedAuthJobService(authContext: auth, authJobService: this)
    }

    @Override
    List<ExecutionReference> searchExecutions(AuthContext auth, String state, String project, String jobUuid,
                                              String excludeJobUuid, String since){
        searchExecutions(auth,state,project,jobUuid,excludeJobUuid,since,false)
    }

    List<ExecutionReference> searchExecutions(AuthContext auth, String state, String project, String jobUuid,
                                              String excludeJobUuid, String since, boolean reverseSince){

        def executions = Execution.createCriteria().list {
            eq('project',project)
            if(state){
                eq('status', state)
            }
            createAlias('scheduledExecution', 'se')
            if(jobUuid){
                isNotNull 'scheduledExecution'
                eq 'se.uuid',jobUuid
            }
            if(excludeJobUuid){
                or{
                    isNull('scheduledExecution')
                    ne 'se.uuid',excludeJobUuid
                }
            }
            if(since){
                long timeAgo = Sizes.parseTimeDuration(since,TimeUnit.MILLISECONDS)
                Date sinceDt = new Date()
                sinceDt.setTime(sinceDt.getTime()-timeAgo)
                if(reverseSince){
                    le 'dateStarted',sinceDt
                }else{
                    ge 'dateStarted',sinceDt
                }

            }

        }
        executions = rundeckAuthContextEvaluator.filterAuthorizedProjectExecutionsAll(auth,executions,[AuthConstants.ACTION_READ])

        IFramework framework = frameworkService.getRundeckFramework()
        def frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(project)
        INodeSet allNodes = frameworkProject.getNodeSet()
        def fwkNode = framework.frameworkNodeName
        def genTargetNode = { Execution exec ->
            if (!exec.filter) {
                return fwkNode
            } else {
                NodeSet filterNodeSet = filtersAsNodeSet(
                        [
                                filter               : exec.filter,
                                nodeExcludePrecedence: exec.nodeExcludePrecedence,
                                nodeThreadcount      : exec.nodeThreadcount,
                                nodeKeepgoing        : exec.nodeKeepgoing
                        ]
                )
                INodeSet nodeSet = NodeFilter.filterNodes(filterNodeSet, allNodes)
                return nodeSet.nodes*.nodename.join(',')
            }
        }
        return executions.collect { exec ->
            exec.asReference genTargetNode
        }
    }

    NodeSet filtersAsNodeSet(Map econtext) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(BaseNodeFilters.asExcludeMap(econtext)).setDominant(econtext.nodeExcludePrecedence?true:false);
        nodeset.createInclude(BaseNodeFilters.asIncludeMap(econtext)).setDominant(!econtext.nodeExcludePrecedence?true:false);
        nodeset.setKeepgoing(econtext.nodeKeepgoing?true:false)
        nodeset.setThreadCount(econtext.nodeThreadcount?econtext.nodeThreadcount:1)
        return nodeset
    }

    @Override
    ExecutionReference executionForId(AuthContext auth, String id, String project) throws ExecutionNotFound{
        long exid = Long.valueOf(id)
        def exec = Execution.findByIdAndProject(exid,project)
        if(!exec){
            throw new ExecutionNotFound("Execution not found", id, project)
        }
        ScheduledExecution se = exec.scheduledExecution
        def isAuth = null
        if(se){
            isAuth = rundeckAuthContextEvaluator.authorizeProjectJobAny(
                auth,
                se,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
                se.project
            )
        }else if(!se){
            isAuth=rundeckAuthContextEvaluator.authorizeProjectResourceAll(auth, AuthConstants.RESOURCE_ADHOC, [AuthConstants.ACTION_READ],
                    exec.project)
        }
        if(!isAuth){
            throw new ExecutionNotFound("Execution not found", id, project)
        }
        return exec.asReference()
    }


    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            String executionType,
            Map<String, String> provenance,
            String jobArgString,
            String jobFilter,
            String asUser
    )
            throws JobNotFound, JobExecutionError {
        runJob(auth, jobReference, jobArgString, jobFilter, asUser, null)
    }

    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            String jobArgString,
            String jobFilter,
            String asUser,
            Map<String, ?> meta
    )throws JobNotFound, JobExecutionError {

        def inputOpts = ["argString": jobArgString, executionType: executionType, provenance: provenance]
        return doRunJob(jobFilter, inputOpts, jobReference, auth, asUser, meta)
    }

    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            String jobArgString,
            String jobFilter,
            String asUser
    )
        throws JobNotFound, JobExecutionError {
        return runJob(auth, jobReference, 'user', [:], jobArgString, jobFilter, asUser)
    }

    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            String executionType,
            Map<String, String> provenance,
            Map optionData,
            String jobFilter,
            String asUser
    )
        throws JobNotFound, JobExecutionError {
        runJob(auth, jobReference, optionData, jobFilter, asUser, null)
    }

    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            Map optionData,
            String jobFilter,
            String asUser,
            Map<String, ?> meta
    )
            throws JobNotFound, JobExecutionError {
        def inputOpts = [executionType: executionType, provenance: provenance]
        optionData.each { k, v ->
            inputOpts['option.' + k] = v
        }
        return doRunJob(jobFilter, inputOpts, jobReference, auth, asUser, meta)

    }

    @Override
    ExecutionReference runJob(
            UserAndRolesAuthContext auth,
            JobReference jobReference,
            Map optionData,
            String jobFilter,
            String asUser
    )
            throws JobNotFound, JobExecutionError {
        return runJob(auth, jobReference, 'user', [:], optionData, jobFilter, asUser)
    }

    ExecutionReference doRunJob(
            String jobFilter,
            Map inputOpts,
            JobReference jobReference,
            UserAndRolesAuthContext auth,
            String asUser
    ) {
        doRunJob(jobFilter, inputOpts, jobReference, auth, asUser, null)
    }

    ExecutionReference doRunJob(
            String jobFilter,
            Map inputOpts,
            JobReference jobReference,
            UserAndRolesAuthContext auth,
            String asUser,
            Map<String, ?> meta
    ) {
        if (jobFilter) {
            inputOpts.filter = jobFilter
            inputOpts['_replaceNodeFilters'] = 'true'
            inputOpts['doNodedispatch'] = true
            if (null == inputOpts['nodeExcludePrecedence']) {
                inputOpts['nodeExcludePrecedence'] = true
            }
        }

        if (!inputOpts['executionType']) {
            inputOpts['executionType'] = 'user'
        }

        def se = ScheduledExecution.findByUuidAndProject(jobReference.id, jobReference.project)
        if (!se || !rundeckAuthContextEvaluator.authorizeProjectJobAny(
            auth,
            se,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW],
            se.project
        )) {
            throw new JobNotFound("Not found", jobReference.id, jobReference.project)
        }
        if (meta) {
            inputOpts['meta'] = meta
        }
        def result = frameworkService.kickJob(se, auth, asUser, inputOpts)
        if (result && result.success && result.execution) {
            return result.execution.asReference()
        }
        throw new JobExecutionError(
            result?.message ?: result?.error ?: "Unknown: ${result}",
            jobReference.id,
            jobReference.project
        )
    }


    @Override
    Map deleteBulkExecutionIds(AuthContext auth, Collection ids, String asUser){
        frameworkService.deleteBulkExecutionIds(ids,auth, asUser)
    }

    @Override
    Map queryExecutions(AuthContext auth,Map filter){
        ExecutionQuery query = new ExecutionQuery()
        query.projFilter = filter.project
        int offset = filter.offset?:0
        query.offset = offset
        int max = filter.max?:0
        query.max = max
        if(filter.recentFilter){
            query.recentFilter = filter.recentFilter
            query.configureFilter()
        }
        if (filter.olderFilter) {
            Date endDate=ExecutionQuery.parseRelativeDate(filter.olderFilter)
            if(null!=endDate){
                query.endbeforeFilter = endDate
                query.doendbeforeFilter = true
            }
        }
        if(filter.statusFilter){
            query.statusFilter = filter.statusFilter
        }
        if(filter.userFilter){
            query.userFilter = filter.userFilter
        }
        if(filter.adhoc){
            query.adhoc = true
        }else if(filter.jobonly){
            query.adhoc = false
        }
        if(filter.groupPath){
            query.groupPath = filter.groupPath
        }
        if(filter.groupPathExact){
            query.groupPathExact = filter.groupPathExact
        }
        if(filter.excludeGroupPath){
            query.excludeGroupPath = filter.excludeGroupPath
        }
        if(filter.excludeGroupPathExact){
            query.excludeGroupPathExact = filter.excludeGroupPathExact
        }
        if(filter.jobFilter){
            query.jobFilter = filter.jobFilter
        }
        if(filter.excludeJobFilter){
            query.excludeJobFilter = filter.excludeJobFilter
        }
        if(filter.excludeJobExactFilter){
            query.excludeJobExactFilter = filter.excludeJobExactFilter
        }
        if(filter.jobExactFilter){
            query.jobExactFilter = filter.jobExactFilter
        }
        if(filter.excludeJobIdListFilter){
            query.excludeJobIdListFilter = filter.excludeJobIdListFilter.tokenize(',')
        }
        if(filter.excludeJobListFilter){
            query.excludeJobListFilter = filter.excludeJobListFilter.tokenize(',')
        }
        if(filter.jobListFilter){
            query.jobListFilter = filter.jobListFilter.tokenize(',')
        }
        if(filter.jobIdListFilter){
            query.jobIdListFilter = filter.jobIdListFilter.tokenize(',')
        }
        def results = frameworkService.queryExecutions(query, offset, max)
        def result=results.result
        def total=results.total
        //filter query results to READ authorized executions
        def filtered = rundeckAuthContextEvaluator.filterAuthorizedProjectExecutionsAll(auth,result,[AuthConstants.ACTION_READ])
        def reflist = filtered.collect { it.asReference() }
        return [result:reflist, total:reflist.size()]
    }
}
