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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState

/**
 * Created by greg on 2/3/15.
 */
class ResolvedAuthJobService implements JobService {
    AuthorizingJobService authJobService
    AuthContext authContext

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


    String startJob(JobReference jobReference, String jobArgString, String jobFilter, String asUser) throws JobNotFound{
        authJobService.startJob(authContext, jobReference, jobArgString, jobFilter, asUser)
    }

    Map deleteBulkExecutionIds(Collection ids, String asUser){
        authJobService.deleteBulkExecutionIds(authContext, ids, asUser)
    }

    Map queryExecutions(Map filter){
        authJobService.queryExecutions(authContext, filter)
    }

}
