package org.rundeck.app.data.jobfilerecord

import org.rundeck.app.jobfilerecord.JobFileRecordValidationResult
import org.rundeck.app.jobfilerecord.JobFileRecordValidator
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.FileUploadService

class AdaptingJobFileRecordValidator implements JobFileRecordValidator {
    @Autowired
    FileUploadService fileUploadService
    @Override
    JobFileRecordValidationResult validateFileRefForJobOption(String fileuuid, String jobid, String option, boolean isJobRef) {
        return new JobFileRecordValidationResult(fileUploadService.validateFileRefForJobOption(fileuuid, jobid, option, isJobRef))
    }
}
