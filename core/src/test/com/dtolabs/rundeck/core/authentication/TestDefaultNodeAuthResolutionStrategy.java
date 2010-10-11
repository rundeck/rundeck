/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.authentication;

import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: alexh
 * Date: Jul 28, 2008
 * Time: 2:53:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDefaultNodeAuthResolutionStrategy extends AbstractBaseTest {
    public TestDefaultNodeAuthResolutionStrategy(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestDefaultNodeAuthResolutionStrategy.class);
    }

    public void testCreate() {
        INodeAuthResolutionStrategy nodeAuth = DefaultNodeAuthResolutionStrategy.create(getFrameworkInstance());
        assertNotNull("got null result from create method", nodeAuth);
        assertTrue("result from create wasn't an instance of DefaultNodeAuthResolutionStrategy", 
                nodeAuth instanceof DefaultNodeAuthResolutionStrategy);
        INodeEntry nodeentry = new NodeEntryImpl("hoho","hoho");
        assertFalse("Should have returned false for isPasswordBasedAuthentication",
                nodeAuth.isPasswordBasedAuthentication(nodeentry));
        assertTrue("Should have returned true for isKeyBasedAuthentication",
                nodeAuth.isKeyBasedAuthentication(nodeentry));
    }

    public void testFetchUsername() {
        NodeEntryImpl nodeentry = new NodeEntryImpl("hoho","hoho");
        INodeAuthResolutionStrategy nodeAuth = DefaultNodeAuthResolutionStrategy.create(getFrameworkInstance());
        String username = nodeAuth.fetchUsername(nodeentry);
        /**
         * With no username configured it should default to the framework.ssh.user setting
         */
        assertEquals("Unexpected username fetched: " + username,
                getFrameworkInstance().getProperty("framework.ssh.user"), username);
        /**
         * Configure the hostname to have an embedded username. It should get parsed out.
         */
        nodeentry.setHostname("barney@hoho");
        assertEquals("Unexpected username fetched: " + nodeAuth.fetchUsername(nodeentry),
                "barney", nodeAuth.fetchUsername(nodeentry));        
        /**
         * Now configure the nodeentry with a run-username
         */
        nodeentry.setUsername("flintstone");
        assertEquals("Unexpected username fetched: " + nodeAuth.fetchUsername(nodeentry),
                "flintstone", nodeAuth.fetchUsername(nodeentry));

    }

    public void testFetchPassword() {
        INodeEntry nodeentry = new NodeEntryImpl("hoho","hoho");

        INodeAuthResolutionStrategy nodeAuth = DefaultNodeAuthResolutionStrategy.create(getFrameworkInstance());
        String pass =   nodeAuth.fetchPassword(nodeentry);
        assertTrue("this strategy is expected to return null for the password but it was: " + pass,
                null == pass);
    }
}
