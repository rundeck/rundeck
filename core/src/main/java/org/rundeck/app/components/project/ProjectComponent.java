package org.rundeck.app.components.project;

import org.rundeck.core.projects.ProjectDataExporter;
import org.rundeck.core.projects.ProjectDataImporter;

/**
 * A component for a project that allows importing/exporting data from the project to archives, and deleting data when
 * the project is deleted.
 */
public interface ProjectComponent
        extends ProjectDataExporter, ProjectDataImporter
{
    /**
     * @return component identifier
     */
    String getName();

    /**
     * @return title text when displaying Import and Export options
     */
    default String getTitle() {
        return null;
    }

    /**
     * @return message code for title
     */
    default String getTitleCode() {
        return null;
    }

    /**
     * Project definition is deleted
     *
     * @param name project name
     */
    default void projectDeleted(String name) throws Exception{

    }
}
