

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

<g:embedJSON data="${logFilterPlugins?.entrySet().collect {
    [
            type       : it.value.name,
            iconSrc    : stepplugin.pluginIconSrc(service: 'LogFilter', name: it.value.name),
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
<g:javascript>
    jQuery(function () {
        workflowEditor.loadStepFilterPlugins(loadJsonData('logFilterPluginsData'));
        ko.applyBindings(workflowEditor, jQuery('#addLogFilterPluginModal')[0]);
        ko.applyBindings(workflowEditor, jQuery('#editLogFilterPluginModal')[0]);
    });
</g:javascript>
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

                    <!-- ko if: modalFilterEditStep()-->
                    <!-- ko with: modalFilterEditStep()-->
                    for Step: <span data-bind="text: displayNum"></span>
                    <!-- ko if: description -->
                    <span class="text-info" data-bind="text: description"></span>
                    <!-- /ko -->
                    <!-- /ko -->
                    <!-- /ko -->
                </h4>
            </div>

            <div class="modal-body">

                <div class="list-group">

                    <!-- ko foreach: filterPlugins -->
                    <a class="list-group-item textbtn" href="#"
                       data-bind="click: $root.addSelectedFilterPopup, attr: {'data-plugin-type':type}">
                        <!-- ko if: iconSrc -->
                        <img width="16px" height="16px" data-bind="attr: {src: iconSrc}"/>
                        <!-- /ko -->
                        <!-- ko if: !iconSrc() -->
                        <i class="rdicon icon-small plugin"></i>
                        <!-- /ko -->
                        <span data-bind="text: title"></span>
                        <span class="text-info">
                            -
                            <span data-bind="text: descriptionFirstLine"></span>
                        </span>
                    </a>
                    <!-- /ko -->

                </div>
                <div class="help-block">
                    Choose Log Filter Plugin to add to the Step
                </div>

            </div>

            <div class="modal-footer">

                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                    <g:message code="button.action.Cancel" />
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
                    <!-- ko if: modalFilterEdit() -->
                    Edit Log Filter Plugin
                    <!-- /ko -->
                    <!-- ko if: !modalFilterEdit() -->
                    Add Log Filter Plugin
                    <!-- /ko -->

                    <!-- ko if: modalFilterEditStep()-->
                    <!-- ko with: modalFilterEditStep()-->
                    for Step: <span data-bind="text: displayNum"></span>
                    <!-- ko if: description -->
                    <span class="text-info" data-bind="text: description"></span>
                    <!-- /ko -->
                    <!-- /ko -->
                    <!-- /ko -->
                </h4>
            </div>

            <div class="modal-body">
                <div id="editLogFilterPluginModalForm"></div>

            </div>

            <div class="modal-footer">

                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                    <g:message code="button.action.Cancel" />
                </button>
                <button type="submit" class="btn btn-success  " data-bind="click: $root.saveFilterPopup">
                    <g:message code="button.action.Save" />
                </button>
            </div>
        </div>
    </div>
</div>
