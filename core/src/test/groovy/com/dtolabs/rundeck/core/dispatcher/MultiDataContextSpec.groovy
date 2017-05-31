/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/2/17
 */
class MultiDataContextSpec extends Specification {

    @Unroll
    def "resolve basic with default #view"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base"]])
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
        null                             | "blah"     | "xyz"  | "base"
        null                             | "notfound" | "xyz"  | "xyz"
        ContextView.global()             | "blah"     | "xyz"  | "global"
        ContextView.global()             | "notfound" | "xyz"  | "xyz"
        ContextView.node("anode")        | "blah"     | "xyz"  | "anode"
        ContextView.node("anode")        | "notfound" | "xyz"  | "xyz"
        ContextView.step(1)              | "blah"     | "xyz"  | "step1"
        ContextView.step(1)              | "notfound" | "xyz"  | "xyz"
        ContextView.nodeStep(1, "bnode") | "blah"     | "xyz"  | "bnode step 1"
        ContextView.nodeStep(1, "bnode") | "notfound" | "xyz"  | "xyz"
    }

    @Unroll
    def "resolve basic with strict #view #key"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base", blee: "base"]])
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))


        when:
        def result = context.resolve(view, true, "test", key, defval)
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
    def "resolve expand view #view find #key"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base", blee: "base"]])
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
        ContextView.nodeStep(2, "anode") | "blah" | "anode"
        ContextView.nodeStep(2, "anode") | "blee" | "base"
        ContextView.nodeStep(2, "bnode") | "blah" | "global"
        ContextView.nodeStep(2, "bnode") | "blee" | "base"
        ContextView.nodeStep(1, "znode") | "blah" | "global"
        ContextView.nodeStep(1, "znode") | "blee" | "base"
        ContextView.nodeStep(2, "znode") | "blah" | "global"
        ContextView.nodeStep(2, "znode") | "blee" | "base"
        null                             | "blah" | "base"
        null                             | "blee" | "base"
    }

    @Unroll
    def "resolve expand key view #view #key"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base", base: "base value"]])
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global", "global": "global value"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode", "node": "node value"]]))
//        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1", "step": "step value"]]))
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
        ContextView.nodeStep(1, "anode") | "node"     | "node value"
        ContextView.nodeStep(1, "anode") | "global"   | "global value"
        ContextView.nodeStep(1, "anode") | "base"     | "base value"
    }
}
