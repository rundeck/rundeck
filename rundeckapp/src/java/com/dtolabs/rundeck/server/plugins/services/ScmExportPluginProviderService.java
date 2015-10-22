package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory;

/**
 * Created by greg on 8/24/15.
 */
public class ScmExportPluginProviderService extends BasePluginProviderService<ScmExportPluginFactory> {
    public static final String SERVICE_NAME = ServiceNameConstants.ScmExport;

    public ScmExportPluginProviderService() {
        super(SERVICE_NAME, ScmExportPluginFactory.class);
    }

}
