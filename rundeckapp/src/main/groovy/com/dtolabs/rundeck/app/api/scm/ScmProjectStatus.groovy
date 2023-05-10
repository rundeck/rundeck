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
 * Created by greg on 10/29/15.
 */
@Schema
class ScmProjectStatus {
    String project
    @Schema(allowableValues = ['import','export'])
    String integration
    @Schema(description = '''Indicates the state.

Import plugin values for `synchState`:

* `CLEAN` - no changes
* `UNKNOWN` - status unknown
* `REFRESH_NEEDED` - plugin needs to refresh
* `IMPORT_NEEDED` - some changes need to be imported
* `DELETE_NEEDED` - some jobs need to be deleted

Export plugin values for `synchState`:

* `CLEAN` - no changes
* `REFRESH_NEEDED` - plugin needs to refresh
* `EXPORT_NEEDED` - some changes need to be exported
* `CREATE_NEEDED` - some jobs need to be added to the repo
''', allowableValues = [
        'CLEAN',
        'UNKNOWN',
        'REFRESH_NEEDED',
        'IMPORT_NEEDED',
        'DELETE_NEEDED',
        'EXPORT_NEEDED',
        'CREATE_NEEDED'
    ])
    String synchState
    String message
    @Schema(description = '''empty, or a list of action ID strings.''')
    List<String> actions

}
