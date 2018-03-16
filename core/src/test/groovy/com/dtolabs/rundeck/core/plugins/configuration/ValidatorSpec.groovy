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

package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.core.plugins.EmbeddedType
import com.dtolabs.rundeck.core.plugins.MultiPluginProviderLoader
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ValidatorSpec extends Specification {
    @Unroll
    def "validateProperties options can have collection values #input"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().options('testOptionsDynamic').dynamicValues(true).build(),
            PropertyBuilder.builder().select('testSelectStatic').values(['asdf', 'xyz']).build(),
            PropertyBuilder.builder().options('testOptionsStatic').values(['3asdf', '3xyz']).build(),
            PropertyBuilder.builder().string('testString').build(),
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid
        report.errors == errMap

        where:
        input                                  || isValid | errMap
        [testOptionsDynamic: 'asdf']           || true    | [:]
        [testOptionsDynamic: ['abc', 'def']]   || true    | [:]

        [testSelectStatic: 'asdf']             || true    | [:]
        [testSelectStatic: 'xyz']              || true    | [:]
        [testSelectStatic: ['asdf']]           || false   | [testSelectStatic: 'Invalid data type: expected a String']
        [testSelectStatic: ['xyz']]            || false   | [testSelectStatic: 'Invalid data type: expected a String']
        [testSelectStatic: ['abc', 'def']]     || false   | [testSelectStatic: 'Invalid data type: expected a String']

        [testOptionsStatic: '3asdf']           || true    | [:]
        [testOptionsStatic: '3xyz']            || true    | [:]
        [testOptionsStatic: 'asdf']            || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']
        [testOptionsStatic: ['3asdf', '3xyz']] || true    | [:]
        [testOptionsStatic: ['3asdf']]         || true    | [:]
        [testOptionsStatic: ['3xyz']]          || true    | [:]
        [testOptionsStatic: ['3asdf', 'xyz']]  || false   | [testOptionsStatic: 'Invalid value(s): [xyz]']
        [testOptionsStatic: ['asdf', '3xyz']]  || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']
        [testOptionsStatic: ['asdf']]          || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']

        [testString: 'ok']                     || true    | [:]
        [testString: ['notok']]                || false   | [testString: 'Invalid data type: expected a String']
        [testString: [a: 'b']]                 || false   | [testString: 'Invalid data type: expected a String']
    }

    @Unroll
    def "validateProperties map requires a map"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Map).dynamicValues(true).build(),
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors['testMap'] == errMessage


        where:
        input                     || isValid | errMessage
        [testMap: 'asdf']         || false   | 'Invalid data type: expected a Map'
        [testMap: ['abc', 'def']] || false   | 'Invalid data type: expected a Map'
        [testMap: [asdf: 'xyz']]  || true    | null

    }

    @EmbeddedType(name = 'test')
    static class TestEmbed {
        @PluginProperty
        String myname

        @PluginProperty(required = true)
        String asdf

        @PluginProperty()
        @SelectValues(values = ['asdf', 'xyz'])
        List<String> myopts
    }

    @Unroll
    def "validateProperties embedded requires a map"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Embedded).embeddedType(TestEmbed).build()
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors['testMap'] == errMessage

        where:
        input                     || isValid | errMessage
        [testMap: 'asdf']         || false   | 'Invalid data type: expected a Map'
        [testMap: ['abc', 'def']] || false   | 'Invalid data type: expected a Map'
        [testMap: [asdf: 'xyz']]  || true    | null
    }

    @Unroll
    def "validateProperties embedded list requires a list of maps or map"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Options).embeddedType(TestEmbed).build()
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors['testMap'] == errMessage

        where:
        input                      || isValid | errMessage
        [testMap: 'asdf']          || false   | 'Invalid data type: expected a Map or List of Map'
        [testMap: ['abc', 'def']]  || false   | 'Invalid data type: expected a Map or List of Map'
        [testMap: [asdf: 'xyz']]   || true    | null
        [testMap: [[asdf: 'xyz']]] || true    | null
    }

    @Unroll
    def "validateProperties embedded tests values"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Embedded).embeddedType(TestEmbed).build()
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors == errors


        where:
        input                                              || isValid | errors
        [testMap: [zasdf: 'boof']]                         || false   | ['testMap.asdf': 'required']
        [testMap: [asdf: 'boof']]                          || true    | [:]
        [testMap: [asdf: 'boof', myopts: 'z']]             || false   | ['testMap.myopts': 'Invalid value(s): [z]']
        [testMap: [asdf: 'boof', myopts: 'asdf']]          || true    | [:]
        [testMap: [asdf: 'boof', myopts: ['asdf']]]        || true    | [:]
        [testMap: [asdf: 'boof', myopts: ['xyz', 'asdf']]] || true    | [:]

    }

    @Unroll
    def "validateProperties embedded list tests values"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Options).embeddedType(TestEmbed).build()
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors == errors


        where:
        input                                              || isValid | errors
        [testMap: [zasdf: 'boof']]                         || false   | ['testMap.asdf': 'required']
        [testMap: [asdf: 'boof']]                          || true    | [:]
        [testMap: [asdf: 'boof', myopts: 'z']]             || false   | ['testMap.myopts': 'Invalid value(s): [z]']
        [testMap: [asdf: 'boof', myopts: 'asdf']]          || true    | [:]
        [testMap: [asdf: 'boof', myopts: ['asdf']]]        || true    | [:]
        [testMap: [asdf: 'boof', myopts: ['xyz', 'asdf']]] || true    | [:]

    }

    @Unroll
    def "validateProperties embedded plugin invalid input"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Embedded).embeddedPluginType(TestEmbed).build()
        ]
        def loader = Mock(MultiPluginProviderLoader)
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope, loader)
        then:
        report.valid == isValid
        report.errors == errors
        0 * loader._(*_)


        where:
        input                      || isValid | errors
        [testMap: [zasdf: 'boof']] || false   |
        ['testMap': 'Invalid data type: expected a Map containing \'type\' and \'config\'']
        [testMap: 'asdf']          || false   |
        ['testMap': 'Invalid data type: expected a Map']
        [testMap: ['asdf']]        || false   |
        ['testMap': 'Invalid data type: expected a Map']
        [testMap: [['asdf': 'z']]] || false   |
        ['testMap': 'Invalid data type: expected a Map']
    }

    @Plugin(name = 'test', service = 'Notification')
    static class TestEmbedNotification implements NotificationPlugin {
        @PluginProperty
        String myname

        @PluginProperty(required = true)
        String asdf

        @PluginProperty()
        @SelectValues(values = ['asdf', 'xyz'])
        List<String> myopts

        @Override
        boolean postNotification(final String trigger, final Map executionData, final Map config) {
            return false
        }
    }

    @Unroll
    def "validateProperties embedded plugin valid"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().
                name('testMap').
                type(Property.Type.Embedded).
                embeddedPluginType(TestEmbedNotification).
                build()
        ]
        def loader = Mock(MultiPluginProviderLoader)
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope, loader)
        then:
        report.valid == isValid
        report.errors == errors
        1 * loader.describe(TestEmbedNotification, 'test') >>
        PluginAdapterUtility.buildDescription(TestEmbedNotification, DescriptionBuilder.builder(), true)
        0 * loader._(*_)


        where:
        input                                                                   || isValid | errors
        [testMap: [type: 'test', config: [:]]]                                  || false   |
        ['testMap.config.asdf': 'required']
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'z']]]             || false   |
        ['testMap.config.myopts': 'Invalid value(s): [z]']
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'asdf']]]          || true    | [:]
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'xyz']]]           || true    | [:]
        [testMap: [type: 'test', config: [asdf: 'x', myopts: ['asdf', 'xyz']]]] || true    | [:]
    }

    @Unroll
    def "validateProperties embedded plugin list valid"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().
                name('testMap').
                type(Property.Type.Options).
                embeddedPluginType(TestEmbedNotification).
                build()
        ]
        def loader = Mock(MultiPluginProviderLoader)
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope, loader)
        then:
        report.valid == isValid
        report.errors == errors
        count * loader.describe(TestEmbedNotification, 'test') >>
        PluginAdapterUtility.buildDescription(TestEmbedNotification, DescriptionBuilder.builder(), true)
        0 * loader._(*_)


        where:
        input                                                                         || isValid|count| errors
        [testMap: [type: 'test', config: [:]]]                                        || false  |1| ['testMap[0].config.asdf': 'required']
        [testMap: [[type: 'test', config: [asdf: 'x']], [type: 'test', config: [:]]]] || false  |2| ['testMap[1].config.asdf': 'required']
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'z']]]                   || false  |1| ['testMap[0].config.myopts': 'Invalid value(s): [z]']
        [testMap: [[type: 'test', config: [asdf: 'x']],[type: 'test', config: [asdf: 'x', myopts: 'z']]]]                   || false  |2| ['testMap[1].config.myopts': 'Invalid value(s): [z]']
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'asdf']]]                || true   |1| [:]
        [testMap: [[type: 'test', config: [asdf: 'x']],[type: 'test', config: [asdf: 'x', myopts: 'asdf']]]]                || true   |2| [:]
        [testMap: [type: 'test', config: [asdf: 'x', myopts: 'xyz']]]                 || true   |1| [:]
        [testMap: [[type: 'test', config: [asdf: 'x']],[type: 'test', config: [asdf: 'x', myopts: 'xyz']]]]                 || true   |2| [:]
        [testMap: [type: 'test', config: [asdf: 'x', myopts: ['asdf', 'xyz']]]]       || true   |1| [:]
        [testMap: [[type: 'test', config: [asdf: 'x']],[type: 'test', config: [asdf: 'x', myopts: ['asdf', 'xyz']]]]]       || true   |2| [:]
    }
}
