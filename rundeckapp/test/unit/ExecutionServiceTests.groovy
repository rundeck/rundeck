import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandExecutionItem
import com.dtolabs.rundeck.core.utils.NodeSet

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.web.ControllerUnitTestMixin;

import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin;
import org.grails.plugins.metricsweb.MetricService
import org.junit.Before;

import rundeck.ScheduledExecution
import rundeck.User
import rundeck.Workflow
import rundeck.Execution
import rundeck.CommandExec
import rundeck.Option
import rundeck.WorkflowStep
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.ReportService
import rundeck.services.StorageService

@TestFor(ExecutionService)
@Mock([ScheduledExecution,Workflow,WorkflowStep,Execution,CommandExec,Option,User])
@TestMixin(ControllerUnitTestMixin)
class ExecutionServiceTests  {
    
    @Before
    public void setup(){
        // hack for 2.3.9:  https://jira.grails.org/browse/GRAILS-11136
        defineBeans(new DataBindingGrailsPlugin().doWithSpring)
    }

    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }

    void testCreateExecutionRunning(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.workflow.save())
        assertNotNull(se.save())
        Execution e = new Execution(project:"AProject",user:'bob',dateStarted: new Date(),dateCompleted: null,scheduledExecution: se,workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
        def valid=e.validate()
        e.errors.allErrors.each {println it.toString() }
        assertTrue(valid)
        assertNotNull(e.save())

//        ScheduledExecution.metaClass.static.lock={id-> return se}
//        def myCriteria = new Expando();
//        myCriteria.get = {Closure cls -> return [id:123]}
//        Execution.metaClass.static.createCriteria = {myCriteria }
//        Execution.metaClass.static.executeQuery = {q,h->[[id: 123]]}
        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
        try{
            svc.createExecution(se,"user1")
            fail("should fail")
        }catch(ExecutionServiceException ex){
            assertTrue(ex.message.contains('is currently being executed'))
        }
    }
    void testCreateExecutionRunningMultiple(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            adhocExecution: true,
            adhocFilepath: '/this/is/a/path',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            multipleExecutions: true,
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.save())

        Execution e = new Execution(project: "AProject", user: 'bob', dateStarted: new Date(), dateCompleted: null, scheduledExecution: se, workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
        def valid = e.validate()
        e.errors.allErrors.each { println it.toString() }
        assertTrue(valid)
        assertNotNull(e.save())
//        ScheduledExecution.metaClass.static.lock={id-> return se}
//        ScheduledExecution.metaClass.static.withNewSession={clos-> clos.call([clear:{}])}
//        def myCriteria = new Expando();
//        myCriteria.get = {Closure cls -> return [id:123]}
//        Execution.metaClass.static.createCriteria = {myCriteria }

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
        def execution=svc.createExecution(se,"user1")
        assertNotNull(execution)
    }
    void testCreateExecutionSimple(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()


        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e2=svc.createExecution(se,"user1")

        assertNotNull(e2)
        assertEquals('-a b -c d', e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('user1', e2.user)
        def execs = se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e2))
    }
    void testCreateExecutionOverrideNodefilter(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e2=svc.createExecution(se,"user1",[('_replaceNodeFilters'):"true",filter:'name: monkey'])

        assertNotNull(e2)
        assertEquals('name: monkey', e2.filter)
        assertEquals('-a b -c d', e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('user1', e2.user)
        def execs = se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e2))
    }
    void testCreateExecutionOverrideNodefilterOldParams(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e2=svc.createExecution(se,"user1",[('_replaceNodeFilters'):"true",nodeIncludeName: 'monkey'])

        assertNotNull(e2)
        assertEquals('name: monkey', e2.filter)
        assertEquals('-a b -c d', e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('user1', e2.user)
        def execs = se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e2))
    }
    void testCreateExecutionOverrideNodefilterOldParamsMulti(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            doNodedispatch: true,
            filter: ".*",
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e2=svc.createExecution(se,"user1",[('_replaceNodeFilters'):"true",nodeIncludeName: ['monkey','banana']])

        assertNotNull(e2)
        assertEquals('name: monkey,banana', e2.filter)
        assertEquals('-a b -c d', e2.argString)
        assertEquals(se, e2.scheduledExecution)
        assertNotNull(e2.dateStarted)
        assertNull(e2.dateCompleted)
        assertEquals('user1', e2.user)
        def execs = se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e2))
    }
    void testCreateExecutionJobUser(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            user:'bob',
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ScheduledExecution.metaClass.static.lock={id-> return se}
        ScheduledExecution.metaClass.static.withNewSession = {clos -> clos.call([clear: {}])}
        def myCriteria = new Expando();
        myCriteria.get = {Closure cls -> return null}
        Execution.metaClass.static.createCriteria = {myCriteria }
        Execution.metaClass.static.executeQuery = {q, h -> []}


        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e=svc.createExecution(se,null,null)

        assertNotNull(e)
        assertEquals('-a b -c d',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        assertEquals('bob',e.user)
        def execs = se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e))
    }
    void testCreateExecutionAsUser(){

        ScheduledExecution se = new ScheduledExecution(
            jobName: 'blue',
            project: 'AProject',
            user:'bob',//created or scheduled job has user setting
            groupPath: 'some/where',
            description: 'a job',
            argString: '-a b -c d',
            workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()

        ScheduledExecution.metaClass.static.lock={id-> return se}
        ScheduledExecution.metaClass.static.withNewSession = {clos -> clos.call([clear: {}])}
        def myCriteria = new Expando();
        myCriteria.get = {Closure cls -> return null}
        Execution.metaClass.static.createCriteria = {myCriteria }
        Execution.metaClass.static.executeQuery = {q, h -> []}


        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

        Execution e=svc.createExecution(se,"user1")

        assertNotNull(e)
        assertEquals('-a b -c d',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        assertEquals('user1', e.user)
        def execs=se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e))
    }
    void testCreateExecutionOptionsValidation(){
        ScheduledExecution se = prepare()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService=fsvc

            assertNull(se.executions)
            Execution e=svc.createExecution(se,"user1",[argString:'-test1 asdf -test2 val2b -test4 asdf4'])

            assertNotNull(e)
            assertEquals("secure option value should not be stored",'-test1 asdf -test2 val2b -test3 val3',e.argString)
            assertEquals(se, e.scheduledExecution)
            assertNotNull(e.dateStarted)
            assertNull(e.dateCompleted)
            def execs=se.executions
            assertNotNull(execs)
            assertTrue(execs.contains(e))
        }

    void testCreateExecutionOptionsValidation2() {
        ScheduledExecution se = prepare()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
        assertNull(se.executions)
        Execution e=svc.createExecution(se,"user1",[argString:'-test2 val2b -test4 asdf4'])

        assertNotNull(e)
        assertEquals("default value should be used",'-test1 val1 -test2 val2b -test3 val3',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        def execs=se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e))
    }

    void testCreateExecutionOptionsValidation3() {
        ScheduledExecution se = prepare()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
        assertNull(se.executions)
        Execution e=svc.createExecution(se,"user1",[argString:'-test2 val2b -test3 monkey3'])

        assertNotNull(e)
        assertEquals('-test1 val1 -test2 val2b -test3 monkey3',e.argString)
        assertEquals(se, e.scheduledExecution)
        assertNotNull(e.dateStarted)
        assertNull(e.dateCompleted)
        def execs=se.executions
        assertNotNull(execs)
        assertTrue(execs.contains(e))
    }

    void testCreateExecutionOptionsValidation4() {
        ScheduledExecution se = prepare()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
            //enforced value failure on test2
            try {
                Execution e = svc.createExecution(se, "user1", [argString: '-test2 val2D -test3 monkey4'])
                fail("shouldn't succeed")
            } catch (ExecutionServiceException e) {
                assertTrue(e.message.contains("was not in the allowed values"))
            }
        }

    void testCreateExecutionOptionsValidation5() {
        ScheduledExecution se = prepare()

        ExecutionService svc = new ExecutionService()
        FrameworkService fsvc = new FrameworkService()
        svc.frameworkService = fsvc
            //regex failure on test3
            try {
                Execution e = svc.createExecution(se, "user1", [argString: '-test2 val2b -test3 monkey4'])
                fail("shouldn't succeed")
            } catch (ExecutionServiceException e) {
                assertTrue(e.message.contains("doesn't match regular expression"))
            }
        }

    /**
     * Create a job definition with 4 options, test1 through test4. test3-4 are required.
     * @return
     */
    private ScheduledExecution prepare() {
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        assertNotNull(se.save())
        def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, realValuesUrl: "http://test.com/test")
        def opt2 = new Option(name: 'test2', defaultValue: 'val2a', enforced: true, values: ['val2c', 'val2a', 'val2b'])
        def opt3 = new Option(name: 'test3', defaultValue: 'val3', enforced: false, required: true, regex: '^.*3$')
        def opt4 = new Option(name: 'test4', defaultValue: 'val4', enforced: false, required: true, secureInput: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        assertTrue(opt3.validate())
        assertTrue(opt4.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        se.addToOptions(opt3)
        se.addToOptions(opt4)
        se.save()
        se
    }

    void testValidateInputOptionValues(){
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
		def testService = service
        testService.frameworkService=frameworkService

            //test regex and optional value
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            se2.addToOptions(new Option(name:'test1',enforced:false))
            se2.addToOptions(new Option(name:'test2',enforced:false,regex:'.*abc.*'))
            assertNotNull(se2.options)
            assertEquals(2,se2.options.size())

            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\''])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'abc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 abc'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'abcdefg'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 abcdefg'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyzabcdefg'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyzabcdefg'])
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyzab'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyzab'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            se2.addToOptions(new Option(name:'test3',enforced:false,regex:'shampoo[abc].*'))

            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampooa'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampoob'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampooc'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampoocxyz234'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampooa'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampoob'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampooc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampoocxyz234'])

            try{
                testService.validateInputOptionValues(se2,['option.test3':'shampooz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test3 shampooz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test3':'zshampooa'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test3 zshampooa'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
        }

    void testValidateInputOptionValues_enforced() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test enforced values list
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', enforced: true)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())

            assertTrue testService.validateInputOptionValues(se2,['option.test1':'a'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'b'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'abc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 a'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 b'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 abc'])
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\''])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test1':'abd'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 abd'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }

            //test1 is not required, so value can be absent:
            assertTrue testService.validateInputOptionValues(se2,[:])
            assertTrue testService.validateInputOptionValues(se2,[argString:''])

        }


    void testValidateInputOptionValues_required_enforced() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test required & values list
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', enforced: true, required:true)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            //test1 is required, so value cannot be absent:
            try{
                testService.validateInputOptionValues(se2,[:])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test1' is required"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:''])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test1' is required"))
            }
        }

    void testValidateInputOptionValues_enforced_notrequired() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test non-multi-valued and list input
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required:false, enforced: true, multivalued: false)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            //valid single value input
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2,['option.test1':['blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("Option 'test1' value: [blah] does not allow multiple values"))
            }
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("Option 'test1' value: [abc] does not allow multiple values"))
            }
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc','a']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("does not allow multiple values"))
            }
        }

    void testValidateInputOptionValues_multivalued_enforced() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test multi-valued and list input
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required:false, enforced: true, multivalued: true, delimiter: ' ')
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
            try{
                //should fail with invalid value input
                testService.validateInputOptionValues(se2, ['option.test1': ['blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("were not all in the allowed values"))
            }
            try{
                //should fail with invalid value input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc','blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("were not all in the allowed values"))
            }
        }
    void testValidateInputOptionValues_multivalued_enforced_stringinput() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test multi-valued and list input
        assertTrue testService.validateInputOptionValues(se,[:])
        ScheduledExecution se2 = new ScheduledExecution()
        final Option option = new Option(name: 'test1', required:false, enforced: true, multivalued: true, delimiter: ',')
        option.addToValues('abc')
        option.addToValues('def')
        option.addToValues('ghi')
        se2.addToOptions(option)
        assertNotNull(se2.options)
        assertEquals(1,se2.options.size())
        assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
        assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc','def']])
        assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc,def'])
        assertTrue testService.validateInputOptionValues(se2, ['argString': '-test1 abc,def'])
        try{
            //should fail with invalid value input
            testService.validateInputOptionValues(se2, ['option.test1': ['blah']])
            fail("Should have thrown exception")
        }catch (Exception e){
            assertNotNull(e)
            assertTrue(e.message, e.message.contains("were not all in the allowed values"))
        }
        try{
            //should fail with invalid value input
            testService.validateInputOptionValues(se2, ['option.test1': ['abc','blah']])
            fail("Should have thrown exception")
        }catch (Exception e){
            assertNotNull(e)
            assertTrue(e.message, e.message.contains("were not all in the allowed values"))
        }
        try {
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc,gef'])
            fail("Should have thrown exception")
        } catch (Exception e) {
            assertNotNull(e)
            assertTrue(e.message, e.message.contains("were not all in the allowed values"))
        }
        try {
            assertTrue testService.validateInputOptionValues(se2, ['argString': '-test1 abc,zef'])
            fail("Should have thrown exception")
        } catch (Exception e) {
            assertNotNull(e)
            assertTrue(e.message, e.message.contains("were not all in the allowed values"))
        }
        }

    void testValidateInputOptionValues_regex_multivalued() {
        ScheduledExecution se = new ScheduledExecution()
        def frameworkService = new FrameworkService()
        def testService = service
        testService.frameworkService = frameworkService

        //test multi-valued list with regex validation
            assertTrue testService.validateInputOptionValues(se, [:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required: false, enforced: false, multivalued: true, delimiter: ' ',regex:'^[abc]+$')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1, se2.options.size())
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc cba'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc  cba   bca  '])
            assertTrue testService.validateInputOptionValues(se2, ['argString': '-test1 "abcbcbabcbbab acbbcb"'])
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': 'zabc'])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': ['blah']])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': ['abc', 'blah']])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': 'abc cba def'])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['argString': '-test1 "abc def"'])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
    }

    void testParseJobOptsFromString() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        t: {
            //test regex and optional value
            assertTrue testService.validateInputOptionValues(se, [:])
            ScheduledExecution se2 = new ScheduledExecution()
            se2.addToOptions(new Option(name: 'test1', enforced: false, multivalued: true, delimiter: ","))
            final opt2 = new Option(name: 'test2', enforced: true, multivalued: true, delimiter: ' ')
            opt2.addToValues('a')
            opt2.addToValues('b')
            opt2.addToValues('abc')
            se2.addToOptions(opt2)
            assertNotNull(se2.options)
            assertEquals(2, se2.options.size())

            final map = testService.parseJobOptsFromString(se2, "-test1 blah")
            assertNotNull map
            assertNotNull map['test1']
            assertTrue map['test1'] instanceof Collection
            assertEquals 1, map.size()
            assertEquals "wrong value: ${map['test1']}",1, map['test1'].size()
            assertEquals ("Wrong value: ${map.get('test1')}",["blah"], map.get('test1'))

            final map2 = testService.parseJobOptsFromString(se2, "-test1 blah,zah")
            assertNotNull map2
            assertNotNull map2['test1']
            assertTrue map2['test1'] instanceof Collection
            assertEquals 1, map2.size()
            assertEquals 2, map2['test1'].size()
            assertEquals (['blah','zah'], map2.get('test1'))

            final map3 = testService.parseJobOptsFromString(se2, "-test2 'blah zah nah'")
            assertNotNull map3
            assertNotNull map3['test2']
            assertTrue map3['test2'] instanceof Collection
            assertEquals 1, map3.size()
            assertEquals 3, map3['test2'].size()
            assertEquals (['blah','zah','nah'], map3.get('test2'))
        }
    }

    void testGenerateJobArgline() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'test1', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'test2', enforced: false, multivalued: true))
        se2.addToOptions(new Option(name: 'test3', enforced: false, multivalued: false))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-test1 \"some value\"", ExecutionService.generateJobArgline(se2, ['test1': 'some value'])
        //multivalue
        assertEquals "-test1 \"some value+another value\"", ExecutionService.generateJobArgline(se2, ['test1': ['some value','another value']])
        assertEquals "-test2 \"some value,another value\"", ExecutionService.generateJobArgline(se2, ['test2': ['some value','another value']])
        assertEquals "-test3 \"some value,another value\"", ExecutionService.generateJobArgline(se2, ['test3': ['some value','another value']])
    }
    void testGenerateJobArglinePreservesOptionSortIndexOrder() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'abc', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'zyx', enforced: false, multivalued: true,sortIndex: 1))
        se2.addToOptions(new Option(name: 'pst', enforced: false, multivalued: false,sortIndex: 0))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value'])
        assertEquals "-pst blah -zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah'])
        assertEquals "-pst blah -zyx value -abc elf", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah', abc:'elf'])
    }
    void testGenerateJobArglineQuotesBlanks() {
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        //test regex and optional value
        ScheduledExecution se2 = new ScheduledExecution()
        se2.addToOptions(new Option(name: 'abc', enforced: false, multivalued: true,delimiter: "+"))
        se2.addToOptions(new Option(name: 'zyx', enforced: false, multivalued: true,sortIndex: 1))
        se2.addToOptions(new Option(name: 'pst', enforced: false, multivalued: false,sortIndex: 0))
        assertNotNull(se2.options)
        assertEquals(3, se2.options.size())

        assertEquals "-zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value'])
        assertEquals "-pst blah -zyx value", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah'])
        assertEquals "-pst blah -zyx value -abc elf", ExecutionService.generateJobArgline(se2, ['zyx': 'value','pst':'blah', abc:'elf'])
    }

    /**
     * Test createContext method
     */
    void testCreateContext(){

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1){argString ->
            [test:'args']
        }
        fcontrol.demand.filterNodeSet(1..1){fwk,sel,proj->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) {  project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService=fcontrol.createMock()
        service.storageService=mockWith(StorageService){
            storageTreeWithContext{ctx->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()


            Execution se = new Execution(argString:"-test args",user:"testuser",project:"testproj", loglevel:'WARN',doNodedispatch: false)
            def val= service.createContext(se,null,null,null,null,null,null)
            assertNotNull(val)
            assertNull(val.nodeSelector)
            assertEquals("testproj",val.frameworkProject)
            assertEquals("testuser",val.user)
            assertEquals(1,val.loglevel)
            assertNull(val.framework)
            assertNull(val.executionListener)
    }

    /**
     * Test createContext method
     */
    void testCreateContextDatacontext() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, null, null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)
    }

    /**
     * Test createContext method
     */
    void testCreateContextArgsarray() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromArray(1..1) {String[] argString ->
            [test: 'args',test2:'monkey args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext, inputargs instead of argString

            Execution se = new Execution(user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, null,null, ['-test','args','-test2',
                    'monkey args'] as String[])
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            println val.dataContext.option
            assertEquals([test:"args",test2:'monkey args'],val.dataContext.option)
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobData() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check datacontext, include job data

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, [id:"3",name:"testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals([id: "3", name: "testjob"],val.dataContext.job)
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataEmptyNodeset() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, empty

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = service.createContext(se, null,null, null, null, [id:"3",name:"testjob"], null, null)
            assertNotNull(val)
            assertNull(val.nodeSelector)
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataNodeInclude() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, filtered from execution obj. include name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeIncludeName: "testnode")
            def val = service.createContext(se, null, null, null, null, [id: "3", name: "testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertNull(val.nodeSelector.exclude.name)
            assertEquals("testnode", val.nodeSelector.include.name)
    }

    /**
     * Test createContext method
     */
    void testCreateContextJobDataNodeExclude() {

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()
        //check nodeset, filtered from execution obj. exclude name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeExcludeName: "testnode")
            def val = service.createContext(se, null, null, null, null, [id: "3", name: "testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertEquals("testnode", val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.name)
    }

    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextFilters() {
        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args',test3:'something']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        //basic test

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true,nodeIncludeName: "basic")
            def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertNull(val.nodeSelector.exclude.tags)
            assertNull(val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.tags)
            assertEquals("basic", val.nodeSelector.include.name)
    }
    /**
     * Test node keepgoing, threadcount filter values
     */
    void testCreateContextNodeDispatchOptions() {
        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args',test3:'something']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        //basic test

            Execution execution = new Execution(
                    argString: "-test args -test3 something",
                    user: "testuser",
                    project: "testproj",
                    loglevel: 'WARN',
                    doNodedispatch: true,
                    filter:"name: basic",
                    nodeThreadcount: 2,
                    nodeKeepgoing: true,
                    nodeExcludePrecedence: false,
            )
            def val = service.createContext(execution, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertFalse(val.nodeSelector.exclude.dominant)
            assertNotNull(val.nodeSelector.include)
            assertTrue(val.nodeSelector.include.dominant)
            assertNull(val.nodeSelector.exclude.tags)
            assertNull(val.nodeSelector.exclude.name)
            assertNull(val.nodeSelector.include.tags)
            assertEquals("basic", val.nodeSelector.include.name)
            assertEquals(2, val.threadCount)
            assertEquals(true, val.keepgoing)
    }

    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextParameterizedFilters() {
        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args', test3: 'something']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()


        //variable expansion in include name

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN',
                doNodedispatch: true,
                nodeInclude: "a,\${option.test} \${option.test3}",
                nodeIncludeName: "b,\${option.test} \${option.test3}",
                nodeIncludeTags: "c,\${option.test} \${option.test3}",
                nodeIncludeOsArch: "d,\${option.test} \${option.test3}",
                nodeIncludeOsFamily: "e,\${option.test} \${option.test3}",
                nodeIncludeOsName: "f,\${option.test} \${option.test3}",
                nodeIncludeOsVersion: "g,\${option.test} \${option.test3}",
                nodeExclude: "h,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeName: "i,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeTags: "j,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsArch: "k,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsFamily: "l,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsName: "m,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsVersion: "n,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
            )
            def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                    username:'bill',project:'testproj'], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSelector)
            assertNotNull(val.nodeSelector.exclude)
            assertNotNull(val.nodeSelector.include)
            assertEquals("a,args something", val.nodeSelector.include.hostname)
            assertEquals("b,args something", val.nodeSelector.include.name)
            assertEquals("c,args something", val.nodeSelector.include.tags)
            assertEquals("d,args something", val.nodeSelector.include.osarch)
            assertEquals("e,args something", val.nodeSelector.include.osfamily)
            assertEquals("f,args something", val.nodeSelector.include.osname)
            assertEquals("g,args something", val.nodeSelector.include.osversion)
            assertEquals("h,3 blah something/else bill testproj", val.nodeSelector.exclude.hostname)
            assertEquals("i,3 blah something/else bill testproj", val.nodeSelector.exclude.name)
            assertEquals("j,3 blah something/else bill testproj", val.nodeSelector.exclude.tags)
            assertEquals("k,3 blah something/else bill testproj", val.nodeSelector.exclude.osarch)
            assertEquals("l,3 blah something/else bill testproj", val.nodeSelector.exclude.osfamily)
            assertEquals("m,3 blah something/else bill testproj", val.nodeSelector.exclude.osname)
            assertEquals("n,3 blah something/else bill testproj", val.nodeSelector.exclude.osversion)
    }
    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextParameterizedAttributeFilters() {
        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.parseOptsFromString(1..1) {argString ->
            [test: 'args', test3: 'something']
        }
        fcontrol.demand.filterNodeSet(1..1) {fwk, sel, proj ->
            new NodeSetImpl()
        }
        fcontrol.demand.filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
            new NodeSetImpl()
        }
        service.frameworkService = fcontrol.createMock()
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()



        Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN',
            doNodedispatch: true,
            filter: "monkey:a,\${option.test} !environment:b,\${option.test3},d",
        )
        def val = service.createContext(se, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                username:'bill',project:'testproj'], null, null)
        assertNotNull(val)
        assertNotNull(val.nodeSelector)
        assertNotNull(val.nodeSelector.exclude)
        assertNotNull(val.nodeSelector.include)
        assertEquals("a,args", val.nodeSelector.include.toMap().monkey)
        assertEquals("b,something,d", val.nodeSelector.exclude.toMap().environment)
    }
    /**
     * Test use of ${option.x} parameter expansion in node filter string
     */
    void testCreateContextParameterizedWholeFilter() {

        service.frameworkService = mockWith(FrameworkService){
            filterNodeSet(1..1) { fwk, sel, proj ->
                new NodeSetImpl()
            }
            filterAuthorizedNodes(1..1) { project, actions, unfiltered, authContext ->
                new NodeSetImpl()
            }
        }
        service.storageService = mockWith(StorageService) {
            storageTreeWithContext { ctx ->
                null
            }
        }
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        Execution execution = new Execution(argString: "-test 'tags: args'", user: "testuser", project: "testproj", loglevel: 'WARN',
            doNodedispatch: true,
            filter: "\${option.test}",
        )
        def val = service.createContext(execution, null, null, null, null, [id:'3',name:'blah',group:'something/else',
                username:'bill',project:'testproj'], null, null)
        assertNotNull(val)
        assertNotNull(val.nodeSelector)
        assertNotNull(val.nodeSelector.exclude)
        assertNotNull(val.nodeSelector.include)
        assertNull( val.nodeSelector.include.toMap().name)
        assertEquals("args", val.nodeSelector.include.toMap().tags)
        assertNull(val.nodeSelector.exclude.toMap().environment)
    }

    void testItemForWFCmdItem_command(){
        def testService = service
            //exec
            CommandExec ce = new CommandExec(adhocRemoteString: 'exec command')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ExecCommandExecutionItem)
            ExecCommandExecutionItem item=(ExecCommandExecutionItem) res
            assertEquals(['exec','command'],item.command as List)
        }

    void testItemForWFCmdItem_script() {
        def testService = service
            //adhoc local string
            CommandExec ce = new CommandExec(adhocLocalString: 'local script')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ScriptFileCommandExecutionItem)
            ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res
            assertEquals('local script',item.script)
            assertNull(item.scriptAsStream)
            assertNull(item.serverScriptFilePath)
            assertNotNull(item.args)
            assertEquals(0,item.args.length)
        }

    void testItemForWFCmdItem_script_fileextension() {
        def testService = service
        //adhoc local string
        CommandExec ce = new CommandExec(adhocLocalString: 'local script',fileExtension: 'abc')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res
        assertEquals('local script',item.script)
        assertNull(item.scriptAsStream)
        assertNull(item.serverScriptFilePath)
        assertNotNull(item.args)
        assertEquals(0,item.args.length)
        assertEquals('abc',item.fileExtension)
    }

    void testItemForWFCmdItem_scriptArgs() {
        def testService = service
            //adhoc local string, args
            CommandExec ce = new CommandExec(adhocLocalString: 'local script',argString: 'some args')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ScriptFileCommandExecutionItem)
            ScriptFileCommandExecutionItem item=(ScriptFileCommandExecutionItem) res
            assertEquals('local script',item.script)
            assertNull(item.scriptAsStream)
            assertNull(item.serverScriptFilePath)
            assertNotNull(item.args)
            assertEquals(['some', 'args'], item.args as List)
        }

    void testItemForWFCmdItem_scriptfile() {
        def testService = service
        //adhoc file path
        CommandExec ce = new CommandExec(adhocFilepath: '/some/path', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item = (ScriptFileCommandExecutionItem) res
        assertEquals('/some/path', item.serverScriptFilePath)
        assertNull(item.scriptAsStream)
        assertNull(item.script)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }
    void testItemForWFCmdItem_scriptfile_fileextension() {
        def testService = service
        //adhoc file path
        CommandExec ce = new CommandExec(adhocFilepath: '/some/path', argString: 'some args',fileExtension: 'xyz')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem item = (ScriptFileCommandExecutionItem) res
        assertEquals('/some/path', item.serverScriptFilePath)
        assertNull(item.scriptAsStream)
        assertNull(item.script)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
        assertEquals('xyz', item.fileExtension)
    }

    void testItemForWFCmdItem_scripturl() {
        def testService = service
        //http url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'http://example.com/script', argString: 'some args')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res
        assertEquals('http://example.com/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
    }
    void testItemForWFCmdItem_scripturl_fileextension() {
        def testService = service
        //http url script path
        CommandExec ce = new CommandExec(adhocFilepath: 'http://example.com/script', argString: 'some args',fileExtension: 'mdd')
        def res = testService.itemForWFCmdItem(ce)
        assertNotNull(res)
        assertTrue(res instanceof StepExecutionItem)
        assertTrue(res instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res
        assertEquals('http://example.com/script', item.URLString)
        assertNotNull(item.args)
        assertEquals(['some', 'args'], item.args as List)
        assertEquals('mdd', item.fileExtension)
    }

    void testItemForWFCmdItem_scripturl_https() {
        def testService = service
            //https url script path
            CommandExec ce = new CommandExec(adhocFilepath: 'https://example.com/script', argString: 'some args')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ScriptURLCommandExecutionItem)
            ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res
            assertEquals('https://example.com/script', item.URLString)
            assertNotNull(item.args)
            assertEquals(['some', 'args'], item.args as List)
        }

    void testItemForWFCmdItem_scripturl_file() {
        def testService = service
            //file url script path
            CommandExec ce = new CommandExec(adhocFilepath: 'file:/some/script')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ScriptURLCommandExecutionItem)
            ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res
            assertEquals('file:/some/script', item.URLString)
            assertNotNull(item.args)
            assertEquals(0, item.args.length)
        }

    void testItemForWFCmdItem_scripturl_file_args() {
        def testService = service
            //file url script path
            CommandExec ce = new CommandExec(adhocFilepath: 'file:/some/script', argString: 'some args')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ScriptURLCommandExecutionItem)
            ScriptURLCommandExecutionItem item = (ScriptURLCommandExecutionItem) res
            assertEquals('file:/some/script', item.URLString)
            assertNotNull(item.args)
            assertEquals(['some', 'args'], item.args as List)
    }

    private ExecutionService setupCleanupService(){
        def testService = new ExecutionService()
        def mcontrol = mockFor(MetricService, true)
        mcontrol.demand.markMeter(1..1) { classname,argString ->
        }
        testService.metricService = mcontrol.createMock()

        def fcontrol = mockFor(FrameworkService, true)
        fcontrol.demand.getFrameworkNodeName(2..2) {
            'testnode'
        }
        testService.frameworkService = fcontrol.createMock()

        def rcontrol = mockFor(ReportService, true)
        rcontrol.demand.reportExecutionResult(2..2) { map ->
            [success: true]
        }
        testService.reportService = rcontrol.createMock()

        def ncontrol = mockFor(NotificationService, true)
        ncontrol.demand.triggerJobNotification(2..2) { String trigger, schedId, Map content ->
            true
        }
        testService.notificationService = ncontrol.createMock()
        return testService
    }
    void testCleanupRunningJobsNull(){
        def testService = setupCleanupService()
        def wf1=new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        assertNotNull(wf1)
        assertNotNull(wf1.commands)
        assertEquals(1,wf1.commands.size())
        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf1
        )
        assertNotNull(exec1.save())
        def wf2=new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        assertNotNull(wf2)
        assertNotNull(wf2.commands)
        assertEquals(1,wf2.commands.size())
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf2,
                serverNodeUUID: UUID.randomUUID().toString()
        )
        assertNotNull(exec2.save())

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)
        assertEquals(2,Execution.findAll().size())
        assertEquals(1,Execution.findAllByDateCompletedAndServerNodeUUID(null, null).size())
        testService.cleanupRunningJobs(null)
        exec1.refresh()
        assertNotNull(exec1.dateCompleted)
        assertEquals("false", exec1.status)
        exec2.refresh()
        assertNull(exec2.dateCompleted)
        assertEquals(null, exec2.status)

    }

    void testCleanupRunningJobsForClusterNode() {
        def testService = setupCleanupService()
        def uuid = UUID.randomUUID().toString()


        def wf1 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf1
        )
        assertNotNull(exec1.save())

        def wf2 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]).save()
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: wf2,
                serverNodeUUID: uuid
        )
        assertNotNull(exec2.save())

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)

        testService.cleanupRunningJobs(uuid)
        exec1.refresh()
        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)
        exec2.refresh()
        assertNotNull(exec2.dateCompleted)
        assertEquals("false", exec2.status)

    }
    /**
     * null node filter
     */
    void testOverrideJobReferenceNodeFilter_empty() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null, context, null, null, null, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)
    }
    /**
     * null node filter should not override threadcount
     */
    void testOverrideJobReferenceNodeFilter_emptyWithThreadcount() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null, context, null, 2, null, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)
    }
    /**
     * null node filter should not override keepgoing
     */
    void testOverrideJobReferenceNodeFilter_emptyWithKeepgoing() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        def newctx=service.overrideJobReferenceNodeFilter(null, context, null, null, true, null, null)
        assertEquals(['x','y'],newctx.nodes.nodeNames as List)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)
    }
    /**
     * set node filter
     */
    void testOverrideJobReferenceNodeFilter_filter() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1){ final String project, final Set<String> actions, final INodeSet unfiltered,
                                         AuthContext authContext->
                makeNodeSet(['z','p'])
            }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null, context, 'z p', null, null, null, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(false,newctx.keepgoing)
        assertEquals(1,newctx.threadCount)
    }
    /**
     * set node filter and threadcount
     */
    void testOverrideJobReferenceNodeFilter_filterAndThreadcount() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1){ final String project, final Set<String> actions, final INodeSet unfiltered,
                                         AuthContext authContext->
                makeNodeSet(['z','p'])
            }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null, context, 'z p', 2, null, null, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(false,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_filterAndThreadcountAndKeepgoing() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1){ final String project, final Set<String> actions, final INodeSet unfiltered,
                                         AuthContext authContext->
                makeNodeSet(['z','p'])
            }
        }

        def newctx=service.overrideJobReferenceNodeFilter(null, context, 'z p', 2, true, null, null)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_filterAndRankAttribute() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1){ final String project, final Set<String> actions, final INodeSet unfiltered,
                                         AuthContext authContext->
                makeNodeSet(['z','p'])
            }
        }
        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newctx=service.overrideJobReferenceNodeFilter(null, context, 'z p', 2, true, 'rank', false)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
        assertEquals('rank',newctx.nodeRankAttribute)
        assertEquals(false,newctx.nodeRankOrderAscending)
    }
    /**
     * set node filter and threadcount and keepgoing
     */
    void testOverrideJobReferenceNodeFilter_contextVariablesInFilter() {
        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x','y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            filterNodeSet(1..1){ NodesSelector selector, String project->
                assertEquals('z,p,blah',selector.includes.name)
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1){ final String project, final Set<String> actions, final INodeSet unfiltered,
                                         AuthContext authContext->
                makeNodeSet(['z','p'])
            }
        }
        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newctx=service.overrideJobReferenceNodeFilter([option:[test1:'blah']], context, 'z p ${option.test1}', 2, true, 'rank', false)
        assertEquals(['z','p'] as Set,newctx.nodes.nodeNames as Set)
        assertEquals(true,newctx.keepgoing)
        assertEquals(2,newctx.threadCount)
        assertEquals('rank',newctx.nodeRankAttribute)
        assertEquals(false,newctx.nodeRankOrderAscending)
    }

    protected NodesSelector makeSelector(String filter, int threadcount, boolean keepgoing) {
        def nodeset=new NodeSet()
        def filter1 = NodeSet.parseFilter(filter)
        nodeset.createInclude(filter1.include)
        nodeset.createExclude(filter1.exclude)
        nodeset.setThreadCount(threadcount)
        nodeset.setKeepgoing(keepgoing)
        return nodeset
    }
    protected INodeSet makeNodeSet(List<String> nodes) {
        def nset=new NodeSetImpl()
        nodes.each{
            nset.putNode(new NodeEntryImpl(it))
        }
        return nset;
    }

    void testcreateJobReferenceContext_simple(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':[:],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                ['test1':'value']
            }
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }
            filterAuthorizedNodes(1..1) { final String project,
                                          final Set<String> actions,
                                          final INodeSet unfiltered,
                                          AuthContext authContext ->
                makeNodeSet(['x', 'y'])
            }
        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        def newCtxt=service.createJobReferenceContext(job,context,['-test1','value'] as String[],null,null,null,null,null,false);

        //verify nodeset
        assertEquals(['x','y'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(1,newCtxt.threadCount)
        assertEquals(false,newCtxt.keepgoing)
        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1': 'value'], newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': '1',
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ], newCtxt.dataContext['job'])

    }
    void testcreateJobReferenceContext_overrideNodefilter(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':[:],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                ['test1':'value']
            }
            //called by createContext
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }
            filterAuthorizedNodes(1..1) { final String project,
                                          final Set<String> actions,
                                          final INodeSet unfiltered,
                                          AuthContext authContext ->
                makeNodeSet(['x', 'y'])
            }
            //called by overrideJobReferenceNodeFilter
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['z', 'p'])
            }
            filterAuthorizedNodes(1..1) { final String project,
                                          final Set<String> actions,
                                          final INodeSet unfiltered,
                                          AuthContext authContext ->
                makeNodeSet(['z', 'p'])
            }
        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        assertEquals(null, context.nodeRankAttribute)
        assertEquals(true, context.nodeRankOrderAscending)
        def newCtxt=service.createJobReferenceContext(job,context,['-test1','value'] as String[],'z p',true,3, 'rank', false,false);

        //verify nodeset
        assertEquals(['z','p'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(3,newCtxt.threadCount)
        assertEquals(true,newCtxt.keepgoing)
        assertEquals('rank',newCtxt.nodeRankAttribute)
        assertEquals(false,newCtxt.nodeRankOrderAscending)

        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1':'value'],newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': '1',
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ], newCtxt.dataContext['job'])

    }
    void testcreateJobReferenceContext_argDataReferences(){
        ScheduledExecution job = prepare()

        def context = ExecutionContextImpl.builder()
                                          .nodes(makeNodeSet(['x', 'y']))
                                          .nodeSelector(makeSelector("x y", 1, false))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'job':['execid':'123']])
                                          .user('aUser')
                                          .build()
        def parseOptsCount=0
        service.frameworkService=mockWith(FrameworkService){
            parseOptsFromArray(1..2){String[] args->
                def argsl=args as List
                if(parseOptsCount<1){
                    assertEquals(['test1','wakeful'],argsl)
                }else{
                    assertTrue(argsl.indexOf('-test1')>=0 && argsl.indexOf('-test1')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test2')>=0 && argsl.indexOf('-test2')<=argsl.size()-2)
                    assertTrue(argsl.indexOf('-test3')>=0 && argsl.indexOf('-test3')<=argsl.size()-2)
                    assertEquals('wakeful',argsl[argsl.indexOf('-test1')+1])
                    assertEquals('val2a',argsl[argsl.indexOf('-test2')+1])
                    assertEquals('val3',argsl[argsl.indexOf('-test3')+1])
                }
                parseOptsCount++
                ['test1':'wakeful']
            }
            //called by createContext
            filterNodeSet(1..1) { NodesSelector selector, String project ->
                makeNodeSet(['x','y'])
            }
            filterAuthorizedNodes(1..1) { final String project,
                                          final Set<String> actions,
                                          final INodeSet unfiltered,
                                          AuthContext authContext ->
                makeNodeSet(['x', 'y'])
            }

        }
        service.storageService=mockWith(StorageService){
            storageTreeWithContext(1..1){AuthContext->
                null
            }
        }
        def newCtxt=service.createJobReferenceContext(job,context,['test1','${option.monkey}'] as String[],null,null,null, null, null,false);

        //verify nodeset
        assertEquals(['x','y'] as Set,newCtxt.nodes.nodeNames as Set)
        assertEquals(1,newCtxt.threadCount)
        assertEquals(false,newCtxt.keepgoing)

        assertNotNull(newCtxt.dataContext['option'])

        //values from parseOptsFromArray mock
        assertEquals("expected options size incorrect",1,newCtxt.dataContext['option'].size())
        assertEquals(['test1':'wakeful'],newCtxt.dataContext['option'])

        //expected job data context
        assertEquals("expected job data size incorrect", 8, newCtxt.dataContext['job'].size())
        assertEquals(['id': '1',
                      'execid': '123',
                      'project': 'AProject',
                      'username':'aUser',
                      'loglevel': 'ERROR',
                      'user.name': 'aUser',
                      'name':'blue',
                      'group':'some/where'
                     ], newCtxt.dataContext['job'])

    }
}
