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

import spock.lang.Specification

/**
 * Created by greg on 1/6/16.
 */
class ExecutionQuerySpec extends Specification {
    def "parse relative date valid format"() {
        expect:
        null != ExecutionQuery.parseRelativeDate("1${unit}")

        where:
        unit | _
        "h"  | _
        "d"  | _
        "w"  | _
        "m"  | _
        "y"  | _
        "n"  | _
        "s"  | _
    }

    def "parse relative date invalid format"() {
        expect:
        null == ExecutionQuery.parseRelativeDate(fmt)

        where:
        fmt   | _
        "h"   | _
        "d1"  | _
        "1x"  | _
        " 1d" | _
        "1d " | _
        "1"   | _
        "-1d" | _
    }

    static Date mkdate(int y, int m, int d, int h, int n, int s) {
        Calendar instance = GregorianCalendar.getInstance()
        instance.setTimeInMillis(0)
        instance.set(y, m, d, h, n, s)
        instance.set(Calendar.MILLISECOND,0)
        instance.getTime()
    }

    def "parse relative date"() {
        given:
        Date date = mkdate(2015, 0, 2, 12, 25, 20)

        expect:
        0 == edate.compareTo(ExecutionQuery.parseRelativeDate(fmt, date))

        where:
        fmt   | edate
        "0d"  | mkdate(2015, 0, 2, 12, 25, 20)
        "1d"  | mkdate(2015, 0, 1, 12, 25, 20)
        "2d"  | mkdate(2014, 11, 31, 12, 25, 20)
        "10s" | mkdate(2015, 0, 2, 12, 25, 10)
        "10n" | mkdate(2015, 0, 2, 12, 15, 20)
        "1h"  | mkdate(2015, 0, 2, 11, 25, 20)
        "1w"  | mkdate(2014, 11, 26, 12, 25, 20)
        "1m"  | mkdate(2014, 11, 2, 12, 25, 20)
        "1y"  | mkdate(2014, 0, 2, 12, 25, 20)
    }

    // ==================== escapeLikePattern Tests ====================

    def "escapeLikePattern escapes percent wildcard"() {
        expect:
        ExecutionQuery.escapeLikePattern("test%value") == "test\\%value"
        ExecutionQuery.escapeLikePattern("%") == "\\%"
        ExecutionQuery.escapeLikePattern("%%") == "\\%\\%"
        ExecutionQuery.escapeLikePattern("start%middle%end") == "start\\%middle\\%end"
    }

    def "escapeLikePattern escapes underscore wildcard"() {
        expect:
        ExecutionQuery.escapeLikePattern("test_value") == "test\\_value"
        ExecutionQuery.escapeLikePattern("_") == "\\_"
        ExecutionQuery.escapeLikePattern("__") == "\\_\\_"
        ExecutionQuery.escapeLikePattern("a_b_c") == "a\\_b\\_c"
    }

    def "escapeLikePattern escapes backslash"() {
        expect:
        ExecutionQuery.escapeLikePattern("test\\value") == "test\\\\value"
        ExecutionQuery.escapeLikePattern("\\") == "\\\\"
        ExecutionQuery.escapeLikePattern("\\\\") == "\\\\\\\\"
    }

    def "escapeLikePattern escapes all special chars together"() {
        expect:
        ExecutionQuery.escapeLikePattern("test%_\\value") == "test\\%\\_\\\\value"
        ExecutionQuery.escapeLikePattern("%_\\") == "\\%\\_\\\\"
    }

    def "escapeLikePattern handles null"() {
        expect:
        ExecutionQuery.escapeLikePattern(null) == null
    }

    def "escapeLikePattern returns unchanged for safe input"() {
        expect:
        ExecutionQuery.escapeLikePattern("simple") == "simple"
        ExecutionQuery.escapeLikePattern("node1") == "node1"
        ExecutionQuery.escapeLikePattern("test-value") == "test-value"
        ExecutionQuery.escapeLikePattern("user@example.com") == "user@example.com"
        ExecutionQuery.escapeLikePattern("") == ""
    }
}
