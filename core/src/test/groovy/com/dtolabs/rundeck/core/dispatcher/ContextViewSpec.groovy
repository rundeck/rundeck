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

/**
 * @author greg
 * @since 5/2/17
 */
class ContextViewSpec extends Specification {
    def "widen"() {
        expect:
        ContextView.global().isGlobal()
        ContextView.global().isWidest()
        !ContextView.step(1).isGlobal()
        !ContextView.step(1).isWidest()
        !ContextView.nodeStep(1, "node").isGlobal()
        !ContextView.nodeStep(1, "node").isWidest()
        !ContextView.node("node").isGlobal()
        !ContextView.node("node").isWidest()

        ContextView.global().widenView().isGlobal()
        ContextView.global().widenView().isWidest()
        ContextView.step(1).widenView().isGlobal()
        ContextView.step(1).widenView().isWidest()

        !ContextView.nodeStep(1, "node").widenView().isGlobal()
        !ContextView.nodeStep(1, "node").widenView().isWidest()
        ContextView.nodeStep(1, "node").widenView().getStepNumber() == null
        ContextView.nodeStep(1, "node").widenView().getNodeName() == "node"

        ContextView.node("node").widenView().isGlobal()
        ContextView.node("node").widenView().isWidest()

    }

    def "compare"() {
        given:
        def global = ContextView.global()
        def nodea = ContextView.node('nodea')
        def step1nodea = ContextView.nodeStep(1, 'nodea')
        def step2nodea = ContextView.nodeStep(2, 'nodea')
        def nodeb = ContextView.node('nodeb')
        def step1nodeb = ContextView.nodeStep(1, 'nodeb')
        def step2nodeb = ContextView.nodeStep(2, 'nodeb')
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
