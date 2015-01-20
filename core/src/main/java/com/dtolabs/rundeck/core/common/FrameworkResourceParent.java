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

import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


/**
 * Provides capability of managing child {@link FrameworkResource} instances.
 * <br>
 */
public abstract class FrameworkResourceParent extends FrameworkResource implements IFrameworkResourceParent {
    /**
     * Constructor
     *
     * @param name Name of resource
     * @param dir  Base directory of resource
     * @param parent parent
     */
    public FrameworkResourceParent(final String name, final File dir, final IFrameworkResourceParent parent) {
        super(name, dir, parent);
    }


    /**
     * create a new child resource
     */
    public IFrameworkResource createChild(final String name) {
        if(!name.matches(VALID_RESOURCE_NAME_REGEX)) {
            throw new IllegalArgumentException("Child resource name \"" + name + "\" does not match: " +
                    VALID_RESOURCE_NAME_REGEX);
        }
        FrameworkResource resource = new FrameworkResource(name, new File(getBaseDir(), name), this);
        if(!resource.getBaseDir().mkdirs()) {
            logger.warn("Unable to create basedir for resource: " + resource.getBaseDir());
        }
        return resource;
    }

    /**
     * Gets requested child FrameworkResource. Throws a {@link FrameworkResourceException} if not found.
     *
     */
    public IFrameworkResource getChild(final String name) {
        if(childCouldBeLoaded(name)) {
            IFrameworkResource o = loadChild(name);
            if(null!=o){
                return o;
            }
        }
        throw new NoSuchResourceException("Framework resource not found: '" + name +"'"
                + ". basedir: " + getBaseDir().getAbsolutePath(), this);
    }

    protected Map getChildren() {
        HashMap map = new HashMap();
        for (Iterator i = listChildNames().iterator(); i.hasNext();) {
            String name = (String) i.next();
            try {
                map.put(name, getChild(name));
            } catch (NoSuchResourceException e) {
            }
        }
        return map;
    }

    public static class NoSuchResourceException extends FrameworkResourceException{

        NoSuchResourceException(final String message, final IFrameworkResource resource) {
            super(message, resource);
        }

        NoSuchResourceException(final String message, final FrameworkResource resource, final Throwable cause) {
            super(message, resource, cause);
        }
    }


    /**
     * Checks if there is a child FrameworkResource with specified name.
     *
     */
    public boolean existsChild(final String name) {
        if (null == name) {
            throw new IllegalArgumentException("name parameter for framework resource was null");
        }
        return childCouldBeLoaded(name);
    }

    /**
     * Returns a collection of {@link FrameworkResource} child resources.
     *
     */
    public Collection listChildren() {
        return getChildren().values();
    }

    /**
     * Remove the resource by its name.
     *
     * @param name          Name of object
     */
    public void remove(final String name) {
        if (existsChild(name)) {
            final IFrameworkResource resource = getChild(name);
            if (resource.getBaseDir().exists()) {
                FileUtils.deleteDir(resource.getBaseDir());
            }
        }
    }

    /**
     * Initializes state from any loaded children. This implementation does nothing.
     */
    public void initialize() {

    }

    /**
     * Default implementation lists the subdirectory names and adds any existing child names
     */
    public Collection listChildNames() {
        HashSet childnames = new HashSet();
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches(VALID_RESOURCE_NAME_REGEX);
            }
        };
        final String[] list = getBaseDir().list(filter);
        if(null!=list){
            for (int i = 0; i < list.length; i++) {
                //
                // add new child
                final File projectDir = new File(getBaseDir(), list[i]);
                final String resName = projectDir.getName();
                if (childCouldBeLoaded(resName)) {
                    childnames.add(resName);
                }
            }
        }
        return childnames;
    }


    /**
     * Default implementation checks whether a subdir under the basedir exists with the specified name.
     * (Calls {@link #existsChildResourceDirectory(String)})
     * Should be overridden by subtypes if this is not the desired behavior.
     */
    public boolean childCouldBeLoaded(String name) {
        return existsChildResourceDirectory(name);
    }

    /**
     * Given a child resource name, checks if a subdirectory by that name exists
     *
     * @param name Child resource name
     * @return true if the directory exists
     */
    public boolean existsChildResourceDirectory(final String name) {
        if (null == name) throw new IllegalArgumentException("name parameter was null");
        final File file = new File(getBaseDir(), name);
        return file.exists() && file.isDirectory();
    }
}
