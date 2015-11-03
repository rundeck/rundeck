package com.dtolabs.rundeck.app.api.scm

/**
 * Created by greg on 11/2/15.
 */
class ScmCommit {
    /**
     * @return Commit ID as a string
     */
    String commitId

    /**
     * @return Commit message
     */
    String message;

    /**
     * @return author
     */
    String author;

    /**
     * @return commit date
     */
    Date date

    Map<String,Object> info
}
