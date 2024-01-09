package org.rundeck.app.quartz

import rundeck.data.quartz.QuartzJobSpecifier
import rundeck.quartzjobs.ExecutionJob

class ExecutionJobQuartzJobSpecifier implements QuartzJobSpecifier {
    @Override
    Class<ExecutionJob> getJobClass() {
        return ExecutionJob
    }
}
