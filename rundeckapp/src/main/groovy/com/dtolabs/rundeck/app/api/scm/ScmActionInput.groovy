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

package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString
import com.dtolabs.rundeck.app.api.marshall.ApiResource
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Action input data
 */
@ApiResource
@Schema
class ScmActionInput {
    @Schema(description = 'ID for the action')
    String actionId

    @Schema(allowableValues = ['import','export'])
    String integration
    @Schema(description = 'Display title for the action')
    String title

    @Schema(type='string')
    CDataString description
    List<ScmPluginInputField> fields

    List<ScmImportActionItem> importItems
    List<ScmExportActionItem> exportItems
}
