package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.AuthStorageManager
import grails.gorm.transactions.Transactional
import org.rundeck.storage.data.MemoryTree

@Transactional
class AuthStorageTreeService implements AuthStorageManager{

    def AuthStorageManager rundeckAuthStorageManager

    StorageTree storageTreeWrapper(AuthContext authContext) {
        return rundeckAuthStorageManager.storageTreeWrapper(authContext)
    }
}

class RundeckAuthStorageManager implements AuthStorageManager {

    @Override
    StorageTree storageTreeWrapper(AuthContext authContext) {
        return StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>())
    }
}
