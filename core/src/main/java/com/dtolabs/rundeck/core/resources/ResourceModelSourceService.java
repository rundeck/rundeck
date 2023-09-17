/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* ResourceModelSourceService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 10:53 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import org.rundeck.app.spi.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

/**
 * ResourceModelSourceService provides NodeSource factories
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceModelSourceService
    extends PluggableProviderRegistryService<ResourceModelSourceFactory>
    implements ConfigurableService<ResourceModelSource>,
               DescribableService,
               PluggableProviderService<ResourceModelSourceFactory>,
               JavaClassProviderLoadable<ResourceModelSourceFactory>,
               ScriptPluginProviderLoadable<ResourceModelSourceFactory>
{

    public static final String SERVICE_NAME = ServiceNameConstants.ResourceModelSource;

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<String>(registry.keySet()));
    }
    public ResourceModelSourceService(final Framework framework) {
        super(framework);

        registry.put(LocalResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, LocalResourceModelSourceFactory.class);
        registry.put(FileResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, FileResourceModelSourceFactory.class);
        registry.put(DirectoryResourceModelSourceFactory.SERVICE_PROVIDER_TYPE,
            DirectoryResourceModelSourceFactory.class);
        registry.put(URLResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, URLResourceModelSourceFactory.class);
        registry.put(ScriptResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, ScriptResourceModelSourceFactory.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }


    public static ResourceModelSourceService getInstanceForFramework(final Framework framework,
                                                                     final IServicesRegistration registration) {
        if (null == registration.getService(SERVICE_NAME)) {
            final ResourceModelSourceService service = new ResourceModelSourceService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (ResourceModelSourceService) registration.getService(SERVICE_NAME);
    }


    /**
     * @return a ResourceModelSource of a give type with a given configuration, without retention
     * @param configuration configuration
     * @param type provider name
     * @throws ExecutionServiceException on error
     */
    public ResourceModelSource getSourceForConfiguration(final String type, final Properties configuration) throws
        ExecutionServiceException {

        //load instance without retention
        final ResourceModelSourceFactory nodesSourceFactory = providerOfType(type);
        try {
            return nodesSourceFactory.createResourceModelSource(configuration);
        } catch (Throwable e) {
            throw new ResourceModelSourceServiceException(e);
        }
    }

    /**
     * @param configuration configuration
     * @param type          provider name
     *
     * @return a ResourceModelSource of a give type with a given configuration with a closeable
     *
     * @throws ExecutionServiceException on error
     */
    public CloseableProvider<ResourceModelSource> getCloseableSourceForConfiguration(
        final String type,
        final Properties configuration
    ) throws ExecutionServiceException
    {
        return closeableProviderOfType(type).convert(factoryConverter(configuration));
    }

    /**
     * @param configuration configuration
     * @param type          provider name
     *
     * @return a ResourceModelSource of a give type with a given configuration with a closeable
     *
     * @throws ExecutionServiceException on error
     */
    public CloseableProvider<ResourceModelSource> getCloseableSourceForConfiguration(
        final String type,
        final Properties configuration,
        final Services services
    ) throws ExecutionServiceException
    {
        return closeableProviderOfType(type).convert(factoryConverter(services, configuration));
    }

    /**
     * Given input configuration, produce a function to convert from a factory to model source
     * @param configuration
     * @return
     */
    public static Function<ResourceModelSourceFactory, ResourceModelSource> factoryConverter(
        final Properties configuration
    )
    {
        //nb: not using lambda due to inability to mock this class within grails tests, some conflict with cglib and
        // j8 lambdas
        return new Function<ResourceModelSourceFactory, ResourceModelSource>() {
            @Override
            public ResourceModelSource apply(final ResourceModelSourceFactory resourceModelSourceFactory) {
                try {
                    return resourceModelSourceFactory.createResourceModelSource(configuration);
                } catch (ConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    /**
     * Given input configuration, produce a function to convert from a factory to model source
     * @param configuration
     * @return
     */
    public static Function<ResourceModelSourceFactory, ResourceModelSource> factoryConverter(
        final Services services,
        final Properties configuration
    )
    {
        return resourceModelSourceFactory -> {
            try {
                return resourceModelSourceFactory.createResourceModelSource(services, configuration);
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public boolean isValidProviderClass(Class clazz) {

        return ResourceModelSourceFactory.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends ResourceModelSourceFactory> ResourceModelSourceFactory
    createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    @Override
    public ResourceModelSourceFactory createScriptProviderInstance(final ScriptPluginProvider provider) throws
        PluginException {
        ScriptPluginResourceModelSourceFactory.validateScriptPlugin(provider);
        return new ScriptPluginResourceModelSourceFactory(provider,framework);
    }

    public ResourceModelSource getProviderForConfiguration(final String type,
                                                           final Properties configuration) throws
        ExecutionServiceException {
        return getSourceForConfiguration(type, configuration);
    }

    public CloseableProvider<ResourceModelSource> getCloseableProviderForConfiguration(
            final String type,
            final Properties configuration
    ) throws ExecutionServiceException
    {
        return getCloseableSourceForConfiguration(type, configuration);
    }

    public List<Description> listDescriptions() {
        //TODO: enable field annotations for properties, update plugin Interface and deprecate use of Factory
        return DescribableServiceUtil.listDescriptions(this, false);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }
}
