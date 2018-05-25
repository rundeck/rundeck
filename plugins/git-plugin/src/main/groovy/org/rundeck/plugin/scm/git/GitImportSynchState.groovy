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

import com.dtolabs.rundeck.plugins.scm.ImportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmImportSynchState
import org.eclipse.jgit.lib.BranchTrackingStatus

/**
 * Created by greg on 9/15/15.
 */
class GitImportSynchState implements ScmImportSynchState {
    int importNeeded
    int notFound
    int deleted
    BranchTrackingStatus branchTrackingStatus
    ImportSynchState state
    String message
}
