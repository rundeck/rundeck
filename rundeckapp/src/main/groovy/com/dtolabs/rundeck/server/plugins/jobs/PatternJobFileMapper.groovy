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

package com.dtolabs.rundeck.server.plugins.jobs

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.JobFileMapper

/**
 * Maps a job ref to a file path
 */
class PatternJobFileMapper implements JobFileMapper{
    String mappingPath
    @Override
    File fileForJob(final JobReference jobReference) {
        return new File(substitute(mappingPath,jobReference))
    }

    @Override
    String pathForJob(final JobReference jobReference) {
        return substitute(mappingPath,jobReference)
    }

    private String substitute(String key, JobReference reference){
        return substitute(key,[
                project: reference.project,
                id: reference.id,
                name: reference.jobName,
                group: (reference.groupPath?reference.groupPath+'/':'')
        ])
    }
    private String substitute(String key,Map data){
        DataContextUtils.replaceDataReferencesInString(key,
                                               DataContextUtils.addContext(
                                                       "job",
                                                       data,
                                                       null
                                               )
        )
    }
}
