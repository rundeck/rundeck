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

    function fakeFlow() {
        return {
            pendingStepsForNode: function () { return 0; },
            pluralize: function (n, word) { return n == 1 ? word : word + 's'; },
            hideSkippedSteps: ko.observable(false)
        };
    }

    function nodeWithSteps(states, flow) {
        var node = new RDNode('node1', [], flow || fakeFlow());
        states.forEach(function (state, i) {
            node.steps.push({
                stepctx: String(i + 1),
                parameterizedStep: ko.observable(false),
                executionState: ko.observable(state)
            });
        });
        return node;
    }

    function summarizeNode(states) {
        var node = nodeWithSteps(states);
        node.summarize();
        return {state: node.summaryState(), text: node.summary()};
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
        },
        summarizeCountsSkippedStepsAsCompleteTest: function () {
            this.assert('ok + skipped + ok is a complete node',
                summarizeNode(['SUCCEEDED', 'SKIPPED', 'SUCCEEDED']).state === 'SUCCEEDED');
            this.assert('summary mentions the skipped step',
                summarizeNode(['SUCCEEDED', 'SKIPPED', 'SUCCEEDED']).text === 'All Steps OK (1 skipped)');
            this.assert('all skipped is also complete',
                summarizeNode(['SKIPPED', 'SKIPPED']).state === 'SUCCEEDED');
            this.assert('all skipped text',
                summarizeNode(['SKIPPED', 'SKIPPED']).text === 'All Steps skipped');
        },
        summarizeKeepsNeverStartedStepsIncompleteTest: function () {
            this.assert('steps that never started leave the node incomplete',
                summarizeNode(['SUCCEEDED', 'NOT_STARTED', 'NOT_STARTED']).state === 'PARTIAL_NOT_STARTED');
            this.assert('a skipped step does not hide a never-started one',
                summarizeNode(['SUCCEEDED', 'SKIPPED', 'NOT_STARTED']).state === 'PARTIAL_NOT_STARTED');
        },
        summarizeDoesNotCountSkippedAsDidNotSucceedTest: function () {
            this.assert('only the aborted step did not succeed',
                summarizeNode(['SUCCEEDED', 'SKIPPED', 'ABORTED']).text === '1 did not succeed');
            this.assert('failure still wins',
                summarizeNode(['SUCCEEDED', 'SKIPPED', 'FAILED']).state === 'FAILED');
        },
        computeNodeDurationIgnoresSkippedStepsTest: function () {
            var steps = [
                stepFixture({
                    startTime: '2014-09-05T21:28:04Z',
                    endTime: '2014-09-05T21:28:05Z',
                    duration: 1000
                }),
                stepFixture({
                    executionState: 'SKIPPED',
                    startTime: '2014-09-05T21:28:43Z',
                    endTime: '2014-09-05T21:28:43Z'
                })
            ];
            var ms = RDNode.computeNodeDurationMs(-1, steps, -1);
            this.assert('a skipped step does not stretch the node duration', ms === 1000);
        },
        visibleStepsHidesSkippedStepsOnlyWhenToggledTest: function () {
            var flow = fakeFlow();
            var node = nodeWithSteps(['SUCCEEDED', 'SKIPPED', 'SUCCEEDED'], flow);
            this.assert('all steps shown by default', node.visibleSteps().length === 3);
            flow.hideSkippedSteps(true);
            this.assert('skipped step hidden when toggled', node.visibleSteps().length === 2);
        }
    });
});
