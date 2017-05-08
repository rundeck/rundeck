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
}
