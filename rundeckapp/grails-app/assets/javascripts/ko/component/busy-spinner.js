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

//= require asset-ko-template-loader
"use strict";


ko.components.register('busy-spinner', {
    viewModel: function (params) {
        // Data: value is either null, 'like', or 'dislike'
        var self = this;
        self.busy = params.busy;
        var messageCode = ko.unwrap(params.messageCode) || 'loading.text';
        self.message = ko.unwrap(params.message) || message(messageCode) || 'Loading...';
    },
    template: {assetTemplate: 'busy-spinner.html'}
});