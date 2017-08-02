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

import static com.dtolabs.rundeck.core.dispatcher.ContextView.global
import static com.dtolabs.rundeck.core.dispatcher.ContextView.node
import static com.dtolabs.rundeck.core.dispatcher.ContextView.nodeStep
import static com.dtolabs.rundeck.core.dispatcher.ContextView.step

/**
 * @author greg
 * @since 5/2/17
 */
class ContextViewSpec extends Specification {
    @Unroll
    def "widen"() {
        expect:
        global().isGlobal()
        global().isWidest()
        !ContextView.step(1).isGlobal()
        !ContextView.step(1).isWidest()
        !nodeStep(1, "node").isGlobal()
        !nodeStep(1, "node").isWidest()
        !node("node").isGlobal()
        !node("node").isWidest()

        global().widenView().isGlobal()
        global().widenView().isWidest()
        ContextView.step(1).widenView().isGlobal()
        ContextView.step(1).widenView().isWidest()

        !nodeStep(1, "node").widenView().isGlobal()
        !nodeStep(1, "node").widenView().isWidest()
        nodeStep(1, "node").widenView().getStepNumber() == 1
        nodeStep(1, "node").widenView().getNodeName() == null

        node("node").widenView().isGlobal()
        node("node").widenView().isWidest()

    }

    @Unroll
    def "merge #a to #b is #result"() {

        expect:
        result == a.merge(b)
        where:
        a        | b                || result
        global() | global()         || global()
        global() | node('a')        || node('a')
        global() | nodeStep(1, 'a') || nodeStep(1, 'a')
        step(1)  | node('a')        || nodeStep(1, 'a')
        step(1)  | nodeStep(2, 'a') || nodeStep(1, 'a')

    }

    @Unroll
    def "#a isWider #b is #result"() {

        expect:
        result == a.isWider(b)
        where:
        a                | b                | result
        global()         | global()         | false
        node('a')        | global()         | true
        step(1)          | global()         | true
        nodeStep(1, 'a') | global()         | true
        node('b')        | node('a')        | false
        step(1)          | node('a')        | true
        nodeStep(1, 'a') | node('a')        | true
        node('a')        | step(1)          | false
        step(2)          | step(1)          | false
        nodeStep(1, 'a') | step(1)          | true
        node('b')        | nodeStep(1, 'a') | false
        step(2)          | nodeStep(1, 'a') | false
        nodeStep(2, 'b') | nodeStep(1, 'a') | false

    }

    def "isContainerOf"() {
        expect:
        global().globExpandTo(node('blah'))
        node('blah1').globExpandTo(node('blah'))
        !nodeStep(1, 'blah1').globExpandTo(node('blah'))
        !nodeStep(1, 'blah1').globExpandTo(nodeStep(2, 'blah'))
        nodeStep(1, 'blah1').globExpandTo(nodeStep(1, 'blah'))

    }

    def "compare"() {
        given:
        def global = global()
        def nodea = node('nodea')
        def step1nodea = nodeStep(1, 'nodea')
        def step2nodea = nodeStep(2, 'nodea')
        def nodeb = node('nodeb')
        def step1nodeb = nodeStep(1, 'nodeb')
        def step2nodeb = nodeStep(2, 'nodeb')
        def step1 = ContextView.step(1)
        def step2 = ContextView.step(2)
        expect:
        global.compareTo(global) == 0
        global.compareTo(nodea) == -1
        global.compareTo(nodeb) == -1
        global.compareTo(step1) == -1
        global.compareTo(step2) == -1
        global.compareTo(step1nodea) == -1
        global.compareTo(step2nodea) == -1
        global.compareTo(step1nodeb) == -1
        global.compareTo(step2nodeb) == -1

        nodea.compareTo(nodea) == 0
        nodea.compareTo(nodeb) == -1
        nodea.compareTo(global) == 1
        nodea.compareTo(step1nodea) == 1
        nodea.compareTo(step2nodea) == 1
        nodea.compareTo(step1nodeb) == 1
        nodea.compareTo(step2nodeb) == 1
        nodea.compareTo(step1) == 1
        nodea.compareTo(step2) == 1

        nodeb.compareTo(nodea) == 1
        nodeb.compareTo(nodeb) == 0
        nodeb.compareTo(global) == 1
        nodeb.compareTo(step1nodea) == 1
        nodeb.compareTo(step2nodea) == 1
        nodeb.compareTo(step1nodeb) == 1
        nodeb.compareTo(step2nodeb) == 1
        nodeb.compareTo(step1) == 1
        nodeb.compareTo(step2) == 1

        step1nodea.compareTo(nodea) == -1
        step1nodea.compareTo(nodeb) == -1
        step1nodea.compareTo(global) == 1
        step1nodea.compareTo(step1nodea) == 0
        step1nodea.compareTo(step2nodea) == -1
        step1nodea.compareTo(step1nodeb) == -1
        step1nodea.compareTo(step2nodeb) == -1
        step1nodea.compareTo(step1) == 1
        step1nodea.compareTo(step2) == -1

        step2nodea.compareTo(nodea) == -1
        step2nodea.compareTo(nodeb) == -1
        step2nodea.compareTo(global) == 1
        step2nodea.compareTo(step1nodea) == 1
        step2nodea.compareTo(step2nodea) == 0
        step2nodea.compareTo(step1nodeb) == 1
        step2nodea.compareTo(step2nodeb) == -1
        step2nodea.compareTo(step1) == 1
        step2nodea.compareTo(step2) == 1

        step1nodeb.compareTo(nodea) == -1
        step1nodeb.compareTo(nodeb) == -1
        step1nodeb.compareTo(global) == 1
        step1nodeb.compareTo(step1nodea) == 1
        step1nodeb.compareTo(step2nodea) == -1
        step1nodeb.compareTo(step1nodeb) == 0
        step1nodeb.compareTo(step2nodeb) == -1
        step1nodeb.compareTo(step1) == 1
        step1nodeb.compareTo(step2) == -1

        step2nodeb.compareTo(nodea) == -1
        step2nodeb.compareTo(nodeb) == -1
        step2nodeb.compareTo(global) == 1
        step2nodeb.compareTo(step1nodea) == 1
        step2nodeb.compareTo(step2nodea) == 1
        step2nodeb.compareTo(step1nodeb) == 1
        step2nodeb.compareTo(step2nodeb) == 0
        step2nodeb.compareTo(step1) == 1
        step2nodeb.compareTo(step2) == 1

        step1.compareTo(nodea) == -1
        step1.compareTo(nodeb) == -1
        step1.compareTo(global) == 1
        step1.compareTo(step1nodea) == -1
        step1.compareTo(step2nodea) == -1
        step1.compareTo(step1nodeb) == -1
        step1.compareTo(step2nodeb) == -1
        step1.compareTo(step1) == 0
        step1.compareTo(step2) == -1

        step2.compareTo(nodea) == -1
        step2.compareTo(nodeb) == -1
        step2.compareTo(global) == 1
        step2.compareTo(step1nodea) == 1
        step2.compareTo(step2nodea) == -1
        step2.compareTo(step1nodeb) == 1
        step2.compareTo(step2nodeb) == -1
        step2.compareTo(step1) == 1
        step2.compareTo(step2) == 0

    }
}
