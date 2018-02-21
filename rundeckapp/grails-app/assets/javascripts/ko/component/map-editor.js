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

"use strict";
//= require util/ko-multi-map



ko.components.register('map-editor', {
    viewModel: function (params) {
        // Data: value is either null, 'like', or 'dislike'
        var self = this;
        self.prefix = params.prefix;
        self.userData = new MultiMap({
            value: params.value,
            inputPrefix: ko.unwrap(self.prefix) || ''
        });

    },
    template:
    '<div class=" form-horizontal" data-bind="foreach: {data: userData.entries}">\n' +
    '                        <div class="form-group">\n' +
    '                            <div class="col-sm-4">\n' +
    '                                <div class="input-group ">\n' +
    '\n' +
    '                                    <span class="input-group-addon">\n' +
    '                                        <span data-bind="messageValue: true">key.value.key.title</span>\n' +
    '                                    </span>\n' +
    '                                    <input type="text"\n' +
    '                                           data-bind="value: key, attr: {name: keyFieldName }"\n' +
    '                                           class="form-control "\n' +
    '                                           placeholder="key"/>\n' +
    '                                </div>\n' +
    '\n' +
    '                            </div>\n' +
    '\n' +
    '\n' +
    '                            <div class=" col-sm-8">\n' +
    '                                <div class="input-group ">\n' +
    '                                    <input type="text"\n' +
    '                                           data-bind="value: value, attr: {name: valueFieldName }"\n' +
    '                                           class="form-control "\n' +
    '                                           placeholder="value"/>\n' +
    '                                    <span class="input-group-btn">\n' +
    '                                        <button class="btn btn-danger-hollow" type="button"\n' +
    '                                                data-bind="click: $component.userData.delete">\n' +
    '                                            <i class="glyphicon glyphicon-remove"></i>\n' +
    '                                        </button>\n' +
    '                                    </span>\n' +
    '                                </div>\n' +
    '                            </div>\n' +
    '\n' +
    '                        </div>\n' +
    '                    </div>\n' +
    '\n' +
    '                    <div>\n' +
    '                        <span class="btn btn-success-hollow btn-sm " data-bind="click: userData.newEntry">\n' +
    '                            <span data-bind="messageValue: true">button.title.add.key.value.pair</span>\n' +
    '                            <i class="glyphicon glyphicon-plus"></i>\n' +
    '                        </span>\n' +
    '                    </div>'
});