package rundeck.services.data

import grails.gorm.services.Service
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
    int count()

    /**
     * Counts the number of enabled projects by name. 
     */
    int countByName(String projectName)


    /**
     * Get all available project names
     * @return
     */
    List<String> findProjectName()

    /**
     * Get all available project names by state
     * @return
     */
    List<String> findProjectName(RdProject.State state)

    /**
     * Get the description of a project.
     * @return
     */
    String findProjectDescription(String name)
    
    

}
