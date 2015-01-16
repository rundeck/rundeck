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
 * Wraps a ResourceModelSource and provides resilience in case the underlying source throws checked or unchecked
 * exceptions. Any exceptions thrown will be caught, and
 * {@link #returnResultNodes(com.dtolabs.rundeck.core.common.INodeSet)}
 * called with either null (exception thrown), or the result of the underlying call to getNodes
 */
public abstract class ExceptionCatchingResourceModelSource extends DelegateResourceModelSource {
    public static final Logger logger = Logger.getLogger(ExceptionCatchingResourceModelSource.class);
    String identity;

    protected ExceptionCatchingResourceModelSource(ResourceModelSource delegate) {
        super(delegate);
    }

    protected ExceptionCatchingResourceModelSource(ResourceModelSource delegate, String identity) {
        super(delegate);
        this.identity = identity;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        INodeSet nodes = null;
        try {
            nodes = getDelegate().getNodes();
        } catch (ResourceModelSourceException e) {
            logException(e);
        } catch (Throwable e) {
            logException(e);
        }
        try {
            return returnResultNodes(nodes);
        } catch (ResourceModelSourceException e) {
            logException(e);
        } catch (Throwable e) {
            logException(e);
        }
        return nodes;
    }

    private void logException(Throwable e) {
        logger.error(null != identity ? identity + " " : "" + e.getMessage(), e);
    }

    /**
     * @return the result nodes given the nodes returned by the underlying call
     *
     * @param nodes nodes
     * @throws ResourceModelSourceException on error
     */
    abstract INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException;
}
