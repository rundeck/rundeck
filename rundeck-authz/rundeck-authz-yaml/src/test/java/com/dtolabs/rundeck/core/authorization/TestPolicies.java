/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.Policies;
import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TestPolicies extends TestCase {

    private Policies policies;
    private Policies policiesSingle;

    public void setUp() throws Exception {

        policies = Policies.load(new File("src/test/resources/com/dtolabs/rundeck/core/authorization"));
        policiesSingle = Policies.loadFile(
                new File(
                        "src/test/resources/com/dtolabs/rundeck/core/authorization/admintest.aclpolicy"
                )
        );
    }

    public void testPoliciesStructural() throws Exception {
        assertEquals("Policy count mismatch", 8, policies.count());
        assertEquals("Policy count mismatch", 4, policiesSingle.count());
    }

    public void testListAllRoles() throws Exception {
        List<String> results = policies.listAllRoles();
        assertEquals("Results did not return the correct number of policies.", 8, results.size());
        results.containsAll(Arrays.asList("admin","foo","admin-environment","ou=Foo,dn=example,dn=com"));
    }
    public void testListAllRolesSingle() throws Exception {
        List<String> results = policiesSingle.listAllRoles();
        assertEquals("Results did not return the correct number of policies: "+results, 4, results.size());
        results.containsAll(Arrays.asList("test1","admin"));
    }
}
