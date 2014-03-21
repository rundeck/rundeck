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

    def putResource(AuthContext context, String path, Map<String, String> meta, String data) {
        if (authRundeckResourceTree.hasResource(context, PathUtil.asPath(path))) {
            authRundeckResourceTree.updateResource(context, PathUtil.asPath(path), DataUtil.withText(data, meta,
                    ResourceUtil.factory()))
        } else {
            authRundeckResourceTree.createResource(context, PathUtil.asPath(path), DataUtil.withText(data, meta,
                    ResourceUtil.factory()))
        }
    }

    def putResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        if (authRundeckResourceTree.hasResource(context, PathUtil.asPath(path))) {
            authRundeckResourceTree.updateResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                    ResourceUtil.factory()))
        } else {
            authRundeckResourceTree.createResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                    ResourceUtil.factory()))
        }
    }

    def listDir(AuthContext context, String path) {
        authRundeckResourceTree.listDirectory(context, PathUtil.asPath(path))
    }

    def delResource(AuthContext context, String path) {
        authRundeckResourceTree.deleteResource(context, PathUtil.asPath(path))
    }
}
