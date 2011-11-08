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

import junit.framework.Test;
import junit.framework.TestSuite;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;

/**
 * Tests the  factory methods in NodeAuthResolutionStrategyFactory
 */
public class TestNodeAuthResolutionStrategyFactory extends AbstractBaseTest {
    public TestNodeAuthResolutionStrategyFactory(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestNodeAuthResolutionStrategyFactory.class);
    }

    public void testCreate() {
        try {
            INodeAuthResolutionStrategy nodeAuth = NodeAuthResolutionStrategyFactory
                    .create("", getFrameworkInstance());
            assertNull("expected an exception", nodeAuth);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        try {
            INodeAuthResolutionStrategy nodeAuth = NodeAuthResolutionStrategyFactory
                    .create(null, getFrameworkInstance());
            assertNull("expected an exception", nodeAuth);
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        INodeAuthResolutionStrategy nodeAuth = NodeAuthResolutionStrategyFactory
                .create(getFrameworkInstance().getProperty("framework.nodeauthentication.classname"),
                        getFrameworkInstance());
        assertNotNull("expected a DefaultNodeAuthResolutionStrategy", nodeAuth);
        assertTrue("expected an instance of INodeAuthResolutionStrategy but it was a: "
                + nodeAuth.getClass().getName(), nodeAuth instanceof INodeAuthResolutionStrategy);

    }

}
