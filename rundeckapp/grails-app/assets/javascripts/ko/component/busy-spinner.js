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

function BusySpinner(params) {
    var self = this;
    self.busy = params.busy;
    self.message = params.message;
    self.css = params.css;
}

ko.components.register('busy-spinner', {
    viewModel: {
        createViewModel: function (params, componentInfo) {

            var msgText = '';
            if (!componentInfo || !componentInfo.templateNodes || componentInfo.templateNodes.length < 1) {

                var messageCode = ko.unwrap(params.messageCode) || 'loading.text';
                msgText = ko.unwrap(params.message) || message(messageCode) || 'Loading...';
            }
            return new BusySpinner({
                busy: params.busy,
                message: msgText,
                css: params.css
            });

        }
    },
    template: {assetTemplate: 'busy-spinner.html'}
});