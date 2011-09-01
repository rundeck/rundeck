/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* TestPoliciesYaml.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 10:18 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import junit.framework.TestCase;

import java.io.File;
import java.util.*;

/**
 * TestPoliciesYaml is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestPoliciesYaml extends TestCase {
    File testdir;
    File test1;
    File test2;

    public void setUp() throws Exception {
        testdir = new File("src/test/com/dtolabs/rundeck/core/authorization/providers");
        test1 = new File(testdir, "test1.yaml");
        test2 = new File(testdir, "test2.yaml");

    }

    public void tearDown() throws Exception {

    }

    public void testCountPolicies() throws Exception {
        PoliciesYaml policies = new PoliciesYaml(test1);
        assertEquals(2, policies.countPolicies());
    }

    public void testGroupNames() throws Exception {
        PoliciesYaml policies = new PoliciesYaml(test1);
        final Collection<String> strings = policies.groupNames();
        assertEquals(3, strings.size());
        assertTrue(strings.contains("qa_group"));
        assertTrue(strings.contains("prod_group"));
        assertTrue(strings.contains("dev_group"));
    }
}
