package rundeck.services.logging

/**
 * Represents a file produced by an execution producer
 */
interface ExecutionFile {
    File getLocalFile()

    /**
     * @return policy for deleting this file
     */
    ExecutionFileDeletePolicy getFileDeletePolicy()
}

enum ExecutionFileDeletePolicy {
    /**
     * Always delete the produced file (e.g. temp file usage)
     */
    ALWAYS,
    /**
     * Never delete the produced file (required for other use)
     */
    NEVER,
    /**
     * Delete only when it can be retrieved again later
     */
    WHEN_RETRIEVABLE
}

class ExecutionFileUtil {
    public static deleteExecutionFilePerPolicy(ExecutionFile file, boolean canRetrieve) {
        if (file.fileDeletePolicy == ExecutionFileDeletePolicy.ALWAYS) {
            file.localFile.delete()
        } else if (file.fileDeletePolicy == ExecutionFileDeletePolicy.WHEN_RETRIEVABLE && canRetrieve) {
            //todo: cache/delete after timeout
            file.localFile.deleteOnExit()
        }
    }
}

class ProducedExecutionFile implements ExecutionFile {
    File localFile
    ExecutionFileDeletePolicy fileDeletePolicy
}