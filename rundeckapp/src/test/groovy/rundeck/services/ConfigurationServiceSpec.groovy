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
import org.rundeck.app.config.SysConfigProp
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

    void "executionMode remains the same after config reload"() {
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
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = 'avalue'
        service.setAppConfig(grailsApplication.config.rundeck)
        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        def val1=service.getString('something.value', 'blah')
        then:
        'avalue' == val1
        when:
        def val2=service.getString(prop, 'blah')
        then:
        'avalue' == val2
    }

    void "get string missing"() {
        given:

        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        def val1=service.getString('something.value', 'blah')
        then:
        'blah' ==val1
        when:
        def val2=service.getString(prop, 'blah')

        then:
        'blah'==val2
    }

    void "get integer"() {
        given:

        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getInteger('something.value', defval)
        expval == service.getInteger(prop, defval)

        where:
        confVal | expval | defval
        null    | 1      | 1
        12      | 12     | 1
        12L     | 12     | 1
        '12'    | 12     | 1
        '3'     | 3      | 1
    }

    void "get long"() {
        given:

        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getLong('something.value', defval)
        expval == service.getLong(prop, defval)

        where:
        confVal | expval | defval
        null    | 1L     | 1L
        12      | 12L    | 1L
        12L     | 12L    | 1L
        '12'    | 12L    | 1L
        '3'     | 3L     | 1L
    }

    void "get time duration"() {
        given:

        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getTimeDuration('something.value', defval)
        expval == service.getTimeDuration(prop, defval)

        where:
        confVal | expval | defval
        null  | 30L   | '30s'
        ''    | 30L   | '30s'
        '12s' | 12L   | '30s'
        '1m'  | 60L   | '30s'
        '1h'  | 3600L | '30s'
        '12'  | 12L   | '30s'
    }

    void "get file size"() {
        given:

        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        service.setAppConfig(grailsApplication.config.rundeck)
        then:
        expval == service.getFileSize('something.value', defval)
        expval == service.getFileSize(prop, defval)

        where:
        confVal | expval   | defval
        null    | 10L      | 10L
        ''      | 10L      | 10L
        '12b'   | 12L      | 10L
        '12k'   | 12288L   | 10L
        '1m'    | 1048576L | 10L
    }


    void "get boolean present"(testval, resultval) {
        given:
        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = testval
        service.setAppConfig(grailsApplication.config.rundeck)
        when:
        def test1 = service.getBoolean('something.value', false)
        def test2 = service.getBoolean(prop, false)
        then:
        resultval==test1
        resultval==test2

        where:
        testval | resultval
        'true'  | true
        true    | true
        'false' | false
        false   | false
    }

    void "get boolean missing"() {
        given:
        def prop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        grailsApplication.config.clear()
        then:
        resultval == service.getBoolean('something.value', defval)
        resultval == service.getBoolean(prop, defval)

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
        def sysconfigprop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> 'something.value'
        }
        when:
        service.setBoolean('something.value', tval)
        then:
        service.getBoolean('something.value', false) == tval
        service.getBoolean(sysconfigprop, false) == tval
        grailsApplication.config.rundeck.something.value == tval

        where:
        tval  | _
        true  | _
        false | _
    }


    void "get deprecated"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.feature.'*'.enabled = false
        grailsApplication.config.rundeck.feature.'option-values-plugin'.enabled = true
        service.setAppConfig(grailsApplication.config.rundeck)
        def sysconfigprop=Mock(SysConfigProp){
            1 * subKey(ConfigurationService.RUNDECK_PREFIX) >> prop
        }
        when:
        def val = service.getBoolean(prop, false)
        def val2 = service.getBoolean(sysconfigprop, false)

        then:
        val == expected
        val2 == expected

        where:
        prop                                 | expected
        "feature.optionValuesPlugin.enabled" | true
        "feature.enableAll"                  | false
    }
}
