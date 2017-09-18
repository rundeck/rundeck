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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 7/7/17
  Time: 9:01 AM
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>

<g:set var="hasAdminAuth" value="${auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_ADMIN],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasEditAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_UPDATE],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasCreateAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_CREATE],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasDeleteAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_DELETE],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasUploadValidationError" value="${input?.upload && validation && !validation.valid}"/>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'gui.menu.AccessControl')}"/>
    <title><g:message code="page.title.project.access.control.0" args="${[params.project]}"/></title>

    <asset:javascript src="menu/aclListing.js"/>
    <script type="application/javascript">
        var checkUploadForm;
        jQuery(function () {
            var data = loadJsonData('aclPolicyList');
            window.policies = new PolicyFiles(data);
            ko.applyBindings(policies, jQuery('#policyList')[0]);
            <g:if test="${hasCreateAuth}" >
            window.aclfileupload = new PolicyUpload({uploadField: '#uploadFile'});
            ko.applyBindings(aclfileupload, jQuery('#aclUpload')[0]);
            checkUploadForm = function () {
                return aclfileupload.check();
            };
            <g:if test="${hasUploadValidationError}" >
            window.uploadedpolicy = new PolicyDocument(loadJsonData('uploadedPolicy'));
            ko.applyBindings(uploadedpolicy, jQuery('#uploadedPolicyValidation')[0]);
            </g:if>
            </g:if>
        })

    </script>
    <g:embedJSON data="${[policies: acllist.collect {
        [name: it, valid: true, description: descriptions?.get(
                it
        )] + (flash.storedFile == it ? [wasSaved: true, savedSize: flash.storedSize] : [:])
    }]}" id="aclPolicyList"/>
    <g:embedJSON id="uploadedPolicy"
                 data="${hasUploadValidationError ?
                         [name: input?.file, valid: validation.valid, validation: validation.errors] :
                         [:]}"/>
</head>
<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
    <g:if test="${hasUploadValidationError}">
        <div id="uploadedPolicyValidation" class="col-sm-12">
            <g:render template="aclValidationReportKO"/>
            <div class="alert alert-default alert-dismissible">
                <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                <h4><g:message code="aclpolicy.file.upload.failed.acl.policy.validation.message" /></h4>
                <span data-bind="template: { name: 'acl-policy-validation', data:$data }"></span>
            </div>
        </div>

    </g:if>
</div>

<div class="row">
    <div class="col-sm-10 col-sm-offset-1">
        <div class="panel panel-default">
            <div class="panel-heading clearfix">
                <span class="panel-title pull-left">
                    <span class="text-info">${acllist?.size() ?: 0}</span>
                    <g:message code="project.access.control.prompt" args="${[params.project]}"/>
                </span>
                <g:if test="${hasCreateAuth}">
                    <div class="btn-group pull-right">
                        <span class="btn btn-sm btn-default" data-toggle="modal" data-target="#aclUpload">
                            <g:icon name="upload"/>
                            Upload
                        </span>
                        <g:link controller="menu"
                                action="createProjectAclFile"
                                params="${[project: params.project]}"
                                class="btn btn-sm btn-success">
                            <g:icon name="plus"/>
                            <g:message code="access.control.action.create.acl.policy.button.title"/>
                        </g:link>
                    </div>
                </g:if>
            </div>

            <div class="panel-body" id="policyList">
                <div class="grid">
                    <div data-bind="foreach: policies">
                        <g:render template="/menu/aclValidationRowKO"
                                  model="${[
                                          hasEditAuth  : hasEditAuth,
                                          hasDeleteAuth: hasDeleteAuth,
                                          editHref     : g.createLink(
                                                  [controller: 'menu', action: 'editProjectAclFile', params: [project: params.project, file: '<$>']]
                                          ),
                                          deleteModalId: 'deleteAclPolicy',
                                  ]}"/>

                    </div>

                    <g:render template="/menu/aclManageKO" model="[
                            deleteAction:
                                    [controller: 'menu', action: 'deleteProjectAclFile', params: [project: params.project]]
                    ]"/>
                </div>
            </div>
        </div>
    </div>
</div>

<g:if test="${hasCreateAuth}">
    <g:uploadForm useToken="true"
                  controller="menu"
                  action="saveProjectAclFile"
                  params="[project: params.project, upload: true]"
                  class="form form-horizontal"
                  onsubmit="return checkUploadForm()">
        <g:render template="/common/modal" model="[
                modalid: 'aclUpload',
                title  : message(code: 'aclpolicy.file.upload.modal.title'),
                buttons: [[
                                  css    : 'btn-success',
                                  message: message(code: 'button.upload.title'),
                          ]]
        ]">
            <div class="form-group">
                <label class="control-label col-sm-2"><g:message code="form.option.optionType.file.label"/></label>

                <div class="col-sm-10">
                    <input type="file" name="uploadFile" id="uploadFile" data-bind="event: { change: fileChanged }"/>
                </div>

            </div>

            <div class="form-group" data-bind="css: {'has-error':nameError} ">
                <label class="control-label col-sm-2"><g:message code="aclpolicy.file.upload.name.label"/></label>

                <div class="col-sm-10">
                    <g:textField name="file" class="form-control" data-bind="value: name"/>
                    <!-- ko if: nameError -->
                    <span class="help-block">
                        <g:message code="aclpolicy.file.upload.name.is.required"/>
                    </span>
                    <!-- /ko -->
                </div>
            </div>

        </g:render>
    </g:uploadForm>
</g:if>
</body>
</html>
