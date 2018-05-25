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

package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.common.FilesystemFramework
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkFactory
import com.dtolabs.rundeck.core.common.FrameworkSupportService
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import org.apache.log4j.Logger

/**
 * Created by greg on 2/20/15.
 */
class RundeckFrameworkFactory {
    public static final String PROJ_STORAGE_TYPE_FILESYSTEM = 'filesystem'
    public static final String PROJ_STORAGE_TYPE_FILE = 'file'
    public static final String PROJ_STORAGE_TYPE_DB = 'db'
    public static final Set<String> STORAGE_TYPES = Collections.unmodifiableSet(
        [PROJ_STORAGE_TYPE_FILESYSTEM, PROJ_STORAGE_TYPE_DB] as Set
    )
    public static final Logger logger = Logger.getLogger(RundeckFrameworkFactory)
    FilesystemFramework frameworkFilesystem
    String type
    ProjectManager dbProjectManager
    ProjectManager filesystemProjectManager
    IPropertyLookup propertyLookup
    PluginManagerService pluginManagerService

    Framework createFramework() {
        Map<String, FrameworkSupportService> services = [(PluginManagerService.SERVICE_NAME): pluginManagerService]

        if (type in [PROJ_STORAGE_TYPE_FILESYSTEM, PROJ_STORAGE_TYPE_FILE]) {
            logger.info("Creating Filesystem project manager")
            return FrameworkFactory.createFramework(
                    propertyLookup,
                    frameworkFilesystem,
                    filesystemProjectManager,
                    services
            )
        } else if (type == PROJ_STORAGE_TYPE_DB) {
            logger.info("Creating DB project manager")
            return FrameworkFactory.createFramework(propertyLookup, frameworkFilesystem, dbProjectManager, services)
        } else {
            throw new IllegalArgumentException(
                "Invalid config value for: rundeck.projectsStorageType: $type, expected one of: $STORAGE_TYPES"
            )
        }
    }
}
