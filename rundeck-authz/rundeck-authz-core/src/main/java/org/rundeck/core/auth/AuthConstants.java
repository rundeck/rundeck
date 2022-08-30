/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.core.auth;

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;

import java.util.Collections;
import java.util.Map;

public class AuthConstants {
    public static final String CTX_APPLICATION = "application";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_READ = "read";
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_RUN = "run";
    public static final String ACTION_KILL = "kill";
    public static final String ACTION_ADMIN = "admin";
    public static final String ACTION_OPS_ADMIN = "ops_admin";
    public static final String ACTION_APP_ADMIN = "app_admin";
    public static final String ACTION_GENERATE_USER_TOKEN = "generate_user_token";
    public static final String ACTION_GENERATE_SERVICE_TOKEN = "generate_service_token";
    public static final String ACTION_RUNNER_PING = "ping";
    public static final String ACTION_RUNNER_REGENERATE_CREDENTIALS = "regenerate_credentials";
    public static final String ACTION_REFRESH = "refresh";
    public static final String ACTION_RUNAS = "runAs";
    public static final String ACTION_KILLAS = "killAs";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_IMPORT = "import";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_INSTALL = "install";
    public static final String ACTION_UNINSTALL = "uninstall";
    public static final String ACTION_DELETE_EXECUTION = "delete_execution";
    public static final String ACTION_ENABLE_EXECUTIONS = "enable_executions";
    public static final String ACTION_DISABLE_EXECUTIONS = "disable_executions";
    public static final String ACTION_TOGGLE_SCHEDULE = "toggle_schedule";
    public static final String ACTION_TOGGLE_EXECUTION = "toggle_execution";
    public static final String ACTION_SCM_UPDATE="scm_update";
    public static final String ACTION_SCM_CREATE="scm_create";
    public static final String ACTION_SCM_DELETE="scm_delete";
    public static final String ACTION_SCM_IMPORT = "scm_import";
    public static final String ACTION_SCM_EXPORT = "scm_export";
    public static final String ACTION_PROMOTE = "promote";
    public static final String ACTION_POST = "post";

    public static final String VIEW_HISTORY = "view_history";

    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_SYSTEM_ACL = "system_acl";
    public static final String TYPE_NODE = "node";
    public static final String TYPE_JOB = "job";
    public static final String TYPE_APITOKEN = "apitoken";
    public static final String TYPE_ADHOC = "adhoc";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_PROJECT_ACL = "project_acl";
    public static final String TYPE_PLUGIN = "plugin";
    public static final String TYPE_RUNNER = "runner";
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_USER = "user";
    public static final String TYPE_STORAGE = "storage";
    public static final String TYPE_WEBHOOK = "webhook";
    public static final String TYPE_RESOURCE = "resource";

    public static final Map<String, String> RESOURCE_TYPE_SYSTEM = AuthorizationUtil.resourceType(TYPE_SYSTEM);
    public static final Map<String, String> RESOURCE_TYPE_SYSTEM_ACL = AuthorizationUtil.resourceType(TYPE_SYSTEM_ACL);
    public static final Map<String, String> RESOURCE_TYPE_NODE = AuthorizationUtil.resourceType(TYPE_NODE);
    public static final Map<String, String> RESOURCE_TYPE_JOB = AuthorizationUtil.resourceType(TYPE_JOB);
    public static final Map<String, String> RESOURCE_TYPE_EVENT = AuthorizationUtil.resourceType(TYPE_EVENT);
    public static final Map<String, String> RESOURCE_TYPE_WEBHOOK = AuthorizationUtil.resourceType(TYPE_WEBHOOK);
    public static final Map<String, String> RESOURCE_TYPE_PLUGIN = AuthorizationUtil.resourceType(TYPE_PLUGIN);
    public static final Map<String, String> RESOURCE_TYPE_USER = AuthorizationUtil.resourceType(TYPE_USER);
    public static final Map<String, String> RESOURCE_TYPE_APITOKEN = AuthorizationUtil.resourceType(TYPE_APITOKEN);
    public static final Map<String, String> RESOURCE_ADHOC = Collections.unmodifiableMap(AuthorizationUtil
            .resource(TYPE_ADHOC));
}
