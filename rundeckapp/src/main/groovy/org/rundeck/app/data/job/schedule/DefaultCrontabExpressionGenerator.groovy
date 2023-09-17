package org.rundeck.app.data.job.schedule

import org.rundeck.app.data.model.v1.job.JobData

class DefaultCrontabExpressionGenerator implements CrontabExpressionGenerator {

    String generate(JobData jobData) {
        return [jobData.schedule?.seconds?jobData.schedule?.seconds:'0',
                jobData.schedule?.minute?jobData.schedule?.minute:'0',
                jobData.schedule?.hour?jobData.schedule?.hour:'0',
                jobData.schedule?.dayOfMonth?jobData.schedule?.dayOfMonth?.toUpperCase():'?',
                jobData.schedule?.month?jobData.schedule?.month?.toUpperCase():'*',
                jobData.schedule?.dayOfMonth=='?'?jobData.schedule?.dayOfWeek?.toUpperCase():'?',
                jobData.schedule?.year?jobData.schedule?.year:'*'].join(" ")
    }

    static String generateCrontab(JobData jobData) {
        new DefaultCrontabExpressionGenerator().generate(jobData)
    }
}
