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

/**
 * $INTERFACE is ... User: greg Date: 12/17/13 Time: 12:14 PM
 */
public class MemCachedResourceModelSource extends CachingResourceModelSource {
    INodeSet cache;

    public MemCachedResourceModelSource(ResourceModelSource delegate) {
        super(delegate);
    }

    @Override
    void storeNodesInCache(INodeSet nodes) {
        cache = nodes;
    }

    @Override
    INodeSet loadCachedNodes() {
        return cache;
    }
}
