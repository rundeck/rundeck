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
public class ScmExportPluginProviderService extends BasePluggableProviderService<ScmExportPluginFactory> {
    public static final String SERVICE_NAME = ServiceNameConstants.ScmExport;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public ScmExportPluginProviderService() {
        super(SERVICE_NAME, ScmExportPluginFactory.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    @Override
    public ServiceProviderLoader getPluginManager() {
        return getRundeckServerServiceProviderLoader();
    }

    /**
     * @param configuration configuration
     * @param type          provider name
     *
     * @return a ResourceModelSource of a give type with a given configuration
     *
     * @throws ExecutionServiceException on error
     */
    public ScmExportPlugin getPluginForConfiguration(
            final String type,
            final Map<String, String> configuration,
            final String projectName
    ) throws
            ExecutionServiceException
    {

        //try to acquire supplier from registry
        final ScmExportPluginFactory nodesSourceFactory = providerOfType(type);
        try {
            return nodesSourceFactory.createPlugin(configuration, projectName);
        } catch (Throwable e) {
            throw new ExecutionServiceException(e, SERVICE_NAME);
        }
    }

    public boolean isValidProviderClass(Class clazz) {

        return ScmExportPluginFactory.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends ScmExportPluginFactory> ScmExportPluginFactory
    createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException
    {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    @Override
    public ScmExportPluginFactory createScriptProviderInstance(final ScriptPluginProvider provider)
            throws PluginException
    {
        return null;
    }


    public List<Description> listDescriptions() {
        //TODO: enable field annotations for properties, update plugin Interface and deprecate use of Factory
        return DescribableServiceUtil.listDescriptions(this, false);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }
}
