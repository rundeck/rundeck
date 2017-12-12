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

package com.dtolabs.rundeck.app.support

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import rundeck.controllers.MenuController
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 10/20/16.
 */
@TestFor(MenuController)
class ExecQuerySpec extends Specification {
    @Unroll
    def "recentFilter valid values #recentFilter"() {
        given:
        def item = new ExecQuery()
        item.recentFilter = recentFilter

        when:
        item.validate()

        then:
        !item.hasErrors()

        where:
        recentFilter | _
        '1h'         | _
        '1123123h'   | _
        '1d'         | _
        '1231231d'   | _
        '1w'         | _
        '4145141w'   | _
        '1m'         | _
        '345451m'    | _
        '1y'         | _
        '-'          | _
    }

    @Unroll
    def "recentFilter invalid values #recentFilter"() {
        given:
        def item = new ExecQuery()
        item.recentFilter = recentFilter

        when:
        item.validate()

        then:
        item.hasErrors()
        item.errors.hasFieldErrors('recentFilter')

        where:
        recentFilter    | _
        '1'             | _
        'h'             | _
        ' 1d'           | _
        'd1231231d'     | _
        'a<asdf'        | _
        'qq54q51y'      | _
        'wrong &lt;' | _
    }
}
