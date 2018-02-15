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

<g:embedJSON
        data="${task?.triggerConfig != null ? [data: true, config: task?.triggerConfig, report: [errors: validation?.
                get('TaskTrigger')]] : [data: false]}"
        id="conditionConfigJson"/>
<g:embedJSON
        data="${task?.actionConfig != null ? [data: true, config: task?.actionConfig, report: [errors: validation?.
                get('TaskAction')]] : [data: false]}"
        id="actionConfigJson"/>

<div class="list-group">

    <div class="list-group-item">
        %{--name--}%
        <div class="form-group ${g.hasErrors(bean: task, field: 'name', 'has-error')}">
            <label for="triggerName"
                   class="required ${enc(attr: labelColClass)}">
                Name
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
                Description
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
                Enabled
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

        <div class="form-group ${g.hasErrors(bean: task, field: 'triggerType', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Condition
            </label>

            <div class="${fieldColSize}">

                <select name="triggerType" value="${task?.triggerType}" class="form-control"
                        id="conditionTypeSelect" data-bind="value: taskEditor.trigger.provider">
                    <g:if test="${!task?.triggerType}">
                        <option value="" selected>-Choose-</option>
                    </g:if>
                    <g:each in="${triggerPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }}"
                            var="plugin">
                        <option value="${plugin.name}" ${task?.triggerType == plugin.name ? 'selected' :
                                ''}><stepplugin:message
                                service="${org.rundeck.core.tasks.TaskPluginTypes.TaskTrigger}"
                                name="${plugin.name}"
                                code="plugin.title"
                                default="${plugin.title ?: plugin.name}"/></option>
                    </g:each>
                </select>
            </div>

            <div class="${offsetColSize}">
                <div id="condeditor">
                </div>
                <g:hasErrors bean="${task}" field="triggerType">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="triggerType"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: task, field: 'actionType', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Action
            </label>

            <div class="${fieldColSize}">

                <select name="actionType" value="${task?.actionType}" class="form-control" id="actionTypeSelect"
                        data-bind="value: taskEditor.action.provider">
                    <g:if test="${!task?.actionType}">
                        <option value="" selected>-Choose-</option>
                    </g:if>
                    <g:each in="${actionPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }}"
                            var="plugin">
                        <option value="${plugin.name}" ${task?.actionType == plugin.name ? 'selected' :
                                ''}><stepplugin:message
                                service="${org.rundeck.core.tasks.TaskPluginTypes.TaskAction}"
                                name="${plugin.name}"
                                code="plugin.title"
                                default="${plugin.title ?: plugin.name}"/></option>
                    </g:each>
                </select>
            </div>

            <div class="${offsetColSize}">
                <div id="actionEditor">
                </div>
                <g:hasErrors bean="${task}" field="actionType">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="actionType"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: task, field: 'triggerData', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Data
            </label>

            <div class="${fieldColSize}">

                <div id="triggerDataEditor">
                    <textarea
                            name="triggerData"
                            class="form-control code apply_ace"
                            data-ace-autofocus='true'
                            data-ace-session-mode="json"
                            data-ace-height="150px"
                            data-ace-control-soft-wrap="true">${task?.triggerData}</textarea>
                </div>
                <g:hasErrors bean="${task}" field="triggerData">

                    <span class="text-warning">
                        <g:renderErrors bean="${task}" as="list" field="name"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

    </div>

</div>
