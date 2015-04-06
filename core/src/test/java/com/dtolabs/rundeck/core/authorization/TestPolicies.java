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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.AclContext;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.authorization.providers.Policies;
import junit.framework.TestCase;

import javax.security.auth.Subject;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        assertEquals("Policy count mismatch", 9, policies.count());
        assertEquals("Policy count mismatch", 4, policiesSingle.count());
    }
    
    public void testSelectOnPrincipal() throws Exception {
        
        Subject formalSubject = new Subject();
        Set<Attribute> environment = new HashSet<Attribute>();
        environment.add(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), "aproject"));

        List<AclContext> contexts;


        formalSubject = new Subject();
        formalSubject.getPrincipals().add(new Username("yml_usr_1"));
        contexts = policies.narrowContext(formalSubject, environment);
        assertNotNull("Context is null.", contexts);
        assertEquals("Incorrect number of contexts returned when matching on group.", 1, contexts.size());
        
        formalSubject = new Subject();
        formalSubject.getPrincipals().add(new Username("test_1"));
        formalSubject.getPrincipals().add(new Group("admin")); // <-- will match on group membership.
        contexts = policies.narrowContext(formalSubject, environment);
        assertNotNull("Context is null.", contexts);
        assertEquals("Incorrect number of contexts returned when matching on group.", 1, contexts.size());
        

    }
    
    public void testListAllRoles() throws Exception {
        List<String> results = policies.listAllRoles();
        assertEquals("Results did not return the correct number of policies.", 9, results.size());
        results.containsAll(Arrays.asList("admin","foo","admin-environment","ou=Foo,dn=example,dn=com"));
    }
    public void testListAllRolesSingle() throws Exception {
        List<String> results = policiesSingle.listAllRoles();
        assertEquals("Results did not return the correct number of policies: "+results, 4, results.size());
        results.containsAll(Arrays.asList("test1","admin"));
    }
}
