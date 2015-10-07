package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext

import java.util.regex.Pattern

import static BuilderUtil.pluginDescription
import static BuilderUtil.property

/**
 * Created by greg on 9/9/15.
 */
@Plugin(name = GitImportPluginFactory.PROVIDER_NAME, service = ServiceNameConstants.ScmImport)
@PluginDescription(title = GitImportPluginFactory.TITLE, description = GitImportPluginFactory.DESC)
class GitImportPluginFactory implements ScmImportPluginFactory, Describable {
    static final String PROVIDER_NAME = 'git-import'
    public static final String DESC = "Import Jobs from a Git Repository"
    public static final String TITLE = "Git Import"


    @Override
    Description getDescription() {
        pluginDescription {
            name PROVIDER_NAME
            title TITLE
            description DESC
            def del = delegate
            setupProperties.each {
                del.property it
            }
        }
    }

    List<Property> getSetupPropertiesForBasedir(File basedir) {

        [
                property {
                    string "dir"
                    title "Base Directory"
                    description "Directory for checkout"
                    required true
                    defaultValue null != basedir ? new File(basedir, 'scm').absolutePath : 'scm'
                }
        ] + getSetupProperties()
    }

    List<Property> getSetupProperties() {
        [
                property {
                    string "pathTemplate"
                    title("File Path Template")
                    description '''Path template for storing a Job to a file within the base dir.

Available expansion patterns:

* `${job.name}` - the job name
* `${job.group}` - blank, or `path/`
* `${job.project} - project name`
* `${job.id}` - job UUID
'''
                    defaultValue '${job.group}${job.name}-${job.id}.xml'
                    required true
                    validator({ it ==~ ('^.*' + Pattern.quote('${job.id}') + '.*$') } as PropertyValidator)
                },
                property {
                    string "url"
                    title "Git URL"
                    description '''Checkout url.

See [git-clone](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html)
specifically the [GIT URLS](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS) section.

Some examples:

* `ssh://[user@]host.xz[:port]/path/to/repo.git/`
* `git://host.xz[:port]/path/to/repo.git/`
* `http[s]://host.xz[:port]/path/to/repo.git/`
* `ftp[s]://host.xz[:port]/path/to/repo.git/`
* `rsync://host.xz/path/to/repo.git/`'''
                    required true
                },
                property {
                    string "branch"
                    title "Branch"
                    description "Checkout branch"
                    required true
                    defaultValue "master"
                },

                property {
                    string BaseGitPlugin.SSH_PRIVATE_KEY_PATH
                    title "SSH Key Storage Path"
                    description '''Path can include variable references

* `${user.login}` login name of logged in user
* `${project}` current project name'''
                    renderingOptions(
                            [
                                    (StringRenderingConstants.SELECTION_ACCESSOR_KEY)      : StringRenderingConstants.SelectionAccessor.STORAGE_PATH,
                                    (StringRenderingConstants.STORAGE_PATH_ROOT_KEY)       : "keys",
                                    (StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY): "Rundeck-key-type=private",

                            ]
                    )
                },
                property {
                    string BaseGitPlugin.GIT_PASSWORD_PATH
                    title "Password Storage Path"
                    description '''Password to authenticate remotely (e.g. for SSH or HTTPS URLs).

Path can include variable references

* `${user.login}` login name of logged in user
* `${project}` current project name'''
                    renderingOptions(
                            [
                                    (StringRenderingConstants.SELECTION_ACCESSOR_KEY)      : StringRenderingConstants.SelectionAccessor.STORAGE_PATH,
                                    (StringRenderingConstants.STORAGE_PATH_ROOT_KEY)       : "keys",
                                    (StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY): "Rundeck-data-type=password",

                            ]
                    )
                },
                property {
                    select "format"
                    title "Format"
                    description "Format for serializing Job definitions"
                    values 'xml', 'yaml'
                    defaultValue 'xml'
                    required true
                },

        ]
    }
    static List<String> requiredProperties = ['dir', 'pathTemplate', 'branch', 'url']

    @Override
    ScmImportPlugin createPlugin(
            final ScmOperationContext context,
            final Map<String, String> input,
            final List<String> trackedItems
    )
    {
        def plugin = new GitImportPlugin(input, trackedItems, context.frameworkProject)
        plugin.initialize(context)
        return plugin
    }
}
