package rundeck

class JobFileRecord {
    /**
     * Original file name
     */
    String fileName
    /**
     * Data size
     */
    Long size
    /**
     * The type of file record
     */
    String recordType
    String user
    Date dateCreated
    Date lastUpdated
    Date expirationDate
    Boolean retained = false //true if file+record should be retained, false if it may be expired and removed
    //TODO change to deleted
    Boolean available = false //true if file is still available after execution
    String uuid
    String jobId
    String storageType // storage plugin type "tmpdir", "storage"
    String storageReference // path in storage facility, or temp dir, depends on backend plugin
    String storageMeta // metadata...?
    Execution execution
    static constraints = {
        fileName(nullable: false, maxSize: 1024)
        size(nullable: false)
        recordType(nullable: false, maxSize: 255)
        user(nullable: false, maxSize: 255)
        expirationDate(nullable: true)
        uuid(nullable: false)
        jobId(nullable: false)
        storageType(nullable: false, maxSize: 255)
        storageReference(nullable: false, maxSize: 255)
        storageMeta(nullable: true)
        execution(nullable: true)
    }

    static mapping = {
        storageMeta(type: 'text')
    }

    @Override
    public String toString() {
        return "rundeck.JobFileRecord{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", size=" + size +
                ", recordType='" + recordType + '\'' +
                ", user='" + user + '\'' +
                ", dateCreated=" + dateCreated +
                ", uuid='" + uuid + '\'' +
                ", jobId='" + jobId + '\'' +
                ", storageType='" + storageType + '\'' +
                ", storageReference='" + storageReference + '\'' +
                "} " + super.toString();
    }
}
