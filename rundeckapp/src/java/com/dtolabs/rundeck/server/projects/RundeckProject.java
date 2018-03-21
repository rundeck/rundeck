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

import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import rundeck.services.ProjectManagerService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by greg on 2/23/15.
 */
public class RundeckProject implements IRundeckProject{
    private ProjectManagerService projectService;
    private IRundeckProjectConfig projectConfig;
    private IProjectInfo info;
    private IProjectNodesFactory nodesFactory;

    public RundeckProject(
            final IRundeckProjectConfig projectConfig,
            final ProjectManagerService projectService
    )
    {
        this.projectConfig=projectConfig;
        this.projectService = projectService;

    }

    public String getName(){
        return projectConfig.getName();
    }

    public String getProperty(final String property) {
        return projectConfig.getProperty(property);
    }

    @Override
    public List<Map<String, Object>> listResourceModelConfigurations() {
        return getProjectNodes().listResourceModelConfigurations();
    }

    @Override
    public INodeSet getNodeSet() throws NodeFileParserException {
        return getProjectNodes().getNodeSet();
    }

    @Override
    public boolean hasProperty(final String key) {
        return projectConfig.hasProperty(key);
    }

    @Override
    public Map<String, String> getProperties() {
        return projectConfig.getProperties();
    }
    @Override
    public Map<String, String> getProjectProperties() {
        return projectConfig.getProjectProperties();
    }

    @Override
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        projectService.mergeProjectProperties(this, properties, removePrefixes);
    }

    @Override
    public void setProjectProperties(final Properties properties) {
        projectService.setProjectProperties(this, properties);
    }

    @Override
    public boolean existsFileResource(final String path) {
        return projectService.existsProjectFileResource(getName(), path);
    }

    @Override
    public boolean existsDirResource(final String path) {
        return projectService.existsProjectDirResource(getName(), path);
    }

    @Override
    public List<String> listDirPaths(final String path) {
        return projectService.listProjectDirPaths(getName(), path);
    }

    @Override
    public boolean deleteFileResource(final String path) {
        return projectService.deleteProjectFileResource(getName(), path);
    }

    @Override
    public long storeFileResource(final String path, final InputStream input) throws IOException {
        return projectService.writeProjectFileResource(getName(), path, input, new HashMap<String, String>())
                             .getContents()
                             .getContentLength();
    }

    @Override
    public long loadFileResource(final String path, final OutputStream output) throws IOException {
        return projectService.readProjectFileResource(getName(),path,output);
    }

    @Override
    public Date getConfigLastModifiedTime() {
        return projectConfig.getConfigLastModifiedTime();
    }


    public IProjectNodes getProjectNodes() {
        return getNodesFactory().getNodes(getName());
    }

    @Override
    public String toString() {
        return "RundeckProject{" +
               "config='" + projectConfig + '\'' +
               '}';
    }


    public Authorization getProjectAuthorization() {
        return projectService.getProjectAuthorization(getName());
    }

    public IRundeckProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public void setProjectConfig(IRundeckProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
    }

    @Override
    public IProjectInfo getInfo() {
        return info;
    }

    public void setInfo(IProjectInfo info) {
        this.info = info;
    }

    public IProjectNodesFactory getNodesFactory() {
        return nodesFactory;
    }

    public void setNodesFactory(IProjectNodesFactory nodesFactory) {
        this.nodesFactory = nodesFactory;
    }
}
