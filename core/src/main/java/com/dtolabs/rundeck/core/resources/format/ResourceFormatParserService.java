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
import java.util.*;

/**
 * ResourceFormatParserService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceFormatParserService extends PluggableProviderRegistryService<ResourceFormatParser> implements
    DescribableService {

    public static final String SERVICE_NAME = ServiceNameConstants.ResourceFormatParser;


    public List<String> getBundledProviderNames() {
        return Collections.unmodifiableList(new ArrayList<String>(registry.keySet()));
    }
    public ResourceFormatParserService(final Framework framework) {
        super(framework);


        registry.put(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE, ResourceXMLFormatParser.class);
        registry.put(ResourceYamlFormatParser.SERVICE_PROVIDER_TYPE, ResourceYamlFormatParser.class);
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
     * @return  the available format identifiers provided by all parsers
     */
    public List<String> listSupportedFileExtensions() {
        final ArrayList<String> list = new ArrayList<String>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final ResourceFormatParser resourceFormatParser = providerOfType(providerIdent.getProviderName());
                list.addAll(resourceFormatParser.getFileExtensions());
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * Return a parser for a file, based on the file extension.
     *
     * @param file the file
     *
     * @return the parser found for the extension
     *
     * @throws UnsupportedFormatException if the file extension does not match an available parser, or if the file has
     *                                    no extension
     */
    public ResourceFormatParser getParserForFileExtension(final File file) throws UnsupportedFormatException {
        String extension = getFileExtension(file.getName());
        if (null != extension) {
            return getParserForFileExtension(extension);
        } else {
            throw new UnsupportedFormatException("Could not determine format for file: " + file.getAbsolutePath());
        }
    }

    /**
     * @return  the file extension of the file, without ".", or null if the file name doesn't have an extension
     * @param name file name
     */
    public static String getFileExtension(final String name) {
        final int i = name.lastIndexOf(".");
        return i > 0 && i < (name.length() - 1) ? name.substring(name.lastIndexOf(".") + 1) : null;
    }

    /**
     * Return a parser for a file, based on the bare file extension.
     *
     * @param extension the file extension string
     *
     * @return the parser found for the extension
     *
     * @throws UnsupportedFormatException if the file extension does not match an available parser, or if the file has
     *                                    no extension
     */
    public ResourceFormatParser getParserForFileExtension(final String extension) throws UnsupportedFormatException {
        for (final ResourceFormatParser resourceFormatParser : listParsers()) {
            if (resourceFormatParser.getFileExtensions().contains(extension)) {
                return resourceFormatParser;
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
     * @throws com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException if format is not supported
     */
    public ResourceFormatParser getParserForFormat(final String format) throws UnsupportedFormatException {
        try {
            return providerOfType(format);
        } catch (ExecutionServiceException e) {
            throw new UnsupportedFormatException("No provider available to parse format: " + format,e);
        }
    }

    /**
     * Return a parser for a mime type.
     *
     * @param mimeType the MIME type string
     *
     * @return The first matching parser for the mime type
     *
     * @throws UnsupportedFormatException if no parser are available for the MIME type
     */
    public ResourceFormatParser getParserForMIMEType(final String mimeType) throws UnsupportedFormatException {
        //clean up encoding
        final String cleanMime;
        if (null != mimeType && mimeType.indexOf(";") > 0) {
            cleanMime = mimeType.substring(0, mimeType.indexOf(";"));
        } else {
            cleanMime = mimeType;
        }
        if (!validMimeType(cleanMime)) {
            throw new IllegalArgumentException("Invalid MIME type: " + mimeType);
        }
        for (final ResourceFormatParser resourceFormatParser : listParsers()) {
            if(null!= resourceFormatParser.getMIMETypes()){
                if (resourceFormatParser.getMIMETypes().contains(cleanMime)) {
                    return resourceFormatParser;
                } else {
                    for (final String s : resourceFormatParser.getMIMETypes()) {
                        if (validMimeType(s) && s.startsWith("*/")) {
                            String t1 = s.substring(2);
                            String t2 = cleanMime.substring(cleanMime.indexOf("/") + 1);
                            if (t1.equals(t2)) {
                                return resourceFormatParser;
                            }
                        }
                    }
                }
            }
        }
        throw new UnsupportedFormatException("No provider available to parse MIME type: " + mimeType);
    }

    static boolean validMimeType(final String cleanMime) {
        if(null==cleanMime){
            return false;
        }
        final int l = cleanMime.indexOf("/");
        return l > 0 && l < cleanMime.length() - 1 && l == cleanMime.lastIndexOf("/");
    }

    public boolean isValidProviderClass(Class clazz) {
        return ResourceFormatParser.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    @Override
    public <X extends ResourceFormatParser> ResourceFormatParser createProviderInstance(Class<X> clazz, String name)
            throws PluginException, ProviderCreationException {
        return createProviderInstanceFromType(clazz, name);
    }

    public boolean isScriptPluggable() {
        return false;
    }

    public ResourceFormatParser createScriptProviderInstance(ScriptPluginProvider provider) throws
        PluginException {
        return null;
    }

    public List<ProviderIdent> listDescribableProviders() {
        return listProviders();
    }


    public List<Description> listDescriptions() {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ResourceFormatParser provider : listParsers()) {
            if (provider instanceof Describable) {
                final Describable desc = (Describable) provider;
                list.add(desc.getDescription());
            }
        }
        return list;
    }

    private List<ResourceFormatParser> listParsers() {
        final ArrayList<ResourceFormatParser> list = new ArrayList<ResourceFormatParser>();
        for (final ProviderIdent providerIdent : listProviders()) {
            try {
                final ResourceFormatParser providerForType = providerOfType(providerIdent.getProviderName());
                list.add(providerForType);
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static ResourceFormatParserService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final ResourceFormatParserService service = new ResourceFormatParserService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (ResourceFormatParserService) framework.getService(SERVICE_NAME);

    }
}
