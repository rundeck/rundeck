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
        data="${trigger?.conditionConfig != null ? [data: true, config: trigger?.conditionConfig, report: [errors: validation?.
                get('TaskTrigger')]] : [data: false]}"
        id="conditionConfigJson"/>
<g:embedJSON
        data="${trigger?.actionConfig != null ? [data: true, config: trigger?.actionConfig, report: [errors: validation?.
                get('TaskAction')]] : [data: false]}"
        id="actionConfigJson"/>

<div class="list-group">

    <div class="list-group-item">
        %{--name--}%
        <div class="form-group ${g.hasErrors(bean: trigger, field: 'name', 'has-error')}">
            <label for="triggerName"
                   class="required ${enc(attr: labelColClass)}">
                Name
            </label>

            <div class="${fieldColSize}">
                <g:textField name="name"
                             value="${trigger?.name}"
                             id="triggerName"
                             class="form-control"/>
                <g:hasErrors bean="${trigger}" field="name">
                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="name"/>
                    </span>
                </g:hasErrors>
            </div>

        </div>

        <!-- description-->

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'description', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Description
            </label>

            <div class="${fieldColSize}">
                <g:textField name="description"
                             value="${trigger?.description}"
                             id="description"
                             class="form-control"/>
                <g:hasErrors bean="${trigger}" field="description">

                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="description"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>
        <!-- enabled-->

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'enabled', 'has-error')}">
            <label for="enabled"
                   class="required ${enc(attr: labelColClass)}">
                Enabled
            </label>

            <div class="${fieldColSize}">
                <g:checkBox name="enabled"
                            value="${trigger ? trigger.enabled : true}"
                            id="enabled"
                            class="form-control"/>
                <g:hasErrors bean="${trigger}" field="enabled">

                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="enabled"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'conditionType', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Condition
            </label>

            <div class="${fieldColSize}">

                <select name="conditionType" value="${trigger?.conditionType}" class="form-control"
                        id="conditionTypeSelect" data-bind="value: triggerEditor.condition.provider">
                    <g:if test="${!trigger?.conditionType}">
                        <option value="" selected>-Choose-</option>
                    </g:if>
                    <g:each in="${triggerPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }}"
                            var="plugin">
                        <option value="${plugin.name}" ${trigger?.conditionType == plugin.name ? 'selected' :
                                ''}><stepplugin:message
                                service="${com.dtolabs.rundeck.plugins.ServiceNameConstants.TaskTrigger}"
                                name="${plugin.name}"
                                code="plugin.title"
                                default="${plugin.title ?: plugin.name}"/></option>
                    </g:each>
                </select>
            </div>

            <div class="${offsetColSize}">
                <div id="condeditor">
                </div>
                <g:hasErrors bean="${trigger}" field="conditionType">

                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="conditionType"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'actionType', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Action
            </label>

            <div class="${fieldColSize}">

                <select name="actionType" value="${trigger?.actionType}" class="form-control" id="actionTypeSelect"
                        data-bind="value: triggerEditor.action.provider">
                    <g:if test="${!trigger?.actionType}">
                        <option value="" selected>-Choose-</option>
                    </g:if>
                    <g:each in="${actionPlugins.values()?.description?.sort { a, b -> a.name <=> b.name }}"
                            var="plugin">
                        <option value="${plugin.name}" ${trigger?.actionType == plugin.name ? 'selected' :
                                ''}><stepplugin:message
                                service="${com.dtolabs.rundeck.plugins.ServiceNameConstants.TaskAction}"
                                name="${plugin.name}"
                                code="plugin.title"
                                default="${plugin.title ?: plugin.name}"/></option>
                    </g:each>
                </select>
            </div>

            <div class="${offsetColSize}">
                <div id="actionEditor">
                </div>
                <g:hasErrors bean="${trigger}" field="actionType">

                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="actionType"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'triggerData', 'has-error')}">
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
                            data-ace-control-soft-wrap="true">${trigger?.triggerData}</textarea>
                </div>
                <g:hasErrors bean="${trigger}" field="triggerData">

                    <span class="text-warning">
                        <g:renderErrors bean="${trigger}" as="list" field="name"/>
                    </span>

                </g:hasErrors>
            </div>

        </div>

    </div>

</div>
