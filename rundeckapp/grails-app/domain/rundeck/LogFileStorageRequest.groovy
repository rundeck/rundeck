package rundeck

class LogFileStorageRequest {
    Execution execution
    String pluginName
    String filekey
    Boolean completed

    Date dateCreated
    Date lastUpdated

    static constraints = {
        execution nullable: false
        pluginName maxSize: 255
        filekey nullable: true
    }
}
