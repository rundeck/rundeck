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

package rundeck

import grails.testing.gorm.DataTest
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class PluginStepTests  extends Specification implements DataTest{
    @Override
    Class[] getDomainClassesToMock() {
        [PluginStep]
    }

    def testClone() {
        when:
        PluginStep t = new PluginStep(type: 'blah',configuration: [elf:'hello'],nodeStep: true, keepgoingOnSuccess: true)
        PluginStep j1 = t.createClone()
        then:
        assertEquals('blah', j1.type)
        assertEquals([elf:'hello'], j1.configuration)
        assertEquals(true, j1.nodeStep)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }

    def testToMap() {
        when:
        PluginStep t = new PluginStep(type: 'blah',configuration: [elf:'hello'],nodeStep: true, keepgoingOnSuccess: true)
        then:
        Map configMap = t.toMap()
        assertEquals([elf:'hello'], configMap.configuration)
        assertEquals(true, configMap.nodeStep)
        assertEquals(true, !!configMap.keepgoingOnSuccess)
        assertNull(t.toMap().errorHandler)
    }

    def testToMapKeepCompatibilityWithOldBuiltInTypes(){
        when:
        PluginStep t = new PluginStep(type: type,configuration: pluginConfig,nodeStep: true, keepgoingOnSuccess: true, pluginConfigData: '{"key1": "value1"}')
        then:
        Map configMap = t.toMap()
        assertEquals(true, !!configMap.keepgoingOnSuccess)
        assertNull(configMap.errorHandler)
        if(type == ExecCommand.EXEC_COMMAND_TYPE) {
            assertEquals('adhocRemoteString', configMap.exec)
            assertEquals(['key1': 'value1'], configMap.plugins)
        }else if(type == ScriptCommand.SCRIPT_COMMAND_TYPE) {
            assertEquals('adhocLocalString', configMap.script)
            assertEquals(['key1': 'value1'], configMap.plugins)
        }else if(type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE) {
            assertEquals('adhocFilepath', configMap.scriptfile)
            assertEquals(['key1': 'value1'], configMap.plugins)
        }else{
            assertEquals(pluginConfig, configMap.configuration)
            assertEquals(null, configMap.script)
            assertEquals(null, configMap.exec)
            assertEquals(null, configMap.scriptfile)
            assertEquals(['key1': 'value1'], configMap.plugins)
        }

        where:
        type                                       | pluginConfig
        "blah"                                     | ["argString": "", "adhocRemoteString": "adhocRemoteString","adhocLocalString": "adhocLocalString","adhocFilepath": "adhocFilepath","scriptInterpreter": "bash","fileExtension": "sh", "interpreterArgsQuoted": true,"expandTokenInScriptFile": true]
        ExecCommand.EXEC_COMMAND_TYPE              | ["argString": "", "adhocRemoteString": "adhocRemoteString","adhocLocalString": "adhocLocalString","adhocFilepath": "","scriptInterpreter": "bash","fileExtension": "sh", "interpreterArgsQuoted": true,"expandTokenInScriptFile": true]
        ScriptCommand.SCRIPT_COMMAND_TYPE          | ["argString": "", "adhocRemoteString": "","adhocLocalString": "adhocLocalString","adhocFilepath": "","scriptInterpreter": "bash","fileExtension": "sh", "interpreterArgsQuoted": true,"expandTokenInScriptFile": true]
        ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | ["argString": "", "adhocRemoteString": "","adhocLocalString": "","adhocFilepath": "adhocFilepath","scriptInterpreter": "bash","fileExtension": "sh", "interpreterArgsQuoted": true,"expandTokenInScriptFile": true]
    }
}
