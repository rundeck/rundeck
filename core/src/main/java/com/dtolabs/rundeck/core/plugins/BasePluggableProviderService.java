package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.utils.Converter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * BasePluggableProviderService is an abstract base for a provider service which can load providers from plugins.
 * Created by greg
 * Date: 4/12/13
 * Time: 4:52 PM
 */
public abstract class BasePluggableProviderService<T> implements PluggableProviderService<T> {
    protected Class<? extends T> implementationClass;
    protected String name;

    public BasePluggableProviderService(final String name, final Class<? extends T> implementationClass) {
        this.name = name;
        this.implementationClass = implementationClass;
    }

    /*
         * Default implementation of isValidProviderClas, which checks the class is assignable from the specified
         * implementation class, and has a valid signature.
         */
    public boolean isValidProviderClass(final Class clazz) {
        return implementationClass.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }
    /**
     * default implementation of createProviderInstance
     */
    @Override
    public <X extends T> T createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public T providerOfType(final String providerName) throws ExecutionServiceException {
        final ServiceProviderLoader pluginManager = getPluginManager();
        if (null != pluginManager) {
            return pluginManager.loadProvider(this, providerName);
        } else {
            throw new MissingProviderException("Provider not found", getName(), providerName);
        }
    }

    /**
     * @return the plugin manager to use
     */
    public abstract ServiceProviderLoader getPluginManager();

    public List<ProviderIdent> listProviders() {
        final ArrayList<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>();

        final ServiceProviderLoader pluginManager = getPluginManager();
        if (null != pluginManager) {
            final List<ProviderIdent> providerIdents1 = pluginManager.listProviders();
            for (final ProviderIdent providerIdent : providerIdents1) {
                if (getName().equals(providerIdent.getService())) {
                    providerIdents.add(providerIdent);
                }
            }
        }
        return providerIdents;
    }

    /**
     * default implementation returns false, subclasses should override to
     */
    public boolean isScriptPluggable() {
        return false;
    }

    public T createScriptProviderInstance(ScriptPluginProvider provider)
            throws PluginException {
        throw new UnsupportedOperationException("This service does not support script plugins");
    }

    protected T createProviderInstanceFromType(final Class<? extends T> execClass, final String providerName) throws
            ProviderCreationException {

        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[0]);
            return method.newInstance();
        } catch (NoSuchMethodException e) {
            throw new ProviderCreationException(
                    "No constructor found with signature (Framework) or (): " + e.getMessage(), e,
                    getName(),
                    providerName);
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e,
                    getName(),
                    providerName);
        }
    }

    protected boolean hasValidProviderSignature(final Class clazz) {

        try {
            final Constructor method = clazz.getDeclaredConstructor(new Class[0]);
            return null != method;
        } catch (NoSuchMethodException e) {
        }
        return false;
    }

    /**
     * default implementation of listDescriptions that can be used if subclasses implement {@link
     * com.dtolabs.rundeck.core.plugins.configuration.DescribableService}
     */
    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    /**
     * default implementation of listDescribableProviders that can be used if subclasses implement {@link
     * com.dtolabs.rundeck.core.plugins.configuration.DescribableService}
     */
    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    /**
     * @return Create an adapted form of this service given a converter.
     * @param <X> provider type
     * @param converter converter
     */
    public <X> ProviderService<X> adapter(final Converter<T, X> converter) {
        return AdapterService.adaptFor(this, converter);
    }

    public String getName() {
        return name;
    }
}
