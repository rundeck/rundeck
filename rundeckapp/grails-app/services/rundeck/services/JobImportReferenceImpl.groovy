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

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobImportReference
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 9/14/15.
 */
class JobImportReferenceImpl extends JobReferenceImpl implements JobScmReference {
    Long version
    Map scmImportMetadata
    Long importVersion
    JobSerializer jobSerializer
    String sourceId

    JobImportReferenceImpl(
            final JobRevReference job,
            final long importVersion,
            final Map scmImportMetadata,
            String sourceId
    )
    {
        this.id=job.id
        this.groupPath=job.groupPath
        this.jobName=job.jobName
        this.version=job.version
        this.project=job.project
        this.scmImportMetadata = scmImportMetadata
        this.importVersion = importVersion
        this.sourceId = sourceId
    }

    @Override
    String toString() {
            return "JobImportReferenceImpl{" +
                    "id='" + id + '\'' +
                    ", project='" + project + '\'' +
                    ", jobName='" + jobName + '\'' +
                    ", groupPath='" + groupPath + '\'' +
                    ", version='" + version + '\'' +
                    ", importVersion='" + importVersion + '\'' +
                    ", scmImportMetadata='" + scmImportMetadata + '\'' +
                    ", sourceId='" + sourceId + '\'' +
                    '}';
    }
}
