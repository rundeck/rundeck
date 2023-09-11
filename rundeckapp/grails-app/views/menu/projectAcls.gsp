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
  Date: 7/7/17
  Time: 9:01 AM
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<html>

<g:set var="hasAdminAuth" value="${auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        context   : AuthConstants.CTX_APPLICATION,
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasEditAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_UPDATE],
        context   : AuthConstants.CTX_APPLICATION,
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasCreateAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_CREATE],
        context   : AuthConstants.CTX_APPLICATION,
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasDeleteAuth" value="${hasAdminAuth || auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_DELETE],
        context   : AuthConstants.CTX_APPLICATION,
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
    <meta name="projconfigselected" content="access-control"/>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[params.project]:params.project}"/>
    <title><g:message code="page.title.project.access.control.0" args="${[projectLabel]}"/></title>

    <!-- VUE JS REQUIREMENTS -->
    <g:loadEntryAssets entry="components/ko-paginator" />
    <!-- /VUE JS REQUIREMENTS -->

    <asset:javascript src="menu/aclListing.js"/>
    <script type="application/javascript">
        var checkUploadForm;
        let project="${enc(js:params.project)}"
        jQuery(function () {
            var data = loadJsonData('aclPolicyList');
            jQuery.extend(data,{
                pagingEnabled: ${params.getBoolean('pagingEnabled',cfg.getBoolean(config: 'gui.system.aclList.pagingEnabled',default: true))},
                paging:{
                    max: ${params.getInt('pagingMax')?:cfg.getInteger(config: 'gui.system.aclList.pagingMax', default: 30)}
                }
            })
            window.policies = new PolicyFiles(data,_genUrl(_rundeck.rdBase+'/menu/ajaxProjectAclMeta',{project:project}));
            new PagerVueAdapter(window.policies.paging, 'acl-stored')
            ko.applyBindings(policies, jQuery('#policyList')[0]);
            ko.applyBindings(policies, jQuery('#deleteAclPolicy')[0]);
            <g:if test="${hasCreateAuth}" >
            window.aclfileupload = new PolicyUpload({uploadField: '#uploadFile', policies: policies.policies()});
            policies.fileUpload = aclfileupload;
            ko.applyBindings(aclfileupload, jQuery('#aclUploadForm')[0]);
            <g:if test="${hasUploadValidationError}" >
            window.uploadedpolicy = new PolicyDocument(loadJsonData('uploadedPolicy'));
            ko.applyBindings(uploadedpolicy, jQuery('#uploadedPolicyValidation')[0]);
            </g:if>
            </g:if>
        })

    </script>
    <g:embedJSON data="${[policies: acllist.collect {
        [name: it.name,id:it.id, valid: true, meta: it.meta] + (flash.storedFile == it.id ? [wasSaved: true, savedSize: flash.storedSize] : [:])
    }]}" id="aclPolicyList"/>
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
    <div class="col-xs-12">
      <div class="card">
        <div class="card-header clearfix">
          <h3 class="card-title">
            <g:message code="project.access.control.prompt" args="${[params.project]}"/>
            <span class="label label-default">${acllist?.size() ?: 0}</span>
            <g:if test="${hasCreateAuth}">
              <div class="pull-right">
                <span class="btn btn-sm btn-default" data-toggle="modal" data-target="#aclUpload">
                  <g:icon name="upload"/>
                  <g:message code="button.action.Upload"/>
                </span>
                <g:link controller="menu"
                        action="createProjectAclFile"
                        params="${[project: params.project]}"
                        class="btn btn-sm btn-primary">
                  <g:icon name="plus"/>
                  <g:message code="access.control.action.create.acl.policy.button.title"/>
                </g:link>
              </div>
            </g:if>
          </h3>
        </div>

        <div class="card-content" id="policyList">
            <g:render template="aclsPagingKO" model="[name: 'acl-stored']"/>
            <g:render template="aclKOTemplates"/>
            <div data-bind="foreach: policiesView">
              <g:render template="/menu/aclValidationRowKO"
                        model="${[
                                hasEditAuth  : hasEditAuth,
                                hasDeleteAuth: hasDeleteAuth,
                                hasCreateAuth: hasCreateAuth,
                                editHref     : g.createLink(
                                        [controller: 'menu', action: 'editProjectAclFile', params: [project: params.project, id: '<$>']]
                                ),
                                deleteModalId: 'deleteAclPolicy',
                                uploadModalId: 'aclUpload',

              ]}"/>

            </div>
        </div>
      </div>
    </div>
  </div>
  <g:render template="/menu/aclManageKO" model="[
                                                deleteAction : [controller: 'menu', action: 'deleteProjectAclFile', params: [project: params.project]],
                                                uploadModalId: 'aclUpload',
                                                uploadAction : hasCreateAuth || hasEditAuth ? [controller: 'menu', action: 'saveProjectAclFile', params: [project: params.project, upload: true]] : null
  ]"/>
</div>
</div>
</div>
</body>
</html>
