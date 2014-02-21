package rundeck.services

import com.dtolabs.rundeck.core.storage.ResourceTree
import com.dtolabs.rundeck.core.storage.ResourceUtil
import org.rundeck.storage.data.DataUtil

class ResourceService {
    ResourceTree rundeckResourceTree

    def hasResource(String path) {
        rundeckResourceTree.hasPath(path)
    }
    def getResource(String path) {
        rundeckResourceTree.getResource(path)
    }

    def putResource(String path, Map<String, String> meta, String data) {
        if (rundeckResourceTree.hasResource(path)) {
            rundeckResourceTree.updateResource(path, DataUtil.withText(data, meta, ResourceUtil.factory()))
        } else {
            rundeckResourceTree.createResource(path, DataUtil.withText(data, meta, ResourceUtil.factory()))
        }
    }
    def putResource(String path, Map<String, String> meta, InputStream data) {
        if (rundeckResourceTree.hasResource(path)) {
            rundeckResourceTree.updateResource(path, DataUtil.withStream(data, meta, ResourceUtil.factory()))
        } else {
            rundeckResourceTree.createResource(path, DataUtil.withStream(data, meta, ResourceUtil.factory()))
        }
    }

    def listDir(String path) {
        rundeckResourceTree.listDirectory(path)
    }

    def delResource(String path) {
        rundeckResourceTree.deleteResource(path)
    }
}
