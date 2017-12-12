/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
