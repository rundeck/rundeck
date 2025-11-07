package com.dtolabs.rundeck.app.mail

import spock.lang.Specification

class DynamicMailSenderSpec extends Specification {
    def "flatten props"() {
        expect:
            DynamicMailSender.flattenMapToProperties(input) == expected
        where:
            input                           | expected
            [a: 'b']                        | [a: 'b']
            [a: 'b', c: 'd']                | [a: 'b', c: 'd']
            [a: [c: 'd']]                   | ['a.c': 'd']
            [a: [c: 'd'], b: [e: [f: 'g']]] | ['a.c': 'd', 'b.e.f': 'g']
    }
}