package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.ScmCommitInfo

/**
 * Created by greg on 8/28/15.
 */
class GitScmCommit implements ScmCommitInfo {

    Map mapData

    GitScmCommit(final Map mapData) {
        this.mapData = new HashMap(mapData)
    }

    @Override
    String getCommitId() {
        mapData?.get('commitId')
    }

    @Override
    String getMessage() {
        mapData?.get("message")
    }

    @Override
    String getAuthor() {
        (mapData?.get("authorName") ?: '') + (mapData?.get("authorEmail") ? '<' + mapData?.get("authorEmail") + '>' :
                '')
    }

    @Override
    Date getDate() {
        mapData?.get("date")
    }

    @Override
    Map asMap() {
        return mapData
    }
}
