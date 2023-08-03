/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.server.storage.NamespacedStorage
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.apache.commons.lang3.exception.ExceptionUtils
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.app.data.model.v1.storage.SimpleStorageBuilder
import org.rundeck.app.data.providers.v1.storage.StorageDataProvider
import org.rundeck.spi.data.DataAccessException
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.impl.ResourceBase

import java.util.regex.Pattern

/**
 * Implements StoragePlugin and provides DB storage for rundeck resources if configured to be used.
 */
class DbStorageService implements NamespacedStorage{
    static transactional = false
    StorageDataProvider storageDataProvider


    protected static Resource<ResourceMeta> loadDir(Path path) {
        new ResourceBase(path, null, true)
    }

    @CompileStatic
    protected static Resource<ResourceMeta> loadResource(RundeckStorage storage1) {

        new ResourceBase(storage1.getPath(),
                StorageUtil.withStream(lazyData(storage1), storage1.getStorageMeta()), false)

    }

    @CompileStatic
    protected static HasInputStream lazyData(RundeckStorage storage1) {
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

        if(PathUtil.isRoot(path)){
            return true
        }

        storageDataProvider.hasPath(ns, path)
    }

    boolean hasPath(String ns,String path) {
        return hasPath(ns,PathUtil.asPath(path))
    }

    @Override
    @Transactional(readOnly = true)
    boolean hasResource(String ns,Path path) {
        findResource(ns,path) !=null
    }

    boolean hasResource(String ns,String path) {
        return hasResource(ns,PathUtil.asPath(path))
    }

    @Override
    boolean hasDirectory(String ns,Path path) {
        if (PathUtil.isRoot(path)) {
            return true
        }

       storageDataProvider.hasDirectory(ns, path)
    }

    boolean hasDirectory(String ns,String path) {
        return hasDirectory(ns,PathUtil.asPath(path))
    }

    @Override
    @Transactional(readOnly = true)
    Resource<ResourceMeta> getPath(String ns,Path path) {
        RundeckStorage found = findResource(ns,path)
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
    @Transactional(readOnly = true)
    Resource<ResourceMeta> getResource(String ns,Path path) {
        RundeckStorage found = findResource(ns,path)
        if (!found) {
            throw StorageException.readException(path,"Not found")
        }
        return loadResource(found)
    }

    protected RundeckStorage findResource(String ns, Path path) {
        def dir, name
        (dir, name) = splitPath(path)
        storageDataProvider.findResource(ns, dir, name)
    }

    Resource<ResourceMeta> getResource(String ns,String path) {
        return getResource(ns,PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectoryResources(String ns,Path path) {
        storageDataProvider.findAllByNamespaceAndDir(ns ?: null,path.path).collect{ loadResource(it) }
    }

    Set<Resource<ResourceMeta>> listDirectoryResources(String ns,String path) {
        return listDirectoryResources(ns,PathUtil.asPath(path))
    }

    @Override
    Set<Resource<ResourceMeta>> listDirectory(String ns,Path path) {
        def foundset=new HashSet<String>()
        def pathkey= path.path ? (path.path + '/') : ''
        storageDataProvider.listDirectory(ns, path).collect {
            def parent = PathUtil.parentPath(it.path)
            String dir = parent ? parent.path: ''
            def m= dir =~ "^(${Pattern.quote(pathkey)}[^/]+)/?.*"
            if (dir == path.path) {
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
        def pathkey = path.path ? (path.path + '/') : ''
        storageDataProvider.listDirectorySubdirs(ns, path).collect {
            def parent = PathUtil.parentPath(it.path)
            String dir = parent ? parent.path: ''
            def m = dir =~ "^(${Pattern.quote(pathkey)}[^/]+)/?.*"
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
        RundeckStorage storage1 = findResource(ns,path)
        if (!storage1) {
            throw StorageException.deleteException(path, "Not found")
        }
        storageDataProvider.delete(storage1.getId())
        return true
    }

    boolean deleteResource(String ns,String path) {
        return deleteResource(ns,PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> createResource(String ns,Path path, ResourceMeta content) {
        if (path.path.contains('../')) {
            throw StorageException.createException(path, "Invalid path: ${path.path}")
        }
        if(findResource(ns,path)){
            throw StorageException.createException(path,"Exists")
        }
        def storage= saveStorage(null,content, ns,path,'create')

        return loadResource(storage)
    }

    @CompileStatic
    protected RundeckStorage saveStorage(RundeckStorage storage, ResourceMeta content,String namespace, Path path, String event) {
        def id = storage?.id
        def retry = true
        RundeckStorage saved=null;
        def data = content.getInputStream().bytes
        def saveStorage={
            try {
                if (id) {
                    storage = storageDataProvider.getData(id)
                } else {
                    storage = SimpleStorageBuilder.builder().build()
                }
                SimpleStorageBuilder storageBuilder = SimpleStorageBuilder.with(storage)
                storageBuilder.setNamespace( namespace ? namespace : null)
                storageBuilder.setPath(path)
                storageBuilder.setStorageMeta(content.meta)
                storageBuilder.setData( data)
                try {
                    if (id) {
                        storageDataProvider.update(id, storageBuilder, content.meta)

                    } else {
                        id = storageDataProvider.create(storageBuilder)
                    }
                    saved = storageDataProvider.getData(id)
                } catch (DataAccessException e) {
                    def causeMessage = ExceptionUtils.getRootCause(e).message
                    throw new StorageException("Failed to save content at path ${storageBuilder.path.getPath()}: validation error: " +
                            causeMessage,
                            StorageException.Event.valueOf(event.toUpperCase()),
                            path)
                }

                retry = false
                return true;
            } catch (org.springframework.dao.ConcurrencyFailureException e) {
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
                    saveStorage()
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
        if (path.path.contains('../')) {
            throw StorageException.updateException(path, "Invalid path: ${path.path}")
        }
        def storage = findResource(ns, path)
        if (!storage) {
            throw StorageException.updateException(path, "Not found")
        }
        storage = saveStorage(storage, content, ns, path, 'update')

        return loadResource(storage)
    }

    Resource<ResourceMeta> updateResource(String ns,String path, ResourceMeta content) {
        return updateResource(ns,PathUtil.asPath(path), content)
    }
}
