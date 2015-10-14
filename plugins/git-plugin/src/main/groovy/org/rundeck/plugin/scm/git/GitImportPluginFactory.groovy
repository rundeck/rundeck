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
import org.rundeck.plugin.scm.git.config.Import

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
        Common.addDirDefaultValue setupProperties, basedir
    }


    static List<Property> getSetupProperties() {
        Config.listProperties Import
    }

    @Override
    ScmImportPlugin createPlugin(
            final ScmOperationContext context,
            final Map<String, String> input,
            final List<String> trackedItems
    )
    {
        def config = Config.create Import, input
        def plugin = new GitImportPlugin(config, trackedItems)
        plugin.initialize context
        return plugin
    }
}
