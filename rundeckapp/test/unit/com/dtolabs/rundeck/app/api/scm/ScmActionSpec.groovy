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
