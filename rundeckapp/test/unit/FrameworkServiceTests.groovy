import com.dtolabs.rundeck.core.authorization.Attribute
import grails.test.mixin.TestFor
import org.grails.plugins.metricsweb.MetricService
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService

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

    void testParseOptsFromString(){
        def FrameworkService testService = new FrameworkService();

        test:{
            def m1 = testService.parseOptsFromString("-test 1")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(1, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 flamjamps")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.keySet().size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flamjamps", m1['test2'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps'")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }
    }

    void testParseOptsIgnoredValues() {
        def FrameworkService testService = new FrameworkService();
        //ignores unassociated string and trailing -opt
        def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed -ignored")
        assertNotNull(m1)
        assertEquals(['test':'1',test2:'flam jamps'],m1)
    }

    void testParseOptsFromStringShouldPreserveDashedValue() {
        def FrameworkService testService = new FrameworkService();
        def m1 = testService.parseOptsFromString("-test -blah")
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }
    void testParseOptsFromArrayShouldPreserveDashedValue() {
        def FrameworkService testService = new FrameworkService();
        def m1 = testService.parseOptsFromArray(["-test","-blah"] as String[])
        assertNotNull(m1)
        assertTrue(m1 instanceof Map<String, String>)
        assertEquals(1, m1.size())
        assertNotNull(m1['test'])
        assertEquals("-blah", m1['test'])
    }

    void testAuthorizeProjectJobAll(){
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            //single authorization is true
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1',groupPath:'blah/blee')
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env-> 
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            assertTrue(test.authorizeProjectJobAll(tfwk,job,['test'],'testProject'))
        }
    }

    void testAuthorizeProjectJobAllSingleAuthFalse() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            //single authorization is false
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1',groupPath:'blah/blee')
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals "testProject", attr.value
                return [[authorized:false]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            assertFalse(test.authorizeProjectJobAll(tfwk,job,['test'],'testProject'))
        }
    }

    void testAuthorizeProjectJobAllMultipleAuthFalse() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            //one of multiple authorization is false
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1',groupPath:'blah/blee')
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals "testProject", attr.value
                return [[authorized:false],[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            assertFalse(test.authorizeProjectJobAll(tfwk,job,['test'],'testProject'))
        }
    }

    void testAuthorizeProjectJobAllAllMultipleAuthFalse() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            //all of multiple authorization is false
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1',groupPath:'blah/blee')
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals "testProject", attr.value
                return [[authorized:false],[authorized:false]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            assertFalse(test.authorizeProjectJobAll(tfwk,job,['test'],'testProject'))
        }
    }

    void testAuthorizeProjectJobAllAllMultipleAuthTrue() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            //all of multiple authorization is true
            ScheduledExecution job = new ScheduledExecution(jobName: 'name1',groupPath:'blah/blee')
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project", attr.property.toString()
                assertEquals "testProject", attr.value
                return [[authorized:true],[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            assertTrue(test.authorizeProjectJobAll(tfwk,job,['test'],'testProject'))
        }
    }
    void testAuthorizeProjectResources(){
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 1,actions.size()
                assertEquals 'test',actions.iterator().next()
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resources = [[type: 'job', name: 'name1', group: 'blah/blee']] as Set
            final result = test.authorizeProjectResources(tfwk, resources, ['test'] as Set, 'testProject')
        }
    }
    void testAuthorizeProjectResource(){
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Map res1, subject, String action, Collection env->
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 'test',action
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [authorized:true]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeProjectResource(tfwk, resource, 'test', 'testProject')
        }
    }
    void testAuthorizeProjectResourceAllSuccess(){
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('test')
                assertTrue actions.contains('test2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeProjectResourceAll(tfwk, resource, ['test','test2'], 'testProject')
        }
    }

    void testAuthorizeProjectResourceAllFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('test')
                assertTrue actions.contains('test2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:false]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeProjectResourceAll(tfwk, resource, ['test','test2'], 'testProject')
        }
    }

    void testAuthorizeProjectResourceAllMixedFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('test')
                assertTrue actions.contains('test2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:false],[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeProjectResourceAll(tfwk, resource, ['test','test2'], 'testProject')
        }
    }

    void testAuthorizeProjectResourceAllAllFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('test')
                assertTrue actions.contains('test2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:false],[authorized:false]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeProjectResourceAll(tfwk, resource, ['test','test2'], 'testProject')
        }
    }

    void testAuthorizeProjectResourceAllMultiSuccess() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('test')
                assertTrue actions.contains('test2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/project",attr.property.toString()
                assertEquals "testProject",attr.value
                return [[authorized:true],[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeProjectResourceAll(tfwk, resource, ['test','test2'], 'testProject')
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
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Map res1, subject, String action, Collection env->
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 'testAction', action
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application",attr.property.toString()
                assertEquals "rundeck",attr.value
                return [authorized:true]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeApplicationResource(tfwk, resource, 'testAction')
        }
    }

    void testAuthorizeApplicationResourceFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos = { Map res1, subject, String action, Collection env ->
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 'testAction', action
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [authorized:false]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeApplicationResource(tfwk, resource, 'testAction')
        }
    }
    void testAuthorizeApplicationResourceAllSuccess(){
        FrameworkService test= new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test:{
            def expected=[type:'job',name:'name1',group:'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr= { return [  subject: 'subject1'] }
            def evalClos={ Set resources, subject, Set actions, Collection env->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('testAction')
                assertTrue actions.contains('testAction2')
                Attribute attr=env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application",attr.property.toString()
                assertEquals "rundeck",attr.value
                return [[authorized:true]]
            }
            tfwk.getAuthorizationMgr= { return [ evaluate:evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeApplicationResourceAll(tfwk, resource, ['testAction','testAction2'])
        }
    }

    void testAuthorizeApplicationResourceAllFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1, resources.size()
                def res1 = resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('testAction')
                assertTrue actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeApplicationResourceAll(tfwk, resource, ['testAction', 'testAction2'])
        }
    }

    void testAuthorizeApplicationResourceAllMultiMixed() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1, resources.size()
                def res1 = resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('testAction')
                assertTrue actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false],[authorized:true]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeApplicationResourceAll(tfwk, resource, ['testAction', 'testAction2'])
        }
    }

    void testAuthorizeApplicationResourceAllMultiFail() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1, resources.size()
                def res1 = resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('testAction')
                assertTrue actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false],[authorized:false]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertFalse test.authorizeApplicationResourceAll(tfwk, resource, ['testAction', 'testAction2'])
        }
    }

    void testAuthorizeApplicationResourceAllMultiSuccess() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'job', name: 'name1', group: 'blah/blee']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1, resources.size()
                def res1 = resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue actions.contains('testAction')
                assertTrue actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: true],[authorized:true]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            assertTrue test.authorizeApplicationResourceAll(tfwk, resource, ['testAction', 'testAction2'])
        }
    }

    void testAuthorizeApplicationResourceType() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Map res1, subject, String action, Collection env ->
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 'testAction', action
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [authorized: true]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertTrue test.authorizeApplicationResourceType(tfwk, 'aType', 'testAction')
        }
    }
    void testAuthorizeApplicationResourceTypeAllSuccess() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue  actions.contains('testAction')
                assertTrue  actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: true]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertTrue test.authorizeApplicationResourceTypeAll(tfwk, 'aType', ['testAction','testAction2'])
        }
    }

    void testAuthorizeApplicationResourceTypeAllFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue  actions.contains('testAction')
                assertTrue  actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertFalse test.authorizeApplicationResourceTypeAll(tfwk, 'aType', ['testAction','testAction2'])
        }
    }

    void testAuthorizeApplicationResourceTypeAllMultiFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue  actions.contains('testAction')
                assertTrue  actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false],[authorized:false]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertFalse test.authorizeApplicationResourceTypeAll(tfwk, 'aType', ['testAction','testAction2'])
        }
    }

    void testAuthorizeApplicationResourceTypeAllMixedFailure() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue  actions.contains('testAction')
                assertTrue  actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: false],[authorized:true]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertFalse test.authorizeApplicationResourceTypeAll(tfwk, 'aType', ['testAction','testAction2'])
        }
    }

    void testAuthorizeApplicationResourceTypeAllMultiSuccess() {
        FrameworkService test = new FrameworkService();
        def mcontrol = mockFor(MetricService, false)
        mcontrol.demand.withTimer() { String argString, Closure clos ->
            clos.call()
        }
        test.metricService = mcontrol.createMock()
        test: {
            def expected = [type: 'resource', kind:'aType']
            def tfwk = new Expando()
            tfwk.getAuthenticationMgr = { return [subject: 'subject1'] }
            def evalClos = { Set resources, subject, Set actions, Collection env ->
                assertEquals 1,resources.size()
                def res1=resources.iterator().next()
                assertEquals expected, res1
                assertEquals 'subject1', subject
                assertEquals 2, actions.size()
                assertTrue  actions.contains('testAction')
                assertTrue  actions.contains('testAction2')
                Attribute attr = env.iterator().next()
                assertEquals "http://dtolabs.com/rundeck/env/application", attr.property.toString()
                assertEquals "rundeck", attr.value
                return [[authorized: true],[authorized:true]]
            }
            tfwk.getAuthorizationMgr = { return [evaluate: evalClos] }
            assertTrue test.authorizeApplicationResourceTypeAll(tfwk, 'aType', ['testAction','testAction2'])
        }
    }
}
