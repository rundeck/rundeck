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
 * $INTERFACE is ... User: greg Date: 12/17/13 Time: 3:27 PM
 */
@RunWith(JUnit4.class)
public class TestExceptionCatchingResourceModelSource {

    class testSource implements ResourceModelSource{
        INodeSet returnNodes;
        boolean throwException;
        boolean throwRTException;
        @Override
        public INodeSet getNodes() throws ResourceModelSourceException {
            if(throwException) {
                throw new ResourceModelSourceException("test exception");
            }
            if(throwRTException) {
                throw new RuntimeException("test runtime exception");
            }
            return returnNodes;
        }
    }
    @Test
    public void normal() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        testSource.returnNodes = iNodeEntries;
        new ExceptionCatchingResourceModelSource(testSource){
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertEquals(iNodeEntries, nodes);
                return nodes;
            }
        }.getNodes();
    }
    @Test
    public void nullresult() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        testSource.returnNodes = null;
        new ExceptionCatchingResourceModelSource(testSource){
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertNull(nodes);
                return nodes;
            }
        }.getNodes();
    }
    @Test
    public void exception() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        testSource.returnNodes = null;
        testSource.throwException = true;
        new ExceptionCatchingResourceModelSource(testSource){
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertNull(nodes);
                return nodes;
            }
        }.getNodes();
    }
    @Test
    public void rtexception() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        testSource.returnNodes = null;
        testSource.throwRTException = true;
        new ExceptionCatchingResourceModelSource(testSource){
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertNull(nodes);
                return nodes;
            }
        }.getNodes();
    }
    @Test
    public void testSubexception() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        testSource.returnNodes = null;
        testSource.throwRTException = true;
        INodeSet nodes = new ExceptionCatchingResourceModelSource(testSource) {
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertNull(nodes);
                throw new ResourceModelSourceException("test exception");
            }
        }.getNodes();
        Assert.assertEquals(null, nodes);
    }
    @Test
    public void testSubexceptionValues() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        testSource.returnNodes = iNodeEntries;
        testSource.throwRTException = false;
        INodeSet nodes = new ExceptionCatchingResourceModelSource(testSource) {
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertEquals(iNodeEntries,nodes);
                throw new ResourceModelSourceException("test exception");
            }
        }.getNodes();
        Assert.assertEquals(iNodeEntries, nodes);
    }
    @Test
    public void testSubRTexceptionValues() throws Exception{
        testSource testSource = new TestExceptionCatchingResourceModelSource.testSource();
        final NodeSetImpl iNodeEntries = new NodeSetImpl();
        testSource.returnNodes = iNodeEntries;
        testSource.throwRTException = false;
        INodeSet nodes = new ExceptionCatchingResourceModelSource(testSource) {
            @Override
            INodeSet returnResultNodes(INodeSet nodes) throws ResourceModelSourceException {
                Assert.assertEquals(iNodeEntries,nodes);
                throw new RuntimeException("test exception");
            }
        }.getNodes();
        Assert.assertEquals(iNodeEntries, nodes);
    }
}
