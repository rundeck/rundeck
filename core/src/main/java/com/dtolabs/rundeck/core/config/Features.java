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
package com.dtolabs.rundeck.core.config;

public enum Features implements FeaturesDefinition{
    ENHANCED_NODES("enhancedNodes"),
    REPOSITORY("repository"),
    WEBHOOKS("webhooks"),
    CLEAN_EXECUTIONS_HISTORY("cleanExecutionsHistoryJob"),
    CLEAN_EXECUTIONS_HISTORY_ASYNC_START("cleanExecutionsHistoryJobAsyncStart"),
    LEGACY_PROJECT_NODES_UI("legacyProjectNodesUi"),
    OPTION_VALUES_PLUGIN("optionValuesPlugin"),
    EMAIL_CSS_FRAMEWORK("emailCSSFramework"),
    WORKFLOW_DYNAMIC_STEP_SUMMARY_GUI("workflowDynamicStepSummaryGUI"),
    JOB_LIFECYCLE_PLUGIN("jobLifecyclePlugin"),
    EXECUTION_LIFECYCLE_PLUGIN("executionLifecyclePlugin"),
    SIDEBAR_PROJECT_LISTING("sidebarProjectListing"),
    USER_SESSION_PROJECTS_CACHE("userSessionProjectsCache"),
    AUTH_SVC_BOOTSTRAP_WARMUP_CACHE("authorizationServiceBootstrapWarmupCache"),
    PROJMGR_SVC_BOOTSTRAP_WARMUP_CACHE("projectManagerServiceBootstrapWarmupCache"),
    NOTIFICATIONS_OWN_THREAD("notificationsOwnThread"),
    EVENT_STORE("eventStore"),
    PLUGIN_SECURITY("pluginSecurity"),
    PROJECT_KEY_STORAGE("projectKeyStorage"),
    FILE_UPLOAD_PLUGIN("fileUploadPlugin"),
    PLUGIN_GROUPS("pluginGroups"),
    VUE_KEY_STORAGE("vueKeyStorage"),
    LEGACY_UI("legacyUi"),
    LEGACY_XML("legacyXml");

    private final String propertyName;

    Features(final String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
