package rundeck.services.logging

import rundeck.Execution

/**
 * Produces files that need to be stored for an execution
 */
interface ExecutionFileProducer {
    /**
     * @return the filetype string
     */
    String getExecutionFileType()

    /**
     * @param e execution
     * @return the file to store
     */
    ExecutionFile produceStorageFileForExecution(Execution e)
}