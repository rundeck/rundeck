package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobExportReference;
import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.core.jobs.JobRevReference;
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent;
import com.dtolabs.rundeck.plugins.scm.JobSerializer;
import com.dtolabs.rundeck.plugins.scm.ScmPlugin;

/**
 * Created by greg on 4/28/15.
 */
public interface JobChangeListener {
    public void jobChangeEvent(
            JobChangeEvent event,
            JobSerializer serializer
    );
}
