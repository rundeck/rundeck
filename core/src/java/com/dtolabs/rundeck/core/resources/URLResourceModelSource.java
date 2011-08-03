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
* URLResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 4:33 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdaterBuilder;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * URLResourceModelSource produces nodes from a URL
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class URLResourceModelSource implements ResourceModelSource, Configurable {
    static final Logger logger = Logger.getLogger(URLResourceModelSource.class.getName());
    public static final int DEFAULT_TIMEOUT = 30;
    final private Framework framework;
    Configuration configuration;
    private File destinationTempFile;
    private File destinationCacheData;
    private String tempFileName;
    private Nodes.Format contentFormat;
    URLFileUpdater.httpClientInteraction interaction;

    public URLResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    final static HashSet<String> allowedProtocols = new HashSet<String>(Arrays.asList("http", "https", "file"));

    static ArrayList<Property> properties = new ArrayList<Property>();

    static {
        properties.add(PropertyUtil.string(Configuration.URL, "URL", "URL for the remote resource model document", true,
            null, new Property.Validator() {
            public boolean isValid(String value) throws ValidationException {
                final URL url;

                try{
                    url= new URL(value);
                } catch (MalformedURLException e) {
                    throw new ValidationException(e.getMessage());
                }
                if (null != url && !allowedProtocols.contains(url.getProtocol().toLowerCase())) {
                    throw new ValidationException("url protocol not supported: " + url.getProtocol());
                }
                return true;
            }
        }));
        properties.add(PropertyUtil.integer(Configuration.TIMEOUT, "Timeout", "Timeout (in seconds) before requests fail. 0 means no timeout.", false, "30"));
        properties.add(PropertyUtil.bool(Configuration.CACHE, "Cache results",
            "Refresh results only if modified?", true, "true"));

    }
    public static final Description DESCRIPTION = new Description(){
        public String getName() {
            return "url";
        }

        public String getTitle() {
            return "URL Source";
        }

        public String getDescription() {
            return "Retrieves a URL containing node definitions in a supported format";
        }

        public List<Property> getProperties() {

            return properties;
        }
    };
    public static class Configuration {
        public static final String URL = "url";
        public static final String PROJECT = "project";
        public static final String CACHE = "cache";
        public static final String TIMEOUT = "timeout";
        URL nodesUrl;
        String project;
        boolean useCache = true;
        int timeout = DEFAULT_TIMEOUT;

        private final Properties properties;

        Configuration() {
            properties = new Properties();
        }

        Configuration(final Properties configuration) {
            if (null == configuration) {
                throw new NullPointerException("configuration");
            }
            this.properties = configuration;
            configure();
        }

        private void configure() {
            if (properties.containsKey(URL)) {
                try {
                    nodesUrl = new URL(properties.getProperty(URL));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            if (properties.containsKey(PROJECT)) {
                project = properties.getProperty(PROJECT);
            }
            if (properties.containsKey(CACHE)) {
                useCache = Boolean.parseBoolean(properties.getProperty(CACHE));
            }
            if (properties.containsKey(TIMEOUT)) {
                try {
                    timeout = Integer.parseInt(properties.getProperty(TIMEOUT));
                } catch (NumberFormatException e) {
                }
            }
        }

        void validate() throws ConfigurationException {
            if (null == project) {
                throw new ConfigurationException("project is required");
            }
            if (null == nodesUrl && properties.containsKey(URL)) {
                try {
                    nodesUrl = new URL(properties.getProperty(URL));
                } catch (MalformedURLException e) {
                    throw new ConfigurationException("url is malformed: " + e.getMessage(), e);
                }
            } else if (null == nodesUrl) {
                throw new ConfigurationException("url is required");
            }
            if (null != nodesUrl && !allowedProtocols.contains(nodesUrl.getProtocol().toLowerCase())) {
                throw new ConfigurationException("url protocol not allowed: " + nodesUrl.getProtocol());
            }
            if (properties.containsKey(TIMEOUT)) {
                try {
                    timeout = Integer.parseInt(properties.getProperty(TIMEOUT));
                } catch (NumberFormatException e) {
                    throw new ConfigurationException("timeout is invalid: " + e.getMessage(), e);
                }
            }
        }

        Configuration(final Configuration configuration) {
            this(configuration.getProperties());
        }

        public Configuration url(final String url) {
            try {
                this.nodesUrl = new URL(url);
            } catch (MalformedURLException e) {
            }
            properties.setProperty("url", url);
            return this;
        }

        public Configuration project(final String project) {
            this.project = project;
            properties.setProperty(PROJECT, project);
            return this;
        }

        public Configuration cache(final boolean cache) {
            this.useCache = cache;
            properties.setProperty(CACHE, Boolean.toString(cache));
            return this;
        }

        public Configuration timeout(final int timeout) {
            this.timeout = timeout;
            properties.setProperty(TIMEOUT, Integer.toString(timeout));
            return this;
        }

        public static Configuration fromProperties(final Properties configuration) {
            return new Configuration(configuration);
        }

        public static Configuration clone(final Configuration configuration) {
            return fromProperties(configuration.getProperties());
        }

        public static Configuration build() {
            return new Configuration();
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
        //set destination temp file
        final FrameworkProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
            this.configuration.project);

        tempFileName =  hashURL(this.configuration.nodesUrl.toExternalForm()) + ".temp";
        destinationTempFile = new File(frameworkProject.getBaseDir(), "var/urlResourceModelSourceCache/" + tempFileName);
        destinationCacheData = new File(frameworkProject.getBaseDir(),
            "var/urlResourceModelSourceCache/" + tempFileName + ".cache.properties");
        destinationTempFile.getParentFile().mkdirs();
    }
    private String hashURL(final String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(url.getBytes(Charset.forName("UTF-8")));
            return new String(Hex.encodeHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return Integer.toString(url.hashCode());
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        //update from URL if necessary
        URLFileUpdater updater = null;
        try {
            final URLFileUpdaterBuilder urlFileUpdaterBuilder = new URLFileUpdaterBuilder()
                .setUrl(configuration.nodesUrl)
                .setAcceptHeader("*/xml,*/yaml,*/yml")
                .setTimeout(configuration.timeout);
            if (configuration.useCache) {
                urlFileUpdaterBuilder
                    .setCacheMetadataFile(destinationCacheData)
                    .setCachedContent(destinationTempFile)
                    .setUseCaching(true);
            }
            updater = urlFileUpdaterBuilder.createURLFileUpdater();
            if (null != interaction) {
                //allow mock
                updater.setInteraction(interaction);
            }
            UpdateUtils.update(updater, destinationTempFile);

            logger.debug("Updated nodes resources file: " + destinationTempFile);
        } catch (UpdateUtils.UpdateException e) {
            if (!destinationTempFile.isFile() || destinationTempFile.length() < 1) {
                throw new ResourceModelSourceException(
                    "Error updating from URL: " + configuration.nodesUrl + ": " + e.getMessage(), e);
            } else {
                logger.error("Error updating from URL: " + configuration.nodesUrl + ": " + e.getMessage(), e);
            }
        }
        final Nodes.Format format;
        if ("file".equalsIgnoreCase(configuration.nodesUrl.getProtocol())) {
            final String urlStr = configuration.nodesUrl.toExternalForm();
            format = urlStr.endsWith(".xml") ? Nodes.Format.resourcexml : urlStr.endsWith(".yaml")
                                                                          || urlStr.endsWith(".yml")
                                                                          ? Nodes.Format.resourceyaml : null;
        } else {
            format = determineFormat(null != updater ? updater.getContentType() : null);
        }
        if (null != format) {
            contentFormat = format;
        }
        //parse file
        if (null == contentFormat) {
            throw new ResourceModelSourceException("Unable to determine content format");
        }
        logger.debug("Determined URL content format: " + contentFormat);
        if (destinationTempFile.isFile() && destinationTempFile.length() > 0) {
            try {
                return FileResourceModelSource.parseFile(destinationTempFile, contentFormat, framework,
                    configuration.project);
            } catch (ConfigurationException e) {
                throw new ResourceModelSourceException(e);
            }
        } else {
            return new NodeSetImpl();
        }
    }


    private Nodes.Format determineFormat(final String contentType) {
        if (null != contentType) {
            if (contentType.endsWith("/xml")) {
                return Nodes.Format.resourcexml;
            } else if (contentType.endsWith("/yaml") || contentType.endsWith("/yml")) {
                return Nodes.Format.resourceyaml;
            }
        }
        return null;
    }

}
