package com.dtolabs.rundeck.app.api

import spock.lang.Specification
import spock.lang.Unroll

class ApiVersionsSpec extends Specification {
    def "parse versions"() {
        expect:
            ApiVersions.parseVersion("1").toString() == "1.0"
            ApiVersions.parseVersion("2").toString() == "2.0"
            ApiVersions.parseVersion("1.1").toString() == "1.1"
            ApiVersions.parseVersion("1.1.4").toString() == "1.1.4"
            ApiVersions.parseVersion("1.1.4.4") == null
            ApiVersions.parseVersion("0") == null
            ApiVersions.parseVersion("0123") == null
    }

    def "compare versions"() {
        expect:
            ApiVersions.parseVersion("1") < ApiVersions.parseVersion("2")
            ApiVersions.parseVersion("2") < ApiVersions.parseVersion("3")
            ApiVersions.parseVersion("1.1") < ApiVersions.parseVersion("2")
            ApiVersions.parseVersion("2.1") > ApiVersions.parseVersion("2")
            ApiVersions.parseVersion("2.1") == ApiVersions.parseVersion("2.1.0")
            ApiVersions.parseVersion("2") == ApiVersions.parseVersion("2.0")
            ApiVersions.parseVersion("2") == ApiVersions.parseVersion("2.0.0")

    }

    @Unroll
    def "is supported #value"() {
        given:
            ApiVersions versions = new ApiVersions(early, cur)
        expect:
            versions.isSupported(value) == result
        where:
            value      | early | cur  | result
            '1'        | '1'   | '12' | true
            '11'       | '1'   | '12' | true
            '12'       | '1'   | '12' | true
            '12.0'     | '1'   | '12' | true
            '12.0.0'   | '1'   | '12' | true
            '11.1'     | '1'   | '12' | true
            '11.99.99' | '1'   | '12' | true
            '12.0.1'   | '1'   | '12' | false
            '3'        | '1'   | '2'  | false
            '3'        | '4'   | '8'  | false
            '8.0.1'    | '4'   | '8'  | false
    }
}
