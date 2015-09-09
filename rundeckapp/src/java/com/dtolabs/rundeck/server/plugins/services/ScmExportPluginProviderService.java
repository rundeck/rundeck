package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.scm.PluginState;
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin;
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 8/24/15.
 */
public class ScmExportPluginProviderService extends BasePluginProviderService<ScmExportPluginFactory> {
    public static final String SERVICE_NAME = ServiceNameConstants.ScmExport;

    public ScmExportPluginProviderService() {
        super(SERVICE_NAME, ScmExportPluginFactory.class);
    }

}
