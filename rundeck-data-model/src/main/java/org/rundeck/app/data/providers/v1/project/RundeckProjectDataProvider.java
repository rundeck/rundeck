package org.rundeck.app.data.providers.v1.project;

import org.rundeck.app.data.model.v1.project.RdProject;
import org.rundeck.app.data.providers.v1.DataProvider;

import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;
import java.util.Collection;


public interface RundeckProjectDataProvider extends DataProvider {
    /**
     * Retrieves a RundeckProject based on the id provided.
     *
     * @param id of the RundeckProject, format Serializable
     * @return RundeckProject if found, otherwise null
     */
    RdProject getData(Serializable id);

    /**
     * Creates a RundeckProject
     *
     * @param data RundeckProject attributes
     *
     * @return id of the created RundeckProject
     * @throws DataAccessException on error
     */
    Long create(RdProject data) throws DataAccessException;

    /**
     * Updates a RundeckProject with the supplied attributes
     *
     * @param data RundeckProject attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(Serializable id, RdProject data) throws DataAccessException;

    /**
     * Removes a RundeckProject
     *
     * @param name of RundeckProject
     * @throws DataAccessException on error
     */
    void delete(final String name) throws DataAccessException;

    /**
     * Finds a RundeckProject by name
     *
     * @param name of RundeckProject
     * @return RundeckProject if found, otherwise null
     */
    RdProject findByName(String name);

    /** Finds a project by name and state
     * 
     * @param name Project name
     * @param state Project state from @{@link RdProject.State}
     * @return Project instance or null if doesn't exists.
     */
    RdProject findByNameAndState(String name, RdProject.State state);

    /**
     * Rerieves all enabled project names
     *
     * @return Collection of project names
     */
    Collection<String> getFrameworkProjectNames();

    /**
     * Rerieves all enabled project names
     *
     * @return Collection of project names
     */
    Collection<String> getFrameworkProjectNamesByState(RdProject.State state);

    /**
     * Retrieves the number of enabled RundeckProjects
     *
     * @return Count of RundeckProjects
     */
    int countFrameworkProjects();

    /**
     * Checks if RundeckProject exists and is enabled
     *
     * @param name of RundeckProject
     * @return true if RundeckProject is found, otherwise false
     */
    boolean projectExists(final String name);

    /**
     * Retrieves the description of an enabled Rundeck Project
     *
     * @param name of RundeckProject
     * @return Description of the RundeckProject if found, otherwise null
     */
    String getProjectDescription(final String name);

}
