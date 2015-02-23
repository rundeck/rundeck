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

import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.Collection;
import java.util.Properties;

/**
 * A set of interfaces for managing a set of Projects
 */
public interface IFrameworkProjectMgr extends ProjectManager {

    /**
     * Create a new project. This also creates its structure
     *
     * @param projectName Name of project
     * @return newly created {@link FrameworkProject}
     */
    FrameworkProject createFSFrameworkProject(String projectName);
}
