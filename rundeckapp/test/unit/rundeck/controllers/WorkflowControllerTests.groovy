package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.services.FrameworkService

@TestFor(WorkflowController)
@Mock([Workflow, JobExec, CommandExec, PluginStep])
class WorkflowControllerTests {


    public void testWFEditActions() {
        WorkflowController ctrl = new WorkflowController()
        //test insert
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            wf.commands = new ArrayList()


            def result = ctrl._applyWFEditAction(wf, [action: 'insert', num: 0, params: [jobName: 'blah', jobGroup: 'blee']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof JobExec
            assertEquals 'blah', item.jobName
            assertEquals 'blee', item.jobGroup
            //test undo
            assertNotNull result.undo
            assertEquals 'remove', result.undo.action
            assertEquals 0, result.undo.num
        }
        //test remove
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            wf.addToCommands(je)

            assertEquals 1, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'remove', num: 0])
            assertNull result.error
            assertEquals 0, wf.commands.size()
            assertNotNull result.undo
            assertEquals 'insert', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup
        }

        //test modify
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            wf.addToCommands(je)

            assertEquals 1, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'modify', num: 0, params: [jobName: 'xxa', jobGroup: 'xxz']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof JobExec
            assertEquals 'xxa', item.jobName
            assertEquals 'xxz', item.jobGroup
            //test undo
            assertNotNull result.undo
            assertEquals 'modify', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup
        }

        //test move
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            CommandExec ce = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
            CommandExec ce2 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
            wf.addToCommands(je)
            wf.addToCommands(ce)
            wf.addToCommands(ce2)

            assertEquals 3, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'move', from: 0, to: 2])
            assertNull result.error
            assertEquals 3, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertEquals ce, item
            final Object item1 = wf.commands.get(1)
            assertEquals ce2, item1
            final Object item2 = wf.commands.get(2)
            assertEquals je, item2

            //test undo
            assertNotNull result.undo
            assertEquals 'move', result.undo.action
            assertEquals 2, result.undo.from
            assertEquals 0, result.undo.to
        }
    }

    public void testWFEditActionsInsertPluginStep() {
        WorkflowController ctrl = new WorkflowController()
        //test insert
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()

        def result = ctrl._applyWFEditAction(wf, [action: 'insert', num: 0, params: [pluginItem: true, newitemtype: 'test', newitemnodestep: 'false', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)
        assertTrue item instanceof PluginStep
        assertEquals 'test', item.type
        assertEquals false, item.nodeStep
        assertNotNull(item.configuration)
        assertEquals(['blah': 'value'], item.configuration)
        //test undo
        assertNotNull result.undo
        assertEquals 'remove', result.undo.action
        assertEquals 0, result.undo.num
    }

    public void testWFEditActionsRemovePluginStep() {
        WorkflowController ctrl = new WorkflowController()
        //test remove
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        PluginStep je = new PluginStep(type: 'test1', nodeStep: true, configuration: ['elf': 'monkey'])
        wf.addToCommands(je)

        assertEquals 1, wf.commands.size()


        def result = ctrl._applyWFEditAction(wf, [action: 'remove', num: 0])
        assertNull result.error
        assertEquals 0, wf.commands.size()
        assertNotNull result.undo
        assertEquals 'insert', result.undo.action
        assertEquals 0, result.undo.num
        assertNotNull result.undo.params
        assertEquals 'test1', result.undo.params.type
        assertEquals true, result.undo.params.nodeStep
        assertEquals(['elf': 'monkey'], result.undo.params.configuration)
    }

    public void testWFEditActionsModifyPluginStep() {
        WorkflowController ctrl = new WorkflowController()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getNodeStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()

        //test modify
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        PluginStep je = new PluginStep(type: 'test1', nodeStep: true, configuration: ['elf': 'monkey'])
        wf.addToCommands(je)

        assertEquals 1, wf.commands.size()


        def result = ctrl._applyWFEditAction(wf, [action: 'modify', num: 0, params: [pluginItem: true, newitemtype: 'test', newitemnodestep: 'false', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)
        assertTrue item instanceof PluginStep
        //type not modified
        assertEquals 'test1', item.type
        //nodeStep not modified
        assertEquals true, item.nodeStep

        assertEquals(['blah': 'value'], item.configuration)

        //test undo
        assertNotNull result.undo
        assertEquals 'modify', result.undo.action
        assertEquals 0, result.undo.num
        assertNotNull result.undo.params
        assertEquals(['elf': 'monkey'], result.undo.params.configuration)
    }

    public void testWFErrorHandlerEditActions() {
        WorkflowController ctrl = new WorkflowController()
        //test addHandler
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands: [new JobExec(jobName: 'asdf', jobGroup: 'blee')])

            def result = ctrl._applyWFEditAction(wf, [action: 'addHandler', num: 0, params: [jobName: 'blah', jobGroup: 'blee']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertNotNull item.errorHandler

            assertTrue item.errorHandler instanceof JobExec

            assertEquals 'blah', item.errorHandler.jobName
            assertEquals 'blee', item.errorHandler.jobGroup

            //test undo memo
            assertNotNull result.undo
            assertEquals 'removeHandler', result.undo.action
            assertEquals 0, result.undo.num
        }
        //test removeHandler
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                    [new CommandExec(adhocRemoteString: 'test', errorHandler: new JobExec(jobName: 'blah', jobGroup: 'blee'))])
            assertNotNull(wf.commands[0].errorHandler)

            def result = ctrl._applyWFEditAction(wf, [action: 'removeHandler', num: 0])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof CommandExec
            final CommandExec stepitem = (CommandExec) item
            assertNull item.errorHandler

            //test undo memo
            assertNotNull result.undo
            assertEquals 'addHandler', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup
        }
        //test modifyHandler
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                    [new CommandExec(adhocRemoteString: 'test', errorHandler: new JobExec(jobName: 'blah', jobGroup: 'blee'))])
            assertNotNull(wf.commands[0].errorHandler)

            def result = ctrl._applyWFEditAction(wf, [action: 'modifyHandler', num: 0, params: [jobName: 'blah2', jobGroup: 'blee2']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof CommandExec
            final CommandExec stepitem = (CommandExec) item
            assertNotNull item.errorHandler

            assertTrue item.errorHandler instanceof JobExec

            assertEquals 'blah2', item.errorHandler.jobName
            assertEquals 'blee2', item.errorHandler.jobGroup

            //test undo memo
            assertNotNull result.undo
            assertEquals 'modifyHandler', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup
        }

    }

    public void testWFErrorHandlerActionsInsertPluginStep() {
        WorkflowController ctrl = new WorkflowController()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getNodeStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()
        //test insert
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands: [new CommandExec(adhocRemoteString: 'test')])


        def result = ctrl._applyWFEditAction(wf, [action: 'addHandler', num: 0, params: [pluginItem: true, newitemtype: 'test', newitemnodestep: 'true', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)

        assertTrue item instanceof CommandExec
        final CommandExec stepitem = (CommandExec) item
        assertNotNull item.errorHandler

        assertTrue item.errorHandler instanceof PluginStep
        assertEquals 'test', item.errorHandler.type
        assertEquals true, item.errorHandler.nodeStep
        assertEquals(['blah': 'value'], item.errorHandler.configuration)
        assertTrue(!item.errorHandler.keepgoingOnSuccess)

        //test undo memo
        assertNotNull result.undo
        assertEquals 'removeHandler', result.undo.action
        assertEquals 0, result.undo.num
    }

    public void testWFErrorHandlerActionsInsertPluginStepKeepgoingOnSuccess() {
        WorkflowController ctrl = new WorkflowController()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getNodeStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()
        //test insert
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands: [new CommandExec(adhocRemoteString: 'test')])


        def result = ctrl._applyWFEditAction(wf, [action: 'addHandler', num: 0, params: [
                keepgoingOnSuccess: 'true',
                pluginItem: true, newitemtype: 'test', newitemnodestep: 'true', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)

        assertTrue item instanceof CommandExec
        final CommandExec stepitem = (CommandExec) item
        assertNotNull item.errorHandler

        assertTrue item.errorHandler instanceof PluginStep
        assertEquals 'test', item.errorHandler.type
        assertEquals true, item.errorHandler.nodeStep
        assertEquals(['blah': 'value'], item.errorHandler.configuration)
        assertTrue(item.errorHandler.keepgoingOnSuccess)

        //test undo memo
        assertNotNull result.undo
        assertEquals 'removeHandler', result.undo.action
        assertEquals 0, result.undo.num
    }

    public void testWFErrorHandlerActionsRemovePluginStep() {
        WorkflowController ctrl = new WorkflowController()
        //test remove
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                [new CommandExec(adhocRemoteString: 'test', errorHandler: new PluginStep(type: 'test1', nodeStep: true, configuration: ['elf': 'monkey']))])
        assertNotNull(wf.commands[0].errorHandler)

        def result = ctrl._applyWFEditAction(wf, [action: 'removeHandler', num: 0])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)
        assertTrue item instanceof CommandExec
        final CommandExec stepitem = (CommandExec) item
        assertNull item.errorHandler

        //test undo memo
        assertNotNull result.undo
        assertEquals 'addHandler', result.undo.action
        assertEquals 0, result.undo.num
        assertNotNull result.undo.params
        assertEquals 'test1', result.undo.params.type
        assertEquals true, result.undo.params.nodeStep
        assertEquals(['elf': 'monkey'], result.undo.params.configuration)
        assertEquals 1, wf.commands.size()
    }

    public void testWFErrorHandlerActionsModifyPluginStep() {
        WorkflowController ctrl = new WorkflowController()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getNodeStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()

        //test modify
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                [new CommandExec(adhocRemoteString: 'test', errorHandler: new PluginStep(type: 'test1', nodeStep: true, configuration: ['elf': 'monkey']))])
        assertNotNull(wf.commands[0].errorHandler)

        def result = ctrl._applyWFEditAction(wf, [action: 'modifyHandler', num: 0, params: [pluginItem: true, newitemtype: 'test', newitemnodestep: 'false', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)
        assertTrue item instanceof CommandExec
        final CommandExec stepitem = (CommandExec) item
        assertNotNull item.errorHandler

        assertTrue item.errorHandler instanceof PluginStep
        //type and nodeStep do not get modified
        assertEquals 'test1', item.errorHandler.type
        assertEquals true, item.errorHandler.nodeStep
        assertEquals true, !item.errorHandler.keepgoingOnSuccess

        assertEquals(['blah': 'value'], item.errorHandler.configuration)

        //test undo
        assertNotNull result.undo
        assertEquals 'modifyHandler', result.undo.action
        assertEquals 0, result.undo.num
        assertNotNull result.undo.params
        assertEquals(['elf': 'monkey'], result.undo.params.configuration)
    }

    public void testWFErrorHandlerActionsModifyPluginStepKeepgoingOnSuccess() {
        WorkflowController ctrl = new WorkflowController()

        def fwmock = mockFor(FrameworkService)
        fwmock.demand.getFrameworkFromUserSession { s, r -> null }
        fwmock.demand.getNodeStepPluginDescription { fwk, type -> null }
        fwmock.demand.validateDescription { desc, p, params -> [valid: true, props: ['blah': 'value']] }
        ctrl.frameworkService = fwmock.createMock()

        //test modify
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                [new CommandExec(adhocRemoteString: 'test', errorHandler: new PluginStep(type: 'test1', nodeStep: true,
                        configuration: ['elf': 'monkey']))])
        assertNotNull(wf.commands[0].errorHandler)

        def result = ctrl._applyWFEditAction(wf, [action: 'modifyHandler', num: 0, params: [
                keepgoingOnSuccess: 'true',
                pluginItem: true, newitemtype: 'test', newitemnodestep: 'false', pluginConfig: ['blah': 'value']]])
        assertNull result.error
        assertEquals 1, wf.commands.size()
        final Object item = wf.commands.get(0)
        assertTrue item instanceof CommandExec
        final CommandExec stepitem = (CommandExec) item
        assertNotNull item.errorHandler

        assertTrue item.errorHandler instanceof PluginStep
        //type and nodeStep do not get modified
        assertEquals 'test1', item.errorHandler.type
        assertEquals true, item.errorHandler.nodeStep
        assertEquals true, item.errorHandler.keepgoingOnSuccess

        assertEquals(['blah': 'value'], item.errorHandler.configuration)

        //test undo
        assertNotNull result.undo
        assertEquals 'modifyHandler', result.undo.action
        assertEquals 0, result.undo.num
        assertNotNull result.undo.params
        assertEquals(['elf': 'monkey'], result.undo.params.configuration)
    }

    public void testUndoWFEditActions() {
        WorkflowController ctrl = new WorkflowController()
        //test insert & then undo
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            wf.commands = new ArrayList()


            def result = ctrl._applyWFEditAction(wf, [action: 'insert', num: 0, params: [jobName: 'blah', jobGroup: 'blee']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof JobExec
            assertEquals 'blah', item.jobName
            assertEquals 'blee', item.jobGroup
            //test undo
            assertNotNull result.undo
            assertEquals 'remove', result.undo.action
            assertEquals 0, result.undo.num

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 0, wf.commands.size()
        }
        //test remove & undo
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            wf.addToCommands(je)

            assertEquals 1, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'remove', num: 0])
            assertNull result.error
            assertEquals 0, wf.commands.size()
            assertNotNull result.undo
            assertEquals 'insert', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof JobExec
            assertEquals 'blah', item.jobName
            assertEquals 'blee', item.jobGroup

        }

        //test modify & undo
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            wf.addToCommands(je)

            assertEquals 1, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'modify', num: 0, params: [jobName: 'xxa', jobGroup: 'xxz']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof JobExec
            assertEquals 'xxa', item.jobName
            assertEquals 'xxz', item.jobGroup
            //test undo
            assertNotNull result.undo
            assertEquals 'modify', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()

            final Object item2 = wf.commands.get(0)
            assertTrue item2 instanceof JobExec
            assertEquals 'blah', item2.jobName
            assertEquals 'blee', item2.jobGroup


        }

        //test move
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
            JobExec je = new JobExec(jobName: 'blah', jobGroup: 'blee')
            CommandExec ce = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
            CommandExec ce2 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
            wf.addToCommands(je)
            wf.addToCommands(ce)
            wf.addToCommands(ce2)

            assertEquals 3, wf.commands.size()


            def result = ctrl._applyWFEditAction(wf, [action: 'move', from: 0, to: 2])
            assertNull result.error
            assertEquals 3, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertEquals ce, item
            final Object item1 = wf.commands.get(1)
            assertEquals ce2, item1
            final Object item2 = wf.commands.get(2)
            assertEquals je, item2

            //test undo
            assertNotNull result.undo
            assertEquals 'move', result.undo.action
            assertEquals 2, result.undo.from
            assertEquals 0, result.undo.to

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 3, wf.commands.size()

            final Object xitem = wf.commands.get(0)
            assertEquals je, xitem
            final Object xitem1 = wf.commands.get(1)
            assertEquals ce, xitem1
            final Object xitem2 = wf.commands.get(2)
            assertEquals ce2, xitem2
        }
    }

    public void testUndoWFErrorHandlerEditActionsAddHandler() {
        WorkflowController ctrl = new WorkflowController()
        //test addHandler then undo
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands: [new JobExec(jobName: 'bladf', jobGroup: 'elf')])

            def result = ctrl._applyWFEditAction(wf, [action: 'addHandler', num: 0, params: [jobName: 'blah', jobGroup: 'blee']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertNotNull item.errorHandler

            assertTrue item.errorHandler instanceof JobExec

            assertEquals 'blah', item.errorHandler.jobName
            assertEquals 'blee', item.errorHandler.jobGroup
            assertTrue "should be false", !item.errorHandler.keepgoingOnSuccess

            //test undo memo
            assertNotNull result.undo
            assertEquals 'removeHandler', result.undo.action
            assertEquals 0, result.undo.num

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()
            assertNull wf.commands.get(0).errorHandler
        }
    }

    public void testUndoWFErrorHandlerEditActionsAddHandlerKeepgoingTrue() {
        WorkflowController ctrl = new WorkflowController()
        //test addHandler then undo
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands: [new JobExec(jobName: 'bladf', jobGroup: 'elf')])

            def result = ctrl._applyWFEditAction(wf, [action: 'addHandler', num: 0, params: [jobName: 'blah', jobGroup: 'blee', keepgoingOnSuccess: 'true']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertNotNull item.errorHandler

            assertTrue item.errorHandler instanceof JobExec

            assertEquals 'blah', item.errorHandler.jobName
            assertEquals 'blee', item.errorHandler.jobGroup
            assertTrue item.errorHandler.keepgoingOnSuccess

            //test undo memo
            assertNotNull result.undo
            assertEquals 'removeHandler', result.undo.action
            assertEquals 0, result.undo.num

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()
            assertNull wf.commands.get(0).errorHandler
        }
    }

    public void testUndoWFErrorHandlerEditActionsRemoveHandler() {

        WorkflowController ctrl = new WorkflowController()
        //test removeHandler
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                    [new JobExec(jobName: 'monkey', jobGroup: 'elf', errorHandler: new JobExec(jobName: 'blah', jobGroup: 'blee'))])
            assertNotNull(wf.commands[0].errorHandler)

            def result = ctrl._applyWFEditAction(wf, [action: 'removeHandler', num: 0])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertNull item.errorHandler

            //test undo memo
            assertNotNull result.undo
            assertEquals 'addHandler', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()
            assertNotNull wf.commands.get(0).errorHandler

            final Object item2 = wf.commands.get(0)
            assertNotNull item2.errorHandler

            assertTrue item2.errorHandler instanceof JobExec

            assertEquals 'blah', item2.errorHandler.jobName
            assertEquals 'blee', item2.errorHandler.jobGroup
        }

    }

    public void testUndoWFErrorHandlerEditActionsModifyHandler() {

        WorkflowController ctrl = new WorkflowController()
        //test modifyHandler
        test: {
            Workflow wf = new Workflow(threadcount: 1, keepgoing: true, commands:
                    [new CommandExec(adhocRemoteString: 'test', errorHandler: new JobExec(jobName: 'blah', jobGroup: 'blee'))])
            assertNotNull(wf.commands[0].errorHandler)

            def result = ctrl._applyWFEditAction(wf, [action: 'modifyHandler', num: 0, params: [jobName: 'blah2', jobGroup: 'blee2']])
            assertNull result.error
            assertEquals 1, wf.commands.size()
            final Object item = wf.commands.get(0)
            assertTrue item instanceof CommandExec
            final CommandExec stepitem = (CommandExec) item
            assertNotNull item.errorHandler

            assertTrue item.errorHandler instanceof JobExec

            assertEquals 'blah2', item.errorHandler.jobName
            assertEquals 'blee2', item.errorHandler.jobGroup

            //test undo memo
            assertNotNull result.undo
            assertEquals 'modifyHandler', result.undo.action
            assertEquals 0, result.undo.num
            assertNotNull result.undo.params
            assertEquals 'blah', result.undo.params.jobName
            assertEquals 'blee', result.undo.params.jobGroup

            //apply undo
            def result2 = ctrl._applyWFEditAction(wf, result.undo)
            assertNull result2.error
            assertEquals 1, wf.commands.size()
            assertNotNull wf.commands.get(0).errorHandler

            final Object item2 = wf.commands.get(0)
            assertNotNull item2.errorHandler

            assertTrue item2.errorHandler instanceof JobExec

            assertEquals 'blah', item2.errorHandler.jobName
            assertEquals 'blee', item2.errorHandler.jobGroup
        }
    }

    public void testUndoWFSession() {
        //test session undo storage

        WorkflowController ctrl = new WorkflowController()
        ctrl._pushUndoAction('test1', [testx: 'test1x'])
        assertNotNull ctrl.session
        assertNotNull ctrl.session.undoWF
        assertNotNull ctrl.session.undoWF['test1']
        assertEquals 1, ctrl.session.undoWF['test1'].size()
        assertEquals 'test1x', ctrl.session.undoWF['test1'].get(0).testx

        ctrl._pushUndoAction('test1', [testz: 'test2z'])
        assertEquals 2, ctrl.session.undoWF['test1'].size()
        assertEquals 'test1x', ctrl.session.undoWF['test1'].get(0).testx
        assertEquals 'test2z', ctrl.session.undoWF['test1'].get(1).testz

        ctrl._pushUndoAction('test1', [testy: 'test3p'])
        assertEquals 3, ctrl.session.undoWF['test1'].size()
        assertEquals 'test1x', ctrl.session.undoWF['test1'].get(0).testx
        assertEquals 'test2z', ctrl.session.undoWF['test1'].get(1).testz
        assertEquals 'test3p', ctrl.session.undoWF['test1'].get(2).testy

        def pop1 = ctrl._popUndoAction('test1')
        assertNotNull pop1
        assertEquals 2, ctrl.session.undoWF['test1'].size()
        assertEquals 'test3p', pop1.testy

        def pop2 = ctrl._popUndoAction('test1')
        assertNotNull pop2
        assertEquals 1, ctrl.session.undoWF['test1'].size()
        assertEquals 'test2z', pop2.testz

        def pop3 = ctrl._popUndoAction('test1')
        assertNotNull pop3
        assertEquals 0, ctrl.session.undoWF['test1'].size()
        assertEquals 'test1x', pop3.testx

        def pop4 = ctrl._popUndoAction('test1')
        assertNull(pop4)

    }
}
