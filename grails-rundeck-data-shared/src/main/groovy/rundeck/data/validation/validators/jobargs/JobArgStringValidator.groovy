package rundeck.data.validation.validators.jobargs

import org.rundeck.app.data.model.v1.job.JobData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.ZoneId

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
                            // Extract optional timezone from last colon-segment
                            String fdate = tstamp
                            final lastColon = tstamp.lastIndexOf(":")
                            if (lastColon >= 0) {
                                try {
                                    ZoneId.of(tstamp.substring(lastColon + 1))
                                    fdate = tstamp.substring(0, lastColon)
                                } catch (DateTimeException ignored) {
                                    // last segment is not a valid ZoneId; treat whole tstamp as FORMAT
                                }
                            }
                            new SimpleDateFormat(fdate).format(new Date())
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
