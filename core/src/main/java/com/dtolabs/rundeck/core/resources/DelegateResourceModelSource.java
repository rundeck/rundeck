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

import java.util.List;

/**
 * Delegating source meant to be subclassed
 */
public abstract class DelegateResourceModelSource implements ResourceModelSource,ResourceModelSourceErrors {
    private ResourceModelSource delegate;

    public DelegateResourceModelSource(ResourceModelSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        return getDelegate().getNodes();
    }

    public ResourceModelSource getDelegate() {
        return delegate;
    }

    @Override
    public List<String> getModelSourceErrors() {
        if(getDelegate() instanceof ResourceModelSourceErrors){
            return ((ResourceModelSourceErrors)getDelegate()).getModelSourceErrors();
        }else{
            return null;
        }
    }
}
