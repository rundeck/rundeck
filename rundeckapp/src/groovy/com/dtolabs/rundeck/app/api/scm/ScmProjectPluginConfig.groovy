package com.dtolabs.rundeck.app.api.scm

/**
 * Created by greg on 10/29/15.
 */
class ScmProjectPluginConfig {
    String integration
    String project
    String type
    boolean enabled

    Map<String, String> config
}
