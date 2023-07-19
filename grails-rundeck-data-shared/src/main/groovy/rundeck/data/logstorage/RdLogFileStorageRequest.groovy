package rundeck.data.logstorage

import grails.validation.Validateable
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData

class RdLogFileStorageRequest implements LogFileStorageRequestData, Validateable {
    Long id
    Long executionId
    String pluginName
    String filetype
    Boolean completed
    Date dateCreated
    Date lastUpdated

    static constraints = {
        executionId nullable: false
        pluginName maxSize: 255
        filetype nullable: true
    }
}
