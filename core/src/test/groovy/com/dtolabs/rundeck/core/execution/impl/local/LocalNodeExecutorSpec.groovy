package com.dtolabs.rundeck.core.execution.impl.local

import com.dtolabs.rundeck.core.common.INodeEntry
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
}
