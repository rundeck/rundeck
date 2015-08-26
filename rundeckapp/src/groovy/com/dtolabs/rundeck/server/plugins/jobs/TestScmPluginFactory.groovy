package com.dtolabs.rundeck.server.plugins.jobs

import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.scm.PluginState
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

/**
 * Created by greg on 8/21/15.
 */
class TestScmPluginFactory implements Describable {
    @Override
    Description getDescription() {
        def builder=DescriptionBuilder.builder().name("test-scm-export").title("Test Export").description("Test")
        getSetupProperties().each{
            builder.property(it)
        }

        return builder.build()
    }

    List<Property> getSetupProperties() {
        [
                PropertyBuilder.builder()
                               .string("url")
                               .title("Git URL")
                               .description("Checkout url.\n\n" +
                                                    "See [git-clone](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html) " +
                                                    "specifically the [GIT URLS](https://www.kernel.org/pub/software/scm/git/docs/git-clone.html#URLS) section.\n\n" +
                                                    "Some examples:\n\n" +
                                                    "* `ssh://[user@]host.xz[:port]/path/to/repo.git/`\n" +
                                                    "\n" +
                                                    "* `git://host.xz[:port]/path/to/repo.git/`\n" +
                                                    "\n" +
                                                    "* `http[s]://host.xz[:port]/path/to/repo.git/`\n" +
                                                    "\n" +
                                                    "* `ftp[s]://host.xz[:port]/path/to/repo.git/`\n" +
                                                    "\n" +
                                                    "* `rsync://host.xz/path/to/repo.git/`"
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
                        description("Path can include variable references\n\n" +
                                            "* `\${username}` login name of logged in user\n" +
                                            "* `\${project}` current project name").
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
                        description("Path can include variable references\n\n" +
                                            "* `\${username}` login name of logged in user\n" +
                                            "* `\${project}` current project name").
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

        ]
    }

//    @Override
    ScmExportPlugin createPlugin(final Map<String, ?> input, final PluginState data, final String project) {
        new TestScmPlugin(input, data, project)
    }
}
