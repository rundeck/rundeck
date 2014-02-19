package rundeck.services

import us.vario.greg.lct.data.DataUtil
import us.vario.greg.lct.model.Tree

class ResourceService {
    Tree rundeckResourceTree

    def hasResource(String path) {
        rundeckResourceTree.hasPath(path)
    }
    def getResource(String path) {
        rundeckResourceTree.getResource(path)
    }

    def putResource(String path, Map<String, String> meta, String data) {
        if (rundeckResourceTree.hasResource(path)) {
            rundeckResourceTree.updateResource(path, DataUtil.withData(data, meta))
        } else {
            rundeckResourceTree.createResource(path, DataUtil.withData(data, meta))
        }
    }
    def putResource(String path, Map<String, String> meta, InputStream data) {
        if (rundeckResourceTree.hasResource(path)) {
            rundeckResourceTree.updateResource(path, DataUtil.withData(data, meta))
        } else {
            rundeckResourceTree.createResource(path, DataUtil.withData(data, meta))
        }
    }

    def listDir(String path) {
        rundeckResourceTree.listDirectory(path)
    }

    def delResource(String path) {
        rundeckResourceTree.deleteResource(path)
    }
}
