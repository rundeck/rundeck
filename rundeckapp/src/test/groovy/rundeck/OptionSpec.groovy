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
import org.rundeck.app.jobs.options.RemoteUrlAuthenticationType
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
                configData: '{"jobOptionConfigEntries":{"plugin-attributes":{"@class":"org.rundeck.app.jobs.options.JobOptionConfigPluginAttributes","pluginAttributes":{"key":"val","key2":"val2"}}}}'
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

    def "to map remote URL config"() {
        given:
        def opt = new Option(
                name: 'bob',
                optionType: 'text',
                valuesUrl: 'http://test.com',
                configData: '{"jobOptionConfigEntries":{"remote-url":{"@class":"org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl","authenticationType":"BASIC","username":"admin","passwordStoragePath":"keys/test"}}}'
        )

        when:
        def result = opt.toMap()
        then:
        result.type == 'text'
        result.configRemoteUrl.authenticationType == "BASIC"
        result.configRemoteUrl.username == "admin"
        result.configRemoteUrl.passwordStoragePath == "keys/test"

    }

    def "from map to remote URL config"() {
        given:
        def opt = Option.fromMap('test', [type: 'text', configRemoteUrl: [authenticationType: 'API_KEY', tokenStoragePath: 'keys/test', keyName: 'api-key']])
        expect:
        opt.optionType == 'text'
        opt.configData == '{"jobOptionConfigEntries":{"remote-url":{"@class":"org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl","authenticationType":"API_KEY","keyName":"api-key","tokenStoragePath":"keys/test"}}}'
        opt.configRemoteUrl !=null
        opt.configRemoteUrl.authenticationType == RemoteUrlAuthenticationType.API_KEY
        opt.configRemoteUrl.tokenStoragePath == 'keys/test'
        opt.configRemoteUrl.keyName == 'api-key'


    }
    
    def "toMap should order valueList if sortValues is true "() {

        given:"an option"
        def opt = new Option(
                name: 'bob',
                valuesList:'A,C,B',
                sortValues: true
        )

        when:"the options is being converted into a map"
        def result = opt.toMap()

        then:"values should be in order"
        result.sortValues==true
        result.values[0]=="A"
        result.values[1]=="B"
        result.values[2]=="C"
    }

    def "toMap should keep order in valueList if sortValues is false "() {

        given:"an option"
        def opt = new Option(
                name: 'bob',
                sortValues:false,
                valuesList:'A,C,B'
        )

        when:"the options is being converted into a map"
        def result = opt.toMap()

        then:"values should keep original order"
        result.values[0]=="A"
        result.values[1]=="C"
        result.values[2]=="B"
    }

    def "option fromMap should have sortValues value"() {

        given: "a map with option data"
        Map map = [enforcedvalues:'true', name:'option1', sortValues:true, required:'true', values:'A,C,B,D,E', valuesListDelimiter:',']
        String name = "test"

        when:"creating the option"
        def opt = Option.fromMap(name,map )

        then:"the option should have the value of sortValues"
        opt.sortValues == true
    }    
        
        
    def "from map to remote URL json filter"() {
        given:
        def opt = Option.fromMap('test', [type: 'text', configRemoteUrl: [jsonFilter:"\$.key"]])
        expect:
        opt.optionType == 'text'
        opt.configData == '{"jobOptionConfigEntries":{"remote-url":{"@class":"org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl","jsonFilter":"\$.key"}}}'
        opt.configRemoteUrl !=null
        opt.configRemoteUrl.jsonFilter == "\$.key"
    }

    def "fromMap merges top-level remoteUrlAuthenticationType into configRemoteUrl when not already set"() {
        given: "a map with remoteUrlAuthenticationType at the top level and configRemoteUrl without authenticationType"
        def opt = Option.fromMap('test', [
                type                      : 'text',
                remoteUrlAuthenticationType: 'BASIC',
                configRemoteUrl           : [username: 'admin', passwordStoragePath: 'keys/pass']
        ])

        expect: "authenticationType from top-level field is merged into configRemoteUrl"
        opt.configRemoteUrl != null
        opt.configRemoteUrl.authenticationType == RemoteUrlAuthenticationType.BASIC
        opt.configRemoteUrl.username == 'admin'
        opt.configRemoteUrl.passwordStoragePath == 'keys/pass'
    }

    def "fromMap does not override authenticationType already present in configRemoteUrl"() {
        given: "a map where both remoteUrlAuthenticationType and configRemoteUrl.authenticationType are set"
        def opt = Option.fromMap('test', [
                type                      : 'text',
                remoteUrlAuthenticationType: 'BASIC',
                configRemoteUrl           : [authenticationType: 'API_KEY', tokenStoragePath: 'keys/token']
        ])

        expect: "the existing authenticationType in configRemoteUrl is preserved"
        opt.configRemoteUrl != null
        opt.configRemoteUrl.authenticationType == RemoteUrlAuthenticationType.API_KEY
        opt.configRemoteUrl.tokenStoragePath == 'keys/token'
    }

    def "fromMap ignores empty remoteUrlAuthenticationType"() {
        given: "a map with an empty remoteUrlAuthenticationType"
        def opt = Option.fromMap('test', [
                type                      : 'text',
                remoteUrlAuthenticationType: '',
                configRemoteUrl           : [tokenStoragePath: 'keys/token']
        ])

        expect: "no authenticationType is set on configRemoteUrl"
        opt.configRemoteUrl != null
        opt.configRemoteUrl.authenticationType == null
        opt.configRemoteUrl.tokenStoragePath == 'keys/token'
    }

    def "toMap includes remoteUrlAuthenticationType when configRemoteUrl has authenticationType"() {
        given: "an option loaded from a map with authenticationType"
        def opt = Option.fromMap('test', [
                type                      : 'text',
                valuesUrl                 : 'http://example.com/values',
                remoteUrlAuthenticationType: 'BEARER_TOKEN',
                configRemoteUrl           : [tokenStoragePath: 'keys/token']
        ])

        when:
        def result = opt.toMap()

        then: "toMap re-emits remoteUrlAuthenticationType at the top level"
        result.remoteUrlAuthenticationType == 'BEARER_TOKEN'
        result.configRemoteUrl != null
    }

    def "toMap round-trips remoteUrlAuthenticationType for all auth types"() {
        given:
        def opt = Option.fromMap('test', [
                type                      : 'text',
                valuesUrl                 : 'http://example.com/values',
                remoteUrlAuthenticationType: authType,
                configRemoteUrl           : [tokenStoragePath: 'keys/token']
        ])

        when:
        def result = opt.toMap()

        then:
        result.remoteUrlAuthenticationType == authType

        where:
        authType << ['BEARER_TOKEN', 'BASIC', 'API_KEY']
    }
}
