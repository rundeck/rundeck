/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(PasswordFieldsService)
class PasswordFieldsServiceSpec extends Specification {
    def "track with arg"() {
        given:
            def configs = [
                    [
                            type : 'asdf',
                            props: [prop1: 'avalue'],

                    ]
            ]
            def descs = [
                    DescriptionBuilder.builder().name('asdf')
                                      .property(
                                              PropertyBuilder.builder()
                                                             .string('prop1')
                                                             .renderingAsPassword().build()
                                      )
                                      .build()
            ]
        when:
            def count = service.track('arg1', configs, descs)

        then:
            count == 1
            service.tracking() == 1
            service.tracking('arg1') == 1
            configs[0].props.prop1 != 'avalue'
            configs[0].props.prop1.length() == 64
    }

    def "track and untrack with arg"() {
        given:
            def inputConfigs = [
                    [
                            type : 'asdf',
                            props: [prop1: 'avalue'],

                    ]
            ]
            def descs = [
                    DescriptionBuilder.builder().name('asdf')
                                      .property(
                                              PropertyBuilder.builder()
                                                             .string('prop1')
                                                             .renderingAsPassword().build()
                                      )
                                      .build()
            ]
            def count = service.track('arg1', inputConfigs, descs)
            def configs = [
                    [

                            type: 'asdf',
                            config: [
                                    type: 'asdf',
                                    props: new HashMap(inputConfigs[0].props)
                            ],
                            index : 0
                    ]
            ]

        when:
            def uncount = service.untrack('arg1', configs, descs)

        then:
            count == 1
            uncount == 1
            service.tracking() == 0
            service.tracking('arg1') == 0
            inputConfigs[0].props.prop1 != 'avalue'
            inputConfigs[0].props.prop1.length() == 64
            configs[0].config.props.prop1 == 'avalue'
    }

    def "track 2 with two args"() {
        given:
            def configs = [
                    [
                            type : 'asdf',
                            props: [prop1: 'avalue'],

                    ]
            ]
            def configs2 = [
                    [
                            type : 'asdf',
                            props: [prop1: 'bvalue'],

                    ]
            ]
            def descs = [
                    DescriptionBuilder.builder().name('asdf')
                                      .property(
                                              PropertyBuilder.builder()
                                                             .string('prop1')
                                                             .renderingAsPassword().build()
                                      )
                                      .build()
            ]
        when:
            def count = service.track('arg1', configs, descs)
            def count2 = service.track('arg2', configs2, descs)

        then:
            count == 1
            count2 == 1
            service.tracking() == 2
            service.tracking('arg1') == 1
            service.tracking('arg2') == 1
            configs[0].props.prop1 != 'avalue'
            configs[0].props.prop1.length() == 64
            configs2[0].props.prop1 != 'bvalue'
            configs2[0].props.prop1.length() == 64
    }
    def "track and untrack 2 with two args"() {
        given:
            def configs = [
                    [
                            type : 'asdf',
                            props: [prop1: 'avalue'],

                    ]
            ]
            def configs2 = [
                    [
                            type : 'asdf',
                            props: [prop1: 'bvalue'],

                    ]
            ]
            def descs = [
                    DescriptionBuilder.builder().name('asdf')
                                      .property(
                                              PropertyBuilder.builder()
                                                             .string('prop1')
                                                             .renderingAsPassword().build()
                                      )
                                      .build()
            ]

            def count = service.track('arg1', configs, descs)
            def count2 = service.track('arg2', configs2, descs)
            def unconfig1 = [
                    [
                            type: 'asdf',

                            config: [
                                    props: new HashMap(configs[0].props)
                            ],
                            index : 0
                    ]
            ]
        when:
            def uncount = service.untrack('arg1', unconfig1, descs)
        then:
            count == 1
            count2 == 1
            uncount == 1
            service.tracking() == 1
            service.tracking('arg1') == 0
            service.tracking('arg2') == 1

            configs[0].props.prop1 != 'avalue'
            configs[0].props.prop1.length() == 64
            unconfig1[0].config.props.prop1 == 'avalue'

            configs2[0].props.prop1 != 'bvalue'
            configs2[0].props.prop1.length() == 64
    }
}
