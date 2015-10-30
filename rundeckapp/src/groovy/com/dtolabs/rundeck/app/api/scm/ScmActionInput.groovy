package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString

/**
 * Created by greg on 10/29/15.
 */
class ScmActionInput {
    String actionId
    String integration
    String title
    CDataString description
    List<ScmPluginInputField> fields
}
