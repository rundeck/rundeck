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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;

/**
 * Created by greg on 2/19/15.
 */
public interface IFilesystemFramework {

    /**
     * @return the config dir
     */
    File getConfigDir();

    File getFrameworkProjectsBaseDir();

    /**
     * @return the directory containing plugins/extensions for the framework.
     */
    File getLibextDir();

    /**
     * @return the cache directory used by the plugin system
     */
    File getLibextCacheDir();

    /**
     * @return a framework property lookup for this basedir
     */
    IPropertyLookup getPropertyLookup();

    File getBaseDir();
}
