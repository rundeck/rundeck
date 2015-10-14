package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty

/**
 * Created by greg on 10/13/15.
 */
class Export extends Common {

    @PluginProperty(
            title = 'Committer Name',
            description = '''Name of committer/author of changes.

Can be set to `${user.firstName} ${user.lastName}` or
`${user.fullName}` to expand as the name
of the committing user.''',
            defaultValue = '${user.fullName}',
            required = true
    )
    String committerName

    @PluginProperty(
            title = "Committer Email",
            description = '''Email of committer/author of changes.

Can be set to `${user.email}` to expand
as the email of the committing user''',
            defaultValue = '${user.email}',
            required = true

    )
    String committerEmail


}
