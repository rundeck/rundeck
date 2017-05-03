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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/2/17
 */
class MultiDataContextSpec extends Specification {
    @Unroll
    def "resolve basic view #view"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base"]])
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))

        when:
        def result = context.resolve(view, "test", "blah")
        then:
        result == expect

        where:
        view                             | expect

        ContextView.global()             | "global"
        ContextView.node("anode")        | "anode"
        ContextView.step(1)              | "step1"
        ContextView.nodeStep(1, "bnode") | "bnode step 1"
        null                             | "base"
    }

    @Unroll
    def "resolve expand view #view"() {
        given:
        MultiDataContextImpl<ContextView, DataContext> context = new MultiDataContextImpl<ContextView, DataContext>(
                new BaseDataContext([test: [blah: "base"]])
        )
        context.merge(ContextView.global(), new BaseDataContext([test: [blah: "global"]]))
        context.merge(ContextView.node("anode"), new BaseDataContext([test: [blah: "anode"]]))
        context.merge(ContextView.step(1), new BaseDataContext([test: [blah: "step1"]]))
        context.merge(ContextView.nodeStep(1, "bnode"), new BaseDataContext([test: [blah: "bnode step 1"]]))

        when:
        def result = context.resolve(view, "test", "blah")
        then:
        result == expect

        where:
        view                             | expect

        ContextView.global()             | "global"
        ContextView.node("znode")        | "global"
        ContextView.step(2)              | "global"
        ContextView.nodeStep(2, "anode") | "anode"
        ContextView.nodeStep(2, "bnode") | "global"
        ContextView.nodeStep(1, "znode") | "global"
        ContextView.nodeStep(2, "znode") | "global"
        null                             | "base"
    }
}
