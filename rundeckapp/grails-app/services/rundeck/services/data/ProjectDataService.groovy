package rundeck.services.data

import grails.gorm.services.Service
import rundeck.Project

@Service(Project)
interface ProjectDataService {

    Project get(Serializable id)

    Project getByName(String name)

    Project save(Project project)

    void delete(Serializable id)

    int count()

    int countByName(String name)

}
