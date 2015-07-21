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

package com.dtolabs.rundeck.core.authorization.providers;
/*
* TestPoliciesCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 1:32:37 PM
* 
*/

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class TestPoliciesCache extends TestCase {
    PoliciesCache policiesCache;

    public TestPoliciesCache(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestPoliciesCache.class);
    }


    public void setUp() throws Exception {
        policiesCache = PoliciesCache.fromDir(new File("src/test/resources/com/dtolabs/rundeck/core/authorization"));
    }

    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public void testIterator() throws Exception {
        final Iterator<PolicyCollection> iterator = policiesCache.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        ArrayList<PolicyCollection> docs = new ArrayList<PolicyCollection>();
        while(iterator.hasNext()) {
            final PolicyCollection policiesDocument = iterator.next();
            assertNotNull(policiesDocument);
            docs.add(policiesDocument);
        }
        assertEquals(4, docs.size());
    }

    public void testSingleFile() throws Exception {

        policiesCache = PoliciesCache.fromFile(
                new File(
                        "src/test/resources/com/dtolabs/rundeck/core/authorization/admintest.aclpolicy"
                )
        );
        final Iterator<PolicyCollection> iterator = policiesCache.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        ArrayList<PolicyCollection> docs = new ArrayList<PolicyCollection>();
        while (iterator.hasNext()) {
            final PolicyCollection policiesDocument = iterator.next();
            assertNotNull(policiesDocument);
            docs.add(policiesDocument);
        }
        assertEquals(1, docs.size());
    }
}
