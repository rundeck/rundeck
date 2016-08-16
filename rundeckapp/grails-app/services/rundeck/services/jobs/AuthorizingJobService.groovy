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
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobState

/**
 * Extends {@link com.dtolabs.rundeck.core.jobs.JobService} interface to include an AuthContext
 */
interface AuthorizingJobService {

    JobReference jobForID(AuthContext auth, String uuid, String project) throws JobNotFound;

    JobReference jobForName(AuthContext auth, String name, String project) throws JobNotFound;

    JobReference jobForName(AuthContext auth, String group, String name, String project) throws JobNotFound;

    JobState getJobState(AuthContext auth, JobReference jobReference) throws JobNotFound;
}