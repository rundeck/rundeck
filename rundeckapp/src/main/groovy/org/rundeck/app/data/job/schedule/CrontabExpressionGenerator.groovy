package org.rundeck.app.data.job.schedule

import org.rundeck.app.data.model.v1.job.JobData

interface CrontabExpressionGenerator {
    String generate(JobData jobData)
}