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
}
