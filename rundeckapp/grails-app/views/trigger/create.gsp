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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 1/30/18
  Time: 11:18 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="triggers"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - Create Trigger</title>
    <asset:stylesheet src="bootstrap-datetimepicker.min.css"/>
    <asset:javascript src="jquery.autocomplete.min.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="jobedit.js"/>
    <asset:javascript src="trigger/edit.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
    <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ext-language_tools"/><!--<![endif]-->
    <g:javascript>"use strict";
    var confirm = new PageConfirm(message('page.unsaved.changes'));
    jQuery(function () {
        jQuery('.apply_ace').each(function () {
            _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
        });
         window.triggerEditor=new TriggerEdit({
                conditionConfig:loadJsonData('conditionConfigJson'),
                conditionFormId:'condeditor',
                conditionFormPrefixes:['conditionConfig.', 'orig.conditionConfig.'],
                conditionInputPrefix:'conditionConfig.',
                actionConfig:loadJsonData('actionConfigJson'),
                actionFormId:'actionEditor',
                actionFormPrefixes:['actionConfig.', 'orig.actionConfig.'],
                actionInputPrefix:'actionConfig.'
            });
            ko.applyBindings(triggerEditor);
            triggerEditor.init();
    });
        function getFrameworkProject() {
            return "${project}";
        }
    </g:javascript>
    <g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
    <g:embedJSON data="${timeZones ?: []}" id="timeZonesData"/>

</head>

<body id="trigger_create">

<g:render template="/common/errorFragment"/>
<g:form method="POST"
        useToken="true"
        controller="trigger"
        action="createPost"
        params="[project: params.project]"
        class="form-horizontal">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <div class="row">
                <div class="col-sm-10">
                    <span class="h4">
                        Create Trigger
                    </span>
                </div>

                <div class="col-sm-2 ">
                    <g:link controller="scheduledExecution" action="upload"
                            params="[project: params.project ?: request.project]"
                            class="btn btn-default btn-sm pull-right">
                        <i class="glyphicon glyphicon-upload"></i>
                        <g:message code="upload.definition.button.label"/>
                    </g:link>
                </div>
            </div>
        </div>

        <tmpl:editForm/>

        <div class="panel-footer">
            <div id="triggerCreateButtons">
                <g:actionSubmit id="createFormCancelButton"
                                value="${g.message(code: 'cancel')}"
                                class="btn btn-default reset_page_confirm"/>
                <g:submitButton name="Create"
                                value="${g.message(code: 'button.action.Create')}"
                                onclick="['triggerCreateButtons','schedCreateSpinner'].each(Element.toggle)"
                                class="cformAllowSave cformAllowSaveOnly btn btn-primary reset_page_confirm"/>

            </div>

            <div id="schedCreateSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Creating Trigger...
            </div>
        </div>

    </div>
</g:form>
</body>
</html>
