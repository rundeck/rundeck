package com.dtolabs.rundeck.server.projects;

import com.dtolabs.rundeck.core.common.IProjectInfo;
import rundeck.services.ProjectManagerService;

/**
 * Created by greg on 3/17/16.
 */
public class ProjectInfo implements IProjectInfo {
    private String description;
    private ProjectManagerService projectService;
    private String projectName;

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getReadme() {
        return projectService.readCachedProjectFileAsAstring(projectName, "readme.md");
    }


    @Override
    public String getMotd() {
        return projectService.readCachedProjectFileAsAstring(projectName, "motd.md");
    }


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
