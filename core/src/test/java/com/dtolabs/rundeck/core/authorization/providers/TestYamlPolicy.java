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
import junit.framework.TestCase;
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
    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_description_required(){

        //test "description" is required
            final Map map = new HashMap();
            final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext;
        yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory);
    }

    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_type_required() {
        //test resource requires "type"
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new HashMap());
        final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory);

    }

    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_for_contents() {
        //for: must be map
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", "test1");
        final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext;
        yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory);
        Assert.fail("Expected syntax error");
    }

    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_for_must_be_map() {
        //for: must be map
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new ArrayList());
        final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map, null,typeContextFactory);
    }
    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_for_must_exist() {
        //for: must not be null
        final Map map = new HashMap();
        map.put("description", "test1");
//            map.put("for", new ArrayList());
        final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory);
    }

    @Test(expected = YamlPolicy.AclPolicySyntaxException.class)
    public void testYamlAclContext_for_not_empty() {
        //for: may not be empty
        final Map map = new HashMap();
        map.put("description", "test1");
        map.put("for", new HashMap());
        final YamlPolicy.TypeContextFactory typeContextFactory = null;
        final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory);
    }




    @Test public void testYamlAclContext_no_rules() {
        //if type!='job' and rules: exists, it does not use legacy
        final Map map = new HashMap();
        map.put("description", "test1");
        final HashMap forRules = new HashMap();
        ArrayList value = new ArrayList();
        value.add(new HashMap<>());
        forRules.put("testtype", value);
        map.put("for", forRules);
        map.put("rules", new HashMap());
        final TestTypeContextFactory typeContextFactory = new TestTypeContextFactory();
        typeContextFactory.context = new AclContext() {
            public ContextDecision includes(Map<String, String> resource, String action) {
                return new ContextDecision(Explanation.Code.REJECTED, false);
            }

            @Override
            public Set<AclRule> createRules(final AclRuleBuilder prototype) {
                return null;
            }
        };
        final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map, null, typeContextFactory);
        final HashMap<String, String> resmap = new HashMap<String, String>();
        resmap.put("type", "bob");

        final ContextDecision includes = yamlAclContext.includes(resmap, null);
        Assert.assertFalse(includes.granted());
        Assert.assertEquals(Explanation.Code.REJECTED_NO_RULES_DECLARED, includes.getCode());
    }



    @Test public void testYamlAclContext() {
            //otherwise, uses TypeContext
            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("type", "testtype");
            final Map map = new HashMap();
            map.put("description", "test1");
            final HashMap forRules = new HashMap();
            forRules.put("testtype", Arrays.asList(new HashMap()));
            map.put("for", forRules);
            map.put("rules", new HashMap());
            final TestTypeContextFactory typeContextFactory = new TestTypeContextFactory();
            final ContextDecision res2 = new ContextDecision(Explanation.Code.REJECTED, false);
            typeContextFactory.context = new AclContext() {
                public ContextDecision includes(Map<String, String> resource, String action) {
                    return res2;
                }

                @Override
                public Set<AclRule> createRules(final AclRuleBuilder prototype) {
                    return null;
                }
            };
            final ContextDecision res1 = new ContextDecision(Explanation.Code.REJECTED, false);
            final YamlPolicy.YamlAclContext yamlAclContext = new YamlPolicy.YamlAclContext(map,null, typeContextFactory
            );

            final ContextDecision includes = yamlAclContext.includes(resmap, null);
            Assert.assertNotNull(typeContextFactory.called);
            Assert.assertNotNull(typeContextFactory.typeSection);
            Assert.assertEquals(res2, includes);
        }

    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_single() {

            //test a single allow results in granted decision
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertNotNull(includes.granted());
            Assert.assertEquals(Explanation.Code.GRANTED,includes.getCode());

        }
    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_deny() {
            //test a single deny results in deny decision
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED_DENIED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED,includes.getCode());

        }
    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_reject() {
            //test a single reject results in reject decision
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, includes.getCode());

        }

        //test multiple results


    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_reject_grant() {
            //test a [REJECT*,GRANT] results in GRANT
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertNotNull(includes.granted());
            Assert.assertEquals(Explanation.Code.GRANTED, includes.getCode());

        }
    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_multiple_with_deny() {
            //test a [REJECT*,GRANT*,DENY] results in DENY
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED_DENIED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, includes.getCode());

        }
    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_grant_deny() {
            //test a [GRANT*,DENY] results in DENY
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED_DENIED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, includes.getCode());

        }
    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_reject_deny() {
            //test a [REJECT*,DENY] results in DENY
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED_DENIED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, includes.getCode());

        }

        //test subevaluations will expose a DENY result

    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_multi_with_deny() {
            //test a [GRANT,REJECT] with DENY evaluation results in DENY
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            final List<ContextEvaluation> evals = new ArrayList<ContextEvaluation>();
            evals.add(new ContextEvaluation(Explanation.Code.REJECTED, "reject"));
            evals.add(new ContextEvaluation(Explanation.Code.REJECTED, "reject"));
            evals.add(new ContextEvaluation(Explanation.Code.GRANTED, "granted"));
            evals.add(new ContextEvaluation(Explanation.Code.REJECTED_DENIED, "denied"));
            evals.add(new ContextEvaluation(Explanation.Code.REJECTED, "reject"));
            evals.add(new ContextEvaluation(Explanation.Code.REJECTED, "reject"));
            evals.add(new ContextEvaluation(Explanation.Code.GRANTED, "granted"));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false, evals));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, includes.getCode());

        }


        //test matcher that do not match are ignored


    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_only_matches() {
            //only matches apply
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(false, Explanation.Code.REJECTED_DENIED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertFalse(includes.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, includes.getCode());

        }

    /**
     * test evaluation of rules within a type
     */
    @Test public void testTypeContext_only_matches_granted() {
            //only matches apply
            final List<YamlPolicy.ContextMatcher> contextMatchers = new ArrayList<YamlPolicy.ContextMatcher>();
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.GRANTED, true));
            contextMatchers.add(createTestMatcher(false, Explanation.Code.REJECTED_DENIED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            contextMatchers.add(createTestMatcher(true, Explanation.Code.REJECTED, false));
            final YamlPolicy.TypeContext typeContext = new YamlPolicy.TypeContext(contextMatchers);

            final ContextDecision includes = typeContext.includes(null, null);
            Assert.assertNotNull(includes.granted());
            Assert.assertEquals(Explanation.Code.GRANTED, includes.getCode());


    }

    private YamlPolicy.ContextMatcher createTestMatcher(final boolean matched, final Explanation.Code code,
                                                        final boolean granted) {
        return createTestMatcher(matched, code, granted, new ArrayList<ContextEvaluation>());
    }

    private YamlPolicy.ContextMatcher createTestMatcher(final boolean matched, final Explanation.Code code,
                                                        final boolean granted,
                                                        final List<ContextEvaluation> contextEvaluations) {
        return new YamlPolicy.ContextMatcher() {
            public YamlPolicy.MatchedContext includes(Map<String, String> resource, String action) {
                return new YamlPolicy.MatchedContext(matched, new ContextDecision(code, granted, contextEvaluations));
            }

            @Override
            public AclRule createRule(final AclRuleBuilder prototype) {
                return null;
            }
        };
    }

    @Test public void testTypeRuleContextMatcherEvaluateActionsAllow_default_reject() {
            //no allow or deny should result in REJECTED
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsAllow_allow_all() {
            //allow '*' should allow any action 
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertNotNull(decision.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsAllow_specific() {
            //allow string should only allow exact action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: 'testaction'"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsAllow_inlist() {
            //allow list should allow any action in the list
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: ['testaction',zah,zee]"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertNotNull(decision3.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertNotNull(decision4.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsAllow_all_in_list() {
            //allow list with '*' will also allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: ['*',zah,zee]"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertNotNull(decision.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertNotNull(decision3.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertNotNull(decision4.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
        }



    @Test public void testTypeRuleContextMatcherEvaluateActionsDeny_all() {
            //allow '*' should deny any action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsDeny_specific() {
            //deny string should only deny exact action
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'testaction'"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsDeny_inlist() {
            //deny list should deny any action in the list
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['testaction',zah,zee]"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsDeny_all_inlist() {
            //deny list with '*' will also deny all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*',zah,zee]"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("testaction", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zah", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());

    }

    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_all_deny() {
            //allow '*' and deny '*' should always deny
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: '*'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_all_deny_2() {
            //allow 'X' and deny '*' should always deny
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: 'blah'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher("x",
                                                                                                            ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_all_deny_specific() {
            //allow '*' and deny 'X' should only deny X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'blah'\n"
                                          + "allow: '*'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blah blee", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertNotNull(decision3.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_deny() {
            //allow 'X' and deny 'Y' should only deny Y and only allow X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'blah'\n"
                                          + "allow: 'blee'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("test2", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision2.getCode());
            Assert.assertEquals(0, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("blah blee", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision4.getCode());
            Assert.assertEquals(0, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertNotNull(decision3.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_deny_all() {
            //allow List and deny '*' should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: [abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_all_deny_all() {
            //allow List with '*' and deny '*' should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: '*'\n"
                                          + "allow: ['*',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("blee", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_all_deny_x() {
            //allow List with '*' and deny 'X' should deny X only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'zam'\n"
                                          + "allow: ['*',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertNotNull(decision.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertNotNull(decision4.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_deny_x() {
            //allow List and deny 'X' should deny X only, and allow list only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: 'zam'\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertNotNull(decision4.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            Assert.assertNotNull(decision5);
            Assert.assertNotNull(decision5.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision5.getCode());
            Assert.assertEquals(1, decision5.getEvaluations().size());
            contextEvaluations.clear();

            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_deny_list() {
            //allow List and deny List should deny list only, and allow list only
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['zam','zee']\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertNotNull(decision4.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            Assert.assertNotNull(decision5);
            Assert.assertNotNull(decision5.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision5.getCode());
            Assert.assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_deny_list_all() {
            //allow List and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: ['ghi',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            Assert.assertNotNull(decision5);
            Assert.assertFalse(decision5.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision5.getCode());
            Assert.assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_list_all_deny_list_all() {
            //allow List with * and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: ['*',abc,def]\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());
            contextEvaluations.clear();


            contextEvaluations.clear();
            ContextDecision decision5 = typeRuleContext.evaluateActions("ghi", contextEvaluations);
            Assert.assertNotNull(decision5);
            Assert.assertFalse(decision5.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision5.getCode());
            Assert.assertEquals(1, decision5.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_x_deny_list() {
            //allow X and deny List should deny only in list, grant only X
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['zam','zee']\n"
                                          + "allow: abc\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision.getCode());
            Assert.assertEquals(0, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertNotNull(decision2.granted());
            Assert.assertEquals(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED, decision4.getCode());
            Assert.assertEquals(0, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_x_deny_list_all() {
            //allow X and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: abc\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
        }
    @Test public void testTypeRuleContextMatcherEvaluateActionsCombined_allow_all_deny_list_all() {
            //allow '*' and deny List with * should deny all
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "deny: ['*','zee']\n"
                                          + "allow: '*'\n"
            );
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();
            resmap.put("name", "blah");

            final List<ContextEvaluation> contextEvaluations = new ArrayList<ContextEvaluation>();
            ContextDecision decision = typeRuleContext.evaluateActions("blah", contextEvaluations);
            Assert.assertNotNull(decision);
            Assert.assertFalse(decision.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision.getCode());
            Assert.assertEquals(1, decision.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision2 = typeRuleContext.evaluateActions("abc", contextEvaluations);
            Assert.assertNotNull(decision2);
            Assert.assertFalse(decision2.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision2.getCode());
            Assert.assertEquals(1, decision2.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision4 = typeRuleContext.evaluateActions("def", contextEvaluations);
            Assert.assertNotNull(decision4);
            Assert.assertFalse(decision4.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision4.getCode());
            Assert.assertEquals(1, decision4.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision3 = typeRuleContext.evaluateActions("zam", contextEvaluations);
            Assert.assertNotNull(decision3);
            Assert.assertFalse(decision3.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision3.getCode());
            Assert.assertEquals(1, decision3.getEvaluations().size());

            contextEvaluations.clear();
            ContextDecision decision6 = typeRuleContext.evaluateActions("zee", contextEvaluations);
            Assert.assertNotNull(decision6);
            Assert.assertFalse(decision6.granted());
            Assert.assertEquals(Explanation.Code.REJECTED_DENIED, decision6.getCode());
            Assert.assertEquals(1, decision6.getEvaluations().size());
    }

    @Test public void testApplyTest() {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
        final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
        final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
        final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
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

    @Test public void testTypeRuleContextMatcher() {
        {
            //match any resource without any match constraints
            final Object load = yaml.load("allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //true result for any input
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            Assert.assertNotNull(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            Assert.assertNotNull(any);
            Assert.assertNotNull(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());

        }
        {
            //match any resource with name=~ blah, and allow all actions
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            Assert.assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            Assert.assertNotNull(any);
            Assert.assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());

        }
        {
            //add other match sections
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "equals: \n"
                                          + "  group: potato\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            Assert.assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            Assert.assertNotNull(any);
            Assert.assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'group'
            resmap.put("group", "loop");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("group", "potato");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());

        }
        {
            //add other match sections
            final Object load = yaml.load("match: \n"
                                          + "  name: '.*blah.*'\n"
                                          + "equals: \n"
                                          + "  group: potato\n"
                                          + "contains: \n"
                                          + "  elf: [brand,wake]\n"
                                          + "allow: '*'");
            Assert.assertNotNull(load instanceof Map);
            final Map ruleSection = (Map) load;
            final YamlPolicy.TypeRuleContextMatcher typeRuleContext = new YamlPolicy.TypeRuleContextMatcher(
            "x",                 ruleSection,null, 1,null);

            final HashMap<String, String> resmap = new HashMap<String, String>();

            //false result for no match
            ArrayList<ContextEvaluation> list = new ArrayList<ContextEvaluation>();
            Assert.assertFalse(typeRuleContext.matchesRuleSections(resmap, list));
            final YamlPolicy.MatchedContext any = typeRuleContext.includes(resmap, "any");
            Assert.assertNotNull(any);
            Assert.assertFalse(any.getDecision().getEvaluations().toString(), any.isMatched());

            resmap.put("name", "something");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "blah");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("name", "ablahz");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'group'
            resmap.put("group", "loop");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("group", "potato");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());

            //set 'elf' attribute
            resmap.put("elf", "brand, plaid");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("elf", "wake, plaid");
            Assert.assertFalse(typeRuleContext.includes(resmap, "any").isMatched());
            resmap.put("elf", "wake, plaid, milk, brand");
            Assert.assertNotNull(typeRuleContext.includes(resmap, "any").isMatched());

        }
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



    private static class TestTypeContextFactory implements YamlPolicy.TypeContextFactory {
        boolean called;
        AclContext context;
        List typeSection;
        String type;

        public AclContext createAclContext(String type, List typeSection) {
            called=true;
            this.type=type;
            this.typeSection=typeSection;
            return context;
        }
    }
}
