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

package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import groovy.transform.CompileStatic

/**
 * Created by greg on 10/13/15.
 */
class Import extends Common{

    @PluginProperty(
            title = "Import UUID Behavior",
            description = '''How to handle UUIDs from imported Job source files

* `preserve` - Preserve the Source UUID as the Job UUID
* `archive` - Remove the Source UUID but keep it for use in Export. Allows you to use `${job.sourceId}` in the "File
 Path Template" instead of `${job.id}`.
* `remove` - Remove the source UUID
 ''',
            defaultValue = 'preserve',
            required = false
    )
    @SelectValues(values = ['preserve', 'archive', 'remove'])
    String importUuidBehavior


    boolean isImportPreserve() {
        importUuidBehavior == 'preserve' || !importUuidBehavior
    }
    boolean isImportArchive() {
        importUuidBehavior == 'archive'
    }
    boolean isImportRemove() {
        importUuidBehavior == 'remove'
    }
}
