package rundeck.services.data

import grails.gorm.services.Service
import grails.gorm.services.Where
import rundeck.Project

@Service(Project)
interface ProjectDataService {

    @Where({ state == null || state != 'DELETED'})
    Project get(Serializable id)

    @Where({ state == null || state != 'DELETED'})
    Project getByName(String name)

    Project save(Project project)

    void delete(Serializable id)

    @Where({ state == null || state != 'DELETED'})
    int count()
    
    @Where({ state == null || state != 'DELETED'})
    int countByName(String name)

}
