package rundeck.services

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import grails.transaction.Transactional
import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.data.DataUtil

/**
 * Interact with configuration storage
 */
@Transactional
class ConfigStorageService implements StorageManager {

    def StorageTree rundeckConfigStorageTree
    def List<StorageManagerListener> listeners=Collections.synchronizedList([])

    /**
     * @return storage
     */
    def StorageTree getStorage() {
        rundeckConfigStorageTree
    }

    @Override
    void addListener(final StorageManagerListener listener) {
        listeners << listener
    }

    @Override
    void removeListener(final StorageManagerListener listener) {
        listeners.remove(listener)
    }

    boolean existsFileResource(String path) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        return getStorage().hasResource(storagePath)
    }

    boolean existsDirResource(String path) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        return getStorage().hasDirectory(storagePath)
    }
    /**
     * List the full paths of file resources in the directory at the given path
     * @param path path directory path
     * @param pattern pattern match
     * @return
     */
    List<String> listDirPaths(String path, String pattern = null) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        def resources = getStorage().listDirectory(storagePath)
        def outprefix = path.endsWith('/') ? path.substring(0, path.length() - 1) : path
        resources.collect { Resource<ResourceMeta> res ->
            def pathName = res.path.name + (res.isDirectory() ? '/' : '')
            (!pattern || pathName ==~ pattern) ? (outprefix + '/' + pathName) : null
        }.findAll { it }
    }

    /**
     * Return config file contents
     * @param path
     * @return
     */
    Resource<ResourceMeta> getFileResource(String path) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        if (!getStorage().hasResource(storagePath)) {
            return null
        }
        getStorage().getResource(storagePath)
    }

    long loadFileResource(String path, OutputStream output) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        def resource = getStorage().getResource(storagePath)
        Streams.copy(resource.contents.inputStream, output, true)
    }
    /**
     * Update existing resource, fails if it does not exist
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> updateFileResource(String path, InputStream input, Map<String, String> meta) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        def res=getStorage().
                updateResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
        listeners*.resourceModified(path)
        res
    }
    /**
     * Create new resource, fails if it exists
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> createFileResource(String path, InputStream input, Map<String, String> meta) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        def res=getStorage().
                createResource(storagePath, DataUtil.withStream(input, meta, StorageUtil.factory()))
        listeners*.resourceCreated(path)
        res
    }
    /**
     * Write to a resource, create if it does not exist
     * @param projectName project
     * @param path path
     * @param input stream
     * @param meta metadata
     * @return resource
     */
    Resource<ResourceMeta> writeFileResource(String path, InputStream input, Map<String, String> meta) {
        def storagePath = (path.startsWith("/") ? path : "/${path}")
        if (!getStorage().hasResource(storagePath)) {
            createFileResource(path, input, meta)
        } else {
            updateFileResource(path, input, meta)
        }
    }

    /**
     * delete a resource
     * @param path path
     * @return true if file was deleted or does not exist
     */
    boolean deleteFileResource(String path) {
        def storagePath =  (path.startsWith("/")?path:"/${path}")
        if (!getStorage().hasResource(storagePath)) {
            return true
        }else{
            def res= getStorage().deleteResource(storagePath)
            listeners*.resourceDeleted(path)
            return res
        }
    }

    /**
     * Recursively delete all files from the root path
     * @param root root path
     * @return true if successful
     */
    boolean deleteAllFileResources(String root) {
        def storagePath = root
        return StorageUtil.deletePathRecursive(getStorage(), PathUtil.asPath(storagePath))
    }
}
