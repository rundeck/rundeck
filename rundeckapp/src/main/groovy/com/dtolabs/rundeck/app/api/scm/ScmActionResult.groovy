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

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Created by greg on 10/28/15.
 */
@Schema
class ScmActionResult {
    @Schema(description='Status message')
    String message
    @Schema(description = 'true if successful, false otherwise')
    boolean success
    @Schema(description = 'Name of the next `action` that should be invoked.')
    String nextAction
    @Schema(description = 'Validation errors, keyed by input field name.')
    Map<String,String> validationErrors
}
