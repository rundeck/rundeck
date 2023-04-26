package rundeck.services.data

import grails.gorm.services.Service
import grails.gorm.services.Where
import org.rundeck.app.data.model.v1.project.RdProject
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
    @Where({ state == null || state != RdProject.State.DISABLED })
    int countEnabled()

    /**
     * Counts the number of enabled projects by name. 
     */
    @Where({ name == projectName && (state == null || state != RdProject.State.DISABLED) })
    int countEnabledByName(String projectName)

}
