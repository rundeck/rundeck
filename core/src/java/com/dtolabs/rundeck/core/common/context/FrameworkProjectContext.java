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

package com.dtolabs.rundeck.core.common.context;

import com.dtolabs.rundeck.core.utils.ToStringFormatter;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents Depot context
 */
public class FrameworkProjectContext implements IDepotContext {
    private String project;

    /**
     * Base constructor
     */
    FrameworkProjectContext() {
    }

    /**
     * Constructor.
     * @param name  name of project
     */
    FrameworkProjectContext(final String name) {
        this();
        project = name;
    }

    /**
     * Factory method.
     * @param name    name of project
     * @return a new instance
     */
    public static FrameworkProjectContext create(final String name) {
        return new FrameworkProjectContext(name);
    }

    /**
     * Returns name of project
     *
     * @return project name
     */
    public String getFrameworkProject() {
        return project;
    }
     /**
      * Setter for project
      * @param projectName name of project
      */
    public void setFrameworkProject(final String projectName) {
        project = projectName;
    }

    /**
     * Returns true if a project has been specified
     */
    public boolean isDepotContext() {
        return project != null && !"".equals(project);
    }

    /**
     * Checks if no contextual info has been specified
     *
     * @return true if nothing specified
     */
    public boolean isEmptyContext() {
        return null == project || "".equals(project);
    }

          /**
     * Returns fields as a map of key value pairs
     * @return
     */
    protected Map toMap() {
       final Map map = new HashMap();
        map.put("project", project);
        //map.put("isDepotContext", Boolean.toString(isDepotContext()));
        return map;
    }

    /**
     * Returns fields as a formatted set of key value pairs
     * @return formatted string
     */
    public String toString() {
        return ToStringFormatter.create(this, toMap()).toString();
    }
}
