package rundeck.services

import com.dtolabs.rundeck.core.storage.AuthStorageTree

/**
 * Service layer access to the authorized storage
 */
class FileStorageService extends StorageService{
    AuthStorageTree authFileRundeckStorageTree

    protected AuthStorageTree getServiceTree() {
        return authFileRundeckStorageTree
    }
}
