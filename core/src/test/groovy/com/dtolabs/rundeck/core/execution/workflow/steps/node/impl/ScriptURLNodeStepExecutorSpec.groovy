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

package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl

import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/9/17
 */
class ScriptURLNodeStepExecutorSpec extends Specification {
    @Unroll
    def "shared context data in url string"() {
        given:
        final WFSharedContext datacontext = new WFSharedContext();
        datacontext.merge(
                ContextView.global(),
                new BaseDataContext("data", [value: 'some value ? for things & stuff'])
        );
        datacontext.merge(ContextView.node("anodename"), new BaseDataContext("node", [name: 'node/name']));
        datacontext.merge(ContextView.node("bnodename"), new BaseDataContext("node", [name: 'bogus']));
        datacontext.merge(ContextView.node("bnodename"), new BaseDataContext("data", [value: 'hotenntot']));
        when:
        def result = ScriptURLNodeStepExecutor.expandUrlString(
                url,
                datacontext,
                curnode
        )
        then:
        result != null
        result == expected

        where:
        url                                                                            | curnode     | expected

        'http://example.com/path/${node.name}?query=${data.value}'                     | "anodename" |
                'http://example.com/path/node%2Fname?query=some%20value%20%3F%20for%20things%20%26%20stuff'
        'http://example.com/path/${node.name}?query=${data.value}'                     | "bnodename" |
                'http://example.com/path/bogus?query=hotenntot'
        'http://example.com/path/${node.name@bnodename}?query=${data.value}'           | "anodename" |
                'http://example.com/path/bogus?query=some%20value%20%3F%20for%20things%20%26%20stuff'
        'http://example.com/path/${node.name@bnodename}?query=${data.value@bnodename}' | "anodename" |
                'http://example.com/path/bogus?query=hotenntot'

    }
}
