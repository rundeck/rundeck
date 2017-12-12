/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.app.api.scm

import spock.lang.Specification

/**
 * Created by greg on 12/1/15.
 */
class ScmActionSpec extends Specification {
    def "input becomes string string map"(){
        expect:
        'b'==ScmAction.parseWithJson([input:[a:'b']]).input.a
        'true'==ScmAction.parseWithJson([input:[a:true]]).input.a
        '123'==ScmAction.parseWithJson([input:[a:123]]).input.a
    }
    def "jobids becomes string list"(){
        expect:
        ['a']==ScmAction.parseWithJson([jobs:['a']]).jobIds
        ['a','123']==ScmAction.parseWithJson([jobs:['a',123]]).jobIds
        ['a','123','true']==ScmAction.parseWithJson([jobs:['a',123,true]]).jobIds
    }

    def "delete jobs input xml"() {
        given:
        def slurp = new XmlSlurper().parseText(xml)

        when:
        def result = ScmAction.parseWithXml(slurp)

        then:
        result instanceof ScmAction
        result.jobIds == ['job1']
        result.selectedItems == ['item1']
        result.deletedItems == ['delitem1', 'delitem2']
        result.deletedJobs == ['deljob1', 'deljob2']
        result.input == [message: 'blah']

        where:
        xml | _
        '''<scmAction>
    <input>
        <entry key="message">blah</entry>
    </input>
    <jobs>
        <job jobId="job1"/>
    </jobs>
    <items>
        <item itemId="item1"/>
    </items>
    <deleted>
        <item itemId="delitem1"/>
        <item itemId="delitem2"/>
    </deleted>
    <deletedJobs>
        <job jobId="deljob1"/>
        <job jobId="deljob2"/>
    </deletedJobs>
</scmAction>''' | _
    }
}
