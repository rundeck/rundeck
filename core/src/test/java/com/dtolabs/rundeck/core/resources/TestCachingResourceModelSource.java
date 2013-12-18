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
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class TestCachingResourceModelSource {
    class test implements ResourceModelSourceCache {
        INodeSet stored;
        INodeSet cached;
        boolean storedCalled=false;
        boolean loadCalled=false;


        @Override
        public void storeNodesInCache(INodeSet nodes) throws ResourceModelSourceException {
            storedCalled=true;
            stored = nodes;
        }

        @Override
        public INodeSet loadCachedNodes() throws ResourceModelSourceException {
            loadCalled=true;
            return cached;
        }
    }

    @Test
    public void testStoreNodesInCache() throws Exception {
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        //delegate returns nodes, should be stored
        test test = new test();
        CachingResourceModelSource source = new CachingResourceModelSource(new ResourceModelSource() {
            @Override
            public INodeSet getNodes() throws ResourceModelSourceException {
                return iNodeEntries;
            }
        }, test);

        Assert.assertNull(test.stored);
        INodeSet nodes = source.getNodes();
        Assert.assertTrue(test.storedCalled);
        Assert.assertFalse(test.loadCalled);
        Assert.assertEquals(iNodeEntries, nodes);
        Assert.assertEquals(iNodeEntries, test.stored);
    }

    @Test
    public void testNullResult() throws Exception {
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        //delegate returns nodes, should be stored

        test test = new test();
        CachingResourceModelSource source = new CachingResourceModelSource(new ResourceModelSource() {
            @Override
            public INodeSet getNodes() throws ResourceModelSourceException {
                return null;
            }
        }, test);
        test.cached = iNodeEntries;
        Assert.assertNull(test.stored);
        INodeSet nodes = source.getNodes();
        Assert.assertFalse(test.storedCalled);
        Assert.assertTrue(test.loadCalled);
        Assert.assertEquals(iNodeEntries, nodes);
        Assert.assertNull(test.stored);
    }

    @Test
    public void testExceptionResult() throws Exception {
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        //delegate returns nodes, should be stored
        test test = new test();
        CachingResourceModelSource source = new CachingResourceModelSource(new ResourceModelSource() {
            @Override
            public INodeSet getNodes() throws ResourceModelSourceException {
                throw new ResourceModelSourceException("test exception");
            }
        }, test);
        test.cached = iNodeEntries;
        Assert.assertNull(test.stored);
        INodeSet nodes = source.getNodes();
        Assert.assertFalse(test.storedCalled);
        Assert.assertTrue(test.loadCalled);
        Assert.assertEquals(iNodeEntries, nodes);
        Assert.assertNull(test.stored);
    }

    @Test
    public void testRTExceptionResult() throws Exception {
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        //delegate returns nodes, should be stored
        test test = new test();
        CachingResourceModelSource source = new CachingResourceModelSource(new ResourceModelSource() {
            @Override
            public INodeSet getNodes() throws ResourceModelSourceException {
                throw new RuntimeException("test exception");
            }
        }, test);
        test.cached = iNodeEntries;
        Assert.assertNull(test.stored);
        INodeSet nodes = source.getNodes();
        Assert.assertFalse(test.storedCalled);
        Assert.assertTrue(test.loadCalled);
        Assert.assertEquals(iNodeEntries, nodes);
        Assert.assertNull(test.stored);
    }


}
