/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck


import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertEquals

/**
 * Created by greg on 11/4/15.
 */

class OptionSpec extends Specification implements DomainUnitTest<Option> {
    @Unroll("path #path valid #value")
    def "validate default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        value == opt.validate(['defaultStoragePath'])

        where:
        path             | value
        'keys/blah'      | true
        'keys/blah/blah' | true
        'keys/'          | false
        'blah/toodles'   | false
    }

    def "from map default storage path"() {
        given:
        def opt = Option.fromMap('test', datamap)

        expect:
        value == opt.defaultStoragePath

        where:
        datamap                   | value
        [:]                       | null
        [storagePath: 'keys/abc'] | 'keys/abc'

    }

    def "to map default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        path == opt.toMap().storagePath

        where:
        path       | _
        'keys/abc' | _
        null       | _
    }

    def "to map option type config"() {
        given:
        def opt = new Option(
                name: 'bob',
                optionType: 'atype',
                configData: '{"jobOptionConfigEntries":[{"@class":"rundeck.options.JobOptionConfigPluginAttributes","pluginAttributes":{"key":"val","key2":"val2"}}]}'
        )

        when:
        def result = opt.toMap()
        then:
        result.type == 'atype'
        result.config == [key: 'val', key2: 'val2']

    }

    def "from map option type config"() {
        given:
        def opt = Option.fromMap('test', [type: 'atype', config: [a: 'b', c: 'd']])
        expect:
        opt.optionType == 'atype'
        opt.configMap == [a: 'b', c: 'd']

    }

    def "to map multivalue default select all"() {
        given:
        def opt = new Option(name: 'bob', multivalued: true, multivalueAllSelected: mvas)
        expect:
        res == opt.toMap().multivalueAllSelected

        where:
        mvas  | res
        true  | true
        false | null
    }

    def "from map multivalue all selected"() {
        given:
        def opt = Option.fromMap('test', [multivalued: true, multivalueAllSelected: mvas])
        expect:
        opt.multivalued
        opt.multivalueAllSelected == res

        where:
        mvas    | res
        true    | true
        "true"  | true
        false   | false
        "false" | false
        null    | false

    }

    void "testConstraints"() {

        when:
        def option = new Option(name: 'ABCdef-4._12390', defaultValue: '12',enforced: true)
        def validate = option.validate()
        if(!validate){
            option.errors.allErrors.each {println it}
        }
        then:
        assertEquals(true, validate)
        assertEquals(false, option.errors.hasErrors())
        assertEquals(false, option.errors.hasFieldErrors('name'))
    }

    void "testDelimiter"() {
        when:
        def opt1=new Option(name:'abc',multivalued:true,delimiter:',')
        then:
        assertEquals(',',opt1.delimiter)
        when:
        def opt2=new Option(name:'abc',multivalued:true,delimiter:" ")
        then:
        assertEquals(" ",opt2.delimiter)
    }

    void "testInvalidName"() {

        expect:
        assertInvalidName(new Option(name: 'abc def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc+def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc/def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc!@#$%^&*()def', defaultValue: '12',enforced: true))
    }

    private void assertInvalidName(Option option) {
        assertEquals(false, option.validate())
        assertEquals(true, option.errors.hasErrors())
        assertEquals(true, option.errors.hasFieldErrors('name'))
    }
}
