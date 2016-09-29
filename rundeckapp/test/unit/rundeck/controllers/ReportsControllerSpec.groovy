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

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExecQuery
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.ExecReport
import rundeck.Execution
import rundeck.User
import rundeck.services.FrameworkService
import rundeck.services.ReportService
import rundeck.services.UserService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 9/22/16.
 */
@Mock([ExecReport, Execution, User])
@TestFor(ReportsController)
class ReportsControllerSpec extends Specification {
    @Unroll
    def "events query date binding format #dateFilter"() {
        given:

        controller.frameworkService = Mock(FrameworkService) {
            _ * authorizeProjectResourceAll(*_) >> true
        }
        controller.userService = Mock(UserService) {
            findOrCreateUser(*_) >> new User()
        }
        controller.reportService = Mock(ReportService)


        when:
        params.doendafterFilter = 'true'
        params.endafterFilter = dateFilter
        def result = controller.eventsAjax()


        then:
        response.status == 200
        response.json.reports == []
        1 * controller.reportService.getExecutionReports({ ExecQuery query ->
            query.doendafterFilter && query.endafterFilter
                                                         }, true
        ) >> [reports: []]
        1 * controller.reportService.finishquery(_, _, _) >> { args -> args[2] }

        where:
        dateFilter                      | _
        '1999-01-01T01:23:45Z'          | _
        '1999-01-01 01:23:45.123'       | _
        '1999-01-01T13:23:45Z'          | _
        '1999-01-01T13:23:45-08'        | _
        '1999-01-01T13:23:45-0800'      | _
        '1999-01-01T13:23:45-08:00'     | _
        '1999-01-01T13:23:45.123Z'      | _
        '1999-01-01T13:23:45.123-08'    | _
        '1999-01-01T13:23:45.123-0800'  | _
        '1999-01-01T13:23:45.123-08:00' | _
    }
}
