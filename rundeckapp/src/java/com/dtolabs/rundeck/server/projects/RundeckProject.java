package com.dtolabs.rundeck.server.projects;

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
    private String name;
    private ProjectManagerService projectService;
    private IProjectNodes projectNodes;
    private Date lastModifiedTime;
    private IPropertyLookup lookup;
    private IPropertyLookup projectLookup;

    public RundeckProject(
            final String name,
            final IPropertyLookup lookup,
            final IPropertyLookup projectLookup,
            final ProjectManagerService projectService,
            final Date lastModifiedTime
    )
    {
        this.name = name;
        this.setLookup(lookup);
        this.setProjectLookup(projectLookup);
        this.projectService = projectService;
        this.setLastModifiedTime(lastModifiedTime);

    }

    public String getName(){
        return name;
    }
    public String getProperty(final String property) {
        return getLookup().getProperty(property);
    }

    @Override
    public List<Map<String, Object>> listResourceModelConfigurations() {
        return projectNodes.listResourceModelConfigurations();
    }

    @Override
    public INodeSet getNodeSet() throws NodeFileParserException {
        return projectNodes.getNodeSet();
    }

    @Override
    public boolean hasProperty(final String key) {
        return getProperties().containsKey(key);
    }

    @Override
    public Map<String, String> getProperties() {
        HashMap<String, String> result = new HashMap<>();
        if(null!= getLookup()){
            for(Object key: getLookup().getPropertiesMap().keySet()) {
                result.put(key.toString(), getLookup().getProperty(key.toString()));
            }
        }
        return result;
    }
    @Override
    public Map<String, String> getProjectProperties() {
        HashMap<String, String> result = new HashMap<>();
        if(null!= getProjectLookup()){
            for(Object key: getProjectLookup().getPropertiesMap().keySet()) {
                result.put(key.toString(), getProjectLookup().getProperty(key.toString()));
            }
        }
        return result;
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
        return projectService.existsProjectFileResource(name, path);
    }

    @Override
    public boolean deleteFileResource(final String path) {
        return projectService.deleteProjectFileResource(name, path);
    }

    @Override
    public long storeFileResource(final String path, final InputStream input) throws IOException {
        return projectService.writeProjectFileResource(name, path, input, new HashMap<String, String>())
                             .getContents()
                             .getContentLength();
    }

    @Override
    public long loadFileResource(final String path, final OutputStream output) throws IOException {
        return projectService.readProjectFileResource(name,path,output);
    }

    @Override
    public Date getConfigLastModifiedTime() {
        return getLastModifiedTime();
    }

    @Override
    public boolean updateNodesResourceFile() throws UpdateUtils.UpdateException {
        return projectNodes.updateNodesResourceFile(projectService.getNodesResourceFilePath(this));
    }

    @Override
    public void updateNodesResourceFileFromUrl(final String providerURL, final String username, final String password)
            throws UpdateUtils.UpdateException
    {
        projectNodes.updateNodesResourceFileFromUrl(providerURL, username, password,projectService.getNodesResourceFilePath(this));
    }

    @Override
    public void updateNodesResourceFile(final INodeSet nodeset) throws UpdateUtils.UpdateException {
        projectNodes.updateNodesResourceFile(nodeset,projectService.getNodesResourceFilePath(this));
    }

    public IProjectNodes getProjectNodes() {
        return projectNodes;
    }

    public void setProjectNodes(final IProjectNodes projectNodes) {
        this.projectNodes = projectNodes;
    }

    @Override
    public String toString() {
        return "RundeckProject{" +
               "name='" + name + '\'' +
               ", lastModifiedTime=" + getLastModifiedTime() +
               '}';
    }

    public IPropertyLookup getLookup() {
        return lookup;
    }

    public void setLookup(final IPropertyLookup lookup) {
        this.lookup = lookup;
    }

    public IPropertyLookup getProjectLookup() {
        return projectLookup;
    }

    public void setProjectLookup(final IPropertyLookup projectLookup) {
        this.projectLookup = projectLookup;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(final Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
