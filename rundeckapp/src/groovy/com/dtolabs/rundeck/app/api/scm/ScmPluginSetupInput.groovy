package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.core.plugins.configuration.Property

/**
 * Created by greg on 10/27/15.
 */
class ScmPluginSetupInput {
    String type
    String integration
    List<ScmPluginInputField> fields
}
