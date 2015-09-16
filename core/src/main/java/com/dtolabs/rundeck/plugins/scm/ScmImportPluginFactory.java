package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Factory for {@link ScmImportPlugin}, interface for SCMImport plugins.
 */
public interface ScmImportPluginFactory {
    /**
     * Create the plugin
     *
     * @param input        setup config
     * @param trackedItems tracked items list
     * @param project      project name
     *
     * @return plugin instance
     *
     * @throws ConfigurationException if an error occurs
     */
    ScmImportPlugin createPlugin(Map<String, String> input, List<String> trackedItems, String project)
            throws ConfigurationException;

    /**
     * Setup properties for the base directory
     *
     * @param basedir project base directory
     *
     * @return setup properties
     */
    List<Property> getSetupPropertiesForBasedir(File basedir);
}
