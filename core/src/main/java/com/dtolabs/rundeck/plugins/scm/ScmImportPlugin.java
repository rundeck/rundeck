package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;
import java.util.Map;

/**
 * Created by greg on 8/20/15.
 */
public interface ScmImportPlugin {

    /**
     * Define UI properties for export action
     *
     * @return list of properties
     */
    List<Property> getImportProperties();

    /**
     * Perform import with the input
     *
     * @param input result of GUI input
     * @param importer TODO can import files as jobs
     */
    void scmImport(Map<String, Object> input, Object importer) throws ScmPluginException;
}
