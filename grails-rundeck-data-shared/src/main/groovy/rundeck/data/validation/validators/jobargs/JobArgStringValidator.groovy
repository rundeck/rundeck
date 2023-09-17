package rundeck.data.validation.validators.jobargs

import org.rundeck.app.data.model.v1.job.JobData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.text.SimpleDateFormat

class JobArgStringValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return JobData.class.isAssignableFrom(clazz);
    }

    @Override
    void validate(Object target, Errors errors) {
        JobData jobData = (JobData)target
        if(jobData.argString) {
            try {
                jobData.argString.replaceAll(
                        /\$\{DATE:(.*)\}/,
                        { all, String tstamp ->
                            new SimpleDateFormat(tstamp).format(new Date())
                        }
                )
            } catch (IllegalArgumentException e) {
                errors.rejectValue(
                        'argString',
                        'scheduledExecution.argString.datestamp.invalid',
                        [e.getMessage()].toArray(),
                        'datestamp format is invalid: {0}'
                )
            }
        }
    }
}
