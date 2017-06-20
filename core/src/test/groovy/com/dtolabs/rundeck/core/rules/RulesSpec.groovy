package com.dtolabs.rundeck.core.rules

import spock.lang.Specification

/**
 * Created by greg on 5/17/16.
 */
class RulesSpec extends Specification {
    def "equals condition key value"() {
        given:
        def cond = Rules.equalsCondition("a", "b")
        def cond2 = Rules.equalsCondition("a", null)

        expect:
        cond.apply(States.state("a", "b"))
        !cond.apply(States.state("a", "c"))
        !cond.apply(States.state("c", "b"))

        !cond2.apply(States.state("a", "b"))
        cond2.apply(States.state("b", "c"))
        cond2.apply(States.state("a", null))
    }

    def "equals condition state"() {
        given:
        def cond = Rules.equalsCondition(States.state(a: 'b', c: 'd'))

        expect:
        !cond.apply(States.state('a', 'b'))
        cond.apply(States.state("a": "b", c: 'd'))
        cond.apply(States.state("a": "b", c: 'd', e: 'f'))
        !cond.apply(States.state("a": "d", c: 'd', e: 'f'))
    }

    def "matches condition"() {
        given:
        def cond = Rules.matchesCondition("abc", false, "[def]+", true)


        expect:
        cond.apply(States.state('abc', 'd'))
        cond.apply(States.state('abc', 'e'))
        cond.apply(States.state('abc', 'f'))
        cond.apply(States.state('abc', 'def'))
        !cond.apply(States.state('abc', 'a'))
    }

    def "not predicate"() {
        given:
        def base = Rules.equalsCondition("a", "b")
        def cond = Rules.not(base)

        expect:
        !cond.apply(States.state("a", "b"))
        cond.apply(States.state("a", "c"))
        cond.apply(States.state("c", "b"))
    }

    def "and predicate"() {
        given:
        def cond = Rules.and(Rules.equalsCondition("a", "b"), Rules.equalsCondition("c", "d"))

        expect:
        !cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "d"))
        cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))
    }

    def "and predicate iterable"() {
        given:
        def cond = Rules.and([Rules.equalsCondition("a", "b"), Rules.equalsCondition("c", "d")])

        expect:
        !cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "d"))
        cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))
    }

    def "and predicate varargs"() {
        given:
        def cond = Rules.and(
                Rules.equalsCondition("a", "b"),
                Rules.equalsCondition("c", "d"),
                Rules.equalsCondition("e", "f")
        )

        expect:
        !cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "d"))
        !cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f', g: 'h'))
    }

    def "or predicate"() {
        given:
        def cond = Rules.or(Rules.equalsCondition("a", "b"), Rules.equalsCondition("c", "d"))

        expect:
        !cond.apply(States.state("a", "z"))
        cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "z"))
        cond.apply(States.state("c", "d"))

        cond.apply(States.state(a: 'b', c: 'z'))
        cond.apply(States.state(a: 'z', c: 'd'))

        cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))

    }

    def "or predicate iterable"() {
        given:
        def cond = Rules.or([Rules.equalsCondition("a", "b"), Rules.equalsCondition("c", "d")])

        expect:
        !cond.apply(States.state("a", "z"))
        cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "z"))
        cond.apply(States.state("c", "d"))

        cond.apply(States.state(a: 'b', c: 'z'))
        cond.apply(States.state(a: 'z', c: 'd'))

        cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))
    }

    def "or predicate varargs"() {
        given:
        def cond = Rules.or(
                Rules.equalsCondition("a", "b"),
                Rules.equalsCondition("c", "d"),
                Rules.equalsCondition('e', 'f')
        )

        expect:
        !cond.apply(States.state("a", "z"))
        cond.apply(States.state("a", "b"))
        !cond.apply(States.state("c", "z"))
        cond.apply(States.state("c", "d"))
        !cond.apply(States.state("e", "z"))
        cond.apply(States.state("e", "f"))

        cond.apply(States.state(a: 'b', c: 'z'))
        cond.apply(States.state(a: 'z', c: 'd'))

        cond.apply(States.state(a: 'b', c: 'd'))
        cond.apply(States.state(a: 'b', c: 'd', e: 'f'))
    }
}
