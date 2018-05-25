/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
