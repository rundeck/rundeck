package org.rundeck.app.data.validation.validators.schedule

import org.quartz.CronExpression
import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.RdSchedule
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class JobScheduleValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        return RdJob.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        RdJob job = (RdJob)target
        if(job.scheduled && job.schedule) {
            def hasCrontab = job.schedule.crontabString && job.schedule.crontabString != job.schedule?.generateCrontabExpression()
            def genCron = hasCrontab ? job.schedule.crontabString : job.schedule.generateCrontabExpression()
            if (!CronExpression.isValidExpression(genCron)) {
                errors.rejectValue(
                        'schedule.crontabString',
                        'scheduledExecution.crontabString.invalid.message', [genCron] as Object[], "invalid: {0}"
                )
            } else {
                //test for valid schedule
                CronExpression c = new CronExpression(genCron)
                def next = c.getNextValidTimeAfter(new Date());
                if (!next) {
                    errors.rejectValue(
                            'schedule.crontabString',
                            'scheduledExecution.crontabString.noschedule.message', [genCron] as Object[], "invalid: {0}"
                    )
                }
            }
        }
        if (job.timeZone) {
            TimeZone test = TimeZone.getTimeZone(job.timeZone)
            boolean found = Arrays.asList(TimeZone.getAvailableIDs()).contains(job.timeZone);
            if (!found && test.getID() == 'GMT' && job.timeZone != 'GMT') {
                errors.rejectValue(
                        'timeZone',
                        'scheduledExecution.timezone.error.message', [job.timeZone] as Object[],
                        "Invalid: {0}"
                )
            }
        }
    }
}
