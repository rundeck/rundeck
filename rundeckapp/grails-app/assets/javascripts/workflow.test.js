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

//= require workflow
//= require util/testing


jQuery(function () {
    "use strict";
    new TestHarness("workflow.test.js", {
        workflowTest: function () {

            this.assert("unescape", RDWorkflow.unescape('abc/123', '\\', ['\\', '/'], ['/']), {
                text: 'abc',
                bchar: '/',
                rest: '123'
            });
            this.assert("unescape", RDWorkflow.unescape('abc/123/456', '\\', ['\\', '/'], ['/']), {
                text: 'abc',
                bchar: '/',
                rest: '123/456'
            });
            this.assert("unescape", RDWorkflow.unescape('a\\/bc/123/456', '\\', ['\\', '/'], ['/']), {
                text: 'a/bc',
                bchar: '/',
                rest: '123/456'
            });
        },
        splitEscapedTest: function () {
            this.assert("splitEscaped", RDWorkflow.splitEscaped('a\\/bc/123/456', '/'), ['a/bc', '123', '456']);
            this.assert("splitEscaped", RDWorkflow.splitEscaped('a\\/b@c/1,2=3/4\\\\56', '/'), ['a/b@c', '1,2=3', '4\\56']);

        },
        isErrorhandlerForContextIdTest: function () {
            this.assert(RDWorkflow.isErrorhandlerForContextId('1e@blah=c') === true);
            this.assert(RDWorkflow.isErrorhandlerForContextId('2') === false);
            this.assert(RDWorkflow.isErrorhandlerForContextId('2@node=a') === false);
            this.assert(RDWorkflow.isErrorhandlerForContextId('2e') === true);
            this.assert(RDWorkflow.isErrorhandlerForContextId('2e@blah=c') === true);

        },
        paramsForContextIdTest: function () {
            this.assert(RDWorkflow.paramsForContextId('2') === null);
            this.assert(RDWorkflow.paramsForContextId('2@node=a') === 'node=a');
            this.assert(RDWorkflow.paramsForContextId('2@node\\=a') === 'node=a');
            this.assert(RDWorkflow.paramsForContextId('2e') === null);
            this.assert(RDWorkflow.paramsForContextId('2e@blah=c') === 'blah=c');

        },
        stepNumberForContextIdTest: function () {
            this.assert(RDWorkflow.stepNumberForContextId('1e@blah=c') === 1);
            this.assert(RDWorkflow.stepNumberForContextId('2') === 2);
            this.assert(RDWorkflow.stepNumberForContextId('2@node=a') === 2);
            this.assert(RDWorkflow.stepNumberForContextId('2e') === 2);
            this.assert(RDWorkflow.stepNumberForContextId('2e@blah=c') === 2);

        },
        workflowIndexForContextIdTest: function () {
            this.assert(RDWorkflow.workflowIndexForContextId('1e@blah=c') === 0);
            this.assert(RDWorkflow.workflowIndexForContextId('2') === 1);
            this.assert(RDWorkflow.workflowIndexForContextId('2@node=a') === 1);
            this.assert(RDWorkflow.workflowIndexForContextId('2e') === 1);
            this.assert(RDWorkflow.workflowIndexForContextId('2e@blah=c') === 1);

        },
        parseContextIdTest: function () {
            //parse context id
            this.assert("parseContextId", RDWorkflow.parseContextId('1'), ['1']);
            this.assert("parseContextId", RDWorkflow.parseContextId('1/1'), ['1', '1']);
            this.assert("parseContextId", RDWorkflow.parseContextId('1/1/1'), ['1', '1', '1']);
            this.assert("parseContextId", RDWorkflow.parseContextId('1/2/3'), ['1', '2', '3']);
            this.assert("parseContextId", RDWorkflow.parseContextId('1e@abc/2/3'), ['1e@abc', '2', '3']);
            this.assert("parseContextId", RDWorkflow.parseContextId('1/2e@asdf=xyz/3'), ['1', '2e@asdf=xyz', '3']);
            this.assert("parseContextId", RDWorkflow.parseContextId('2@node=crub\\/dub-1/1'), ['2@node=crub/dub-1', '1']);


        },
        cleanContextIdTest: function () {
            //clean context id
            this.assert(RDWorkflow.cleanContextId('1/2/3') === '1/2/3', 'wrong value');
            this.assert(RDWorkflow.cleanContextId('1e@abc/2/3') === '1/2/3', 'wrong value');
            this.assert(RDWorkflow.cleanContextId('1/2e@asdf=xyz/3') === '1/2/3', 'wrong value');


        },
        stepPluginDescriptionsTest: function () {
            var orig = RDWorkflow.nodeSteppluginDescriptions;
            var orig2 = RDWorkflow.wfSteppluginDescriptions;
            RDWorkflow.nodeSteppluginDescriptions = {};
            RDWorkflow.wfSteppluginDescriptions = {};
            RDWorkflow.nodeSteppluginDescriptions = {
                "example-node-step": {
                    "title": "blah"
                }
            };
            //render string, with descriptions
            var wf1 = new RDWorkflow([{
                "type": "example-node-step",
                "nodeStep": true,
                "configuration": {"example": "whatever"}
            }]);
            this.assert(wf1.renderContextString("1") === "blah");

            RDWorkflow.wfSteppluginDescriptions = {
                "example-node-step": {
                    "title": "blah"
                }
            };
            var wf2 = new RDWorkflow([{
                "type": "example-node-step",
                "nodeStep": false,
                "configuration": {"example": "whatever"}
            }]);
            this.assert(wf2.renderContextString("1") === "blah");

            RDWorkflow.nodeSteppluginDescriptions = {};
            RDWorkflow.wfSteppluginDescriptions = {};
            //render string, missing descriptions
            var wf3 = new RDWorkflow([{
                "type": "example-node-step",
                "nodeStep": true,
                "configuration": {"example": "whatever"}
            }]);
            this.assert(wf3.renderContextString("1") === "Plugin example-node-step");

            var wf4 = new RDWorkflow([{
                "type": "example-node-step",
                "nodeStep": false,
                "configuration": {"example": "whatever"}
            }]);
            this.assert(wf4.renderContextString("1") === "Plugin example-node-step");

            RDWorkflow.nodeSteppluginDescriptions = orig;
            RDWorkflow.wfSteppluginDescriptions = orig;
        }
    });
});

