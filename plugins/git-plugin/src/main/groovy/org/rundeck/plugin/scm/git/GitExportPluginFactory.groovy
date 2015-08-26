package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.scm.PluginState
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.StoredPluginState
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

/**
 * Factory for git export plugin
 */
@Plugin(name = GitExportPluginFactory.PROVIDER_NAME, service = ServiceNameConstants.ScmExport)
@PluginDescription(title = GitExportPluginFactory.TITLE,description = GitExportPluginFactory.DESC)
class GitExportPluginFactory implements ScmExportPluginFactory, Describable {
    static final String PROVIDER_NAME = 'git-export'
    public static final String DESC = "Export Jobs to a Git Repository"
    public static final String TITLE = "Git Export"

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
                    defaultValue(null != basedir ? new File(basedir, 'scm').absolutePath : 'scm')
                    build()
                }
        ] + getSetupProperties()
    }
    List<Property> getSetupProperties() {
        [
                 PropertyBuilder.builder().with {
                    string("pathTemplate")
                    title("File Path Template")
                    description("Path template for storing a Job to a file within the base dir.\n\n" +
                                        "Available expansion patterns:\n\n" +
                                        "* `\${job.name}` - the job name\n" +
                                        "* `\${job.group}` - blank, or `path/`\n" +
                                        "* `\${job.project} - project name`\n" +
                                        "* `\${job.id}` - job UUID\n"
                    )
                    defaultValue('${job.group}${job.name}-${job.id}.xml')
                    required(true)
                    build()
                },
                PropertyBuilder.builder()
                               .string("url")
                               .title("Git URL")
                               .description('''Checkout url.

See [git-clone](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html)
specifically the [GIT URLS](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS) section.

Some examples:

* `ssh://[user@]host.xz[:port]/path/to/repo.git/`
* `git://host.xz[:port]/path/to/repo.git/`
* `http[s]://host.xz[:port]/path/to/repo.git/`
* `ftp[s]://host.xz[:port]/path/to/repo.git/`
* `rsync://host.xz/path/to/repo.git/`'''
                )
                               .required(true)
                               .build(),
                PropertyBuilder.builder()
                               .string("branch")
                               .title("Branch")
                               .description("Checkout branch")
                               .required(true)
                               .defaultValue("master")
                               .build(),
                PropertyBuilder.builder().
                        string("sshPrivateKeyPath").
                        title("SSH Key Storage Path").
                        description('''Path can include variable references

* `${username}` login name of logged in user
* `${project}` current project name'''
                        ).
                        renderingOptions(
                                [
                                        (StringRenderingConstants.SELECTION_ACCESSOR_KEY): StringRenderingConstants.SelectionAccessor.STORAGE_PATH,
                                        (StringRenderingConstants.STORAGE_PATH_ROOT_KEY): "keys",
                                        (StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY): "Rundeck-key-type=private",

                                ]
                        ).
                        build(),
                PropertyBuilder.builder().
                        string("sshPasswordPath").
                        title("SSH Password Storage Path").
                        description('''Path can include variable references

* `${username}` login name of logged in user
* `${project}` current project name'''
                        ).
                        renderingOptions(
                                [
                                        (StringRenderingConstants.SELECTION_ACCESSOR_KEY): StringRenderingConstants.SelectionAccessor.STORAGE_PATH,
                                        (StringRenderingConstants.STORAGE_PATH_ROOT_KEY): "keys",
                                        (StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY): "Rundeck-data-type=password",

                                ]
                        ).
                        build(),
                PropertyBuilder.builder()
                               .string("committerName")
                               .title("Committer Name")
                               .description("Name of committer/author of changes")
                               .required(true)
                               .build(),
                PropertyBuilder.builder()
                               .string("committerEmail")
                               .title("Committer Email")
                               .description("Email of committer/author of changes")
                               .required(true)
                               .build(),

                 PropertyBuilder.builder().with {
                     select "format"
                     title "Format"
                     description "Format for serializing Job definitions"
                     values 'xml','yaml'
                     defaultValue 'xml'
                     required true
                     build()
                 },

        ]
    }

    @Override
    ScmExportPlugin createPlugin(final Map<String, ?> input, final String project) {
        def plugin=new GitExportPlugin(input, project)
        plugin.initialize()
        return plugin
    }
}
