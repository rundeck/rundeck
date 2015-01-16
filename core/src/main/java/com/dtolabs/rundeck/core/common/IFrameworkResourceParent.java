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

import java.util.Collection;

/**
 * a set of interfaces for managing child {@link IFrameworkResource} instances. Each child is keyed by
 * its name.
 */
public interface IFrameworkResourceParent extends IFrameworkResource {
    /**
     * Create a new FrameworkResource child.
     *
     * @param name name of child
     * @return new child
     */
    IFrameworkResource createChild(String name);

    /**
     * Gets specified child
     *
     * @param name Name of child
     * @return child instance
     */
    IFrameworkResource getChild(String name);


    /**
     * Checks if a child by that name exists
     *
     * @param name key to lookup child
     * @return true if a child by that name exists
     */
    boolean existsChild(String name);

    /**
     * @return true if the child resource could be loaded from a file resource
     * @param name child name
     */
    boolean childCouldBeLoaded(String name);


    /**
     * @return Load a specified child by name, returning null if it does not exist
     *
     * @param name child name
     */
    IFrameworkResource loadChild(String name);

    /**
     * List all children.
     *
     * @return A Collection of {@link IFrameworkResource} children
     */
    Collection listChildren();

    /**
     * List all child names.
     *
     * @return A Collection of Strings
     */
    Collection listChildNames();

    /**
     * Remove the child's base directory
     *
     * @param name          Name of child
     */
    void remove(String name);

    /**
     * initialize the parent. This may ask it to load in all children
     */
    void initialize();

    /**
     * Checks if a child's base dir exists
     *
     * @param name name of child
     * @return true if it exists
     */
    boolean existsChildResourceDirectory(String name);
}
