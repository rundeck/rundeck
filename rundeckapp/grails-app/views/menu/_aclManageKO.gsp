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

                            <input type="hidden" name="id" data-bind="value: selectedPolicy().id"/>
                        </span>
                    </p>
                </div>
            </div>
        </div>
    </g:render>
</g:form>
<g:if test="${uploadAction}">
    <div id="${uploadFormId ?: 'aclUploadForm'}">
        <g:uploadForm useToken="true"
                      controller="${uploadAction.controller}"
                      action="${uploadAction.action}"
                      params="${uploadAction.params}"
                      class="form form-horizontal"
                      data-bind="submit: check">
            <g:render template="/common/modal" model="[
                    modalid : uploadModalId ?: 'aclUpload',
                    title   : message(code: 'aclpolicy.file.upload.modal.title'),
                    nocancel: true,
                    buttons : [[
                                       message: message(code: 'cancel'),
                                       bind   : 'click: function(){cancelUploadModal(\'' + (uploadModalId ?:
                                               'aclUpload') + '\');}'
                               ],
                               [
                                       css    : 'btn-success',
                                       message: message(code: 'button.action.Upload'),
                               ]]
            ]">
                <div class="form-group" data-bind="css: {'has-error':fileError} ">
                    <label class="control-label col-sm-2"><g:message code="form.option.optionType.file.label"/></label>

                    <div class="col-sm-10">
                        <input type="file" name="uploadFile"
                               data-bind="event: { change: fileChanged }"/>
                        <!-- ko if: fileError -->
                        <span class="help-block">
                            File is required
                        </span>
                        <!-- /ko -->
                    </div>

                </div>

                <div class="form-group" data-bind="css: {'has-error':nameError} ">
                    <label class="control-label col-sm-2"><g:message code="aclpolicy.file.upload.name.label"/></label>

                    <div class="col-sm-10">
                        <!-- ko if: !nameFixed() -->
                        <g:textField name="name" class="form-control" data-bind="value: name"/>
                        <span class="help-block">
                            <g:message code="policy.name.description"/>
                        </span>
                        <!-- /ko -->
                        <!-- ko if: nameFixed() -->
                        <input type="hidden" name="id" data-bind="value: idFixed"/>

                        <p class="form-control-static">
                            <g:icon name="file"/>
                            <span data-bind="text: nameFixed"></span>
                        </p>
                        <!-- /ko -->
                        <!-- ko if: nameError -->
                        <span class="help-block">
                            <g:message code="aclpolicy.file.upload.name.is.required"/>
                        </span>
                        <!-- /ko -->
                    </div>
                </div>

                <div class="form-group" data-bind="css: {'has-error':overwriteError}">

                    <div class="col-sm-10 col-sm-offset-2">
                        <!-- ko if: !nameFixed() -->
                        <div class="checkbox">
                            <label>

                                <g:checkBox name="overwrite"
                                            value="true"
                                            checked="false"
                                            data-bind="checked: overwrite"/>

                                <g:message code="aclpolicy.file.upload.overwrite.label"/>
                            </label>
                        </div>
                        <!-- /ko -->

                        <!-- ko if: nameFixed() -->
                        <span class="help-block">
                            <g:icon name="ok"/>
                            <g:message code="aclpolicy.file.upload.overwrite.label"/>
                            <input type="hidden" name="overwrite" value="true"/>
                        </span>
                        <!-- /ko -->
                        <!-- ko if: overwriteError -->
                        <span class="help-block">
                            <g:message code="aclpolicy.file.upload.exists.warning.message"/>
                        </span>
                        <!-- /ko -->
                    </div>
                </div>

            </g:render>
        </g:uploadForm>
    </div>
</g:if>
