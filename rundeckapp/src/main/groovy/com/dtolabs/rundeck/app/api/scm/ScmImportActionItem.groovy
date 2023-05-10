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

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ApiVersion
import com.dtolabs.rundeck.app.api.marshall.Ignore
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Created by greg on 10/30/15.
 */
@ApiResource
@Schema
class ScmImportActionItem {

    @Schema(description = 'ID of the repo item, e.g. a file path')
    String itemId
    JobReference job

    @Schema(description = 'true if there is an associated `job`')
    boolean tracked

    @ApiVersion(22)
    @Ignore(onlyIfNull = true)
    @Schema(description = 'whether the job was deleted on remote and requires to be deleted')
    boolean deleted

    @ApiVersion(22)
    @Ignore(onlyIfNull = true)
    @Schema(description = 'file status String, the same value as in the `synchState` of Job Scm Status result.')
    String status
}
