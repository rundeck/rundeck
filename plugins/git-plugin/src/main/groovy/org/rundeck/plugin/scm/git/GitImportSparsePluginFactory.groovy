package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.rundeck.plugin.scm.git.config.Common
import org.rundeck.plugin.scm.git.config.Config
import org.rundeck.plugin.scm.git.config.SparseImport

import static BuilderUtil.pluginDescription

@Plugin(name = GitImportSparsePluginFactory.PROVIDER_NAME, service = ServiceNameConstants.ScmImport)
@PluginDescription(title = GitImportSparsePluginFactory.TITLE, description = GitImportSparsePluginFactory.DESC)
class GitImportSparsePluginFactory implements ScmImportPluginFactory, Describable {
    static final String PROVIDER_NAME = 'git-import-sparse'
    public static final String DESC = "Import Jobs from a Git Repository (Sparse Checkout)"
    public static final String TITLE = "Git Import Sparse"

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
        Common.addDirDefaultValue setupProperties, basedir, ServiceNameConstants.ScmImport
    }

    static List<Property> getSetupProperties() {
        Config.listProperties SparseImport
    }

    @Override
    ScmImportPlugin createPlugin(
            final ScmOperationContext context,
            final Map<String, String> input,
            final List<String> trackedItems
    ) {
        return createPlugin(context, input, trackedItems, true)
    }

    @Override
    ScmImportPlugin createPlugin(
            final ScmOperationContext context,
            final Map<String, String> input,
            final List<String> trackedItems,
            final boolean initialize
    ) {
        def config = Config.create SparseImport, input
        def plugin = new GitImportSparsePlugin(config, trackedItems)
        if (initialize) {
            plugin.initialize context
        }
        return plugin
    }
} 