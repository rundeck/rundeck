package com.dtolabs.rundeck.core.scm;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.scm.PluginState;
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin;
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory;
import com.dtolabs.rundeck.plugins.scm.StoredPluginState;

import java.util.*;

/**
 * Created by greg on 8/24/15.
 */
public class ScmExportService extends PluggableProviderRegistryService<ScmExportPluginFactory> implements
        DescribableService
{

    public static final String SERVICE_NAME = ServiceNameConstants.ScmExport;

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<String>(registry.keySet()));
    }

    public ScmExportService(final Framework framework) {
        super(framework);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    public static ScmExportService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final ScmExportService service = new ScmExportService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (ScmExportService) framework.getService(SERVICE_NAME);
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
            final StoredPluginState state,
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
}
