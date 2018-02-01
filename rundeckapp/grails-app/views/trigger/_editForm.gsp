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
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

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
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

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
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'conditionData', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Condition
            </label>

            <div class="${fieldColSize}">
                <g:set var="conditionList" value="${[[name: 'schedule', label: 'Schedule']]}"/>
                <g:select name="conditionType"
                          value="${trigger?.conditionType}"
                          from="${conditionList}"
                          optionValue="label"
                          optionKey="name"
                          class="form-control"/>

            </div>

            <div class="${offsetColSize}">
                <div id="condeditor">
                    <textarea
                            name="conditionData"
                            class="form-control code apply_ace"
                            data-ace-autofocus='true'
                            data-ace-session-mode="json"
                            data-ace-height="150px"
                            data-ace-control-soft-wrap="true">${trigger?.conditionData}</textarea>
                </div>
                <g:hasErrors bean="${trigger}" field="conditionData">
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

                </g:hasErrors>
            </div>

        </div>

        <div class="form-group ${g.hasErrors(bean: trigger, field: 'actionData', 'has-error')}">
            <label for="description"
                   class="required ${enc(attr: labelColClass)}">
                Action
            </label>

            <div class="${fieldColSize}">
                <g:set var="actionList" value="${[[name: 'JobRun', label: 'Run a Job']]}"/>
                <g:select name="actionType"
                          value="${trigger?.actionType}"
                          from="${actionList}"
                          optionValue="label"
                          optionKey="name"
                          class="form-control"/>
            </div>

            <div class="${offsetColSize}">
                <div id="actionEditor">
                    <textarea
                            name="actionData"
                            class="form-control code apply_ace"
                            data-ace-autofocus='true'
                            data-ace-session-mode="json"
                            data-ace-height="150px"
                            data-ace-control-soft-wrap="true">${trigger?.actionData}</textarea>
                </div>
                <g:hasErrors bean="${trigger}" field="actionData">
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

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
                    <i alt="Error" class="glyphicon glyphicon-warning-sign"></i>

                </g:hasErrors>
            </div>

        </div>

    </div>

</div>
