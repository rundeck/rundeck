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
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${g.message(code: 'gui.menu.AccessControl')}"/>
    <title><g:message code="gui.menu.AccessControl"/></title>

    <asset:javascript src="menu/aclListing.js"/>
    <script type="application/javascript">
        jQuery(function () {
            var filepolicies = loadJsonData('aclFileList');
            var storedpolicies = loadJsonData('aclStoredList');
            window.fspolicies = new PolicyFiles(filepolicies);
            window.stpolicies = new PolicyFiles(storedpolicies);
            ko.applyBindings(fspolicies, jQuery('#fsPolicies')[0]);
            ko.applyBindings(stpolicies, jQuery('#storedPolicies')[0]);
        })
    </script>

    %{--file system acl policies list--}%
    <g:embedJSON id="aclFileList"
                 data="${[policies: aclFileList.collect {
                     [name: it.name, valid: validations[it] ? validations[it].valid : true,
                      validation: validations[it]?.errors] +
                             (flash.storedFile == it.name &&
                                     flash.storedType ==
                                     'fs' ? [wasSaved: true, savedSize: flash.storedSize] :
                                     [:])
                 }]}"/>

    %{--storage acl policies list --}%
    <g:embedJSON id="aclStoredList"
                 data="${[policies: aclStoredList.collect {
                     [name: it, valid: true,] +
                             (flash.storedFile == it &&
                                     flash.storedType ==
                                     'storage' ? [wasSaved: true, savedSize: flash.storedSize] : [:])
                 }]}"/>

</head>
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
<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>
<div class="row">
    <div class="col-sm-10 col-sm-offset-1">
        <div class="panel panel-default">
            <div class="panel-heading">
                <span class="panel-title">
                    <span class="text-info">${aclFileList.size()}</span>
                    <g:message
                            code="list.of.acl.policy.files.in.directory"/>
                    <code>${fwkConfigDir.absolutePath}</code>:
                </span>
            </div>

            <div class="panel-body" id="fsPolicies">

                <g:if test="${hasCreateAuth}">
                    <div class="col-sm-12">
                        <g:link controller="menu"
                                action="createSystemAclFile"
                                params="${[fileType: 'fs']}"
                                class="btn btn-sm btn-success">
                            <g:icon name="plus"/>
                            <g:message code="access.control.action.create.acl.policy.button.title"/>
                        </g:link>
                    </div>
                </g:if>
                <div class="grid">
                    <div data-bind="foreach: policies">
                        <g:render template="/menu/aclValidationRowKO"
                                  model="${[
                                          hasEditAuth  : hasEditAuth,
                                          hasDeleteAuth: hasDeleteAuth,
                                          editHref     : g.createLink(
                                                  [controller: 'menu', action: 'editSystemAclFile', params: [fileType: 'fs', file: '<$>']]
                                          ),
                                          deleteModalId: 'deleteFSAclPolicy',
                                  ]}"/>

                    </div>

                    <g:render template="/menu/aclManageKO" model="[
                            deleteModalId: 'deleteFSAclPolicy',
                            deleteAction :
                                    [controller: 'menu', action: 'deleteSystemAclFile', params: [fileType: 'fs']]
                    ]"/>
                    <%--
                        <g:each in="${aclFileList}" var="file">
                            <g:render template="/menu/aclValidationTableRow" model="${[
                                    policyFile  : file.name,
                                    validation  : validations[file],
                                    prefix      : fwkConfigDir.absolutePath + '/',
                                    editHref    : hasEditAuth ? g.createLink(
                                            [controller: 'menu', action: 'editSystemAclFile',
                                             params    : [file: file.name, fileType: 'fs']]
                                    ) : null,
                                    flashMessage: flash.storedFile == file.name && flash.storedType == 'fs' ?
                                            g.message(code: 'file.was.saved.flash.message.0',
                                                      args: [flash.storedSize]
                                            ) : null
                            ]}"/>

                        </g:each>
                        --%>
                </div>
            </div>

            <div class="panel-heading">
                <span class="panel-title">
                    <span class="text-info">${aclStoredList.size()}</span>
                    <g:message code="stored.acl.policy.files.prompt"/>
                </span>
            </div>

            <div class="panel-body" id="storedPolicies">
                <g:if test="${hasCreateAuth}">
                    <div class="col-sm-12">
                        <g:link controller="menu"
                                action="createSystemAclFile"
                                params="${[fileType: 'storage']}"
                                class="btn btn-sm btn-success">
                            <g:icon name="plus"/>
                            <g:message code="access.control.action.create.acl.policy.button.title"/>
                        </g:link>
                    </div>
                </g:if>
                <div class="grid">
                    <div data-bind="foreach: policies">
                        <g:render template="/menu/aclValidationRowKO"
                                  model="${[
                                          hasEditAuth  : hasEditAuth,
                                          hasDeleteAuth: hasDeleteAuth,
                                          editHref     : g.createLink(
                                                  [controller: 'menu', action: 'editSystemAclFile', params: [fileType: 'storage', file: '<$>']]
                                          ),
                                          deleteModalId: 'deleteStorageAclPolicy',
                                  ]}"/>

                    </div>

                    <g:render template="/menu/aclManageKO" model="[
                            deleteModalId: 'deleteStorageAclPolicy',
                            deleteAction :
                                    [controller: 'menu', action: 'deleteSystemAclFile', params: [fileType: 'storage']]
                    ]"/>
                    <%--
                        <g:each in="${aclStoredList}" var="name">
                            <g:render template="/menu/aclValidationTableRow" model="${[
                                    policyFile  : name,
                                    validation  : [valid: true],
                                    editHref    : hasEditAuth ? g.createLink(
                                            [controller: 'menu', action: 'editSystemAclFile',
                                             params    : [file: name, fileType: 'storage']]
                                    ) : null,
                                    flashMessage: flash.storedFile == name && flash.storedType == 'storage' ?
                                            g.message(code: 'file.was.saved.flash.message.0',
                                                      args: [flash.storedSize]
                                            ) : null
                            ]}"/>

                        </g:each>
                        --%>
                </div>
            </div>
        </div>

    </div>
</div>
</body>
</html>
