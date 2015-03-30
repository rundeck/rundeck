/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.common.PropertyRetriever;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Simple utiltiy class to lookup property info
 */
public class PropertyLookup implements IPropertyLookup {
    /**
     * Properties instance where data will be kept in memory
     */
    final Properties properties;
    final File propsFile;
    volatile boolean deferred;

    private PropertyLookup(final File propsFile, final boolean deferred) {
        properties = new Properties();
        this.propsFile=propsFile;
        this.deferred=deferred;
    }
    private PropertyLookup(final Properties props) {
        properties = props;
        this.propsFile=null;
        this.deferred=false;
    }
    private PropertyLookup(final Map props) {
        properties = new Properties();
        properties.putAll(props);
        this.propsFile=null;
        this.deferred=false;
    }

    public static PropertyLookup create(final Properties props) {
        return new PropertyLookup(props);
    }


    public static PropertyLookup create(final IPropertyLookup defaultsLookup) {
        return new PropertyLookup(new Properties(), defaultsLookup);
    }


    /**
     * Calls base constructor then reads defaults map. Properties which are NOT contained
     * in the internal store, will be accepted and added.
     *
     * @param props    Property set
     * @param defaults Map of default properties
     */
    private PropertyLookup(final Properties props, final Map defaults) {
        this(props);
        properties.putAll(difference(defaults));
    }
    /**
     * Calls base constructor then reads defaults map. Properties which are NOT contained
     * in the internal store, will be accepted and added.
     *
     * @param props    Property set
     * @param defaults Map of default properties
     */
    private PropertyLookup(final Map props, final Map defaults) {
        this(props);
        properties.putAll(difference(defaults));
    }

    /**
     * Calls base constructor with data from IPropertyLookup paramater as defaults. Defaults
     * data is read via the {@link IPropertyLookup#getPropertiesMap()} method.
     *
     * @param props          Property set
     * @param defaultsLookup IPropertyLookup of default properties
     */
    private PropertyLookup(final Properties props, final IPropertyLookup defaultsLookup) {
        this(props, defaultsLookup.getPropertiesMap());
    }

    /**
     * Calls base constructor with data from IPropertyLookup paramater as defaults. Defaults
     * data is read via the {@link IPropertyLookup#getPropertiesMap()} method.
     *
     * @param props          Property set
     * @param defaultsLookup IPropertyLookup of default properties
     */
    private PropertyLookup(final IPropertyLookup props, final IPropertyLookup defaultsLookup) {
        this(props.getPropertiesMap(), defaultsLookup.getPropertiesMap());
    }

    /**
     * Calls base constructor feeding defaults from Map and IPropertyLookup params
     *
     * @param propfile       File containing property data
     * @param defaults       Map of default properties
     * @param defaultsLookup IPropertyLookup of default properties
     */
    private PropertyLookup(final File propfile, final Map defaults, final IPropertyLookup defaultsLookup) {
        this(fetchProperties(propfile), defaults);
        properties.putAll(difference(defaultsLookup.getPropertiesMap()));
    }

    /**
     * @return Factory method to create a property lookup object
     *
     * @param propFile File where proeprty data is contained
     */
    public static PropertyLookup create(final File propFile) {
        return new PropertyLookup(fetchProperties(propFile));
    }
    /**
     * @return Factory method to create a property lookup object
     *
     * @param propFile File where proeprty data is contained
     */
    public static PropertyLookup createDeferred(final File propFile) {
        return new PropertyLookup(propFile, true);
    }

    /**
     * Calls base constructor with data from IPropertyLookup paramater as defaults. Defaults
     * data is read via the {@link IPropertyLookup#getPropertiesMap()} method.
     *
     * @param propfile       File containing property data
     * @param defaultsLookup IPropertyLookup of default properties
     *                       @return lookup
     */
    public static PropertyLookup create(final File propfile, final IPropertyLookup defaultsLookup) {
        return new PropertyLookup(fetchProperties(propfile), defaultsLookup);
    }
    /**
     *
     * @param data       Properties data
     * @param defaultsLookup IPropertyLookup of default properties
     *                       @return lookup
     */
    public static PropertyLookup create(final Properties data, final IPropertyLookup defaultsLookup) {
        return new PropertyLookup(data, defaultsLookup);
    }

    /**
     *
     * @param data       Properties data
     * @param defaultsLookup IPropertyLookup of default properties
     *                       @return lookup
     */
    public static PropertyLookup create(final IPropertyLookup data, final IPropertyLookup defaultsLookup) {
        return new PropertyLookup(data, defaultsLookup);
    }

    /**
     * Calls base constructor feeding defaults from Map and IPropertyLookup params
     *
     * @param propfile       File containing property data
     * @param defaults       Map of default properties
     * @param defaultsLookup IPropertyLookup of default properties
     *                       @return lookup
     */
    public static PropertyLookup create(final File propfile, final Map defaults, final IPropertyLookup defaultsLookup) {
        return new PropertyLookup(propfile, defaults, defaultsLookup);
    }

    /**
     * Get the property per specified key
     *
     * @param key name of the property
     * @return Value of the property
     * @throws PropertyLookupException thrown if lookup fails for specified key
     */
    public String getProperty(final String key) {
        if (hasProperty(key)) {
            return properties.getProperty(key);
        } else {
            throw new PropertyLookupException("property not found: " + key);
        }
    }

    public PropertyRetriever safe() {
        return safePropertyRetriever(this);
    }
    /**
     * @return Create a PropertyRetriever from a PropertyLookup that will not throw exception
     * @param lookup lookup
     */
    public static PropertyRetriever safePropertyRetriever(final IPropertyLookup lookup){
        return new PropertyRetriever() {
            public String getProperty(String name) {
                if(lookup.hasProperty(name)) {
                    return lookup.getProperty(name);
                }else {
                    return null;
                }
            }
        };
    }

    /**
     * Check if property exists in file
     *
     * @param key Name of the property
     * @return true if it exists; false otherwise
     */
    public boolean hasProperty(final String key) {
        if(deferred){
            loadProperties();
        }
        return properties.containsKey(key);
    }

    private synchronized void loadProperties() {
        if (deferred && propsFile.exists()) {
            try {
                properties.putAll(fetchProperties(propsFile));
                expand();
                deferred = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * given a file reads in its properties
     *
     * @param propFile File to read
     * @return a Properties object with data filled from propFile
     * @throws PropertyLookupException thrown if error loading property file
     */
    public static Properties fetchProperties(final File propFile) {
        final Properties properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream(propFile);
            try {
                properties.load(fis);
            } finally {
                if(null!=fis){
                    fis.close();
                }
            }
        } catch (IOException e) {
            throw new PropertyLookupException("failed loading properties from file: " + propFile, e);
        }
        return properties;
    }


    /**
     * Retrieves map of property data
     *
     * @return Unmodifiable {@link Map} containing property key/value pair
     * @throws PropertyLookupException thrown if loaderror
     */
    public Map getPropertiesMap() {
        return Collections.unmodifiableMap(properties);
    }


    /**
     * Calls {@link PropertyUtil#expand(Map)} to expand all properties.
     * @return expanded lookup
     */
    public PropertyLookup expand() {
        try {
            final Properties expanded = PropertyUtil.expand(properties);
            properties.putAll(expanded);
        } catch (Exception e) {
            throw new PropertyLookupException("failed expanding properties", e);
        }
        return this;
    }

    /**
     * Reads map of input properties and returns a collection of those that are unique
     * to that input set.
     *
     * @param map Map of key/value pairs
     * @return Properties unique to map
     */
    protected Properties difference(final Map map) {
        final Properties difference = new Properties();
        for (final Object o : map.entrySet()) {
            final Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final String val = (String) entry.getValue();
            if (!properties.containsKey(key)) {
                difference.setProperty(key, val);
            }
        }
        return difference;
    }

    /**
     * Counts number of properties currently in this object.
     *
     * @return number of properties
     */
    protected int countProperties() {
        return properties.size();
    }

    /**
     * Reads propFile and then checks if specified key exists.
     *
     * @param propKey  property name
     * @param propFile property file
     * @return file if a property with that name exists. If an exception occurs while reading
     *         the file, false is returned.
     */
    public static boolean hasProperty(final String propKey, final File propFile) {
        if (null == propKey) throw new IllegalArgumentException("propKey param was null");
        if (null == propFile) throw new IllegalArgumentException("propFile param was null");
        if (propFile.exists()) {
            final Properties p = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propFile);
                try {
                    p.load(fis);
                } finally {
                    if (null != fis) {
                        fis.close();
                    }
                }
                return p.containsKey(propKey);
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
