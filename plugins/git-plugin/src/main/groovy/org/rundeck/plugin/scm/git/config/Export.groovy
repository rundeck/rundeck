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

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import groovy.transform.CompileStatic

/**
 * Created by greg on 10/13/15.
 */
class Export extends Common {

    @PluginProperty(
            title = 'Committer Name',
            description = '''Name of committer/author of changes.

Can be set to `${user.login}` (login name), or `${user.firstName} ${user.lastName}` or
`${user.fullName}` to expand as the name
of the committing user.''',
            defaultValue = '${user.fullName}',
            required = true
    )
    String committerName

    @PluginProperty(
            title = "Committer Email",
            description = '''Email of committer/author of changes.

Can be set to `${user.email}` to expand
as the email of the committing user''',
            defaultValue = '${user.email}',
            required = true

    )
    String committerEmail

    @PluginProperty(
            title = "Export UUID Behavior",
            description = '''How to handle UUIDs for exported Job source files

* `preserve` - Write the Job UUID into exported Jobs, and as `${job.id}` in the "File Path Template"
* `original` - Write the imported Source UUID into exported Jobs, and use it as the `${job.sourceId}` in the "File Path
Template".
* `remove` - Do not write a UUID into the exported Jobs.
''',
            defaultValue = 'preserve',
            required = false
    )
    @SelectValues(values = ['preserve', 'original', 'remove'])
    String exportUuidBehavior

    @PluginProperty(
            title = "Synchronize Automatically",
            description = "Automatically pull remote changes on automatic fetch. If false, you can perform it manually",
            defaultValue = 'false',
            required = false
    )
    @SelectValues(values = ['true', 'false'])
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Git Repository"
    )
    String pullAutomatically

    boolean isExportPreserve() {
        exportUuidBehavior == 'preserve' || !exportUuidBehavior
    }
    boolean isExportOriginal() {
        exportUuidBehavior == 'original'
    }
    boolean isExportRemove() {
        exportUuidBehavior == 'remove'
    }

    boolean shouldPullAutomatically(){
        return pullAutomatically in ['true']
    }
}
