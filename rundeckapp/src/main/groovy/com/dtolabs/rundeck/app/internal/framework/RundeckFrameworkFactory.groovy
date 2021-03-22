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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by greg on 2/20/15.
 */
class RundeckFrameworkFactory {
    public static final String PROJ_STORAGE_TYPE_FILESYSTEM = 'filesystem'
    public static final String PROJ_STORAGE_TYPE_FILE = 'file'
    public static final String PROJ_STORAGE_TYPE_DB = 'db'
    public static final Set<String> STORAGE_TYPES = Collections.unmodifiableSet(
        [PROJ_STORAGE_TYPE_DB] as Set
    )
    public static final Logger logger = LoggerFactory.getLogger(RundeckFrameworkFactory)
    FilesystemFramework frameworkFilesystem
    String type
    ProjectManager dbProjectManager
    IPropertyLookup propertyLookup
    PluginManagerService pluginManagerService

    Framework createFramework() {
        Map<String, FrameworkSupportService> services = [(PluginManagerService.SERVICE_NAME): pluginManagerService]

        if (isFSType(type)) {
            logger.warn(
                "Invalid value for rundeck.projectsStorageType configuration: $type, The '$type' value is no " +
                "longer supported. You should remove the rundeck.projectsStorageType configuration. Using 'db'" +
                " storage type."
            )
            type = PROJ_STORAGE_TYPE_DB
        }
        if (type == PROJ_STORAGE_TYPE_DB) {
            logger.info("Creating DB project manager")
            return FrameworkFactory.createFramework(propertyLookup, frameworkFilesystem, dbProjectManager, services)
        } else {
            throw new IllegalArgumentException(
                "Invalid config value for: rundeck.projectsStorageType: $type, expected one of: $STORAGE_TYPES"
            )
        }
    }

    public static boolean isFSType(String type1) {
        type1 in [PROJ_STORAGE_TYPE_FILESYSTEM, PROJ_STORAGE_TYPE_FILE]
    }
}
