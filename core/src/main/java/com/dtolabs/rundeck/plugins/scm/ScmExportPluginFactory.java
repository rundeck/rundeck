package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Factory interface, used to define a ScmExportPlugin instance.
 */
public interface ScmExportPluginFactory {

    /**
     * Create a plugin instance
     * @param input input properties
     * @param project project name
     * @return instance
     * @throws ConfigurationException
     */
    ScmExportPlugin createPlugin(Map<String, String> input, String project) throws ConfigurationException;

    /**
     * Return the list of setup properties
     * @param basedir base dir
     * @return property list
     */
    List<Property> getSetupPropertiesForBasedir(File basedir) ;
}
