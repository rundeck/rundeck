package com.dtolabs.rundeck.app.api.jobs.options

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for RUN-4538: valuesUrl validation must accept templates with
 * ${...} variable references and reject plain invalid URLs.
 */
class OptionValidateRequestSpec extends Specification implements GrailsWebUnitTest {

    @Unroll
    def "valuesUrl with variable references is valid: #url"() {
        given:
        def opt = new OptionValidateRequest(name: 'test', enforced: false, valuesUrl: url)

        expect:
        opt.validate(['valuesUrl'])
        !opt.errors.hasFieldErrors('valuesUrl')

        where:
        url << [
            'https://${globals.s3_host}/${globals.s3_bucket}/options.json',
            'http://${globals.api_host}/api/${option.ENV.value}/data.json',
            '${globals.base_url}/path/values.json',
            'https://static.example.com/${globals.env}/options.json',
            'https://host.example.com/path?token=${globals.api_token}',
        ]
    }

    @Unroll
    def "valuesUrl without variables is validated as URL: '#url' -> valid=#expected"() {
        given:
        def opt = new OptionValidateRequest(name: 'test', enforced: false, valuesUrl: url)

        expect:
        opt.validate(['valuesUrl']) == expected
        opt.errors.hasFieldErrors('valuesUrl') == !expected

        where:
        url                              | expected
        'https://valid.example.com/opts' | true
        'http://host/path?k=v'           | true
        'notaurl'                        | false
        'foobar'                         | false
        'just text'                      | false
    }

    def "null and blank valuesUrl are valid"() {
        given:
        def opt = new OptionValidateRequest(name: 'test', enforced: false, valuesUrl: url)

        expect:
        opt.validate(['valuesUrl'])
        !opt.errors.hasFieldErrors('valuesUrl')

        where:
        url << [null, '']
    }
}
