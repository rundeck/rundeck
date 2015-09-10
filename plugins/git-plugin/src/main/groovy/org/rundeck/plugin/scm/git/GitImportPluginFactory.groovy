package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

import java.util.regex.Pattern

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
        def builder = DescriptionBuilder.builder().
                name(PROVIDER_NAME).
                title(TITLE).
                description(DESC)
        getSetupProperties().each {
            builder.property(it)
        }

        return builder.build()
    }

    List<Property> getSetupPropertiesForBasedir(File basedir) {

        [
                PropertyBuilder.builder().with {
                    string "dir"
                    title "Base Directory"
                    description "Directory for checkout"
                    required true
                    defaultValue null != basedir ? new File(basedir, 'scm').absolutePath : 'scm'
                    build()
                }
        ] + getSetupProperties()
    }

    List<Property> getSetupProperties() {
        [
                PropertyBuilder.builder().with {
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
                    build()
                },
                PropertyBuilder.builder().with {
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
                    build()
                },
                PropertyBuilder.builder().with {
                    string "branch"
                    title "Branch"
                    description "Checkout branch"
                    required true
                    defaultValue "master"
                    build()
                },
                //TODO: enable SSH

                PropertyBuilder.builder().with {
                    select "format"
                    title "Format"
                    description "Format for serializing Job definitions"
                    values 'xml', 'yaml'
                    defaultValue 'xml'
                    required true
                    build()
                },

        ]
    }
    static List<String> requiredProperties = ['dir', 'pathTemplate', 'branch', 'url']

    @Override
    ScmImportPlugin createPlugin(final Map<String, ?> input, final String project) {
        def plugin = new GitImportPlugin(input, project)
        plugin.initialize()
        return plugin
    }
}
