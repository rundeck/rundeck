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

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.INodeSet;

/**
 * Utils for creating ResourceModelSources
 */
public class SourceFactory {

    /**
     * @return A source which always returns the given data
     */
    public static ResourceModelSource staticSource(final INodeSet data) {
        return new ResourceModelSource() {
            @Override
            public INodeSet getNodes() throws ResourceModelSourceException {
                return data;
            }
        };
    }

    public static ResourceModelSource cachedSource(
            final ResourceModelSource delegate,
            final String identity,
            final ExceptionCatchingResourceModelSource.ExceptionHandler handler,
            final ResourceModelSourceCache cache
    )
    {
        return cachedSource(delegate, identity, handler, cache, CacheType.BOTH);
    }

    public static ResourceModelSource cacheWritingSource(
            final ResourceModelSource delegate,
            final String identity,
            final ExceptionCatchingResourceModelSource.ExceptionHandler handler,
            final ResourceModelSourceCache cache
    )
    {
        return cachedSource(delegate, identity, handler, cache, CacheType.STORE_ONLY);
    }

    public static ResourceModelSource cacheLoadingSource(
            final ResourceModelSource delegate,
            final String identity,
            final ExceptionCatchingResourceModelSource.ExceptionHandler handler,
            final ResourceModelSourceCache cache
    )
    {
        return cachedSource(delegate, identity, handler, cache, CacheType.LOAD_ONLY);
    }

    public static ResourceModelSource cachedSource(
            final ResourceModelSource delegate,
            final String identity,
            final ExceptionCatchingResourceModelSource.ExceptionHandler handler,
            final ResourceModelSourceCache cache,
            final CacheType type
    )
    {
        return new CachingResourceModelSource(delegate, identity, handler, cache, type);
    }

    /**
     * behavior of the cache
     */
    public static enum CacheType {
        /**
         * Only load data
         */
        LOAD_ONLY(true, false),
        /**
         * only store data
         */
        STORE_ONLY(false, true),
        /**
         * Store and load data
         */
        BOTH(true, true);

        private final boolean storeType;
        private final boolean loadType;

        CacheType(boolean load, boolean store) {
            this.loadType = load;
            this.storeType = store;
        }

        public boolean isStoreType() {
            return storeType;
        }

        public boolean isLoadType() {
            return loadType;
        }
    }
}
