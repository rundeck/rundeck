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
    /**
     * Name used for the file record (e.g. option name)
     */
    String recordName
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
    String serverNodeUUID
    String sha
    String jobId
    String storageType // storage plugin type "tmpdir", "storage"
    String storageReference // path in storage facility, or temp dir, depends on backend plugin
    String storageMeta // metadata...?
    Execution execution
    static constraints = {
        fileName(nullable: true, maxSize: 1024)
        size(nullable: false)
        recordType(nullable: false, maxSize: 255)
        fileState(nullable: false, maxSize: 255, inList: [STATE_TEMP, STATE_DELETED, STATE_EXPIRED, STATE_RETAINED])
        user(nullable: false, maxSize: 255)
        expirationDate(nullable: true)
        uuid(nullable: false)
        serverNodeUUID(nullable: true)
        sha(nullable: false, size: 64..64)
        jobId(nullable: false)
        recordName(nullable: true, maxSize: 255)
        storageType(nullable: false, maxSize: 255)
        storageReference(nullable: false)
        storageMeta(nullable: true)
        execution(nullable: true)
    }

    static mapping = {
        storageMeta(type: 'text')
        storageReference(type: 'text')
        serverNodeUUID(type: 'text')
    }

    boolean stateIsExpired() {
        fileState == STATE_EXPIRED
    }

    boolean stateIsTemp() {
        fileState == STATE_TEMP
    }

    boolean stateIsRetained() {
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
    public boolean canBecomeRetained() {
        state(STATE_RETAINED,true)
    }

    public void stateDeleted() {
        state(STATE_DELETED)
    }

    public boolean state(String toState, boolean test=false) {
        changeState(STATES[toState], toState,test)
    }

    private boolean changeState(List<String> fromStates, String toState, boolean test=false) {
        if (!(fromStates && fromStates.contains(fileState) || fileState == toState)) {
            if(test){
                return false
            }
            throw new IllegalStateException("Cannot change to '$toState' state from $fileState")
        }
        if(test){
            return true
        }
        fileState = toState
        true
    }

    @Override
    public String toString() {
        return "rundeck.JobFileRecord{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", size=" + size +
                ", recordType='" + recordType + '\'' +
                ", recordName='" + recordName + '\'' +
                ", user='" + user + '\'' +
                ", dateCreated=" + dateCreated +
                ", expirationDate=" + expirationDate +
                ", fileState='" + fileState + '\'' +
                ", uuid='" + uuid + '\'' +
                ", serverNodeUUID='" + serverNodeUUID + '\'' +
                ", jobId='" + jobId + '\'' +
                ", execution.id=" + execution?.id +
                "} " + super.toString();
    }

    /**
     * Exported form for api
     */

    Map exportMap() {
        [
                id        : uuid,
                execId    : execution?.id,
                optionName: recordName
        ] + properties.subMap([
                'jobId',
                'fileName',
                'sha',
                'size',
                'dateCreated',
                'expirationDate',
                'user',
                'fileState',
                'serverNodeUUID',
        ]
        )
    }

    def toMap() {
        [execId: execution?.id] + properties.subMap(
                [
                        'uuid',
                        'recordName',
                        'jobId',
                        'fileName',
                        'sha',
                        'size',
                        'dateCreated',
                        'lastUpdated',
                        'expirationDate',
                        'user',
                        'fileState',
                        'storageReference',
                        'storageType',
                        'storageMeta',
                        'serverNodeUUID',
                        'recordType'
                ]
        )
    }

    static JobFileRecord fromMap(Map map) {
        new JobFileRecord(
                uuid: map.uuid,
                recordName: map.recordName,
                jobId: map.jobId,
                fileName: map.fileName,
                sha: map.sha,
                size: map.size,
                dateCreated: map.dateCreated,
                lastUpdated: map.lastUpdated,
                expirationDate: map.expirationDate,
                user: map.user,
                fileState: map.fileState,
                storageReference: map.storageReference,
                storageType: map.storageType,
                storageMeta: map.storageMeta,
                serverNodeUUID: map.serverNodeUUID,
                execution: map.execution,
                recordType: map.recordType
        )
    }
}
