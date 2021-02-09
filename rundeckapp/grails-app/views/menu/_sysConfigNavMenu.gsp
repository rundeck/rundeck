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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>

<g:set var="authRead" value="${auth.resourceAllowedTest(
        type: 'resource',
        kind: 'system',
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
        any: true,
        context: 'application'
)}"/>

<g:set var="pluginRead" value="${auth.resourceAllowedTest(
        type: 'resource',
        kind: 'plugin',
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
        any: true,
        context: 'application'
)}"/>
<g:set var="pluginInstall" value="${auth.resourceAllowedTest(
        type: 'resource',
        kind: 'plugin',
        action: ["install","admin"],
        any: true,
        context: 'application'
)}"/>
<g:set var="repoEnabled" value="${grailsApplication.config.rundeck?.feature?.repository?.enabled in [true,'true']}"/>

<style>
.dropdown-submenu {
  position: relative;
}

.dropdown-submenu .dropdown-menu {
  top: 0;
  right: 100%;
  margin-top: -1px;
  display: none;
}
</style>

<ul class="dropdown-menu dropdown-menu-right">
  <li class="dropdown-header">System</li>
  <li>
    <g:link controller="menu" action="storage">
        <g:message code="gui.menu.KeyStorage"/>
    </g:link>
  </li>
  <g:if test="${authRead}">
    <li>
      <g:link controller="menu" action="systemConfig">
        <g:message code="gui.menu.SystemConfig"/>
      </g:link>
    </li>
    <li>
      <g:link controller="menu" action="acls">
        <g:message code="gui.menu.AccessControl"/>
      </g:link>
    </li>
    <li>
      <g:link controller="menu" action="systemInfo">
        <g:message code="gui.menu.SystemInfo"/>
      </g:link>
    </li>
    <li>
      <g:link shown="${g.logStorageEnabled()}" controller="menu" action="logStorage">
        <g:message code="gui.menu.LogStorage"/>
      </g:link>
    </li>
  </g:if>
<g:if test="${pluginRead && repoEnabled}">
  <li class="dropdown-submenu">
    <a href="#">Plugins <span class="caret"></span></a>
    <ul class="dropdown-menu">
      <li>
        <a href="${g.createLink(uri:'/artifact/index/repositories')}">
            <g:message code="gui.menu.FindPlugins"/>
        </a>
      </li>
      <li>
        <a href="${g.createLink(uri:'/artifact/index/configurations')}">
          <g:message code="gui.menu.InstalledPlugins"/>
        </a>
      </li>
      <li>
        <a href="${g.createLink(uri:'/artifact/index/upload')}">
          <g:message code="gui.menu.UploadPlugin"/>
        </a>
      </li>
    </ul>
  </li>
</g:if>
<g:if test="${pluginRead && !repoEnabled}">
  <li>
    <a href="${g.createLink(uri:'/artifact/index/configurations')}">
      <g:message code="gui.menu.InstalledPlugins"/>
    </a>
  </li>
</g:if>
<g:if test="${pluginInstall && !repoEnabled}">
  <li>
    <a href="${g.createLink(uri:'/artifact/index/upload')}">
      <g:message code="gui.menu.UploadPlugin"/>
    </a>
  </li>
</g:if>
  <li>
    <g:link controller="passwordUtility" action="index">
      <g:message code="gui.menu.PasswordUtility"/>
    </g:link>
  </li>
    <g:ifMenuItems type="SYSTEM_CONFIG">
        <li role="separator" class="divider"></li>
    </g:ifMenuItems>
    <g:forMenuItems type="SYSTEM_CONFIG" var="item">
        <li>
            <a href="${enc(attr:item.href)}"
               title="${enc(attr:g.message(code:item.titleCode,default:item.title))}">
                <g:message code="${item.titleCode}" default="${item.title}"/>
            </a>
        </li>
    </g:forMenuItems>
  <g:render template="/menu/sysConfigExecutionModeNavMenu"/>
</ul>
<script>
jQuery(document).ready(function($){
  $('.dropdown-submenu > a').on("click", function(e){
    $(this).next('ul').toggle();
    e.stopPropagation();
    e.preventDefault();
  });
});
</script>
