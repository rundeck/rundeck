<%@ page import="rundeck.AuthToken; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>%{--
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

    <g:set var="tokenAdmin" value="${auth.resourceAllowedTest(
            kind: 'user',
            action: [AuthConstants.ACTION_ADMIN],
            context: 'application'
    )}"/>
    <g:set var="selfToken" value="${auth.resourceAllowedTest(
            kind: 'apitoken',
            action: [AuthConstants.GENERATE_USER_TOKEN],
            context: 'application'
    )}"/>
    <g:set var="serviceToken" value="${auth.resourceAllowedTest(
            kind: 'apitoken',
            action: [AuthConstants.GENERATE_SERVICE_TOKEN],
            context: 'application'
    )}"/>
    <g:appTitle/> - <g:message code="userController.page.profile.title"/>: ${user.login}</title>
    <asset:javascript src="user/profile.js"/>
    <script type="text/javascript">

    function changeLanguage() {
        var url = '${g.createLink(controller: 'user', action: 'profile')}';
        window.location.href = url + "?lang=" + jQuery("#language").val();
    }
    </script>
    <g:embedJSON
            data="${[user         : user.login,
                     roles        : authRoles,
                     adminAuth    : tokenAdmin,
                     userTokenAuth: selfToken,
                     svcTokenAuth : serviceToken,
                     //grails stores current locale in http session under below key
                     language     : session[org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME ]
            ]}"
            id="genPageData"></g:embedJSON>
</head>
<body>

<div class="row">
    <g:render template="/common/messages"/>
    <div class="col-sm-12">
        <div class="panel panel-default">

            <div class="panel-heading">
                <span class="panel-title">
                    <g:link action="profile" params="[login: user.login]">
                        <g:icon name="user"/>
                        ${user.login}
                    </g:link>

                    <g:link action="edit"
                            params="[login: user.login]"
                            class="small btn btn-link btn-sm"
                            title="${message(code: 'userController.action.edit.description', args: [user.login])}">
                        <g:icon name="edit"/>
                        <g:message code="button.Edit.label"/>
                    </g:link>
                </span>
                <span class="pull-right">
                    <label for="language"><g:message code="user.profile.language.label"/></label>
                    <select name="language" id="language" onchange="changeLanguage();">
                        <option value="">English</option>
                        <option value="es_419">Español</option>
                        <option value="fr_FR">Français</option>
                        <option value="zh_cn">简体中文</option>
                    </select>
                </span>
            </div>
            <div class="panel-body">

                <div class="help-block">
                    <g:message code="userController.page.profile.description"/>
                </div>
                <div class="pageBody" id="userProfilePage">
                    <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
                    <tmpl:user user="${user}" edit="${true}"/>
                </div>
            </div>
        </div>
    </div>
</div>

</body>
</html>


