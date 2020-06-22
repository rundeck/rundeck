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

public enum Features {
    ENHANCED_NODES("enhancedNodes"),
    REPOSITORY("repository"),
    WEBHOOKS("webhooks"),
    CLEAN_EXECUTION_HISTORY("cleanExecutionHistoryJob"),
    LEGACY_PROJECT_NODES_UI("legacyProjectNodesUi"),
    OPTION_VALUES_PLUGIN("optionValuesPlugin"),
    EMAIL_CSS_FRAMEWORK("emailCSSFramework"),
    WORKFLOW_DYNAMIC_STEP_SUMMARY_GUI("workflowDynamicStepSummaryGUI"),
    JOB_LIFECYCLE_PLUGIN("jobLifecyclePlugin"),
    EXECUTION_LIFECYCLE_PLUGIN("executionLifecyclePlugin");

    private final String propertyName;

    Features(final String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
