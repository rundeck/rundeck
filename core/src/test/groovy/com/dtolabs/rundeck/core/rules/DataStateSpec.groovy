package com.dtolabs.rundeck.core.rules

import spock.lang.Specification

/**
 * Created by greg on 4/29/16.
 */
class DataStateSpec extends Specification {
    def "update state with map"() {
        given:
        DataState state = new DataState([:])

        when:
        state.updateState([a: 'b', c: 'd'])

        then:
        state.state == [a: 'b', c: 'd']

    }

    def "has state"() {
        given:
        DataState state = new DataState([a: 'b', c: 'd'])


        expect:
        state.hasState('a', 'b')
        !state.hasState('a', 'd')
        state.hasState('c', 'd')
        !state.hasState('c', 'b')
    }

    def "update state with map null value removes key"() {
        given:
        DataState state = new DataState([a: 'b', c: 'd'])

        when:
        state.updateState([a: null])


        then:
        state.state == [c: 'd']

    }

    def "update state key value"() {
        given:
        DataState state = new DataState([c: 'd'])

        when:
        state.updateState('a', 'b')


        then:
        state.state == [a: 'b', c: 'd']
    }


    def "update state null value removes key"() {
        given:
        DataState state = new DataState([c: 'd'])

        when:
        state.updateState('c', null)


        then:
        state.state == [:]
    }

    def "update state other state"() {
        given:
        DataState state = new DataState([c: 'd'])
        DataState newstate = new DataState([a: 'b'])


        when:
        state.updateState(newstate)


        then:
        state.state == [a: 'b', c: 'd']
    }
}
