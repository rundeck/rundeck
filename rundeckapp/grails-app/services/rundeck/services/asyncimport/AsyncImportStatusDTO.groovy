package rundeck.services.asyncimport

import groovy.transform.builder.Builder
import org.apache.commons.beanutils.PropertyUtils

@Builder
class AsyncImportStatusDTO {
    String tempFilepath;
    String projectName;
    Integer milestoneNumber;
    String milestone;
    String lastUpdated;
    String errors;
    String lastUpdate;
    String jobUuidOption;

    AsyncImportStatusDTO(){}

    AsyncImportStatusDTO(
            String projectName,
            int milestoneNumber
    ){
        this.projectName = projectName
        this.milestoneNumber = milestoneNumber
    }

    AsyncImportStatusDTO(
            AsyncImportStatusDTO newStatus
    ){
        this.tempFilepath = newStatus.tempFilepath
        this.projectName = newStatus.projectName
        this.milestoneNumber = newStatus.milestoneNumber
        this.milestone = newStatus.milestone
        this.lastUpdated = newStatus.lastUpdated
        this.errors = newStatus.errors
        this.lastUpdate = newStatus.lastUpdate
        this.jobUuidOption = newStatus.jobUuidOption
    }
}
