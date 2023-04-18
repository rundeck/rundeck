package rundeck.services.data

import grails.gorm.services.Service
import grails.gorm.services.Where
import org.rundeck.app.data.model.v1.project.RdProject
import rundeck.Project

@Service(Project)
interface ProjectDataService {

    @Where({ id == pid && (state == null || state != RdProject.State.DISABLED) })
    Project get(Serializable pid)

    @Where({ name == projectName && (state == null || state != RdProject.State.DISABLED) })
    Project getByName(String projectName)

    Project save(Project project)

    void delete(Serializable id)

    @Where({ state == null || state != RdProject.State.DISABLED})
    int count()
    
    @Where({ name == projectName && (state == null || state != RdProject.State.DISABLED) })
    int countByName(String projectName)

}
