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

package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Provides metadata about plugin files
 */
public interface PluginMetadata {
    /**
     * @return Name of file on disk
     */
    String getFilename();

    /**
     * @return Source file on disk
     */
    File getFile();

    /**
     * @return author metadata
     */
    String getPluginAuthor();

    /**
     * @return plugin file version
     */
    String getPluginFileVersion();

    /**
     * @return rundeck plugin format version
     */
    String getPluginVersion();

    /**
     * @return plugin URL
     */
    String getPluginUrl();

    /**
     * @return build date of plugin
     */
    Date getPluginDate();

    /**
     * @return date loaded
     */
    Date getDateLoaded();

    //Plugin Metadata Version 2.0 Fields
    /**
     * @return plugin name
     */
    String getPluginName();
    /**
     * @return plugin description
     */
    String getPluginDescription();

    /**
     * This id is generated off of the plugin name. It is the first 12 characters of the sha256 of the plugin name.
     * This is set automatically by the system. No need to add it manually.
     * @return plugin id
     */
    String getPluginId();

    /**
     * @return version of Rundeck with which this plugin is compatible
     */
    String getRundeckCompatibilityVersion();

    /**
     * @return os family with which this plugin is compatible
     */
    String getTargetHostCompatibility();

    /**
     * @return tags
     */
    List<String> getTags();

    /**
     * @return plugin license
     */
    String getPluginLicense();

    /**
     * @return plugin third party dependencies
     */
    String getPluginThirdPartyDependencies();

    /**
     * @return plugin third party dependencies
     */
    String getPluginSourceLink();

    /**
     * @return plugin type
     */
    String getPluginType();
}
