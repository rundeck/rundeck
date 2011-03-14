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

/**
 * 
 */
package com.dtolabs.rundeck.core.authorization;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization;

import junit.framework.TestCase;

/**
 * @author noahcampbell
 *
 */
public class TestSAREAuthorization extends TestCase {
    private Authorization authorization;
    private Authorization legacyAuthorization;
    private Authorization legacyvalidationAuthorization;
    private Set<Attribute> environment = new HashSet<Attribute>();
    
    public void setUp() throws Exception {
        authorization = new SAREAuthorization(TestPolicies.getPath("com/dtolabs/rundeck/core/authorization"));
        legacyAuthorization = new SAREAuthorization(TestPolicies.getPath("com/dtolabs/rundeck/core/authorization/legacyconv"));
        legacyvalidationAuthorization = new SAREAuthorization(TestPolicies.getPath("com/dtolabs/rundeck/core/authorization/legacyconv"));
    }
    
    public void tearDown() throws Exception {
        environment.clear();
    }
    
    public void testInvalidParameters() throws Exception {
        try {
            authorization.evaluate((Map<String, String>)null, null, null, null);
        } catch(Exception e) { /* ignore...it should throw an exception */ }
        
        try {
            authorization.evaluate(new HashMap<String,String>(), new Subject(), "", new HashSet<Attribute>());
        } catch(Exception e) { /* ignore...it should throw an exception */ }
    }
    
    public void testAdminModulePrivileges() throws Exception {
        
        Map<String,String> resource = declareScript("adhocScript", "foo/bar");
        Subject subject = createSubject("default", "admin", "foo");
        String action = "EXECUTE";
        
        assertTrue("'default' does not have access to 'foo/bar/adhocScript' to 'EXECUTE' with no environment specified.", 
                authorization.evaluate(resource, subject, action, null).isAuthorized());
        
        assertTrue("'default' does not have access to 'foo/bar/adhocScript' to 'EXECUTE' with no environment specified.",
                authorization.evaluate(resource, subject, action, null).isAuthorized());
        
    }
    
    public void testXML() throws Exception {
        Map<String,String> resource = declareModule("Monkey", "barbaz");
        Subject subject = createSubject("XML", "admin");
        String action = "READ";
        assertTrue("policy didn't match.", legacyAuthorization.evaluate(resource, subject, action, environment).isAuthorized());
        
        subject = createSubject("XML", "adminz");
        assertFalse("policy should match", legacyAuthorization.evaluate(resource, subject, action, environment).isAuthorized());
    }
    
    public void testXML1Validation() throws Exception {
        assertTrue("Policy didn't match, XML1 Validation for admin to do anything.", 
                legacyvalidationAuthorization.evaluate(declareModule("Monkey", "barbaz"), 
                        createSubject("testXML1Validation", "admin"), 
                        "READ", 
                        environment).isAuthorized());
    }
    
    public void testNoModuleAccess() throws Exception {

        assertFalse("Policy should not match because the admin-no-module cannot run modules.", 
                legacyvalidationAuthorization.evaluate(declareModule("Monkey", "barbaz"), 
                        createSubject("testNoModuleAccess", "admin-no-module"), 
                        "READ", 
                        environment).isAuthorized());
    }
    
    public void testNoScriptAccess() throws Exception {
        
        Map<String, String> resource = declareScript("adhocScript", "foo/bar");
        assertFalse("Policy should not have matched: group admin-no-script, does not have script access.", 
                legacyvalidationAuthorization.evaluate(resource, 
                        createSubject("testNoScriptAccess", "admin-no-script"), 
                        "READ", 
                        environment).isAuthorized());
        
        
        
        resource.remove("group");
        
        assertFalse("Policy should not have matched: group admin-no-script, does not have" +
        		" script access.  Omitted group from resource request.", 
                legacyvalidationAuthorization.evaluate(resource,
                        createSubject("testNoScriptAccess", "admin-no-script"), 
                        "READ", 
                        environment).isAuthorized());
        
    }
    
    public void testInvalidInput() throws Exception {
        Map<String,String> resource = declareScript("", "bar/baz/boo");
        Subject subject = createSubject("testActionAuthorization", "admin-invalidinput");
        
        /* Check that workflow_run is actually a matching action */
        Decision decision = authorization.evaluate(resource, subject, "workflow_run", null);
        assertEquals("Expecting to see a code indicating that a property on the resource is missing.", 
                Code.REJECTED_COMMAND_NOT_MATCHED, decision.explain().getCode());
        
        assertFalse("An empty job name should not be authorized.", decision.isAuthorized());
        
        try {
            authorization.evaluate(declareScript(null, "test"), subject, "invalid_input_missing_key", null);
            assertTrue("A null resource key should not be evaluated.", false);
        } catch (IllegalArgumentException e) {
            // ignore...this is expected.
        }
        
        try {
            authorization.evaluate(declareScript("test_key_with_null_value", null), subject, "invalid_input_missing_value", null);
            assertTrue("A null resource value should not be evaluated.", false);
        } catch (IllegalArgumentException e) {
            // ignore...this is expected.
        }
        
        
    }
    
    public void testActionAuthorizationYml() throws Exception {
        Map<String,String> resource = declareScript("myScript", "/yml/bar/baz/boo");
        Subject subject = createSubject("yml_user_1", "yml_group_1");
        
        /* Check that workflow_run is actually a matching action */
        Decision decision = authorization.evaluate(resource, subject, "pattern_match", null);      
        assertEquals("Decision for successful authoraztion for action: pattern_match does not match, but should.",
                Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.explain().getCode());
        assertTrue("Action not granted authorization.", decision.isAuthorized());
        
        resource = declareScript("Script2", "/listAction");
        decision = authorization.evaluate(resource, subject, "action_list_2", null);
        assertEquals("Decision for successful authoraztion for action: action_list_2 does not match, but should.",
                Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.explain().getCode());
        assertTrue("Action not granted authorization.", decision.isAuthorized());
        
        resource = declareScript("Script3", "/wldcrd");
        decision = authorization.evaluate(resource, subject, "action_list_not_in_list_and_shouldn't_be", null);
        assertEquals("Decision for successful authoraztion for action: action_list_not_in_list_and_shouldn't_be does not match, but should.",
                Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.explain().getCode());
        assertTrue("Action not granted authorization.", decision.isAuthorized());
        
        
    }
    
    public void testActionAuthorizationYmlInvalid() throws Exception {
        Map<String,String> resource = declareScript("Script3", "/noactions");
        Subject subject = createSubject("yml_usr_2", "broken");
        
        Decision decision = authorization.evaluate(resource, subject, "none", null);
        assertEquals("Decision for authoraztion for action: none is not REJECTED_NO_ACTIONS_DECLARED.",
                Code.REJECTED, decision.explain().getCode());
        assertTrue("Action granted authorization.", !decision.isAuthorized());
        
        subject = createSubject("yml_usr_3", "missing_rules");
        
        decision = authorization.evaluate(resource, subject, "none", null);
        assertEquals("Decision for authoraztion for action: none is not REJECTED_NO_RULES_DEFINED.",
                Code.REJECTED_NO_RULES_DECLARED, decision.explain().getCode());
        assertTrue("Action granted authorization.", !decision.isAuthorized());
        
    }
    
    public void testActionAuthorization() throws Exception {
        Map<String,String> resource = declareScript("myScript", "bar/baz/boo");
        Subject subject = createSubject("testActionAuthorization", "admin-action");
        
        /* Check that workflow_run is actually a matching action */
        Decision decision = authorization.evaluate(resource, subject, "workflow_run", null);      
        assertEquals("Decision for successful authoraztion for action: workflow_run does not match, but should.",
                Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.explain().getCode());
        assertTrue("Action not granted authorization.", decision.isAuthorized());
       
        /* bobble_head action doesn't exist, so should not be authorized */
        decision = authorization.evaluate(resource, subject, "bobble_head", environment);
        assertEquals("Decision does not contain the proper explanation. ", 
                Code.REJECTED_COMMAND_NOT_MATCHED,  decision.explain().getCode());
        assertFalse("Action bobble_head should not have been authorized", decision.isAuthorized());
       
        System.out.println(decision);
        decision.explain().describe(System.out);
        
        /* Empty actions never match. */
        decision = authorization.evaluate(resource, subject, "", environment);
        assertEquals("Decision for empty action does not match", Code.REJECTED_NO_ACTION_PROVIDED,
                decision.explain().getCode());
        assertFalse("An empty action should not select", decision.isAuthorized());

        System.out.println(decision);
        decision.explain().describe(System.out);        
        
        /* The given job=anyaction of group=foobar should allow any action. */
        decision = authorization.evaluate(declareScript("anyaction", "foobar"), subject, 
                "my_wacky_action", environment);
        assertEquals("my_wacky_action reason does not match.", 
                Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.explain().getCode());
        assertTrue("foobar/barbaz was denied even though it allows any action.", decision.isAuthorized());
        
        
        decision = authorization.evaluate(declareModule("foobar", "moduleName"), subject, 
                "execute", null);
        assertFalse("foobar/moduleName was granted authorization when it shouldn't.", decision.isAuthorized());
        
        Set<Map<String,String>> resources = new HashSet<Map<String,String>>();
        final int resourcesCount = 100;
        final int actionsCount = 10;
        for(int i = 0; i < resourcesCount; i++) {
            resources.add(declareScript(Integer.toString(i), "big/test/" + Integer.toString(i)));
        }
        Set<String> actions = new HashSet<String>();
        for(int i = 0; i < actionsCount; i++) {
            actions.add("Action" + Integer.toString(i));
        }
        long start = System.currentTimeMillis();
        authorization.evaluate(resources, subject, actions, environment);
        long end = System.currentTimeMillis() - start;
        System.out.println("Took " + end + "ms for " + resourcesCount + " resources and " + actionsCount + " actions.");
        
        
    }
    
    public void off_testProjectEnvironment() throws Exception {
        Map<String,String> resource = declareScript("adhocScript", "foo/bar");
        Subject subject = createSubject("testProjectEnvironment", "admin-environment");
        
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), "Lion"));
        
        assertTrue("Policy did not match the Lion context.", 
                authorization.evaluate(resource, subject, "READ", environment).isAuthorized());
        
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/project"), "Tiger"));
        
        assertFalse("Policy should not match the Lion context.", 
                authorization.evaluate(resource, subject, "READ", environment).isAuthorized());
    }
    
    public void off_testTimeOfDay() throws Exception {
        declareScript("adhocScript", "foo/bar");
        String action = "READ";
        Subject subject = createSubject("testTimeOfDay", "admin-environment");
        
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/now.gmt"),
                new Date().toGMTString()));
        throw new Exception();
    }
    
    public void off_testNodeTarget() throws Exception {
        declareScript("adhocScript", "foo/bar");
        String action = "READ";
        Subject subject = createSubject("testNodeTarget", "admin-environment");
        
        environment.add(new Attribute(URI.create("http://dtolabs.com/rundeck/env/target-node-name"),
                "web99.example.com"));
        
        throw new Exception();
    }
    

    /**
     * @param scriptName
     * @param scriptGroup
     */
    private Map<String, String> declareScript(String scriptName, String scriptGroup) {
        Map<String, String> resource = new HashMap<String, String>();
        resource.put("job", scriptName);
        resource.put("group", scriptGroup);
        return resource;
    }

    /**
     * @param module
     * @param moduleName
     */
    private Map<String,String> declareModule(String module, String moduleName) {
        Map<String, String> resource = new HashMap<String, String>();
        resource.put("module", module);
        resource.put("name", moduleName);
        return resource;
    }
    
    /**
     * Create a subject for a username and a series of groups.
     * 
     * @param username
     * @param groups (optional)
     * @return subject
     */
    private Subject createSubject(String username, String...groups) {
        
        if(username == null) throw new IllegalArgumentException("Username cannot be null.");
        if(groups == null) {
            groups = new String[0];
        }
        Subject subject = new Subject();
        subject.getPrincipals().add(new Username(username));
        for(String group : groups) {
            if(group == null || group.length() <= 0) throw new IllegalArgumentException("Group null or zero length.");
            subject.getPrincipals().add(new Group(group));
        }
        subject.setReadOnly();
        return subject;
    }
}
