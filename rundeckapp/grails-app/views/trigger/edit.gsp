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
  Time: 2:11 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - Edit Trigger</title>
    <asset:javascript src="jquery.autocomplete.min.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
    <g:javascript>"use strict";
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        jQuery(function (z) {

            jQuery('.apply_ace').each(function () {
                _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
            });
        });
    </g:javascript>
    <g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
    <g:embedJSON data="${timeZones ?: []}" id="timeZonesData"/>

</head>

<body id="trigger_edit">
<g:render template="/common/errorFragment"/>
<g:form method="POST"
        useToken="true"
        controller="trigger"
        action="updatePost"
        params="[project: params.project]"
        class="form-horizontal">
    <div class="panel panel-primary">
        <div class="panel-heading">
            <span class="h4">
                Edit Trigger
            </span>
            <g:hiddenField name="id" value="${trigger.uuid}"/>
        </div>

        <tmpl:editForm model="[trigger: trigger]"/>

        <div class="panel-footer">
            <div id="formButtons">
                <g:actionSubmit id="createFormCancelButton" value="${g.message(code: 'cancel')}"

                                class="btn btn-default reset_page_confirm"/>
                <g:submitButton name="Save" value="${g.message(code: 'button.action.Save')}"
                                onclick="['formButtons','submitSpinner'].each(Element.toggle)"
                                class="btn btn-primary reset_page_confirm"/>
            </div>

            <div id="submitSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Saving...
            </div>
        </div>

    </div>
</g:form>

</body>
</html>
