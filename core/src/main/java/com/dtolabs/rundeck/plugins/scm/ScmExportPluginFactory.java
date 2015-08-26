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

    ScmExportPlugin createPlugin(Map<String, ?> input, String project) throws ConfigurationException;

    List<Property> getSetupPropertiesForBasedir(File basedir) ;
}
