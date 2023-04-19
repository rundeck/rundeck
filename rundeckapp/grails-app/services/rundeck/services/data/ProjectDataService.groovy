package rundeck.services.data

import grails.gorm.services.Service
import grails.gorm.services.Where
import org.rundeck.app.data.model.v1.project.RdProject
import rundeck.Project

@Service(Project)
interface ProjectDataService {

    /**
     * Get an enabled project by id.
     */
    @Where({ id == pid && (state == null || state != RdProject.State.DISABLED) })
    Project getEnabledProject(Serializable pid)

    /**
     * Get an enabled project by name
     */
    @Where({ name == projectName && (state == null || state != RdProject.State.DISABLED) })
    Project getEnabledProject(String projectName)

    /**
     * Get a project by name regardless of its state. 
     */
    @Where({ name == projectName })
    Project getAnyProject(String projectName)

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
