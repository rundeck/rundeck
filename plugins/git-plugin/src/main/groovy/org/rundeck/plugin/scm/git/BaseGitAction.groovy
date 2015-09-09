package org.rundeck.plugin.scm.git

/**
 * Created by greg on 9/8/15.
 */
abstract class BaseGitAction implements GitAction {
    String id
    String title
    String description

    BaseGitAction(String id, String title, String description) {
        this.id = id
        this.title = title
        this.description = description
    }

}
