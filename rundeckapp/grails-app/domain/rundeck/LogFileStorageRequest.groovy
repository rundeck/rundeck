package rundeck

class LogFileStorageRequest {
    Execution execution
    String pluginName
    String filetype
    Boolean completed

    Date dateCreated
    Date lastUpdated

    static belongsTo = Execution

    static constraints = {
        execution nullable: false
        pluginName maxSize: 255
        filetype nullable: true
    }
}
