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

package com.dtolabs.rundeck.core.common.context;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestFrameworkProjectContext
 */
public class TestFrameworkProjectContext extends TestCase{
    static final String RESOURCE_PROJECT = "TestFrameworkProjectContext";
        public TestFrameworkProjectContext(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestFrameworkProjectContext.class);
    }

    protected void setUp() throws Exception {

    }

    public void testContext() {
        FrameworkProjectContext d1 = new FrameworkProjectContext();
        assertNotNull(d1);
        assertTrue(d1.isEmptyContext());
        FrameworkProjectContext d2 = FrameworkProjectContext.create(RESOURCE_PROJECT);
        assertNotNull(d2);
        assertEquals(d2.getFrameworkProject(), RESOURCE_PROJECT);
        assertFalse(d2.isEmptyContext());
        assertTrue(d2.isDepotContext());
    }
}
