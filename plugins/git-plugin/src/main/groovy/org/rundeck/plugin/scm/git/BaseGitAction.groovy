package org.rundeck.plugin.scm.git

/**
 * Created by greg on 9/8/15.
 */
abstract class BaseGitAction implements GitAction {
    String id

    BaseGitAction(String id) {
        this.id = id
    }

}
