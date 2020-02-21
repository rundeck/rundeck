package org.rundeck.app.components.project;

import org.rundeck.core.projects.ProjectDataExporter;
import org.rundeck.core.projects.ProjectDataImporter;

public interface ProjectComponent extends ProjectDataExporter, ProjectDataImporter {

    /**
     * Project definition is deleted
     * @param name project name
     */
    default void projectDeleted(String name){

    }
}
