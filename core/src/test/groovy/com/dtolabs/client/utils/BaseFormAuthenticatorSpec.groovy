package com.dtolabs.client.utils

import org.apache.commons.httpclient.HttpMethod
import spock.lang.Specification

/**
 * Created by greg on 4/20/16.
 */
class BaseFormAuthenticatorSpec extends Specification {
    def "absolute url"() {
        given:
        URL url = new URL("http", host, 8080, "/some/path")
        when:
        def result = BaseFormAuthenticator.absoluteUrl(url, location)
        then:
        result == expected

        where:
        host    | location                 | expected
        "ahost" | '/monkey'                | 'http://ahost:8080/monkey'
        "bhost" | '/monkey'                | 'http://bhost:8080/monkey'
        "ahost" | 'http://ahost2:8080/abc' | 'http://ahost2:8080/abc'

    }
}
