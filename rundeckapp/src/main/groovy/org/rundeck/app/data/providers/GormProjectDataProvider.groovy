package org.rundeck.app.data.providers

import grails.compiler.GrailsCompileStatic
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
    RdProject getData(final Serializable id) {
        RdProject project = projectDataService.get(id)
        return project ?: null
    }

    Long create(RdProject data) throws DataAccessException {
        Project project = new Project(
                name: data.getName(),
                description: data.getDescription(),
                state: data.getState())
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
        project.state = data.getState()
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
    RdProject findByName(final String name) {
        RdProject project = projectDataService.getByName(name)
        return project ?: null
    }

    @Override
    RdProject findByNameAndState(String name, RdProject.State state) {
        RdProject project = projectDataService.getByName(name)
        if (!project) return null

        def pstate = project.state ?: RdProject.State.ENABLED
        def qstate = state ?: RdProject.State.ENABLED

        return (pstate == qstate) ? project : null
    }

    @Override
    boolean projectExists(String project) {
        projectDataService.countByName(project) > 0
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<String> getFrameworkProjectNames() {
        return Project.createCriteria().list {
            projections {
                property "name"
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<String> getFrameworkProjectNamesByState(RdProject.State qstate) {
        Objects.requireNonNull(qstate, "null qstate")
        def namelist = []

        namelist.addAll(Project.where {
            state == qstate
        }.property("name").list())

        if(qstate == RdProject.State.ENABLED) {
            namelist.addAll(Project.where {
                state == null
            }.property("name").list())
        }

        return namelist
    }

    @Override
    int countFrameworkProjects() {
        return projectDataService.count()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    int countFrameworkProjectsByState(RdProject.State qstate) {
        Objects.requireNonNull(qstate, "null qstate")
        int count = 0

        count += Project.where {
            state == qstate
        }.count()

        if(qstate == RdProject.State.ENABLED) {
            count += Project.where {
                state == null
            }.count()
        }

        return count
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    String getProjectDescription(String qname) {
        return Project.where {
            name == qname
        }.property("description").get()
    }

}
