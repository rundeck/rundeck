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

%{--KO template--}%
<g:render template="aclListItemKO"/>
<g:render template="aclValidationReportKO"/>

<div data-bind="css: {'flash_info':wasSaved}" style="border-top:1px solid #cfcfca; padding-top:1em;">
    <div class=" hover-action-holder">
        <span class="h3" data-bind="template: { name: 'acl-policy-ident', data:$data }"></span>
          <!-- ko if: wasSaved -->
          <span class="label label-success">
              <g:icon name="saved"/>
              <g:message code="file.was.saved.flash.message"/>
          </span>
          <!-- /ko -->
          <span data-bind="if: !valid()">
              <span class="label label-warning" data-bind="click: toggleShowValidation">
                  <g:message code="validation.error"/>
                  <i class="glyphicon" data-bind="css: {'glyphicon-chevron-right':!showValidation(), 'glyphicon-chevron-down':showValidation()}"></i>
              </span>
          </span>
        <g:if test="${flashMessage}">
            <span class="badge badge-default flash_info">
                ${flashMessage}
            </span>
        </g:if>
        <g:if test="${hasDeleteAuth || hasEditAuth}">
            <span class="dropdown pull-right" style="margin-left:1em;">
                <bs:dropdownToggle css="btn btn-sm btn-default"/>
                <bs:dropdown>
                    <bs:menuitem
                            shown="${hasEditAuth}"
                            href="#"
                            icon="upload"
                            code="button.action.Upload"
                            data-bind="click: function(){ \$root.showUploadModal('${uploadModalId}',\$data);}"></bs:menuitem>
                    <bs:menuitem
                            shown="${hasDeleteAuth && hasEditAuth}"/>
                    <bs:menuitem
                            shown="${hasDeleteAuth}"
                            href="#"
                            icon="remove"
                            code="button.action.Delete"
                            data-bind="click: function(){ \$root.showModal('${deleteModalId}', \$data);}"></bs:menuitem>

                </bs:dropdown>
            </span>
        </g:if>
        <g:if test="${hasEditAuth}">
            <a href="${editHref}" class=" btn btn-default btn-sm pull-right" data-bind="urlPathParam: id" title="Edit">
              <g:icon name="edit"/>
              <g:message code="edit"/>
            </a>
        </g:if>
        <ul data-bind="foreach:  { data: meta().policies, as: 'policy' }">
            <li>
                <span class="text-primary" data-bind="text: policy.description()">
                </span>
                <ul>
                    <li>
                        <span class="text-primary" data-bind="text: policy.by()">
                        </span>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</div>

<div class="" data-bind="if: !valid()">
    <div style="display: inline-block" data-bind="visible: showValidation" colspan="2">
        <div class="well well-sm inline">
            <span data-bind="template: { name: 'acl-policy-validation', data:$data }"></span>
        </div>
    </div>
</div>
