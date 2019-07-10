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

    <g:jsMessages code="userController.page.profile.pager.summary"/>

    <g:set var="currentLang" value="${response.locale?.toString() ?: request.locale?.toString()}"/>
    <g:embedJSON
            data="${[user             : user.login,
                     roles            : authRoles,
                     adminAuth        : tokenAdmin,
                     userTokenAuth    : selfToken,
                     svcTokenAuth     : serviceToken,
                     language         : currentLang,
                     tokenPagingMax   : params.max,
                     tokenPagingOffset: params.offset,
                     tokenTotal       : tokenTotal,
                     tokenTableSummaryText: message(code: "userController.page.profile.pager.summary",
                             args: [
                                     params.offset.toInteger() + 1,
                                     Math.min((params.offset.toInteger() + params.max.toInteger()), tokenTotal),
                                     tokenTotal
                             ])
            ]}"
            id="genPageData"/>
</head>
<body>
<div class="container-fluid">
  <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <g:render template="/common/messages"/>
        </div>
      </div>
      <div class="col-sm-12">
          <div class="card">
              <div class="card-content">
                <div class="row">
                    <div class="col-xs-6">
                    <div class="pull-left">
                      <g:link action="edit"
                              params="[login: user.login]"
                              class="btn btn-sm"
                              title="${message(code: 'userController.action.edit.description', args: [user.login])}">
                          <g:icon name="edit"/>
                          <g:message code="button.Edit.label"/>
                      </g:link>
                    </div>
                  </div>

                    <div class="col-xs-6 form-inline">
                    <div class="form-group pull-right">
                        <label for="language" class=" control-label"><g:message
                            code="user.profile.language.label"/></label>


                        <g:set var="supportedLangs" value="${
                            [
                                en_US : 'English',
                                es_419: 'Español',
                                fr_FR : 'Français',
                                zh_CN : '简体中文',
                            ]
                        }"/>
                        <g:select class="form-control" name="language" id="language" onchange="changeLanguage();"
                                  value="${currentLang}" from="${supportedLangs}" optionKey="key"
                                  optionValue="value">

                        </g:select>


                    </div>
                  </div>
                </div>

                  <div class="help-block">
                      <g:message code="userController.page.profile.description"/>
                  </div>
                  <div class="pageBody" id="userProfilePage">
                      <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
                      <tmpl:user user="${user}" edit="${true}" tokens="${tokenList}" tokenTotal="${tokenTotal}" />
                  </div>
              </div>
          </div>
      </div>
  </div>
</div>
</body>
</html>
