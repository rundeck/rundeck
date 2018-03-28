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

function PluginListEditorContent(params) {
    const self = this;
    self.listEditor = params.listEditor;
    self.service = params.service;
    self.labels = pluginServices.serviceByName(params.service).labels;
    self.providers = pluginServices.serviceByName(params.service).providers;
    self.labelColumnCss = params.labelColumnCss || '';
    self.fieldColumnCss = params.fieldColumnCss || '';
}

ko.components.register('plugin-list-editor', {
    viewModel: {
        createViewModel: function (params, componentInfo) {
            return new PluginListEditorContent(params);
        }
    },
    template : {assetTemplate: 'plugin-list-editor.html'}
});