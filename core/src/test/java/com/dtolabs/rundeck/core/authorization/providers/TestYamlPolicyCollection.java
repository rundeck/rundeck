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
* TestYamlPolicyCollection.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 10:18 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import junit.framework.TestCase;

import java.io.File;
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

    public void setUp() throws Exception {
        testdir = new File("src/test/resources/com/dtolabs/rundeck/core/authorization/providers");
        test1 = new File(testdir, "test1.yaml");
        test2 = new File(testdir, "test2.yaml");

    }

    public void tearDown() throws Exception {

    }

    public void testCountPolicies() throws Exception {
        YamlPolicyCollection policies = new YamlPolicyCollection(test1);
        assertEquals(2, policies.countPolicies());
    }

    public void testGroupNames() throws Exception {
        YamlPolicyCollection policies = new YamlPolicyCollection(test1);
        final Collection<String> strings = policies.groupNames();
        assertEquals(3, strings.size());
        assertTrue(strings.contains("qa_group"));
        assertTrue(strings.contains("prod_group"));
        assertTrue(strings.contains("dev_group"));
    }
    public void testPatterns() throws Exception{
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns("abc")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns("abc")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns("a.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns("a.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns("d.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns("g.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns(".*c")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns(".*c")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns(".*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc","def","ghi"), makePatterns(".*")));

        assertFalse(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns("d.*")));
        assertFalse(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns("g.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc", "def", "ghi"), makePatterns("d.*")));
        assertTrue(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc", "def", "ghi"), makePatterns("g.*")));
        assertFalse(YamlPolicyCollection.matchesAnyPatterns(testStrings("abc"), makePatterns("d")));
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
