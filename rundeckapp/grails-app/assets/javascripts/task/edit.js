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
//= require ko/component/plugin-editor
//= require menu/joboptions
//= require koBind
//= require task/TaskEditor




jQuery(function (z) {
    const confirm = new PageConfirm(message('page.unsaved.changes'));

    function postLoadEditor(dom) {
        dom.find('.form-control.apply_ace').each(function () {
            _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
        });
        dom.find('.scriptContent.apply_ace').each(function () {
            _applyAce(this, '400px');
        });
    }

    postLoadEditor(jQuery('body'));
    window.pluginServices = new PluginServices(
        [
            {name: 'TaskAction', providers: loadJsonData('actionPluginDescJson')},
            {name: 'TaskTrigger', providers: loadJsonData('triggerPluginDescJson')},
            {name: 'TaskCondition', providers: loadJsonData('conditionPluginDescJson')}
        ]);
    window.taskEditor = new TaskEditor(
        {
            postLoadEditor       : postLoadEditor,
            triggerConfig        : loadJsonData('triggerConfigJson'),
            triggerFormId        : 'trigeditor',
            triggerFormPrefixes  : ['triggerConfig.config.', 'orig.triggerConfig.config.'],
            triggerInputPrefix   : 'triggerConfig.',
            actionConfig         : loadJsonData('actionConfigJson'),
            actionFormId         : 'actionEditor',
            actionFormPrefixes   : ['actionConfig.config.', 'orig.actionConfig.config.'],
            actionInputPrefix    : 'actionConfig.',
            conditionData        : loadJsonData('conditionListJson'),
            conditionFormPrefixes: ['conditionList.', 'orig.conditionList.'],
            conditionInputPrefix : 'conditionList.',
            userData             : loadJsonData('taskUserDataJson'),
            userDataInputPrefix  : 'userData.'
        });

    taskEditor.init();
    taskEditor.formSubmit = new UIToggle();
    initKoBind();
});