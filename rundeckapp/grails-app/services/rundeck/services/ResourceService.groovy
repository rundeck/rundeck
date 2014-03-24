package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.AuthResourceTree
import com.dtolabs.rundeck.core.storage.ResourceUtil
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

class ResourceService {
    AuthResourceTree authRundeckResourceTree

    def hasPath(AuthContext context, String path) {
        authRundeckResourceTree.hasPath(context, PathUtil.asPath(path))
    }

    def hasResource(AuthContext context, String path) {
        authRundeckResourceTree.hasResource(context, PathUtil.asPath(path))
    }

    def getResource(AuthContext context, String path) {
        authRundeckResourceTree.getPath(context, PathUtil.asPath(path))
    }

    def updateResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        authRundeckResourceTree.updateResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                ResourceUtil.factory()))
    }
    def createResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        authRundeckResourceTree.createResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta, ResourceUtil.factory()))
    }

    def listDir(AuthContext context, String path) {
        authRundeckResourceTree.listDirectory(context, PathUtil.asPath(path))
    }

    def delResource(AuthContext context, String path) {
        authRundeckResourceTree.deleteResource(context, PathUtil.asPath(path))
    }
}
