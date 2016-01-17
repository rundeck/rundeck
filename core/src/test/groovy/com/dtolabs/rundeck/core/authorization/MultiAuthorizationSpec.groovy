package com.dtolabs.rundeck.core.authorization

import spock.lang.Specification

import javax.security.auth.Subject

import static com.dtolabs.rundeck.core.authorization.Explanation.Code.*

/**
 * Created by greg on 7/20/15.
 */
class MultiAuthorizationSpec extends Specification {


    def "evaluate multiple decisions"(
            Explanation.Code code1,
            Explanation.Code code2,
            Explanation.Code expectCode
    )
    {
        given:
        def auth1 = Mock(Authorization) {
            evaluate(!null, !null, 'fake', !null) >> Mock(Decision) {
                isAuthorized() >> (code1 == GRANTED)
                explain() >> Mock(Explanation) {
                    getCode() >> code1
                }
            }

        }
        def auth2 = Mock(Authorization) {
            evaluate(!null, !null, 'fake', !null) >> Mock(Decision) {
                isAuthorized() >> (code2 == GRANTED)
                explain() >> Mock(Explanation) {
                    getCode() >> code2
                }
            }
        }
        def multi = new MultiAuthorization(auth1, auth2)

        def decision = multi.evaluate([:], new Subject(), 'fake', [] as Set<Attribute>)
        expect:
        (expectCode == GRANTED) == decision.isAuthorized()
        expectCode == decision.explain().getCode()

        where:
        code1           | code2           | expectCode
        REJECTED_DENIED | GRANTED         | REJECTED_DENIED
        REJECTED_DENIED | REJECTED        | REJECTED_DENIED
        REJECTED_DENIED | REJECTED_DENIED | REJECTED_DENIED
        REJECTED        | GRANTED         | GRANTED
        REJECTED        | REJECTED        | REJECTED
        REJECTED        | REJECTED_DENIED | REJECTED_DENIED
        GRANTED         | GRANTED         | GRANTED
        GRANTED         | REJECTED        | GRANTED
        GRANTED         | REJECTED_DENIED | REJECTED_DENIED
    }

    def "eval set"(Map<String,Explanation.Code> resp1,Map<String,Explanation.Code> resp2,Map<String,Explanation.Code> expect) {
        given:

        Set<Map<String,String>> resSet = [[a: 'a'], [b: 'b'], [c: 'c']] as Set
        Set<String> authSet = ['fake', 'fake2', 'fake3'] as Set
        def result1=resp1.collect{pair->
            def code=pair.value
            def parts=pair.key.split(':',2)
            return Mock(Decision) {
                getAction() >> parts[1]
                getResource() >> [(parts[0]):parts[0]]
                isAuthorized() >> (code==GRANTED)
                explain() >> Mock(Explanation) {
                    getCode() >> code
                }
            }
        } as Set
        def auth1 = Mock(Authorization) {
            evaluate(resSet, !null, !null, !null) >> result1
        }
        def result2=resp2.collect{pair->
            def code=pair.value
            def parts=pair.key.split(':',2)
            return Mock(Decision) {
                getAction() >> parts[1]
                getResource() >> [(parts[0]):parts[0]]
                isAuthorized() >> (code==GRANTED)
                explain() >> Mock(Explanation) {
                    getCode() >> code
                }
            }
        } as Set
        def auth2 = Mock(Authorization) {
            evaluate(resSet, !null, !null, !null) >> result2
        }
        def multi = new MultiAuthorization(auth1, auth2)
        def resp=multi.evaluate(resSet,new Subject(),authSet,[] as Set)
        expect:
        resp.collectEntries{[("${it.resource.values().first()}:${it.action}".toString()):it.explain().getCode()]}==expect

        where:
        //same matrix as single evaluation test,
        resp1 | resp2 | expect
        ['a:fake':REJECTED_DENIED,
         'a:fake2':REJECTED_DENIED,
         'a:fake3':REJECTED_DENIED,
         'b:fake':REJECTED,
         'b:fake2':REJECTED,
         'b:fake3':REJECTED,
         'c:fake':GRANTED,
         'c:fake2':GRANTED,
         'c:fake3':GRANTED        ] | ['a:fake':GRANTED,
                                        'a:fake2':REJECTED,
                                        'a:fake3':REJECTED_DENIED,
                                        'b:fake':GRANTED,
                                        'b:fake2':REJECTED,
                                        'b:fake3':REJECTED_DENIED,
                                        'c:fake':GRANTED,
                                        'c:fake2':REJECTED,
                                        'c:fake3':REJECTED_DENIED ] | ['a:fake':REJECTED_DENIED,
                                                                        'a:fake2':REJECTED_DENIED,
                                                                        'a:fake3':REJECTED_DENIED,
                                                                        'b:fake':GRANTED,
                                                                        'b:fake2':REJECTED,
                                                                        'b:fake3':REJECTED_DENIED,
                                                                        'c:fake':GRANTED,
                                                                        'c:fake2':GRANTED,
                                                                        'c:fake3':REJECTED_DENIED]

    }
}
