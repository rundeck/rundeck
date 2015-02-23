package com.dtolabs.rundeck.server.projects;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.common.UpdateUtils;
import rundeck.ProjectManagerService;

import java.util.*;

/**
 * Created by greg on 2/23/15.
 */
public class RundeckProject implements IRundeckProject{
    private String name;
    private Properties properties;
    private ProjectManagerService projectService;

    public RundeckProject(final String name, final Properties properties, final ProjectManagerService projectService) {
        this.name = name;
        this.properties = properties;
        this.projectService = projectService;
    }

    public String getName(){
        return name;
    }
    public String getProperty(final String property) {
        return properties.getProperty(property);
    }

    @Override
    public List<Map> listResourceModelConfigurations() {
        return projectService.listResourceModelConfigurations(name);
    }

    @Override
    public INodeSet getNodeSet() throws NodeFileParserException {
        return projectService.getNodeSet(name);
    }

    @Override
    public boolean hasProperty(final String key) {
        return getProperties().containsKey(key);
    }

    @Override
    public Map<String, ?> getProperties() {
        HashMap<String, Object> result = new HashMap<>();
        if(null!=properties){
            for(Object key:properties.keySet()) {
                result.put(key.toString(), properties.getProperty(key.toString()));
            }
        }
        return result;
    }

    @Override
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        projectService.mergeProjectProperties(name, properties, removePrefixes);
    }

    @Override
    public void setProjectProperties(final Properties properties) {
        projectService.setProjectProperties(name, properties);
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
}
