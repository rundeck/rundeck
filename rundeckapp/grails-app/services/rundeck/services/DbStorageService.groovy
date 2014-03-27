package rundeck.services

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.ResourceUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import org.hibernate.Session
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.impl.ResourceBase
import rundeck.Storage

import java.util.regex.Pattern

/**
 * Implements StoragePlugin and provides DB storage for rundeck resources
 */
@Plugin(name = 'db', service = ServiceNameConstants.ResourceStorage)
@PluginDescription(title = 'DB Storage', description = 'Uses DB as storage layer.')
class DbStorageService implements StoragePlugin{

    protected static Resource<ResourceMeta> loadDir(Path path) {
        new ResourceBase(path, null, true)
    }

    protected static Resource<ResourceMeta> loadResource(Storage storage1) {
        new ResourceBase(PathUtil.asPath(storage1.path),
                ResourceUtil.withStream(lazyData(storage1), storage1.storageMeta), false)
    }

    protected static HasInputStream lazyData(Storage storage1) {
        new HasInputStream() {
            @Override
            InputStream getInputStream() throws IOException {
                new ByteArrayInputStream(storage1.data)
            }
        }
    }
    protected static List<String> splitPath(Path path1){
        def parent = PathUtil.parentPath(path1)
        [parent?parent.path:'',path1.name]
    }
    @Override
    boolean hasPath(Path path) {
        def dir,name
        (dir,name)=splitPath(path)
        if(path.path==''){
            return true
        }
        def c = Storage.createCriteria()
        c.get {
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

    @Override
    boolean hasPath(String path) {
        return hasPath(PathUtil.asPath(path))
    }

    @Override
    boolean hasResource(Path path) {
        findResource(path) !=null
    }

    @Override
    boolean hasResource(String path) {
        return hasResource(PathUtil.asPath(path))
    }

    @Override
    boolean hasDirectory(Path path) {
        def dir, name
        (dir, name) = splitPath(path)
        if (path.path == '') {
            return true
        }
        def c = Storage.createCriteria()
        c.get {
            or {
                eq('dir', path.path)
                like('dir', path.path + '/%')
            }
            projections {
                rowCount()
            }
        } > 0
    }

    @Override
    boolean hasDirectory(String path) {
        return hasDirectory(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPath(Path path) {
        Storage found = findResource(path)
        if(found){
            return loadResource(found)
        }else{
            //find dir
           if(hasDirectory(path)){
                return loadDir(path)
            }
        }
        throw StorageException.readException(path,"Not found")
    }

    @Override
    Resource<ResourceMeta> getPath(String path) {
        return getPath(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getResource(Path path) {
        Storage found = findResource(path)
        if (!found) {
            throw StorageException.readException(path,"Not found")
        }
        return loadResource(found)
    }

    protected Storage findResource(Path path) {
        def dir, name
        (dir, name) = splitPath(path)
        def found = Storage.findByDirAndName(dir, name)
        found
    }

    @Override
    Resource<ResourceMeta> getResource(String path) {
        return getResource(PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectoryResources(Path path) {
        Storage.findAllByDir(path.path,[sort:'name',order:'desc']).collect{ loadResource(it) }
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectoryResources(String path) {
        return listDirectoryResources(PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectory(Path path) {
        def foundset=new HashSet<String>()
        def c = Storage.createCriteria()
        def pathkey= path.path ? (path.path + '/') : ''
        c.list {
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

    @Override
    Set<Resource<ResourceMeta>> listDirectory(String path) {
        return listDirectory(PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectorySubdirs(Path path) {
        def foundset = new HashSet<String>()
        def c = Storage.createCriteria()
        def pathkey = path.path ? (path.path + '/') : ''
        c.list {
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

    @Override
    Set<Resource<ResourceMeta>> listDirectorySubdirs(String path) {
        return listDirectorySubdirs(PathUtil.asPath(path))
    }

    @Override
    boolean deleteResource(Path path) {
        Storage storage1 = findResource(path)
        if (!storage1) {
            throw StorageException.deleteException(path, "Not found")
        }
        storage1.delete(flush: true)
        return true
    }

    @Override
    boolean deleteResource(String path) {
        return deleteResource(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        if(findResource(path)){
            throw StorageException.createException(path,"Exists")
        }
        def storage = new Storage()
        saveStorage(storage,content, path)

        return loadResource(storage)
    }

    protected void saveStorage(Storage storage, ResourceMeta content, Path path) {
        storage.path = path.path
        storage.storageMeta = content.meta
        storage.data=content.readContent().bytes
        try {
            storage.save(flush: true)
        } catch (Exception e) {
            throw StorageException.createException(path, e.message, e)
        }
    }

    @Override
    Resource<ResourceMeta> createResource(String path, ResourceMeta content) {
        return createResource(PathUtil.asPath(path), content)
    }

    @Override
    Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        def storage = findResource(path)
        if (!storage) {
            throw StorageException.createException(path, "Not found")
        }
        saveStorage(storage,content,path)

        return loadResource(storage)
    }

    @Override
    Resource<ResourceMeta> updateResource(String path, ResourceMeta content) {
        return updateResource(PathUtil.asPath(path), content)
    }
}
