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

    /**
     * Replaces all null properties in destinationDTO with the properties in sourceDTO that match with null keys of destinationDTO,
     * be sure that destinationDTO has null the values that you DONT want to change of status file in DB.
     *
     * @param destinationDTO
     * @param sourceDTO
     * @return
     */
    static def replacePropsInTargetDTO(AsyncImportStatusDTO destinationDTO, AsyncImportStatusDTO sourceDTO) {
        try {
            PropertyUtils.describe(sourceDTO).entrySet().stream()
                    .filter(source -> source.getValue() != null)
                    .filter(source -> !source.getKey().equals("class"))
                    .forEach(source -> {
                        try {
                            if( destinationDTO[source.getKey()] == null ){
                                PropertyUtils.setProperty(destinationDTO, source.getKey(), source.getValue())
                            }
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
