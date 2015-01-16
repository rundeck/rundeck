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

package com.dtolabs.rundeck.core;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Constants for Rundeck Core
 */

public final class Constants {
    public static final String MSG_DEBUG = "debug";
    public static final String MSG_ERR = "error";
    public static final String MSG_INFO = "info";
    public static final String MSG_WARN = "warn";
    public static final String MSG_VERBOSE = "verbose";
    public static final int ERR_LEVEL = 0;
    public static final int WARN_LEVEL = 1;
    public static final int INFO_LEVEL = 2;
    public static final int VERBOSE_LEVEL = 3;
    public static final int DEBUG_LEVEL = 4;

    /*
     * Bootstrapping Constants
     * These Constants are seeded by java Properties that will exist via setup and cmdr shell scripts 
     */
    // file separator
    public static final String FILE_SEP = System.getProperty("file.separator");

    // java.home
    public static final String JAVA_HOME = System.getProperty("java.home");

    /**
     * @return RDECK_BASE, base directory patch for instance of client, equivalent to ${rdeck.base}
     */
    public static String getSystemBaseDir() {
        return System.getProperty("rdeck.base");
    }

    /* ******************************************************************************  */

    /* ******* The rest of these Constants mostly depend on the bootstrapping constants above */

    /**
     * @return the framework configuration directory
     *
     * @param rdeck_base Ctl Base directory
     */
    public static String getFrameworkConfigDir(final String rdeck_base) {
        return System.getProperty("rdeck.config", rdeck_base + FILE_SEP + "etc");
    }

    /**
     * @return configDirectory.
     */
    public static String getFrameworkConfigDir() {
        return getFrameworkConfigDir(getSystemBaseDir());
    }

    public static File getFrameworkConfigFile() {
        return new File(getFrameworkConfigDir());
    }

    /**
     * @return path to framework.properties file
     *
     * @param rdeck_base Ctl Base directory
     */
    public static String getFrameworkProperties(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "framework.properties";
    }

    /**
     * @return get path to log4j.properties file
     *
     * @param rdeck_base Ctl Base directory
     */
    public static String getLog4jProperties(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "log4j.properties";
    }

    public static String getLog4jProperties() {
        return getLog4jProperties(getSystemBaseDir());
    }

    public static File getLog4jPropertiesFile() {
        return new File(getLog4jProperties());
    }

    /**
     * @return path to the rdeck framework defaults file for template filterization
     */
    public static String getDefaultsPropertiesName() {
        return "run-defaults.properties";
    }


    /**
     * @return path to the framework preferences file for setup
     *
     * @param rdeck_base Ctl Base directory
     */
    public static String getFrameworkPreferences(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "preferences.properties";
    }


    /**
     * @return the var subdir for RDECK_BASE, containing framework logs, etc
     *
     * @param rdeck_base Ctl Base directory
     */
    public static String getBaseVar(final String rdeck_base) {
        return rdeck_base + FILE_SEP + "var";
    }
    
    /**
     * @return the scratch directory used for temporary storage located within the 
     * the directory returned by {@link getSystemBaseDir}
     */
    public static String getBaseTempDirectory() {
        return getBaseTempDirectory(getSystemBaseDir());
    }

    /**
     * @return the scratch directory used for temporary storage located within the 
     * the provided rdeck_base param
     * 
     * @param rdeck_base the prefix location for the scratch directory
     */
    public static String getBaseTempDirectory(final String rdeck_base) {
        return getBaseVar(rdeck_base) + FILE_SEP + "tmp";
    }

    /**
     * @return framework projects dir
     *
     * @param rdeck_base Ctl Base directory
     *
     */
    public static String getFrameworkProjectsDir(final String rdeck_base) {
        return System.getProperty("rdeck.projects", rdeck_base + FILE_SEP + "projects");
    }

    /**
     * @return framework logs dir
     *
     * @param rdeck_base Ctl Base directory
     *
     */
    public static String getFrameworkLogsDir(final String rdeck_base) {
        return System.getProperty("rdeck.runlogs", getBaseVar(rdeck_base) + FILE_SEP + "logs");
    }

    public static final String DEFAULT_NODE_AUTHSTRATEGY_CLASSNAME =
        "com.dtolabs.rundeck.core.authentication.DefaultNodeAuthResolutionStrategy";

    /**
     * SSH related properties
     */
    public static final String SSH_KEYPATH_PROP = "framework.ssh.keypath";
    public static final String SSH_KEYRESOURCE_PROP = "framework.ssh.key.resource";
    public static final String SSH_USER_PROP = "framework.ssh.user";
    public static final String SSH_TIMEOUT_PROP = "framework.ssh.timeout";

}
