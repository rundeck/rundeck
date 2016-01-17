package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory;

/**
 * Loads ScmImportPluginFactory
 */
public class ScmImportPluginProviderService extends BasePluginProviderService<ScmImportPluginFactory> {
    public static final String SERVICE_NAME = ServiceNameConstants.ScmImport;

    public ScmImportPluginProviderService() {
        super(SERVICE_NAME, ScmImportPluginFactory.class);
    }

}
