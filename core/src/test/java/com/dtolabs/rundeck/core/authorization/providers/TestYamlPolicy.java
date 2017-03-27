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
* TestYamlPolicy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 11:49 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.AclRule;
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.utils.Converter;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TestYamlPolicy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@RunWith(JUnit4.class)
public class TestYamlPolicy  {
    File testdir;
    File test1;
    File test2;
    Yaml yaml;

    @Before
    public void setUp() throws Exception {
        testdir = new File("src/test/resources/com/dtolabs/rundeck/core/authorization/providers");
        test1 = new File(testdir, "test1.yaml");
        test2 = new File(testdir, "test2.yaml");
        yaml = new Yaml();
    }

    public void tearDown() throws Exception {

    }

    /**
     * Test evaluation of top level policy definition
     */
    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_description_required(){

        //test "description" is required
            final Map map = new HashMap();
            final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor;
        yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null, typeRuleSetConstructorFactory);
    }

    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_type_required() {
        //test resource requires "type"
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new HashMap());
        final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null,


                                                                                                               typeRuleSetConstructorFactory
        );

    }

    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_for_contents() {
        //for: must be map
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", "test1");
        final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor;
        yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null, typeRuleSetConstructorFactory);
        Assert.fail("Expected syntax error");
    }

    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_for_must_be_map() {
        //for: must be map
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new ArrayList());
        final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null,
                                                                                                               typeRuleSetConstructorFactory
        );
    }
    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_for_must_exist() {
        //for: must not be null
        final Map map = new HashMap();
        map.put("description", "test1");
//            map.put("for", new ArrayList());
        final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null,
                                                                                                               typeRuleSetConstructorFactory
        );
    }

    @Test(expected = AclPolicySyntaxException.class)
    public void testYamlAclContext_for_not_empty() {
        //for: may not be empty
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new HashMap());
        final YamlPolicy.TypeRuleSetConstructorFactory typeRuleSetConstructorFactory = null;
        final YamlPolicy.YamlRuleSetConstructor yamlRuleSetConstructor = new YamlPolicy.YamlRuleSetConstructor(map, null,
                                                                                                               typeRuleSetConstructorFactory
        );
    }






    private YamlPolicy.RuleConstructor createTestMatcher(final boolean matched, final Explanation.Code code,
                                                         final boolean granted) {
        return createTestMatcher(matched, code, granted, new ArrayList<ContextEvaluation>());
    }

    private YamlPolicy.RuleConstructor createTestMatcher(final boolean matched, final Explanation.Code code,
                                                         final boolean granted,
                                                         final List<ContextEvaluation> contextEvaluations) {
        return new YamlPolicy.RuleConstructor() {
            public YamlPolicy.MatchedContext includes(Map<String, String> resource, String action) {
                return new YamlPolicy.MatchedContext(matched, new ContextDecision(code, granted, contextEvaluations));
            }

            @Override
            public AclRule createRule(final AclRuleBuilder prototype) {
                return null;
            }
        };
    }


    @Test public void testApplyTest() {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);
            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            Converter<String, Predicate> test1 = new Converter<String, Predicate>() {
                public Predicate convert(String s) {
                    return PredicateUtils.equalPredicate(s);
                }
            };

            //test single value predicate value is returned

            Assert.assertNotNull(typeRuleContext.applyTest(resmap, false, test1, "name", "blah"));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", "blee"));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", new ArrayList()));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", Arrays.asList("blah")));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", Arrays.asList("blah", "blah")));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, false, test1, "name", new Object()));

            //test multivalue predicate value is AND result
            Assert.assertNotNull(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah")));
            Assert.assertNotNull(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah", "blah")));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blah", "blee")));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blee", "blah")));
            Assert.assertFalse(typeRuleContext.applyTest(resmap, true, test1, "name", Arrays.asList("blee", "blee")));
    }

    @Test public void testPredicateMatchRules() {
        //match any resource with name=~ blah, and allow all actions
        final Object load = yaml.load("match: \n"
                                      + "  name: '.*blah.*'\n"
                                      + "allow: '*'");
        Assert.assertNotNull(load instanceof Map);
        final Map ruleSection = (Map) load;
        final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
        "x",                 ruleSection,null, 1,null);
        final HashMap<String, String> resmap = new HashMap<String, String>();
        resmap.put("name", "blah");
        resmap.put("king", "true");
        resmap.put("wave", "bland");

        Converter<String, Predicate> test1 = new Converter<String, Predicate>() {
            public Predicate convert(String s) {
                return PredicateUtils.equalPredicate(s);
            }
        };
        HashMap rules = new HashMap();

        //test empty rules
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules, match false
        rules.put("name", "bloo");
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match true
        rules.put("name", "blah");
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, false
        rules.put("name", "blah");
        rules.put("king", "false");
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, true
        rules.put("name", "blah");
        rules.put("king", "true");
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, false
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bloo");
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  match all, true
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bland");
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertNotNull(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

        //set rules,  additional rules match false
        rules.put("name", "blah");
        rules.put("king", "true");
        rules.put("wave", "bland");
        rules.put("another", "blee");
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, false, test1));
        Assert.assertFalse(typeRuleContext.predicateMatchRules(rules, resmap, true, test1));

    }

    @Test public void testTypeRuleContextMatcherMatchRule() {
        {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
        {
            //multiple regexes must all match
            final Object load = yaml.load("match: \n"
                                          + "  name: ['.*blah.*','.*nada.*']\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "nada");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz nada");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
        {
            //multiple attributes must all match
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "  something: '.*else.*'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));


            resmap.put("something", "els");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("something", "else");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("something", "bloo else zaaf");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "naba");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.remove("name");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
        {
            //invalid regex match becomes eequality match
            final Object load = yaml.load("match: \n"
                                          + "  name: 'abc[def'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "abcdef");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.put("name", "abc[def");
            Assert.assertNotNull(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

            resmap.remove("name");
            Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        }
    }

    @Test public void testTypeRuleContextMatcherMatchRuleWithInvalidContentShouldNotMatch() {
        //invalid content
        final Object load = yaml.load("match: \n"
                + "name: '.*blah.*'\n"
                + "allow: '*'");
        Assert.assertNotNull(load instanceof Map);
        final Map ruleSection = (Map) load;
        final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

        final HashMap<String, String> resmap = new HashMap<String, String>();

        //false result for no match
        Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

        resmap.put("name", "something");
        Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
        resmap.put("name", "blah");
        Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));
        resmap.put("name", "ablahz");
        Assert.assertFalse(typeRuleContext.ruleMatchesMatchSection(resmap, ruleSection));

    }

    @Test public void testTypeRuleContextMatcherEqualsRule() {
        {
            //equality for single attribute 'name'
            final Object load = yaml.load("equals: \n"
                                          + "  name: blah\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertNotNull(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        }
        {
            //equality for multiple attributes
            final Object load = yaml.load("equals: \n"
                                          + "  name: blah\n"
                                          + "  something: zelse\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

            resmap.put("something", "else");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.put("something", "zelse");
            Assert.assertNotNull(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));


            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
            resmap.remove("name");
            Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        }
    }

    @Test public void testTypeRuleContextMatcherEqualsRuleWithInvalidContentShouldNotMatch() {
        //yaml name: is not indented properly
        final Object load = yaml.load("equals: \n"
                + "name: blah\n"
                + "allow: '*'");
        Assert.assertNotNull(load instanceof Map);
        final Map ruleSection = (Map) load;
        final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

        final HashMap<String, String> resmap = new HashMap<String, String>();

        //false result for no match
        Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));

        resmap.put("name", "something");
        Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        resmap.put("name", "blah");
        Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
        resmap.put("name", "ablahz");
        Assert.assertFalse(typeRuleContext.ruleMatchesEqualsSection(resmap, ruleSection));
    }
    @Test public void testTypeRuleContextMatcherContainsRule() {
        {
            //match single attribute
            final Object load = yaml.load("contains: \n"
                                          + "  name: blah\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
        {
            //list must all match the attribute
            final Object load = yaml.load("contains: \n"
                                          + "  name: [blah,shamble]\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, test");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, blah");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, shamble");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah,shamble");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", " blah,shamble   ");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
        {
            //multiple attributes must all match
            final Object load = yaml.load("contains: \n"
                                          + "  name: [blah,shamble]\n"
                                          + "  something: [plead]\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, test");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "shamble, blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, shamble");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah,shamble");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", " blah,shamble   ");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            //set 'something' attribute
            resmap.put("something", " bloo   ");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("something", " blee   ");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("something", " blee  , plead ");
            Assert.assertNotNull(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
        }
    }

    @Test public void testTypeRuleContextMatcherContainsRuleWithInvalidContentShouldNotMatch() {
            //empty contains section
            final Object load = yaml.load("contains: \n"
                    + "name: blah\n"
                    + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleConstructor typeRuleContext = new YamlPolicy.TypeRuleConstructor(
                "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
            resmap.put("name", "blah, test");
            Assert.assertFalse(typeRuleContext.ruleMatchesContainsSection(resmap, ruleSection));
    }


    @Test public void testRegexPredicate() {
        final YamlPolicy.RegexPredicate blah = new YamlPolicy.RegexPredicate(Pattern.compile("a|b"));

        Assert.assertFalse(blah.evaluate(null));
        Assert.assertFalse(blah.evaluate(new Object()));
        Assert.assertFalse(blah.evaluate("c"));
        Assert.assertFalse(blah.evaluate("ababababa"));
        Assert.assertNotNull(blah.evaluate("a"));
        Assert.assertNotNull(blah.evaluate("b"));

    }

    @Test public void testSetContainsPredicate() {
        final YamlPolicy.SetContainsPredicate blah = new YamlPolicy.SetContainsPredicate("blah");
        final ArrayList<String> strings = new ArrayList<String>();
        Assert.assertFalse(blah.evaluate(strings));
        Assert.assertFalse(blah.evaluate(""));
        Assert.assertFalse(blah.evaluate(null));
        strings.add("nomatch");
        Assert.assertFalse(blah.evaluate(strings));
        Assert.assertFalse(blah.evaluate("nomatch"));
        strings.add("blah");
        Assert.assertNotNull(blah.evaluate(strings));
        Assert.assertNotNull(blah.evaluate("blah"));
        Assert.assertNotNull(blah.evaluate("blah, nomatch"));

        final ArrayList<String> input = new ArrayList<String>();
        input.add("test1");
        input.add("test2");
        final YamlPolicy.SetContainsPredicate multiple = new YamlPolicy.SetContainsPredicate(input);
        final ArrayList<String> strings2 = new ArrayList<String>();
        Assert.assertFalse(multiple.evaluate(strings2));
        Assert.assertFalse(multiple.evaluate(""));
        Assert.assertFalse(multiple.evaluate(null));
        strings2.add("nomatch");
        Assert.assertFalse(multiple.evaluate(strings2));
        Assert.assertFalse(multiple.evaluate("nomatch"));
        strings2.add("blah");
        Assert.assertFalse(multiple.evaluate(strings2));
        Assert.assertFalse(multiple.evaluate("nomatch, blah"));
        strings2.add("test1");
        Assert.assertFalse(multiple.evaluate(strings2));
        Assert.assertFalse(multiple.evaluate("nomatch, blah, test1"));
        strings2.remove("test1");
        strings2.add("test2");
        Assert.assertFalse(multiple.evaluate(strings2));
        Assert.assertFalse(multiple.evaluate("nomatch, blah, test2"));
        strings2.add("test1");

        Assert.assertNotNull(multiple.evaluate(strings2));
        Assert.assertNotNull(multiple.evaluate("nomatch, blah, test1, test2"));
    }

    @Test public void testYamlEnvironmentalContext() throws URISyntaxException {
        {
            final Map context=new HashMap();
            context.put("project", "abc");
            final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
                "test://", context);

            Assert.assertNotNull(test.isValid());
            final HashSet<Attribute> env = new HashSet<Attribute>();

            //empty env
            Assert.assertFalse(test.matches(env));

            //single matching env
            env.add(new Attribute(new URI("test://project"), "abc"));
            Assert.assertNotNull(test.matches(env));

            //multi attrs, matches context value
            env.add(new Attribute(new URI("test://application"), "bloo"));
            Assert.assertNotNull(test.matches(env));
        }
        {
            final Map context=new HashMap();
            context.put("project", "ab[c");
            final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
                "test://", context);

            Assert.assertNotNull(test.isValid());
            final HashSet<Attribute> env = new HashSet<Attribute>();

            //invalid regex should be equality check
            env.add(new Attribute(new URI("test://project"), "abc"));
            Assert.assertFalse(test.matches(env));

            env.clear();
            env.add(new Attribute(new URI("test://project"), "ab[c"));
            Assert.assertNotNull(test.matches(env));

        }
    }

    @Test public void testYamlEnvironmentalContextMultiple() throws URISyntaxException {
        final Map context=new HashMap();
        context.put("project", "abc");
        context.put("application", "bloo");
        final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
            "test://", context);

        Assert.assertNotNull(test.isValid());
        final HashSet<Attribute> env = new HashSet<Attribute>();
        Assert.assertFalse(test.matches(env));
        env.add(new Attribute(new URI("test://project"), "abc"));
        Assert.assertFalse(test.matches(env));
        env.add(new Attribute(new URI("test://application"), "bloo"));
        Assert.assertNotNull(test.matches(env));

        final HashSet<Attribute> env2 = new HashSet<Attribute>();
        env2.add(new Attribute(new URI("test://application"), "bloo"));
        Assert.assertFalse(test.matches(env2));
    }

    @Test public void testYamlEnvironmentalContextInvalid() throws URISyntaxException {
        {
            final Map context=new HashMap();
            ///value is not a string
            context.put("project", new ArrayList());
            final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
                "test://", context);

            Assert.assertFalse(test.isValid());
            Assert.assertNotNull(test.getValidation().contains("Context section: project: expected 'String', saw"));
            final HashSet<Attribute> env = new HashSet<Attribute>();
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://project"), "abc"));
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://application"), "bloo"));
            Assert.assertFalse(test.matches(env));
        }
        {
            final Map context=new HashMap();
            //key is not a string
            context.put(new HashMap(), "monkey");
            final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
                "test://", context);

            Assert.assertFalse(test.isValid());
            Assert.assertNotNull(test.getValidation().contains("Context section key expected 'String', saw"));
            final HashSet<Attribute> env = new HashSet<Attribute>();
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://project"), "abc"));
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://application"), "bloo"));
            Assert.assertFalse(test.matches(env));
        }
        {
            final Map context = new HashMap();
            //key is not a valid URI component
            context.put(" project", "monkey");
            final YamlPolicy.YamlEnvironmentalContext test = new YamlPolicy.YamlEnvironmentalContext(
                "test://", context);

            Assert.assertFalse(test.isValid());
            Assert.assertNotNull(test.getValidation().contains("invalid URI"));
            final HashSet<Attribute> env = new HashSet<Attribute>();
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://project"), "abc"));
            Assert.assertFalse(test.matches(env));
            env.add(new Attribute(new URI("test://application"), "bloo"));
            Assert.assertFalse(test.matches(env));
        }
    }
}
