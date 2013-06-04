package rundeck

class LogFileStorageRequest {
    Execution execution
    String pluginName
    Boolean completed

    Date dateCreated
    Date lastUpdated

    static constraints = {
        execution nullable: false
        pluginName maxSize: 255
    }
}
