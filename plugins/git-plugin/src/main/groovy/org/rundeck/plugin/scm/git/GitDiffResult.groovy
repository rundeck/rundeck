package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ScmDiffResult

/**
 * Created by greg on 8/25/15.
 */
class GitDiffResult implements ScmDiffResult {
    String contentType
    String content
}
