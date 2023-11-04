package com.dtolabs.rundeck.app.api.jobs.browse

import com.dtolabs.rundeck.app.api.marshall.Ignore
import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.job.JobBrowseItem

@CompileStatic
class JobBrowseItemData {
    boolean job
    String groupPath
    @Ignore(onlyIfNull = true)
    String jobName
    @Ignore(onlyIfNull = true)
    String description
    @Ignore(onlyIfNull = true)
    String id
    @Ignore(onlyIfNull = true)
    List<JobItemMeta> meta

    static JobBrowseItemData from(JobBrowseItem model) {
        new JobBrowseItemData(
            job: model.job,
            groupPath: model.groupPath ?: model.jobData?.groupPath,
            jobName: model.jobData?.jobName,
            id: model.jobData?.uuid,
            description: model.jobData?.description,
            )
    }
}
