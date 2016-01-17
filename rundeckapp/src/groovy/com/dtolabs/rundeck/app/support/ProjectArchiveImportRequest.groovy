package com.dtolabs.rundeck.app.support

/**
 * Created by greg on 8/11/15.
 */
interface ProjectArchiveImportRequest {
    String getProject()
    String getJobUuidOption()
    Boolean getImportExecutions()
    Boolean getImportConfig()
    Boolean getImportACL()

}