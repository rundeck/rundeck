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

package com.dtolabs.rundeck.core.common;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents a generic framework resource. Each resoure has a name, a
 * base directory and a set of properties.
 * <br>
 */
public class FrameworkResource implements IFrameworkResource {
    public static final String VALID_RESOURCE_NAME_CHARSET_REGEX = "[-_a-zA-Z0-9+][-\\._a-zA-Z0-9+]*";
    public static final String VALID_RESOURCE_NAME_REGEX = "^"+VALID_RESOURCE_NAME_CHARSET_REGEX+"$";

    final Logger logger;

    /**
     * Constructor
     *
     * @param name Name of resource
     * @param dir  Base directory of resource
     * @param parent The parent resource
     */
    public FrameworkResource(final String name, final File dir, final IFrameworkResourceParent parent) {
        if ("".equals(name)) {
            throw new IllegalArgumentException("empty string cannot be used as a name");
        }
        this.name = name;
        baseDir = dir;
        this.parent = parent;
        logger = Logger.getLogger(this.getClass().getName());
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * constains name of resource. The name should be unique for its
     * {@link FrameworkResourceParent} if it has one.
     */
    private final String name;


    /**
     * @return name property
     *
     */
    public String getName() {
        return name;
    }

    /**
     * points to the base directory of resource
     */
    private final File baseDir;

    /**
     * @return baseDir
     */
    public File getBaseDir() {
        return baseDir;
    }


    private IFrameworkResourceParent parent;

    public IFrameworkResourceParent getParent() {
        return parent;
    }

    public boolean isValid() {
        return getParent().childCouldBeLoaded(getName());
    }


    public String toString() {
        return name;
    }

    /**
     * Store properties to disk
     *
     * @param props Properties to store
     * @param file  File to write property data to
     * @throws IOException thrown if file not found or can't write to file
     */
    protected void storeProperties(final Properties props, final File file) throws IOException {
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            props.store(fileOutputStream, "auto generated. do not edit.");
        } finally {
            fileOutputStream.close();
        }
    }
}
