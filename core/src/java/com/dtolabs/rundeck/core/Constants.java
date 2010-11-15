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


/**
 * Manifest constants for Ctl
 * NOTE:  the following bootstrapping constants are derived from java runtime via java properties:<br/>
 * JAVA_HOME<br/>
 * RDECK_HOME<br/>
 * RDECK_BASE<br/>
 * ANT_HOME<br/>
 * MAIN_CLASSNAME<br/>
 */

public final class Constants {

    public static final String PARM_ACTION = "params.action";
    public static final String PARM_PROJECT = "context.project";
    public static final String PARM_TYPE = "context.type";
    public static final String PARM_OBJECT = "context.name";
    public static final String PARM_USER = "context.user";

    public static final String FWK_NODE = "framework.node.hostname";
    public static final String FWK_CMD_INV_ID = "framework.command.invocation.id";

    public static final String MOD_CAT_NAME = "module.logger.name";
    public static final String PROPERTY_MOD_MAPREF_FIELD = "module.mapref-uri";

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

    public static String MAP_CONTENT_OBJECTS = "instances";
    public static String MAP_CONTENT_TYPES = "types";
    public static String MAP_CONTENT_COMBINED = "combined";

    public static final String FILEPATHACTION_HANDLER = "handler";
    public static final String FILEPATHACTION_PROPERTIES = "properties";
    public static final String FILEPATHACTION_BASEDIR = "basedir";
    public static final String LISTACTION_PROJECTS = "projects";
    public static final String LISTACTION_TYPES = "types";
    public static final String LISTACTION_OBJECTS = "objects";
    public static final String LISTACTION_COMMANDS = "commands";
    /**
     * ant execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_ANT = "ant";
    /**
     * antfetch execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_ANTFETCH = "antfetch";
    /**
     * exec execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_EXEC = "exec";
   /**
     * localdispatch execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_LOCALDISPATCH = "localdispatch";
   /**
     * localfetch execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_LOCALFETCH = "localfetch";    
    /**
     * nodedispatch execution strategy
     */
    public static final String EXECUTEACTION_STRATEGY_NODEDISPATCH = "nodedispatch";

    public static final String UPDATEPROPERTIES_FLAG = "controller.updateproperties.flag";

    public static final String PROPERTYSORT_VALUE = "value";
    public static final String PROPERTYSORT_NAME = "name";
    public static final String PROPERTYSORT_ASCENDING = "ascending";
    public static final String PROPERTYSORT_DESCENDING = "descending";
    public static final String PROPERTY_RES_TYPE_FIELD = "type";
    public static final String PROPERTY_RES_NAME_FIELD = "name";
    public static final String PROPERTY_RES_DOC_FIELD = "doc";
    public static final String PROPERTY_RES_MAPREF_FIELD = "mapref-uri";
    public static final String PROPERTY_RES_PROX_FIELD = "target-proximity";
    public static final String PROPERTY_RES_PROXCNSTRT_FIELD = "proximity-constraint";
    public static final String PROPERTY_RES_DIRCNSTRT_FIELD = "direction-constraint";

    public static final String PROPERTY_DEP_RUNLEVEL_FIELD = "runlevel";
    public static final String PROPERTY_DEP_BASEDIR_FIELD = "basedir";
    public static final String PROPERTY_DEP_CONTROLLER_FIELD = "controller-module";

    public static final String PROPERTY_NODE_OSFAMILY_FIELD = "os-family";
    public static final String PROPERTY_NODE_OSNAME_FIELD = "os-name";
    public static final String PROPERTY_NODE_OSVERSION_FIELD = "os-version";
    public static final String PROPERTY_NODE_OSARCH_FIELD = "os-arch";
    public static final String PROPERTY_NODE_HOSTNAME_FIELD = "hostname";

    public static final String PROPERTY_PKG_ARCH_FIELD = "package-arch";
    public static final String PROPERTY_PKG_BASE_FIELD = "package-base";
    public static final String PROPERTY_PKG_BUILDTIME_FIELD = "package-buildtime";
    public static final String PROPERTY_PKG_FILENAME_FIELD = "package-filename";
    public static final String PROPERTY_PKG_FILETYPE_FIELD = "package-filetype";
    public static final String PROPERTY_PKG_RELEASE_FIELD = "package-release";
    public static final String PROPERTY_PKG_RELEASETAG_FIELD = "package-release-tag";
    public static final String PROPERTY_PKG_RESTART_FIELD = "package-restart";
    public static final String PROPERTY_PKG_VENDOR_FIELD = "package-vendor";
    public static final String PROPERTY_PKG_VERSION_FIELD = "package-version";
    public static final String PROPERTY_PKG_INSTALLROOT_FIELD = "package-install-root";
    public static final String PROPERTY_PKG_INSTALLRANK_FIELD = "package-install-rank";

    public static final String PROPERTY_PKG_REPOURL_FIELD = "package-repo-url";

    public static final String PROPERTY_DOC_EXISTS_FIELD = "exists";
    public static final String PROPERTY_DOC_FILETYPE_FIELD = "filetype";
    public static final String PROPERTY_DOC_MODDATE_FIELD = "modify-date";
    public static final String PROPERTY_DOC_OUTPUTDIR_FIELD = "outputdir";
    public static final String PROPERTY_DOC_TEMPLATE_FIELD = "template";
    public static final String PROPERTY_DOC_TEMPLATEDIR_FIELD = "templatedir";
    public static final String PROPERTY_DOC_TEMPLATETYPE_FIELD = "templatetype";
    public static final String PROPERTY_DOC_NAMES_FIELD = "names";
    public static final String PROPERTY_DOC_MANAGEMENT_VIEW_FIELD = "management-view";

    public static final String PROPERTY_MOD_SUPER_FIELD = "supermodule";
    public static final String PROPERTY_MOD_SUPERS_FIELD = "supermodules";
    public static final String PROPERTY_MOD_COMMANDS_FIELD = "commands";
    public static final String PROPERTY_MOD_VERSION_FIELD = "version";
    public static final String PROPERTY_MOD_INHERITED_FIELD = "inherited";
    public static final String PROPERTY_MOD_DESC_FIELD = "module.description";

    public static final String MODTASK_ACTION_UPTODATE = "uptodate";
    public static final String PROPERTY_RESOURCE_CTRLMODHEADREV = "entity.controller-module-head-revision";

    public static final String PROPERTY_SELECT_BY_NAME = "name";
    public static final String PROPERTY_SELECT_BY_VALUE = "value";

    public static final String PROPERTY_SET_VALUE_FIELD = "value";
    public static final String PROPERTY_SET_TYPE_FIELD = "settingType";
    /**
     * Task definitions
     * list of taskdefinitions that are available to command handlers
     */
    public static final String[] TASK_PROPS = {
        "/com/dtolabs/rundeck/core/tasks/taskdef.properties"
    };
    /**
     * Datatype definitions
     * list of dataype definitions that are available to command handlers
     */
    public static final String[] TYPE_PROPS = {
        "/com/dtolabs/rundeck/core/types/typedef.properties"
    };

    /**
     * rdeck paralleladdtask classname
     */
    public static final String PARALLELADD_TASK_CLSNAME = "com.dtolabs.rundeck.core.tasks.ParallelAdd";


    /*
     * Bootstrapping Constants
     * These Constants are seeded by java Properties that will exist via setup and cmdr shell scripts 
     */
    // file separator
    public static final String FILE_SEP = System.getProperty("file.separator");

    // java.home
    public static final String JAVA_HOME = System.getProperty("java.home");

    /**
     * Get the value of the "rdeck.home" system property, home directory of the rdeck installation
     */
    public static final String getSystemHomeDir(){
        return System.getProperty("rdeck.home");
    }

    /**
     * RDECK_BASE, base directory patch for instance of client, equivalent to ${rdeck.base}
     */
    public static final String getSystemBaseDir(){
        return System.getProperty("rdeck.base");
    }


    /* ******************************************************************************  */

    /* ******* The rest of these Constants mostly depend on the bootstrapping constants above */

    /**
     * the framework configuration directory
     * @param rdeck_base Ctl Base directory
     */
    public static final String getFrameworkConfigDir(final String rdeck_base) {
        return System.getProperty("rdeck.config", rdeck_base + FILE_SEP + "etc");
    }

    /**
     * Standard Ctl configuration properties
     */
    public static final String[] FRAMEWORK_CONFIG_PROPERTIES = {
        "framework.properties",
        "project.properties",
        "modules.properties",
        "resource.properties"
    };

    /**
     * path to framework.properties file
     * @param rdeck_base Ctl Base directory
     */
    public static final String getFrameworkProperties(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "framework.properties";
    }

    /**
     * get path to log4j.properties file
     * @param rdeck_base Ctl Base directory
     */
    public static final String getLog4jProperties(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "log4j.properties";
    }

    /**
     * return the templates/etc dir for rdeck install
     * @param rdeck_home Ctl Home directory
     * @return
     */
    public static final String getHomeTemplatesEtcDir(final String rdeck_home) {
        return rdeck_home + FILE_SEP + ("lib" + FILE_SEP + "templates" + FILE_SEP + "etc");
    }
    /**
     * path to the rdeck framework defaults file for template filterization
     * @param rdeck_home Ctl Home directory
     */
    public static final String getDefaultsPropertiesName() {
        return "run-defaults.properties";
    }


    /**
     * path to the framework preferences file for setup
     * @param rdeck_base Ctl Base directory
     */
    public static final String getFrameworkPreferences(final String rdeck_base) {
        return getFrameworkConfigDir(rdeck_base) + FILE_SEP + "preferences.properties";
    }

    // list of allowed property types generated in the preferences.properties file
    // this includes standard java system props and rdeck
    // ensure this is sorted alphabeticaly
    public static final String[] PREFS_ALLOWED_PROP_TYPES = {
        "file",
        "framework",
        "java",
        "jndi",
        "line",
        "modules",
        "os",
        "path",
        "user",
        "workbench"
    };


    /**
     * the var subdir for RDECK_BASE, containing framework logs, etc
     * @param rdeck_base Ctl Base directory
     */
    public static final String getBaseVar(final String rdeck_base) {
        return rdeck_base + FILE_SEP + "var";
    }

    /**
     * Return framework projects dir
     * @param rdeck_base Ctl Base directory
     * @return
     * @see Constants#getFrameworkProjectsDir(String)
     */
    @Deprecated
    public static final String getFrameworkDepotsDir(final String rdeck_base) {
        return getFrameworkProjectsDir(rdeck_base);
    }
    
    /**
     * Return framework projects dir
     * @param rdeck_base Ctl Base directory
     * @return
     */
    public static final String getFrameworkProjectsDir(final String rdeck_base) {
        return System.getProperty("rdeck.projects", rdeck_base + FILE_SEP + "projects");
    }

    /**
     * Return framework logs dir
     * @param rdeck_base Ctl Base directory
     * @return
     */
    public static final String getFrameworkLogsDir(final String rdeck_base) {
        return getBaseVar(rdeck_base) + FILE_SEP + "logs";
    }

    /**
     * return framework modules dir of installation
     * @param rdeck_base Ctl Base directory
     * @return
     */
    public static final String getFrameworkModulesDir(final String rdeck_base) {
        return rdeck_base + FILE_SEP + "modules";
    }

    /**
     * Return modules dir of rdeck home
     * @param rdeck_home Ctl Home directory
     * @return
     * @deprecated Use {@link #getFrameworkModulesDir}
     */
    public static final String getModulesDir(final String rdeck_home) {
        return rdeck_home + FILE_SEP + "modules";
    }

    /**
     * Return modules templates dir
     * @param rdeck_home Ctl Home directory
     * @return
     */
    public static final String getModulesTemplateDir(final String rdeck_home) {
        return rdeck_home + FILE_SEP + ("lib" + FILE_SEP + "templates" + FILE_SEP + "ant");
    }

    // framework.exts property name
    public static final String FRAMEWORK_EXTS_PROPERTY = "framework.exts";

    // framework.ext. property prefix name
    public static final String FRAMEWORK_EXT_PROPERTY_PREFIX = "framework.ext.";

    // framework.application.libpath property name
    public static final String FRAMEWORK_APPLICATION_LIBPATH_PROPERTY = "framework.application.libpath";

    // jndi properties
    public static final String JNDI_CONNECTION_NAME_PROPERTY = "jndi.connectionName";
    public static final String JNDI_CONNECTION_PASSWORD_PROPERTY = "jndi.connectionPassword";
    public static final String JNDI_CONNECTION_URL_PROPERTY = "jndi.connectionUrl";
    public static final String JNDI_ROLEBASE_PROPERTY = "jndi.roleBase";
    public static final String JNDI_ROLENAMERDN_PROPERTY = "jndi.roleNameRDN";
    public static final String JNDI_ROLEMEMBERRDN_PROPERTY = "jndi.roleMemberRDN";
    public static final String JNDI_USERBASE_PROPERTY = "jndi.userBase";
    public static final String JNDI_USERNAMERDN_PROPERTY = "jndi.userNameRDN";
    public static final String JNDI_RESOURCE_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";


    public static final String DEFAULT_NODE_AUTHSTRATEGY_CLASSNAME = "com.dtolabs.rundeck.core.authentication.DefaultNodeAuthResolutionStrategy";

    /**
     * SSH related properties
     */
    public static final String Ctl_SHELL_PROP = "framework.rdeck.shell";
    public static final String SSH_KEYPATH_PROP = "framework.ssh.keypath";
    public static final String SSH_USER_PROP = "framework.ssh.user";
    public static final String SSH_TIMEOUT_PROP = "framework.ssh.timeout";

    public static String formatCanonicalPath(final String filepath) {
        if (null==filepath) throw new IllegalArgumentException("null filepath argument");
        return filepath.replaceAll("\\\\","/");
    }
}
