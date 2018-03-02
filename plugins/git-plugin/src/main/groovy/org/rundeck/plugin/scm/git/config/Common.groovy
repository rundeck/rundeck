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
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * Common configuration class
 */
class Common extends Config {
    @PluginProperty(
            title = "File Path Template",
            description = '''Path template for storing a Job to a file within the base dir.

Available expansion patterns:

* `${job.name}` - the job name
* `${job.group}` - blank, or `path/`
* `${job.project}` - project name
* `${job.id}` - job UUID
* `${job.sourceId}` - Original Job UUID from imported source (see *Strip Job UUID*)
* `${config.format}` - Serialization format chosen below.

If you set `Strip Job UUID` to true, then you most likely do not want to include `${job.id}` in the expansion pattern,
as it the job UUID after import will be different than the one on disk.
''',
            defaultValue = '${job.group}${job.name}-${job.id}.${config.format}',
            required = true
    )
    @SelectValues(
            values = ['${job.group}${job.name}-${job.id}.${config.format}',
                    '${job.group}${job.name}-${job.sourceId}.${config.format}',
                    '${job.group}${job.name}.${config.format}'],
            freeSelect = true
    )
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Job Source Files"
    )
    String pathTemplate

    @PluginProperty(
            title = "Base Directory",
            description = "Directory for checkout",
            required = true
    )
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Git Repository"
    )
    String dir

    static class PathTemplateValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            value ==~ ('^.*' + Pattern.quote('${job.id}') + '.*$')
        }
    }


    @PluginProperty(
            title = "Git URL",
            description = '''Checkout url.

See [git-clone](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html)
specifically the [GIT URLS](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS) section.

Some examples:

* `ssh://[user@]host.xz[:port]/path/to/repo.git/`
* `git://host.xz[:port]/path/to/repo.git/`
* `http[s]://host.xz[:port]/path/to/repo.git/`
* `ftp[s]://host.xz[:port]/path/to/repo.git/`
* `rsync://host.xz/path/to/repo.git/`''',
            required = true,
            validatorClass = GitURLValidator
    )
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Git Repository"
    )
    String url

    @PluginProperty(
            title = "Branch",
            description = "Checkout branch",
            required = true,
            defaultValue = "master"
    )
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Git Repository"
    )
    String branch

    @PluginProperty(
            title = "SSH: Strict Host Key Checking",
            description = '''Use strict host key checking.

If `yes`, require remote host SSH key is defined in the `~/.ssh/known_hosts` file, otherwise do not verify.''',
            required = true,
            defaultValue = 'yes'
    )
    @SelectValues(values = ['yes', 'no'])
    @RenderingOptions([

            @RenderingOption(
                    key = StringRenderingConstants.GROUP_NAME,
                    value = "Authentication"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.GROUPING,
                    value = "secondary"
            )
    ])
    String strictHostKeyChecking

    @PluginProperty(

            title = "SSH Key Storage Path",
            description = '''Path can include variable references

* `${user.login}` login name of logged in user
* `${project}` current project name'''
    )
    @RenderingOptions(
            [
                    @RenderingOption(
                            key = StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                            value = 'STORAGE_PATH'
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY,
                            value = 'keys'
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY,
                            value = "Rundeck-key-type=private"
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.GROUP_NAME,
                            value = "Authentication"
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.GROUPING,
                            value = "secondary"
                    )
            ]
    )
    String sshPrivateKeyPath

    @PluginProperty(
            title = 'Password Storage Path',
            description = '''Password to authenticate remotely (e.g. for SSH or HTTPS URLs).

Path can include variable references

* `${user.login}` login name of logged in user
* `${project}` current project name'''
    )
    @RenderingOptions(
            [
                    @RenderingOption(
                            key = StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                            value = 'STORAGE_PATH'
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY,
                            value = 'keys'
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY,
                            value = "Rundeck-data-type=password"
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.GROUP_NAME,
                            value = "Authentication"
                    ),
                    @RenderingOption(
                            key = StringRenderingConstants.GROUPING,
                            value = "secondary"
                    )
            ]
    )
    String gitPasswordPath

    @PluginProperty(
            title = "Format",
            description = "Format for serializing Job definitions",
            defaultValue = 'xml',
            required = true
    )
    @SelectValues(values = ['xml', 'yaml'])
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Job Source Files"
    )
    String format

    @PluginProperty(
            title = "Fetch Automatically",
            description = "Automatically fetch remote changes for local comparison. If false, you can perform it manually",
            defaultValue = 'true',
            required = false
    )
    @SelectValues(values = ['true', 'false'])
    @RenderingOption(
            key = StringRenderingConstants.GROUP_NAME,
            value = "Git Repository"
    )
    String fetchAutomatically

    boolean shouldFetchAutomatically(){
        return fetchAutomatically in [null,'true']
    }


    static List<Property> addDirDefaultValue(List<Property> properties, File basedir) {
        if (null == basedir) {
            return properties
        }
        substituteDefaultValue properties, 'dir', new File(basedir, 'scm').absolutePath
    }
}
