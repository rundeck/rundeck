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

package rundeck.services.nodes

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.resources.ResourceModelSource

/**
 * Created by greg on 1/15/16.
 */
class CachedProjectNodes implements IProjectNodes {
    @Delegate
    ProjectNodeSupport nodeSupport
    ResourceModelSource source
    INodeSet nodes
    boolean doCache
    Date cacheTime


    @Override
    INodeSet getNodeSet() {
        return doCache?nodes:reloadNodeSet()
    }

    INodeSet reloadNodeSet() {
        nodes = source.getNodes()
        nodes
    }
}
