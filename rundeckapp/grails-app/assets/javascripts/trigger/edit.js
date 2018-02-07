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
//= require storageBrowseKO
//= require ui/toggle
//= require pluginPropKO
//= require ko/binding-job-link

"use strict";

function TriggerEdit(data) {
    var self = this;
    self.condition = new PluginEditor({
        service: 'TriggerCondition',
        config: data.conditionConfig,
        formId: data.conditionFormId,
        formPrefixes: data.conditionFormPrefixes,
        inputFieldPrefix: data.conditionInputPrefix
    });
    self.action = new PluginEditor({
        service: 'TriggerAction',
        config: data.actionConfig,
        formId: data.actionFormId,
        formPrefixes: data.actionFormPrefixes,
        inputFieldPrefix: data.actionInputPrefix
    });
    self.init = function () {
        self.condition.init();
        self.action.init();
    };

}