package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString

/**
 * Output for scm diff request
 */
class ScmJobDiff  {
    String id
    String project
    String integration
    ScmCommit commit
    ScmCommit incomingCommit
    CDataString diffContent
}
