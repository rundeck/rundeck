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

/*
* TestYamlPolicyCollection.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 10:18 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import junit.framework.TestCase;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TestYamlPolicyCollection is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestYamlPolicyCollection extends TestCase {
    File testdir;
    File test1;
    File test2;
    private Set<Attribute> environment = AuthorizationUtil.context("application", "rundeck");

    public void setUp() throws Exception {
        testdir = new File("src/test/resources/com/dtolabs/rundeck/core/authorization/providers");
        test1 = new File(testdir, "test1.yaml");
        test2 = new File(testdir, "test2.yaml");
//        environment.add(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), "aproject"));

    }

    public void tearDown() throws Exception {

    }

    public void testCountPolicies() throws Exception {
        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
        assertEquals(6, policies.countPolicies());
    }

    private YamlPolicyCollection makeTestPolicies(final CacheableYamlSource source) throws IOException {
        return new YamlPolicyCollection(
                "test",
                YamlPolicy.loader(source, null),
                YamlPolicy.creator(null, null),
                null
        );
    }
//
//    public void testGroupEqualsSingle() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("user1", "prod_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(1, ruleSetConstructors.size());
//    }
//    public void testGroupEqualsMulti() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("user1", "qa_group","prod_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(2, ruleSetConstructors.size());
//    }
//    public void testGroupPatternMatch() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("user1", "dev_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(2, ruleSetConstructors.size());
//    }
//    public void testGroupPatternMatch2() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("user1", "dev_test");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(1, ruleSetConstructors.size());
//    }
//
//    public void testUserEqualsSingleNoMatch() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("devX", "some_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(0, ruleSetConstructors.size());
//    }
//    public void testUserEqualsSingle1Match() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("dev2", "some_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(1, ruleSetConstructors.size());
//    }
//    public void testUserEqualsSingle2Match() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("dev1", "some_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(2, ruleSetConstructors.size());
//    }
//    public void testUserRegexMatch() throws Exception {
//        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
//        Subject subject = makeSubject("devA", "some_group");
//        Collection<RuleSetConstructor> ruleSetConstructors = policies.matchedContexts(subject, environment);
//        assertEquals(1, ruleSetConstructors.size());
//    }

    private Subject makeSubject(String username, String... groups) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new Username(username));
        for (int i = 0; i < groups.length; i++) {
            String group = groups[i];
            subject.getPrincipals().add(new Group(group));
        }
        return subject;
    }

    public void testGroupNames() throws Exception {
        YamlPolicyCollection policies = makeTestPolicies(YamlProvider.sourceFromFile(test1));
        final Collection<String> strings = policies.groupNames();
        assertEquals(4, strings.size());
        assertTrue(strings.contains("qa_group"));
        assertTrue(strings.contains("prod_group"));
        assertTrue(strings.contains("dev_group"));
        assertTrue(strings.contains("dev_.*"));
    }
    private HashSet<Pattern> makePatterns(String... regexes) {
        HashSet<Pattern> patterns = new HashSet<Pattern>();
        for (int i = 0; i < regexes.length; i++) {
            String regex = regexes[i];
            patterns.add(Pattern.compile(regex));
        }

        return patterns;
    }

    private HashSet<String> testStrings(String... string) {
        HashSet<String> strings = new HashSet<String>();
        for (int i = 0; i < string.length; i++) {
            String s = string[i];
            strings.add(s);
        }
        return strings;
    }
}
