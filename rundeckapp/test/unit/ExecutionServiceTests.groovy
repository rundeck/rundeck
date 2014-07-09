import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandExecutionItem
import grails.test.GrailsUnitTestCase
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.mock.interceptor.MockFor
import org.grails.plugins.metricsweb.MetricService
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
class ExecutionServiceTests  {
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
        Execution e = new Execution(project:"test",user:'bob',dateStarted: new Date(),dateCompleted: null,scheduledExecution: se,workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
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

        Execution e = new Execution(project: "test", user: 'bob', dateStarted: new Date(), dateCompleted: null, scheduledExecution: se, workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]))
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
		def testService = new ExecutionService()
		def frameworkService = new FrameworkService()
        testService.frameworkService=frameworkService

        t:{
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

        t:{
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


        t:{
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

        t:{
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
        t:{
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
        t: {
            //test multi-valued list with regex validation
            assertTrue testService.validateInputOptionValues(se, [:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required: false, enforced: false, multivalued: true, delimiter: ' ',regex:'^[abc]+$')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1, se2.options.size())
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
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

    void testItemForWFCmdItem(){
        def testService = new ExecutionService()

        t:{
            //exec
            CommandExec ce = new CommandExec(adhocRemoteString: 'exec command')
            def res = testService.itemForWFCmdItem(ce)
            assertNotNull(res)
            assertTrue(res instanceof StepExecutionItem)
            assertTrue(res instanceof ExecCommandExecutionItem)
            ExecCommandExecutionItem item=(ExecCommandExecutionItem) res
            assertEquals(['exec','command'],item.command as List)
        }
        t:{
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
        t:{
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
        t: {
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
        t: {
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
        t: {
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
        t: {
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
        t: {
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

        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test")])
        )
        assertNotNull(exec1.save())
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]),
                serverNodeUUID: UUID.randomUUID().toString()
        )
        assertNotNull(exec2.save())

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)

        testService.cleanupRunningJobs(null)

        assertNotNull(exec1.dateCompleted)
        assertEquals("false", exec1.status)

        assertNull(exec2.dateCompleted)
        assertEquals(null, exec2.status)

    }

    void testCleanupRunningJobsForClusterNode() {
        def testService = setupCleanupService()
        def uuid = UUID.randomUUID().toString()

        Execution exec1 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test")])
        )
        assertNotNull(exec1.save())
        Execution exec2 = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false,
                dateStarted: new Date(),
                dateCompleted: null,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test")]),
                serverNodeUUID: uuid
        )
        assertNotNull(exec2.save())

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNull(exec2.dateCompleted)
        assertNull(exec2.status)

        testService.cleanupRunningJobs(uuid)

        assertNull(exec1.dateCompleted)
        assertNull(exec1.status)

        assertNotNull(exec2.dateCompleted)
        assertEquals("false", exec2.status)

    }
}
