package org.rundeck.app.job.option;

import org.rundeck.app.data.model.v1.job.JobData;

import java.util.Map;

public interface RemoteOptionValueLoader {

    /**
     * Load options values from remote URL
     * @param  jobData
     * @param mapConfig
     * @return option remote
     */
    RemoteOptionValuesResponse loadOptionsRemoteValues(JobData jobData, Map mapConfig, String username);
}
