package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.rundeck.plugin.scm.git.config.Common
import org.rundeck.plugin.scm.git.config.Config
import org.rundeck.plugin.scm.git.config.Export

import static BuilderUtil.pluginDescription
import static BuilderUtil.property

/**
 * Factory for git export plugin
 */
@Plugin(name = GitExportPluginFactory.PROVIDER_NAME, service = ServiceNameConstants.ScmExport)
@PluginDescription(title = GitExportPluginFactory.TITLE, description = GitExportPluginFactory.DESC)
class GitExportPluginFactory implements ScmExportPluginFactory, Describable {
    static final String PROVIDER_NAME = 'git-export'
    public static final String DESC = "Export Jobs to a Git Repository"
    public static final String TITLE = "Git Export"

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
        Common.addDirDefaultValue(getSetupProperties(), basedir)
    }


    static List<Property> getSetupProperties() {
        Config.listProperties(Export)
    }

    @Override
    ScmExportPlugin createPlugin(final ScmOperationContext context, final Map<String, String> input) {
        def config = Config.create(Export, input)
        def plugin = new GitExportPlugin(config)
        plugin.initialize(context)
        return plugin
    }
}
