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
 * Facade for {@link CachingResourceModelSource} that logs cache store and load events.
 */
public class LoggingResourceModelSourceCache implements ResourceModelSourceCache {
    public static final Logger logger = Logger.getLogger(LoggingResourceModelSourceCache.class);
    ResourceModelSourceCache cache;
    String ident;

    public LoggingResourceModelSourceCache(ResourceModelSourceCache cache, String ident) {
        this.cache = cache;
        this.ident = ident;
    }

    @Override
    public void storeNodesInCache(INodeSet nodes) throws ResourceModelSourceException {
        logger.debug(ident + " Caching model data for (" + nodes.getNodes().size() + " nodes)");
        cache.storeNodesInCache(nodes);
    }

    @Override
    public INodeSet loadCachedNodes() throws ResourceModelSourceException {
        logger.warn(ident + " Returning cached model data");

        return cache.loadCachedNodes();
    }
}
