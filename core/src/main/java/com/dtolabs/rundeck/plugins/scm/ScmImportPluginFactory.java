package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 9/9/15.
 */
public interface ScmImportPluginFactory {
    ScmImportPlugin createPlugin(Map<String, ?> input, String project) throws ConfigurationException;

    List<Property> getSetupPropertiesForBasedir(File basedir) ;
}
