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

package com.dtolabs.rundeck.execution

import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem
import com.dtolabs.rundeck.core.execution.HasFailureHandler
import com.dtolabs.rundeck.core.execution.ScriptFileCommand
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandExecutionItem

import com.dtolabs.rundeck.core.jobs.JobExecutionItem
import com.dtolabs.rundeck.core.jobs.JobRefCommand
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import rundeck.CommandExec

/**
 * ExecutionItemFactoryTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-09-25
 */
@RunWith(JUnit4)
class ExecutionItemFactoryTest {
    @Test
    public void creatScriptFileWithScript(){
        Map config = [scriptInterpreter: 'interp',
                      fileExtension: 'ext',
                      interpreterArgsQuoted: true,
                      script: 'script string',
                      serverScriptFilePath: null,
                      scriptAsStream: null,
                      args: ['args', 'args2'] as String[],
                      keepgoingOnSuccess: true
        ]
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "a_type",
                config,
                null,
                true,
                null,
                null
        )
        Assert.assertTrue(test instanceof PluginNodeStepExecutionItemImpl)
        PluginNodeStepExecutionItemImpl testcommand=(PluginNodeStepExecutionItemImpl) test
        Assert.assertEquals "interp",testcommand.stepConfiguration.scriptInterpreter
        Assert.assertEquals "ext",testcommand.stepConfiguration.fileExtension
        Assert.assertEquals true,testcommand.stepConfiguration.interpreterArgsQuoted
        Assert.assertEquals "script string",testcommand.stepConfiguration.script
        Assert.assertEquals null,testcommand.stepConfiguration.serverScriptFilePath
        Assert.assertEquals null,testcommand.stepConfiguration.scriptAsStream
        Assert.assertEquals "NodeDispatch",testcommand.getType()
        Assert.assertEquals "a_type",testcommand.getNodeStepType()
        Assert.assertEquals( ['args','args2'],testcommand.stepConfiguration.args as List)
        Assert.assertEquals( true,testcommand.stepConfiguration.keepgoingOnSuccess)
    }
    @Test
    public void creatScriptFileWithScript_withHandler(){
        StepExecutionItem handler = ExecutionItemFactory.createScriptFileItem(
                ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE,
                [script: 'script string'],null,false,null, null)

        Map config = [scriptInterpreter: 'interp',
                      fileExtension: 'ext',
                      interpreterArgsQuoted: true,
                      script: 'script string',
                      serverScriptFilePath: null,
                      scriptAsStream: null,
                      args: ['args', 'args2'] as String[],
                      keepgoingOnSuccess: true
        ]
        StepExecutionItem test = ExecutionItemFactory.createScriptFileItem(
                "a_type",
                config,
                handler,
                true,
                null,
                null
        )
        Assert.assertTrue(test instanceof PluginNodeStepExecutionItemImpl)
        PluginNodeStepExecutionItemImpl testcommand=(PluginNodeStepExecutionItemImpl) test
        Assert.assertEquals "interp",testcommand.stepConfiguration.scriptInterpreter
        Assert.assertEquals "ext",testcommand.stepConfiguration.fileExtension
        Assert.assertEquals true,testcommand.stepConfiguration.interpreterArgsQuoted
        Assert.assertEquals "script string",testcommand.stepConfiguration.script
        Assert.assertEquals null,testcommand.stepConfiguration.serverScriptFilePath
        Assert.assertEquals null,testcommand.stepConfiguration.scriptAsStream
        Assert.assertEquals( ['args','args2'],testcommand.stepConfiguration.args as List)
        Assert.assertEquals( true,testcommand.stepConfiguration.keepgoingOnSuccess)
        Assert.assertEquals( "NodeDispatch",testcommand.getType())
        Assert.assertEquals "a_type",testcommand.getNodeStepType()
        Assert.assertTrue(testcommand instanceof HasFailureHandler)
        Assert.assertEquals(handler, testcommand.failureHandler)
    }
    @Test
    public void createJobRef(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,  null,  null,
                               null,  null,  null, null,false)
    }

    protected void assertJobExecutionItem(JobExecutionItem testcommand,
                                          String identifier,
                                          ArrayList<String> args,
                                          boolean nodeStep,
                                          boolean keepgoingOnSuccess,
                                          String nodeFilter,
                                          Boolean nodeKeepgoing,
                                          Integer nodeThreadcount,
                                          String nodeRankAttribute,
                                          Boolean nodeRankOrderAscending,
                                          Boolean nodeIntersect,
                                          Boolean isFailOnDisable
    )
    {

        Assert.assertEquals(identifier, testcommand.jobIdentifier)
        Assert.assertEquals(args, testcommand.args as List)
        Assert.assertEquals(nodeStep, testcommand.nodeStep)
        Assert.assertEquals(keepgoingOnSuccess, testcommand.keepgoingOnSuccess)
        Assert.assertEquals(nodeFilter, testcommand.nodeFilter)
        Assert.assertEquals(nodeKeepgoing, testcommand.nodeKeepgoing)
        Assert.assertEquals(nodeThreadcount, testcommand.nodeThreadcount)
        Assert.assertEquals(nodeRankAttribute, testcommand.nodeRankAttribute)
        Assert.assertEquals(nodeRankOrderAscending, testcommand.nodeRankOrderAscending)
        Assert.assertEquals(nodeIntersect, testcommand.nodeIntersect)
        Assert.assertEquals(isFailOnDisable, testcommand.failOnDisable)
    }

    @Test
    public void createJobRef_nodeFilter(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                "abc def",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true, 'abc def',
                               null, null, null, null, null,false)
    }
    @Test
    public void createJobRef_nodeThreadCount(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                2,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, null, 2, null, null, null,false)
    }
    @Test
    public void createJobRef_nodeKeepgoing(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, true, null, null, null, null, false)
    }
    @Test
    public void createJobRef_nodeKeepgoingFalse(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, false, null, null, null, null, false)
    }
    @Test
    public void createJobRef_nodeRankAttribute(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                null,
                'rank',
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, null, null, 'rank', null, null,false)
    }
    @Test
    public void createJobRef_nodeRankOrderAscending(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, null, null, null, true, null, false)
    }
    @Test
    public void createJobRef_nodeRankOrderDescending(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobExecutionItem)
        JobExecutionItem testcommand=(JobExecutionItem) test
        assertJobExecutionItem(testcommand, 'monkey/piece', ['args', 'args2'], false, true,
                               null, null, null, null, false, null, false)
    }
    @Test
    public void createJobRef_withHandler(){
        StepExecutionItem handler =  ExecutionItemFactory.createScriptFileItem(
                ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE,
                [script: 'script string'],null,false,null, null)

        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                false,
                handler,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
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
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
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
        StepExecutionItem handler =  ExecutionItemFactory.createScriptFileItem(
                ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE,
                [script: 'script string'],null,false,null, null)

        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                handler,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                false,
                false,
                false
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
    public void createJobRef_from_AnotherProject(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                'anotherProject',
                false,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobRefCommand)
        JobRefCommand testcommand=(JobRefCommand) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals('anotherProject',testcommand.project)
    }
    @Test
    public void createPluginStep(){
        StepExecutionItem test = ExecutionItemFactory.createPluginStepItem(
                "myplugin",
                [a:'b'],
                true,
                null,
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
        StepExecutionItem handler =  ExecutionItemFactory.createScriptFileItem(
                ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE,
                [script: 'script string'],null,false,null, null)

        StepExecutionItem test = ExecutionItemFactory.createPluginStepItem(
                "myplugin",
                [a:'b'],
                true,
                handler,
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
        Assert.assertEquals(handler, handlered2.failureHandler)
    }
    @Test
    public void createPluginNodeStep(){
        StepExecutionItem test = ExecutionItemFactory.createPluginNodeStepItem(
                "myplugin",
                [a:'b'],
                true,
                null,
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
        StepExecutionItem handler =  ExecutionItemFactory.createScriptFileItem(
                ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE,
                [script: 'script string'],null,false,null, null)

        StepExecutionItem test = ExecutionItemFactory.createPluginNodeStepItem(
                "myplugin",
                [a:'b'],
                true,
                handler,
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
        Assert.assertEquals(handler, handlered2.failureHandler)
    }

    @Test
    public void createJobRef_with_disabled_Execution(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobRefCommand)
        JobRefCommand testcommand=(JobRefCommand) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals(true,testcommand.failOnDisable)
    }

    @Test
    public void createJobRef_with_uuid_reference(){
        StepExecutionItem test = ExecutionItemFactory.createJobRef(
                "monkey/piece",
                ['args', 'args2'] as String[],
                true,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
        )
        Assert.assertTrue(test instanceof JobRefCommand)
        JobRefCommand testcommand=(JobRefCommand) test
        Assert.assertEquals( 'monkey/piece',testcommand.jobIdentifier)
        Assert.assertEquals( ['args','args2'],testcommand.args as List)
        Assert.assertEquals('bd80d431-b70a-42ad-8ea8-37ad4885ea0d',testcommand.uuid)
    }
  
  @Test
  public void createJobRef_with_import_options() {
      StepExecutionItem test = ExecutionItemFactory.createJobRef(
              "monkey/piece",
              ['args', 'args2'] as String[],
              true,
              null,
              true,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              false,
              true,
              null,
              false,
              false,
              false
      )
      Assert.assertTrue(test instanceof JobRefCommand)
      JobRefCommand testcommand = (JobRefCommand) test
      Assert.assertEquals('monkey/piece', testcommand.jobIdentifier)
      Assert.assertEquals(['args', 'args2'], testcommand.args as List)
      Assert.assertEquals(true, testcommand.importOptions)
  }
}
