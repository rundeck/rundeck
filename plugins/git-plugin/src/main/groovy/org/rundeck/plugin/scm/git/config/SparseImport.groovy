package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption

class SparseImport extends Import {
    
    @PluginProperty(
        title = "Jobs Directory",
        description = "Directory path within the repository containing job definitions (e.g., 'jobs' or 'rundeck/jobs')",
        required = true
    )
    @RenderingOption(
        key = StringRenderingConstants.GROUP_NAME,
        value = "Setup"
    )
    String jobsDirectory
} 