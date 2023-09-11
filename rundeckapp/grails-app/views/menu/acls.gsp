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
<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<html>
<g:set var="hasAdminAuth" value="${auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        context: AuthConstants.CTX_APPLICATION,
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasOpsAdminAuth" value="${auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN],
        context: AuthConstants.CTX_APPLICATION,
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasEditAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_UPDATE],
        context: AuthConstants.CTX_APPLICATION,
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasCreateAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_CREATE],
        context: AuthConstants.CTX_APPLICATION,
        kind   : AuthConstants.TYPE_SYSTEM_ACL,
]
)}"/>
<g:set var="hasDeleteAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any    : true,
        action : [AuthConstants.ACTION_DELETE],
        context: AuthConstants.CTX_APPLICATION,
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

    <!-- VUE JS REQUIREMENTS -->
    <g:loadEntryAssets entry="components/ko-paginator" />
    <!-- /VUE JS REQUIREMENTS -->

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

            jQuery.extend(filepolicies,{
                pagingEnabled: ${params.getBoolean('pagingEnabled',cfg.getBoolean(config: 'gui.system.aclList.pagingEnabled',default: true))},
                paging:{
                    max: ${params.getInt('pagingMax')?:cfg.getInteger(config: 'gui.system.aclList.pagingMax', default: 30)}
                }
            })
            window.fspolicies = new PolicyFiles(filepolicies);
            new PagerVueAdapter(window.fspolicies.paging, 'acl-file')
            <g:if test="${clusterMode && hasOpsAdminAuth}">
                window.policiesPage = new SysPoliciesPage({policyFiles: window.fspolicies});
                ko.applyBindings(policiesPage, jQuery('#clusterModeArea')[0]);
            </g:if>
            <g:if test="${!clusterMode}">
                ko.applyBindings(fspolicies, jQuery('#fsPolicies')[0]);
                ko.applyBindings(fspolicies, jQuery('#deleteFSAclPolicy')[0]);
            </g:if>
                let storedpolicies = loadJsonData('aclStoredList');
                jQuery.extend(storedpolicies,{
                    pagingEnabled: ${params.getBoolean('pagingEnabled',cfg.getBoolean(config: 'gui.system.aclList.pagingEnabled',default: true))},
                    paging:{
                        max: ${params.getInt('pagingMax')?:cfg.getInteger(config: 'gui.system.aclList.pagingMax', default: 30)}
                    }
                })
                window.stpolicies = new PolicyFiles(storedpolicies,_rundeck.rdBase+'/menu/ajaxSystemAclMeta');
                new PagerVueAdapter(window.stpolicies.paging, 'acl-stored')
                ko.applyBindings(stpolicies, jQuery('#storedPolicies')[0]);
                ko.applyBindings(stpolicies, jQuery('#deleteStorageAclPolicy')[0]);
                <g:if test="${hasCreateAuth}" >
                    window.aclstorageupload = new PolicyUpload({policies: stpolicies.policies()});
                    stpolicies.fileUpload = aclstorageupload;
                    ko.applyBindings(aclstorageupload, jQuery('#aclStorageUploadForm')[0]);
                </g:if>
            <g:if test="${!clusterMode}">
                window.aclfsupload = new PolicyUpload({ policies: fspolicies.policies()});
                fspolicies.fileUpload = aclfsupload;
                ko.applyBindings(aclfsupload, jQuery('#aclFSUploadForm')[0]);
            </g:if>
            <g:if test="${hasUploadValidationError}" >
                window.uploadedpolicy = new PolicyDocument(loadJsonData('uploadedPolicy'));
                ko.applyBindings(uploadedpolicy, jQuery('#uploadedPolicyValidation')[0]);
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
<div class="content">
<div id="layoutBody">
  <div class="title">
    <span class="text-h3"><i class="fas fa-unlock-alt"></i> ${g.message(code:"gui.menu.AccessControl")}</span>
  </div>
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
                                      class="btn btn-sm btn-primary">
                                  <g:icon name="plus"/>
                                  <g:message code="access.control.action.create.acl.policy.button.title"/>
                              </g:link>
                          </div>
                      </g:if>
                  </div>

                  <div class="card-content panel-content-embed" id="fsPolicies">

                      <div>

                          <g:render template="aclsPagingKO" model="[name: 'acl-file']"/>
                          <g:render template="aclKOTemplates"/>
                          <div data-bind="foreach: policiesView">
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
                  <div class="card-header clearfix" id="storedPolicies_header">
                      <h3 class="card-title pull-left">
                          <g:message code="stored.acl.policy.files.title"/>
                          <span class="badge" style="font-size:1em">${aclStoredList.size()}</span>
                      </h3>
                      <g:if test="${hasCreateAuth}">
                          <div class="btn-group pull-right">
                              <span class="btn btn-sm btn-default" data-toggle="modal" data-target="#aclStorageUpload"
                                    id="storage_acl_upload_btn">
                                  <g:icon name="upload"/>
                                  <g:message code="button.action.Upload"/>
                              </span>
                              <g:link controller="menu"
                                      id="storage_acl_create_btn"
                                      action="createSystemAclFile"
                                      params="${[fileType: 'storage']}"
                                      class="btn btn-sm btn-primary">
                                  <g:icon name="plus"/>
                                  <g:message code="access.control.action.create.acl.policy.button.title"/>
                              </g:link>
                          </div>
                      </g:if>
                  </div>

                  <div class="card-content" id="storedPolicies">
                      <div>
                          <g:render template="aclsPagingKO" model="[name: 'acl-stored']"/>
                          <g:render template="aclKOTemplates"/>
                          <div data-bind="foreach: policiesView" id="storedPolicies_list">
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
          <g:if test="${clusterMode && hasOpsAdminAuth}">
              <div id="clusterModeArea" class="card card-expandable" data-bind="css: { 'card-expandable-open': show }">
                  <div class="card-header">
                    <h4 class="card-title" data-bind="click: toggleShow" style="cursor: pointer;">
                        <span data-bind="visible: !show()" class=" text-secondary">
                            <g:icon name="chevron-right"/>
                        </span>
                        <span  data-bind="visible: show" class=" text-secondary">
                            <g:icon name="chevron-down"/>
                        </span>
                          ${aclFileList.size()}
                          <g:message code="list.of.acl.policy.files.in.directory"/>
                          <span data-bind="if: !policyFiles().valid()">
                              <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                                 title="${message(code: "aclpolicy.format.validation.failed")}"></i>
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
                            <div data-bind="with: policyFiles">
                                <g:render template="aclsPagingKO" model="[name: 'acl-file']"/>
                            </div>
                            <g:render template="aclKOTemplates"/>
                            <div data-bind="foreach: policyFiles().policiesView">
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
</div>
</div>
</body>
</html>
