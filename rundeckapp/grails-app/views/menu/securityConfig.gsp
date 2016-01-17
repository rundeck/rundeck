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
        <h4><g:message code="login.module" /></h4>
        <div class="text-muted">
        <g:message code="authentication.is.performed.using.jaas.the.configuration.file.is.defined.using.a.system.property" />
        </div>
        <div>
            <g:render template="displayConfigProps" model="[map:System.properties,keys:['java.security.auth.login.config']]"/>
        </div>
        <div class="text-muted">
        <g:message code="the.currently.used.login.module" />
        </div>
        <div>
            <g:set var="loginmodule" value="${System.getProperty('loginmodule.name', "rundecklogin")}"/>

            <div>
                <code><g:enc>${loginmodule}</g:enc></code>
            </div>
        </div>
        <h4><g:message code="access.control" /></h4>

        <div class="text-muted">
            <g:message code="to.modify.access.control.rules.create.or.edit.a.aclpolicy.file.in.the.rundeck.etc.directory" />
        </div>
        <div>
            <g:message code="list.of.acl.policy.files.in.directory" /> <code>${fwkConfigDir.absolutePath}</code>:
            <ul>
            <g:each in="${aclFileList}" var="file">
                <li>
                <g:if test="${validations[file].valid}">
                    <i class="glyphicon glyphicon-ok text-success has_tooltip" title="${message(code:"aclpolicy.format.validation.succeeded")}"></i>
                </g:if>
                <g:else>
                    <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip" title="${message(code:"aclpolicy.format.validation.failed")}"></i>
                </g:else>
                <span class="${validations[file].valid?'':'text-warning'}">${file.name}</span>
                <g:set var="akey" value="${g.rkey()}"/>
                <g:if test="${!validations[file].valid}">
                    <g:expander key="${akey}"><g:message code="more" /></g:expander>
                    <div class="well well-sm well-embed" id="${akey}" style="display: none">

                    <ol>
                    <g:each in="${validations[file].errors.keySet().sort()}" var="ident">

                            <li><code>${ident}</code><g:helpTooltip css="text-info" code="acl.validation.error.sourceIdentity.help"/>
                            <ol>
                                <g:each in="${validations[file].errors[ident]}" var="message">
                                    <li><code>${message}</code></li>
                                </g:each>
                                </ol>
                            </li>
                    </g:each>
                    </ol>
                    </div>
                </g:if>
                </li>
            </g:each>
            </ul>
        </div>
    </div>
</div>
</body>
</html>
