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

    def "update from map plugin type"() {
        when:
            PluginStep t = new PluginStep()
            PluginStep.updateFromMap(t, pluginConfig)
        then:
            t.type == type
            t.configuration == expect
            t.nodeStep == nodeStep
            t.keepgoingOnSuccess == keep
            t.description == desc
            t.pluginConfig == plugins
            t.errorHandler == null
        where:
            type    | pluginConfig                                                                            | expect         | nodeStep | keep  | desc | plugins
            'atype' | [type: 'atype', configuration: [some: 'data'], nodeStep: true]                          | [some: 'data'] | true     | false | null | null
            'atype' | [type: 'atype', configuration: [some: 'data'], nodeStep: true, keepgoingOnSuccess:true] | [some: 'data'] | true     | true  | null | null
            'atype' | [type: 'atype', configuration: [some: 'data'], nodeStep: true, keepgoingOnSuccess:true,description:'desc'] | [some: 'data'] | true     | true  | 'desc' | null
            'atype' | [type: 'atype', configuration: [some: 'data'], nodeStep: true, keepgoingOnSuccess:true,plugins:[some:'data']] | [some: 'data'] | true     | true  | null | [some:'data']
            'atype' | [type: 'atype', configuration: [some: 'data'], nodeStep: true, keepgoingOnSuccess:true,plugins:[some:'data'], errorhandler:[type:'x']] | [some: 'data'] | true     | true  | null | [some:'data']
    }

    def "update from map legacy type"() {
        when:
            PluginStep t = new PluginStep()
            PluginStep.updateFromMap(t, pluginConfig)
        then:
            t.type == type
            t.configuration == expect
            t.nodeStep
            !t.keepgoingOnSuccess
            t.errorHandler == null
        where:
            type                                       | pluginConfig | expect
            ExecCommand.EXEC_COMMAND_TYPE              | [exec:'a command']|[adhocRemoteString: 'a command']
            ExecCommand.EXEC_COMMAND_TYPE              | [exec:'a command', errorhandler:[type:'x']]|[adhocRemoteString: 'a command']
            ScriptCommand.SCRIPT_COMMAND_TYPE          | [script:'a script']|[adhocLocalString: 'a script']
            ScriptCommand.SCRIPT_COMMAND_TYPE          | [script:'a script', errorhandler:[type:'x']]|[adhocLocalString: 'a script']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile:'a file']|[adhocFilepath: 'a file']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile:'a file', errorhandler:[type:'x']]|[adhocFilepath: 'a file']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl:'http://example.com']|[adhocFilepath: 'http://example.com']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl:'http://example.com', errorhandler:[type:'x']]|[adhocFilepath: 'http://example.com']
    }
    def "update from map legacy type script props"() {
        when:
            PluginStep t = new PluginStep()
            PluginStep.updateFromMap(t, pluginConfig+ extraScriptConfig)
        then:
            t.type == type
            t.configuration == expect + expectExtraProps
            t.nodeStep
            !t.keepgoingOnSuccess
            t.errorHandler == null
        where:
            extraScriptConfig = [
                fileExtension        : 'sh',
                args                 : 'asdf',
                scriptInterpreter    : 'bash',
                interpreterArgsQuoted: true
            ]
            expectExtraProps = [
                argString              : 'asdf',
                fileExtension          : 'sh',
                scriptInterpreter      : 'bash',
                interpreterArgsQuoted  : 'true'
            ]
            type                                       | pluginConfig                      | expect
            ScriptCommand.SCRIPT_COMMAND_TYPE          | [script: 'a script']              | [adhocLocalString: 'a script']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file']            | [adhocFilepath: 'a file']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file',expandTokenInScriptFile:true]            | [adhocFilepath: 'a file',expandTokenInScriptFile:'true']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file',expandTokenInScriptFile:'true']            | [adhocFilepath: 'a file',expandTokenInScriptFile:'true']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file',expandTokenInScriptFile:false]            | [adhocFilepath: 'a file',expandTokenInScriptFile:'false']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file',expandTokenInScriptFile:'false']            | [adhocFilepath: 'a file',expandTokenInScriptFile:'false']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com'] | [adhocFilepath: 'http://example.com']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com',expandTokenInScriptFile:true] | [adhocFilepath: 'http://example.com',expandTokenInScriptFile:'true']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com',expandTokenInScriptFile:'true'] | [adhocFilepath: 'http://example.com',expandTokenInScriptFile:'true']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com',expandTokenInScriptFile:false] | [adhocFilepath: 'http://example.com',expandTokenInScriptFile:'false']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com',expandTokenInScriptFile:'false'] | [adhocFilepath: 'http://example.com',expandTokenInScriptFile:'false']
    }

    def "update from map legacy type script props with extra configuration"() {
        when:
            PluginStep t = new PluginStep()
            PluginStep.updateFromMap(t, pluginConfig + extraScriptConfig)
        then:
            t.type == type
            t.configuration == expect + expectExtraProps
            t.nodeStep
            !t.keepgoingOnSuccess
            t.errorHandler == null
        where:
            extraScriptConfig = [
                fileExtension        : 'sh',
                args                 : 'asdf',
                scriptInterpreter    : 'bash',
                interpreterArgsQuoted: true
            ]
            expectExtraProps = [
                argString            : 'asdf',
                fileExtension        : 'sh',
                scriptInterpreter    : 'bash',
                interpreterArgsQuoted: 'true'
            ]
            type | pluginConfig | expect
            ScriptCommand.SCRIPT_COMMAND_TYPE | [script: 'a script', extraprop1: 'a value'] |
            [adhocLocalString: 'a script', extraprop1: 'a value']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scriptfile: 'a file', extraprop2: 'b value'] |
            [adhocFilepath: 'a file',  extraprop2: 'b value']
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE | [scripturl: 'http://example.com', extraprop3: 'c value'] |
            [adhocFilepath: 'http://example.com',  extraprop3: 'c value']
    }

    def "test toMap for legacy steps with extra configuration"() {
        when:
            PluginStep t = new PluginStep(
                type: type,
                configuration: pluginConfig + extraConfig,
                nodeStep: true,
                keepgoingOnSuccess: true,
                pluginConfigData: '{"key1": "value1"}'
            )
            Map configMap = t.toMap()
        then:
            !!configMap.keepgoingOnSuccess
            configMap.errorHandler==null
            configMap.plugins == ['key1': 'value1']
            if (type == ExecCommand.EXEC_COMMAND_TYPE) {
                assert configMap.exec == 'adhocRemoteString'
                assert configMap.argString == null
                assert configMap.other == 'value1'
                assert configMap.otherB == 'value2'
            } else if (type == ScriptCommand.SCRIPT_COMMAND_TYPE) {
                assert configMap.script == 'adhocLocalString'
                assert configMap.args == 'argString'
                assert configMap.other == 'value1'
                assert configMap.otherB == 'value2'
            } else if (type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE) {
                assert configMap.scriptfile == 'adhocFilepath'
                assert configMap.args == 'argString'
                assert configMap.other == 'value1'
                assert configMap.otherB == 'value2'
            } else {
                assert configMap.configuration == pluginConfig + extraConfig
                assert configMap.script == null
                assert configMap.exec == null
                assert configMap.scriptfile == null
            }

        where:
            extraConfig = [other: 'value1', otherB: 'value2']
            type | pluginConfig
            "blah" | ["argString": "argString", "adhocRemoteString": "adhocRemoteString", "adhocLocalString":
                "adhocLocalString", "adhocFilepath": "adhocFilepath", "scriptInterpreter": "bash", "fileExtension":
                "sh", "interpreterArgsQuoted": true, "expandTokenInScriptFile": true]
            ExecCommand
                .EXEC_COMMAND_TYPE | ["argString": "argString", "adhocRemoteString": "adhocRemoteString", "adhocLocalString":
                "adhocLocalString", "adhocFilepath": "", "scriptInterpreter": "bash", "fileExtension": "sh",
                                      "interpreterArgsQuoted": true, "expandTokenInScriptFile": true]
            ScriptCommand
                .SCRIPT_COMMAND_TYPE | ["argString": "argString", "adhocRemoteString": "", "adhocLocalString":
                "adhocLocalString", "adhocFilepath": "", "scriptInterpreter": "bash", "fileExtension": "sh",
                                        "interpreterArgsQuoted": true, "expandTokenInScriptFile": true]
            ScriptFileCommand
                .SCRIPT_FILE_COMMAND_TYPE | ["argString": "argString", "adhocRemoteString": "", "adhocLocalString": "",
                                             "adhocFilepath": "adhocFilepath", "scriptInterpreter": "bash",
                                             "fileExtension": "sh", "interpreterArgsQuoted": true,
                                             "expandTokenInScriptFile": true]
    }

    def "isLegacyBuiltinCommandData"() {
        when:
            def result = PluginStep.isLegacyBuiltinCommandData(data)
        then:
            result == expected
        where:
            data                               | expected
            [exec: 'something']                | true
            [script: 'something']              | true
            [scriptfile: 'something']          | true
            [scripturl: 'something']           | true
            [type: 'something']                | false
            [type: 'something', script: 'xyz'] | false
            [blah: 'something']                | false
    }

    def "getLegacyBuiltinCommandType for valid data"() {
        when:
            def result = PluginStep.getLegacyBuiltinCommandType(data)
        then:
            result == expected
        where:
            data                      | expected
            [exec: 'something']       | ExecCommand.EXEC_COMMAND_TYPE
            [script: 'something']     | ScriptCommand.SCRIPT_COMMAND_TYPE
            [scriptfile: 'something'] | ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
            [scripturl: 'something']  | ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
    }

    def "getLegacyBuiltinCommandType for invalid data"() {
        when:
            def result = PluginStep.getLegacyBuiltinCommandType(data)
        then:
            IllegalArgumentException e = thrown()
        where:
            data                                              | _
            [other: 'data']                                   | _
            [type: 'other', configuration: [other: 'plugin']] | _
    }

    def "createLegacyConfigurationFromDefinitionMap for valid data"() {
        when:
            def result = PluginStep.createLegacyConfigurationFromDefinitionMap(data)
        then:
            result == expected
        where:
            data                      | expected
            [exec: 'something']       | [adhocRemoteString: 'something']
            [script: 'something']     | [adhocLocalString: 'something']
            [script: 'something',args:'asdf']     | [adhocLocalString: 'something',argString: 'asdf']
            [script: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: true,fileExtension: 'sh',args:'asdf']     | [adhocLocalString: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: 'true',fileExtension: 'sh',argString:'asdf']
            [scriptfile: 'something'] | [adhocFilepath: 'something']
            [scriptfile: 'something',args:'asdf'] | [adhocFilepath: 'something',argString:'asdf']
            [scriptfile: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: true,expandTokenInScriptFile: true,fileExtension: 'sh',args:'asdf'] | [adhocFilepath: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: 'true',expandTokenInScriptFile: 'true',fileExtension: 'sh',argString:'asdf']
            [scripturl: 'something']  | [adhocFilepath: 'something']
            [scripturl: 'something',args:'asdf']  | [adhocFilepath: 'something',argString: 'asdf']
            [scripturl: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: true,expandTokenInScriptFile: true,fileExtension: 'sh',args:'asdf']  | [adhocFilepath: 'something',scriptInterpreter: 'bash',interpreterArgsQuoted: 'true',expandTokenInScriptFile: 'true',fileExtension: 'sh',argString:'asdf']
    }
}
