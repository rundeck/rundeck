package rundeck.services.data


import grails.gorm.services.Service
import rundeck.Project

@Service(Project)
interface ProjectDataService {

    /**
     * Get a project by id.
     */
    Project get(Serializable id)

    /**
     * Get a project by name regardless of its state. 
     */
    Project getByName(String name)
    
    /**
     * Saves a project 
     */
    Project save(Project project)

    /**
     * Deletes a project by id. 
     */
    void delete(Serializable id)

    /**
     * Counts the number of enabled projects. 
     */
    int count()

    /**
     * Counts the number of enabled projects by name. 
     */
    int countByName(String projectName)

}
