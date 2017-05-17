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
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/16/17
 */
class MaskPasswordsFilterPluginSpec extends Specification {
    @Unroll
    def "mask values"() {
        given:
        def plugin = new MaskPasswordsFilterPlugin()
        plugin.replacement = '*****'
        def privateData = DataContextUtils.context("option", privatedata)
        def exposeddata = DataContextUtils.context("option", ["bogus": "fanmail"])
        exposeddata.merge(DataContextUtils.context("secureOption", secureoption))

        def loggingContext = Mock(PluginLoggingContext) {
            getPrivateDataContext() >> privateData
            getDataContext() >> exposeddata
        }
        def event = Mock(LogEventControl)
        when:
        plugin.init(loggingContext)
        plugin.handleEvent(loggingContext, event)

        then:
        if (privatedata || secureoption) {
            1 * event.getEventType() >> 'log'
            1 * event.getMessage() >> logmessage
            1 * event.setMessage(outputmessage) >> event
        } else {
            0 * event._(*_)
        }

        where:
        privatedata                          | secureoption                         | logmessage | outputmessage
        [:]                                  | [:]                                  | 'suspense' | 'suspense'
        [secret1: 'zamboni$$']               | [:]                                  |
                'mysql -u root -p \'zamboni$$\' '                                                |
                'mysql -u root -p ***** '

        //contains quotes, the escaped variants are replaced
        [secret1: 'djasdf\'3"dlkja']         | [:]                                  |
                'mysql -u root -p \'djasdf\\\'3"dlkja\' ' |
                'mysql -u root -p ***** '
        [secret1: 'zamboni$$']               | [:]                                  |
                'mysql -u root -p "zamboni\\$\\$" ' |
                'mysql -u root -p ***** '
        [:]                                  | [secret1: 'zamboni$$']               |
                'mysql -u root -p \'zamboni$$\' '                                                |
                'mysql -u root -p ***** '
        [secret1: 'c4kq$*f9099{{j3]&jkdlf*$$^',
         secret2: '_\\asdf  \t3#*#*Djdkflj'] | [:]                                  |
                'mysql -u root -p \'c4kq$*f9099{{j3]&jkdlf*$$^\'' +
                ' && echo _\\asdf  \t3#*#*Djdkflj'                                               |
                'mysql -u root -p ***** ' +
                '&& echo *****'
        [secret2: '_\\asdf  \t3#*#*Djdkflj'] | [:]                                  |
                'echo \'_\\\\asdf  \\t3#*#*Djdkflj\'' |
                'echo *****'
        [:]                                  | [secret1: 'c4kq$*f9099{{j3]&jkdlf*$$^',
                                                secret2: '_\\asdf  \t3#*#*Djdkflj'] |
                'mysql -u root -p \'c4kq$*f9099{{j3]&jkdlf*$$^\' ' +
                '&& echo _\\asdf  \t3#*#*Djdkflj'                                                |
                'mysql -u root -p ***** ' +
                '&& echo *****'
        [secret1: '9090asfj0sadj0f93jfjJFKdfj',
         secret2: 'aljkdfjld']               | [secret1: 'c4kq$*f9099{{j3]&jkdlf*$$^',
                                                secret2: '_\\asdf  \t3#*#*Djdkflj'] |
                'mysql -u root -p \'c4kq$*f9099{{j3]&jkdlf*$$^\' ' +
                '&& mysql -u root -p \'9090asfj0sadj0f93jfjJFKdfj\' ' +
                '&& mysql -u root -p \'aljkdfjld\' ' +
                '&& echo _\\asdf  \t3#*#*Djdkflj'                                                |
                'mysql -u root -p ***** ' +
                '&& mysql -u root -p ***** ' +
                '&& mysql -u root -p ***** ' +
                '&& echo *****'


    }
}
