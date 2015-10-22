package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectValues

import java.util.regex.Pattern

/**
 * Common configuration class
 */
class Common extends Config {
    @PluginProperty(
            title = "Base Directory",
            description = "Directory for checkout",
            required = true
    )
    String dir

    static class PathTemplateValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            value ==~ ('^.*' + Pattern.quote('${job.id}') + '.*$')
        }
    }

    @PluginProperty(
            title = "File Path Template",
            description = '''Path template for storing a Job to a file within the base dir.

Available expansion patterns:

* `${job.name}` - the job name
* `${job.group}` - blank, or `path/`
* `${job.project} - project name`
* `${job.id}` - job UUID (this value *should* be included in the template to guarantee a unique path for each job.)
* `${config.format}` - Serialization format chosen below.
''',
            defaultValue = '${job.group}${job.name}-${job.id}.${config.format}',
            required = true
    )
    String pathTemplate

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
            required = true
    )
    String url

    @PluginProperty(
            title = "Branch",
            description = "Checkout branch",
            required = true,
            defaultValue = "master"
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
    String format


    static List<Property> addDirDefaultValue(List<Property> properties, File basedir) {
        if (null == basedir) {
            return properties
        }
        substituteDefaultValue properties, 'dir', new File(basedir, 'scm').absolutePath
    }
}
