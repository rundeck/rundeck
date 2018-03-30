<%@ page import="org.rundeck.core.tasks.TaskPluginTypes" %>
%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%


<g:set var="labelColSize" value="col-sm-2"/>
<g:set var="labelColClass" value="${labelColSize}  control-label"/>
<g:set var="fieldColSize" value="col-sm-10"/>
<g:set var="fieldColHalfSize" value="col-sm-5"/>
<g:set var="fieldColShortSize" value="col-sm-4"/>
<g:set var="offsetColSize" value="col-sm-10 col-sm-offset-2"/>

%{--<g:embedJSON data="${}" id="pluginDescriptions"/>--}%
<g:embedJSON
    data="${task?.triggerConfig != null ? [data: true, type: task?.triggerType, config: task?.triggerConfig, report: [errors: validation?.
        get('TaskTrigger')]] : [data: false]}"
    id="triggerConfigJson"/>
<g:embedJSON
    data="${task?.actionConfig != null ? [data: true, type: task?.actionType, config: task?.actionConfig, report: [errors: validation?.
        get('TaskAction')]] : [data: false]}"
    id="actionConfigJson"/>

<g:embedJSON
    data="${task?.conditionList != null ?
            [
                data  : true,
                list  : task?.conditionList,
                errors: validation?.get('TaskCondition')
            ]
                                        : [data: false]}"
    id="conditionListJson"/>

<g:embedJSON data="${task?.userData ?: [:]}" id="taskUserDataJson"/>

<div class="list-group">

    <div class="list-group-item">
        %{--name--}%
        <div class="form-group ${g.hasErrors(bean: task, field: 'name', 'has-error')}">
            <label for="triggerName"
                   class="required ${enc(attr: labelColClass)}">
                <g:message code="Task.domain.name.title"/>
            </label>

            <div class="${fieldColSize}">
                <g:textField name="name"
                             value="${task?.name}"
                             id="triggerName"
                             class="form-control"/>
                <g:hasErrors bean="${task}" field="name">
                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="name"/>
                    </span>
                </g:hasErrors>
            </div>

        </div>

        <!-- description-->

        <div class="form-group ${g.hasErrors(bean: task, field: 'description', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                <g:message code="Task.domain.description.title"/>
            </label>

            <div class="${fieldColSize}">
                <g:textField name="description"
                             value="${task?.description}"
                             id="description"
                             class="form-control"/>
                <g:hasErrors bean="${task}" field="description">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="description"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>
        <!-- enabled-->

        <div class="form-group ${g.hasErrors(bean: task, field: 'enabled', 'has-error')}">
            <label for="enabled"
                   class="required ${enc(attr: labelColClass)}">
                <g:message code="Task.domain.enabled.title"/>
            </label>

            <div class="${fieldColSize}">
                <g:checkBox name="enabled"
                            value="${task ? task.enabled : true}"
                            id="enabled"
                            class="form-control"/>
                <g:hasErrors bean="${task}" field="enabled">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="enabled"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

    </div>

    <div class="list-group-item ${g.hasErrors(bean: task, field: 'triggerType', 'has-error')}">

        <h3 class="list-group-item-heading rd-task-section-title">
            <g:message code="Task.domain.section.triggers.title" />
        </h3>

        <div class="container">
            <div class=" ${enc(attr: labelColClass)}">
                <label class="required " data-bind="visible: taskEditor.trigger.isModeEdit">
                    <g:message code="task.trigger.display.title"/>
                </label>
                <a href="#" class="btn btn-info-hollow btn-sm"
                   data-bind="click: taskEditor.trigger.setModeEdit, visible: taskEditor.trigger.isModeView">
                    <g:icon name="pencil"/>
                    <g:message code="task.trigger.display.title"/>
                </a>
            </div>

            <div class="${fieldColSize}">

                <plugin-editor params="editor: taskEditor.trigger, typeField: 'triggerType'"></plugin-editor>

                <g:hasErrors bean="${task}" field="triggerType">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="triggerType"/>
                    </span>

                </g:hasErrors>
            </div>
        </div>

    </div>

    <div class="list-group-item ${g.hasErrors(bean: task, field: 'conditionList', 'has-error')}">

        <h3 class="list-group-item-heading rd-task-section-title">
            <g:message code="Task.domain.section.conditions.title" />
        </h3>

        <div class="container">
            <plugin-list-editor params="listEditor: conditions, labelColumnCss: '${enc(attr:labelColClass)}', fieldColumnCss: '${enc(attr:fieldColSize)}' "></plugin-list-editor>
        </div>
    </div>

    <div class="list-group-item ${g.hasErrors(bean: task, field: 'actionType', 'has-error')}">

        <h3 class="list-group-item-heading rd-task-section-title">
            <g:message code="Task.domain.section.actions.title" />
        </h3>

        <div class="container">
            <div class=" ${enc(attr: labelColClass)}">
                <label class="required " data-bind="visible: taskEditor.action.isModeEdit">
                    <g:message code="framework.service.TaskAction.label"/>
                </label>
                <a href="#" class="btn btn-info-hollow btn-sm"
                   data-bind="click: taskEditor.action.setModeEdit, visible: taskEditor.action.isModeView">
                    <g:icon name="pencil"/>
                    <g:message code="framework.service.TaskAction.label"/>
                </a>
            </div>

            <div class="${fieldColSize}">

                <plugin-editor params="editor: taskEditor.action, typeField: 'actionType'"></plugin-editor>

                <g:hasErrors bean="${task}" field="actionType">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="actionType"/>
                    </span>

                </g:hasErrors>
            </div>
        </div>

    </div>


    <div class="list-group-item ${g.hasErrors(bean: task, field: 'userData', 'has-error')}">

        <h4 class="list-group-item-heading">

            <g:message code="Task.domain.userData.title"/>
        </h4>

        <div class="container">

            <div>

                <div class=" form-horizontal" data-bind="foreach: {data: userData.entries}">
                    <div class="form-group">
                        <div class="col-sm-4">
                            <div class="input-group ">

                                <span class="input-group-addon">
                                    <g:message code="key.value.key.title"/>
                                </span>
                                <input type="text"
                                       data-bind="value: key, attr: {name: keyFieldName }"
                                       class="form-control "
                                       placeholder="key"/>
                            </div>

                        </div>


                        <div class=" col-sm-8">
                            <div class="input-group ">
                                <input type="text"
                                       data-bind="value: value, attr: {name: valueFieldName }"
                                       class="form-control "
                                       placeholder="value"/>
                                <span class="input-group-btn">
                                    <button class="btn btn-danger-hollow" type="button"
                                            data-bind="click: $root.userData.delete">
                                        <g:icon name="remove"/>
                                    </button>
                                </span>
                            </div>
                        </div>

                    </div>
                </div>

                <div>
                    <span class="btn btn-success-hollow btn-sm " data-bind="click: userData.newEntry">
                        <g:message code="button.title.add.key.value.pair"/>
                        <g:icon name="plus"/>
                    </span>
                </div>

                <div class="help-block">
                    <g:message code="Task.domain.userData.description"/>
                </div>
            </div>

            <g:hasErrors bean="${task}" field="taskUserData">

                <span class="text-warning">
                    <g:renderErrors bean="${task}" as="list" field="name"/>
                </span>

            </g:hasErrors>
        </div>

    </div>

</div>
