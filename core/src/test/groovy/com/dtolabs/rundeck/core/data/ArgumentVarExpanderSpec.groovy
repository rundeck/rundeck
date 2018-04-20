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

package com.dtolabs.rundeck.core.data

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/31/17
 */
class ArgumentVarExpanderSpec extends Specification {
    @Unroll
    def "parse arg string var valid"() {
        given:
        when:
        def result = new ArgumentVarExpander().parseVariable(str)
        then:
        result.step == step
        result.group == group
        result.key == key
        result.node == node
        result.nodeglob == nodeglob
        result.format == format

        where:
        str           | step | group | key | node      | nodeglob | format
        'a.b'         | null | 'a'   | 'b' | null      | false    | null
        'a.b@badnode' | null | 'a'   | 'b' | 'badnode' | false    | null
        '2:a.b'       | "2"  | 'a'   | 'b' | null      | false    | null
        '2:a.b@node1' | "2"  | 'a'   | 'b' | 'node1'   | false    | null
        'a.b*'        | null | 'a'   | 'b' | null      | true     | null
        'a.b*json'    | null | 'a'   | 'b' | null      | true     | 'json'
        '1:a.b*'      | '1'  | 'a'   | 'b' | null      | true     | null
        '1:a.b*json'  | '1'  | 'a'   | 'b' | null      | true     | 'json'
    }


    @Unroll
    def "parse arg string var invalid"() {
        given:
        when:
        def result = new ArgumentVarExpander().parseVariable(str)
        then:
        result == null

        where:
        str            | _
        'a'            | _
        'a .b@node'    | _
        '2: a.b'       | _
        '2:a.b @node1' | _
    }

    @Unroll
    def "expand glob #str is #expected"() {
        given:
        def expander = new ArgumentVarExpander()
        WFSharedContext shared = new WFSharedContext()

        shared.merge(ContextView.global(), new BaseDataContext([a: [b: "global", globalval: "aglobalval"]]))
        shared.merge(
                ContextView.node("node1"),
                new BaseDataContext([a: [b: "node1", nodeval: "anodeval"]])
        )
        shared.merge(
                ContextView.node("node2"),
                new BaseDataContext([a: [b: "node2", nodeval: "anodeval2"]])
        )
        shared.merge(
                ContextView.step(2),
                new BaseDataContext([a: [b: "step2", stepval: "astepval"]])
        )
        shared.merge(
                ContextView.nodeStep(2, "node1"),
                new BaseDataContext([a: [b: "step2 node1", nodestepval: "anodestepval"]])
        )
        shared.merge(
                ContextView.nodeStep(2, "node2"),
                new BaseDataContext([a: [b: "step2 node2", nodestepval: "anodestepval2"]])
        )

        when:
        def result = expander.expandVariable(shared, ContextView.global(), ContextView.&nodeStep, str)
        then:
        result == expected
        where:
        str       | expected
        'a.b'     | 'global'
        'a.b*'    | 'node1,node2'
        'a.b*+'   | 'node1+node2'
        '1:a.b'   | null
        '2:a.b'   | 'step2'
        '2:a.b*'  | 'step2 node1,step2 node2'
        '2:a.b*-' | 'step2 node1-step2 node2'
    }

    @Unroll
    def "expand glob #str default node"() {
        given:
        def expander = new ArgumentVarExpander()
        WFSharedContext shared = new WFSharedContext()

        shared.merge(ContextView.global(), new BaseDataContext([a: [b: "global", globalval: "aglobalval"]]))
        shared.merge(
                ContextView.node("node1"),
                new BaseDataContext([a: [b: "node1", nodeval: "anodeval"]])
        )
        shared.merge(
                ContextView.node("node2"),
                new BaseDataContext([a: [b: "node2", nodeval: "anodeval2"]])
        )
        shared.merge(
                ContextView.step(2),
                new BaseDataContext([a: [b: "step2", stepval: "astepval"]])
        )
        shared.merge(
                ContextView.nodeStep(2, "node1"),
                new BaseDataContext([a: [b: "step2 node1", nodestepval: "anodestepval"]])
        )
        shared.merge(
                ContextView.nodeStep(2, "node2"),
                new BaseDataContext([a: [b: "step2 node2", nodestepval: "anodestepval2"]])
        )

        when:
        def result = expander.expandVariable(shared, ContextView.node(defnode), ContextView.&nodeStep, str)
        then:
        result == expected
        where:
        str       | defnode | expected
        'a.b'     | 'node1' | 'node1'
        'a.b*'    | 'node1' | 'node1,node2'
        'a.b*+'   | 'node1' | 'node1+node2'
        '1:a.b'   | 'node1' | null
        '2:a.b'   | 'node1' | 'step2 node1'
        '2:a.b*'  | 'node1' | 'step2 node1,step2 node2'
        '2:a.b*-' | 'node1' | 'step2 node1-step2 node2'
    }
}
