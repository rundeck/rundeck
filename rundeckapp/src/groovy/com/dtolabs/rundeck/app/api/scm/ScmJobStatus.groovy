package com.dtolabs.rundeck.app.api.scm


/**
 * Created by greg on 11/2/15.
 */
class ScmJobStatus {
    String id
    String project
    String integration
    String synchState
    String message
    List<String> actions
    ScmCommit commit
}
