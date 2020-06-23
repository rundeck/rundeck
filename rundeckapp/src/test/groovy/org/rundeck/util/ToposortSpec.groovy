package org.rundeck.util

import spock.lang.Specification

class ToposortSpec extends Specification {
    def "topo sort map"() {
        expect:
            result == Toposort.toposort(nodes, edges, reverseEdges).result
        where:
            nodes           | edges                | reverseEdges         | result
            ['a', 'b', 'c'] | [:]                  | [:]                  | ['a', 'b', 'c']
            ['a', 'b', 'c'] | [b: ['a']]           | [:]                  | ['b', 'a', 'c']
            ['a', 'b', 'c'] | [:]                  | [a: ['b']]           | ['b', 'a', 'c']
            ['a', 'b', 'c'] | [b: ['a'], c: ['b']] | [:]                  | ['c', 'b', 'a']
            ['a', 'b', 'c'] | [:]                  | [a: ['b'], b: ['c']] | ['c', 'b', 'a']
    }

    def "topo sort functional"() {
        expect:
            result == Toposort.toposort(nodes, edges, reverseEdges).result
        where:
            nodes           | edges                            | reverseEdges                     | result
            ['a', 'b', 'c'] | { [] }                           | { [] }                           | ['a', 'b', 'c']
            ['a', 'b', 'c'] | { [b: ['a']].get(it) }           | { [] }                           | ['b', 'a', 'c']
            ['a', 'b', 'c'] | { [] }                           | { [a: ['b']].get(it) }           | ['b', 'a', 'c']
            ['a', 'b', 'c'] | { [b: ['a'], c: ['b']].get(it) } | { [] }                           | ['c', 'b', 'a']
            ['a', 'b', 'c'] | { [] }                           | { [a: ['b'], b: ['c']].get(it) } | ['c', 'b', 'a']
    }

    def "topo sort cyclical"() {

        expect:
            def sorted = Toposort.toposort(nodes, edges, reverseEdges)
            !sorted.result
            sorted.cycle
        where:
            nodes           | edges                          | reverseEdges
            ['a', 'b', 'c'] | [b: ['a'], a: ['b']]           | [:]
            ['a', 'b', 'c'] | [:]                            | [b: ['a'], a: ['b']]
            ['a', 'b', 'c'] | [b: ['a'], a: ['c'], c: ['b']] | [:]
            ['a', 'b', 'c'] | [:]                            | [b: ['a'], a: ['c'], c: ['b']]
    }
}
