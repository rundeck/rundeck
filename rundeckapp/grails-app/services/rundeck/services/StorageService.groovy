package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.AuthStorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.server.plugins.storage.KeyStorageTree
import com.dtolabs.rundeck.server.plugins.storage.KeyStorageUtil
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

/**
 * Service layer access to the authorized storage
 */
class StorageService {
    AuthStorageTree authRundeckStorageTree

    def hasPath(AuthContext context, String path) {
        authRundeckStorageTree.hasPath(context, PathUtil.asPath(path))
    }

    def hasResource(AuthContext context, String path) {
        authRundeckStorageTree.hasResource(context, PathUtil.asPath(path))
    }

    def getResource(AuthContext context, String path) {
        authRundeckStorageTree.getPath(context, PathUtil.asPath(path))
    }

    def updateResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        authRundeckStorageTree.updateResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                StorageUtil.factory()))
    }
    def createResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        authRundeckStorageTree.createResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta, StorageUtil.factory()))
    }

    def listDir(AuthContext context, String path) {
        authRundeckStorageTree.listDirectory(context, PathUtil.asPath(path))
    }

    def delResource(AuthContext context, String path) {
        authRundeckStorageTree.deleteResource(context, PathUtil.asPath(path))
    }
    /**
     * Return a tree using the authorization context
     * @param ctx auth context
     * @return StorageTree
     */
    def KeyStorageTree storageTreeWithContext(AuthContext ctx) {
        KeyStorageUtil.keyStorageWrapper StorageUtil.resolvedTree(ctx, authRundeckStorageTree)
    }
}
