package rundeck

import com.dtolabs.rundeck.core.authorization.Attribute

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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import groovy.mock.interceptor.MockFor
import org.grails.plugins.metricsweb.MetricService
import org.junit.After
import org.junit.Before
import org.junit.Test
import rundeck.data.util.OptionsParserUtil
import rundeck.services.FrameworkService

import javax.security.auth.Subject

import static org.junit.Assert.*

class FrameworkServiceTests  {

    Properties props1
    @Before
    void setUp(){
        props1=new Properties()
        props1.setProperty ("a.test","value for a")
        props1.setProperty ("b.test","value for b")
        props1.setProperty ("c.test",'embed a: ${a.test}')
        props1.setProperty ("d.test",'embed c: ${c.test}')
        props1.setProperty ("e.test",'embed 2 a: ${a.test}, ${a.test}')
        props1.setProperty ("f.test",'embed 1 a, 1 b, 1 c: ${a.test}, ${b.test}, ${c.test}')

    }

    @Test
    void testPropertyUtil() {
        assert 6 == props1.size()
        assert "value for a"==props1.getProperty("a.test")
        assert 'embed a: ${a.test}' == props1.get("c.test")

        Properties props2 = com.dtolabs.rundeck.core.utils.PropertyUtil.expand(props1)

        //test that props without embedded ant refs are not modified
        String t1 = props2.getProperty('a.test')
        assertEquals("a.test result was not correct: ${t1}", 'value for a',t1)

        t1 = props2.getProperty('b.test')
        assertEquals("b.test result was not correct: ${t1}",'value for b',t1)

        //test non existent prop
        t1 = props2.getProperty('z.test')
        assertNull(t1)

        //test single embedded ant ref
        t1 = props2.getProperty('c.test')
        assertEquals("embeded a test is wrong: ${t1}",'embed a: value for a',t1)

        //test double embedded ant ref
        t1 = props2.getProperty('d.test')
        assertEquals("embeded c test is wrong: ${t1}",'embed c: embed a: value for a',t1)

        //test two embedded ant refs
        t1 = props2.getProperty('e.test')
        assertEquals("embeded 2 a test is wrong: ${t1}",'embed 2 a: value for a, value for a',t1)

        //test multiples
        t1 = props2.getProperty('f.test')
        assertEquals("embeded multiple test is wrong: ${t1}",'embed 1 a, 1 b, 1 c: value for a, value for b, embed a: value for a',t1)
    }

    @After
	void tearDown(){
        props1=null
    }

    @Test
    void testParseOptsFromString1(){
            def m1 = OptionsParserUtil.parseOptsFromString("-test 1")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(1, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
    }

    @Test
    void testParseOptsFromString2() {
            def m1 = OptionsParserUtil.parseOptsFromString("-test 1 -test2 flamjamps")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.keySet().size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flamjamps", m1['test2'])
    }

    @Test
    void testParseOptsFromStringQuoted() {
            def m1 = OptionsParserUtil.parseOptsFromString("-test 1 -test2 'flam jamps'")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }

    @Test
    void testParseOptsFromStringIgnored() {
            def m1 = OptionsParserUtil.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
    }

    @Test
    void testParseOptsIgnoredValues() {
        //ignores unassociated string and trailing -opt
        def m1 = OptionsParserUtil.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed -ignored")
        assertNotNull(m1)
        assertEquals(['test':'1',test2:'flam jamps'],m1)
    }

    @Test
    void testParseOptsFromStringShouldPreserveDashedValue() {
        def m1 = OptionsParserUtil.parseOptsFromString("-test -blah")
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }
    @Test
    void testParseOptsFromArrayShouldPreserveDashedValue() {
        def m1 = OptionsParserUtil.parseOptsFromArray(["-test", "-blah"] as String[])
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }
    def assertTestAuthorizeSet(FrameworkService test, Map expected, Set results, Collection<String> expectActions,
                               String projectName, Closure call){
        def mcontrol = new MockFor(MetricService, false)
        mcontrol.demand.withTimer() { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.proxyInstance()
        def ctrl = new MockFor(AuthContext)
        ctrl.demand.evaluate { Set resources, Set actions, Collection env ->
            assertEquals 1, resources.size()
            def res1 = resources.iterator().next()
            assertEquals expected, res1
            assertEquals expectActions.size(), actions.size()
            expectActions.each{
                assertTrue(actions.contains(it))
            }
            Attribute attr = env.iterator().next()
            if (projectName) {
                assertEquals "rundeck:auth:env:project", attr.property.toString()
                assertEquals projectName, attr.value
            } else {
                assertEquals "rundeck:auth:env:application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            return results
        }
        def tfwk = ctrl.proxyInstance()
        call.call(tfwk)
    }
    def assertTestAuthorizeAll(FrameworkService test, Map expected, List results, List<String> expectActions,
                               String projectName, Closure call){
        def mcontrol = new MockFor(MetricService, false)
        mcontrol.demand.withTimer(results.size()..results.size()) { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.proxyInstance()
        def ctrl = new MockFor(AuthContext)
        def ndx=0
        ctrl.demand.evaluate(results.size()..results.size()) { Set resources, Set actions, Collection env ->
            assertEquals 1, resources.size()
            def res1 = resources.iterator().next()
            assertEquals expected, res1
            assertEquals 1, actions.size()
            assertEquals(expectActions[ndx],actions.first())
            Attribute attr = env.iterator().next()
            if (projectName) {
                assertEquals "rundeck:auth:env:project", attr.property.toString()
                assertEquals projectName, attr.value
            } else {
                assertEquals "rundeck:auth:env:application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            ndx++
            return [results[ndx-1]] as Set
        }
        def tfwk = ctrl.proxyInstance()
        call.call(tfwk)
    }
    def assertTestAuthorizeSingle(FrameworkService test, Map expected, Map result, String projectName, Closure call){
        def mcontrol = new MockFor(MetricService, false)
        mcontrol.demand.withTimer() { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.proxyInstance()
        def ctrl = new MockFor(AuthContext)
        ctrl.demand.evaluate { Map resource, String action, Collection env ->
            assertEquals expected, resource
            assertEquals 'test', action
            Attribute attr = env.iterator().next()
            if(projectName){
                assertEquals "rundeck:auth:env:project", attr.property.toString()
                assertEquals projectName, attr.value
            }else{
                assertEquals "rundeck:auth:env:application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            return makeDecision(result,resource,action,env)
        }
        def tfwk = ctrl.proxyInstance()
        call.call(tfwk)
    }

    def makeDecision(Map decision,Map resource, String action, Set<Attribute> environment) {
        return new Decision(){
            @Override
            boolean isAuthorized() {
                decision.authorized?true:false
            }

            @Override
            Explanation explain() {
                return null
            }

            @Override
            long evaluationDuration() {
                return 0
            }

            @Override
            Map<String, String> getResource() {
                return resource
            }

            @Override
            String getAction() {
                return action
            }

            @Override
            Set<Attribute> getEnvironment() {
                return environment
            }

            @Override
            Subject getSubject() {
                return null
            }
        }
    }


}
