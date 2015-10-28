package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString

/**
 * Created by greg on 10/27/15.
 */
class ScmPluginDescription {
    String type
    String title
    CDataString description
    boolean configured
    boolean enabled
}
