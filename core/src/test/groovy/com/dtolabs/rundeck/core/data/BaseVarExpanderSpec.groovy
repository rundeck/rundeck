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
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification

/**
 * @author greg
 * @since 6/12/17
 */
class BaseVarExpanderSpec extends Specification {
    def "expand all multi level"() {
        given:
        def data1 = WFSharedContext.with(null)
        data1.merge(ContextView.node('anode'), DataContextUtils.context([test: [testkey: 'aval']]))
        def data2 = WFSharedContext.with(data1)
        data2.merge(ContextView.node('bnode'), DataContextUtils.context([test: [testkey: 'bval']]))
        def data3 = WFSharedContext.with(data2)
        data3.merge(ContextView.node('cnode'), DataContextUtils.context([test: [testkey: 'cval']]))
        def viewmap = ContextView.&nodeStep

        when:
        List<String> result = BaseVarExpander.expandAllNodesVariable(
                data3,
                ContextView.global(),
                viewmap,
                step,
                group,
                key
        )

        then:
        result == expected

        where:
        step = null
        group = 'test'
        key = 'testkey'
        expected = ['aval', 'bval', 'cval']

    }
}
