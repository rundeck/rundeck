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

//= require momentutil
//= require vendor/knockout.min
//= require executionStateKO
//= require util/testing

jQuery(function () {
    "use strict";

    function stepFixture(opts) {
        return {
            parameterizedStep: ko.observable(!!opts.parameterizedStep),
            executionState: ko.observable(opts.executionState || 'SUCCEEDED'),
            startTime: ko.observable(opts.startTime || null),
            endTime: ko.observable(opts.endTime || null),
            updateTime: ko.observable(opts.updateTime || null),
            duration: ko.observable(opts.duration != null ? opts.duration : -1)
        };
    }

    new TestHarness("executionStateKO.test.js", {
        computeNodeDurationUsesWallClockNotStepSum: function () {
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 370
                }),
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 358
                })
            ];
            var stepSum = 370 + 358;
            var ms = RDNode.computeNodeDurationMs(-1, steps);
            this.assert('wall-clock is one second', ms === 1000);
            this.assert('wall-clock is less than summed step durations', ms < stepSum);
            this.assert('would have exceeded if summed', stepSum > ms);
        },
        computeNodeDurationClampsInflatedServerValue: function () {
            var steps = [
                stepFixture({
                    executionState: 'SUCCEEDED',
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 1000
                }),
                stepFixture({
                    executionState: 'NOT_STARTED',
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:43Z',
                    updateTime: '2014-09-05T21:28:43Z',
                    duration: 39000
                })
            ];
            this.assert(RDNode.computeNodeDurationMs(39000, steps) === 1000);
        },
        computeNodeDurationDerivesWhenServerReportsZero: function () {
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:06Z',
                    duration: 1000
                }),
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:06Z',
                    duration: 1000
                })
            ];
            this.assert(RDNode.computeNodeDurationMs(0, steps) === 2000);
        },
        computeNodeDurationUsesStartPlusStepDurationWhenEndMissing: function () {
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    duration: 500
                }),
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    duration: 1500
                })
            ];
            this.assert(RDNode.computeNodeDurationMs(0, steps) === 1500);
        },
        computeNodeDurationIgnoresParameterizedSteps: function () {
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 1000
                }),
                stepFixture({
                    parameterizedStep: true,
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:20Z',
                    duration: 16000
                })
            ];
            this.assert(RDNode.computeNodeDurationMs(-1, steps) === 1000);
        },
        computeNodeDurationClampsToExecDurationWhenTruncationInflates: function () {
            // Simulates truncation inflation: timestamps truncated to whole seconds
            // yield 1000ms wall-clock, but actual execution was only 457ms.
            // The execDurationMs (ms precision) should be the final clamp.
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 457
                })
            ];
            // Wall-clock from truncated timestamps: 1000ms
            // Actual execution duration: 457ms
            var ms = RDNode.computeNodeDurationMs(-1, steps, 457);
            this.assert('clamps to execDurationMs', ms === 457);
        }
    });
});
