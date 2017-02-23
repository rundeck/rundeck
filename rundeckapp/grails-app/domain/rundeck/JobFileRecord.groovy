package rundeck

class JobFileRecord {
    /**
     * Original file name
     */
    public static final String STATE_TEMP = 'temp'
    public static final String STATE_DELETED = 'deleted'
    public static final String STATE_EXPIRED = 'expired'
    public static final String STATE_RETAINED = 'retained'
    /**
     * State changes [to: [from,from]]
     */
    public static final Map<String, List<String>> STATES = [
            (STATE_EXPIRED) : [STATE_TEMP],
            (STATE_RETAINED): [STATE_TEMP],
            (STATE_DELETED) : [STATE_TEMP, STATE_RETAINED]
    ]
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
    /**
     * State of the file,
     * 'temp' = The file is uploaded but not yet used, -> expired, retained, deleted
     * 'deleted' = The file was deleted
     * 'expired' = The file was removed after expiration
     * 'retained' = The file is available and was retained, -> deleted
     */
    String fileState
    String uuid
    String sha
    String jobId
    String storageType // storage plugin type "tmpdir", "storage"
    String storageReference // path in storage facility, or temp dir, depends on backend plugin
    String storageMeta // metadata...?
    Execution execution
    static constraints = {
        fileName(nullable: false, maxSize: 1024)
        size(nullable: false)
        recordType(nullable: false, maxSize: 255)
        fileState(nullable: false, maxSize: 255, inList: [STATE_TEMP, STATE_DELETED, STATE_EXPIRED, STATE_RETAINED])
        user(nullable: false, maxSize: 255)
        expirationDate(nullable: true)
        uuid(nullable: false)
        sha(nullable: false, size: 64..64)
        jobId(nullable: false)
        storageType(nullable: false, maxSize: 255)
        storageReference(nullable: false, maxSize: 255)
        storageMeta(nullable: true)
        execution(nullable: true)
    }

    static mapping = {
        storageMeta(type: 'text')
    }

    boolean stateIsExpired() {
        fileState == STATE_EXPIRED
    }

    boolean stateIsTemp() {
        fileState == STATE_TEMP
    }

    boolean stateIsAvailable() {
        fileState == STATE_RETAINED
    }

    boolean stateIsDeleted() {
        fileState == STATE_DELETED
    }

    public void stateExpired() {
        state(STATE_EXPIRED)
    }

    public void stateRetained() {
        state(STATE_RETAINED)
    }

    public void stateDeleted() {
        state(STATE_DELETED)
    }

    public void state(String toState) {
        changeState(STATES[toState], toState)
    }

    private void changeState(List<String> fromStates, String toState) {
        if (!fromStates || !fromStates.contains(fileState)) {
            throw new IllegalStateException("Cannot change to '$toState' state from $fileState")
        }
        fileState = toState
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
