package com.dtolabs.rundeck.core.execution.workflow.state

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 4/25/16.
 */
class StateUtilsSpec extends Specification {

    @Unroll
    def "stepIdentifier to and from String"() {
        expect:
        StateUtils.stepIdentifierToString(new StepIdentifierImpl(input)) == identString
        StateUtils.stepIdentifierFromString(identString) == new StepIdentifierImpl(input)

        where:
        input                                                                    | identString
        [ctxItem(1, false, null)]                                                             | '1'
        [ctxItem(1, false, null), ctxItem(2, false, null)]                                    | '1/2'
        [ctxItem(1, false, [node: 'node1']), ctxItem(2, false, [node: 'node2'])]              |
                '1@node=node1/2@node=node2'
        [ctxItem(1, false, [node: 'n/b']), ctxItem(2, false, [node: 'node2'])]                |
                '1@node=n\\/b/2@node=node2'
        [ctxItem(1, false, [node: 'n/b=c,d3@peanut', z: 'park']), ctxItem(2, false, [node: 'node2'])] |
                '1@node=n\\/b\\\\=c\\\\,d3@peanut,z=park/2@node=node2'
    }

    private StateUtils.CtxItem ctxItem(int num, boolean iserr=false, Map params=null) {
        new StateUtils.CtxItem(num, iserr, params)
    }


    @Unroll
    def "stepContextId from and to String"() {
        expect:
        StateUtils.stepContextIdToString(ctxItem(step, iserr, params)) == expect

        StateUtils.stepContextIdFromString(expect).aspect == iserr ? StepAspect.ErrorHandler : StepAspect.Main
        StateUtils.stepContextIdFromString(expect).step == step
        StateUtils.stepContextIdFromString(expect).params == params



        where:
        expect                         | iserr | step | params
        '1@a=b,c=d'                    | false | 1    | [a: 'b', c: 'd']
        '1e@a=b,c=d'                   | true  | 1    | [a: 'b', c: 'd']
        '1@node=my/node'               | false | 1    | [node: 'my/node']
        '1@node=my\\=node'             | false | 1    | [node: 'my=node']
        '1@node=my\\,node'             | false | 1    | [node: 'my,node']
        '1@node=my@node'               | false | 1    | [node: 'my@node']
        '1@node=my\\\\node'            | false | 1    | [node: 'my\\node']
        '1@node=my\\\\n,z@dang=til/de' | false | 1    | [node: 'my\\n', ('z@dang'): 'til/de']
    }

    @Unroll
    def "parameterString"() {
        expect:
        StateUtils.parameterString(input) == result
        StateUtils.parseParameterString(result) == input
        where:
        input              | result

        [a: 'b', c: 'd']   | 'a=b,c=d'
        [node: 'my/node']  | 'node=my/node'
        [node: 'my\\node'] | 'node=my\\\\node'
        [node: 'my=node']  | 'node=my\\=node'
        [node: 'my,node']  | 'node=my\\,node'
        [node: 'my@node']  | 'node=my@node'
    }

    @Unroll
    def "parseParameterString"() {
        expect:
        StateUtils.parseParameterString(input) == result
        StateUtils.parameterString(result) == input
        where:
        input                  | result

        'a=b,c=d'              | [a: 'b', c: 'd']
        'node=my\\\\/node'     | [node: 'my\\/node']
        'node=my\\\\node'      | [node: 'my\\node']
        'node=my\\=node'       | [node: 'my=node']
        'node=my\\,node'       | [node: 'my,node']
        'node=my\\\\@node'     | [node: 'my\\@node']
        'a=b\\,c,node=my@node' | [a: 'b,c', node: 'my@node']

    }
}
