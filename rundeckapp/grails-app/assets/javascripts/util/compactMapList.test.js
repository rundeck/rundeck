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


//= require util/testing


jQuery(function () {
    "use strict";
    new TestHarness("compactMapList.test.js", {

        decompactMapListTest: function () {
            var self = this;
            var arr = [
                {log: 'blah1', user: 'auser', node: 'anode1', level: 'NORMAL', stepctx: '1'},
                'blah2',
                {},
                'blah4',
                {node: null, stepctx: null}
            ];
            var newarr = [];
            _decompactMapList(arr, 'log', function (e) {
                newarr.push(e);
            });

            self.assert('same size', arr.length, newarr.length);
            self.assert('first value', arr[0], newarr[0]);
            self.assert('string only', {
                log: 'blah2',
                user: 'auser',
                node: 'anode1',
                level: 'NORMAL',
                stepctx: '1'
            }, newarr[1]);
            self.assert('dupe map', {
                log: 'blah2',
                user: 'auser',
                node: 'anode1',
                level: 'NORMAL',
                stepctx: '1'
            }, newarr[2]);
            self.assert('string again', {
                log: 'blah4',
                user: 'auser',
                node: 'anode1',
                level: 'NORMAL',
                stepctx: '1'
            }, newarr[3]);
            self.assert('remove attributes', {log: 'blah4', user: 'auser', level: 'NORMAL'}, newarr[4]);
        }
    });
});