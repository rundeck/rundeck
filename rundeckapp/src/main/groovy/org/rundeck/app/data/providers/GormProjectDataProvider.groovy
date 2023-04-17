package org.rundeck.app.data.providers

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.project.RdProject
import org.rundeck.app.data.providers.v1.project.RundeckProjectDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.Project
import rundeck.services.data.ProjectDataService

import javax.transaction.Transactional

@GrailsCompileStatic
@Slf4j
@Transactional
class GormProjectDataProvider implements RundeckProjectDataProvider {

    @Autowired
    ProjectDataService projectDataService

    @Autowired
    MessageSource messageSource

    @Override
    RdProject getData (final Serializable id) {
        RdProject project = projectDataService.get(id)
        return project ?: null
    }

    Long create(RdProject data) throws DataAccessException {
        Project project = new Project(description: data.getDescription(), name: data.getName())
        try {
            if (projectDataService.save(project)) {
                return project.getId()
            } else {
                log.warn(project.errors.allErrors.collect { messageSource.getMessage(it, null) }.join(","))
                throw new DataAccessException("Failed to create project: ${data.name}")
            }
        } catch (Exception e) {
            throw new DataAccessException("Failed to create project: ${data.name}: ${e}", e)
        }
    }

    @Override
    void update(final Serializable id, final RdProject data) throws DataAccessException {
        def project = projectDataService.get(id)
        if (!project) {
            throw new DataAccessException("Not found: project with ID: ${id}")
        }
        project.description = data.getDescription()
        try {
            projectDataService.save(project)
        } catch (Exception e) {
            throw new DataAccessException("Error: could not update project ${id}: ${e}", e)
        }
    }

    @Override
    void delete(final String projectName) throws DataAccessException {
        def project = projectDataService.getByName(projectName)
        if (!project) {
            throw new DataAccessException("Project does not exist: ${projectName}")
        }
        try {
            projectDataService.delete(project.getId())
        } catch (Exception e) {
            throw new DataAccessException("Project does not exist: ${projectName} : ${e}", e)
        }
    }

    @Override
    RdProject findByName (final String name) {
        RdProject project = projectDataService.getByName(name)
        return project ?: null
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<String> getFrameworkProjectNames() {
        def c = Project.createCriteria().list {
            ne('state', 'DELETED')
            projections {
                property "name"
            }
        }
    }

    @Override
    int countFrameworkProjects() {
        return projectDataService.count()
    }

    @Override
    boolean projectExists(String project) {
        projectDataService.countByName(project) > 0
    }

    @Override
    @CompileDynamic
    String getProjectDescription(String name){
        def c = Project.createCriteria()
        c.get {
            eq('name', name)
            ne('state', 'DELETED')
            projections {
                property "description"
            }
        }

    }

}
