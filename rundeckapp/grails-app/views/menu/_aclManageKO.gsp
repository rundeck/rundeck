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

%{--Delete modal--}%
<g:form
        controller="${deleteAction.controller}"
        action="${deleteAction.action}"
        params="${deleteAction.params}"
        useToken="true">
    <g:render template="/common/modal" model="[
            modalid: deleteModalId?:'deleteAclPolicy',
            title  : message(code: 'delete.acl.policy'),
            buttons: [
                    [
                            css    : 'btn-danger',
                            message: message(code:'button.action.Delete'),
                    ]
            ]
    ]">
        <div class="form form-horizontal">
            <div class="row">
                <div class="col-sm-12">
                    <span class="text-danger"><g:message code="really.delete.this.policy"/></span>
                </div>
            </div>

            <div class="form-group">
                <label class="control-label col-sm-2"><g:message code="policy"/></label>

                <div class="col-sm-10">
                    <p class="form-control-static">

                        <span data-bind="if: selectedPolicy">

                            <span data-bind="template: { name: 'acl-policy-ident', data:selectedPolicy() }"></span>

                            <input type="hidden" name="file" data-bind="value: selectedPolicy().name"/>
                        </span>
                    </p>
                </div>
            </div>
        </div>
    </g:render>
</g:form>