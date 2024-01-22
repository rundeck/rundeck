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

import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import spock.lang.Specification


class ScriptUserGroupSourceSpec extends Specification {
    def isPosixSystem() {
        def osName = System.getProperty("os.name").toLowerCase()
        return !(osName.contains("win") || osName.contains("mac") || osName.contains("sunos"))
    }

    def "GetGroups"() {
        when:

        TestScriptUserGroupSourceProvider testProvider = new TestScriptUserGroupSourceProvider()
        testProvider.archiveFile = File.createTempFile("user","archive")
        testProvider.contentsBaseDir = File.createTempDir()
        if (isPosixSystem()) {
            testProvider.scriptFile = File.createTempFile("user","groups")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """echo "==START_GROUPS=="
                                    echo "group1"
                                    echo "group2"
                                    echo "==END_GROUPS==" """
        } else {
            testProvider.scriptFile = File.createTempFile("option",".bat")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """@echo off
                                    echo ==START_GROUPS==
                                    echo group1
                                    echo group2
                                    echo ==END_GROUPS=="""
        }
        ScriptUserGroupSource scriptOptionValues = new ScriptUserGroupSource(testProvider, Stub(ServiceProviderLoader))
        def result = scriptOptionValues.getGroups("test",[:])

        then:
        result.size() == 2
        result[0] == "group1"
        result[1] == "group2"

    }

    def "GetGroups passing username to the script as argument"() {
        when:

        TestScriptUserGroupSourceProvider testProvider = new TestScriptUserGroupSourceProvider()
        testProvider.archiveFile = File.createTempFile("user","archive")
        testProvider.contentsBaseDir = File.createTempDir()
        testProvider.scriptArgs = "\${session.username}"
        if (isPosixSystem()) {
            testProvider.scriptFile = File.createTempFile("user","groups")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """echo "==START_GROUPS=="
                                    echo "\$1"
                                    echo "==END_GROUPS==" """
        } else {
            testProvider.scriptFile = File.createTempFile("option",".bat")
            testProvider.scriptFile.setExecutable(true,true)
            testProvider.scriptFile << """@echo off
                                    echo ==START_GROUPS==
                                    echo %1
                                    echo ==END_GROUPS=="""
        }
        ScriptUserGroupSource scriptOptionValues = new ScriptUserGroupSource(testProvider, Stub(ServiceProviderLoader))
        def result = scriptOptionValues.getGroups("test",[:])

        then:
        result.size() == 1
        result[0] == "test"

    }

    class TestScriptUserGroupSourceProvider implements ScriptPluginProvider {
        File scriptFile
        File archiveFile
        File contentsBaseDir
        String scriptArgs

        @Override
        String getName() {
            return "test"
        }

        @Override
        String getService() {
            return ServiceNameConstants.UserGroupSource
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
            return scriptArgs
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
