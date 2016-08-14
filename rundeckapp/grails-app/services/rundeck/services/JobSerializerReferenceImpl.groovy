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

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 4/28/15.
 */
class JobSerializerReferenceImpl extends JobReferenceImpl implements JobExportReference{
    JobSerializer jobSerializer
    Long version

    JobSerializerReferenceImpl(
            JobRevReference ref,
            JobSerializer jobSerializer
    )
    {
        this.id=ref.id
        this.groupPath=ref.groupPath
        this.jobName=ref.jobName
        this.project=ref.project
        this.version=ref.version
        this.jobSerializer=jobSerializer
    }

}
