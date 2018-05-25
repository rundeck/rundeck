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

package com.dtolabs.rundeck.core.dispatcher

import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.data.MultiDataContextImpl
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/2/17
 */
class MultiDataContextSpec extends Specification {

    @Unroll
    def "resolve with base #view #key is #expect"() {
        given:
        def base = WFSharedContext.with(ContextView.global(), new BaseDataContext([test: [blah2: "base"]]))
        base.merge(ContextView.node("anode"), new BaseDataContext([test: [blah2: "baseanode"]]))
        base.merge(ContextView.step(1), new BaseDataContext([test: [blah2: "basestep1"]]))
        base.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah2: "basebnode step 1"]]))

        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                base
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))

        when:
        def result = context.resolve(view, "test", key, defval)
        then:
        result == expect

        where:
        view                             | key        | defval | expect
        ContextView.global()             | "blah"     | "xyz"  | "global"
        ContextView.global()             | "notfound" | "xyz"  | "xyz"
        ContextView.node("anode")        | "blah"     | "xyz"  | "anode"
        ContextView.node("anode")        | "notfound" | "xyz"  | "xyz"
        ContextView.step(1)              | "blah"     | "xyz"  | "step1"
        ContextView.step(1)              | "notfound" | "xyz"  | "xyz"
        ContextView.nodeStep(1, "bnode") | "blah"     | "xyz"  | "bnode step 1"
        ContextView.nodeStep(1, "bnode") | "notfound" | "xyz"  | "xyz"
        //resolve base values
        ContextView.global()             | "blah2"    | "xyz"  | "base"
        ContextView.global()             | "notfound2" | "xyz" | "xyz"
        ContextView.node("anode")        | "blah2" | "xyz"     | "baseanode"
        ContextView.node("anode")        | "notfound2" | "xyz" | "xyz"
        ContextView.step(1)              | "blah2" | "xyz"     | "basestep1"
        ContextView.step(1)              | "notfound2" | "xyz" | "xyz"
        ContextView.nodeStep(1, "bnode") | "blah2" | "xyz"     | "basebnode step 1"
        ContextView.nodeStep(1, "bnode") | "notfound2" | "xyz" | "xyz"
    }

    @Unroll
    def "resolve with multiple bases default #view #key is #expect"() {
        given:
        def base0 = WFSharedContext.with(null)
        base0.merge(ContextView.global(), new BaseDataContext([test: [blah3: "base0"]]))
        base0.merge(ContextView.node("anode"), new BaseDataContext([test: [blah3: "base0anode"]]))
        base0.merge(ContextView.step(1), new BaseDataContext([test: [blah3: "base0step1"]]))
        base0.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah3: "base0bnode step 1"]]))


        def base1 = WFSharedContext.with(base0)

        base1.merge(ContextView.global(), new BaseDataContext([test: [blah2: "base"]]))
        base1.merge(ContextView.node("anode"), new BaseDataContext([test: [blah2: "baseanode"]]))
        base1.merge(ContextView.step(1), new BaseDataContext([test: [blah2: "basestep1"]]))
        base1.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah2: "basebnode step 1"]]))

        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                base1
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))

        when:
        def result = context.resolve(view, "test", key, defval)
        then:
        result == expect

        where:
        view                             | key         | defval | expect

        //resolve base3 values
        ContextView.global()             | "blah3"     | "xyz"  | "base0"
        ContextView.node("anode")        | "blah3"     | "xyz"  | "base0anode"
        ContextView.step(1)              | "blah3"     | "xyz"  | "base0step1"
        ContextView.nodeStep(1, "bnode") | "blah3"     | "xyz"  | "base0bnode step 1"
    }

    @Unroll
    def "resolve basic with strict #view key=#key def #defval expect #expect"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>()

        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))


        when:
        def result = context.resolve(view, view, "test", key, defval)
        then:
        result == expect

        where:
        view                             | key        | defval | expect
        ContextView.global()             | "blah"     | "xyz"  | "global"
        ContextView.global()             | "blee"     | null   | null
        ContextView.global()             | "blee"     | "xyz"  | "xyz"
        ContextView.node("anode")        | "blah"     | "xyz"  | "anode"
        ContextView.node("anode")        | "notfound" | null   | null
        ContextView.node("anode")        | "notfound" | "xyz"  | "xyz"
        ContextView.step(1)              | "blah"     | "xyz"  | "step1"
        ContextView.step(1)              | "notfound" | null   | null
        ContextView.step(1)              | "notfound" | "xyz"  | "xyz"
        ContextView.nodeStep(1, "bnode") | "blah"     | "xyz"  | "bnode step 1"
        ContextView.nodeStep(1, "bnode") | "notfound" | null   | null
        ContextView.nodeStep(1, "bnode") | "notfound" | "xyz"  | "xyz"
    }

    @Unroll
    def "resolve expand view #view find #key expect #expect"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                WFSharedContext.with(ContextView.global(), new BaseDataContext([test: [blah: "base", blee: "base"]]))
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))

        when:
        def result = context.resolve(view, "test", key)
        then:
        result == expect

        where:
        view                             | key    | expect

        ContextView.global()             | "blah" | "global"
        ContextView.global()             | "blee" | "base"
        ContextView.node("znode")        | "blah" | "global"
        ContextView.node("znode")        | "blee" | "base"
        ContextView.step(2)              | "blah" | "global"
        ContextView.step(2)              | "blee" | "base"
        ContextView.nodeStep(2, "anode") | "blah" | "global"
        ContextView.nodeStep(2, "anode") | "blee" | "base"
        ContextView.nodeStep(2, "bnode") | "blah" | "global"
        ContextView.nodeStep(2, "bnode") | "blee" | "base"
        ContextView.nodeStep(1, "znode") | "blah" | "step1"
        ContextView.nodeStep(1, "znode") | "blee" | "base"
        ContextView.nodeStep(2, "znode") | "blah" | "global"
        ContextView.nodeStep(2, "znode") | "blee" | "base"
    }

    @Unroll
    def "resolve expand key view #view #key"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                WFSharedContext.with(
                        ContextView.global(),
                        new BaseDataContext([test: [blah: "base", base: "base value"]])
                )
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global", "global": "global value"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode", "node": "node value"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1", "step": "step value"]]))
        context.merge(
                ContextView.nodeStep(1, "bnode"),
                new BaseDataContext([test: [blah: "bnode step 1", "nodestep": "nodestep value"]])
        )

        when:
        def result = context.resolve(view, "test", key)
        then:
        result == expect

        where:
        view                             | key        | expect
        ContextView.nodeStep(1, "bnode") | "nodestep" | "nodestep value"
        ContextView.nodeStep(1, "bnode") | "global"   | "global value"
//        ContextView.nodeStep(1, "bnode") | "step"     | "step value"
        ContextView.nodeStep(1, "anode") | "nodestep" | null
        ContextView.nodeStep(1, "anode") | "node"     | null
        ContextView.nodeStep(1, "anode") | "step"     | "step value"
        ContextView.nodeStep(1, "anode") | "global"   | "global value"
        ContextView.nodeStep(1, "anode") | "base"     | "base value"
    }
}
