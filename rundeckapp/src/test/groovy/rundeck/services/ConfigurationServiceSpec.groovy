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

package rundeck.services

import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class ConfigurationServiceSpec extends Specification implements ServiceUnitTest<ConfigurationService> {

    void "executionMode active config"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.executionMode = 'active'
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        service.executionModeActive
    }

    void "executionMode passive config"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.executionMode = 'passive'
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        !service.executionModeActive
    }

    void "executionMode remains passive after config reload"() {
        when:
            grailsApplication.config.clear()
            grailsApplication.config.rundeck.executionMode = beginMode
            service.setAppConfig(grailsApplication.config.rundeck)
        then:
            service.executionModeActive == expected
        when:
            grailsApplication.config.rundeck.executionMode = changedMode
            service.setAppConfig(grailsApplication.config.rundeck)
        then:
            service.executionModeActive == expected
        where:
            beginMode | changedMode | expected
            'passive' | 'active'    | false
            'active'  | 'passive'   | true
    }

    void "get string present"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = 'avalue'
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        'avalue' == service.getString('something.value', 'blah')
    }

    void "get string missing"() {
        when:
        grailsApplication.config.clear()
        then:
        'blah' == service.getString('something.value', 'blah')
    }

    void "get integer"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getInteger('something.value', defval)

        where:
        confVal | expval | defval
        null    | 1      | 1
        12      | 12     | 1
        12L     | 12     | 1
        '12'    | 12     | 1
        '3'     | 3      | 1
    }

    void "get long"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getLong('something.value', defval)

        where:
        confVal | expval | defval
        null    | 1L     | 1L
        12      | 12L    | 1L
        12L     | 12L    | 1L
        '12'    | 12L    | 1L
        '3'     | 3L     | 1L
    }


    void "get boolean present"(testval, resultval) {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = testval
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        resultval == service.getBoolean('something.value', false)

        where:
        testval | resultval
        'true'  | true
        true    | true
        'false' | false
        false   | false
    }

    void "get boolean missing"() {
        when:
        grailsApplication.config.clear()
        then:
        resultval == service.getBoolean('something.value', defval)

        where:
        defval | resultval
        true   | true
        false  | false
    }

    void "set boolean"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = ''
        service.setAppConfig(grailsApplication.config.rundeck)
        when:
        service.setBoolean('something.value', tval)
        then:
        service.getBoolean('something.value', false) == tval
        grailsApplication.config.rundeck.something.value == tval

        where:
        tval  | _
        true  | _
        false | _
    }

    void "get object"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.mail.complexConfig = [prop1:[sub1:"sub val"]]
        service.setAppConfig(grailsApplication.config.rundeck)

        when:
        def val = service.getConfig("mail.complexConfig")

        then:
        val.prop1.sub1 == "sub val"

    }

    void "get deprecated"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.feature.'*'.enabled = false
        grailsApplication.config.rundeck.feature.'option-values-plugin'.enabled = true
        service.setAppConfig(grailsApplication.config.rundeck)

        when:
        def val = service.getBoolean(prop, false)

        then:
        val == expected

        where:
        prop | expected
        "feature.optionValuesPlugin.enabled" | true
        "feature.enableAll" | false
    }
}
