<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/28/13
  Time: 12:06 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="gui.menu.Security" /></title>
</head>
<body>
<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'securityConfig']"/>
    </div>

    <div class="col-sm-9">
        <div class="well well-sm">
            <div class="text-info">
                <g:message code="page.SecurityConfiguration.description"/>
            </div>
        </div>
        <h4>Login Module</h4>
        <div class="text-muted">
        Authentication is performed using JAAS.  The configuration file is defined using a System property:
        </div>
        <div>
            <g:render template="displayConfigProps" model="[map:System.properties,keys:['java.security.auth.login.config']]"/>
        </div>
        <div class="text-muted">
        The currently used login module:
        </div>
        <div>
            <g:set var="loginmodule" value="${System.getProperty('loginmodule.name', "rundecklogin")}"/>

            <div>
                <code><g:enc>${loginmodule}</g:enc></code>
            </div>
        </div>
        <h4>Access Control</h4>

        <div class="text-muted">
            To modify Access Control rules, create or edit a .aclpolicy file in the Rundeck etc directory.
        </div>
        <div>
            <g:set var="fwkConfigDir" value="${rundeckFramework.getConfigDir()}"/>
            List of ACL Policy Files in directory <code><g:enc>${fwkConfigDir.absolutePath}</g:enc></code>:
            <ul>
            <g:each in="${fwkConfigDir.listFiles().grep{it.name=~/\.aclpolicy$/}}" var="file">
                <li class=""><g:enc>${file.name}</g:enc></li>
            </g:each>
            </ul>
        </div>
    </div>
</div>
</body>
</html>
