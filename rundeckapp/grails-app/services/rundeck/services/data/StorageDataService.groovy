package rundeck.services.data

import grails.gorm.services.Service
import rundeck.Storage

@Service(Storage)
interface StorageDataService {
    Storage get(Serializable id)
    void delete(Serializable id)
    Storage save(Storage storage)
    Storage findByNamespaceAndDirAndName(String namespace, String dir, String name)
    List<Storage> findAllByNamespaceAndDir(String namespace, String dir, Map args)

}
