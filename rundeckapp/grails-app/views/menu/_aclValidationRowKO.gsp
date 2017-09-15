%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

<script type="text/html" id="acl-policy-ident">
<span data-bind="if: valid">

    <g:icon name="file"/>
</span>
<span data-bind="if: !valid()">

    <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
       title="${message(code: "aclpolicy.format.validation.failed")}"></i>
</span>
<span data-bind="text: name"></span>
</script>


<div class="grid-row">
    <div class="grid-cell hover-action-holder">
        <g:if test="${hasEditAuth}">
            <a href="${editHref}" class=" btn btn-link textbtn-info " data-bind="urlPathParam: name" title="Edit">

                <span data-bind="template: { name: 'acl-policy-ident', data:$data }"></span>

                <span class=" text-info">
                    <g:icon name="edit"/>
                    <g:message code="edit"/>
                </span>
            </a>
        </g:if>
        <g:else>
            <span data-bind="template: { name: 'acl-policy-ident', data:$data }"></span>
        </g:else>
        <g:if test="${flashMessage}">
            <span class="badge badge-default flash_info">
                ${flashMessage}
            </span>
        </g:if>
        <g:if test="${hasDeleteAuth}">
            <span class="btn btn-sm btn-link btn-danger hover-action"
                  title="${message(code: "button.action.Delete")}"
                  data-bind="click: function(){$root.showModal('${deleteModalId}',$data);}">
                <g:icon name="remove"/>
                <g:message code="button.action.Delete"/>
            </span>
        </g:if>
        <!-- ko if: wasSaved -->
        <span class="badge badge-default flash_info" data-bind="messageTemplate: savedSize">
            <g:message code="file.was.saved.flash.message.0"/>
        </span>
        <!-- /ko -->
    </div>

    <div class="grid-cell">
        <span data-bind="if: !valid()">
            <span class="text-warning btn btn-link" data-bind="click: toggleShowValidation">
                <g:message code="validation.error"/>
                <i class="glyphicon" data-bind="css: {'glyphicon-chevron-right':!showValidation(), 'glyphicon-chevron-down':showValidation()}"></i>
            </span>
        </span>
    </div>
</div>

<div class="" data-bind="if: !valid()">
    <div style="display: inline-block" data-bind="visible: showValidation">
        <div class="well well-sm inline">
            <ul>
                <li data-bind="foreachprop: validation">

                    <code data-bind="text: key"></code>
                    <g:helpTooltip css="text-info" code="acl.validation.error.sourceIdentity.help"/>
                    <ol data-bind="foreach: value">
                        <li><code data-bind="text: $data"></code></li>
                    </ol>

                </li>
            </ul>
        </div>
    </div>
</div>