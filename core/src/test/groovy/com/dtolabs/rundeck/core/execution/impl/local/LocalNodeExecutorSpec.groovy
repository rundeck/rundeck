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

package com.dtolabs.rundeck.core.execution.impl.local

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGeneratorImpl
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.ExecTask
import org.apache.tools.ant.types.RedirectorElement
import spock.lang.Specification

/**
 * Created by greg on 7/7/16.
 */
class LocalNodeExecutorSpec extends Specification {
    class TExecTask extends ExecTask {
        RedirectorElement getTestRedirectorElement() {
            return this.redirectorElement
        }
    }

    def "build exec task uses input encoding"() {
        given:
        def task = new TExecTask()
        def project = new Project()
        def node = Mock(INodeEntry)
        def params = new ExecTaskParameterGeneratorImpl().generate(
                node, true, null, ['a', 'command'] as String[]
        )
        when:
        def result = LocalNodeExecutor.buildExecTask(project, params, [:], encoding, task)

        then:
        result == task
        if (encoding) {
            task.testRedirectorElement != null
            task.testRedirectorElement.inputEncoding == encoding
        } else {
            task.testRedirectorElement == null
        }

        where:

        encoding     | _
        null         | _
        'UTF-8'      | _
        'ISO-8859-2' | _
    }

    def "disable local executor"() {
        given:
        def node = Mock(INodeEntry)
        def context = Mock(ExecutionContext){
            getExecutionListener()>> Mock(ExecutionListener)
        }
        def command = ['a', 'command'] as String[]
        def framework = Mock(Framework)

        def plugin = new LocalNodeExecutor(framework)
        plugin.setDisableLocalExecutor(true)

        when:
        def result = plugin.executeCommand(context, command, node)

        then:
        result != null
        !result.success
        result.failureReason != null
        result.failureMessage == "Local Executor is disabled"

    }

    def "disable local executor jvm propertu"() {
        given:
        def node = Mock(INodeEntry)
        def context = Mock(ExecutionContext){
            getExecutionListener()>> Mock(ExecutionListener)
        }
        def command = ['a', 'command'] as String[]
        def framework = Mock(Framework)

        System.setProperty("rundeck.localExecutor.disabled","true")

        def plugin = new LocalNodeExecutor(framework)

        when:
        def result = plugin.executeCommand(context, command, node)

        then:
        result != null
        !result.success
        result.failureReason != null
        result.failureMessage == "Local Executor is disabled"

    }
}
