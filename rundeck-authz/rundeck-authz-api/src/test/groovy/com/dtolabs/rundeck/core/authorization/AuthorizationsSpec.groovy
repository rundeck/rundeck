package com.dtolabs.rundeck.core.authorization

import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

import static com.dtolabs.rundeck.core.authorization.Explanation.Code.GRANTED
import static com.dtolabs.rundeck.core.authorization.Explanation.Code.REJECTED
import static com.dtolabs.rundeck.core.authorization.Explanation.Code.REJECTED_DENIED

class AuthorizationsSpec extends Specification {
    static final Explanation.Code G = GRANTED
    static final Explanation.Code R = REJECTED
    static final Explanation.Code D = REJECTED_DENIED

    def "append single auth"() {
        given:
            def auth1 = Mock(Authorization)
            def auth2 = Mock(Authorization)
            def auth3 = Authorizations.append(auth1, auth2)
            def resource = [a: 'resource']
            def subject = new Subject()
            def action = 'action'
            def env = AuthorizationUtil.RUNDECK_APP_ENV
        when:
            def result = auth3.evaluate(resource, subject, action, env)
        then:
            1 * auth1.evaluate(resource, subject, action, env) >> decision1
            1 * auth2.evaluate(resource, subject, action, env) >> decision2
            result.authorized == isauth
            result.explain().code == code

        where:
            decision1 | decision2 | isauth | code
            mkd(G)    | mkd(R)    | true   | G
            mkd(R)    | mkd(G)    | true   | G
            mkd(G)    | mkd(G)    | true   | G
            mkd(R)    | mkd(R)    | false  | R
            mkd(D)    | mkd(G)    | false  | D
            mkd(D)    | mkd(R)    | false  | D
            mkd(R)    | mkd(D)    | false  | D
            mkd(G)    | mkd(D)    | false  | D
    }

    @Unroll
    def "append multi auth granted"() {
        given:
            def auth1 = Mock(Authorization)
            def auth2 = Mock(Authorization)
            def auth3 = Authorizations.append(auth1, auth2)
            def res = [b: 'resource']
            def subject = new Subject()
            def action = 'a'
            def action2 = 'b'
            def env = AuthorizationUtil.RUNDECK_APP_ENV
            Set<Map<String, String>> resSet = [res].toSet()
            def actions = [action, action2].toSet()
        when:
            def result = auth3.evaluate(resSet, subject, actions, env)
        then:
            1 * auth1.evaluate(resSet, subject, actions, env) >> {
                (0..1).collect { mkd(evalA[it], res, actions[it]) }
            }
            1 * auth2.evaluate(resSet, subject, actions, env) >> {
                (0..1).collect { mkd(evalB[it], res, actions[it]) }
            }
            result.size() == 2
            result.find { it.action == 'a' }.authorized == (expect[0] == G)
            result.find { it.action == 'a' }.explain().code == codes[0]
            result.find { it.action == 'b' }.authorized == (expect[1] == G)
            result.find { it.action == 'b' }.explain().code == codes[1]

        where:
            evalA  | evalB  | expect | codes
        //Grant over reject
            [R, R] | [R, R] | [R, R] | [R, R]

            [R, G] | [R, R] | [R, G] | [R, G]
            [R, R] | [R, G] | [R, G] | [R, G]
            [R, G] | [R, G] | [R, G] | [R, G]

            [R, R] | [G, R] | [G, R] | [G, R]
            [G, R] | [R, R] | [G, R] | [G, R]
            [G, R] | [G, R] | [G, R] | [G, R]

            [G, G] | [R, R] | [G, G] | [G, G]
            [G, G] | [R, G] | [G, G] | [G, G]
            [G, G] | [G, R] | [G, G] | [G, G]
            [G, G] | [G, G] | [G, G] | [G, G]
            [R, G] | [G, R] | [G, G] | [G, G]
            [G, R] | [R, G] | [G, G] | [G, G]
            [G, R] | [G, G] | [G, G] | [G, G]
            [R, G] | [G, G] | [G, G] | [G, G]
        //Deny over reject
            [R, D] | [R, R] | [R, D] | [R, D]
            [R, R] | [R, D] | [R, D] | [R, D]
            [R, D] | [R, D] | [R, D] | [R, D]

            [R, R] | [D, R] | [D, R] | [D, R]
            [D, R] | [R, R] | [D, R] | [D, R]
            [D, R] | [D, R] | [D, R] | [D, R]

            [D, D] | [R, R] | [D, D] | [D, D]
            [D, D] | [R, D] | [D, D] | [D, D]
            [D, D] | [D, R] | [D, D] | [D, D]
            [D, D] | [D, D] | [D, D] | [D, D]
            [R, D] | [D, R] | [D, D] | [D, D]
            [D, R] | [R, D] | [D, D] | [D, D]
            [D, R] | [D, D] | [D, D] | [D, D]
            [R, D] | [D, D] | [D, D] | [D, D]
        //Deny over grant
            [G, D] | [G, G] | [G, D] | [G, D]
            [G, G] | [G, D] | [G, D] | [G, D]
            [G, D] | [G, D] | [G, D] | [G, D]

            [G, G] | [D, G] | [D, G] | [D, G]
            [D, G] | [G, G] | [D, G] | [D, G]
            [D, G] | [D, G] | [D, G] | [D, G]

            [D, D] | [G, G] | [D, D] | [D, D]
            [D, D] | [G, D] | [D, D] | [D, D]
            [D, D] | [D, G] | [D, D] | [D, D]
            [D, D] | [D, D] | [D, D] | [D, D]
            [G, D] | [D, G] | [D, D] | [D, D]
            [D, G] | [G, D] | [D, D] | [D, D]
            [D, G] | [D, D] | [D, D] | [D, D]
            [G, D] | [D, D] | [D, D] | [D, D]
    }


    static Decision mkd(int ordinal, Map<String, String> resource = [:], String action = 'action') {
        mkd(Explanation.Code.values()[ordinal])
    }

    static Decision mkd(boolean success, Map<String, String> resource = [:], String action = 'action') {
        mkd(success ? GRANTED : REJECTED, resource, action)
    }

    static Decision mkd(Explanation.Code code, Map<String, String> resource = [:], String action = 'action') {
        new D(
            authorized: code == GRANTED,
            explanation: new E(
                code: code
            ),
            duration: 1,
            resource: resource,
            action: action,
            environment: AuthorizationUtil.RUNDECK_APP_ENV,
            subject: null
        )
    }

    static class E implements Explanation {
        Code code
        Explanation other

        @Override
        void describe(final PrintStream out) {
            other?.describe(out)
        }
    }

    static class D implements Decision {
        boolean authorized
        Explanation explanation
        long duration
        Map<String, String> resource
        String action
        Set<Attribute> environment
        Subject subject

        @Override
        long evaluationDuration() {
            return duration
        }

        @Override
        Explanation explain() {
            return explanation
        }
    }
}
