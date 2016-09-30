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

package com.dtolabs.rundeck.server.plugins.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import spock.lang.Specification

/**
 * Created by greg on 8/30/16.
 */
class ScriptUIPluginSpec extends Specification {

    def "validate missing ui:"() {
        given:

        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    :
            ]
        }
        when:
        ScriptUIPlugin.validateScriptPlugin(provider)

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /ui:/
    }

    def "create missing ui:"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    :
            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /ui:/

    }

    def "create missing pages:"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [:]
            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /ui: pages:/

    }

    def "create invalid requires:"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [
                            pages   : 'xyz',
                            scripts : 'asdf',
                            requires: invalidRequires
                    ]
            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /ui: pages: requires:/

        where:
        invalidRequires | _
        [a: 'b']        | _
        123L            | _
        [123]           | _
        []              | _

    }

    def "create missing pages: scripts or styles"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: invalidContent
            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /either 'scripts' or 'styles' was expected/

        where:
        invalidContent                                       | _
        [pages: 'test',]                                     | _
        [[pages: 'test']]                                    | _
        [[pages: 'test'], [pages: 'test2', scripts: 'asdf']] | _
        [[pages: 'test'], [pages: 'test2', styles: 'asdf']]  | _
    }

    def "validate missing pages:"() {
        given:

        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [:]
            ]
        }
        when:
        ScriptUIPlugin.validateScriptPlugin(provider)

        then:
        IllegalArgumentException e = thrown()
        e.message =~ /ui:/
    }

    def "create with metadata"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [
                            pages  : pages,
                            scripts: scripts,
                            styles : styles,
                    ]

            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        plugin.doesApply('a/test')
        plugin.resourcesForPath('a/test') == expectAll
        plugin.scriptResourcesForPath('a/test') == (scripts == null ? null : ['a/b'])
        plugin.styleResourcesForPath('a/test') == (styles == null ? null : ['a/c'])


        where:
        pages      | scripts | styles  | expectAll
        'a/test'   | 'a/b'   | null    | ['a/b']
        'a/test'   | null    | 'a/c'   | ['a/c']
        ['a/test'] | 'a/b'   | null    | ['a/b']
        ['a/test'] | null    | 'a/c'   | ['a/c']
        'a/test'   | ['a/b'] | null    | ['a/b']
        'a/test'   | null    | ['a/c'] | ['a/c']
    }

    def "create with multiple entries"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [
                            [pages  : pages,
                             scripts: scripts,
                             styles : styles
                            ],
                            [pages  : pages2,
                             scripts: scripts2,
                             styles : styles2
                            ],
                    ]

            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        plugin.doesApply('a/test')
        plugin.resourcesForPath('a/test') == expectA
        plugin.scriptResourcesForPath('a/test') == (scripts == null ? null : ['a/b'])
        plugin.styleResourcesForPath('a/test') == (styles == null ? null : ['a/c'])
        plugin.resourcesForPath('b/test') == expectB
        plugin.scriptResourcesForPath('b/test') == (scripts2 == null ? null : ['b/c'])
        plugin.styleResourcesForPath('b/test') == (styles2 == null ? null : ['b/d'])


        where:
        pages      | scripts | styles  | expectA | pages2     | scripts2 | styles2 | expectB
        'a/test'   | 'a/b'   | null    | ['a/b'] | 'b/test'   | 'b/c'    | null    | ['b/c']
        'a/test'   | null    | 'a/c'   | ['a/c'] | 'b/test'   | null     | 'b/d'   | ['b/d']
        ['a/test'] | 'a/b'   | null    | ['a/b'] | ['b/test'] | 'b/c'    | null    | ['b/c']
        ['a/test'] | null    | 'a/c'   | ['a/c'] | ['b/test'] | null     | 'b/d'   | ['b/d']
        'a/test'   | ['a/b'] | null    | ['a/b'] | 'b/test'   | ['b/c']  | null    | ['b/c']
        'a/test'   | null    | ['a/c'] | ['a/c'] | 'b/test'   | null     | ['b/d'] | ['b/d']
    }

    def "create with multiple pages"() {
        given:
        def provider = Mock(ScriptPluginProvider) {
            getMetadata() >> [
                    ui: [
                            pages  : pages,
                            scripts: scripts,
                            styles : styles

                    ]

            ]
        }
        when:
        def plugin = new ScriptUIPlugin(provider, Mock(Framework))

        then:
        plugin.doesApply('a/test')
        plugin.resourcesForPath('a/test') == expectA
        plugin.scriptResourcesForPath('a/test') == (scripts == null ? null : ['a/b'])
        plugin.styleResourcesForPath('a/test') == (styles == null ? null : ['a/c'])
        plugin.resourcesForPath('b/test') == expectA


        where:
        pages                | scripts | styles  | expectA
        ['a/test', 'b/test'] | 'a/b'   | null    | ['a/b']
        ['a/test', 'b/test'] | null    | 'a/c'   | ['a/c']
        ['a/test', 'b/test'] | ['a/b'] | null    | ['a/b']
        ['a/test', 'b/test'] | null    | ['a/c'] | ['a/c']
    }

}
