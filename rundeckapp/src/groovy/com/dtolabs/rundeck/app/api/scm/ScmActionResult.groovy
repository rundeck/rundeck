package com.dtolabs.rundeck.app.api.scm

/**
 * Created by greg on 10/28/15.
 */
class ScmActionResult {
    String message
    boolean success
    String nextAction
    Map<String,String> validationErrors
}
