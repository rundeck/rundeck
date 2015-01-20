/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.INodeSet;
import org.apache.log4j.Logger;

/**
 * Abstract caching model source. calls to getNodes will attempt to use the delegate to get nodes.  If successful the
 * nodes will be stored in the cache with a call to {@link ResourceModelSourceCache#storeNodesInCache(com.dtolabs.rundeck.core.common.INodeSet)}.
 * If any exception is thrown it will be caught.  finally getNodes returns the result of {@link ResourceModelSourceCache#loadCachedNodes()}
 */
public class CachingResourceModelSource extends ExceptionCatchingResourceModelSource  {
    private ResourceModelSourceCache cache;

    public CachingResourceModelSource(ResourceModelSource delegate, ResourceModelSourceCache cache) {
        super(delegate);
        this.cache = cache;
    }

    public CachingResourceModelSource(ResourceModelSource delegate, String identity, ResourceModelSourceCache cache) {
        super(delegate, identity);
        this.cache = cache;
    }

    @Override
    INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
        if (null != nodes) {
            cache.storeNodesInCache(nodes);
            return nodes;
        }
        return cache.loadCachedNodes();
    }

}
