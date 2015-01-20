/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* ResourceFormatParserService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/4/11 9:48 AM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.PluggableProviderRegistryService;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ResourceFormatParserService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceFormatGeneratorService extends PluggableProviderRegistryService<ResourceFormatGenerator> implements
    DescribableService {

    public static final String SERVICE_NAME = ServiceNameConstants.ResourceFormatGenerator;

    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<String>(registry.keySet()));
    }

    public ResourceFormatGeneratorService(final Framework framework) {
        super(framework);

        registry.put(ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE, ResourceXMLFormatGenerator.class);
        registry.put(ResourceYamlFormatGenerator.SERVICE_PROVIDER_TYPE, ResourceYamlFormatGenerator.class);
    }

    public String getName() {
        return SERVICE_NAME;
    }

    /**
     * @return the available format identifiers provided by all parsers
     */
    public List<String> listFormats() {
        final ArrayList<String> list = new ArrayList<String>();
        for (final ProviderIdent providerIdent : listProviders()) {
            list.add(providerIdent.getProviderName());
        }
        return list;
    }


    /**
     * Return a generator for a file, based on the file extension.
     *
     * @param file the file
     *
     * @return the generator found for the extension
     *
     * @throws UnsupportedFormatException if the file extension does not match an available generator, or if the file
     *                                    has no extension
     */
    public ResourceFormatGenerator getGeneratorForFileExtension(final File file) throws UnsupportedFormatException {
        String extension = file.getName().lastIndexOf(".") > 0 ? file.getName().substring(file.getName().lastIndexOf(
            ".") + 1) : null;
        if (null != extension) {
            return getGeneratorForFileExtension(extension);
        } else {
            throw new UnsupportedFormatException("Could not determine format for file: " + file.getAbsolutePath());
        }
    }

    /**
     * Return a generator for a file, based on the bare file extension.
     *
     * @param extension the file extension string
     *
     * @return the generator found for the extension
     *
     * @throws UnsupportedFormatException if the file extension does not match an available generator
     */
    public ResourceFormatGenerator getGeneratorForFileExtension(final String extension) throws
        UnsupportedFormatException {
        for (final ResourceFormatGenerator generator : listGenerators()) {
            if (generator.getFileExtensions().contains(extension)) {
                return generator;
            }
        }
        throw new UnsupportedFormatException("No provider available to parse file extension: " + extension);
    }

    /**
     * Return a parser for the exact format name
     *
     * @param format the format name
     *
     * @return the parser found for the format
     *
     * @throws com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException
     *          if no provider for the format exists
     */
    public ResourceFormatGenerator getGeneratorForFormat(final String format) throws UnsupportedFormatException {
        try {
            return providerOfType(format);
        } catch (ExecutionServiceException e) {
            throw new UnsupportedFormatException("No provider available to parse format: " + format, e);
        }
    }


    /**
     * Return a generator for a mime type.
     *
     * @param mimeType the MIME type string
     *
     * @return The first matching parser for the mime type
     *
     * @throws UnsupportedFormatException if no generators are available for the MIME type
     */
    public ResourceFormatGenerator getGeneratorForMIMEType(final String mimeType) throws UnsupportedFormatException {
        //clean up encoding
        final String cleanMime;
        if (mimeType.indexOf(";") > 0) {
            cleanMime = mimeType.substring(0, mimeType.indexOf(";"));
        } else {
            cleanMime = mimeType;
        }
        if (!ResourceFormatParserService.validMimeType(cleanMime)) {
            throw new IllegalArgumentException("Invalid MIME type: " + mimeType);
        }
        for (final ResourceFormatGenerator generator : listGenerators()) {
            if (null != generator.getMIMETypes()) {
                if (generator.getMIMETypes().contains(cleanMime)) {
                    return generator;
                } else {
                    for (final String s : generator.getMIMETypes()) {
                        if (ResourceFormatParserService.validMimeType(s) && cleanMime.startsWith("*/")) {
                            String t1 = cleanMime.substring(2);
                            String t2 = s.substring(s.indexOf("/") + 1);
                            if (t1.equals(t2)) {
                                return generator;
                            }
                        }
                    }
                }
            }
        }
        throw new UnsupportedFormatException("No provider available to parse MIME type: " + mimeType);
    }


    public boolean isValidProviderClass(Class clazz) {
        return ResourceFormatGenerator.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends ResourceFormatGenerator> ResourceFormatGenerator createProviderInstance(
            Class<X> clazz,
            String name
    )
            throws PluginException, ProviderCreationException {

        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    public ResourceFormatGenerator createScriptProviderInstance(ScriptPluginProvider provider) throws
        PluginException {
        return null;
    }

    public List<ProviderIdent> listDescribableProviders() {
        return listProviders();
    }


    public List<Description> listDescriptions() {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ResourceFormatGenerator provider : listGenerators()) {
            if (provider instanceof Describable) {
                final Describable desc = (Describable) provider;
                list.add(desc.getDescription());
            }
        }
        return list;
    }

    private List<ResourceFormatGenerator> listGenerators() {
        final ArrayList<ResourceFormatGenerator> list = new ArrayList<ResourceFormatGenerator>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final ResourceFormatGenerator providerForType = providerOfType(providerIdent.getProviderName());
                list.add(providerForType);
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static ResourceFormatGeneratorService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final ResourceFormatGeneratorService service = new ResourceFormatGeneratorService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (ResourceFormatGeneratorService) framework.getService(SERVICE_NAME);

    }
}
