package org.rundeck.app.jobfilerecord;

public interface JobFileRecordValidator {
    JobFileRecordValidationResult validateFileRefForJobOption(String fileuuid, String jobid, String option, boolean isJobRef);
}
