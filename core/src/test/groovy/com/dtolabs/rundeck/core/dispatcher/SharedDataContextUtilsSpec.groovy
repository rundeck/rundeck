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

package com.dtolabs.rundeck.core.dispatcher

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 11/2/16.
 */
class SharedDataContextUtilsSpec extends Specification {
    @Unroll
    def "multi context replace #str"() {
        given:
        WFSharedContext shared = new WFSharedContext()
        shared.merge(ContextView.global(), new BaseDataContext([a: [b: "global", globalval: "aglobalval"]]))
        shared.merge(ContextView.node("node1"), new BaseDataContext([a: [b: "node1", nodeval: "anodeval"]]))
        shared.merge(ContextView.node("node2"), new BaseDataContext([a: [b: "node2", nodeval: "anodeval2"]]))
        shared.merge(
                ContextView.nodeStep(2, "node1"),
                new BaseDataContext([a: [b: "step2 node1", nodestepval: "anodestepval"]])
        )
        shared.merge(
                ContextView.nodeStep(2, "node2"),
                new BaseDataContext([a: [b: "step2 node2", nodestepval: "anodestepval2"]])
        )
        shared.merge(ContextView.step(2), new BaseDataContext([a: [b: "step2", stepval: "astepval"]]))
        when:
        def result = SharedDataContextUtils.replaceDataReferences(
                str,
                shared,
                ContextView.global(),
                ContextView.&nodeStep,
                null,
                false,
                true
        )
        then:
        result == expected

        where:
        str                        | expected
        'abc'                      | 'abc'
        '${a.b}'                   | 'global'
        '${a.globalval}'           | 'aglobalval'
        '${a.b@badnode}'           | ''
        '${a.globalval@badnode}'   | ''
        '${a.b@node1}'             | 'node1'
        '${a.nodeval@node1}'       | 'anodeval'
        '${a.globalval@node1}'     | ''
        '${2:a.b@node1}'           | 'step2 node1'
        '${2:a.nodestepval@node1}' | 'anodestepval'
        '${2:a.nodeval@node1}'     | ''
        '${2:a.globalval@node1}'   | ''
        '${2:a.b}'                 | 'step2'
        '${2:a.stepval}'           | 'astepval'
        '${2:a.nodeval}'           | ''
        '${2:a.nodestepval}'       | ''
        '${2:a.globalval}'         | ''
        '${2:a.b*}'                | 'step2 node1,step2 node2'
        '${a.b*}'                  | 'node1,node2'
        '${a.nodeval*}'            | 'anodeval,anodeval2'
    }

    @Unroll
    def "replace data references in #str default node context #nodeName expect #expected"() {
        given:
        WFSharedContext shared = new WFSharedContext()
        shared.merge(ContextView.global(), new BaseDataContext([a: [b: "global", globalval: "aglobalval"]]))
        shared.merge(
                ContextView.nodeStep(1, "node1"),
                new BaseDataContext([a: [b: "nodestep1", nodeval: "anodestepval"]])
        )
        shared.merge(
                ContextView.nodeStep(1, "node2"),
                new BaseDataContext([a: [b: "nodestep2", nodeval: "anodestepval2"]])
        )
        shared.merge(ContextView.node("node1"), new BaseDataContext([a: [b: "node1", nodeval: "anodeval"]]))
        shared.merge(ContextView.node("node2"), new BaseDataContext([a: [b: "node2", nodeval: "anodeval2"]]))
        shared.merge(ContextView.step(1), new BaseDataContext([a: [b: "step1", stepval: "astepval1"]]))
        when:
        def result = SharedDataContextUtils.replaceDataReferences(
                str,
                shared,
                ContextView.node(nodeName),
                ContextView.&nodeStep,
                null,
                false,
                true
        )
        then:
        result == expected

        where:
        str              | nodeName | expected
        '${a.b}'         | 'node1'  | 'node1'
        '${a.b@node1}'   | 'node1'  | 'node1'
        '${a.b@node2}'   | 'node1'  | 'node2'
        '${1:a.b}'       | 'node1'  | 'nodestep1'
        '${1:a.b@node1}' | 'node1'  | 'nodestep1'
        '${1:a.b*}'      | 'node1'  | 'nodestep1,nodestep2'
        '${a.b*}'        | 'node1'  | 'node1,node2'
//        '${1:a.b@}'      | 'node1'  | 'node1'
//        '${1:a.b@.}'     | 'node1'  | 'node1'
        '${1:a.stepval}' | 'node1'  | 'astepval1'
        '${1:a.b@node1}' | 'node1'  | 'nodestep1'
        '${1:a.b@node2}' | 'node1'  | 'nodestep2'
        '${1:a.b@node3}' | 'node1'  | ''

    }

    @Unroll
    def "replace data references in #str no node context expect #expected"() {
        given:
        WFSharedContext shared = new WFSharedContext()
        shared.merge(ContextView.global(), new BaseDataContext([a: [b: "global", globalval: "aglobalval"]]))
        shared.merge(ContextView.node("node1"), new BaseDataContext([a: [b: "node1", nodeval: "anodeval"]]))
        shared.merge(ContextView.node("node2"), new BaseDataContext([a: [b: "node2", nodeval: "anodeval2"]]))
        shared.merge(ContextView.step(1), new BaseDataContext([a: [b: "step1", stepval: "astepval1"]]))
        when:
        def result = SharedDataContextUtils.replaceDataReferences(
                str,
                shared,
                ContextView.global(),
                ContextView.&nodeStep,
                null,
                false,
                true
        )
        then:
        result == expected

        where:
        str              | expected
        '${a.b}'         | 'global'
        '${a.b@node1}'   | 'node1'
        '${a.b@node2}'   | 'node2'
        '${a.b*}'        | 'node1,node2'
        '${1:a.b}'       | 'step1'
        '${1:a.stepval}' | 'astepval1'
        '${1:a.b@node1}' | ''
        '${1:a.b@node2}' | ''

    }

    @Unroll
    def "replace tokens in script duplicate start char #script"() {
        given:
        Framework fwk = null
        File dest = File.createTempFile('test', 'tmp')

        when:
        File result = DataContextUtils.replaceTokensInScript(
                script,
                context,
                fwk,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest
        )


        then:
        result.text == expect

        where:

        script                                 | context                       | expect
        'abc'                                  | [a: [b: 'bcd']]               | 'abc\n'
        'echo \'hello@@option.domain@\''       | [:]                           | 'echo \'hello@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''      | [:]                           | 'echo \'hello@\'\n'
        'echo \'hello@milk @option.domain@\''  | [:]                           | 'echo \'hello@milk \'\n'
        'echo \'hello@@option.domain@\''       | [option: [domain: 'peabody']] | 'echo \'hello@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''      | [option: [domain: 'peabody']] | 'echo \'hello@peabody\'\n'
        'echo \'hello@milk @option.domain@\''  | [option: [domain: 'peabody']] | 'echo \'hello@milk peabody\'\n'
        'echo \'hello@milk@option.domain@\''   | [option: [domain: 'peabody']] | 'echo \'hellooption.domain@\'\n'
        'echo \'hello@@milk.@option.domain@\'' | [option: [domain: 'peabody']] | 'echo \'hello@milk.peabody\'\n'
        'bloo hello@@@nothing@'                | [:]                           | 'bloo hello@\n'
        'bloo hello@@@ending'                  | [:]                           | 'bloo hello@@ending\n'
        'bloo hello@ending'                    | [:]                           | 'bloo hello@ending\n'
        'bloo hello@@@ending\n'                | [:]                           | 'bloo hello@@ending\n'
    }

    @Unroll
    def "replace tokens in script escaped start char #script"() {
        given:
        Framework fwk = null
        File dest = File.createTempFile('test', 'tmp')

        when:
        File result = DataContextUtils.replaceTokensInScript(
                script,
                context,
                fwk,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest
        )


        then:
        result.text == expect

        where:

        script                                  | context                       | expect
        'abc'                                   | [a: [b: 'bcd']]               | 'abc\n'
        'a\\\\bc'                               | [a: [b: 'bcd']]               | 'a\\\\bc\n'
        'a@@bc'                                 | [a: [b: 'bcd']]               | 'a@bc\n'
        'echo \'hello\\@@option.domain@\''      | [:]                           | 'echo \'hello\\@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''       | [:]                           | 'echo \'hello@\'\n'
        'echo \'hello\\@milk @option.domain@\'' | [:]                           | 'echo \'hello\\@milk \'\n'
        'echo \'hello@@milk @option.domain@\''  | [:]                           | 'echo \'hello@milk \'\n'
        'echo \'hello\\@@option.domain@\''      | [option: [domain: 'peabody']] | 'echo \'hello\\@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''       | [option: [domain: 'peabody']] | 'echo \'hello@peabody\'\n'
        'echo \'hello\\@milk@option.domain@\''  | [option: [domain: 'peabody']] | 'echo \'hello\\option.domain@\'\n'
        'echo \'hello@@milk@option.domain@\''   | [option: [domain: 'peabody']] | 'echo \'hello@milkpeabody\'\n'
    }

    @Unroll
    def "shared replace tokens in script #script"() {
        given:
        File dest = File.createTempFile('test', 'tmp')
        def sharedContext = WFSharedContext.with(ContextView.global(), DataContextUtils.context(context))
        def node = null

        when:
        SharedDataContextUtils.replaceTokensInScript(
                script,
                sharedContext,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest,
                node
        )


        then:
        dest.text == expect

        where:

        script                                  | context                       | expect
        'abc'                                   | [a: [b: 'bcd']]               | 'abc\n'
        'a\\\\bc'                               | [a: [b: 'bcd']]               | 'a\\\\bc\n'
        'a@@bc'                                 | [a: [b: 'bcd']]               | 'a@bc\n'
        'echo \'hello\\@@option.domain@\''      | [:]                           | 'echo \'hello\\@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''       | [:]                           | 'echo \'hello@\'\n'
        'echo \'hello\\@milk @option.domain@\'' | [:]                           | 'echo \'hello\\@milk \'\n'
        'echo \'hello@@milk @option.domain@\''  | [:]                           | 'echo \'hello@milk \'\n'
        'echo \'hello\\@@option.domain@\''      | [option: [domain: 'peabody']] | 'echo \'hello\\@option.domain@\'\n'
        'echo \'hello@@@option.domain@\''       | [option: [domain: 'peabody']] | 'echo \'hello@peabody\'\n'
        'echo \'hello\\@milk@option.domain@\''  | [option: [domain: 'peabody']] | 'echo \'hello\\option.domain@\'\n'
        'echo \'hello@@milk@option.domain@\''   | [option: [domain: 'peabody']] | 'echo \'hello@milkpeabody\'\n'
    }

    @Unroll
    def "shared replace tokens node context in script #script"() {
        given:
        File dest = File.createTempFile('test', 'tmp')
        def sharedContext = WFSharedContext.with(ContextView.global(), DataContextUtils.context(context))
        sharedContext.merge(ContextView.node('mynode'), DataContextUtils.context(nodedata))
        def node = 'mynode'

        when:
        SharedDataContextUtils.replaceTokensInScript(
                script,
                sharedContext,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest,
                node
        )


        then:
        dest.text == expect

        where:

        script                     | context                       | nodedata                    | expect
        'echo \'@option.domain@\'' | [:]                           | [:]                         | 'echo \'\'\n'
        'echo \'@option.domain@\'' | [option: [domain: 'peabody']] | [:]                         | 'echo \'peabody\'\n'
        'echo \'@option.domain@\'' | [option: [domain: 'peabody']] | [option: [domain: 'olive']] | 'echo \'olive\'\n'
        'echo \'@option.domain@\'' | [:]                           | [option: [domain: 'olive']] | 'echo \'olive\'\n'
    }

    @Unroll
    def "shared replace tokens node context with global base in script #script"() {
        given:
        File dest = File.createTempFile('test', 'tmp')
        def base = WFSharedContext.with(ContextView.global(), DataContextUtils.context(context))
        def sharedContext = WFSharedContext.with(base)
        sharedContext.merge(ContextView.node('mynode'), DataContextUtils.context(nodedata))
        def node = 'mynode'

        when:
        SharedDataContextUtils.replaceTokensInScript(
                script,
                sharedContext,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest,
                node
        )


        then:
        dest.text == expect

        where:

        script                     | context                       | nodedata                    | expect
        'echo \'@option.domain@\'' | [:]                           | [:]                         | 'echo \'\'\n'
        'echo \'@option.domain@\'' | [option: [domain: 'peabody']] | [:]                         | 'echo \'peabody\'\n'
        'echo \'@option.domain@\'' | [option: [domain: 'peabody']] | [option: [domain: 'olive']] | 'echo \'olive\'\n'
        'echo \'@option.domain@\'' | [:]                           | [option: [domain: 'olive']] | 'echo \'olive\'\n'
    }
}
