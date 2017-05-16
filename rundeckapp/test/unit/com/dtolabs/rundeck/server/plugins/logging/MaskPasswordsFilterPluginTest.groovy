/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import spock.lang.Specification

/**
 * @author greg
 * @since 5/16/17
 */
class MaskPasswordsFilterPluginTest extends Specification {
    def "mask values"() {
        given:
        def plugin = new MaskPasswordsFilterPlugin()
        def data = DataContextUtils.context("option", passwords)
        def exposeddata = DataContextUtils.context("option", ["bogus": "fanmail"])
        exposeddata.merge(DataContextUtils.context("secureOption", passwords2))

        def loggingContext = Mock(PluginLoggingContext) {
            getPrivateDataContext() >> data
            getDataContext() >> exposeddata
        }
        def control = Mock(LogFilterPlugin.Control)
        def event = Mock(LogEventControl)
        when:
        plugin.init(loggingContext)
        plugin.handleEvent(control, event)

        then:
        1 * event.getMessage() >> logmessage
        1 * event.setMessage(outputmessage) >> event

        where:
        passwords                            | passwords2                           | logmessage | outputmessage
        [secret1: 'zamboni$$']               | [:]                                  | 'suspense' | 'suspense'
        [secret1: 'zamboni$$']               | [:]                                  |
                'mysql -u root -p \'zamboni$$\' '                                                |
                'mysql -u root -p \'*****\' '
        [:]                                  | [secret1: 'zamboni$$']               |
                'mysql -u root -p \'zamboni$$\' '                                                |
                'mysql -u root -p \'*****\' '
        [secret1: 'c4kq$*f9099{{j3]&jkdlf*$$^',
         secret2: '_\\asdf  \t3#*#*Djdkflj'] | [:]                                  |
                'mysql -u root -p \'c4kq$*f9099{{j3]&jkdlf*$$^\' && echo _\\asdf  ' +
                '\t3#*#*Djdkflj'                                                                 |
                'mysql -u root -p \'*****\' && echo *****'
        [:]                                  | [secret1: 'c4kq$*f9099{{j3]&jkdlf*$$^',
                                                secret2: '_\\asdf  \t3#*#*Djdkflj'] |
                'mysql -u root -p \'c4kq$*f9099{{j3]&jkdlf*$$^\' && echo _\\asdf  ' +
                '\t3#*#*Djdkflj'                                                                 |
                'mysql -u root -p \'*****\' && echo *****'


    }
}
