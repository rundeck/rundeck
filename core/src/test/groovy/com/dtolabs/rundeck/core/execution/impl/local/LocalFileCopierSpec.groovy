package com.dtolabs.rundeck.core.execution.impl.local

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import spock.lang.Specification

class LocalFileCopierSpec extends Specification{

    def "disable local file copier"() {
        given:
        def node = Mock(INodeEntry)
        def context = Mock(ExecutionContext){
            getExecutionListener()>> Mock(ExecutionListener)
        }
        def framework = Mock(Framework)

        def plugin = new LocalFileCopier(framework)
        plugin.setDisableLocalExecutor(true)

        File scriptFile = File.createTempFile("test","sh");

        when:
        def result = plugin.copyFile(context, scriptFile, node, "/tmp/inlinescript.sh"  )

        then:
        FileCopierException e = thrown()
        e.message == "Local Executor is disabled"

    }


    def "disable local file copier jvm property"() {
        given:
        def node = Mock(INodeEntry)
        def context = Mock(ExecutionContext){
            getExecutionListener()>> Mock(ExecutionListener)
        }
        def framework = Mock(Framework)
        System.setProperty("rundeck.localExecutor.disabled","true")

        def plugin = new LocalFileCopier(framework)

        File scriptFile = File.createTempFile("test","sh");

        when:
        def result = plugin.copyFile(context, scriptFile, node, "/tmp/inlinescript.sh"  )

        then:
        FileCopierException e = thrown()
        e.message == "Local Executor is disabled"

    }
}
