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
import org.rundeck.plugin.scm.git.config.SparseExport

import static BuilderUtil.pluginDescription
import static BuilderUtil.property

@Plugin(name = GitExportSparsePluginFactory.PROVIDER_NAME, service = ServiceNameConstants.ScmExport)
@PluginDescription(title = GitExportSparsePluginFactory.TITLE, description = GitExportSparsePluginFactory.DESC)
class GitExportSparsePluginFactory implements ScmExportPluginFactory, Describable {
    static final String PROVIDER_NAME = 'git-export-sparse'
    public static final String DESC = "Export Jobs to a Git Repository with Sparse Checkout"
    public static final String TITLE = "Git Export Sparse"

    @Override
    Description getDescription() {
        pluginDescription {
            name PROVIDER_NAME
            title TITLE
            description DESC
            metadata('fabicon', 'git-alt')
            def del = delegate
            setupProperties.each {
                del.property it
            }
        }
    }

    List<Property> getSetupPropertiesForBasedir(File basedir) {
        Common.addDirDefaultValue(getSetupProperties(), basedir, ServiceNameConstants.ScmExport)
    }

    static List<Property> getSetupProperties() {
        Config.listProperties(SparseExport)
    }

    @Override
    ScmExportPlugin createPlugin(final ScmOperationContext context, final Map<String, String> input) {
        return createPlugin(context, input, true)
    }

    @Override
    ScmExportPlugin createPlugin(final ScmOperationContext context, final Map<String, String> input, boolean initialize) {
        def config = Config.create(SparseExport, input)
        def plugin = new GitExportSparsePlugin(config)
        if(initialize){
            plugin.initialize(context)
        }
        return plugin
    }
} 