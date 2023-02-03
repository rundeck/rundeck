package rundeck.services.data

import grails.gorm.services.Service
import rundeck.Storage

@Service(Storage)
interface StorageDataService {
    Storage get(Serializable id)
    void delete(Serializable id)
    Storage save(Storage storage)
    List<Storage> findAllByNamespaceAndDir(String namespace, String dir, Map args)

}
