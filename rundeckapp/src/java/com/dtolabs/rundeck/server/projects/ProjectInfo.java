package com.dtolabs.rundeck.server.projects;

import com.dtolabs.rundeck.core.common.IProjectInfo;
import rundeck.codecs.MarkdownCodec;
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

    String readmeStr = null;
    String readmeHTML = null;

    @Override
    public String getReadmeHTML() {
        String readme = getReadme();
        if (readme != null && !readme.equals(readmeStr)) {
            readmeStr = readme;
            readmeHTML = MarkdownCodec.decodeStr(readmeStr);//markdown process
        }
        return readmeHTML;
    }

    String motdStr = null;
    String motdHTML = null;

    @Override
    public String getMotdHTML() {
        String readme = getMotd();
        if (readme != null && !readme.equals(motdStr)) {
            motdStr = readme;
            motdHTML = MarkdownCodec.decodeStr(motdStr);
        }
        return motdHTML;
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
