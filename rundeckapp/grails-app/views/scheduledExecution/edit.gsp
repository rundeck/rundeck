<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title>%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

<g:appTitle/> - <g:message
            code="ScheduledExecution.page.edit.title"/></title>
    <asset:javascript src="jquery.autocomplete.min.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="jobEditPage_bundle.js"/>
    <asset:javascript src="util/markdeep.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        _onJobEdit(confirm.setNeedsConfirm);
        var workflowEditor = new WorkflowEditor();
        jQuery(function () {
            workflowEditor.loadStepFilterPlugins(loadJsonData('logFilterPluginsData'));
            ko.applyBindings(workflowEditor, jQuery('#addLogFilterPluginModal')[0]);
        });
    </g:javascript>
    <g:embedJSON data="${globalVars ?: []}" id="globalVarData"/>
    <g:embedJSON data="${logFilterPlugins?.entrySet().collect {
        [
                type       : it.value.name,
                title      :
                        stepplugin.messageText(
                                service: 'LogFilter',
                                name: it.value.name,
                                code: 'plugin.title',
                                default: it.value.description.title
                        ),
                description:
                        stepplugin.messageText(
                                service: 'LogFilter',
                                name: it.value.name,
                                code: 'plugin.description',
                                default: it.value.description.description
                        )
        ]
    }}" id="logFilterPluginsData"/>
</head>
<body>

%{--add log filter plugin modal--}%
<div class="modal" id="addLogFilterPluginModal" tabindex="-1" role="dialog"
     aria-labelledby="addLogFilterPluginModaltitle" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"
                        aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="addLogFilterPluginModaltitle">
                    Add Log Filter Plugin
                </h4>
            </div>

            <div class="modal-body">
                <div class="list-group">
                    <!-- ko foreach: filterPlugins -->
                    <a class="list-group-item" href="#"
                       data-bind="click: $root.addSelectedFilterPopup, attr: {'data-type':type}">
                        <span data-bind="text: title" class="em"></span>
                        :
                        <span data-bind="text: descriptionFirstLine" class="text-muted"></span>
                    </a>
                    <!-- /ko -->
                </div>

            </div>

            <div class="modal-footer">

                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                    Cancel
                </button>
            </div>
        </div>
    </div>
</div>
%{--edit log filter plugin modal--}%
<div class="modal" id="editLogFilterPluginModal" tabindex="-1" role="dialog"
     aria-labelledby="editLogFilterPluginModaltitle" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"
                        aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="editLogFilterPluginModaltitle">
                    Edit Log Filter Plugin
                </h4>
            </div>

            <div class="modal-body">
                <div id="editLogFilterPluginModalForm"></div>

            </div>

            <div class="modal-footer">

                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                    Cancel
                </button>
                <button type="submit" class="btn btn-success  " data-dismiss="modal">
                    Save
                </button>
            </div>
        </div>
    </div>
</div>

    <tmpl:editForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized, notificationPlugins: notificationPlugins]"/>
</body>
</html>
