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

<g:embedJSON data="${
    actionPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }.collect {
        [name : it.name,
         title: stepplugin.message(
             service: org.rundeck.core.tasks.TaskPluginTypes.TaskAction,
             name: it.name,
             code: 'plugin.title',
             default: it.title ?: it.name
         )]
    }
}" id="actionPluginDescJson"/>
<g:embedJSON data="${
    triggerPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }.collect {
        [name : it.name,
         title: stepplugin.message(
             service: org.rundeck.core.tasks.TaskPluginTypes.TaskTrigger,
             name: it.name,
             code: 'plugin.title',
             default: it.title ?: it.name
         )]
    }
}" id="triggerPluginDescJson"/>
<g:embedJSON data="${
    conditionPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }.collect {
        [name : it.name,
         title: stepplugin.message(
             service: org.rundeck.core.tasks.TaskPluginTypes.TaskCondition,
             name: it.name,
             code: 'plugin.title',
             default: it.title ?: it.name
         )]
    }
}" id="conditionPluginDescJson"/>
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

        <h4 class="list-group-item-heading">

            <g:message code="task.trigger.display.title"/>
        </h4>

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

                <plugin-editor
                    params="editor: taskEditor.trigger, typeField: 'triggerType', service: 'TaskTrigger'"></plugin-editor>

                <g:hasErrors bean="${task}" field="triggerType">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="triggerType"/>
                    </span>

                </g:hasErrors>
            </div>
        </div>

    </div>

    <div class="list-group-item ${g.hasErrors(bean: task, field: 'conditionList', 'has-error')}">

        <h4 class="list-group-item-heading">

            <g:message code="Task.domain.conditions.title"/>
        </h4>

        <div class="container">
            <div data-bind="foreach: {data: conditions.items, as: 'cond'}">

                <div class=" ${enc(attr: labelColClass)}">
                    <label class="required " data-bind="visible: cond.isModeEdit">
                        <span data-bind="messageTemplate: $index() + 1"><g:message code="Task.domain.conditions.label.condition.0"/></span>
                    </label>
                    <a href="#" class="btn btn-info-hollow btn-sm"
                       data-bind="click: cond.setModeEdit, visible: cond.isModeView">
                        <g:icon name="pencil"/>
                        <span data-bind="messageTemplate: $index() + 1"><g:message code="Task.domain.conditions.label.condition.0"/></span>
                    </a>
                </div>

                <div class="${fieldColSize}">

                    <div>
                        <input type="hidden"
                               data-bind="attr: {name: $parent.conditions.inputPrefix+'_indexes' }, value: cond.uid"/>
                        %{--<input type="hidden"--}%
                        %{--data-bind="attr: {name: $parent.conditions.inputPrefix+'entry[' + cond.uid() + '].type' }, value: cond.provider"/>--}%

                        <busy-spinner params="busy: cond.loading, css: 'text-muted'"></busy-spinner>

                    </div>

                    <div>

                        <plugin-editor
                            params="editor: cond, typeField: $parent.conditions.inputPrefix+'entry[' + cond.uid() + '].type', service: 'TaskCondition'"></plugin-editor>

                    </div>
                </div>

            </div>

            <div class="col-sm-2 control-label">

                <div class="btn-group">
                    <button type="button" class="btn btn-sm btn-success-hollow dropdown-toggle"
                            data-toggle="dropdown"
                            aria-haspopup="true"
                            aria-expanded="false">
                        <g:icon name="plus"/>
                        <g:message code="button.add.condition.title"/>
                        <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu"
                        data-bind="foreach: {data:pluginServices.serviceByName('TaskCondition').providers, as: 'provider'}">
                        <li>
                            <a href="#"
                               data-bind="click: $root.conditions.addType,
                                        attr:{'data-plugin-type': provider.name},
                                        text: provider.title">

                            </a>
                        </li>
                    </ul>
                </div>

            </div>
        </div>

    </div>

    <div class="list-group-item ${g.hasErrors(bean: task, field: 'actionType', 'has-error')}">

        <h4 class="list-group-item-heading">

            <g:message code="task.action.display.title"/>
        </h4>

        <div class="container">
            <div class=" ${enc(attr: labelColClass)}">
                <label class="required " data-bind="visible: taskEditor.action.isModeEdit">
                    <g:message code="task.action.display.title"/>
                </label>
                <a href="#" class="btn btn-info-hollow btn-sm"
                   data-bind="click: taskEditor.action.setModeEdit, visible: taskEditor.action.isModeView">
                    <g:icon name="pencil"/>
                    <g:message code="task.action.display.title"/>
                </a>
            </div>

            <div class="${fieldColSize}">

                <plugin-editor
                    params="editor: taskEditor.action, typeField: 'actionType', service: 'TaskAction'"></plugin-editor>

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
