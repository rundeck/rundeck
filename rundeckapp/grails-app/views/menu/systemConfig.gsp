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
  Date: 10/25/13
  Time: 3:50 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="com.dtolabs.rundeck.core.common.FilesystemFramework; com.dtolabs.rundeck.core.common.Framework" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${g.message(code: 'gui.menu.SystemConfig')}"/>
    <title><g:message code="gui.menu.SystemConfig" /></title>
</head>

<body>
  <div class="container-fluid">
    <div class="row">
        <div class="col-sm-12">
            <g:render template="/common/messages"/>
        </div>
    </div>
    <div class="row">
      <g:set var="flatConfig" value="${grailsApplication.config.flatten()}"/>
      <g:set var="fwkProps" value="${rundeckFramework.propertyLookup.propertiesMap}"/>
      <div class="col-xs-12">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title"><g:message code="gui.menu.SystemConfig" /></h3>
            <hr>
          </div>
          <div class="card-content">
            <g:set var="fwkPropFile" value="${FilesystemFramework.getPropertyFile(rundeckFramework.getConfigDir())}"/>
            <div class="text-primary"><g:enc>${fwkPropFile.absolutePath}</g:enc>:</div>
            <g:render template="displayConfigProps" model="[obscurePattern:~/password/,map: fwkProps, keys: fwkProps.keySet().findAll{it=~/^framework.server/}]"/>
            <div class="text-primary" style="margin-top:1em;"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>
            <g:render template="displayConfigProps" model="[map: flatConfig, keys: ['grails.serverURL']]"/>

            <hr>

            <h4>SSH Defaults</h4>
            <div class="text-primary"><g:enc>${fwkPropFile.absolutePath}</g:enc>:</div>
            <g:render template="displayConfigProps" model="[map: fwkProps,keys: fwkProps.keySet().findAll{it=~/^framework\.ssh\./}]"/>

            <hr>

            <h4>Datasource</h4>
            <div class="text-primary"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>
            <g:render template="displayConfigProps" model="[map: flatConfig, keys: ['dataSource.url']]"/>

            <hr>

            <h4>Plugins</h4>
            <div class="text-primary"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>
            <g:render template="displayConfigProps" model="[map: flatConfig, keys: flatConfig.keySet().grep(~/^rundeck\.execution\.logs\..*$/)]"/>

            <hr>

            <h4><g:message code="login.module" /></h4>
            <div class="text-primary">
              <g:message code="authentication.is.performed.using.jaas.the.configuration.file.is.defined.using.a.system.property" />
            </div>
            <div>
              <g:render template="displayConfigProps" model="[map:System.properties,keys:['java.security.auth.login.config']]"/>
            </div>
            <div class="text-primary" style="margin-top:1em;">
              <g:message code="the.currently.used.login.module" />
            </div>
            <div>
              <g:set var="loginmodule" value="${System.getProperty('loginmodule.name', "rundecklogin")}"/>
              <div>
                <code><g:enc>${loginmodule}</g:enc></code>
              </div>
            </div>
          </div>
          <div class="card-footer">
            <hr>
            <div class="text-info">
                <g:message code="page.SystemConfiguration.description"/>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>
