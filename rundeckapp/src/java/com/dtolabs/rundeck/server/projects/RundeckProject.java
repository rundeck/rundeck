package com.dtolabs.rundeck.server.projects;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import rundeck.ProjectManagerService;

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

    public RundeckProject(
            final String name,
            final IPropertyLookup lookup,
            final ProjectManagerService projectService,
            final Date lastModifiedTime
    )
    {
        this.name = name;
        this.lookup = lookup;
        this.projectService = projectService;
        this.lastModifiedTime = lastModifiedTime;

    }

    public String getName(){
        return name;
    }
    public String getProperty(final String property) {
        return lookup.getProperty(property);
    }

    @Override
    public List<Map> listResourceModelConfigurations() {
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
    public Map<String, ?> getProperties() {
        HashMap<String, Object> result = new HashMap<>();
        if(null!=lookup){
            for(Object key:lookup.getPropertiesMap().keySet()) {
                result.put(key.toString(), lookup.getProperty(key.toString()));
            }
        }
        return result;
    }

    @Override
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        this.lookup=projectService.mergeProjectProperties(name, properties, removePrefixes);
        this.lastModifiedTime=projectService.getProjectConfigLastModified(name);
    }

    @Override
    public void setProjectProperties(final Properties properties) {
        this.lookup=projectService.setProjectProperties(name, properties);
        this.lastModifiedTime=projectService.getProjectConfigLastModified(name);
    }

    @Override
    public Date getConfigLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public boolean updateNodesResourceFile() throws UpdateUtils.UpdateException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void updateNodesResourceFileFromUrl(final String providerURL, final String username, final String password)
            throws UpdateUtils.UpdateException
    {
        throw new RuntimeException("not implemented");
    }

    public IProjectNodes getProjectNodes() {
        return projectNodes;
    }

    public void setProjectNodes(final IProjectNodes projectNodes) {
        this.projectNodes = projectNodes;
    }
}
