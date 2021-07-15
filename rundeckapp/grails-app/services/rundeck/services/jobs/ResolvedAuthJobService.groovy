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

package rundeck.services.jobs


import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.jobs.JobExecutionError
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState
import grails.plugins.mail.MailMessageBuilder
import groovy.transform.CompileStatic
import org.rundeck.core.executions.Provenance
import org.springframework.context.ApplicationContext

/**
 * Created by greg on 2/3/15.
 */
@CompileStatic
class ResolvedAuthJobService implements JobService {
    AuthorizingJobService authJobService
    UserAndRolesAuthContext authContext

    @Override
    JobReference jobForID(String uuid, String project) throws JobNotFound {
        authJobService.jobForID(authContext, uuid, project)
    }

    @Override
    JobReference jobForName(String name, String project) throws JobNotFound {
        authJobService.jobForName(authContext, name, project)
    }

    @Override
    JobReference jobForName(String group, String name, String project) throws JobNotFound {
        authJobService.jobForName(authContext, group, name, project)
    }

    @Override
    JobState getJobState(JobReference jobReference) throws JobNotFound {
        authJobService.getJobState(authContext, jobReference)
    }

    List<ExecutionReference> searchExecutions(String state, String project, String job, String excludeJob,
                                              String since, boolean reverseSince){
        authJobService.searchExecutions(authContext, state, project, job, excludeJob, since, reverseSince)
    }

    List<ExecutionReference> searchExecutions(String state, String project, String job, String excludeJob, String since){
        authJobService.searchExecutions(authContext, state, project, job, excludeJob, since)
    }

    ExecutionReference executionForId(String id, String project) throws ExecutionNotFound{
        authJobService.executionForId(authContext, id, project)
    }


    @Override
    ExecutionReference runJob(final RunJob runJob) throws JobNotFound, JobExecutionError {
        authJobService.runJob(authContext, runJob)
    }

    @Override
    Object sendMail(@DelegatesTo(strategy = 1, value = MailMessageBuilder.class) final Closure dsl) {
        return null
    }

    @Override
    ApplicationContext getApplicationContext() {
        return null
    }

    @Override
    void setApplicationContext(final ApplicationContext applicationContext) {

    }

    Map deleteBulkExecutionIds(Collection ids, String asUser){
        authJobService.deleteBulkExecutionIds(authContext, ids, asUser)
    }

    Map queryExecutions(Map filter){
        authJobService.queryExecutions(authContext, filter)
    }

}
