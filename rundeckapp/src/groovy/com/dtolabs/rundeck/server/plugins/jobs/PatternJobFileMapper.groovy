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

    private String substitute(String key,JobReference reference){
        return substitute(key,[
                project: reference.project,
                id: reference.id,
                name: reference.jobName,
                group: (reference.groupPath?reference.groupPath+'/':'')
        ])
    }
    private String substitute(String key,Map data){
        DataContextUtils.replaceDataReferences(key,
                                               DataContextUtils.addContext(
                                                       "job",
                                                       data,
                                                       null
                                               )
        )
    }
}
