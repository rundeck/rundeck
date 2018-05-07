/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.app.internal.workflow

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit

import static com.dtolabs.rundeck.app.internal.workflow.PeriodicFileChecker.Behavior.AND
import static com.dtolabs.rundeck.app.internal.workflow.PeriodicFileChecker.Behavior.OR

/**
 * @author greg
 * @since 9/25/17
 */
class PeriodicFileCheckerSpec extends Specification {

    File testfile

    def setup() {
        testfile = Files.createTempFile("PeriodicFileCheckerSpec-test", ".file").toFile()
        testfile.deleteOnExit()
    }

    def cleanup() {
        testfile?.delete()
    }

    static interface CheckerAction {
        void check(long diff, long timediff)
    }

    @Unroll
    def "triggerCheck"() {
        given:
        def now = Instant.now()
        def allTimes = [now] + times.collect { new Date(it + Date.from(now).time).toInstant() }
        Clock clock1 = Mock(Clock) {
            instant() >>> allTimes
        }
        def checkerAction = Mock(CheckerAction)
        def checker = new PeriodicFileChecker(
                clock: clock1,
                logfile: testfile,
                periodUnit: TimeUnit.SECONDS,
                period: tPer,
                periodThreshold: tThresh,
                sizeThreshold: sThresh,
                sizeIncrement: sInc,
                action: checkerAction.&check,
                thresholdBehavior: behavior
        )
        when:
        def first = checker.triggerCheck()
//        println("check: $checker")
        def rest = (0..<times.size()).collect { inc ->
            if (sizes[inc]) {
                def bytes = new byte[sizes[inc]]
//                (0..<sizes[inc]).each { bytes[it] = 0 }
                testfile.bytes = bytes
            }
            def result = checker.triggerCheck()
//            println("check[$inc]: $checker")
            result
        }

        then:
        [first] + rest == expects
        rest.size() == Math.max(sizes.size(), times.size())

        where:
        sizes          | times        | tPer | tThresh | sThresh | sInc | behavior | expects
        //baseline
        [0]            | [0]          | 0    | 0       | 0       | 0    | AND      | [false, false]
        //minimum time threshold
        [0]            | [0]          | 1    | 1       | 0       | 0    | AND      | [false, false]
        //before threshold
        [0]            | [999]        | 1    | 1       | 0       | 0    | AND      | [false, false]
        //meet threshold
        [0]            | [1000]       | 1    | 1       | 0       | 0    | AND      | [false, true]
        [0]            | [1000]       | 1    | 1       | 0       | 0    | OR       | [false, true]
        //time period
        [0]            | [0]          | 1    | 0       | 0       | 0    | AND      | [false, false]
        [0]            | [999]        | 1    | 0       | 0       | 0    | AND      | [false, false]
        [0]            | [1000]       | 1    | 0       | 0       | 0    | AND      | [false, true]
        [0]            | [1000, 1999] | 1    | 0       | 0       | 0    | AND      | [false, true, false]
        [0]            | [1000, 2000] | 1    | 0       | 0       | 0    | AND      | [false, true, true]
        //period with time threshold
        [0]            | [1999]       | 1    | 2       | 0       | 0    | AND      | [false, false]
        [0]            | [2000]       | 1    | 2       | 0       | 0    | AND      | [false, true]
        [0]            | [2000, 2999] | 1    | 2       | 0       | 0    | AND      | [false, true, false]
        [0]            | [2000, 3000] | 1    | 2       | 0       | 0    | AND      | [false, true, true]
        //size check
        [0]            | [0]          | 0    | 0       | 100     | 1    | AND      | [false, false]
        [99]           | [0]          | 0    | 0       | 100     | 1    | AND      | [false, false]
        [100]          | [0]          | 0    | 0       | 100     | 1    | AND      | [false, true]
        [100]          | [0]          | 0    | 0       | 100     | 1    | OR       | [false, true]
        //size increment
        [0]            | [0]          | 0    | 0       | 0       | 100  | AND      | [false, false]
        [99]           | [0]          | 0    | 0       | 0       | 100  | AND      | [false, false]
        [100]          | [0]          | 0    | 0       | 0       | 100  | AND      | [false, true]
        [99, 199]      | [0, 1]       | 0    | 0       | 0       | 100  | AND      | [false, false, true]
        [99, 299]      | [0, 1]       | 0    | 0       | 0       | 100  | AND      | [false, false, true]
        [99, 299, 300] | [0, 1, 2]    | 0    | 0       | 0       | 100  | AND      | [false, false, true, false]
        [99, 299, 398] | [0, 1, 2]    | 0    | 0       | 0       | 100  | AND      | [false, false, true, false]
        [99, 299, 399] | [0, 1, 2]    | 0    | 0       | 0       | 100  | AND      | [false, false, true, true]
        [100, 199]     | [0, 1]       | 0    | 0       | 0       | 100  | AND      | [false, true, false]
        [100, 200]     | [0, 1]       | 0    | 0       | 0       | 100  | AND      | [false, true, true]
        //both size and time threshold
        [0]            | [0]          | 1    | 1       | 100     | 1    | AND      | [false, false]
        [0]            | [999]        | 1    | 1       | 100     | 1    | AND      | [false, false]
        [99]           | [0]          | 1    | 1       | 100     | 1    | AND      | [false, false]
        [100]          | [0]          | 1    | 1       | 100     | 1    | AND      | [false, false]
        [0]            | [1000]       | 1    | 1       | 100     | 1    | AND      | [false, false]
        [99]           | [1000]       | 1    | 1       | 100     | 1    | AND      | [false, false]
        [100]          | [999]        | 1    | 1       | 100     | 1    | AND      | [false, false]
        [100]          | [1000]       | 1    | 1       | 100     | 1    | AND      | [false, true]
        //both size and time threshold with OR
        [0]            | [0]          | 1    | 1       | 100     | 1    | OR       | [false, false]
        [0]            | [999]        | 1    | 1       | 100     | 1    | OR       | [false, false]
        [99]           | [0]          | 1    | 1       | 100     | 1    | OR       | [false, false]
        [100]          | [0]          | 1    | 1       | 100     | 1    | OR       | [false, true]
        [0]            | [1000]       | 1    | 1       | 100     | 1    | OR       | [false, true]
        [99]           | [1000]       | 1    | 1       | 100     | 1    | OR       | [false, true]
        [100]          | [999]        | 1    | 1       | 100     | 1    | OR       | [false, true]
        [100]          | [1000]       | 1    | 1       | 100     | 1    | OR       | [false, true]

    }
}
