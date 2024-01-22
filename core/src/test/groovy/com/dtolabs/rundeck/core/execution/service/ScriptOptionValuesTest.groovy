/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import spock.lang.Specification


class ScriptOptionValuesTest extends Specification {
    def isPosixSystem() {
        def osName = System.getProperty("os.name").toLowerCase()
        return !(osName.contains("win") || osName.contains("mac") || osName.contains("sunos"))
    }

    def "GetOptionValues"() {
        when:

        TestScriptOptionValuesProvider testProvider = new TestScriptOptionValuesProvider()
        testProvider.archiveFile = File.createTempFile("option","archive")
        testProvider.contentsBaseDir = File.createTempDir()
        if (isPosixSystem()) {
            testProvider.scriptFile = File.createTempFile("option","values")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """echo "==START_OPTIONS=="
                                    echo "First Option:opt1"
                                    echo "Second Option:opt2"
                                    echo "==END_OPTIONS==" """
        } else {
            testProvider.scriptFile = File.createTempFile("option",".bat")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """@echo off
                                    echo ==START_OPTIONS==
                                    echo First Option:opt1
                                    echo Second Option:opt2
                                    echo ==END_OPTIONS== """
        }
        ScriptOptionValues scriptOptionValues = new ScriptOptionValues(testProvider, Stub(ServiceProviderLoader))
        def result = scriptOptionValues.getOptionValues([:])

        then:
        result.size() == 2
        result[0].name == "First Option"
        result[0].value == "opt1"
        result[1].name == "Second Option"
        result[1].value == "opt2"

    }

    class TestScriptOptionValuesProvider implements ScriptPluginProvider {

        File scriptFile
        File archiveFile
        File contentsBaseDir

        @Override
        String getName() {
            return "test"
        }

        @Override
        String getService() {
            return "OptionValues"
        }

        @Override
        File getArchiveFile() {
            return archiveFile
        }

        @Override
        File getContentsBasedir() {
            return contentsBaseDir
        }

        @Override
        String getScriptArgs() {
            return null
        }

        @Override
        String[] getScriptArgsArray() {
            return new String[0]
        }

        @Override
        File getScriptFile() {
            return scriptFile
        }

        @Override
        String getScriptInterpreter() {
            return null
        }

        @Override
        boolean getInterpreterArgsQuoted() {
            return false
        }

        @Override
        Map<String, Object> getMetadata() {
            return [:]
        }

        @Override
        PluginMeta getPluginMeta() {
            return null
        }

        @Override
        boolean getDefaultMergeEnvVars() {
            return false
        }

        @Override
        Map<String, String> getProviderMeta() {
            return null
        }
    }
}
