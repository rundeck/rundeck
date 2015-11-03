package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString

/**
 * Action input data
 */
class ScmActionInput {
    String actionId
    String integration
    String title
    CDataString description
    List<ScmPluginInputField> fields

    List<ScmImportActionItem> importItems
    List<ScmExportActionItem> exportItems
}
