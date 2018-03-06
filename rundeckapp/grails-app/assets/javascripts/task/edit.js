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
//= require jobedit
//= require storageBrowseKO
//= require ui/toggle
//= require pluginPropKO
//= require bootstrap-datetimepicker.min
//= require ko/binding-datetimepicker
//= require nodeFiltersKO
//= require executionOptions
//= require jobs/jobPicker
//= require jobs/jobOptions
//= require menu/job-remote-options
//= require ko/binding-popover
//= require ko/binding-message-template
//= require ko/component/job-link
//= require ko/component/map-editor
//= require ko/component/busy-spinner
//= require menu/joboptions
//= require koBind

"use strict";

function TaskEditor(data) {
    var self = this;
    self.conditionFormPrefixes = data.conditionFormPrefixes;
    self.conditionInputPrefix = data.conditionInputPrefix;
    var privInc = 0;
    self.trigger = new PluginEditor({
        service: 'TaskTrigger',
        config: data.triggerConfig,
        formId: data.triggerFormId,
        formPrefixes: data.triggerFormPrefixes,
        inputFieldPrefix: data.triggerInputPrefix,
        postLoadEditor: data.postLoadEditor
    });
    self.conditions = ko.observableArray();
    self.action = new PluginEditor({
        service: 'TaskAction',
        config: data.actionConfig,
        formId: data.actionFormId,
        formPrefixes: data.actionFormPrefixes,
        inputFieldPrefix: data.actionInputPrefix,
        postLoadEditor: data.postLoadEditor
    });
    self.userData = new MultiMap({data: data.userData, inputPrefix: data.userDataInputPrefix});
    self.init = function () {
        self.trigger.init();
        self.action.init();
        ko.utils.arrayForEach(self.conditions(), function (d) {
            d.init()
        });
    };
    self.createCondition = function (id, type, config) {
        return new PluginEditor({
            uid: 'cond_' + id,
            service: 'TaskCondition',
            provider: type,
            config: config,
            formId: 'form_' + id,
            formPrefixes: [self.conditionFormPrefixes + 'entry[cond_' + id + '].'],
            inputFieldPrefix: self.conditionInputPrefix + 'entry[cond_' + id + '].config.',
            postLoadEditor: data.postLoadEditor
        });
    };
    self.addConditionType = function (obj, evt) {
        var id = (++privInc);
        var data = jQuery(evt.target).data();
        var type = data['pluginType'];
        if (!type) {
            return;
        }
        var pluginEditor = self.createCondition(id, type, {});
        self.conditions.push(pluginEditor);
        pluginEditor.init();
    };

    self.removeCondition = function (cond) {
        self.conditions.splice(self.conditions.indexOf(cond), 1);
    };
    if (data.conditionData && data.conditionData.data) {
        var errors = data.conditionData.errors;
        data.conditionData.list.forEach(function (val, n) {
            var id = (++privInc);
            var error = errors && errors.length > n ? errors[n] : null;
            var pluginEditor = self.createCondition(id, val.type, {
                config: val.config,
                data: true,
                report: {errors: error}
            });
            self.conditions.push(pluginEditor);
        });

    }
}