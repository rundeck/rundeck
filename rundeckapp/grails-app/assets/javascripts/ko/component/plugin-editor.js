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

function PluginEditorContent(params) {
    const self = this;
    self.editor = params.editor;
    self.typeField = params.typeField;
    self.embeddedTypeField = params.embeddedTypeField;
}

ko.components.register('plugin-editor', {
    viewModel: {
        createViewModel: function (params, componentInfo) {
            return new PluginEditorContent(params);
        }
    },
    template : {assetTemplate: 'plugin-editor.html'}
});