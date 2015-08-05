package rundeck.services

/**
 * Created by greg on 7/29/15.
 */
interface StorageManagerListener {
    void resourceCreated(String path)
    void resourceDeleted(String path)
    void resourceModified(String path)

}