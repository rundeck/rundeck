import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import grails.test.mixin.TestFor
import org.grails.plugins.metricsweb.MetricService
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService

import javax.security.auth.Subject

@TestFor(FrameworkService)
class FrameworkServiceTests  {

    Properties props1
    void setUp(){
        props1=new Properties()
        props1.setProperty ("a.test","value for a")
        props1.setProperty ("b.test","value for b")
        props1.setProperty ("c.test",'embed a: ${a.test}')
        props1.setProperty ("d.test",'embed c: ${c.test}')
        props1.setProperty ("e.test",'embed 2 a: ${a.test}, ${a.test}')
        props1.setProperty ("f.test",'embed 1 a, 1 b, 1 c: ${a.test}, ${b.test}, ${c.test}')

    }
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
	void tearDown(){
        props1=null
    }

    void testParseOptsFromString1(){
            def m1 = FrameworkService.parseOptsFromString("-test 1")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(1, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
    }

    void testParseOptsFromString2() {
            def m1 = FrameworkService.parseOptsFromString("-test 1 -test2 flamjamps")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.keySet().size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flamjamps", m1['test2'])
    }

    void testParseOptsFromStringQuoted() {
            def m1 = FrameworkService.parseOptsFromString("-test 1 -test2 'flam jamps'")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }

    void testParseOptsFromStringIgnored() {
            def m1 = FrameworkService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
    }

    void testParseOptsIgnoredValues() {
        //ignores unassociated string and trailing -opt
        def m1 = FrameworkService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed -ignored")
        assertNotNull(m1)
        assertEquals(['test':'1',test2:'flam jamps'],m1)
    }

    void testParseOptsFromStringShouldPreserveDashedValue() {
        def m1 = FrameworkService.parseOptsFromString("-test -blah")
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }
    void testParseOptsFromArrayShouldPreserveDashedValue() {
        def m1 = FrameworkService.parseOptsFromArray(["-test","-blah"] as String[])
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }
    def assertTestAuthorizeSet(FrameworkService test, Map expected, Set results, Collection<String> expectActions,
                               String projectName, Closure call){
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        def ctrl = mockFor(AuthContext)
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
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals projectName, attr.value
            } else {
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            return results
        }
        def tfwk = ctrl.createMock()
        call.call(tfwk)
    }
    def assertTestAuthorizeAll(FrameworkService test, Map expected, List results, List<String> expectActions,
                               String projectName, Closure call){
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer(results.size()..results.size()) { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        def ctrl = mockFor(AuthContext)
        def ndx=0
        ctrl.demand.evaluate(results.size()..results.size()) { Set resources, Set actions, Collection env ->
            assertEquals 1, resources.size()
            def res1 = resources.iterator().next()
            assertEquals expected, res1
            assertEquals 1, actions.size()
            assertEquals(expectActions[ndx],actions.first())
            Attribute attr = env.iterator().next()
            if (projectName) {
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals projectName, attr.value
            } else {
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            ndx++
            return [results[ndx-1]] as Set
        }
        def tfwk = ctrl.createMock()
        call.call(tfwk)
    }
    def assertTestAuthorizeSingle(FrameworkService test, Map expected, Map result, String projectName, Closure call){
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String clsName, String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        def ctrl = mockFor(AuthContext)
        ctrl.demand.evaluate { Map resource, String action, Collection env ->
            assertEquals expected, resource
            assertEquals 'test', action
            Attribute attr = env.iterator().next()
            if(projectName){
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals projectName, attr.value
            }else{
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
            }
            return makeDecision(result,resource,action,env)
        }
        def tfwk = ctrl.createMock()
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

    void testAuthorizeProjectJobAll(){
        FrameworkService test= new FrameworkService();
        ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
        def decisions = [[authorized: true]] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject"){
            assertTrue(test.authorizeProjectJobAll(it, job, ['test'] , 'testProject'))
        }
    }

    void testAuthorizeProjectJobAllSingleAuthFalse() {
        FrameworkService test = new FrameworkService();
        ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
        def decisions = [[authorized: false]] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject") {
            assertFalse(test.authorizeProjectJobAll(it, job, ['test'], 'testProject'))
        }
    }

    void testAuthorizeProjectJobAllMultipleAuthFalse() {
        FrameworkService test = new FrameworkService();
        ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
        def decisions = [[authorized: false], [authorized: true]] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject") {
            assertFalse(test.authorizeProjectJobAll(it, job, ['test'], 'testProject'))
        }
    }

    void testAuthorizeProjectJobAllAllMultipleAuthFalse() {
        FrameworkService test = new FrameworkService();
        ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
        def decisions = [[authorized: false], [authorized: false]] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject") {
            assertFalse(test.authorizeProjectJobAll(it, job, ['test'], 'testProject'))
        }
    }

    void testAuthorizeProjectJobAllAllMultipleAuthTrue() {
        FrameworkService test = new FrameworkService();
        ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
        def decisions = [[authorized: true], [authorized: true]] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject") {
            assertTrue(test.authorizeProjectJobAll(it, job, ['test'], 'testProject'))
        }
    }
    void testAuthorizeProjectResources(){
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: true]] as Set
        def resources = [[type: 'job', name: 'name1', group: 'blah/blee']] as Set
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test'], "testProject") { AuthContext it->
            assertEquals(decisions,test.authorizeProjectResources(it, resources, ['test'] as Set, 'testProject'))
        }
    }
    void testAuthorizeProjectResource(){
        FrameworkService test = new FrameworkService();
        def decision = [authorized: true]
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSingle(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, "testProject") { AuthContext it ->
            assertTrue( test.authorizeProjectResource(it, resource, 'test', 'testProject'))
        }
    }
    void testAuthorizeProjectResourceAllSuccess(){
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: true]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test', 'test2'], "testProject") { AuthContext it ->
            assertTrue( test.authorizeProjectResourceAll(it, resource, ['test','test2'], 'testProject'))
        }
    }

    void testAuthorizeProjectResourceAllFailure() {
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: false]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test', 'test2'], "testProject") { AuthContext it ->
            assertFalse(test.authorizeProjectResourceAll(it, resource, ['test', 'test2'], 'testProject'))
        }
    }

    void testAuthorizeProjectResourceAllMixedFailure() {
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: false],[authorized: true]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test', 'test2'], "testProject") { AuthContext it ->
            assertFalse(test.authorizeProjectResourceAll(it, resource, ['test', 'test2'], 'testProject'))
        }
    }

    void testAuthorizeProjectResourceAllAllFailure() {
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: false],[authorized: false]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test', 'test2'], "testProject") { AuthContext it ->
            assertFalse(test.authorizeProjectResourceAll(it, resource, ['test', 'test2'], 'testProject'))
        }
    }

    void testAuthorizeProjectResourceAllMultiSuccess() {
        FrameworkService test = new FrameworkService();
        def decisions = [[authorized: true],[authorized: true]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decisions, ['test', 'test2'], "testProject") { AuthContext it ->
            assertTrue(test.authorizeProjectResourceAll(it, resource, ['test', 'test2'], 'testProject'))
        }
    }
    void testAuthResourceForJob(){

        FrameworkService test = new FrameworkService();
        test:{
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee')
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            assertEquals expected,test.authResourceForJob(job)
        }
        test:{
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: 'blah')
            def expected = [type: 'job', name: 'name1', group: 'blah']
            assertEquals expected,test.authResourceForJob(job)
        }
        test:{
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: '')
            def expected = [type: 'job', name: 'name1', group: '']
            assertEquals expected,test.authResourceForJob(job)
        }
        test:{
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1', groupPath: null)
            def expected = [type: 'job', name: 'name1', group: '']
            assertEquals expected,test.authResourceForJob(job)
        }
    }
    void testAuthResourceForJobParams(){

        FrameworkService test = new FrameworkService();
        test:{
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            assertEquals expected,test.authResourceForJob('name1','blah/blee')
        }
        test:{
            def expected = [type: 'job', name: 'name1', group: 'blah']
            assertEquals expected,test.authResourceForJob('name1','blah')
        }
        test:{
            def expected = [type: 'job', name: 'name1', group: '']
            assertEquals expected,test.authResourceForJob('name1','')
        }
        test:{
            def expected = [type: 'job', name: 'name1', group: '']
            assertEquals expected,test.authResourceForJob('name1',null)
        }
    }
    void testAuthorizeApplicationResourceSuccess(){
        FrameworkService test = new FrameworkService();
        def decision = [authorized: true]
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSingle(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResource(it, resource, 'test'))
        }
    }

    void testAuthorizeApplicationResourceFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [authorized: false]
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSingle(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision,
                null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResource(it, resource, 'test'))
        }
    }

    void testAuthorizeApplicationResourceAnySuccess() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: true], [authorized: false]]
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeAll(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, ['test', 'test2'], null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceAny(it, resource, ['test', 'test2']))
        }
        def decision2 = [[authorized: false], [authorized: true]]
        assertTestAuthorizeAll(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision2, ['test', 'test2'],
                null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceAny(it, resource, ['test', 'test2']))
        }
        def decision3 = [[authorized: true], [authorized: true]]
        assertTestAuthorizeAll(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision2, ['test', 'test2'],
                null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceAny(it, resource, ['test', 'test2']))
        }
    }
    void testAuthorizeApplicationResourceAnyFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false], [authorized: false]]
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeAll(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceAny(it, resource, ['test', 'test2']))
        }
    }
    void testAuthorizeApplicationResourceAllSuccess(){
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: true]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, ['test','test2'], null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceAll(it, resource, ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceAllFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceAll(it, resource, ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceAllMultiMixed() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false],[authorized: true]] as Set
        def resource = [type: 'job', name: 'name1', group: 'blah/blee']
        assertTestAuthorizeSet(test, [type: 'job', name: 'name1', group: 'blah/blee'], decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceAll(it, resource, ['test', 'test2']))
        }
    }


    void testAuthorizeApplicationResourceType() {
        FrameworkService test = new FrameworkService();
        def decision = [authorized: true]
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSingle(test, expected, decision,
                null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceType(it, 'aType', 'test'))
        }
    }
    void testAuthorizeApplicationResourceTypeAllSuccess() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: true]] as Set
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSet(test, expected, decision, ['test', 'test2'], null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceTypeAll(it, 'aType', ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceTypeAllFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false]] as Set
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSet(test, expected, decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceTypeAll(it, 'aType', ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceTypeAllMultiFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false], [authorized: false]] as Set
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSet(test, expected, decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceTypeAll(it, 'aType', ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceTypeAllMixedFailure() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: false], [authorized: true]] as Set
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSet(test, expected, decision, ['test', 'test2'], null) { AuthContext it ->
            assertFalse(test.authorizeApplicationResourceTypeAll(it, 'aType', ['test', 'test2']))
        }
    }

    void testAuthorizeApplicationResourceTypeAllMultiSuccess() {
        FrameworkService test = new FrameworkService();
        def decision = [[authorized: true], [authorized: true]] as Set
        def expected = [type: 'resource', kind: 'aType']
        assertTestAuthorizeSet(test, expected, decision, ['test', 'test2'], null) { AuthContext it ->
            assertTrue(test.authorizeApplicationResourceTypeAll(it, 'aType', ['test', 'test2']))
        }
    }
}
