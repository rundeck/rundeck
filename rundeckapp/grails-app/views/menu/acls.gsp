%{--
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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/28/13
  Time: 12:06 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<g:set var="hasAdminAuth" value="${auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_ADMIN],
        context: 'application',
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasEditAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_UPDATE],
        context: 'application',
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasCreateAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_CREATE],
        context: 'application',
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasDeleteAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_DELETE],
        context: 'application',
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasUploadValidationError" value="${input?.upload && validation && !validation.valid}"/>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${g.message(code: 'gui.menu.AccessControl')}"/>
    <title><g:message code="gui.menu.AccessControl"/></title>

    <asset:javascript src="menu/aclListing.js"/>
    <script type="application/javascript">
        function SysPoliciesPage(data) {
            var self = this;
            self.show = ko.observable(false);
            self.policyFiles = ko.observable(data.policyFiles);
            self.toggleShow = function () {
                self.show(!self.show());
            };
        }
        jQuery(function () {
            var filepolicies = loadJsonData('aclFileList');
            window.fspolicies = new PolicyFiles(filepolicies);
            <g:if test="${clusterMode}">
            window.policiesPage = new SysPoliciesPage({policyFiles: window.fspolicies});
            ko.applyBindings(policiesPage, jQuery('#clusterModeArea')[0]);
            </g:if>
            <g:else>
            ko.applyBindings(fspolicies, jQuery('#fsPolicies')[0]);
            ko.applyBindings(fspolicies, jQuery('#deleteFSAclPolicy')[0]);
            </g:else>
            var storedpolicies = loadJsonData('aclStoredList');
            window.stpolicies = new PolicyFiles(storedpolicies);
            ko.applyBindings(stpolicies, jQuery('#storedPolicies')[0]);
            ko.applyBindings(stpolicies, jQuery('#deleteStorageAclPolicy')[0]);
            <g:if test="${hasCreateAuth}" >
            window.aclstorageupload = new PolicyUpload({policies: stpolicies.policies()});
            stpolicies.fileUpload = aclstorageupload;
            ko.applyBindings(aclstorageupload, jQuery('#aclStorageUploadForm')[0]);

            <g:if test="${!clusterMode}">

            window.aclfsupload = new PolicyUpload({ policies: fspolicies.policies()});
            fspolicies.fileUpload = aclfsupload;
            ko.applyBindings(aclfsupload, jQuery('#aclFSUploadForm')[0]);

            </g:if>
            <g:if test="${hasUploadValidationError}" >
            window.uploadedpolicy = new PolicyDocument(loadJsonData('uploadedPolicy'));
            ko.applyBindings(uploadedpolicy, jQuery('#uploadedPolicyValidation')[0]);
            </g:if>
            </g:if>
        });
    </script>

    %{--file system acl policies list--}%
    <g:embedJSON id="aclFileList"
                 data="${[policies: aclFileList.collect {
                     it + (flash.storedFile == it.name &&
                                     flash.storedType ==
                                     'fs' ? [wasSaved: true, savedSize: flash.storedSize] :
                                     [:])
                 }]}"/>

    %{--storage acl policies list --}%
    <g:embedJSON id="aclStoredList"
                 data="${[policies: aclStoredList.collect {
                     it +
                             (flash.storedFile == it.name &&
                                     flash.storedType ==
                                     'storage' ? [wasSaved: true, savedSize: flash.storedSize] : [:])
                 }]}"/>
    <g:embedJSON id="uploadedPolicy"
                 data="${hasUploadValidationError ?
                         [id: input?.id,name: input?.name, valid: validation.valid, validation: validation.errors] :
                         [:]}"/>

</head>
<body>
<div class="container-fluid">
  <div class="row">
      <div class="col-sm-12">

          <g:render template="/common/messages"/>

          <g:if test="${hasUploadValidationError}">
              <div id="uploadedPolicyValidation" class="col-sm-12">
                  <div class="alert alert-default alert-dismissible">
                      <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                      <h4><g:message code="aclpolicy.file.upload.failed.acl.policy.validation.message"/></h4>
                      <span data-bind="template: { name: 'acl-policy-validation', data:$data }"></span>
                  </div>
              </div>

          </g:if>
      </div>
  </div>
  <div class="row">
      <div class="col-sm-12">
          <div class="card">
              <g:if test="${!clusterMode}">
                  <div class="card-header clearfix">
                      <span class="panel-title pull-left">
                          <span class="text-info">${aclFileList.size()}</span>
                          <g:message code="list.of.acl.policy.files.in.directory"/>
                          <code>${fwkConfigDir.absolutePath}</code>
                      </span>
                      <g:if test="${hasCreateAuth}">
                          <div class="btn-group pull-right">
                              <span class="btn btn-sm btn-default" data-toggle="modal" data-target="#aclFSUpload">
                                  <g:icon name="upload"/>
                                  <g:message code="button.action.Upload"/>
                              </span>
                              <g:link controller="menu"
                                      action="createSystemAclFile"
                                      params="${[fileType: 'fs']}"
                                      class="btn btn-sm btn-success">
                                  <g:icon name="plus"/>
                                  <g:message code="access.control.action.create.acl.policy.button.title"/>
                              </g:link>
                          </div>
                      </g:if>
                  </div>

                  <div class="card-content panel-content-embed" id="fsPolicies">

                      <div>
                          <div data-bind="foreach: policies">
                              <g:render template="/menu/aclValidationRowKO"
                                        model="${[
                                                hasEditAuth  : hasEditAuth,
                                                hasDeleteAuth: hasDeleteAuth,
                                                editHref     : g.createLink(
                                                        [controller: 'menu', action: 'editSystemAclFile', params: [fileType: 'fs', id: '<$>']]
                                                ),
                                                deleteModalId: 'deleteFSAclPolicy',
                                                uploadModalId: 'aclFSUpload',
                                        ]}"/>

                          </div>


                      </div>
                  </div>

                  <g:render template="/menu/aclManageKO" model="[
                          deleteModalId: 'deleteFSAclPolicy',
                          deleteAction :
                                  [controller: 'menu', action: 'deleteSystemAclFile', params: [fileType: 'fs']],
                          uploadModalId: 'aclFSUpload',
                          uploadFormId: 'aclFSUploadForm',
                          uploadAction : hasCreateAuth || hasEditAuth ?
                                  [controller: 'menu', action: 'saveSystemAclFile', params: [fileType: 'fs', upload: true]] :
                                  null
                  ]"/>

              </g:if>

              <div class="card-header clearfix">
                  <h3 class="card-title pull-left">
                      <g:message code="stored.acl.policy.files.title"/>
                      <span class="badge" style="font-size:1em">${aclStoredList.size()}</span>
                  </h3>
                  <g:if test="${hasCreateAuth}">
                      <div class="btn-group pull-right">
                          <span class="btn btn-sm btn-default" data-toggle="modal" data-target="#aclStorageUpload">
                              <g:icon name="upload"/>
                              <g:message code="button.action.Upload"/>
                          </span>
                          <g:link controller="menu"
                                  action="createSystemAclFile"
                                  params="${[fileType: 'storage']}"
                                  class="btn btn-sm btn-success">
                              <g:icon name="plus"/>
                              <g:message code="access.control.action.create.acl.policy.button.title"/>
                          </g:link>
                      </div>
                  </g:if>
              </div>

              <div class="card-content" id="storedPolicies">
                  <div>
                      <div data-bind="foreach: policies">
                          <g:render template="/menu/aclValidationRowKO"
                                    model="${[
                                            hasEditAuth  : hasEditAuth,
                                            hasDeleteAuth: hasDeleteAuth,
                                            editHref     : g.createLink(
                                                    [controller: 'menu', action: 'editSystemAclFile', params: [fileType: 'storage', id: '<$>']]
                                            ),
                                            deleteModalId: 'deleteStorageAclPolicy',
                                            uploadModalId: 'aclStorageUpload',
                                    ]}"/>

                      </div>
                  </div>
                  <g:render template="/menu/aclManageKO" model="[
                          deleteModalId: 'deleteStorageAclPolicy',
                          deleteAction :
                                  [controller: 'menu', action: 'deleteSystemAclFile', params: [fileType: 'storage']],
                          uploadModalId: 'aclStorageUpload',
                          uploadFormId: 'aclStorageUploadForm',
                          uploadAction : hasCreateAuth || hasEditAuth ?
                                  [controller: 'menu', action: 'saveSystemAclFile', params: [fileType: 'storage', upload: true]] :
                                  null
                  ]"/>
              </div>
          </div>
          <g:if test="${clusterMode}">
              <div id="clusterModeArea" class="card card-expandable" data-bind="css: { 'card-expandable-open': show }">
                  <div class="card-header">
                    <h4 class="card-title" data-bind="click: toggleShow">
                          ${aclFileList.size()}
                          <g:message code="list.of.acl.policy.files.in.directory"/>
                          <span data-bind="if: !policyFiles().valid()">
                              <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                                 title="${message(code: "aclpolicy.format.validation.failed")}"></i>
                          </span>
                          <span data-bind="visible: !show()" class="pull-right">
                            <g:icon name="chevron-down"/>
                          </span>
                          <span  data-bind="visible: show" class="pull-right">
                            <button type="button"
                                    class="close"
                                    data-bind="click: toggleShow"
                                    aria-hidden="true">
                                <g:icon name="chevron-up"/>
                            </button>
                          </span>
                    </h4>
                  </div>
                  <div class="card-content" data-bind="visible: show">
                    <div style="margin-bottom:10px;">
                          <code>${fwkConfigDir.absolutePath}</code>
                          <span data-bind="if: !policyFiles().valid()">

                              <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                                 title="${message(code: "aclpolicy.format.validation.failed")}"></i>
                          </span>
                    </div>
                    <div>
                            <div data-bind="foreach: policyFiles().policies">
                                <g:render template="/menu/aclValidationRowKO"
                                          model="${[
                                                  hasEditAuth  : false,
                                                  hasDeleteAuth: false,
                                          ]}"/>

                            </div>
                    </div>
                </div>
              </div>
          </g:if>

      </div>
  </div>

</div>
</body>
</html>
