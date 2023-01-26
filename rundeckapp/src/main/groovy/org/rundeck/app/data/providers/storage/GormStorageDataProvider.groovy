package org.rundeck.app.data.providers.storage

import grails.compiler.GrailsCompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.app.data.providers.v1.storage.StorageDataProvider
import org.rundeck.spi.data.DataAccessException
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.Storage
import rundeck.services.data.StorageDataService

import javax.transaction.Transactional

@GrailsCompileStatic
@Slf4j
@Transactional
class GormStorageDataProvider implements StorageDataProvider {
    @Autowired
    StorageDataService storageDataService
    @Autowired
    MessageSource messageSource

    @Override
    RundeckStorage getData (final Serializable id) {
        RundeckStorage storage = storageDataService.get(id)
        return storage ?: null
    }

    @Override
    Long create(RundeckStorage data, Map<String, String> metadata) throws DataAccessException {

        def parent = PathUtil.parentPath(data.getPath())
        String dir = parent?parent.path:''
        String name = data.getPath().name
        Storage s = new Storage(namespace: data.getNamespace(),
                                name: name,
                                dir: dir,
                                data: data.getData())
        s.setStorageMeta(metadata)
        try {
            if (storageDataService.save(s)) {
                return s.getId()
            } else {
                log.warn(s.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
                throw new DataAccessException(s.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
            }
        } catch (Exception e) {
            throw new DataAccessException("Failed to create storage: ${name}: ${e}", e)
        }
    }

    @Override
    void update(final Serializable id, final RundeckStorage data, Map<String, String> metadata) throws DataAccessException {
        Storage storage = storageDataService.get(id)
        def parent = PathUtil.parentPath(data.getPath())
        String dir = parent?parent.path:''
        String name = data.getPath().name
        if (!storage) {
            throw new DataAccessException("Not found: storage with ID: ${id}")
        }
        Map<String, String> newdata = storage.storageMeta?:[:]
        storage.storageMeta = newdata + metadata

        storage.namespace = data.getNamespace()
        storage.name = name
        storage.dir = dir
        storage.data= data.getData()
        storage.lastUpdated = new Date()

         try {
             storageDataService.save(storage)
        } catch (Exception e) {
            throw new DataAccessException("Error: could not update project ${id}: ${e}", e)
        }
    }

    @Override
    void delete(final Serializable id) throws DataAccessException {
        def storage = storageDataService.get(id)
        if (!storage) {
            throw new DataAccessException("Not found: storage with ID: ${id}")
        }
        try {
            storage.delete(flush:true)
         } catch (Exception e) {
            throw new DataAccessException("Could not delete storage ${id}: ${e}", e)
        }
    }
    public List<RundeckStorage> findAllByNamespaceAndDir(String namespace, String path) {
        storageDataService.findAllByNamespaceAndDir(namespace, path, [sort:'name',order:'desc']) as List<RundeckStorage>
    }

    @Override
    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    public RundeckStorage findResource(String ns, String dir, String name) {
        def found = Storage.createCriteria().get(){
            if(ns){
                eq('namespace', ns)
            }else{
                isNull('namespace')
            }
            if(dir){
                eq('dir', dir)
            }else{
                or{
                    eq('dir', '')
                    isNull('dir')
                }
            }
            eq('name', name)
            cache(false)
        }
        found?.refresh()
        found
    }

    @Override
    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    boolean hasPath(String ns, Path path) {
        def parent = PathUtil.parentPath(path)
        String dir = parent?parent.path:''
        String name = path.name

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


    @Override
    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    boolean hasDirectory(String ns,Path path) {
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

    @Override
    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    List<RundeckStorage> listDirectory(String ns, Path path) {
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
        }
    }

    @Override
    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    List<RundeckStorage> listDirectorySubdirs(String ns,Path path) {
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
        }
    }


}
