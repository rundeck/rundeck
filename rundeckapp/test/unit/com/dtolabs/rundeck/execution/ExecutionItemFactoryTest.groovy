package com.dtolabs.rundeck.execution

import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem
import com.dtolabs.rundeck.core.execution.HasFailureHandler
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommand
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandExecutionItem
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * ExecutionItemFactoryTest is ...
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-09-25
 */
@RunWith(JUnit4)
class ExecutionItemFactoryTest {
    @Test
    public void creatScriptFileWithScript(){
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "interp",
                "ext",
                true,
                "script string",
                ['args', 'args2'] as String[],
                null,
                true
        )
        Assert.assertTrue(test instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem testcommand=(ScriptFileCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals "script string",testcommand.script
        Assert.assertEquals null,testcommand.serverScriptFilePath
        Assert.assertEquals null,testcommand.scriptAsStream
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void creatScriptFileWithScript_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a','b'] as String[],null,false)
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "interp",
                "ext",
                true,
                "script string",
                ['args', 'args2'] as String[],
                handler,
                true
        )
        Assert.assertTrue(test instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem testcommand=(ScriptFileCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals "script string",testcommand.script
        Assert.assertEquals null,testcommand.serverScriptFilePath
        Assert.assertEquals null,testcommand.scriptAsStream
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        Assert.assertEquals(handler,test.failureHandler)
    }
    @Test
    public void creatScriptFileWithFile(){
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "interp",
                "ext",
                true,
                new File("/path/test/testscript"),
                ['args', 'args2'] as String[],
                null,
                true
        )
        Assert.assertTrue(test instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem testcommand=(ScriptFileCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals null,testcommand.script
        Assert.assertEquals '/path/test/testscript',testcommand.serverScriptFilePath
        Assert.assertEquals null,testcommand.scriptAsStream
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void creatScriptFileWithFile_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "interp",
                "ext",
                true,
                new File("/path/test/testscript"),
                ['args', 'args2'] as String[],
                handler,
                true
        )
        Assert.assertTrue(test instanceof ScriptFileCommandExecutionItem)
        ScriptFileCommandExecutionItem testcommand=(ScriptFileCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals null,testcommand.script
        Assert.assertEquals '/path/test/testscript',testcommand.serverScriptFilePath
        Assert.assertEquals null,testcommand.scriptAsStream
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        Assert.assertEquals(handler, test.failureHandler)
    }
    @Test
    public void creatScriptUrl(){
        StepExecutionItem test = ExecutionItemFactory.createScriptURLItem(
                "interp",
                "ext",
                true,
                "http://example.com/nothing",
                ['args', 'args2'] as String[],
                null,
                true
        )
        Assert.assertTrue(test instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem testcommand=(ScriptURLCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals 'http://example.com/nothing',testcommand.URLString
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void creatScriptUrl_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createScriptURLItem(
                "interp",
                "ext",
                true,
                "http://example.com/nothing",
                ['args', 'args2'] as String[],
                handler,
                true
        )
        Assert.assertTrue(test instanceof ScriptURLCommandExecutionItem)
        ScriptURLCommandExecutionItem testcommand=(ScriptURLCommandExecutionItem) test
        Assert.assertEquals "interp",testcommand.scriptInterpreter
        Assert.assertEquals "ext",testcommand.fileExtension
        Assert.assertEquals true,testcommand.interpreterArgsQuoted
        Assert.assertEquals 'http://example.com/nothing',testcommand.URLString
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        Assert.assertEquals(handler, test.failureHandler)
    }
    @Test
    public void createCommand(){
        StepExecutionItem test = ExecutionItemFactory.createExecCommand(
                ['args', 'args2'] as String[],
                null,
                true
        )
        Assert.assertTrue(test instanceof ExecCommandExecutionItem)
        ExecCommandExecutionItem testcommand=(ExecCommandExecutionItem) test
        Assert.assertEquals( ['args','args2'],testcommand.command as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void createCommand_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createExecCommand(
                ['args', 'args2'] as String[],
                handler,
                true
        )
        Assert.assertTrue(test instanceof ExecCommandExecutionItem)
        ExecCommandExecutionItem testcommand=(ExecCommandExecutionItem) test
        Assert.assertEquals( ['args','args2'],testcommand.command as List)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        Assert.assertEquals(handler, test.failureHandler)
    }
    @Test
    public void createJobRef(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( false,testcommand.nodeStep)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void createJobRef_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                handler,
                true
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( false,testcommand.nodeStep)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        Assert.assertEquals(handler, test.failureHandler)
    }
    @Test
    public void createJobRefNodeStep(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                null,
                true
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.nodeStep)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
    }
    @Test
    public void createJobRefNodeStep_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                handler,
                true
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals( true,testcommand.nodeStep)
        Assert.assertEquals( true,testcommand.keepgoingOnSuccess)
        Assert.assertTrue(test instanceof HasFailureHandler)
        HasFailureHandler handlered2 = (HasFailureHandler) test
        Assert.assertEquals(handler, handlered2.failureHandler)
    }
    @Test
    public void createPluginStep(){
        StepExecutionItem test = ExecutionItemFactory.createPluginStepItem(
                "myplugin",
                [a:'b'],
                true,
                null
        )
        Assert.assertEquals( 'myplugin',test.type)

        Assert.assertTrue(test instanceof ConfiguredStepExecutionItem)
        ConfiguredStepExecutionItem configured = (ConfiguredStepExecutionItem) test
        Assert.assertEquals([a: 'b'], configured.stepConfiguration)

        Assert.assertTrue(test instanceof HandlerExecutionItem)
        HandlerExecutionItem handlered = (HandlerExecutionItem) test
        Assert.assertEquals(true, handlered.keepgoingOnSuccess)

        Assert.assertTrue(test instanceof HasFailureHandler)
        HasFailureHandler handlered2 = (HasFailureHandler) test
        Assert.assertEquals(null, handlered2.failureHandler)
    }
    @Test
    public void createPluginStep_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createPluginStepItem(
                "myplugin",
                [a:'b'],
                true,
                handler
        )
        Assert.assertEquals( 'myplugin',test.type)

        Assert.assertTrue(test instanceof ConfiguredStepExecutionItem)
        ConfiguredStepExecutionItem configured = (ConfiguredStepExecutionItem) test
        Assert.assertEquals([a: 'b'], configured.stepConfiguration)

        Assert.assertTrue(test instanceof HandlerExecutionItem)
        HandlerExecutionItem handlered = (HandlerExecutionItem) test
        Assert.assertEquals(true, handlered.keepgoingOnSuccess)

        Assert.assertTrue(test instanceof HasFailureHandler)
        HasFailureHandler handlered2 = (HasFailureHandler) test
        Assert.assertEquals(handler, handlered2.failureHandler)
    }
    @Test
    public void createPluginNodeStep(){
        StepExecutionItem test = ExecutionItemFactory.createPluginNodeStepItem(
                "myplugin",
                [a:'b'],
                true,
                null
        )
        Assert.assertEquals( 'NodeDispatch',test.type)

        Assert.assertTrue(test instanceof NodeStepExecutionItem)
        NodeStepExecutionItem nodestep = (NodeStepExecutionItem) test
        Assert.assertEquals('myplugin', nodestep.nodeStepType)

        Assert.assertTrue(test instanceof ConfiguredStepExecutionItem)
        ConfiguredStepExecutionItem configured = (ConfiguredStepExecutionItem) test
        Assert.assertEquals([a: 'b'], configured.stepConfiguration)

        Assert.assertTrue(test instanceof HandlerExecutionItem)
        HandlerExecutionItem handlered = (HandlerExecutionItem) test
        Assert.assertEquals(true, handlered.keepgoingOnSuccess)

        Assert.assertTrue(test instanceof HasFailureHandler)
        HasFailureHandler handlered2 = (HasFailureHandler) test
        Assert.assertEquals(null, handlered2.failureHandler)
    }
    @Test
    public void createPluginNodeStep_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createExecCommand(['a', 'b'] as String[], null, false)
        StepExecutionItem test = ExecutionItemFactory.createPluginNodeStepItem(
                "myplugin",
                [a:'b'],
                true,
                handler
        )
        Assert.assertEquals( 'NodeDispatch',test.type)

        Assert.assertTrue(test instanceof NodeStepExecutionItem)
        NodeStepExecutionItem nodestep = (NodeStepExecutionItem) test
        Assert.assertEquals('myplugin', nodestep.nodeStepType)

        Assert.assertTrue(test instanceof ConfiguredStepExecutionItem)
        ConfiguredStepExecutionItem configured = (ConfiguredStepExecutionItem) test
        Assert.assertEquals([a: 'b'], configured.stepConfiguration)

        Assert.assertTrue(test instanceof HandlerExecutionItem)
        HandlerExecutionItem handlered = (HandlerExecutionItem) test
        Assert.assertEquals(true, handlered.keepgoingOnSuccess)

        Assert.assertTrue(test instanceof HasFailureHandler)
        HasFailureHandler handlered2 = (HasFailureHandler) test
        Assert.assertEquals(handler, handlered2.failureHandler)
    }
}
