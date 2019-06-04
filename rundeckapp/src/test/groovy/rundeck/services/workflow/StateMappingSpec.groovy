/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services.workflow

import grails.converters.JSON
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class StateMappingSpec extends Specification {
    def "stepStatesForNode from grails json parser with missing nodeState"() {
        given:
            def sut = new StateMapping()
            def json = '''{
"nodes":{
"Anode":[
    {
        "stepctx":"1/1"
    }
],
"MissingNode":[
    {
        "stepctx":"1/1"
    }
]
},
"steps":[
    {
    "parameterStates":{},
    "workflow":{
        "steps":[
            {
                "nodeStates":{
                    "Anode":{"state":"test"}
                }
            }
        ]
    }
    }
]
}'''
            def map = JSON.parse(json)
        when:
            def result = sut.stepStatesForNode(map, node)
        then:
            result == expect
        where:
            node          | expect
            'Anode'       | [[stepctx: '1/1', state: 'test']]
            'MissingNode' | [[stepctx: '1/1']]

    }
}
