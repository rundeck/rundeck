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
package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.VersionConstants
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.Modifier


class PluginMetadataValidatorTest extends Specification {


    def "ValidateTargetHostCompatibility"() {
        when:
        def errors = []
        Field field = PluginMetadataValidator.getDeclaredField("OS_TYPE")
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null,rundeckHost)
        PluginMetadataValidator.validateTargetHostCompatibility(errors, targetHost)
        String validation = errors.isEmpty() ? "compatible" : "incompatible"

        then:
        validation == expected

        where:
        rundeckHost   |   targetHost      | expected
        "windows"     |    "android"      | "incompatible"
        "windows"     |    null           | "incompatible"
        "windows"     |    "unix"         | "incompatible"
        "unix"        |    "windows"      | "incompatible"
        "windows"     |    "windows"      | "compatible"
        "unix"        |    "unix"         | "compatible"
        "mac os x"    |    "unix"         | "compatible"
        "freebsd"     |    "unix"         | "compatible"

    }

    def "ValidateRundeckCompatibility"() {
        when:
        def errors = []
        Field field = VersionConstants.getDeclaredField("VERSION")
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null,rundeckVersion)
        PluginMetadataValidator.validateRundeckCompatibility(errors, compatVersion)
        String validation = errors.isEmpty() ? "compatible" : "incompatible"

        then:
        validation == expected

        where:
        rundeckVersion  |   compatVersion   | expected
        "3.0.0"         |    null          | "incompatible"
        "3.0.0"         |    "2.0"          | "incompatible"
        "3.0.0"         |    "2.11.x"       | "incompatible"
        "2.11.0"        |    "3.0.x"        | "incompatible"
        "3.1.0"         |    "3.0.0+"       | "incompatible"
        "3.0.5"         |    "3.1.0+"       | "incompatible"
        "3.0.0"         |    "3.0.5+"       | "incompatible"
        "4.0.0"         |    "3.0+"         | "incompatible"
        "3.0.0"         |    "3.0.0"        | "compatible"
        "3.0.0"         |    "3.0.x"        | "compatible"
        "3.0.5"         |    "3.0.x"        | "compatible"
        "3.0.5"         |    "3.0.5+"       | "compatible"
        "3.0.5"         |    "3.0+"         | "compatible"
        "3.1.2"         |    "3.0+"         | "compatible"
        "3.9.6"         |    "3.0+"         | "compatible"
    }
}
