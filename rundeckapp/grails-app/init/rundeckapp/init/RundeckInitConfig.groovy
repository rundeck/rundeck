/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init

import rundeckapp.cli.RundeckCliOptions

class RundeckInitConfig {

    static final String SYS_PROP_RUNDECK_BASE_DIR           = "rdeck.base"
    static final String SYS_PROP_RUNDECK_PROJECTS_DIR       = "rdeck.projects"
    static final String SYS_PROP_LOGIN_MODULE               = "loginmodule.name";
    static final String SYS_PROP_ROLE_CLASS_NAMES           = "loginmodule.role.classnames";
    static final String SYS_PROP_WEB_CONTEXT                = "server.web.context";
    static final String SYS_PROP_RUNDECK_JAASLOGIN          = "rundeck.jaaslogin";
    static final String SYS_PROP_RUNDECK_SERVER_SERVER_DIR  = "rundeck.server.serverDir";
    static final String SYS_PROP_RUNDECK_SERVER_CONFIG_DIR  = "rundeck.server.configDir";
    static final String SYS_PROP_RUNDECK_SERVER_DATA_DIR    = "rundeck.server.dataDir";
    static final String SYS_PROP_RUNDECK_SERVER_WORK_DIR    = "rundeck.server.workDir";
    static final String SYS_PROP_RUNDECK_CONFIG_LOCATION    = "rundeck.config.location";

    static final String RUNDECK_CONFIG_NAME_PROP            = "rundeck.config.name"
    static final String DEFAULT_JAAS_LOGIN_MODULE           = "rundecklogin"
    static final String DEFAULT_WEB_APP_CONTEXT             = "/"
    static final String SERVER_DATASTORE_PATH               = "server.datastore.path";
    static final String LOG_DIR                             = "rundeck.log.dir"
    static final String LAUNCHER_JAR_LOCATION               = "rundeck.launcher.jar.location"

    RundeckCliOptions cliOptions = new RundeckCliOptions()
    boolean useJaas
    String loginModuleName
    String roleClassNames
    String appContext
    String appVersion
    Properties runtimeConfiguration

    RundeckInitConfig() {
        useJaas = null != System.getProperty(SYS_PROP_RUNDECK_JAASLOGIN) || Boolean.getBoolean(SYS_PROP_RUNDECK_JAASLOGIN);
        loginModuleName = System.getProperty(SYS_PROP_LOGIN_MODULE, DEFAULT_JAAS_LOGIN_MODULE);
        roleClassNames = System.getProperty(SYS_PROP_ROLE_CLASS_NAMES, null);
        appContext = System.getProperty(SYS_PROP_WEB_CONTEXT, DEFAULT_WEB_APP_CONTEXT);
    }

    String getBaseDir() {
        cliOptions.baseDir ?: System.getProperty(SYS_PROP_RUNDECK_BASE_DIR)
    }

    String getServerBaseDir() {
        cliOptions.serverBaseDir ?: System.getProperty(SYS_PROP_RUNDECK_SERVER_SERVER_DIR)
    }

    String getConfigDir() {
        cliOptions.configDir ?: System.getProperty(SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)
    }

    String getDataDir() {
        cliOptions.dataDir ?: System.getProperty(SYS_PROP_RUNDECK_SERVER_DATA_DIR)
    }

    String getWorkDir() {
        System.getProperty(SYS_PROP_RUNDECK_SERVER_WORK_DIR, serverBaseDir+"/work")
    }

    boolean isSkipInstall() {
        cliOptions.skipInstall
    }

    boolean isInstallOnly() {
        cliOptions.installOnly
    }
}
