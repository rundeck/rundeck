package rundeck.services

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.server.storage.NamespacedStorage
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.impl.ResourceBase
import org.springframework.dao.OptimisticLockingFailureException
import rundeck.Storage

import java.util.regex.Pattern

/**
 * Implements StoragePlugin and provides DB storage for rundeck resources if configured to be used.
 */
class DbStorageService implements NamespacedStorage{
    static transactional = false

    protected static Resource<ResourceMeta> loadDir(Path path) {
        new ResourceBase(path, null, true)
    }

    protected static Resource<ResourceMeta> loadResource(Storage storage1) {
        new ResourceBase(PathUtil.asPath(storage1.path),
                StorageUtil.withStream(lazyData(storage1), storage1.storageMeta), false)
    }

    protected static HasInputStream lazyData(Storage storage1) {
        new HasInputStream() {
            @Override
            InputStream getInputStream() throws IOException {
                new ByteArrayInputStream(storage1.data)
            }

            @Override
            long writeContent(OutputStream outputStream) throws IOException {
                def data = storage1.data
                long len = (long) data.length
                outputStream.write(data)
                return len
            }
        }
    }
    protected static List<String> splitPath(Path path1){
        def parent = PathUtil.parentPath(path1)
        [parent?parent.path:'',path1.name]
    }
    @Override
    boolean hasPath(String ns,Path path) {
        def dir,name
        (dir,name)=splitPath(path)
        if(path.path==''){
            return true
        }
        def c = Storage.createCriteria()
        c.get {
            if (ns) {
                eq('namespace', ns)
            } else {
                isNull('namespace')
            }
            or {
                and{
                    eq('name', name)
                    eq('dir', dir)
                }
                or {
                    eq('dir', path.path)
                    like('dir', path.path + '/%')
                }
            }
            projections {
                rowCount()
            }
        } > 0
    }

    boolean hasPath(String ns,String path) {
        return hasPath(ns,PathUtil.asPath(path))
    }

    @Override
    boolean hasResource(String ns,Path path) {
        findResource(ns,path) !=null
    }

    boolean hasResource(String ns,String path) {
        return hasResource(ns,PathUtil.asPath(path))
    }

    @Override
    boolean hasDirectory(String ns,Path path) {
        def dir, name
        (dir, name) = splitPath(path)
        if (path.path == '') {
            return true
        }
        def c = Storage.createCriteria()
        c.get {
            if (ns) {
                eq('namespace', ns)
            } else {
                isNull('namespace')
            }
            or {
                eq('dir', path.path)
                like('dir', path.path + '/%')
            }
            projections {
                rowCount()
            }
        } > 0
    }

    boolean hasDirectory(String ns,String path) {
        return hasDirectory(ns,PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPath(String ns,Path path) {
        Storage found = findResource(ns,path)
        if(found){
            return loadResource(found)
        }else{
            //find dir
           if(hasDirectory(ns,path)){
                return loadDir(path)
            }
        }
        throw StorageException.readException(path,"Not found")
    }

    Resource<ResourceMeta> getPath(String ns,String path) {
        return getPath(ns,PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getResource(String ns,Path path) {
        Storage found = findResource(ns,path)
        if (!found) {
            throw StorageException.readException(path,"Not found")
        }
        return loadResource(found)
    }

    protected Storage findResource(String ns, Path path) {
        def dir, name
        (dir, name) = splitPath(path)
        def found = Storage.findByNamespaceAndDirAndName(ns?:null,dir, name)
        found
    }

    Resource<ResourceMeta> getResource(String ns,String path) {
        return getResource(ns,PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectoryResources(String ns,Path path) {
        Storage.findAllByNamespaceAndDir(ns ?: null,path.path,[sort:'name',order:'desc']).collect{ loadResource(it) }
    }

    Set<Resource<ResourceMeta>> listDirectoryResources(String ns,String path) {
        return listDirectoryResources(ns,PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectory(String ns,Path path) {
        def foundset=new HashSet<String>()
        def c = Storage.createCriteria()
        def pathkey= path.path ? (path.path + '/') : ''
        c.list {
            if(ns){
                eq('namespace',ns)
            }else{
                isNull('namespace')
            }
            or {
                eq('dir', path.path)
                like('dir', pathkey+'%')
            }
            order("name", "desc")
        }.collect {
            def m= it.dir =~ "^(${Pattern.quote(pathkey)}[^/]+)/?.*"
            if (it.dir == path.path) {
                return loadResource(it)
            } else if (m.matches() && !foundset.contains(m[0][1])) {
                foundset<<m[0][1]
                return loadDir(PathUtil.asPath(m[0][1]))
            }
            null
        }.findAll{it}
    }

    Set<Resource<ResourceMeta>> listDirectory(String ns,String path) {
        return listDirectory(ns,PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectorySubdirs(String ns,Path path) {
        def foundset = new HashSet<String>()
        def c = Storage.createCriteria()
        def pathkey = path.path ? (path.path + '/') : ''
        c.list {
            if (ns) {
                eq('namespace', ns)
            } else {
                isNull('namespace')
            }
            like('dir', pathkey + '%')
            order("name", "desc")
        }.collect {
            def m = it.dir =~ "^(${Pattern.quote(pathkey)}[^/]+)/?.*"
            if (m.matches() && !foundset.contains(m[0][1])) {
                foundset << m[0][1]
                return loadDir(PathUtil.asPath(m[0][1]))
            }
            null
        }.findAll { it }
    }

    Set<Resource<ResourceMeta>> listDirectorySubdirs(String ns,String path) {
        return listDirectorySubdirs(ns,PathUtil.asPath(path))
    }

    @Override
    boolean deleteResource(String ns,Path path) {
        Storage storage1 = findResource(ns,path)
        if (!storage1) {
            throw StorageException.deleteException(path, "Not found")
        }
        storage1.delete(flush: true)
        return true
    }

    boolean deleteResource(String ns,String path) {
        return deleteResource(ns,PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> createResource(String ns,Path path, ResourceMeta content) {
        if(findResource(ns,path)){
            throw StorageException.createException(path,"Exists")
        }
        def storage= saveStorage(null,content, ns,path,'create')

        return loadResource(storage)
    }

    protected Storage saveStorage(Storage storage, ResourceMeta content,String namespace, Path path, String event) {
        def id = storage?.id
        def retry = true
        Storage saved=null;
        def data = content.getInputStream().bytes
        def saveStorage={
            try {
                if (id) {
                    storage = Storage.get(id)
                } else {
                    storage = new Storage()
                }

                storage.namespace = namespace ? namespace : null
                storage.path = path.path
                Map<String, String> newdata = storage.storageMeta?:[:]
                storage.storageMeta = newdata + content.meta
                storage.data = data
                saved = storage.save(flush: true)
                if (!saved) {
                    throw new StorageException("Failed to save content at path ${path}: validation error: " +
                            storage.errors.allErrors.collect { it.toString() }.join("; "),
                            StorageException.Event.valueOf(event.toUpperCase()),
                            path)
                }

                retry = false
                return true;
            } catch (OptimisticLockingFailureException e) {
                if (!retry) {
                    throw new StorageException("Failed to save content at path ${path}: content was modified", e,
                            StorageException.Event.valueOf(event.toUpperCase()),
                            path)
                } else {
                    log.warn("saveStorage optimistic locking failure for ${path}, retrying...")
                    Thread.sleep(1000)
                }
            }
            return false;
        }
        try{
            if(!saveStorage()){
                while(retry){
                    Storage.withNewSession {session->
                        saveStorage()
                    }
                }
            }
        } catch (Exception e) {
            throw new StorageException(e.class.name+': '+e.message, e, StorageException.Event.valueOf(event.toUpperCase()), path)
        }
        return saved
    }

    Resource<ResourceMeta> createResource(String ns,String path, ResourceMeta content) {
        return createResource(ns,PathUtil.asPath(path), content)
    }

    @Override
    Resource<ResourceMeta> updateResource(String ns,Path path, ResourceMeta content) {
        def storage = findResource(ns,path)
        if (!storage) {
            throw StorageException.createException(path, "Not found")
        }
        storage=saveStorage(storage,content,ns,path,'update')

        return loadResource(storage)
    }

    Resource<ResourceMeta> updateResource(String ns,String path, ResourceMeta content) {
        return updateResource(ns,PathUtil.asPath(path), content)
    }
}
