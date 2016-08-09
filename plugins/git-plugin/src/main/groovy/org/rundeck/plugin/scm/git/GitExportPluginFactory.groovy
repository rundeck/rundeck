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
